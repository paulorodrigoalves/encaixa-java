package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;

import java.util.List;

/**
 * Os objetos escolhidos nao cabem em nenhuma disposicao. Alem da mensagem, carrega sugestoes
 * calculadas pelo motor: medida minima de gaveta que resolveria e/ou divisao em duas camadas.
 */
public class LayoutNaoCabeException extends LayoutInvalidoException {

    /** Uma camada (bandeja) empilhada: altura propria e os objetos que ela guarda. */
    public record CamadaSugerida(int alturaMm, List<ItemObjetoSelecionado> itens) {}

    private final Integer larguraMinimaSugeridaMm;
    private final Integer profundidadeMinimaSugeridaMm;
    private final List<CamadaSugerida> camadas;

    public LayoutNaoCabeException(String message, Integer larguraMinimaSugeridaMm,
                                  Integer profundidadeMinimaSugeridaMm, List<CamadaSugerida> camadas) {
        super(message);
        this.larguraMinimaSugeridaMm = larguraMinimaSugeridaMm;
        this.profundidadeMinimaSugeridaMm = profundidadeMinimaSugeridaMm;
        this.camadas = camadas == null ? List.of() : camadas;
    }

    public Integer larguraMinimaSugeridaMm() {
        return larguraMinimaSugeridaMm;
    }

    public Integer profundidadeMinimaSugeridaMm() {
        return profundidadeMinimaSugeridaMm;
    }

    public List<CamadaSugerida> camadas() {
        return camadas;
    }
}
