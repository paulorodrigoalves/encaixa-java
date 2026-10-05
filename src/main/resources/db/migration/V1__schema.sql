-- =============================================================================
-- V1__schema.sql
-- Schema consolidado do Encaixa (equivalente as migrations 0001-0017 do legado
-- Node, ja com as limpezas: sem pedidos.status/projeto_id/orcamento_id, sem
-- projetos.cliente_id, espessura/margem no TIPO de material).
-- =============================================================================

-- ── Catalogo ────────────────────────────────────────────────────────────────

-- Espessura da divisoria pronta (madeira + revestimento nas duas faces) e folga
-- TOTAL de encaixe sao do tipo de material (veludo, courino...), nao da cor.
CREATE TABLE tipos_material (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          TEXT        NOT NULL UNIQUE,
    espessura_mm  INTEGER     NOT NULL CHECK (espessura_mm > 0),
    margem_mm     INTEGER     NOT NULL CHECK (margem_mm > 0),
    ativo         BOOLEAN     NOT NULL DEFAULT true,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE materiais (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                  TEXT          NOT NULL,
    cor                   TEXT          NOT NULL,              -- hex, ex: #3b82f6
    valor_fixo_adicional  NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (valor_fixo_adicional >= 0),
    tipo_material_id      UUID          NOT NULL REFERENCES tipos_material(id),
    ativo                 BOOLEAN       NOT NULL DEFAULT true,
    criado_em             TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX materiais_tipo_idx ON materiais (tipo_material_id);

-- Fotos de organizadores revestidos com cada material (galeria do catalogo).
CREATE TABLE material_fotos (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id  UUID        NOT NULL REFERENCES materiais(id) ON DELETE CASCADE,
    foto_url     TEXT        NOT NULL,
    storage_key  TEXT,                                        -- chave no bucket (pra remover o arquivo)
    legenda      TEXT,
    ordem        INTEGER     NOT NULL DEFAULT 0,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX material_fotos_material_idx ON material_fotos (material_id, ordem, criado_em);

-- layout_proporcional: {"colunas":[{"proporcao":0.5,"subdivisoes":[{"proporcao":1.0}]}, ...]}
CREATE TABLE templates (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                 TEXT        NOT NULL,
    categoria            TEXT        NOT NULL,
    layout_proporcional  JSONB       NOT NULL,
    thumbnail_url        TEXT,
    tags_objetos         TEXT[]      NOT NULL DEFAULT '{}',
    ativo                BOOLEAN     NOT NULL DEFAULT true,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Faixa de medidas de compartimento que cada objeto precisa (experiencia do vendedor).
CREATE TABLE tipos_objeto (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                 TEXT        NOT NULL UNIQUE,
    categoria            TEXT,                                -- null = "Outros"
    largura_min_mm       INTEGER     NOT NULL,
    largura_max_mm       INTEGER     NOT NULL,
    profundidade_min_mm  INTEGER     NOT NULL,
    profundidade_max_mm  INTEGER     NOT NULL,
    altura_min_mm        INTEGER,                             -- null = sem exigencia
    ativo                BOOLEAN     NOT NULL DEFAULT true,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (largura_min_mm > 0 AND largura_min_mm <= largura_max_mm),
    CHECK (profundidade_min_mm > 0 AND profundidade_min_mm <= profundidade_max_mm),
    CHECK (altura_min_mm IS NULL OR altura_min_mm > 0)
);

-- ── Precificacao ────────────────────────────────────────────────────────────

-- Versionada: nunca UPDATE nos valores. Nova regra = nova linha + fecha a anterior.
CREATE TABLE regras_precificacao (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    preco_por_litro          NUMERIC(10,2) NOT NULL CHECK (preco_por_litro > 0),
    preco_por_cm2_divisoria  NUMERIC(10,4) NOT NULL CHECK (preco_por_cm2_divisoria > 0),
    vigente_desde            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    vigente_ate              TIMESTAMPTZ,                     -- null = regra ativa
    criado_por               TEXT                             -- "sub" do JWT do admin
);

-- Garante no banco que existe no maximo UMA regra ativa.
CREATE UNIQUE INDEX regras_precificacao_unica_ativa
    ON regras_precificacao ((vigente_ate IS NULL)) WHERE vigente_ate IS NULL;

-- ── Vendas ──────────────────────────────────────────────────────────────────

CREATE TABLE clientes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome       TEXT        NOT NULL,
    telefone   TEXT        NOT NULL,
    email      TEXT,
    endereco   JSONB,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Carrinho fechado: agrupa N projetos (itens). Frete e total ficam aqui.
CREATE TABLE compras (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id        UUID          NOT NULL REFERENCES clientes(id),
    status            TEXT          NOT NULL DEFAULT 'FECHADA' CHECK (status IN ('FECHADA', 'APROVADA')),
    valor_frete       NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_total       NUMERIC(10,2) NOT NULL DEFAULT 0,       -- soma(itens x quantidade) + frete
    transportadora    TEXT,
    prazo_frete_dias  INTEGER,
    criado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX compras_cliente_idx ON compras (cliente_id);
CREATE INDEX compras_criado_idx ON compras (criado_em DESC);

-- Um organizador configurado. Layout vem de um template OU de uma lista de
-- objetos, nunca dos dois. Espessura/margem sao snapshot do tipo de material.
CREATE TABLE projetos (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id              UUID        REFERENCES compras(id) ON DELETE CASCADE,
    template_id            UUID        REFERENCES templates(id),
    itens_objeto           JSONB,
    material_id            UUID        NOT NULL REFERENCES materiais(id),
    largura_mm             INTEGER     NOT NULL CHECK (largura_mm BETWEEN 50 AND 1200),
    profundidade_mm        INTEGER     NOT NULL CHECK (profundidade_mm BETWEEN 50 AND 1200),
    altura_mm              INTEGER     NOT NULL CHECK (altura_mm BETWEEN 30 AND 300),
    margem_seguranca_mm    INTEGER     NOT NULL,
    espessura_material_mm  INTEGER     NOT NULL,
    quantidade             INTEGER     NOT NULL DEFAULT 1 CHECK (quantidade BETWEEN 1 AND 50),
    status                 TEXT        NOT NULL DEFAULT 'RASCUNHO' CHECK (status IN ('RASCUNHO', 'FECHADO')),
    criado_em              TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT projetos_origem_layout CHECK ((template_id IS NULL) <> (itens_objeto IS NULL))
);

CREATE INDEX projetos_compra_idx ON projetos (compra_id);

CREATE TABLE compartimentos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id        UUID          NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    pos_x_mm          INTEGER       NOT NULL,
    pos_y_mm          INTEGER       NOT NULL,
    largura_mm        INTEGER       NOT NULL CHECK (largura_mm > 0),
    profundidade_mm   INTEGER       NOT NULL CHECK (profundidade_mm > 0),
    area_paineis_cm2  NUMERIC(10,2) NOT NULL
);

CREATE INDEX compartimentos_projeto_idx ON compartimentos (projeto_id);

-- Snapshot imutavel do calculo: preco nao muda retroativamente.
CREATE TABLE orcamentos (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projeto_id             UUID           NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    regra_precificacao_id  UUID           NOT NULL REFERENCES regras_precificacao(id),
    volume_total_litros    NUMERIC(10,3)  NOT NULL,
    area_divisorias_cm2    NUMERIC(12,2)  NOT NULL,
    valor_volume           NUMERIC(10,2)  NOT NULL,
    valor_divisorias       NUMERIC(10,2)  NOT NULL,
    valor_material         NUMERIC(10,2)  NOT NULL,
    valor_frete            NUMERIC(10,2)  NOT NULL DEFAULT 0,
    valor_total            NUMERIC(10,2)  NOT NULL,
    criado_em              TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX orcamentos_projeto_idx ON orcamentos (projeto_id, criado_em DESC);

-- ── Producao ────────────────────────────────────────────────────────────────

-- Criado quando o admin confirma o pagamento. Um por compra.
CREATE TABLE pedidos (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id            UUID        NOT NULL UNIQUE REFERENCES compras(id),
    codigo_rastreio      TEXT,
    prazo_estimado_dias  INTEGER     NOT NULL DEFAULT 15,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Catalogo editavel de etapas de fabricacao.
CREATE TABLE etapas_producao (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          TEXT    NOT NULL UNIQUE,
    ordem         INTEGER NOT NULL,
    usa_rastreio  BOOLEAN NOT NULL DEFAULT false,
    ativo         BOOLEAN NOT NULL DEFAULT true
);

-- Historico do pedido (linha do tempo do cliente). nome_etapa e snapshot:
-- renomear a etapa no catalogo nao reescreve o historico.
CREATE TABLE pedido_etapas (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id    UUID        NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    etapa_id     UUID        REFERENCES etapas_producao(id) ON DELETE SET NULL,
    nome_etapa   TEXT        NOT NULL,
    observacao   TEXT,
    foto_url     TEXT,
    storage_key  TEXT,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX pedido_etapas_pedido_idx ON pedido_etapas (pedido_id, criado_em);
