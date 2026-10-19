package br.com.encaixa.venda.projeto.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarProjetoRequest(
        @NotNull @Min(50) @Max(1200) Integer larguraMm,
        @NotNull @Min(50) @Max(1200) Integer profundidadeMm,
        @NotNull @Min(30) @Max(300) Integer alturaMm,
        @NotNull UUID materialId,
        @NotNull UUID templateId,
        Integer quantidade
) {}
