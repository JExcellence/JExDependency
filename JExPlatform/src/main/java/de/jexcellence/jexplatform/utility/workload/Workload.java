package de.jexcellence.jexplatform.utility.workload;

import org.jetbrains.annotations.NotNull;

/**
 * Unit of work that can be queued inside a {@link WorkloadExecutor}.
 *
 * <p>Implementations should be lightweight and free of blocking operations so
 * the executor can honor its per-tick latency budget. The default
 * {@link #computeCost()} reports a nominal cost of {@code 1}; heavier workloads
 * should override it so schedulers can batch work more intelligently.
 *
 * <pre>{@code
 * executor.submit(Workload.of(() -> chunk.load()));
 * executor.submit(first.andThen(second));
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
@FunctionalInterface
public interface Workload {

    /**
     * Executes the workload synchronously on the thread draining the executor queue.
     */
    void execute();

    /**
     * Wraps a {@link Runnable} as a workload.
     *
     * @param runnable the runnable to adapt
     * @return a workload delegating to the provided runnable
     */
    static @NotNull Workload of(@NotNull Runnable runnable) {
        return runnable::run;
    }

    /**
     * Reports the relative cost of executing this workload. Higher values indicate
     * heavier work.
     *
     * @return relative execution cost (default {@code 1})
     */
    default int computeCost() {
        return 1;
    }

    /**
     * Chains this workload with another, guaranteeing serial execution within the
     * same tick if the executor has remaining budget. The resulting cost is the sum
     * of both participants.
     *
     * @param after workload to invoke after this one completes
     * @return composed workload running this then {@code after}
     */
    default @NotNull Workload andThen(@NotNull Workload after) {
        var current = this;
        return new Workload() {
            @Override
            public void execute() {
                current.execute();
                after.execute();
            }

            @Override
            public int computeCost() {
                return current.computeCost() + after.computeCost();
            }
        };
    }
}
