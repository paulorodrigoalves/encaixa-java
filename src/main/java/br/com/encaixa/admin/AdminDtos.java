package br.com.encaixa.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {}

    public record RegraPrecificacaoDTO(
            UUID id,
            BigDecimal precoPorLitro,
            BigDecimal precoPorCm2Divisoria,
            OffsetDateTime vigenteDesde,
            String criadoPor
    ) {}

    public record AtualizarRegraPrecoRequest(
            @NotNull @DecimalMin("0.01") BigDecimal precoPorLitro,
            @NotNull @DecimalMin("0.01") BigDecimal precoPorCm2Divisoria
    ) {}

    public record EtapaProducaoDTO(
            UUID id,
            String nome,
            Integer ordem,
            boolean usaRastreio
    ) {}

    public record PedidoEtapaHistoricoDTO(
            UUID id,
            String nomeEtapa,
            String observacao,
            OffsetDateTime criadoEm
    ) {}

    public record CompraAdminDTO(
            UUID compraId,
            String clienteNome,
            String clienteEmail,
            String statusCompra,
            BigDecimal valorTotal,
            int totalProjetos,
            OffsetDateTime criadoEm,
            UUID pedidoId,
            String etapaAtual,
            String codigoRastreio,
            List<PedidoEtapaHistoricoDTO> historicoEtapas
    ) {}

    public record RegistrarEtapaRequest(
            @NotNull UUID etapaId,
            String observacao,
            String codigoRastreio
    ) {}
}
