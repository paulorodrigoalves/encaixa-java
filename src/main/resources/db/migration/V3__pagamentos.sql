-- Pagamentos PIX (Mercado Pago). Um carrinho (compra) pode ter varias tentativas.
CREATE TABLE pagamentos (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id          UUID          NOT NULL REFERENCES compras(id),
    provider           TEXT          NOT NULL DEFAULT 'MERCADO_PAGO',
    provider_payment_id TEXT,
    status             TEXT          NOT NULL DEFAULT 'PENDENTE'
                       CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO', 'CANCELADO', 'ESTORNADO')),
    valor              NUMERIC(10,2) NOT NULL,
    pix_copia_cola     TEXT,
    pix_qr_base64      TEXT,
    ticket_url         TEXT,
    criado_em          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX pagamentos_compra_idx ON pagamentos (compra_id, criado_em);
CREATE UNIQUE INDEX pagamentos_provider_payment_uk ON pagamentos (provider_payment_id) WHERE provider_payment_id IS NOT NULL;
