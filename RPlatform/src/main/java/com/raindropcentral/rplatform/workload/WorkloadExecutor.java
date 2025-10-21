package com.raindropcentral.rplatform.workload;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

/**
 * Executes {@link Workload workloads} against a per-tick budget so that queued tasks are drained during the
 * platform's scheduler loop without exceeding the configured latency threshold. Workloads can be submitted from
 * any thread; they are buffered in a synchronized deque and processed on the thread invoking {@link #run()}. This
 * ensures gameplay logic executes serially while still allowing asynchronous producers to enqueue work without
 * blocking the tick loop for more than the configured time slice.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class WorkloadExecutor implements Runnable {

    /**
     * Maximum wall-clock time in milliseconds that may be spent draining the queue in a single tick, balancing
     * workload throughput with frame latency.
     */
    private static final double MAX_MILLIS_PER_TICK = 2.5;

    /**
     * Cached nanosecond representation of {@link #MAX_MILLIS_PER_TICK} to avoid repeated conversions inside the
     * critical processing loop.
     */
    private static final long MAX_NANOS_PER_TICK = (long) (MAX_MILLIS_PER_TICK * 1_000_000);

    /**
     * FIFO buffer that preserves workload ordering; access is guarded by {@link #lock} to provide thread safety
     * when producers and the tick thread interact concurrently.
     */
    private final Deque<Workload> workloadQueue;

    /**
     * Monitor used to synchronize queue operations so that submissions from asynchronous producers do not race
     * the tick-thread drain loop.
     */
    private final Object lock = new Object();

    /**
     * Creates a workload executor with an empty deque, ready to accept work from multiple threads without
     * additional initialization.
     */
    public WorkloadExecutor() {
        this.workloadQueue = new ArrayDeque<>();
    }

    /**
     * Adds the supplied workload to the queue, synchronizing on the internal lock to keep submissions thread safe
     * and guaranteeing FIFO execution order.
     *
     * @param workload workload to enqueue for future execution
     */
    public void submit(final @NotNull Workload workload) {
        synchronized (lock) {
            workloadQueue.offer(workload);
        }
    }

    /**
     * Enqueues the workload asynchronously and blocks the returned {@link CompletableFuture} until the queue has
     * fully drained, propagating interruption as a runtime exception so callers can react to cancellation.
     *
     * <p>The submission still uses {@link #submit(Workload)} under the hood, so concurrency semantics match the
     * synchronous variant.</p>
     *
     * @param workload workload to enqueue for asynchronous submission
     * @return future completing when the executor reports {@link #isCompleted() completion}
     */
    public @NotNull CompletableFuture<Void> submitAsync(final @NotNull Workload workload) {
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
     * Drains the queue until either all workloads are executed or the per-tick budget has been consumed, ensuring
     * that long queues do not monopolize the tick thread while still respecting workload ordering.
     */
    @Override
    public void run() {
        synchronized (lock) {
            final long stopTime = System.nanoTime() + MAX_NANOS_PER_TICK;

            while (System.nanoTime() <= stopTime && !workloadQueue.isEmpty()) {
                final Workload workload = workloadQueue.poll();
                if (workload != null) {
                    workload.execute();
                }
            }
        }
    }

    /**
     * Indicates whether all submitted workloads have finished executing, enabling callers to monitor queue
     * latency from any thread.
     *
     * @return {@code true} when the queue is empty
     */
    public boolean isCompleted() {
        synchronized (lock) {
            return workloadQueue.isEmpty();
        }
    }

    /**
     * Returns the number of workloads currently buffered, allowing monitoring tools to estimate drain time or
     * trigger back-pressure strategies.
     *
     * @return number of queued workloads awaiting execution
     */
    public int getPendingCount() {
        synchronized (lock) {
            return workloadQueue.size();
        }
    }

    /**
     * Clears the queue, discarding any pending workloads in a thread-safe manner—useful for shutdown sequences or
     * failure recovery when outstanding tasks can no longer be completed safely.
     */
    public void clear() {
        synchronized (lock) {
            workloadQueue.clear();
        }
    }
}
