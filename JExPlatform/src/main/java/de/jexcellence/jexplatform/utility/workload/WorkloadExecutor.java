package de.jexcellence.jexplatform.utility.workload;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

/**
 * Tick-budgeted executor that drains queued {@link Workload workloads} without exceeding
 * a configurable per-tick latency threshold.
 *
 * <p>Workloads can be submitted from any thread; they are buffered in a synchronized
 * deque and processed on the thread invoking {@link #run()}. Register this executor
 * with a repeating tick task via {@code PlatformScheduler}:
 *
 * <pre>{@code
 * var executor = new WorkloadExecutor();
 * scheduler.runRepeating(executor, 1L, 1L);
 * executor.submit(Workload.of(() -> heavyWork()));
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class WorkloadExecutor implements Runnable {

    private static final double DEFAULT_MAX_MILLIS = 2.5;

    private final long maxNanosPerTick;
    private final Deque<Workload> queue;
    private final Object lock = new Object();

    /**
     * Creates an executor with the default 2.5 ms per-tick budget.
     */
    public WorkloadExecutor() {
        this(DEFAULT_MAX_MILLIS);
    }

    /**
     * Creates an executor with a custom per-tick time budget.
     *
     * @param maxMillisPerTick maximum wall-clock milliseconds per tick
     */
    public WorkloadExecutor(double maxMillisPerTick) {
        this.maxNanosPerTick = (long) (maxMillisPerTick * 1_000_000);
        this.queue = new ArrayDeque<>();
    }

    /**
     * Enqueues a workload for future execution in FIFO order.
     *
     * @param workload the workload to enqueue
     */
    public void submit(@NotNull Workload workload) {
        synchronized (lock) {
            queue.offer(workload);
        }
    }

    /**
     * Enqueues the workload and returns a future that completes when the queue
     * has fully drained.
     *
     * @param workload the workload to enqueue
     * @return future completing when the executor reports completion
     */
    public @NotNull CompletableFuture<Void> submitAsync(@NotNull Workload workload) {
        return CompletableFuture.runAsync(() -> {
            submit(workload);
            while (!isCompleted()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Drains the queue until all workloads are executed or the per-tick budget
     * is consumed.
     */
    @Override
    public void run() {
        synchronized (lock) {
            var stopTime = System.nanoTime() + maxNanosPerTick;

            while (System.nanoTime() <= stopTime && !queue.isEmpty()) {
                var workload = queue.poll();
                if (workload != null) {
                    workload.execute();
                }
            }
        }
    }

    /**
     * Returns whether all submitted workloads have been executed.
     *
     * @return {@code true} when the queue is empty
     */
    public boolean isCompleted() {
        synchronized (lock) {
            return queue.isEmpty();
        }
    }

    /**
     * Returns the number of workloads currently queued.
     *
     * @return pending workload count
     */
    public int pendingCount() {
        synchronized (lock) {
            return queue.size();
        }
    }

    /**
     * Discards all pending workloads.
     */
    public void clear() {
        synchronized (lock) {
            queue.clear();
        }
    }
}
