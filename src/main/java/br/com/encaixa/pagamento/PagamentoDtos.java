package br.com.encaixa.pagamento;

import java.math.BigDecimal;
import java.util.UUID;

public final class PagamentoDtos {

    private PagamentoDtos() {}

    public record PagamentoResponse(
            UUID pagamentoId,
            UUID compraId,
            String metodoPagamento,
            Integer parcelas,
            String status,
            BigDecimal valor,
            String pixCopiaCola,
            String pixQrBase64,
            String ticketUrl,
            boolean modoSimulacao
    ) {}

    public record PagamentoCartaoRequest(
            String numeroCartao,
            String nomeTitular,
            String validade,
            String cvv,
            Integer parcelas
    ) {}
}
