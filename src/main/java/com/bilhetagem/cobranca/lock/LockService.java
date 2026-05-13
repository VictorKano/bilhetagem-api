package com.bilhetagem.cobranca.lock;

/**
 * Abstração para operações de lock distribuído.
 */
public interface LockService {

    /**
     * Tenta adquirir o lock para a chave informada com o TTL especificado.
     *
     * @param key        a chave do lock
     * @param ttlSeconds tempo de vida em segundos
     * @return {@code true} se o lock foi adquirido, {@code false} caso contrário
     */
    boolean tryAcquire(String key, long ttlSeconds);

    /**
     * Libera o lock para a chave informada.
     *
     * @param key a chave do lock
     */
    void release(String key);
}
