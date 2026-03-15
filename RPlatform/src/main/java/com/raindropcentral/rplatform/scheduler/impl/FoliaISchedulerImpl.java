package com.raindropcentral.rplatform.scheduler.impl;

import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
    public void runDelayed(@NotNull Runnable task, long delayTicks) {
        final Object global = getGlobalScheduler();
        if (!tryInvoke(global, "runDelayed", new Class[]{JavaPlugin.class, Consumer.class, long.class}, new Object[]{plugin, toConsumer(task), delayTicks})) {
            tryInvoke(global, "runAtFixedRate",
                    new Class[]{JavaPlugin.class, Consumer.class, long.class, long.class},
                    new Object[]{plugin, toConsumer(task), delayTicks, Long.MAX_VALUE / 4});
        }
    }

    /**
     * {@inheritDoc}.
 *
 * <p>Uses {@code GlobalRegionScheduler#runAtFixedRate} and falls back to recursively scheduling when the
     * API is missing, preserving approximate cadence.
     */
    @Override
    public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        final Object global = getGlobalScheduler();
        if (!tryInvoke(global, "runAtFixedRate", new Class[]{JavaPlugin.class, Consumer.class, long.class, long.class},
                new Object[]{plugin, toConsumer(task), delayTicks, periodTicks})) {
            runDelayed(() -> runRepeating(task, periodTicks, periodTicks), delayTicks);
        }
    }

    /**
     * Executes runRepeatingAsync.
     */
    @Override
    public void runRepeatingAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        final Object global = getAsyncScheduler();
        if (!tryInvoke(global, "runAtFixedRate", new Class[]{JavaPlugin.class, Consumer.class, long.class, long.class},
                new Object[]{plugin, toConsumer(task), delayTicks, periodTicks})) {
            runDelayed(() -> runRepeatingAsync(task, periodTicks, periodTicks), delayTicks);
        }
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
        if (!tryInvoke(entityScheduler, "run", new Class[]{Consumer.class}, new Object[]{toConsumer(task)})) {
            tryInvoke(entityScheduler, "runDelayed", new Class[]{Consumer.class, long.class}, new Object[]{toConsumer(task), 0L});
        }
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
        if (!tryInvoke(region, "run", new Class[]{JavaPlugin.class, Location.class, Consumer.class},
                new Object[]{plugin, location, toConsumer(task)})) {
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
        if (!tryInvoke(global, "run", new Class[]{JavaPlugin.class, Consumer.class}, new Object[]{plugin, toConsumer(task)})) {
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
    private boolean tryInvoke(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        if (target == null) return false;
        try {
            final Method m = findMethod(target.getClass(), methodName, paramTypes);
            if (m == null) return false;
            m.invoke(target, args);
            return true;
        } catch (InvocationTargetException ite) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] Invocation failed for " + methodName, ite.getTargetException());
            return false;
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] Invocation failed for " + methodName, t);
            return false;
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
}
