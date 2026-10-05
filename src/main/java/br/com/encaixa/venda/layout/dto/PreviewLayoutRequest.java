package br.com.encaixa.venda.layout.dto;

import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PreviewLayoutRequest(
        @NotNull @Min(50) @Max(1200) Integer larguraMm,
        @NotNull @Min(50) @Max(1200) Integer profundidadeMm,
        @NotNull @Min(30) @Max(300) Integer alturaMm,
        @NotNull UUID materialId,
        UUID templateId,
        List<ItemObjetoSelecionado> itensObjeto
) {}
