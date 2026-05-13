package com.bilhetagem.cobranca.lock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LockExecutorReleaseTest {

    @Test
    void lockIsReleasedExactlyOnceWhenCallableSucceeds() throws Exception {
        LockService lockService = Mockito.mock(LockService.class);
        when(lockService.tryAcquire(any(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);

        lockExecutor.execute("key", 5L, () -> "result");

        verify(lockService, times(1)).release("key");
    }

    @Test
    void lockIsReleasedExactlyOnceWhenCallableThrowsException() {
        LockService lockService = Mockito.mock(LockService.class);
        when(lockService.tryAcquire(any(), anyLong())).thenReturn(true);
        LockExecutor lockExecutor = new LockExecutor(lockService);

        try {
            lockExecutor.execute("key", 5L, () -> {
                throw new RuntimeException("callable error");
            });
        } catch (RuntimeException ignored) {
        }

        verify(lockService, times(1)).release("key");
    }
}
