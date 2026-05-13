package com.bilhetagem.cobranca.exception;

/**
 * Exceção lançada quando o lock distribuído não pode ser adquirido.
 * Mapeada para HTTP 422 Unprocessable Entity com mensagem "Geração de cobrança em andamento".
 */
public class LockNotAcquiredException extends RuntimeException {

    public LockNotAcquiredException(String message) {
        super(message);
    }

    public LockNotAcquiredException() {
        super("Geração de cobrança em andamento");
    }
}
