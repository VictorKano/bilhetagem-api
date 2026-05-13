package com.bilhetagem.cobranca.lock;

import com.bilhetagem.cobranca.exception.LockNotAcquiredException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback {@link LockService} used when Redis is not configured.
 *
 * <p>Since no distributed lock infrastructure is available, {@link #tryAcquire} always
 * throws {@link LockNotAcquiredException} to prevent concurrent charge generation.
 * {@link #release} is a no-op because no lock is ever held.
 */
@Component
@ConditionalOnMissingBean(LockService.class)
public class NoOpLockService implements LockService {

    /**
     * Always throws {@link LockNotAcquiredException} because no lock backend is available.
     *
     * @param key        the lock key (unused)
     * @param ttlSeconds time-to-live in seconds (unused)
     * @return never returns normally
     * @throws LockNotAcquiredException always
     */
    @Override
    public boolean tryAcquire(String key, long ttlSeconds) {
        throw new LockNotAcquiredException("Geração de cobrança em andamento");
    }

    /**
     * No-op: no lock was acquired, so nothing needs to be released.
     *
     * @param key the lock key (unused)
     */
    @Override
    public void release(String key) {
        // intentionally empty — no lock to release
    }
}
