package de.jexcellence.core.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired asynchronously after {@code StatisticService} persists a statistic
 * value. Consumers can subscribe to bridge the change into delivery
 * pipelines, dashboards, or remote analytics.
 */
public class StatisticChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String plugin;
    private final String identifier;
    private final UUID playerId;
    private final Object value;

    /**
     * Creates the event.
     *
     * @param plugin plugin namespace that owns the statistic
     * @param identifier stable identifier within that namespace
     * @param playerId owning player, or {@code null} for server-scoped statistics
     * @param value the new value
     */
    public StatisticChangeEvent(
            @NotNull String plugin,
            @NotNull String identifier,
            @Nullable UUID playerId,
            @NotNull Object value
    ) {
        super(true);
        this.plugin = plugin;
        this.identifier = identifier;
        this.playerId = playerId;
        this.value = value;
    }

    public @NotNull String pluginNamespace() {
        return this.plugin;
    }

    public @NotNull String identifier() {
        return this.identifier;
    }

    public @Nullable UUID playerId() {
        return this.playerId;
    }

    public @NotNull Object value() {
        return this.value;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
