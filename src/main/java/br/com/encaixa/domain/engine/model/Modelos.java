package br.com.encaixa.domain.engine.model;

import java.util.List;

/**
 * Modelo de valor do dominio (sem dependencia de Spring/JPA) — equivalente ao
 * {@code packages/core/src/types.ts} do legado.
 */
public final class Modelos {

    private Modelos() {
    }

    /** Medidas internas reais da gaveta informadas pelo cliente. */
    public record MedidasGaveta(int larguraMm, int profundidadeMm, int alturaMm) {
    }

    /**
     * Configuracao fisica usada no calculo do layout.
     *
     * @param espessuraMaterialMm espessura de UMA divisoria/parede pronta (madeira + revestimento
     *                            nas duas faces). Vem do tipo de material.
     * @param margemTotalMm       folga TOTAL entre gaveta e organizador — descontada uma unica vez
     *                            da medida da gaveta; metade fica de cada lado (organizador centralizado).
     */
    public record ConfigEscala(double espessuraMaterialMm, double margemTotalMm) {
    }

    /** Compartimento (vao interno) ja posicionado, com a area de paineis usada no preco. */
    public record Compartimento(int posXMm, int posYMm, int larguraMm, int profundidadeMm, double areaPaineisCm2) {
    }

    /** Resultado de qualquer gerador de layout. */
    public record ResultadoEscala(List<Compartimento> compartimentos, double larguraUtilMm, double profundidadeUtilMm) {
    }

    // ── Template proporcional (JSONB templates.layout_proporcional) ─────────

    public record SubdivisaoTemplate(double proporcao) {
    }

    public record ColunaTemplate(double proporcao, List<SubdivisaoTemplate> subdivisoes) {
    }

    public record LayoutProporcional(List<ColunaTemplate> colunas) {
    }

    // ── Layout por objetos ──────────────────────────────────────────────────

    /** Faixa de medidas de compartimento exigida por um tipo de objeto. */
    public record TipoObjetoConstraint(
            java.util.UUID id,
            String nome,
            int larguraMinMm,
            int larguraMaxMm,
            int profundidadeMinMm,
            int profundidadeMaxMm,
            Integer alturaMinMm) {
    }

    /** Um tipo de objeto escolhido pelo cliente, com quantidade (JSONB projetos.itens_objeto). */
    public record ItemObjetoSelecionado(java.util.UUID tipoObjetoId, int quantidade) {
    }

    // ── Frete ───────────────────────────────────────────────────────────────

    public record PacoteEstimado(int larguraCm, int alturaCm, int profundidadeCm, double pesoKg) {
    }
}

