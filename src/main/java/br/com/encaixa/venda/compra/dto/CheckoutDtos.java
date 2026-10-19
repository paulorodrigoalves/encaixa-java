package br.com.encaixa.venda.compra.dto;

import br.com.encaixa.domain.engine.model.Modelos.PacoteEstimado;
import br.com.encaixa.venda.cliente.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CheckoutDtos {

    private CheckoutDtos() {}

    public record OpcaoFreteDTO(
            String transportadora,
            BigDecimal valorFrete,
            int prazoDias
    ) {}

    public record CotacaoFreteResponse(
            UUID compraId,
            int totalItens,
            BigDecimal subtotalProjetos,
            PacoteEstimado pacote,
            List<OpcaoFreteDTO> opcoesFrete,
            String telefoneAtual,
            Endereco enderecoAtual
    ) {}

    public record FinalizarCheckoutRequest(
            @NotBlank String telefone,
            @NotNull Endereco endereco,
            @NotBlank String transportadora,
            @NotNull BigDecimal valorFrete,
            @NotNull Integer prazoFreteDias
    ) {}
}
