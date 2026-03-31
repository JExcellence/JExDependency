/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.scheduler.impl;

import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Folia scheduler adapter that binds to the platform's region aware APIs entirely through reflection so the.
 * class is only loaded when Folia is present on the classpath.
 *
 * <p>Methods prefer Folia's {@code GlobalRegionScheduler}, {@code RegionScheduler}, and
 * {@code EntityScheduler} contract, falling back to safe global execution when functionality is missing to
 * keep the platform operational.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class FoliaISchedulerImpl implements ISchedulerAdapter {

    private static final Logger LOG = Logger.getLogger("RPlatform");
    private final JavaPlugin plugin;

    /**
     * Creates a Folia-aware scheduler adapter bound to the given plugin instance.
     *
     * @param plugin plugin that owns the scheduled work
     */
    public FoliaISchedulerImpl(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------
    // Public API
    // -------------------------

    /**
     * {@inheritDoc}.
 *
 * <p>Redirects to {@link #runGlobal(Runnable)} so work executes on Folia's global region thread, matching
     * the synchronous semantics expected by callers.
     */
    @Override
    public void runSync(@NotNull Runnable task) {
        runGlobal(task);
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Uses {@link CompletableFuture#runAsync(Runnable)} to execute work on a JDK managed thread. Folia's
     * dedicated async scheduler could be accessed reflectively, but the default executor avoids signature
     * drift across versions.
     */
    @Override
    public void runAsync(@NotNull Runnable task) {
        CompletableFuture.runAsync(safe(task));
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Attempts to call {@code GlobalRegionScheduler#runDelayed}. If unavailable, the method simulates a
     * one-shot delay using {@code runAtFixedRate} with a very large period.
     */
    @Override
    public @NotNull CancellableTaskHandle runDelayed(@NotNull Runnable task, long delayTicks) {
        final Object global = getGlobalScheduler();
        final Object scheduledTask = invoke(
                global,
                "runDelayed",
                new Class[]{Plugin.class, Consumer.class, long.class},
                new Object[]{plugin, toConsumer(task), delayTicks}
        );
        if (scheduledTask != null) {
            return new ReflectiveTaskHandle(scheduledTask);
        }

        final AtomicReference<ReflectiveTaskHandle> handleRef = new AtomicReference<>();
        final Object repeatingTask = invoke(
                global,
                "runAtFixedRate",
                new Class[]{Plugin.class, Consumer.class, long.class, long.class},
                new Object[]{
                        plugin,
                        toConsumer(() -> {
                            final ReflectiveTaskHandle handle = handleRef.get();
                            if (handle != null) {
                                handle.cancel();
                            }
                            safe(task).run();
                        }),
                        delayTicks,
                        Long.MAX_VALUE / 4
                }
        );
        if (repeatingTask != null) {
            final ReflectiveTaskHandle handle = new ReflectiveTaskHandle(repeatingTask);
            handleRef.set(handle);
            return handle;
        }

        runGlobal(task);
        return CancellableTaskHandle.noop();
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Uses {@code GlobalRegionScheduler#runAtFixedRate} and falls back to recursively scheduling when the
     * API is missing, preserving approximate cadence.
     */
    @Override
    public @NotNull CancellableTaskHandle runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        final Object global = getGlobalScheduler();
        final Object scheduledTask = invoke(
                global,
                "runAtFixedRate",
                new Class[]{Plugin.class, Consumer.class, long.class, long.class},
                new Object[]{plugin, toConsumer(task), delayTicks, periodTicks}
        );
        if (scheduledTask != null) {
            return new ReflectiveTaskHandle(scheduledTask);
        }

        final ReschedulingTaskHandle handle = new ReschedulingTaskHandle();
        scheduleRepeatingFallback(handle, task, delayTicks, periodTicks, false);
        return handle;
    }

    /**
     * Executes runRepeatingAsync.
     */
    @Override
    public @NotNull CancellableTaskHandle runRepeatingAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        final Object global = getAsyncScheduler();
        final Object scheduledTask = invoke(
                global,
                "runAtFixedRate",
                new Class[]{Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class},
                new Object[]{plugin, toConsumer(task), ticksToMillis(delayTicks), ticksToMillis(periodTicks), TimeUnit.MILLISECONDS}
        );
        if (scheduledTask != null) {
            return new ReflectiveTaskHandle(scheduledTask);
        }

        final ReschedulingTaskHandle handle = new ReschedulingTaskHandle();
        scheduleRepeatingFallback(handle, task, delayTicks, periodTicks, true);
        return handle;
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Prefers {@code EntityScheduler#run}. If direct execution fails, the adapter retries with
     * {@code runDelayed(..., 0)} to keep execution on the entity's thread when available.
     */
    @Override
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        final Object entityScheduler = getEntityScheduler(entity);
        if (invoke(
                entityScheduler,
                "run",
                new Class[]{Plugin.class, Consumer.class, Runnable.class},
                new Object[]{plugin, toConsumer(task), null}
        ) != null) {
            return;
        }

        final Object delayedTask = invoke(
                entityScheduler,
                "runDelayed",
                new Class[]{Plugin.class, Consumer.class, Runnable.class, long.class},
                new Object[]{plugin, toConsumer(task), null, 1L}
        );
        if (delayedTask != null) {
            return;
        }

        final Object executeResult = invoke(
                entityScheduler,
                "execute",
                new Class[]{Plugin.class, Runnable.class, Runnable.class, long.class},
                new Object[]{plugin, safe(task), null, 1L}
        );
        if (Boolean.TRUE.equals(executeResult)) {
            return;
        }

        runGlobal(task);
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Schedules on the region thread hosting {@code location} when Folia exposes a region scheduler. If
     * the reflective call fails the method degrades to {@link #runGlobal(Runnable)} as a safe fallback.
     */
    @Override
    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        final Object region = getRegionScheduler();
        if (invoke(
                region,
                "run",
                new Class[]{Plugin.class, Location.class, Consumer.class},
                new Object[]{plugin, location, toConsumer(task)}
        ) == null) {
            runGlobal(task);
        }
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Uses {@code GlobalRegionScheduler#run} and executes immediately when the scheduler cannot be
     * accessed, ensuring synchronous semantics remain intact.
     */
    @Override
    public void runGlobal(@NotNull Runnable task) {
        final Object global = getGlobalScheduler();
        if (invoke(global, "run", new Class[]{Plugin.class, Consumer.class}, new Object[]{plugin, toConsumer(task)}) == null &&
                invoke(global, "execute", new Class[]{Plugin.class, Runnable.class}, new Object[]{plugin, safe(task)}) == null) {
            safe(task).run();
        }
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Relies on {@link CompletableFuture#runAsync(Runnable)} to expose completion to callers regardless of
     * whether Folia's async scheduler is accessible.
     */
    @Override
    public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
        return CompletableFuture.runAsync(safe(task));
    }

    // -------------------------
    // Reflection helpers
    // -------------------------

    /**
     * Resolves Folia's {@code GlobalRegionScheduler} via reflection.
     *
     * @return scheduler instance or {@code null} when unavailable
     */
    private Object getGlobalScheduler() {
        try {
            final Method m = Bukkit.class.getMethod("getGlobalRegionScheduler");
            return m.invoke(null);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] GlobalRegionScheduler not available", t);
            return null;
        }
    }

    /**
     * Resolves Folia's {@code RegionScheduler} through reflection.
     *
     * @return scheduler instance or {@code null} when unavailable
     */
    private Object getRegionScheduler() {
        try {
            final Method m = Bukkit.class.getMethod("getRegionScheduler");
            return m.invoke(null);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] RegionScheduler not available", t);
            return null;
        }
    }

    private Object getAsyncScheduler() {
        try {
            final Method m = Bukkit.class.getMethod("getAsyncScheduler");
            return m.invoke(null);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] AsyncScheduler not available", t);
            return null;
        }
    }

    /**
     * Resolves the {@code EntityScheduler} for the supplied {@code entity}.
     *
     * @param entity entity whose scheduler should be used
     * @return scheduler instance or {@code null} when unavailable
     */
    private Object getEntityScheduler(Entity entity) {
        try {
            final Method m = entity.getClass().getMethod("getScheduler");
            return m.invoke(entity);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] EntityScheduler not available for " + entity, t);
            return null;
        }
    }

    /**
     * Attempts to invoke the given method on {@code target}, logging any reflective failures.
     *
     * @param target     instance that should receive the method call
     * @param methodName method to invoke
     * @param paramTypes parameter type signature expected
     * @param args       arguments forwarded to the method
     * @return {@code true} when invocation succeeded; {@code false} otherwise
     */
    private @Nullable Object invoke(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        if (target == null) {
            return null;
        }
        try {
            final Method m = findMethod(target.getClass(), methodName, paramTypes);
            if (m == null) {
                return null;
            }
            return m.invoke(target, args);
        } catch (InvocationTargetException ite) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] Invocation failed for " + methodName, ite.getTargetException());
            return null;
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] Invocation failed for " + methodName, t);
            return null;
        }
    }

    /**
     * Locates a method with the supplied signature or falls back to searching by name alone.
     *
     * @param clazz      class to inspect
     * @param name       name of the desired method
     * @param paramTypes expected parameter types
     * @return method reference or {@code null} when no matching method exists
     */
    private Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramTypes.length) {
                    return m;
                }
            }
            return null;
        }
    }

    private long ticksToMillis(final long ticks) {
        return Math.max(1L, ticks) * 50L;
    }

    private void scheduleRepeatingFallback(
            final @NotNull ReschedulingTaskHandle handle,
            final @NotNull Runnable task,
            final long delayTicks,
            final long periodTicks,
            final boolean async
    ) {
        if (handle.isCancelled()) {
            return;
        }

        handle.setDelegate(runDelayed(() -> {
            if (handle.isCancelled()) {
                return;
            }

            if (async) {
                runAsync(task);
            } else {
                safe(task).run();
            }

            scheduleRepeatingFallback(handle, task, periodTicks, periodTicks, async);
        }, delayTicks));
    }

    /**
     * Creates a {@link Consumer} proxy without binding to Folia's {@code ScheduledTask} type so the JVM can.
     * adapt any runnable to the reflective scheduler APIs.
     *
     * @param runnable work to run when the consumer is invoked
     * @return proxy that honours the {@link Consumer} contract
     */
    @SuppressWarnings("unchecked")
    private Object toConsumer(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return Proxy.newProxyInstance(
                Consumer.class.getClassLoader(),
                new Class[]{Consumer.class},
                (proxy, method, args) -> {
                    if ("accept".equals(method.getName()) && method.getParameterCount() == 1) {
                        safe(runnable).run();
                        return null;
                    }
                    if ("andThen".equals(method.getName())) {
                        final Object next = (args != null && args.length == 1) ? args[0] : null;
                        return Proxy.newProxyInstance(
                                Consumer.class.getClassLoader(),
                                new Class[]{Consumer.class},
                                (p2, m2, a2) -> {
                                    if ("accept".equals(m2.getName()) && m2.getParameterCount() == 1) {
                                        safe(runnable).run();
                                        if (next instanceof Consumer<?> c) {
                                            try {
                                                ((Consumer<Object>) c).accept(a2 != null ? a2[0] : null);
                                            } catch (Throwable ignored) {
                                            }
                                        }
                                        return null;
                                    }
                                    return MethodHandles.lookup().unreflect(m2).bindTo(p2).invokeWithArguments(a2);
                                }
                        );
                    }
                    return MethodHandles.lookup().unreflect(method).bindTo(proxy).invokeWithArguments(args);
                }
        );
    }

    /**
     * Wraps a runnable so any exception surfaces through the plugin logger before propagating to Folia.
     *
     * @param task task to guard
     * @return runnable that logs and rethrows failures
     */
    private Runnable safe(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "[Scheduler/Folia] Task failed", t);
                throw t;
            }
        };
    }

    private final class ReflectiveTaskHandle implements CancellableTaskHandle {

        private final Object delegate;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private ReflectiveTaskHandle(final @NotNull Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean cancel() {
            if (isCancelled()) {
                return false;
            }

            final Object result = invoke(delegate, "cancel", new Class[0], new Object[0]);
            final boolean cancelledNow = interpretCancellationResult(result) || isCancelled();
            if (cancelledNow) {
                cancelled.set(true);
            }
            return cancelledNow;
        }

        @Override
        public boolean isCancelled() {
            if (cancelled.get()) {
                return true;
            }

            final Object cancelledResult = invoke(delegate, "isCancelled", new Class[0], new Object[0]);
            if (cancelledResult instanceof Boolean isCancelled) {
                if (isCancelled) {
                    cancelled.set(true);
                }
                return isCancelled;
            }

            final Object executionState = invoke(delegate, "getExecutionState", new Class[0], new Object[0]);
            if (executionState instanceof Enum<?> enumState) {
                final boolean currentlyCancelled = enumState.name().startsWith("CANCELLED");
                if (currentlyCancelled) {
                    cancelled.set(true);
                }
                return currentlyCancelled;
            }

            return cancelled.get();
        }

        private boolean interpretCancellationResult(final @Nullable Object result) {
            if (result instanceof Enum<?> enumResult) {
                final String name = enumResult.name();
                return name.startsWith("CANCELLED") || name.startsWith("NEXT_RUNS_CANCELLED");
            }

            return result instanceof Boolean booleanResult && booleanResult;
        }
    }

    private static final class ReschedulingTaskHandle implements CancellableTaskHandle {

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile CancellableTaskHandle delegate = CancellableTaskHandle.noop();

        @Override
        public boolean cancel() {
            final boolean changed = cancelled.compareAndSet(false, true);
            delegate.cancel();
            return changed;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get() || delegate.isCancelled();
        }

        private void setDelegate(final @NotNull CancellableTaskHandle delegate) {
            if (cancelled.get()) {
                delegate.cancel();
                return;
            }
            this.delegate = delegate;
        }
    }
}
