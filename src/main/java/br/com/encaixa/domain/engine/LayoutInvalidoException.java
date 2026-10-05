package br.com.encaixa.domain.engine;

/**
 * Violacao de regra fisica/geometrica do layout (medidas insuficientes,
 * template invalido, objetos que nao cabem...). Mensagem pronta pro cliente.
 */
public class LayoutInvalidoException extends RuntimeException {

    public LayoutInvalidoException(String message) {
        super(message);
    }
}
