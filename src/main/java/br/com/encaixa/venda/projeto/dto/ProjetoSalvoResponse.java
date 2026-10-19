package br.com.encaixa.venda.projeto.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjetoSalvoResponse(
        UUID id,
        UUID compraId,
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
        OffsetDateTime criadoEm
) {}
