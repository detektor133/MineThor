package dev.minethor.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ClientThreadTest {
    @Test
    public void runsTheTaskOnTheExecutorThread() {
        ExecutorService gameThread = Executors.newSingleThreadExecutor(r -> new Thread(r, "fake-client-thread"));
        try {
            ClientThread clientThread = new ClientThread(() -> gameThread);
            assertTrue(clientThread.available());
            String name = clientThread.call(() -> Thread.currentThread().getName(), 1000, null);
            assertEquals("fake-client-thread", name);
        } finally {
            gameThread.shutdownNow();
        }
    }

    @Test
    public void withoutExecutorReturnsFallbackAndIsNotAvailable() {
        ClientThread clientThread = new ClientThread(() -> null);
        assertFalse(clientThread.available());
        assertEquals("fallback", clientThread.call(() -> "x", 10, "fallback"));
    }

    @Test
    public void timedOutTaskIsCancelledAndNeverRunsLater() {
        List<Runnable> queued = new ArrayList<>();
        Executor neverRuns = queued::add; // the "game thread" is stuck
        ClientThread clientThread = new ClientThread(() -> neverRuns);
        AtomicBoolean ran = new AtomicBoolean();

        Boolean result = clientThread.call(() -> ran.compareAndSet(false, true), 30, null);

        assertEquals(null, result);
        assertEquals(1, queued.size());
        queued.get(0).run(); // the game thread wakes up late
        assertFalse("a command answered with TIMEOUT must not be applied afterwards", ran.get());
    }

    @Test
    public void exceptionInTaskGivesFallback() {
        ClientThread clientThread = new ClientThread(() -> Runnable::run);
        assertEquals("fallback", clientThread.call(() -> {
            throw new IllegalStateException("boom");
        }, 100, "fallback"));
    }
}
