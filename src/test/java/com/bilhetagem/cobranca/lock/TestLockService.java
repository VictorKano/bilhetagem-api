package com.bilhetagem.cobranca.lock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestLockService implements LockService {

    @Override
    public boolean tryAcquire(String key, long ttlSeconds) {
        return true;
    }

    @Override
    public void release(String key) {
    }
}
