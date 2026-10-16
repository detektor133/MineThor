package dev.minethor.agent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Runs work on the Minecraft client thread. The bridge thread never touches game objects directly:
 * it submits a task here and waits a bounded time for the result.
 *
 * A task that did not start within the timeout is cancelled, so a command that was already answered
 * with a timeout can never be applied later.
 */
final class ClientThread {
    private final Supplier<Executor> executorSupplier;

    ClientThread(Supplier<Executor> executorSupplier) {
        this.executorSupplier = executorSupplier;
    }

    /** True if the running game exposes a way to schedule work on its main thread. */
    boolean available() {
        return executorSupplier.get() != null;
    }

    /** Returns the task result, or {@code onFailure} if there is no executor, it timed out or it threw. */
    <T> T call(Callable<T> task, long timeoutMs, T onFailure) {
        Executor executor = executorSupplier.get();
        if (executor == null) return onFailure;

        FutureTask<T> future = new FutureTask<>(task);
        try {
            executor.execute(future);
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(false);
            return onFailure;
        } catch (InterruptedException e) {
            future.cancel(false);
            Thread.currentThread().interrupt();
            return onFailure;
        } catch (Exception e) {
            future.cancel(false);
            return onFailure;
        }
    }
}
