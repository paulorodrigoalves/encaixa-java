package br.com.encaixa.catalogo.dto;

import br.com.encaixa.catalogo.tipoobjeto.TipoObjeto;

import java.util.UUID;

public record TipoObjetoDTO(
        UUID id,
        String nome,
        String categoria,
        int larguraMinMm,
        int larguraMaxMm,
        int profundidadeMinMm,
        int profundidadeMaxMm,
        Integer alturaMinMm
) {
    public static TipoObjetoDTO from(TipoObjeto to) {
        return new TipoObjetoDTO(
                to.getId(),
                to.getNome(),
                to.getCategoria(),
                to.getLarguraMinMm(),
                to.getLarguraMaxMm(),
                to.getProfundidadeMinMm(),
                to.getProfundidadeMaxMm(),
                to.getAlturaMinMm()
        );
    }
}

