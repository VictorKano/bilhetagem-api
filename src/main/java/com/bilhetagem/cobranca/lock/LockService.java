package com.bilhetagem.cobranca.lock;

/**
 * Abstraction for distributed lock operations.
 */
public interface LockService {

    /**
     * Attempts to acquire a lock for the given key with the specified TTL.
     *
     * @param key        the lock key
     * @param ttlSeconds time-to-live in seconds
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    boolean tryAcquire(String key, long ttlSeconds);

    /**
     * Releases the lock for the given key.
     *
     * @param key the lock key
     */
    void release(String key);
}
