package de.jexcellence.quests.integration;

import de.jexcellence.core.stats.StatisticEntry;
import de.jexcellence.core.stats.StatisticPriority;
import de.jexcellence.core.stats.StatisticsDelivery;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.api.event.BountyClaimedEvent;
import de.jexcellence.quests.api.event.PerkActivatedEvent;
import de.jexcellence.quests.api.event.QuestAcceptedEvent;
import de.jexcellence.quests.api.event.QuestCompletedEvent;
import de.jexcellence.quests.api.event.RankPromotedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Forwards JExQuests lifecycle events into JExCore's
 * {@link StatisticsDelivery} queue. Every event becomes one
 * {@link StatisticEntry}; delivery rate limiting, batching, and
 * retries are handled by JExCore.
 *
 * <p>Registered on the Bukkit plugin manager by the orchestrator
 * only when the {@code StatisticsDelivery} service is present on
 * the {@code ServicesManager} — otherwise telemetry is a no-op and
 * no listener is attached.
 */
public final class StatisticsBridge implements Listener {

    private static final String SOURCE = "JExQuests";

    private final JavaPlugin plugin;
    private final JExLogger logger;

    public StatisticsBridge(@NotNull JavaPlugin plugin, @NotNull JExLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /** Installs the listener if {@link StatisticsDelivery} is available. */
    public boolean install() {
        if (locate() == null) {
            this.logger.info("JExCore statistics service not available; telemetry disabled");
            return false;
        }
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        this.logger.info("Telemetry bridge online — forwarding JExQuests events to JExCore statistics");
        return true;
    }

    /**
     * Handles quest accepted event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuestAccepted(@NotNull QuestAcceptedEvent event) {
        push("quest.accepted", event.playerUuid(), event.quest().identifier(),
                Map.of("category", event.quest().category(), "difficulty", event.quest().difficulty()),
                StatisticPriority.NORMAL);
    }

    /**
     * Handles quest completed event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuestCompleted(@NotNull QuestCompletedEvent event) {
        push("quest.completed", event.playerUuid(), event.quest().identifier(),
                Map.of("category", event.quest().category(),
                        "difficulty", event.quest().difficulty(),
                        "count", String.valueOf(event.completionCount())),
                StatisticPriority.HIGH);
    }

    /**
     * Handles rank promoted event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRankPromoted(@NotNull RankPromotedEvent event) {
        push("rank.promoted", event.snapshot().playerUuid(), event.snapshot().rankIdentifier(),
                Map.of("tree", event.snapshot().treeIdentifier(),
                        "previous", event.previousRankIdentifier(),
                        "order", String.valueOf(event.snapshot().orderIndex())),
                StatisticPriority.HIGH);
    }

    /**
     * Handles bounty claimed event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBountyClaimed(@NotNull BountyClaimedEvent event) {
        push("bounty.claimed", event.killerUuid(), event.bounty().amount(),
                Map.of("target", event.bounty().targetUuid().toString(),
                        "issuer", event.bounty().issuerUuid().toString(),
                        "currency", event.bounty().currency()),
                StatisticPriority.HIGH);
    }

    /**
     * Handles perk activated event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPerkActivated(@NotNull PerkActivatedEvent event) {
        push("perk.activated", event.snapshot().playerUuid(), event.snapshot().perkIdentifier(),
                Map.of("kind", event.snapshot().kind(),
                        "count", String.valueOf(event.snapshot().activationCount())),
                StatisticPriority.NORMAL);
    }

    private void push(
            @NotNull String identifier,
            @NotNull UUID playerUuid,
            @NotNull Object value,
            @NotNull Map<String, String> attributes,
            @NotNull StatisticPriority priority
    ) {
        final StatisticsDelivery delivery = locate();
        if (delivery == null) return;
        delivery.push(new StatisticEntry(
                SOURCE, identifier, playerUuid, value, attributes, java.time.Instant.now(), priority));
    }

    private @Nullable StatisticsDelivery locate() {
        return Bukkit.getServicesManager().load(StatisticsDelivery.class);
    }
}
