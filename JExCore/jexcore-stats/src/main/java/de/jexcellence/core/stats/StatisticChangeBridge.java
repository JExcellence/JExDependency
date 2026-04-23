package de.jexcellence.core.stats;

import de.jexcellence.core.api.event.StatisticChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges {@link StatisticChangeEvent} (fired by {@code StatisticService}
 * in {@code jexcore-common}) into the {@link StatisticsDelivery} queue.
 */
public final class StatisticChangeBridge implements Listener {

    private final StatisticsDelivery delivery;

    public StatisticChangeBridge(@NotNull StatisticsDelivery delivery) {
        this.delivery = delivery;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStatisticChange(@NotNull StatisticChangeEvent event) {
        this.delivery.push(StatisticEntry.of(
                event.pluginNamespace(),
                event.identifier(),
                event.playerId(),
                event.value()
        ));
    }
}
