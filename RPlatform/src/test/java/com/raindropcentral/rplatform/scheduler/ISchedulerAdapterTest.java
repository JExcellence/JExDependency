package com.raindropcentral.rplatform.scheduler;

import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.impl.BukkitISchedulerImpl;
import com.raindropcentral.rplatform.scheduler.impl.FoliaISchedulerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ISchedulerAdapterTest {

    private JavaPlugin plugin;
    private MockedStatic<Bukkit> mockedBukkit;
    private BukkitScheduler scheduler;
    private BukkitTask taskHandle;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        scheduler = mock(BukkitScheduler.class);
        taskHandle = mock(BukkitTask.class);
        mockedBukkit = mockStatic(Bukkit.class);
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
    }

    @AfterEach
    void tearDown() {
        if (mockedBukkit != null) {
            mockedBukkit.close();
        }
    }

    @Test
    void createReturnsFoliaImplementationWhenPlatformIsFolia() {
        ISchedulerAdapter adapter = ISchedulerAdapter.create(plugin, PlatformType.FOLIA);
        assertInstanceOf(FoliaISchedulerImpl.class, adapter);
    }

    @Test
    void createReturnsBukkitImplementationForPaper() {
        ISchedulerAdapter adapter = ISchedulerAdapter.create(plugin, PlatformType.PAPER);
        assertInstanceOf(BukkitISchedulerImpl.class, adapter);
    }

    @Test
    void createReturnsBukkitImplementationForSpigot() {
        ISchedulerAdapter adapter = ISchedulerAdapter.create(plugin, PlatformType.SPIGOT);
        assertInstanceOf(BukkitISchedulerImpl.class, adapter);
    }

    @Test
    void bukkitRunSyncDelegatesToScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            executed.set(true);
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        adapter.runSync(() -> {});

        assertTrue(executed.get());
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @Test
    void bukkitRunAsyncDelegatesToScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            executed.set(true);
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        adapter.runAsync(() -> {});

        assertTrue(executed.get());
        verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
    }

    @Test
    void bukkitRunDelayedDelegatesToScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(5L))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            executed.set(true);
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        adapter.runDelayed(() -> {}, 5L);

        assertTrue(executed.get());
        verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(5L));
    }

    @Test
    void bukkitRunRepeatingDelegatesToScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(2L), eq(4L))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            executed.set(true);
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        adapter.runRepeating(() -> {}, 2L, 4L);

        assertTrue(executed.get());
        verify(scheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(2L), eq(4L));
    }

    @Test
    void bukkitRunAtEntityFallsBackToSyncScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        Entity entity = mock(Entity.class);
        adapter.runAtEntity(entity, () -> executed.set(true));

        assertTrue(executed.get());
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @Test
    void bukkitRunAtLocationFallsBackToSyncScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        Location location = mock(Location.class);
        adapter.runAtLocation(location, () -> executed.set(true));

        assertTrue(executed.get());
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @Test
    void bukkitRunGlobalFallsBackToSyncScheduler() {
        AtomicBoolean executed = new AtomicBoolean();
        Mockito.when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        adapter.runGlobal(() -> executed.set(true));

        assertTrue(executed.get());
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    @Test
    void bukkitRunAsyncFutureCompletesAndPropagatesErrors() {
        AtomicInteger executions = new AtomicInteger();
        Mockito.when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            executions.incrementAndGet();
            return taskHandle;
        });

        ISchedulerAdapter adapter = new BukkitISchedulerImpl(plugin);
        CompletableFuture<Void> success = adapter.runAsyncFuture(executions::incrementAndGet);
        assertDoesNotThrow(success::join);
        assertEquals(2, executions.get());

        Mockito.reset(scheduler);
        AtomicBoolean failed = new AtomicBoolean();
        Mockito.when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return taskHandle;
        });

        CompletableFuture<Void> failure = adapter.runAsyncFuture(() -> {
            throw new IllegalStateException("boom");
        });
        failure.whenComplete((ignored, throwable) -> failed.set(throwable != null));
        assertThrows(IllegalStateException.class, failure::join);
        assertTrue(failed.get());
    }

    @Test
    void foliaRunSyncDelegatesToRunGlobalHook() {
        FoliaISchedulerImpl adapter = spy(new FoliaISchedulerImpl(plugin));
        AtomicBoolean executed = new AtomicBoolean();
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0, Runnable.class);
            runnable.run();
            return null;
        }).when(adapter).runGlobal(any(Runnable.class));

        adapter.runSync(() -> executed.set(true));

        assertTrue(executed.get());
        verify(adapter).runGlobal(any(Runnable.class));
    }

    @Test
    void foliaRunAsyncExecutesOnCompletableFutureExecutor() throws InterruptedException {
        FoliaISchedulerImpl adapter = new FoliaISchedulerImpl(plugin);
        CountDownLatch latch = new CountDownLatch(1);

        adapter.runAsync(latch::countDown);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void foliaRunAsyncFuturePropagatesExceptions() {
        FoliaISchedulerImpl adapter = new FoliaISchedulerImpl(plugin);
        CompletableFuture<Void> success = adapter.runAsyncFuture(() -> {});
        assertDoesNotThrow(success::join);

        CompletableFuture<Void> failure = adapter.runAsyncFuture(() -> {
            throw new IllegalArgumentException("failure");
        });
        assertThrows(IllegalArgumentException.class, failure::join);
    }
}
