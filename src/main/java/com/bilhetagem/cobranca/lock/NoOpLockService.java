package com.bilhetagem.cobranca.lock;

import com.bilhetagem.cobranca.exception.LockNotAcquiredException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Implementação de fallback de {@link LockService} usada quando o Redis não está configurado.
 *
 * <p>Como nenhuma infraestrutura de lock distribuído está disponível, {@link #tryAcquire}
 * sempre lança {@link LockNotAcquiredException} para impedir a geração concorrente de cobranças.
 * {@link #release} é um no-op pois nenhum lock é adquirido.
 */
@Component
@ConditionalOnMissingBean(LockService.class)
public class NoOpLockService implements LockService {

    /**
     * Sempre lança {@link LockNotAcquiredException} pois nenhum backend de lock está disponível.
     *
     * @param key        a chave do lock (não utilizada)
     * @param ttlSeconds tempo de vida em segundos (não utilizado)
     * @return nunca retorna normalmente
     * @throws LockNotAcquiredException sempre
     */
    @Override
    public boolean tryAcquire(String key, long ttlSeconds) {
        throw new LockNotAcquiredException("Geração de cobrança em andamento");
    }

    /**
     * No-op: nenhum lock foi adquirido, portanto nada precisa ser liberado.
     *
     * @param key a chave do lock (não utilizada)
     */
    @Override
    public void release(String key) {
        // intentionally empty — no lock to release
    }
}
