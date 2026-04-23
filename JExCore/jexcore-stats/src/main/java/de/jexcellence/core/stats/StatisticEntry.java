package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One statistic event queued for delivery. Immutable; {@link #attributes}
 * is expected to contain only JSON-safe primitives and strings.
 *
 * @param plugin plugin namespace claiming the statistic
 * @param identifier stable statistic identifier
 * @param playerId owning player, or {@code null} for server-scoped events
 * @param value statistic value — Number, Boolean, String, or Instant
 * @param attributes optional key/value tags
 * @param timestamp event timestamp
 * @param priority delivery priority
 */
public record StatisticEntry(
        @NotNull String plugin,
        @NotNull String identifier,
        @Nullable UUID playerId,
        @NotNull Object value,
        @NotNull Map<String, String> attributes,
        @NotNull Instant timestamp,
        @NotNull StatisticPriority priority
) {
    public StatisticEntry {
        attributes = Map.copyOf(attributes);
    }

    public static @NotNull StatisticEntry of(
            @NotNull String plugin,
            @NotNull String identifier,
            @Nullable UUID playerId,
            @NotNull Object value
    ) {
        return new StatisticEntry(plugin, identifier, playerId, value, Map.of(), Instant.now(), StatisticPriority.NORMAL);
    }
}
