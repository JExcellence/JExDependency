package com.raindropcentral.rplatform.workload;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;

public class WorkloadExecutor implements Runnable {

    private static final double MAX_MILLIS_PER_TICK = 2.5;
    private static final long MAX_NANOS_PER_TICK = (long) (MAX_MILLIS_PER_TICK * 1_000_000);

    private final Deque<Workload> workloadQueue;
    private final Object lock = new Object();

    public WorkloadExecutor() {
        this.workloadQueue = new ArrayDeque<>();
    }

    public void submit(final @NotNull Workload workload) {
        synchronized (lock) {
            workloadQueue.offer(workload);
        }
    }

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

    public boolean isCompleted() {
        synchronized (lock) {
            return workloadQueue.isEmpty();
        }
    }

    public int getPendingCount() {
        synchronized (lock) {
            return workloadQueue.size();
        }
    }

    public void clear() {
        synchronized (lock) {
            workloadQueue.clear();
        }
    }
}
