package br.com.encaixa.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Violacao de regra de negocio com status HTTP associado (400 por padrao,
 * 409 para conflitos de estado).
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public NegocioException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static NegocioException conflito(String message) {
        return new NegocioException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
