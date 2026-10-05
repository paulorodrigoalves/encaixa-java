package br.com.encaixa.domain.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Funcoes puras de geometria/arredondamento compartilhadas pelos motores. */
public final class Geometria {

    private Geometria() {
    }

    /**
     * Area de paineis de divisoria usada por um compartimento, em cm²:
     * painel lateral (profundidade x altura) + painel frontal (largura x altura).
     */
    public static double calcularAreaPaineis(double larguraMm, double profundidadeMm, double alturaMm) {
        double painelLateralCm2 = (profundidadeMm * alturaMm) / 100;
        double painelFrontalCm2 = (larguraMm * alturaMm) / 100;
        return painelLateralCm2 + painelFrontalCm2;
    }

    /** Equivalente a {@code Math.round} do JavaScript para valores positivos (half-up). */
    public static int roundHalfUp(double v) {
        return (int) Math.floor(v + 0.5);
    }

    public static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static String fmt(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
