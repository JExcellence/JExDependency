package de.jexcellence.quests.machine;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.MachineSnapshot;
import de.jexcellence.quests.api.event.MachineTickEvent;
import de.jexcellence.quests.database.entity.Machine;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Async tick scheduler for placed machines.
 *
 * <p>Every {@link #TICK_INTERVAL_TICKS} server ticks the scheduler
 * iterates every registered {@link MachineType}, loads its live
 * machines, and — for each machine whose per-type
 * {@code tick-interval-ticks} elapsed since its last tick — fires a
 * {@link MachineTickEvent}. Downstream plugins subscribe to implement
 * recipes / processing.
 *
 * <p>The scheduler is self-contained and safe to start/stop multiple
 * times. All work happens off the main thread; event dispatch is
 * intentionally async so subscribers can touch the database without
 * ping-ponging back to Bukkit.
 */
public final class MachineTickScheduler {

    /** How often the scheduler wakes up to consider all machines, in server ticks. */
    private static final long TICK_INTERVAL_TICKS = 20L;

    /** Default per-machine tick interval when the type doesn't declare one. */
    private static final long DEFAULT_TICK_INTERVAL_TICKS = 40L;

    private final JavaPlugin plugin;
    private final JExQuests quests;
    private final MachineRegistry registry;
    private final JExLogger logger;

    /** machineId → last-tick counter (monotonic). */
    private final ConcurrentMap<Long, Long> lastTickCounter = new ConcurrentHashMap<>();

    /** machineId → cumulative ticks emitted for this machine. */
    private final ConcurrentMap<Long, Long> tickCounts = new ConcurrentHashMap<>();

    private BukkitTask task;
    private long schedulerTick;

    public MachineTickScheduler(@NotNull JExQuests quests) {
        this.plugin = quests.getPlugin();
        this.quests = quests;
        this.registry = quests.machineRegistry();
        this.logger = quests.logger();
    }

    public void start() {
        if (this.task != null) return;
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this.plugin, this::tick, TICK_INTERVAL_TICKS, TICK_INTERVAL_TICKS);
        this.logger.info("Machine tick scheduler online — every {}t", TICK_INTERVAL_TICKS);
    }

    public void stop() {
        if (this.task == null) return;
        this.task.cancel();
        this.task = null;
        this.lastTickCounter.clear();
        this.tickCounts.clear();
        this.logger.info("Machine tick scheduler stopped");
    }

    private void tick() {
        this.schedulerTick++;
        if (this.registry.size() == 0) return;

        for (final MachineType type : this.registry.all()) {
            final long interval = perTypeInterval(type);
            this.quests.machineService().ofTypeAsync(type.identifier()).thenAccept(machines ->
                    dispatchForType(type, interval, machines)
            ).exceptionally(ex -> {
                this.logger.error("tick fetch failed for type {}: {}", type.identifier(), ex.getMessage());
                return null;
            });
        }
    }

    private void dispatchForType(
            @NotNull MachineType type,
            long interval,
            @NotNull List<Machine> machines
    ) {
        final long current = this.schedulerTick;
        for (final Machine m : machines) {
            if (m.getId() == null) continue;
            final long id = m.getId();
            final long last = this.lastTickCounter.getOrDefault(id, -1L);
            if (last >= 0 && (current - last) * TICK_INTERVAL_TICKS < interval) continue;
            this.lastTickCounter.put(id, current);
            final long count = this.tickCounts.merge(id, 1L, Long::sum);
            fireEvent(m, type, count);
        }
    }

    private void fireEvent(@NotNull Machine m, @NotNull MachineType type, long count) {
        final MachineSnapshot snapshot = new MachineSnapshot(
                m.getId() != null ? m.getId() : 0L,
                m.getOwnerUuid(),
                m.getMachineType(),
                m.getWorld(),
                m.getX(), m.getY(), m.getZ(),
                m.getFacing()
        );
        try {
            Bukkit.getPluginManager().callEvent(new MachineTickEvent(snapshot, count));
        } catch (final RuntimeException ex) {
            this.logger.error("MachineTickEvent handler failed for {}: {}",
                    type.identifier(), ex.getMessage());
        }
    }

    private static long perTypeInterval(@NotNull MachineType type) {
        final Object raw = type.properties().get("tick-interval-ticks");
        if (raw instanceof Number n) {
            final long value = n.longValue();
            if (value > 0) return value;
        }
        return DEFAULT_TICK_INTERVAL_TICKS;
    }

    /** Returns the total tick events fired for the given machine id. */
    public long ticksFor(long machineId) {
        return this.tickCounts.getOrDefault(machineId, 0L);
    }

    /** Returns a snapshot of the current tick-count map (for /jexquests status). */
    public @NotNull Map<Long, Long> snapshot() {
        return Map.copyOf(this.tickCounts);
    }
}
