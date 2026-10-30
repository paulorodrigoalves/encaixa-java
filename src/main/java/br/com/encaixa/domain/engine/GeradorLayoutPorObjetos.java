package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.LayoutNaoCabeException.CamadaSugerida;
import br.com.encaixa.domain.engine.ResultadoLayoutObjetos.ObjetoColocado;
import br.com.encaixa.domain.engine.model.Modelos.Compartimento;
import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.ResultadoEscala;
import br.com.encaixa.domain.engine.model.Modelos.TipoObjetoConstraint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta um layout de compartimentos (uma unica camada) a partir da lista de objetos que o
 * cliente quer guardar, usando as faixas min/max de cada tipo de objeto.
 *
 * <p>Estrategia "next-fit decreasing": itens ordenados do mais largo pro mais estreito e
 * agrupados em colunas; cada coluna empilha objetos na profundidade enquanto a largura deles
 * for compativel. Depois de encaixar tudo no minimo, a sobra e distribuida ate o maximo de cada
 * objeto (max-min fairness) e, se ainda sobrar, repartida igualmente: o organizador sempre
 * ocupa a gaveta inteira (feito sob medida) e cada objeto informa a folga que ficou.
 *
 * <p>Alem da disposicao em colunas, tenta o mesmo algoritmo com a gaveta transposta (linhas) e com
 * os objetos girados 90 graus ({@link EstrategiaLayout}). Se nada couber, calcula sugestoes
 * (medida minima de gaveta e divisao em duas camadas) em {@link LayoutNaoCabeException}.
 */
public final class GeradorLayoutPorObjetos {

    private static final int ALTURA_MINIMA_CAMADA_MM = 30;
    private static final List<EstrategiaLayout> ORDEM_AUTO = List.of(
            EstrategiaLayout.COLUNAS, EstrategiaLayout.LINHAS,
            EstrategiaLayout.COLUNAS_GIRADO, EstrategiaLayout.LINHAS_GIRADO);

    /** Objeto com as dimensoes ja no referencial do empacotador (apos girar/transpor). */
    private record Un(TipoObjetoConstraint tipo, int wMin, int wMax, int dMin, int dMax) {}

    private record Pos(Un un, int x, int y, int w, int d) {}

    private static final class Coluna {
        double larguraMin;
        double larguraMax;
        final List<Un> itens = new ArrayList<>();
        double profundidadeUsada;
    }

    /** Falha de encaixe de uma estrategia; guarda quanto a gaveta precisaria ter em cada eixo real. */
    private static final class Falha extends LayoutInvalidoException {
        final int larguraNecessariaMm;
        final int profundidadeNecessariaMm;

        Falha(String msg, int larguraNecessariaMm, int profundidadeNecessariaMm) {
            super(msg);
            this.larguraNecessariaMm = larguraNecessariaMm;
            this.profundidadeNecessariaMm = profundidadeNecessariaMm;
        }
    }

    /** Compatibilidade: devolve so os compartimentos da melhor disposicao (AUTO). */
    public ResultadoEscala gerar(MedidasGaveta gaveta,
                                 List<ItemObjetoSelecionado> itensSelecionados,
                                 List<TipoObjetoConstraint> catalogo,
                                 ConfigEscala config) {
        return gerarDetalhado(gaveta, itensSelecionados, catalogo, config, EstrategiaLayout.AUTO).escala();
    }

    public ResultadoLayoutObjetos gerarDetalhado(MedidasGaveta gaveta,
                                                 List<ItemObjetoSelecionado> itensSelecionados,
                                                 List<TipoObjetoConstraint> catalogo,
                                                 ConfigEscala config,
                                                 EstrategiaLayout pedida) {
        validarGaveta(gaveta, config);
        List<TipoObjetoConstraint> unidades = resolverUnidades(gaveta, itensSelecionados, catalogo);

        EstrategiaLayout alvo = pedida == null ? EstrategiaLayout.AUTO : pedida;
        List<EstrategiaLayout> ordem = alvo == EstrategiaLayout.AUTO ? ORDEM_AUTO : List.of(alvo);

        Map<EstrategiaLayout, Falha> falhas = new LinkedHashMap<>();
        for (EstrategiaLayout e : ordem) {
            try {
                return tentar(gaveta, unidades, config, e);
            } catch (Falha f) {
                falhas.put(e, f);
            }
        }

        if (alvo != EstrategiaLayout.AUTO) {
            throw falhas.get(alvo);
        }
        throw montarErroNaoCabe(gaveta, unidades, config, falhas);
    }

    /** Disposicoes distintas que funcionam para estes objetos (layouts identicos aparecem uma vez so). */
    public List<EstrategiaLayout> estrategiasViaveis(MedidasGaveta gaveta,
                                                     List<ItemObjetoSelecionado> itensSelecionados,
                                                     List<TipoObjetoConstraint> catalogo,
                                                     ConfigEscala config) {
        validarGaveta(gaveta, config);
        List<TipoObjetoConstraint> unidades = resolverUnidades(gaveta, itensSelecionados, catalogo);
        List<EstrategiaLayout> viaveis = new ArrayList<>();
        List<List<Compartimento>> vistos = new ArrayList<>();
        for (EstrategiaLayout e : ORDEM_AUTO) {
            try {
                var r = tentar(gaveta, unidades, config, e);
                if (!vistos.contains(r.escala().compartimentos())) {
                    vistos.add(r.escala().compartimentos());
                    viaveis.add(e);
                }
            } catch (Falha ignorada) {
                // estrategia inviavel: simplesmente nao e oferecida
            }
        }
        return viaveis;
    }

    // ------------------------------------------------------------------ validacao

    private void validarGaveta(MedidasGaveta gaveta, ConfigEscala config) {
        double margem = config.margemTotalMm();
        double larguraUtil = gaveta.larguraMm() - margem;
        double profundidadeUtil = gaveta.profundidadeMm() - margem;
        if (larguraUtil <= 0 || profundidadeUtil <= 0) {
            throw new LayoutInvalidoException(
                    "Medidas da gaveta insuficientes apos aplicar a margem de encaixe (%smm). Largura util: %smm, Profundidade util: %smm"
                            .formatted(Geometria.fmt(margem), Geometria.fmt(larguraUtil), Geometria.fmt(profundidadeUtil)));
        }
    }

    private List<TipoObjetoConstraint> resolverUnidades(MedidasGaveta gaveta,
                                                        List<ItemObjetoSelecionado> itensSelecionados,
                                                        List<TipoObjetoConstraint> catalogo) {
        final Map<UUID, TipoObjetoConstraint> mapa = catalogo.stream()
                .collect(Collectors.toMap(TipoObjetoConstraint::id, Function.identity(), (a, b) -> a));

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
        return unidades;
    }

    // ------------------------------------------------------------------ uma estrategia

    private ResultadoLayoutObjetos tentar(MedidasGaveta gaveta, List<TipoObjetoConstraint> unidades,
                                          ConfigEscala config, EstrategiaLayout e) {
        final boolean transpor = e.transpor();
        final boolean girar = e.girar();
        final double espessura = config.espessuraMaterialMm();
        final double margem = config.margemTotalMm();

        double packLarguraUtil = (transpor ? gaveta.profundidadeMm() : gaveta.larguraMm()) - margem;
        double packProfundidadeUtil = (transpor ? gaveta.larguraMm() : gaveta.profundidadeMm()) - margem;

        List<Un> uns = new ArrayList<>();
        for (TipoObjetoConstraint t : unidades) {
            // dimensoes reais do objeto na disposicao (apos girar)
            int wMin = girar ? t.profundidadeMinMm() : t.larguraMinMm();
            int wMax = girar ? t.profundidadeMaxMm() : t.larguraMaxMm();
            int dMin = girar ? t.larguraMinMm() : t.profundidadeMinMm();
            int dMax = girar ? t.larguraMaxMm() : t.profundidadeMaxMm();
            uns.add(transpor ? new Un(t, dMin, dMax, wMin, wMax) : new Un(t, wMin, wMax, dMin, dMax));
        }
        // mais largo pro mais estreito (sort estavel)
        uns.sort(Comparator.comparingInt(Un::wMin).reversed());

        List<Pos> posicoes;
        try {
            posicoes = empacotar(packLarguraUtil, packProfundidadeUtil, espessura, margem, uns);
        } catch (FalhaPack f) {
            int larguraNec = transpor ? f.profundidadePackNecessariaMm : f.larguraPackNecessariaMm;
            int profundidadeNec = transpor ? f.larguraPackNecessariaMm : f.profundidadePackNecessariaMm;
            throw new Falha(mensagemFalha(gaveta, larguraNec, profundidadeNec), larguraNec, profundidadeNec);
        }

        List<Compartimento> compartimentos = new ArrayList<>();
        List<ObjetoColocado> colocados = new ArrayList<>();
        for (Pos p : posicoes) {
            int x = transpor ? p.y() : p.x();
            int y = transpor ? p.x() : p.y();
            int w = transpor ? p.d() : p.w();
            int d = transpor ? p.w() : p.d();
            Compartimento c = new Compartimento(x, y, w, d,
                    Geometria.round2(Geometria.calcularAreaPaineis(w, d, gaveta.alturaMm())));
            compartimentos.add(c);

            TipoObjetoConstraint t = p.un().tipo();
            int larguraMaxReal = girar ? t.profundidadeMaxMm() : t.larguraMaxMm();
            int profundidadeMaxReal = girar ? t.larguraMaxMm() : t.profundidadeMaxMm();
            colocados.add(new ObjetoColocado(t, c, girar,
                    Math.max(0, w - larguraMaxReal), Math.max(0, d - profundidadeMaxReal)));
        }

        var escala = new ResultadoEscala(List.copyOf(compartimentos),
                gaveta.larguraMm() - margem, gaveta.profundidadeMm() - margem);
        return new ResultadoLayoutObjetos(e, escala, List.copyOf(colocados));
    }

    private String mensagemFalha(MedidasGaveta gaveta, int larguraNec, int profundidadeNec) {
        if (larguraNec > 0 && profundidadeNec > 0) {
            return ("Os objetos escolhidos precisam de uma gaveta com pelo menos %dmm de largura e %dmm de profundidade "
                    + "(a informada tem %dmm x %dmm). Remova algum item ou use uma gaveta maior.")
                    .formatted(larguraNec, profundidadeNec, gaveta.larguraMm(), gaveta.profundidadeMm());
        }
        if (larguraNec > 0) {
            return ("Os objetos escolhidos precisam de uma gaveta com pelo menos %dmm de largura, mas a informada tem %dmm. "
                    + "Remova algum item ou use uma gaveta mais larga.").formatted(larguraNec, gaveta.larguraMm());
        }
        return ("Os objetos escolhidos precisam de uma gaveta com pelo menos %dmm de profundidade, mas a informada tem %dmm. "
                + "Remova algum item ou use uma gaveta mais funda.").formatted(profundidadeNec, gaveta.profundidadeMm());
    }

    // ------------------------------------------------------------------ empacotador (referencial "colunas")

    private static final class FalhaPack extends RuntimeException {
        final int larguraPackNecessariaMm;
        final int profundidadePackNecessariaMm;

        FalhaPack(int larguraPackNecessariaMm, int profundidadePackNecessariaMm) {
            super(null, null, false, false);
            this.larguraPackNecessariaMm = larguraPackNecessariaMm;
            this.profundidadePackNecessariaMm = profundidadePackNecessariaMm;
        }
    }

    private List<Pos> empacotar(double larguraUtil, double profundidadeUtil, double espessura, double margem,
                                List<Un> unidades) {
        final double margemLado = margem / 2;

        // agrupa em colunas. N itens numa coluna = N+1 divisorias: o primeiro item custa as 2
        // paredes (abertura + fechamento); cada item seguinte custa so mais 1.
        List<Coluna> colunas = new ArrayList<>();
        for (Un unidade : unidades) {
            Coluna atual = colunas.isEmpty() ? null : colunas.getLast();
            double espacoNecessario = atual != null && !atual.itens.isEmpty()
                    ? espessura + unidade.dMin()
                    : 2 * espessura + unidade.dMin();

            boolean compativel = atual != null
                    && unidade.wMin() <= atual.larguraMax
                    && atual.larguraMin <= unidade.wMax()
                    && atual.profundidadeUsada + espacoNecessario <= profundidadeUtil;

            if (compativel) {
                atual.itens.add(unidade);
                atual.profundidadeUsada += espacoNecessario;
                atual.larguraMin = Math.max(atual.larguraMin, unidade.wMin());
                atual.larguraMax = Math.min(atual.larguraMax, unidade.wMax());
            } else {
                Coluna nova = new Coluna();
                nova.larguraMin = unidade.wMin();
                nova.larguraMax = unidade.wMax();
                nova.itens.add(unidade);
                nova.profundidadeUsada = 2 * espessura + unidade.dMin();
                colunas.add(nova);
            }
        }

        // confere se cabe no minimo (largura somada e profundidade de cada coluna)
        int nDivisoriasV = colunas.size() + 1;
        double larguraTotalMin = colunas.stream().mapToDouble(c -> c.larguraMin).sum() + nDivisoriasV * espessura;
        double maiorProfundidade = colunas.stream().mapToDouble(c -> c.profundidadeUsada).max().orElse(0);
        boolean falhaLargura = larguraTotalMin > larguraUtil;
        boolean falhaProfundidade = maiorProfundidade > profundidadeUtil;
        if (falhaLargura || falhaProfundidade) {
            throw new FalhaPack(
                    falhaLargura ? (int) Math.ceil(larguraTotalMin + margem) : 0,
                    falhaProfundidade ? (int) Math.ceil(maiorProfundidade + margem) : 0);
        }

        // distribui a sobra de largura entre as colunas, ate o teto de cada uma; o que passar do teto
        // e repartido igualmente para o organizador fechar com a gaveta
        double[] bases = colunas.stream().mapToDouble(c -> c.larguraMin).toArray();
        double[] larguraFinal = distribuirTudo(bases,
                colunas.stream().mapToDouble(c -> c.larguraMax).toArray(),
                larguraUtil - larguraTotalMin);

        List<Pos> posicoes = new ArrayList<>();
        double cursorX = margemLado + espessura;

        for (int ci = 0; ci < colunas.size(); ci++) {
            Coluna coluna = colunas.get(ci);
            int nDivisoriasH = coluna.itens.size() + 1;
            double profundidadeMinColuna = coluna.itens.stream().mapToDouble(Un::dMin).sum() + nDivisoriasH * espessura;

            double[] profundidades = distribuirTudo(
                    coluna.itens.stream().mapToDouble(Un::dMin).toArray(),
                    coluna.itens.stream().mapToDouble(Un::dMax).toArray(),
                    profundidadeUtil - profundidadeMinColuna);

            double cursorY = margemLado + espessura;
            for (int i = 0; i < profundidades.length; i++) {
                posicoes.add(new Pos(coluna.itens.get(i),
                        Geometria.roundHalfUp(cursorX),
                        Geometria.roundHalfUp(cursorY),
                        Geometria.roundHalfUp(larguraFinal[ci]),
                        Geometria.roundHalfUp(profundidades[i])));
                cursorY += profundidades[i] + espessura;
            }
            cursorX += larguraFinal[ci] + espessura;
        }
        return posicoes;
    }

    /** distribuirSobra ate os tetos + reparte igualmente o excedente que sobrar acima dos tetos. */
    private static double[] distribuirTudo(double[] bases, double[] tetos, double sobra) {
        double[] valores = distribuirSobra(bases, tetos, sobra);
        double usado = 0;
        for (int i = 0; i < valores.length; i++) {
            usado += valores[i] - bases[i];
        }
        double residual = sobra - usado;
        if (residual > 1e-6) {
            for (int i = 0; i < valores.length; i++) {
                valores[i] += residual / valores.length;
            }
        }
        return valores;
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

    // ------------------------------------------------------------------ sugestoes quando nao cabe

    private LayoutNaoCabeException montarErroNaoCabe(MedidasGaveta gaveta, List<TipoObjetoConstraint> unidades,
                                                     ConfigEscala config, Map<EstrategiaLayout, Falha> falhas) {
        String mensagem = falhas.values().iterator().next().getMessage();

        // medida minima: aumenta so os eixos que faltaram e confirma que aquela disposicao passa a caber
        Integer sugLargura = null;
        Integer sugProfundidade = null;
        long menorArea = Long.MAX_VALUE;
        for (Map.Entry<EstrategiaLayout, Falha> en : falhas.entrySet()) {
            int l = Math.max(gaveta.larguraMm(), en.getValue().larguraNecessariaMm);
            int p = Math.max(gaveta.profundidadeMm(), en.getValue().profundidadeNecessariaMm);
            try {
                tentar(new MedidasGaveta(l, p, gaveta.alturaMm()), unidades, config, en.getKey());
            } catch (Falha naoResolve) {
                continue;
            }
            long area = (long) l * p;
            if (area < menorArea) {
                menorArea = area;
                sugLargura = l > gaveta.larguraMm() ? l : null;
                sugProfundidade = p > gaveta.profundidadeMm() ? p : null;
            }
        }

        return new LayoutNaoCabeException(mensagem, sugLargura, sugProfundidade,
                sugerirDuasCamadas(gaveta, unidades, config));
    }

    /** Divide os objetos em duas bandejas empilhadas (cada uma com metade da altura), se ambas couberem. */
    private List<CamadaSugerida> sugerirDuasCamadas(MedidasGaveta gaveta, List<TipoObjetoConstraint> unidades,
                                                    ConfigEscala config) {
        int alturaCamada = gaveta.alturaMm() / 2;
        if (alturaCamada < ALTURA_MINIMA_CAMADA_MM || unidades.size() < 2) {
            return List.of();
        }
        for (TipoObjetoConstraint t : unidades) {
            if (t.alturaMinMm() != null && t.alturaMinMm() > alturaCamada) {
                return List.of();
            }
        }
        MedidasGaveta camada = new MedidasGaveta(gaveta.larguraMm(), gaveta.profundidadeMm(), alturaCamada);

        List<TipoObjetoConstraint> a = new ArrayList<>();
        List<TipoObjetoConstraint> b = new ArrayList<>();
        for (TipoObjetoConstraint u : unidades) {
            List<TipoObjetoConstraint> tentativa = new ArrayList<>(a);
            tentativa.add(u);
            if (cabeEmAlgumaDisposicao(camada, tentativa, config)) {
                a.add(u);
            } else {
                b.add(u);
            }
        }
        if (a.isEmpty() || b.isEmpty() || !cabeEmAlgumaDisposicao(camada, b, config)) {
            return List.of();
        }
        return List.of(
                new CamadaSugerida(alturaCamada, agrupar(a)),
                new CamadaSugerida(alturaCamada, agrupar(b)));
    }

    private boolean cabeEmAlgumaDisposicao(MedidasGaveta g, List<TipoObjetoConstraint> unidades, ConfigEscala config) {
        for (EstrategiaLayout e : ORDEM_AUTO) {
            try {
                tentar(g, unidades, config, e);
                return true;
            } catch (Falha ignorada) {
                // tenta a proxima disposicao
            }
        }
        return false;
    }

    private List<ItemObjetoSelecionado> agrupar(List<TipoObjetoConstraint> unidades) {
        Map<UUID, Integer> contagem = new LinkedHashMap<>();
        for (TipoObjetoConstraint u : unidades) {
            contagem.merge(u.id(), 1, Integer::sum);
        }
        return contagem.entrySet().stream()
                .map(en -> new ItemObjetoSelecionado(en.getKey(), en.getValue()))
                .toList();
    }
}
