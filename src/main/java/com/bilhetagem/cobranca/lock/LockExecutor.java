package com.bilhetagem.cobranca.lock;

import com.bilhetagem.cobranca.exception.LockNotAcquiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Executes a {@link Callable} under a distributed lock.
 *
 * <p>Acquires the lock before executing the callable and always releases it in a
 * {@code finally} block. If the lock cannot be acquired, a
 * {@link LockNotAcquiredException} is thrown immediately without executing the callable.
 * Any failure during lock release is logged at WARN level and swallowed so that the
 * original result (or exception) from the callable is propagated to the caller.
 */
@Component
public class LockExecutor {

    private static final Logger log = LoggerFactory.getLogger(LockExecutor.class);

    private final LockService lockService;

    public LockExecutor(LockService lockService) {
        this.lockService = lockService;
    }

    /**
     * Acquires the lock identified by {@code key}, executes {@code callable}, and
     * releases the lock in a {@code finally} block.
     *
     * @param key        the lock key
     * @param ttlSeconds lock time-to-live in seconds
     * @param callable   the operation to execute under the lock
     * @param <T>        return type of the callable
     * @return the value returned by {@code callable}
     * @throws LockNotAcquiredException if the lock could not be acquired
     * @throws RuntimeException         wrapping any checked exception thrown by the callable
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
