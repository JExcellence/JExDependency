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
 * Folia scheduler adapter that uses Folia's region/global/entity schedulers via reflection.
 * This avoids hard dependencies so the class is only loaded on Folia servers.
 *
 * Methods prefer the following reflective calls (if present):
 * - GlobalRegionScheduler: run(plugin, Consumer), runDelayed(plugin, Consumer, long), runAtFixedRate(plugin, Consumer, long, long)
 * - RegionScheduler: run(plugin, Location, Consumer), runDelayed(plugin, Location, Consumer, long), runAtFixedRate(plugin, Location, Consumer, long, long)
 * - EntityScheduler: run(Consumer), runDelayed(Consumer, long), runAtFixedRate(Consumer, long, long)
 *
 * If a specific method isn't found, we degrade gracefully where possible (e.g., run + manual delay using runAtFixedRate with large period).
 */
public class FoliaISchedulerImpl implements ISchedulerAdapter {

    private static final Logger LOG = Logger.getLogger("RPlatform");
    private final JavaPlugin plugin;

    public FoliaISchedulerImpl(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------
    // Public API
    // -------------------------

    @Override
    public void runSync(@NotNull Runnable task) {
        runGlobal(task);
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        // Use a simple async execution; Folia also provides AsyncScheduler, but reflection is simpler for broad compatibility.
        CompletableFuture.runAsync(safe(task));
    }

    @Override
    public void runDelayed(@NotNull Runnable task, long delayTicks) {
        final Object global = getGlobalScheduler();
        if (!tryInvoke(global, "runDelayed", new Class[]{JavaPlugin.class, Consumer.class, long.class}, new Object[]{plugin, toConsumer(task), delayTicks})) {
            // Fallback: schedule at fixed rate with huge period so it effectively runs once.
            tryInvoke(global, "runAtFixedRate",
                    new Class[]{JavaPlugin.class, Consumer.class, long.class, long.class},
                    new Object[]{plugin, toConsumer(task), delayTicks, Long.MAX_VALUE / 4});
        }
    }

    @Override
    public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        final Object global = getGlobalScheduler();
        if (!tryInvoke(global, "runAtFixedRate", new Class[]{JavaPlugin.class, Consumer.class, long.class, long.class},
                new Object[]{plugin, toConsumer(task), delayTicks, periodTicks})) {
            // If fixed rate isn't available, run once then re-schedule recursively (coarse fallback).
            runDelayed(() -> runRepeating(task, periodTicks, periodTicks), delayTicks);
        }
    }

    @Override
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        final Object entityScheduler = getEntityScheduler(entity);
        if (!tryInvoke(entityScheduler, "run", new Class[]{Consumer.class}, new Object[]{toConsumer(task)})) {
            // Fallback: try delayed with zero
            tryInvoke(entityScheduler, "runDelayed", new Class[]{Consumer.class, long.class}, new Object[]{toConsumer(task), 0L});
        }
    }

    @Override
    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        final Object region = getRegionScheduler();
        if (!tryInvoke(region, "run", new Class[]{JavaPlugin.class, Location.class, Consumer.class},
                new Object[]{plugin, location, toConsumer(task)})) {
            // Fallback: schedule globally
            runGlobal(task);
        }
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        final Object global = getGlobalScheduler();
        if (!tryInvoke(global, "run", new Class[]{JavaPlugin.class, Consumer.class}, new Object[]{plugin, toConsumer(task)})) {
            // Fallback: immediate
            safe(task).run();
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
        // Use JDK async; callers get completion/error propagation either way.
        return CompletableFuture.runAsync(safe(task));
    }

    // -------------------------
    // Reflection helpers
    // -------------------------

    private Object getGlobalScheduler() {
        try {
            final Method m = Bukkit.class.getMethod("getGlobalRegionScheduler");
            return m.invoke(null);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] GlobalRegionScheduler not available", t);
            return null;
        }
    }

    private Object getRegionScheduler() {
        try {
            final Method m = Bukkit.class.getMethod("getRegionScheduler");
            return m.invoke(null);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] RegionScheduler not available", t);
            return null;
        }
    }

    private Object getEntityScheduler(Entity entity) {
        try {
            final Method m = entity.getClass().getMethod("getScheduler");
            return m.invoke(entity);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "[Scheduler/Folia] EntityScheduler not available for " + entity, t);
            return null;
        }
    }

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

    private Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try to locate by name only (best-effort, in case of signature drift)
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramTypes.length) {
                    return m;
                }
            }
            return null;
        }
    }

    /**
     * Creates a java.util.function.Consumer proxy without binding to Folia's ScheduledTask type.
     * The returned object implements Consumer and just runs the provided runnable when accept(...) is called.
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
                        // Handle default method contract for andThen by returning a composed Consumer proxy.
                        final Object next = (args != null && args.length == 1) ? args[0] : null;
                        return Proxy.newProxyInstance(
                                Consumer.class.getClassLoader(),
                                new Class[]{Consumer.class},
                                (p2, m2, a2) -> {
                                    if ("accept".equals(m2.getName()) && m2.getParameterCount() == 1) {
                                        safe(runnable).run();
                                        if (next instanceof Consumer<?> c) {
                                            // best-effort chain
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
                    // For equals/hashCode/toString or default interface methods
                    return MethodHandles.lookup().unreflect(method).bindTo(proxy).invokeWithArguments(args);
                }
        );
    }

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