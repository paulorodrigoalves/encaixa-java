package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.Compartimento;
import br.com.encaixa.domain.engine.model.Modelos.ResultadoEscala;
import br.com.encaixa.domain.engine.model.Modelos.TipoObjetoConstraint;

import java.util.List;

/** Layout gerado a partir de objetos, ja com o objeto que ocupa cada compartimento. */
public record ResultadoLayoutObjetos(
        EstrategiaLayout estrategia,
        ResultadoEscala escala,
        List<ObjetoColocado> objetos
) {

    /**
     * @param girado             objeto girado 90 graus nesta disposicao
     * @param folgaLarguraMm     quanto o compartimento excede a largura maxima do objeto (0 se nao excede)
     * @param folgaProfundidadeMm idem para a profundidade
     */
    public record ObjetoColocado(
            TipoObjetoConstraint tipo,
            Compartimento compartimento,
            boolean girado,
            int folgaLarguraMm,
            int folgaProfundidadeMm
    ) {}
}
