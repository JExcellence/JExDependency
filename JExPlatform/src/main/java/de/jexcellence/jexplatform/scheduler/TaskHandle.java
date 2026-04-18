package de.jexcellence.jexplatform.scheduler;

import org.jetbrains.annotations.NotNull;

/**
 * Handle to a scheduled task that can be queried and cancelled.
 *
 * <p>Obtain a handle from any {@link PlatformScheduler} method that returns
 * a repeating or delayed task. Call {@link #cancel()} to stop future
 * executions, for example during plugin shutdown or view disposal.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface TaskHandle {

    /**
     * Cancels future executions of this task.
     *
     * @return {@code true} if the task transitioned to cancelled during this call
     */
    boolean cancel();

    /**
     * Returns whether this task has been cancelled.
     *
     * @return {@code true} if the task will not execute again
     */
    boolean isCancelled();

    /**
     * Returns a permanently cancelled no-op handle.
     *
     * @return no-op task handle that is always cancelled
     */
    static @NotNull TaskHandle noop() {
        return NoOp.INSTANCE;
    }

    /** Always-cancelled handle that performs no work. */
    enum NoOp implements TaskHandle {

        /** Singleton instance. */
        INSTANCE;

        @Override
        public boolean cancel() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    }
}
