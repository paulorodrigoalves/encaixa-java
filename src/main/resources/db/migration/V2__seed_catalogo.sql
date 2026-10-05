-- =============================================================================
-- V2__seed_catalogo.sql
-- Dados iniciais do catalogo (mesmos valores de referencia do legado).
-- Ajuste pelo painel admin depois — nada aqui e regra de codigo.
-- =============================================================================

INSERT INTO regras_precificacao (preco_por_litro, preco_por_cm2_divisoria, criado_por)
VALUES (18.00, 0.35, 'seed');

INSERT INTO tipos_material (nome, espessura_mm, margem_mm) VALUES
    ('Veludo',    6, 5),
    ('Linho',     6, 5),
    ('Sintetico', 6, 5);

INSERT INTO materiais (nome, cor, valor_fixo_adicional, tipo_material_id)
SELECT m.nome, m.cor, m.valor, t.id
FROM (VALUES
    ('Veludo Cinza',     '#9ca3af', 40.00, 'Veludo'),
    ('Veludo Azul',      '#3b82f6', 40.00, 'Veludo'),
    ('Veludo Rosa',      '#ec4899', 40.00, 'Veludo'),
    ('Linho Natural',    '#d4b896', 15.00, 'Linho'),
    ('Sintetico Branco', '#e5e7eb',  0.00, 'Sintetico')
) AS m(nome, cor, valor, tipo)
JOIN tipos_material t ON t.nome = m.tipo;

INSERT INTO templates (nome, categoria, layout_proporcional, tags_objetos) VALUES
(
  'Maquiagem', 'Beleza',
  '{"colunas":[{"proporcao":0.35,"subdivisoes":[{"proporcao":0.5},{"proporcao":0.5}]},{"proporcao":0.35,"subdivisoes":[{"proporcao":0.333},{"proporcao":0.333},{"proporcao":0.334}]},{"proporcao":0.30,"subdivisoes":[{"proporcao":1.0}]}]}',
  ARRAY['batom','base','pó','blush','sombra','pincel','esmalte','perfume']
),
(
  'Cozinha', 'Cozinha',
  '{"colunas":[{"proporcao":0.5,"subdivisoes":[{"proporcao":1.0}]},{"proporcao":0.5,"subdivisoes":[{"proporcao":0.5},{"proporcao":0.5}]}]}',
  ARRAY['talher','tempero','potinho','utensílio','guardanapo']
),
(
  'Joias', 'Beleza',
  '{"colunas":[{"proporcao":0.333,"subdivisoes":[{"proporcao":0.4},{"proporcao":0.6}]},{"proporcao":0.333,"subdivisoes":[{"proporcao":0.4},{"proporcao":0.6}]},{"proporcao":0.334,"subdivisoes":[{"proporcao":0.4},{"proporcao":0.6}]}]}',
  ARRAY['anel','brinco','colar','pulseira','relógio','joia']
),
(
  'Escritório', 'Escritório',
  '{"colunas":[{"proporcao":0.6,"subdivisoes":[{"proporcao":1.0}]},{"proporcao":0.4,"subdivisoes":[{"proporcao":0.333},{"proporcao":0.333},{"proporcao":0.334}]}]}',
  ARRAY['caneta','lápis','clipe','post-it','carregador','cabo','cartão']
);

-- Exemplos de faixas de medidas (substituir pelos valores reais do vendedor).
INSERT INTO tipos_objeto (nome, categoria, largura_min_mm, largura_max_mm, profundidade_min_mm, profundidade_max_mm, altura_min_mm) VALUES
    ('Anéis',    'Joias e acessórios', 40, 80,   80, 200, NULL),
    ('Brincos',  'Joias e acessórios', 40, 80,   60, 150, NULL),
    ('Colares',  'Joias e acessórios', 60, 120, 150, 300, NULL),
    ('Relógio',  'Joias e acessórios', 60, 90,   80, 120, 70),
    ('Óculos',   'Joias e acessórios', 70, 90,  150, 180, NULL),
    ('Batom',    'Maquiagem',          30, 60,   80, 200, NULL),
    ('Pincéis',  'Maquiagem',          50, 90,  180, 220, NULL),
    ('Talheres', 'Cozinha',            70, 110, 200, 280, NULL);

INSERT INTO etapas_producao (nome, ordem, usa_rastreio) VALUES
    ('Compra aprovada', 1, false),
    ('Corte do MDF',    2, false),
    ('Revestimento',    3, false),
    ('Embalagem',       4, false),
    ('Envio',           5, true),
    ('Entregue',        6, true);
