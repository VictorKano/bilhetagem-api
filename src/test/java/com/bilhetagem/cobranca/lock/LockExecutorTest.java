package com.bilhetagem.cobranca.lock;

import com.bilhetagem.cobranca.exception.LockNotAcquiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockExecutorTest {

    @Mock
    private LockService lockService;

    private LockExecutor lockExecutor;

    @BeforeEach
    void setUp() {
        lockExecutor = new LockExecutor(lockService);
    }

    @Test
    void executeWhenLockAcquiredReturnsCallableResult() throws Exception {
        when(lockService.tryAcquire("key", 5L)).thenReturn(true);

        String result = lockExecutor.execute("key", 5L, () -> "result");

        assertThat(result).isEqualTo("result");
        verify(lockService).release("key");
    }

    @Test
    void executeWhenLockNotAcquiredThrowsLockNotAcquiredException() {
        when(lockService.tryAcquire("key", 5L)).thenReturn(false);

        assertThatThrownBy(() -> lockExecutor.execute("key", 5L, () -> "result"))
                .isInstanceOf(LockNotAcquiredException.class);

        verify(lockService, never()).release(any());
    }

    @Test
    void executeWhenCallableThrowsRuntimeExceptionReleasesLockAndPropagatesException() {
        when(lockService.tryAcquire("key", 5L)).thenReturn(true);

        assertThatThrownBy(() -> lockExecutor.execute("key", 5L, () -> {
            throw new IllegalStateException("callable error");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("callable error");

        verify(lockService).release("key");
    }

    @Test
    void executeWhenCallableThrowsCheckedExceptionReleasesLockAndWrapsInRuntimeException() {
        when(lockService.tryAcquire("key", 5L)).thenReturn(true);

        assertThatThrownBy(() -> lockExecutor.execute("key", 5L, () -> {
            throw new Exception("checked error");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(Exception.class);

        verify(lockService).release("key");
    }

    @Test
    void executeWhenReleaseThrowsLogsWarnAndDoesNotPropagateReleaseException() {
        when(lockService.tryAcquire("key", 5L)).thenReturn(true);
        doThrow(new RuntimeException("redis down")).when(lockService).release("key");
        String result = lockExecutor.execute("key", 5L, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void executeWhenCallableThrowsAndReleaseThrowsPropagatesCallableException() {
        when(lockService.tryAcquire("key", 5L)).thenReturn(true);
        doThrow(new RuntimeException("redis down")).when(lockService).release("key");

        assertThatThrownBy(() -> lockExecutor.execute("key", 5L, () -> {
            throw new IllegalArgumentException("business error");
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business error");
    }
}
