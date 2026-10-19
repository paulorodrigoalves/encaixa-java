package br.com.encaixa.venda.projeto.dto;

import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse.DivisoriaRenderDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProjetoSalvoResponse(
        UUID id,
        UUID compraId,
        String statusCompra,
        Integer larguraMm,
        Integer profundidadeMm,
        Integer alturaMm,
        Integer espessuraMaterialMm,
        Integer margemSegurancaMm,
        Integer quantidade,
        String status,
        String materialNome,
        String templateNome,
        int totalCompartimentos,
        BigDecimal valorTotal,
        String transportadora,
        BigDecimal valorFrete,
        Integer prazoFreteDias,
        OffsetDateTime criadoEm,
        String etapaAtual,
        String codigoRastreio,
        Integer prazoEstimadoDias,
        List<EtapaClienteDTO> historicoEtapas,
        List<DivisoriaRenderDTO> divisorias
) {
    public record EtapaClienteDTO(
            String nomeEtapa,
            String observacao,
            OffsetDateTime criadoEm
    ) {}
}
