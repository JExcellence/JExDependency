package de.jexcellence.quests.listener;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.event.BountyClaimedEvent;
import de.jexcellence.quests.api.event.PerkActivatedEvent;
import de.jexcellence.quests.api.event.QuestCompletedEvent;
import de.jexcellence.quests.api.event.RankPromotedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Audio/visual feedback for the four main lifecycle events:
 * quest completion, rank promotion, bounty claim, perk activation.
 *
 * <p>Feedback is ephemeral UI (title / subtitle / sound) rather than
 * chat — MiniMessage templates are inlined here with the ecosystem
 * gradient palette. Every effect dispatches back to the main thread
 * since the JExQuests api events fire async.
 */
public final class FeedbackListener implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final Title.Times QUICK = Title.Times.times(
            Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600));
    private static final Title.Times SLOW = Title.Times.times(
            Duration.ofMillis(400), Duration.ofMillis(3500), Duration.ofMillis(800));

    private final JExQuests quests;
    private final JExLogger logger;

    public FeedbackListener(@NotNull JExQuests quests) {
        this.quests = quests;
        this.logger = quests.logger();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuestCompleted(@NotNull QuestCompletedEvent event) {
        runForPlayer(event.playerUuid(), player -> {
            player.showTitle(Title.title(
                    mini("<gradient:#86efac:#16a34a>★</gradient> <gradient:#fde047:#f59e0b>{quest}</gradient>",
                            Map.of("quest", event.quest().displayName())),
                    mini("<gray>Quest complete — category <white>{category}</white>",
                            Map.of("category", event.quest().category())),
                    SLOW));
            player.playSound(player.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1.2, 0), 24, 0.6, 0.6, 0.6, 0.0);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRankPromoted(@NotNull RankPromotedEvent event) {
        runForPlayer(event.snapshot().playerUuid(), player -> {
            player.showTitle(Title.title(
                    mini("<gradient:#d8b4fe:#9333ea>★</gradient> <gradient:#fde047:#f59e0b>{rank}</gradient>",
                            Map.of("rank", event.snapshot().rankIdentifier())),
                    mini("<gray>Promoted from <white>{previous}</white> on <white>{tree}</white>",
                            Map.of(
                                    "previous", event.previousRankIdentifier(),
                                    "tree", event.snapshot().treeIdentifier())),
                    SLOW));
            player.playSound(player.getLocation(),
                    Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 0.9f, 1.0f);
            player.playSound(player.getLocation(),
                    Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 0.7f, 1.1f);
            // Ascending column of end-rod sparkles for promotion flair.
            for (int step = 0; step < 5; step++) {
                final double y = step * 0.4;
                player.getWorld().spawnParticle(Particle.END_ROD,
                        player.getLocation().add(0, y + 0.2, 0), 8, 0.25, 0.1, 0.25, 0.02);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBountyClaimed(@NotNull BountyClaimedEvent event) {
        runForPlayer(event.killerUuid(), player -> {
            player.showTitle(Title.title(
                    mini("<gradient:#fca5a5:#dc2626>☠</gradient> <gradient:#fde047:#f59e0b>+{amount} {currency}</gradient>",
                            Map.of(
                                    "amount", String.valueOf(event.bounty().amount()),
                                    "currency", event.bounty().currency())),
                    mini("<gray>Bounty claimed on <white>{target}</white>",
                            Map.of("target", nameOf(event.bounty().targetUuid()))),
                    QUICK));
            player.playSound(player.getLocation(),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.2f);
            player.playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.6f, 1.4f);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    player.getLocation().add(0, 1.0, 0), 30, 0.5, 0.5, 0.5, 0.03);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPerkActivated(@NotNull PerkActivatedEvent event) {
        runForPlayer(event.snapshot().playerUuid(), player -> {
            player.playSound(player.getLocation(),
                    Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.MASTER, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.ENCHANT,
                    player.getLocation().add(0, 1.2, 0), 24, 0.4, 0.4, 0.4, 0.8);
        });
    }

    /**
     * Runs the consumer on the main thread if the referenced player is
     * online. JExQuests events fire async; titles / sounds must be
     * dispatched synchronously per Bukkit's threading contract.
     */
    private void runForPlayer(@NotNull UUID uuid, @NotNull Consumer<Player> action) {
        Bukkit.getScheduler().runTask(this.quests.getPlugin(), () -> {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            try {
                action.accept(player);
            } catch (final RuntimeException ex) {
                this.logger.error("feedback dispatch failed for {}: {}", uuid, ex.getMessage());
            }
        });
    }

    private static @NotNull Component mini(@NotNull String template, @NotNull Map<String, String> placeholders) {
        String rendered = template;
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        try {
            return MINI.deserialize(rendered);
        } catch (final RuntimeException ex) {
            return Component.text(rendered);
        }
    }

    private static @NotNull String nameOf(@NotNull UUID uuid) {
        final var offline = Bukkit.getOfflinePlayer(uuid);
        final var name = offline.getName();
        return name != null ? name : uuid.toString();
    }
}
