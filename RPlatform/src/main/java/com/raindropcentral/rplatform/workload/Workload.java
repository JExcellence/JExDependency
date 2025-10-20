package com.raindropcentral.rplatform.workload;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a unit of work that can be queued inside a {@link WorkloadExecutor}. Implementations should be
 * lightweight and free of blocking operations so that the executor can honor its per-tick latency budget. The
 * functional nature allows callers to compose workloads fluently while still conveying execution order guarantees.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@FunctionalInterface
public interface Workload {

    /**
     * Executes the workload synchronously on the thread draining the executor queue. Implementations should avoid
     * long-running or blocking logic to keep tick latency predictable.
     */
    void execute();

    /**
     * Wraps a {@link Runnable} as a workload for submission to the executor while preserving the runnable's
     * thread-affinity expectations.
     *
     * @param runnable runnable to adapt into a workload
     * @return workload delegating execution to the provided runnable
     */
    static @NotNull Workload of(final @NotNull Runnable runnable) {
        return runnable::run;
    }

    /**
     * Chains the current workload with another, guaranteeing serial execution within the same tick if the executor
     * has remaining budget and propagating exceptions from either workload to the caller.
     *
     * @param after workload to invoke after the current one completes
     * @return composed workload that runs this workload followed by {@code after}
     */
    default @NotNull Workload andThen(final @NotNull Workload after) {
        return () -> {
            this.execute();
            after.execute();
        };
    }
}
