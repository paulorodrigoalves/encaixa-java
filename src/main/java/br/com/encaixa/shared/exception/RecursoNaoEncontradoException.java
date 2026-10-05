package br.com.encaixa.shared.exception;

import java.util.UUID;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }

    public static RecursoNaoEncontradoException de(String recurso, UUID id) {
        return new RecursoNaoEncontradoException("%s não encontrado(a): %s".formatted(recurso, id));
    }
}
