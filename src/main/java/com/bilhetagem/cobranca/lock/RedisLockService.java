package com.bilhetagem.cobranca.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Implementação de {@link LockService} baseada em Redis via Redisson.
 *
 * <p>Ativa somente quando {@code spring.data.redis.host} está configurado e o perfil
 * não é {@code test}. Utiliza {@code tryLock} sem espera com o TTL informado para que
 * requisições concorrentes para a mesma chave falhem rapidamente em vez de ficarem na fila.
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
     * Tenta adquirir o lock imediatamente (tempo de espera = 0).
     *
     * @param key        a chave do lock
     * @param ttlSeconds tempo de vida em segundos; o lock é liberado automaticamente após esse período
     * @return {@code true} se o lock foi adquirido, {@code false} caso contrário
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
     * Libera o lock para a chave informada se ele estiver sendo mantido pela thread atual.
     *
     * @param key a chave do lock
     */
    @Override
    public void release(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
