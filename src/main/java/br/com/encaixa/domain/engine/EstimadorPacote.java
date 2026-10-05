package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.PacoteEstimado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Estima dimensoes e peso da caixa de envio. Porta de {@code apps/api/src/frete/estimarPacote.ts}.
 *
 * <p>Peso proporcional ao volume, calibrado com a referencia real ja embalada:
 * 50x40x6cm (12L) ~ 3kg => 0,25 kg/litro.
 *
 * <p>Uma compra com varios organizadores vai num pacote so: largura e profundidade do maior,
 * alturas somadas (empilhados) e pesos somados.
 */
public final class EstimadorPacote {

    static final int MARGEM_EMBALAGEM_MM = 40;
    static final double DENSIDADE_KG_POR_LITRO = 0.25;
    static final int CM_MIN = 11;
    static final double PESO_MIN_KG = 0.3;

    public record ItemPacote(MedidasGaveta medidas, int quantidade) {
    }

    public PacoteEstimado estimarCompra(List<ItemPacote> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Pacote sem itens.");
        }
        int largura = itens.stream().mapToInt(i -> i.medidas().larguraMm()).max().orElseThrow();
        int profundidade = itens.stream().mapToInt(i -> i.medidas().profundidadeMm()).max().orElseThrow();
        int altura = itens.stream().mapToInt(i -> i.medidas().alturaMm() * i.quantidade()).sum();
        double volumeLitros = itens.stream()
                .mapToDouble(i -> ((double) i.medidas().larguraMm() * i.medidas().profundidadeMm() * i.medidas().alturaMm())
                        / 1_000_000 * i.quantidade())
                .sum();

        return new PacoteEstimado(
                cm(largura),
                cm(altura),
                cm(profundidade),
                Math.max(PESO_MIN_KG, BigDecimal.valueOf(volumeLitros * DENSIDADE_KG_POR_LITRO)
                        .setScale(3, RoundingMode.HALF_UP).doubleValue()));
    }

    private static int cm(int mm) {
        return Math.max(CM_MIN, (int) Math.ceil((mm + MARGEM_EMBALAGEM_MM) / 10.0));
    }
}
