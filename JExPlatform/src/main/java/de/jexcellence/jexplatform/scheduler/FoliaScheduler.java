package de.jexcellence.jexplatform.scheduler;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Folia scheduler binding to region-aware APIs entirely via reflection.
 *
 * <p>Only instantiated when {@link de.jexcellence.jexplatform.server.ServerType.Folia}
 * is detected. Falls back to global execution when a specific API is
 * unavailable, keeping the platform operational across Folia versions.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class FoliaScheduler implements PlatformScheduler {

    private static final JExLogger LOG = JExLogger.of("Scheduler");
    private final JavaPlugin plugin;

    /**
     * Creates a Folia-aware scheduler bound to the given plugin.
     *
     * @param plugin owning plugin for scheduling calls
     */
    FoliaScheduler(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    @Override
    public void runSync(@NotNull Runnable task) {
        var global = globalScheduler();
        if (invoke(global, "run", sig(Plugin.class, Consumer.class), plugin, wrapConsumer(task)) == null
                && invoke(global, "execute", sig(Plugin.class, Runnable.class), plugin, guard(task)) == null) {
            guard(task).run();
        }
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        CompletableFuture.runAsync(guard(task));
    }

    @Override
    public @NotNull TaskHandle runDelayed(@NotNull Runnable task, long delayTicks) {
        var global = globalScheduler();
        var scheduled = invoke(global, "runDelayed",
                sig(Plugin.class, Consumer.class, long.class),
                plugin, wrapConsumer(task), delayTicks);

        if (scheduled != null) return reflectiveHandle(scheduled);

        // Fallback: single-shot via runAtFixedRate with unreachable period
        var handleRef = new AtomicReference<TaskHandle>();
        var repeating = invoke(global, "runAtFixedRate",
                sig(Plugin.class, Consumer.class, long.class, long.class),
                plugin, wrapConsumer(() -> {
                    var h = handleRef.get();
                    if (h != null) h.cancel();
                    guard(task).run();
                }), delayTicks, Long.MAX_VALUE / 4);

        if (repeating != null) {
            var h = reflectiveHandle(repeating);
            handleRef.set(h);
            return h;
        }

        runSync(task);
        return TaskHandle.noop();
    }

    @Override
    public @NotNull TaskHandle runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        var global = globalScheduler();
        var scheduled = invoke(global, "runAtFixedRate",
                sig(Plugin.class, Consumer.class, long.class, long.class),
                plugin, wrapConsumer(task), delayTicks, periodTicks);

        if (scheduled != null) return reflectiveHandle(scheduled);

        var handle = new ReschedulingHandle();
        reschedule(handle, task, delayTicks, periodTicks, false);
        return handle;
    }

    @Override
    public @NotNull TaskHandle runRepeatingAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        var async = asyncScheduler();
        var scheduled = invoke(async, "runAtFixedRate",
                sig(Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class),
                plugin, wrapConsumer(task), ticksToMs(delayTicks), ticksToMs(periodTicks), TimeUnit.MILLISECONDS);

        if (scheduled != null) return reflectiveHandle(scheduled);

        var handle = new ReschedulingHandle();
        reschedule(handle, task, delayTicks, periodTicks, true);
        return handle;
    }

    @Override
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        var scheduler = entityScheduler(entity);
        if (invoke(scheduler, "run",
                sig(Plugin.class, Consumer.class, Runnable.class),
                plugin, wrapConsumer(task), null) != null) return;

        if (invoke(scheduler, "runDelayed",
                sig(Plugin.class, Consumer.class, Runnable.class, long.class),
                plugin, wrapConsumer(task), null, 1L) != null) return;

        runSync(task);
    }

    @Override
    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        var region = regionScheduler();
        if (invoke(region, "run",
                sig(Plugin.class, Location.class, Consumer.class),
                plugin, location, wrapConsumer(task)) == null) {
            runSync(task);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
        return CompletableFuture.runAsync(guard(task));
    }

    // ── Scheduler resolution ────────────────────────────────────────────────────

    private @Nullable Object globalScheduler() {
        try {
            return Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        } catch (Throwable t) {
            LOG.error("GlobalRegionScheduler unavailable", t);
            return null;
        }
    }

    private @Nullable Object regionScheduler() {
        try {
            return Bukkit.class.getMethod("getRegionScheduler").invoke(null);
        } catch (Throwable t) {
            LOG.error("RegionScheduler unavailable", t);
            return null;
        }
    }

    private @Nullable Object asyncScheduler() {
        try {
            return Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
        } catch (Throwable t) {
            LOG.error("AsyncScheduler unavailable", t);
            return null;
        }
    }

    private @Nullable Object entityScheduler(@NotNull Entity entity) {
        try {
            return entity.getClass().getMethod("getScheduler").invoke(entity);
        } catch (Throwable t) {
            LOG.error("EntityScheduler unavailable for {}", entity);
            return null;
        }
    }

    // ── Reflection helpers ──────────────────────────────────────────────────────

    private @Nullable Object invoke(@Nullable Object target, String method,
                                    Class<?>[] params, Object... args) {
        if (target == null) return null;
        try {
            var m = findMethod(target.getClass(), method, params);
            if (m == null) return null;
            return m.invoke(target, args);
        } catch (Throwable t) {
            LOG.error("Reflective call failed: {}", method);
            return null;
        }
    }

    private @Nullable Method findMethod(Class<?> clazz, String name, Class<?>[] params) {
        try {
            var m = clazz.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {
            // Fallback: match by name + parameter count
            for (var m : clazz.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == params.length) {
                    m.setAccessible(true);
                    return m;
                }
            }
            return null;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Creates a {@link Consumer} proxy compatible with Folia's scheduler APIs
     * without binding to the {@code ScheduledTask} type at compile time.
     */
    @SuppressWarnings("unchecked")
    private Object wrapConsumer(@NotNull Runnable task) {
        return Proxy.newProxyInstance(
                Consumer.class.getClassLoader(),
                new Class[]{Consumer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "accept" -> {
                        guard(task).run();
                        yield null;
                    }
                    case "toString" -> "FoliaSchedulerProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args != null ? args[0] : null);
                    default -> null;
                }
        );
    }

    private Runnable guard(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOG.error("Scheduled task failed", t);
                throw t;
            }
        };
    }

    private static Class<?>[] sig(Class<?>... types) {
        return types;
    }

    private static long ticksToMs(long ticks) {
        return Math.max(1L, ticks) * 50L;
    }

    private void reschedule(ReschedulingHandle handle, Runnable task,
                            long delayTicks, long periodTicks, boolean async) {
        if (handle.isCancelled()) return;
        handle.delegate(runDelayed(() -> {
            if (handle.isCancelled()) return;
            if (async) {
                runAsync(task);
            } else {
                guard(task).run();
            }
            reschedule(handle, task, periodTicks, periodTicks, async);
        }, delayTicks));
    }

    private TaskHandle reflectiveHandle(@NotNull Object scheduled) {
        return new TaskHandle() {
            private final AtomicBoolean cancelled = new AtomicBoolean();

            @Override
            public boolean cancel() {
                if (isCancelled()) return false;
                invoke(scheduled, "cancel", new Class[0]);
                cancelled.set(true);
                return true;
            }

            @Override
            public boolean isCancelled() {
                if (cancelled.get()) return true;
                var result = invoke(scheduled, "isCancelled", new Class[0]);
                if (result instanceof Boolean b && b) cancelled.set(true);
                return cancelled.get();
            }
        };
    }

    // ── Rescheduling handle ─────────────────────────────────────────────────────

    private static final class ReschedulingHandle implements TaskHandle {

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile TaskHandle current = TaskHandle.noop();

        @Override
        public boolean cancel() {
            var changed = cancelled.compareAndSet(false, true);
            current.cancel();
            return changed;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        void delegate(@NotNull TaskHandle next) {
            if (cancelled.get()) {
                next.cancel();
                return;
            }
            this.current = next;
        }
    }
}
