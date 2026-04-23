package de.jexcellence.quests.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Thread-aware event dispatch. Bukkit's {@link Event} class has a
 * single {@code isAsynchronous()} flag that's checked at call time —
 * firing a sync event from an async thread (or vice versa) throws
 * {@link IllegalStateException}. Our lifecycle events (quest accept /
 * complete, rank promote, perk activate, bounty claim) are declared
 * synchronous because their handlers (feedback listener, statistics
 * bridge) run cheap UI work. But the <em>service</em> layer that fires
 * them straddles threads — it's called from both main-thread GUI
 * clicks and async CompletableFuture chains — so a fixed declaration
 * is always wrong on half the call sites.
 *
 * <p>This helper bridges the gap: when fired from the main thread, it
 * calls through synchronously (no task-schedule latency); when fired
 * off-thread, it schedules the call onto the main thread via the
 * stored plugin handle. Listeners never see the async case.
 *
 * <p>Must be {@link #install(Plugin)}ed on plugin enable before any
 * service-layer event fires. In JExQuests this happens in
 * {@code onEnable()} before {@code registerServices()}.
 */
public final class EventDispatch {

    private static volatile Plugin pluginRef;

    private EventDispatch() {
    }

    public static void install(@NotNull Plugin plugin) {
        pluginRef = plugin;
    }

    /**
     * Fire an event from any thread. Main-thread callers see a direct
     * synchronous dispatch; async callers see a scheduled dispatch on
     * the next server tick. Without an {@link #install}ed plugin, falls
     * back to direct dispatch and trusts the caller's thread.
     */
    public static void fire(@NotNull Event event) {
        final Plugin plugin = pluginRef;
        if (plugin == null || Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }
}
