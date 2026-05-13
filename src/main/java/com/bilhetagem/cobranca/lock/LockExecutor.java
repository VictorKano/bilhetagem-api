package com.bilhetagem.cobranca.lock;

import com.bilhetagem.cobranca.exception.LockNotAcquiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Executa um {@link Callable} sob um lock distribuído.
 *
 * <p>Adquire o lock antes de executar o callable e sempre o libera no bloco
 * {@code finally}. Se o lock não puder ser adquirido, uma
 * {@link LockNotAcquiredException} é lançada imediatamente sem executar o callable.
 * Falhas na liberação do lock são registradas em nível WARN e suprimidas para que
 * o resultado original (ou exceção) do callable seja propagado ao chamador.
 */
@Component
public class LockExecutor {

    private static final Logger log = LoggerFactory.getLogger(LockExecutor.class);

    private final LockService lockService;

    public LockExecutor(LockService lockService) {
        this.lockService = lockService;
    }

    /**
     * Adquire o lock identificado por {@code key}, executa o {@code callable} e
     * libera o lock no bloco {@code finally}.
     *
     * @param key        a chave do lock
     * @param ttlSeconds tempo de vida do lock em segundos
     * @param callable   a operação a ser executada sob o lock
     * @param <T>        tipo de retorno do callable
     * @return o valor retornado pelo {@code callable}
     * @throws LockNotAcquiredException se o lock não puder ser adquirido
     * @throws RuntimeException         encapsulando qualquer exceção checada lançada pelo callable
     */
    public <T> T execute(String key, long ttlSeconds, Callable<T> callable) {
        boolean acquired = lockService.tryAcquire(key, ttlSeconds);
        if (!acquired) {
            throw new LockNotAcquiredException("Geração de cobrança em andamento");
        }
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                lockService.release(key);
            } catch (Exception e) {
                log.warn("Falha ao liberar lock para chave {}: {}", key, e.getMessage());
            }
        }
    }
}
