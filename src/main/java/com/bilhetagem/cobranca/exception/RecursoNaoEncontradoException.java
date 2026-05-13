package com.bilhetagem.cobranca.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado.
 * Mapeada para HTTP 404 Not Found com corpo {mensagem}.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
