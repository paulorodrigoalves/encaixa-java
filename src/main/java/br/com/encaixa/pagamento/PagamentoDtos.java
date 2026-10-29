package br.com.encaixa.pagamento;

import java.math.BigDecimal;
import java.util.UUID;

public final class PagamentoDtos {

    private PagamentoDtos() {}

    public record PagamentoResponse(
            UUID pagamentoId,
            UUID compraId,
            String status,
            BigDecimal valor,
            String pixCopiaCola,
            String pixQrBase64,
            String ticketUrl
    ) {}
}
