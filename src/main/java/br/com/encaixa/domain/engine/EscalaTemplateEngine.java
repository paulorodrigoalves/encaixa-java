package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.ColunaTemplate;
import br.com.encaixa.domain.engine.model.Modelos.Compartimento;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.LayoutProporcional;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.ResultadoEscala;
import br.com.encaixa.domain.engine.model.Modelos.SubdivisaoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Escala um layout proporcional de template para as medidas reais de uma gaveta.
 * Porta 1:1 de {@code packages/core/src/template/escalarTemplate.ts}.
 *
 * <p>Modelo fisico (confirmado com exemplo real de producao):
 * <ul>
 *   <li>A margem total e descontada uma unica vez da medida da gaveta (nao dobrada por lado)
 *       e dividida ao meio pra centralizar o organizador.</li>
 *   <li>Toda parede do organizador conta como divisoria de verdade, inclusive as bordas
 *       externas — N compartimentos numa direcao = N+1 divisorias.</li>
 * </ul>
 *
 * <p>Exemplo de referencia: gaveta 300x300mm, margem 5mm, espessura 6mm, 5 subdivisoes iguais
 * numa coluna -> 6 divisorias de 6mm (36mm) + 5 compartimentos de 51,8mm (259mm) = 295mm.
 *
 * <p>Classe sem estado e sem dependencias: pode ser usada direto ou registrada como bean.
 */
public final class EscalaTemplateEngine {

    public ResultadoEscala escalar(LayoutProporcional template, MedidasGaveta gaveta, ConfigEscala config) {
        final double espessura = config.espessuraMaterialMm();
        final double margem = config.margemTotalMm();
        final double margemLado = margem / 2;

        final double larguraUtil = gaveta.larguraMm() - margem;
        final double profundidadeUtil = gaveta.profundidadeMm() - margem;

        if (larguraUtil <= 0 || profundidadeUtil <= 0) {
            throw new LayoutInvalidoException(
                    "Medidas da gaveta insuficientes apos aplicar a margem de encaixe (%smm). Largura util: %smm, Profundidade util: %smm"
                            .formatted(fmt(margem), fmt(larguraUtil), fmt(profundidadeUtil)));
        }

        List<ColunaTemplate> colunas = template == null || template.colunas() == null ? List.of() : template.colunas();
        if (colunas.isEmpty()) {
            throw new LayoutInvalidoException("Template invalido: deve ter ao menos uma coluna.");
        }

        // N colunas -> N+1 divisorias verticais (as duas paredes externas contam).
        final int nDivisoriasVerticais = colunas.size() + 1;
        final double larguraDisponivel = larguraUtil - nDivisoriasVerticais * espessura;
        if (larguraDisponivel <= 0) {
            throw new LayoutInvalidoException(
                    "Divisorias verticais (%d x %smm) excedem a largura util (%smm). Reduza o numero de colunas ou use uma gaveta mais larga."
                            .formatted(nDivisoriasVerticais, fmt(espessura), fmt(larguraUtil)));
        }

        List<Compartimento> compartimentos = new ArrayList<>();
        // Pula a margem e a parede externa esquerda antes do primeiro compartimento.
        double cursorX = margemLado + espessura;

        for (int ci = 0; ci < colunas.size(); ci++) {
            ColunaTemplate coluna = colunas.get(ci);
            double larguraColuna = coluna.proporcao() * larguraDisponivel;
            if (larguraColuna <= 0) {
                throw new LayoutInvalidoException(
                        "Coluna %d tem largura invalida (%smm). Verifique as proporcoes do template."
                                .formatted(ci, fmt(larguraColuna)));
            }

            List<SubdivisaoTemplate> subdivisoes = coluna.subdivisoes() == null ? List.of() : coluna.subdivisoes();
            if (subdivisoes.isEmpty()) {
                throw new LayoutInvalidoException(
                        "Coluna %d nao tem subdivisoes. Cada coluna deve ter ao menos uma subdivisao.".formatted(ci));
            }

            // N subdivisoes -> N+1 divisorias horizontais (idem, paredes externas incluidas).
            int nDivisoriasHorizontais = subdivisoes.size() + 1;
            double profundidadeDisponivel = profundidadeUtil - nDivisoriasHorizontais * espessura;
            if (profundidadeDisponivel <= 0) {
                throw new LayoutInvalidoException(
                        "Divisorias horizontais na coluna %d (%d x %smm) excedem a profundidade util (%smm). Reduza o numero de subdivisoes ou use uma gaveta mais funda."
                                .formatted(ci, nDivisoriasHorizontais, fmt(espessura), fmt(profundidadeUtil)));
            }

            double cursorY = margemLado + espessura;
            for (int si = 0; si < subdivisoes.size(); si++) {
                double profundidadeSub = subdivisoes.get(si).proporcao() * profundidadeDisponivel;
                if (profundidadeSub <= 0) {
                    throw new LayoutInvalidoException(
                            "Subdivisao %d da coluna %d tem profundidade invalida (%smm). Verifique as proporcoes do template."
                                    .formatted(si, ci, fmt(profundidadeSub)));
                }

                double area = Geometria.calcularAreaPaineis(larguraColuna, profundidadeSub, gaveta.alturaMm());
                compartimentos.add(new Compartimento(
                        Geometria.roundHalfUp(cursorX),
                        Geometria.roundHalfUp(cursorY),
                        Geometria.roundHalfUp(larguraColuna),
                        Geometria.roundHalfUp(profundidadeSub),
                        Geometria.round2(area)));

                // Sempre soma a divisoria seguinte (interna, ou a parede final de fechamento).
                cursorY += profundidadeSub + espessura;
            }
            cursorX += larguraColuna + espessura;
        }

        return new ResultadoEscala(List.copyOf(compartimentos), larguraUtil, profundidadeUtil);
    }

    private static String fmt(double v) {
        return Geometria.fmt(v);
    }
}
