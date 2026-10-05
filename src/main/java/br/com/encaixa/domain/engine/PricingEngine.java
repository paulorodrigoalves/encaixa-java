package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.Compartimento;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Calcula o orcamento de UM organizador. Porta de {@code packages/core/src/pricing/PricingEngine.ts}.
 *
 * <pre>
 * total = preco_por_litro x volume_litros
 *       + preco_por_cm2 x soma(area_paineis dos compartimentos)
 *       + material.valor_fixo_adicional
 *       + frete
 * </pre>
 *
 * Funcao pura: recebe a regra vigente ja resolvida (o service busca no banco), o que
 * dispensa a interface de repositorio do legado e deixa o teste trivial.
 */
public final class PricingEngine {

    /** Valores da regra de precificacao vigente no momento do calculo. */
    public record RegraVigente(UUID id, BigDecimal precoPorLitro, BigDecimal precoPorCm2Divisoria) {
    }

    public record EntradaOrcamento(MedidasGaveta gaveta,
                                   List<Compartimento> compartimentos,
                                   BigDecimal valorFixoMaterial,
                                   BigDecimal frete) {
    }

    public record ResultadoOrcamento(BigDecimal volumeTotalLitros,
                                     BigDecimal areaDivisoriasCm2,
                                     BigDecimal valorVolume,
                                     BigDecimal valorDivisorias,
                                     BigDecimal valorMaterial,
                                     BigDecimal valorFrete,
                                     BigDecimal valorTotal,
                                     UUID regraPrecificacaoId) {
    }

    public ResultadoOrcamento calcular(EntradaOrcamento entrada, RegraVigente regra) {
        Objects.requireNonNull(regra, "Nenhuma regra de precificacao vigente.");
        MedidasGaveta g = entrada.gaveta();

        // mm³ -> litros (divisao exata por 10^6)
        BigDecimal volumeLitros = BigDecimal.valueOf((long) g.larguraMm() * g.profundidadeMm() * g.alturaMm())
                .movePointLeft(6);

        BigDecimal areaDivisorias = entrada.compartimentos().stream()
                .map(c -> BigDecimal.valueOf(c.areaPaineisCm2()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorVolume = money(regra.precoPorLitro().multiply(volumeLitros));
        BigDecimal valorDivisorias = money(regra.precoPorCm2Divisoria().multiply(areaDivisorias));
        BigDecimal valorMaterial = money(nz(entrada.valorFixoMaterial()));
        BigDecimal valorFrete = money(nz(entrada.frete()));
        BigDecimal valorTotal = money(valorVolume.add(valorDivisorias).add(valorMaterial).add(valorFrete));

        return new ResultadoOrcamento(
                volumeLitros.setScale(3, RoundingMode.HALF_UP),
                areaDivisorias.setScale(2, RoundingMode.HALF_UP),
                valorVolume, valorDivisorias, valorMaterial, valorFrete, valorTotal,
                regra.id());
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
