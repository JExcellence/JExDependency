package com.raindropcentral.rplatform.scheduler.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class FoliaISchedulerImplTest {

    private JavaPlugin plugin;
    private MockedStatic<Bukkit> mockedBukkit;
    private TestGlobalRegionScheduler globalScheduler;
    private TestRegionScheduler regionScheduler;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("FoliaISchedulerImplTest" + UUID.randomUUID()));
        mockedBukkit = Mockito.mockStatic(Bukkit.class);
        globalScheduler = new TestGlobalRegionScheduler();
        regionScheduler = new TestRegionScheduler();
    }

    @AfterEach
    void tearDown() {
        mockedBukkit.close();
    }

    @Test
    void runGlobalDelegatesToGlobalScheduler() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicInteger invocations = new AtomicInteger();

        scheduler.runGlobal(invocations::incrementAndGet);

        assertEquals(1, invocations.get(), "Global scheduler should execute the runnable immediately");
        assertEquals(1, globalScheduler.runInvocations, "Global scheduler run should be invoked once");
        assertSame(plugin, globalScheduler.lastPlugin, "Scheduler should forward the plugin reference");
    }

    @Test
    void runDelayedUsesProvidedDelay() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicInteger invocations = new AtomicInteger();

        scheduler.runDelayed(invocations::incrementAndGet, 25L);

        assertEquals(1, globalScheduler.delayedInvocations, "Global scheduler should receive delayed invocation");
        assertEquals(25L, globalScheduler.lastDelay, "Delay should be forwarded to the global scheduler");
        assertNotNull(globalScheduler.lastDelayedConsumer, "Delayed consumer should be captured for execution");

        globalScheduler.lastDelayedConsumer.accept(new TestScheduledTask());
        assertEquals(1, invocations.get(), "Executing the delayed consumer should run the task");
    }

    @Test
    void runRepeatingRegistersFixedRateTaskAndSupportsCancellation() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
        globalScheduler.autoExecuteFixedRate = true;

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicInteger invocations = new AtomicInteger();

        scheduler.runRepeating(invocations::incrementAndGet, 10L, 30L);

        assertEquals(1, globalScheduler.fixedRateInvocations, "Fixed rate scheduler should be used once");
        assertEquals(10L, globalScheduler.lastDelay, "Initial delay should be passed to scheduler");
        assertEquals(30L, globalScheduler.lastPeriod, "Period should be forwarded to scheduler");
        assertNotNull(globalScheduler.lastFixedRateConsumer, "Fixed rate consumer should be captured");
        assertEquals(1, invocations.get(), "Auto execution should trigger the runnable once");

        Consumer<Object> repeatingConsumer = globalScheduler.lastFixedRateConsumer;
        TestScheduledTask scheduledTask = new TestScheduledTask();
        Consumer<Object> chained = repeatingConsumer.andThen(o -> ((TestScheduledTask) o).cancel());
        chained.accept(scheduledTask);

        assertEquals(2, invocations.get(), "Chained consumer should execute the runnable again");
        assertTrue(scheduledTask.cancelled, "Chained consumer should cancel the scheduled task");
    }

    @Test
    void runRepeatingFallsBackToDelayedWhenFixedRateUnavailable() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
        globalScheduler.failFixedRate = true;

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicInteger invocations = new AtomicInteger();

        scheduler.runRepeating(invocations::incrementAndGet, 5L, 5L);

        assertEquals(1, globalScheduler.fixedRateInvocations, "Fixed rate attempt should still be recorded");
        assertEquals(1, globalScheduler.delayedInvocations, "Fallback should schedule a delayed retry");
        assertEquals(5L, globalScheduler.lastDelay, "Fallback delay should match the provided delay");
        assertNotNull(globalScheduler.lastDelayedConsumer, "Fallback consumer should be captured for manual execution");

        globalScheduler.failFixedRate = false;
        globalScheduler.autoExecuteFixedRate = true;
        globalScheduler.lastDelayedConsumer.accept(new TestScheduledTask());

        assertEquals(1, invocations.get(), "Executing fallback should eventually run the task");
        assertEquals(2, globalScheduler.fixedRateInvocations, "Retry should attempt fixed rate scheduling again");
    }

    @Test
    void runAtLocationUsesRegionSchedulerAndFallsBackToGlobal() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
        mockedBukkit.when(Bukkit::getRegionScheduler).thenReturn(regionScheduler);
        regionScheduler.autoExecute = true;

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicInteger invocations = new AtomicInteger();
        World world = Mockito.mock(World.class);
        Location location = new Location(world, 3, 64, 3);

        scheduler.runAtLocation(location, invocations::incrementAndGet);

        assertEquals(1, regionScheduler.runInvocations, "Region scheduler should be invoked once");
        assertSame(location, regionScheduler.lastLocation, "Location should be forwarded");
        assertEquals(1, invocations.get(), "Region scheduler should execute the runnable");
        assertEquals(0, globalScheduler.runInvocations, "Global scheduler should not be used when region succeeds");

        regionScheduler.failRun = true;
        scheduler.runAtLocation(location, invocations::incrementAndGet);

        assertEquals(2, regionScheduler.runInvocations, "Region scheduler should be attempted again");
        assertEquals(1, globalScheduler.runInvocations, "Failure should fall back to global execution");
        assertEquals(2, invocations.get(), "Fallback should execute the runnable");
    }

    @Test
    void runAtEntityUsesEntitySchedulerWithFallback() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
        TestEntityScheduler entityScheduler = new TestEntityScheduler();
        entityScheduler.autoExecuteRun = true;
        entityScheduler.autoExecuteDelayed = true;

        Entity entity = Mockito.mock(Entity.class);
        Mockito.when(entity.getScheduler()).thenReturn(entityScheduler);

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicInteger invocations = new AtomicInteger();

        scheduler.runAtEntity(entity, invocations::incrementAndGet);

        assertEquals(1, entityScheduler.runInvocations, "Entity scheduler should handle direct execution");
        assertEquals(1, invocations.get(), "Runnable should execute when entity run succeeds");

        entityScheduler.failRun = true;
        scheduler.runAtEntity(entity, invocations::incrementAndGet);

        assertEquals(2, entityScheduler.runInvocations, "Entity scheduler run should be attempted twice");
        assertEquals(1, entityScheduler.delayedInvocations, "Failure should invoke delayed fallback");
        assertEquals(2, invocations.get(), "Delayed fallback should execute runnable");
    }

    @Test
    void runGlobalFallsBackToCallerThreadWhenSchedulerUnavailable() throws InterruptedException {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(null);

        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        AtomicReference<Thread> executionThread = new AtomicReference<>();

        Thread worker = new Thread(() -> scheduler.runGlobal(() -> executionThread.set(Thread.currentThread())));
        worker.start();
        worker.join();

        assertSame(worker, executionThread.get(), "Fallback execution should occur on the caller thread");
    }

    @Test
    void runAsyncFuturePropagatesExceptions() {
        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);

        CompletableFuture<Void> future = scheduler.runAsyncFuture(() -> {
            throw new IllegalStateException("boom");
        });

        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof IllegalStateException, "Underlying exception should surface");
    }

    @Test
    void nullRunnableArgumentsAreRejected() {
        mockedBukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
        FoliaISchedulerImpl scheduler = new FoliaISchedulerImpl(plugin);
        Entity entity = Mockito.mock(Entity.class);

        assertThrows(NullPointerException.class, () -> scheduler.runGlobal(null));
        assertThrows(NullPointerException.class, () -> scheduler.runDelayed(null, 1L));
        assertThrows(NullPointerException.class, () -> scheduler.runRepeating(null, 1L, 1L));
        assertThrows(NullPointerException.class, () -> scheduler.runAtEntity(entity, null));
        assertThrows(NullPointerException.class, () -> scheduler.runAtLocation(new Location(Mockito.mock(World.class), 0, 0, 0), null));
    }

    private static final class TestGlobalRegionScheduler {
        private int runInvocations;
        private int delayedInvocations;
        private int fixedRateInvocations;
        private long lastDelay;
        private long lastPeriod;
        private JavaPlugin lastPlugin;
        private Consumer<Object> lastDelayedConsumer;
        private Consumer<Object> lastFixedRateConsumer;
        private boolean failFixedRate;
        private boolean autoExecuteFixedRate;

        void run(JavaPlugin plugin, Consumer<Object> consumer) {
            this.runInvocations++;
            this.lastPlugin = plugin;
            consumer.accept(new TestScheduledTask());
        }

        void runDelayed(JavaPlugin plugin, Consumer<Object> consumer, long delay) {
            this.delayedInvocations++;
            this.lastPlugin = plugin;
            this.lastDelay = delay;
            this.lastDelayedConsumer = castConsumer(consumer);
        }

        void runAtFixedRate(JavaPlugin plugin, Consumer<Object> consumer, long delay, long period) {
            this.fixedRateInvocations++;
            this.lastPlugin = plugin;
            this.lastDelay = delay;
            this.lastPeriod = period;
            if (failFixedRate) {
                throw new RuntimeException("fixed rate unsupported");
            }
            this.lastFixedRateConsumer = castConsumer(consumer);
            if (autoExecuteFixedRate) {
                consumer.accept(new TestScheduledTask());
            }
        }

        @SuppressWarnings("unchecked")
        private Consumer<Object> castConsumer(Consumer<?> consumer) {
            return (Consumer<Object>) consumer;
        }
    }

    private static final class TestRegionScheduler {
        private int runInvocations;
        private JavaPlugin lastPlugin;
        private Location lastLocation;
        private boolean failRun;
        private boolean autoExecute;

        void run(JavaPlugin plugin, Location location, Consumer<Object> consumer) {
            this.runInvocations++;
            this.lastPlugin = plugin;
            this.lastLocation = location;
            if (failRun) {
                throw new RuntimeException("region scheduler unavailable");
            }
            if (autoExecute) {
                consumer.accept(new TestScheduledTask());
            }
        }
    }

    private static final class TestEntityScheduler {
        private int runInvocations;
        private int delayedInvocations;
        private boolean failRun;
        private boolean autoExecuteRun;
        private boolean autoExecuteDelayed;

        void run(Consumer<Object> consumer) {
            this.runInvocations++;
            if (failRun) {
                throw new RuntimeException("entity scheduler unavailable");
            }
            if (autoExecuteRun) {
                consumer.accept(new TestScheduledTask());
            }
        }

        void runDelayed(Consumer<Object> consumer, long delay) {
            this.delayedInvocations++;
            if (autoExecuteDelayed) {
                consumer.accept(new TestScheduledTask());
            }
        }
    }

    private static final class TestScheduledTask {
        private boolean cancelled;

        void cancel() {
            this.cancelled = true;
        }
    }
}
