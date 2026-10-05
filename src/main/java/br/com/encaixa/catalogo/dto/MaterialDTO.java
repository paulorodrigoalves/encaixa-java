package br.com.encaixa.catalogo.dto;

import br.com.encaixa.catalogo.material.Material;

import java.math.BigDecimal;
import java.util.UUID;

public record MaterialDTO(
        UUID id,
        String nome,
        String cor,
        BigDecimal valorFixoAdicional,
        UUID tipoMaterialId,
        String tipoMaterialNome,
        int espessuraMm,
        int margemMm
) {
    public static MaterialDTO from(Material m) {
        return new MaterialDTO(
                m.getId(),
                m.getNome(),
                m.getCor(),
                m.getValorFixoAdicional(),
                m.getTipoMaterial().getId(),
                m.getTipoMaterial().getNome(),
                m.getTipoMaterial().getEspessuraMm(),
                m.getTipoMaterial().getMargemMm()
        );
    }
}
