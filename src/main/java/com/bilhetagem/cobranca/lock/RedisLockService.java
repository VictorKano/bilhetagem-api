package com.bilhetagem.cobranca.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * {@link LockService} implementation backed by Redis via Redisson.
 *
 * <p>Active only when {@code spring.data.redis.host} is configured and not in the
 * {@code test} profile.
 * Uses a non-waiting {@code tryLock} with the given TTL so that concurrent
 * requests for the same key fail fast instead of queuing.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisLockService implements LockService {

    private final RedissonClient redissonClient;

    public RedisLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Attempts to acquire the lock immediately (wait time = 0).
     *
     * @param key        the lock key
     * @param ttlSeconds time-to-live in seconds; the lock is released automatically after this period
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    @Override
    public boolean tryAcquire(String key, long ttlSeconds) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.tryLock(0, ttlSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Releases the lock for the given key if it is held by the current thread.
     *
     * @param key the lock key
     */
    @Override
    public void release(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
