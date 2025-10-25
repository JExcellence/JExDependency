package com.raindropcentral.rplatform.scheduler.impl;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BukkitISchedulerImplTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private BukkitISchedulerImpl scheduler;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SchedulerTestPlugin.class);
        scheduler = new BukkitISchedulerImpl(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void runSyncExecutesOnNextTick() {
        AtomicBoolean executed = new AtomicBoolean(false);

        scheduler.runSync(() -> executed.set(true));
        server.getScheduler().performOneTick();

        assertTrue(executed.get());
    }

    @Test
    void runAsyncExecutesImmediately() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.runAsync(latch::countDown);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void runDelayedExecutesAfterConfiguredDelay() {
        AtomicInteger executions = new AtomicInteger();

        scheduler.runDelayed(executions::incrementAndGet, 3L);

        server.getScheduler().performTicks(2L);
        assertEquals(0, executions.get());

        server.getScheduler().performOneTick();
        assertEquals(1, executions.get());
    }

    @Test
    void runRepeatingCanBeCancelled() {
        AtomicInteger executions = new AtomicInteger();

        scheduler.runRepeating(executions::incrementAndGet, 1L, 2L);

        server.getScheduler().performOneTick();
        assertEquals(1, executions.get());

        server.getScheduler().performTicks(2L);
        assertEquals(2, executions.get());

        server.getScheduler().cancelTasks(plugin);
        server.getScheduler().performTicks(3L);

        assertEquals(2, executions.get());
    }

    @Test
    void runAtEntityDelegatesToSynchronousScheduler() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Player player = server.addPlayer();

        scheduler.runAtEntity(player, () -> executed.set(true));
        server.getScheduler().performOneTick();

        assertTrue(executed.get());
    }

    @Test
    void runAtLocationDelegatesToSynchronousScheduler() {
        AtomicBoolean executed = new AtomicBoolean(false);
        server.addSimpleWorld("scheduler-test-world");
        Location location = server.getWorld("scheduler-test-world").getSpawnLocation();

        scheduler.runAtLocation(location, () -> executed.set(true));
        server.getScheduler().performOneTick();

        assertTrue(executed.get());
    }

    @Test
    void runGlobalDelegatesToSynchronousScheduler() {
        AtomicBoolean executed = new AtomicBoolean(false);

        scheduler.runGlobal(() -> executed.set(true));
        server.getScheduler().performOneTick();

        assertTrue(executed.get());
    }

    @Test
    void runAsyncFutureCompletesSuccessfully() throws Exception {
        CompletableFuture<Void> future = scheduler.runAsyncFuture(() -> {
        });

        assertNull(future.get(1, TimeUnit.SECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    void constructorRejectsNullPlugin() {
        assertThrows(NullPointerException.class, () -> new BukkitISchedulerImpl(null));
    }

    @Test
    void schedulingMethodsRejectNullRunnables() {
        assertThrows(NullPointerException.class, () -> scheduler.runSync(null));
        assertThrows(NullPointerException.class, () -> scheduler.runAsync(null));
        assertThrows(NullPointerException.class, () -> scheduler.runDelayed(null, 1L));
        assertThrows(NullPointerException.class, () -> scheduler.runRepeating(null, 1L, 1L));

        Player player = server.addPlayer();
        assertThrows(NullPointerException.class, () -> scheduler.runAtEntity(player, null));

        server.addSimpleWorld("null-run-world");
        Location location = server.getWorld("null-run-world").getSpawnLocation();
        assertThrows(NullPointerException.class, () -> scheduler.runAtLocation(location, null));

        assertThrows(NullPointerException.class, () -> scheduler.runGlobal(null));
        assertThrows(NullPointerException.class, () -> scheduler.runAsyncFuture(null));
    }

    public static class SchedulerTestPlugin extends JavaPlugin {
    }
}
