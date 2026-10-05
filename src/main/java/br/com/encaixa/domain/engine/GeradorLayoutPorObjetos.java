package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.Compartimento;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.ResultadoEscala;
import br.com.encaixa.domain.engine.model.Modelos.TipoObjetoConstraint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta um layout de compartimentos (uma unica camada) a partir da lista de objetos que o
 * cliente quer guardar, usando as faixas min/max de cada tipo de objeto.
 * Porta 1:1 de {@code packages/core/src/layout/gerarLayoutPorObjetos.ts}.
 *
 * <p>Estrategia "next-fit decreasing": itens ordenados do mais largo pro mais estreito e
 * agrupados em colunas; cada coluna empilha objetos na profundidade enquanto a largura deles
 * for compativel. Depois de encaixar tudo no minimo, a sobra (largura entre colunas,
 * profundidade dentro de cada coluna) e distribuida ate o maximo de cada objeto (max-min fairness).
 */
public final class GeradorLayoutPorObjetos {

    private static final class ColunaEmMontagem {
        double larguraMin;
        double larguraMax;
        final List<TipoObjetoConstraint> itens = new ArrayList<>();
        double profundidadeUsada;
    }

    public ResultadoEscala gerar(MedidasGaveta gaveta,
                                 List<ItemObjetoSelecionado> itensSelecionados,
                                 List<TipoObjetoConstraint> catalogo,
                                 ConfigEscala config) {
        final double espessura = config.espessuraMaterialMm();
        final double margem = config.margemTotalMm();
        final double margemLado = margem / 2;
        final Map<UUID, TipoObjetoConstraint> mapa = catalogo.stream()
                .collect(Collectors.toMap(TipoObjetoConstraint::id, Function.identity(), (a, b) -> a));

        final double larguraUtil = gaveta.larguraMm() - margem;
        final double profundidadeUtil = gaveta.profundidadeMm() - margem;
        if (larguraUtil <= 0 || profundidadeUtil <= 0) {
            throw new LayoutInvalidoException(
                    "Medidas da gaveta insuficientes apos aplicar a margem de encaixe (%smm). Largura util: %smm, Profundidade util: %smm"
                            .formatted(Geometria.fmt(margem), Geometria.fmt(larguraUtil), Geometria.fmt(profundidadeUtil)));
        }

        // 1. resolve e valida os objetos escolhidos
        List<TipoObjetoConstraint> unidades = new ArrayList<>();
        for (ItemObjetoSelecionado item : itensSelecionados == null ? List.<ItemObjetoSelecionado>of() : itensSelecionados) {
            if (item.quantidade() <= 0) {
                continue;
            }
            TipoObjetoConstraint tipo = mapa.get(item.tipoObjetoId());
            if (tipo == null) {
                throw new LayoutInvalidoException("Tipo de objeto \"%s\" não encontrado no catálogo.".formatted(item.tipoObjetoId()));
            }
            if (tipo.alturaMinMm() != null && tipo.alturaMinMm() > gaveta.alturaMm()) {
                throw new LayoutInvalidoException(
                        "O objeto \"%s\" precisa de uma gaveta com pelo menos %dmm de altura (a informada tem %dmm)."
                                .formatted(tipo.nome(), tipo.alturaMinMm(), gaveta.alturaMm()));
            }
            for (int i = 0; i < item.quantidade(); i++) {
                unidades.add(tipo);
            }
        }
        if (unidades.isEmpty()) {
            throw new LayoutInvalidoException("Selecione ao menos um objeto para guardar.");
        }

        // 2. ordena do mais largo pro mais estreito (sort estavel, como no JS)
        unidades.sort(Comparator.comparingInt(TipoObjetoConstraint::larguraMinMm).reversed());

        // 3. agrupa em colunas. N itens numa coluna = N+1 divisorias: o primeiro item custa as 2
        //    paredes (abertura + fechamento); cada item seguinte custa so mais 1.
        List<ColunaEmMontagem> colunas = new ArrayList<>();
        for (TipoObjetoConstraint unidade : unidades) {
            ColunaEmMontagem atual = colunas.isEmpty() ? null : colunas.getLast();
            double espacoNecessario = atual != null && !atual.itens.isEmpty()
                    ? espessura + unidade.profundidadeMinMm()
                    : 2 * espessura + unidade.profundidadeMinMm();

            boolean compativel = atual != null
                    && unidade.larguraMinMm() <= atual.larguraMax
                    && atual.larguraMin <= unidade.larguraMaxMm()
                    && atual.profundidadeUsada + espacoNecessario <= profundidadeUtil;

            if (compativel) {
                atual.itens.add(unidade);
                atual.profundidadeUsada += espacoNecessario;
                atual.larguraMin = Math.max(atual.larguraMin, unidade.larguraMinMm());
                atual.larguraMax = Math.min(atual.larguraMax, unidade.larguraMaxMm());
            } else {
                ColunaEmMontagem nova = new ColunaEmMontagem();
                nova.larguraMin = unidade.larguraMinMm();
                nova.larguraMax = unidade.larguraMaxMm();
                nova.itens.add(unidade);
                nova.profundidadeUsada = 2 * espessura + unidade.profundidadeMinMm();
                colunas.add(nova);
            }
        }

        // 4. confere se cabe no minimo
        int nDivisoriasV = colunas.size() + 1;
        double larguraTotalMin = colunas.stream().mapToDouble(c -> c.larguraMin).sum() + nDivisoriasV * espessura;
        if (larguraTotalMin > larguraUtil) {
            throw new LayoutInvalidoException(
                    "Os objetos escolhidos precisam de pelo menos %dmm de largura útil, mas a gaveta informada só tem %dmm. Remova algum item ou use uma gaveta mais larga."
                            .formatted((int) Math.ceil(larguraTotalMin), Geometria.roundHalfUp(larguraUtil)));
        }

        // 5. distribui a sobra de largura entre as colunas, ate o teto de cada uma
        double[] larguraFinal = distribuirSobra(
                colunas.stream().mapToDouble(c -> c.larguraMin).toArray(),
                colunas.stream().mapToDouble(c -> c.larguraMax).toArray(),
                larguraUtil - larguraTotalMin);

        // 6. monta compartimentos distribuindo a sobra de profundidade dentro de cada coluna
        List<Compartimento> compartimentos = new ArrayList<>();
        double cursorX = margemLado + espessura;

        for (int ci = 0; ci < colunas.size(); ci++) {
            ColunaEmMontagem coluna = colunas.get(ci);
            int nDivisoriasH = coluna.itens.size() + 1;
            double profundidadeMinColuna = coluna.itens.stream().mapToDouble(TipoObjetoConstraint::profundidadeMinMm).sum()
                    + nDivisoriasH * espessura;

            double[] profundidades = distribuirSobra(
                    coluna.itens.stream().mapToDouble(TipoObjetoConstraint::profundidadeMinMm).toArray(),
                    coluna.itens.stream().mapToDouble(TipoObjetoConstraint::profundidadeMaxMm).toArray(),
                    profundidadeUtil - profundidadeMinColuna);

            double cursorY = margemLado + espessura;
            for (double profundidade : profundidades) {
                double area = Geometria.calcularAreaPaineis(larguraFinal[ci], profundidade, gaveta.alturaMm());
                compartimentos.add(new Compartimento(
                        Geometria.roundHalfUp(cursorX),
                        Geometria.roundHalfUp(cursorY),
                        Geometria.roundHalfUp(larguraFinal[ci]),
                        Geometria.roundHalfUp(profundidade),
                        Geometria.round2(area)));
                cursorY += profundidade + espessura;
            }
            cursorX += larguraFinal[ci] + espessura;
        }

        return new ResultadoEscala(List.copyOf(compartimentos), larguraUtil, profundidadeUtil);
    }

    /**
     * Cresce cada valor em direcao ao seu teto, em rounds, distribuindo a sobra igualmente
     * entre os que ainda nao bateram no teto (max-min fairness).
     */
    static double[] distribuirSobra(double[] bases, double[] tetos, double sobraInicial) {
        double[] valores = bases.clone();
        double sobra = sobraInicial;
        int rounds = valores.length + 5; // limite de seguranca contra sobras residuais
        while (sobra > 1e-6 && rounds-- > 0) {
            List<Integer> abertos = new ArrayList<>();
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] < tetos[i]) {
                    abertos.add(i);
                }
            }
            if (abertos.isEmpty()) {
                break;
            }
            double fatia = sobra / abertos.size();
            double usado = 0;
            for (int i : abertos) {
                double crescimento = Math.min(fatia, tetos[i] - valores[i]);
                valores[i] += crescimento;
                usado += crescimento;
            }
            if (usado < 1e-6) {
                break;
            }
            sobra -= usado;
        }
        return valores;
    }
}
