package br.com.encaixa.venda.layout.dto;

import java.util.List;

public record PreviewLayoutResponse(
        int larguraTotalMm,
        int profundidadeTotalMm,
        int alturaTotalMm,
        List<DivisoriaRenderDTO> divisorias,
        List<ObjetoPosicionadoDTO> objetosPosicionados,
        int espessuraDivisoriaMm
) {
    public record DivisoriaRenderDTO(int xMm, int yMm, int larguraMm, int profundidadeMm, String orientacao) {}
    public record ObjetoPosicionadoDTO(ObjetoRenderDTO objeto, int xMm, int yMm, int larguraRealMm, int profundidadeRealMm) {}
    public record ObjetoRenderDTO(String id, String nome, String corHex) {}
}
