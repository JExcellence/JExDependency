package com.raindropcentral.rplatform.workload;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.*;

class WorkloadExecutorTest {

    @Test
    void runOnEmptyQueueLeavesExecutorCompleted() {
        final WorkloadExecutor executor = new WorkloadExecutor();

        executor.run();
        executor.run();

        assertTrue(executor.isCompleted());
        assertEquals(0, executor.getPendingCount());
    }

    @Test
    void submitAndRunProcessesWorkloadsInOrder() {
        final WorkloadExecutor executor = new WorkloadExecutor();
        final List<String> executionOrder = new ArrayList<>();

        executor.submit(() -> executionOrder.add("first"));
        executor.submit(() -> executionOrder.add("second"));
        executor.submit(() -> executionOrder.add("third"));

        executor.run();

        assertEquals(List.of("first", "second", "third"), executionOrder);
        assertTrue(executor.isCompleted());
        assertEquals(0, executor.getPendingCount());
    }

    @Test
    void tickBudgetThrottlesLongRunningWorkload() {
        final WorkloadExecutor executor = new WorkloadExecutor();
        final List<String> executionOrder = new ArrayList<>();

        executor.submit(() -> {
            executionOrder.add("slow");
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(3));
        });
        executor.submit(() -> executionOrder.add("fast-1"));
        executor.submit(() -> executionOrder.add("fast-2"));

        executor.run();

        assertEquals(List.of("slow"), executionOrder);
        assertEquals(2, executor.getPendingCount());
        assertFalse(executor.isCompleted());

        executor.run();

        assertEquals(List.of("slow", "fast-1", "fast-2"), executionOrder);
        assertTrue(executor.isCompleted());
    }

    @Test
    void submitAsyncCompletesAfterSimulatedTicks() throws Exception {
        final WorkloadExecutor executor = new WorkloadExecutor();
        final TestScheduler scheduler = new TestScheduler();
        final AtomicInteger executed = new AtomicInteger();

        final CompletableFuture<Void> future = executor.submitAsync(() -> executed.incrementAndGet());

        awaitPendingCount(executor, 1, Duration.ofSeconds(1));

        scheduler.schedule(executor::run);
        while (!future.isDone()) {
            scheduler.tick();
            if (!future.isDone()) {
                scheduler.schedule(executor::run);
            }
        }

        future.get(1, TimeUnit.SECONDS);

        assertEquals(1, executed.get());
        assertTrue(executor.isCompleted());
        assertEquals(0, executor.getPendingCount());
    }

    @Test
    void concurrentAccessAcrossPublicMethodsRemainsThreadSafe() throws Exception {
        final WorkloadExecutor executor = new WorkloadExecutor();
        final ExecutorService pool = Executors.newFixedThreadPool(5);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger executed = new AtomicInteger();
        final List<CompletableFuture<Void>> asyncFutures = Collections.synchronizedList(new ArrayList<>());

        try {
            final Future<?> runnerFuture = pool.submit(() -> {
                start.await();
                while (running.get()) {
                    executor.run();
                    Thread.yield();
                }
                return null;
            });

            final Future<?> submitterFuture = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 200; i++) {
                    executor.submit(() -> executed.incrementAndGet());
                }
                return null;
            });

            final Future<?> asyncSubmitterFuture = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 40; i++) {
                    final CompletableFuture<Void> future = executor.submitAsync(() -> executed.incrementAndGet());
                    asyncFutures.add(future);
                }
                return null;
            });

            final Future<?> observerFuture = pool.submit(() -> {
                start.await();
                for (int i = 0; i < 400; i++) {
                    executor.getPendingCount();
                    executor.isCompleted();
                    Thread.yield();
                }
                return null;
            });

            final Future<?> clearerFuture = pool.submit(() -> {
                start.await();
                Thread.sleep(5);
                executor.clear();
                return null;
            });

            start.countDown();

            submitterFuture.get(1, TimeUnit.SECONDS);
            asyncSubmitterFuture.get(1, TimeUnit.SECONDS);
            for (final CompletableFuture<Void> future : asyncFutures) {
                future.get(1, TimeUnit.SECONDS);
            }
            clearerFuture.get(1, TimeUnit.SECONDS);
            running.set(false);
            runnerFuture.get(1, TimeUnit.SECONDS);
            observerFuture.get(1, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }

        while (!executor.isCompleted()) {
            executor.run();
        }

        assertTrue(executor.isCompleted());
        assertEquals(0, executor.getPendingCount());
        assertTrue(executed.get() >= 0);
    }

    private static void awaitPendingCount(final WorkloadExecutor executor, final int expected, final Duration timeout)
            throws InterruptedException {
        final long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (executor.getPendingCount() == expected) {
                return;
            }
            Thread.sleep(2);
        }
        fail("Timed out waiting for pending count to reach " + expected);
    }

    private static final class TestScheduler {

        private final Deque<Runnable> callbacks = new ArrayDeque<>();

        void schedule(final Runnable callback) {
            callbacks.addLast(callback);
        }

        void tick() {
            final Runnable callback = callbacks.pollFirst();
            if (callback != null) {
                callback.run();
            }
        }
    }
}
