package de.jexcellence.quests.integration;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for JExQuests. Registers on enable only
 * when PlaceholderAPI is present — otherwise the integration is a
 * silent no-op.
 *
 * <p>Supported placeholders:
 * <ul>
 *   <li>{@code %jexquests_version%} — plugin version (static)</li>
 *   <li>{@code %jexquests_edition%} — edition name (Free / Premium)</li>
 *   <li>{@code %jexquests_tracked_quest%} — player's pinned quest, or {@code —}</li>
 *   <li>{@code %jexquests_active_rank_tree%} — player's chosen rank path, or {@code —}</li>
 *   <li>{@code %jexquests_perk_sidebar%} — {@code on} / {@code off}</li>
 *   <li>{@code %jexquests_quest_sidebar%} — {@code on} / {@code off}</li>
 *   <li>{@code %jexquests_active_quests%} — count of active quests</li>
 *   <li>{@code %jexquests_owned_perks%} — count of owned perks</li>
 * </ul>
 *
 * <p>PlaceholderAPI resolution is synchronous (called during chat
 * rendering, scoreboard updates, etc.); async service calls use a
 * short bounded wait — slow queries fall through to a placeholder
 * fallback so bad network latency doesn't freeze chat.
 */
public final class QuestsPlaceholderExpansion extends PlaceholderExpansion {

    private static final Duration SYNC_TIMEOUT = Duration.ofMillis(80);
    private static final String FALLBACK = "—";

    private final JExQuests quests;
    private final JExLogger logger;

    public QuestsPlaceholderExpansion(@NotNull JExQuests quests) {
        this.quests = quests;
        this.logger = quests.logger();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jexquests";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JExcellence";
    }

    @Override
    public @NotNull String getVersion() {
        return this.quests.version();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        try {
            return switch (params) {
                case "version" -> this.quests.version();
                case "edition" -> this.quests.edition();
                case "tracked_quest" -> withPlayer(player, uuid -> playerRow(uuid)
                        .map(QuestsPlayer::getTrackedQuestIdentifier).orElse(null));
                case "active_rank_tree" -> withPlayer(player, uuid -> playerRow(uuid)
                        .map(QuestsPlayer::getActiveRankTree).orElse(null));
                case "perk_sidebar" -> withPlayer(player, uuid -> onOff(playerRow(uuid)
                        .map(QuestsPlayer::isPerkSidebarEnabled).orElse(false)));
                case "quest_sidebar" -> withPlayer(player, uuid -> onOff(playerRow(uuid)
                        .map(QuestsPlayer::isQuestSidebarEnabled).orElse(false)));
                case "active_quests" -> withPlayer(player, this::countActiveQuests);
                case "owned_perks" -> withPlayer(player, this::countOwnedPerks);
                default -> null;
            };
        } catch (final RuntimeException ex) {
            this.logger.warn("placeholder {} failed: {}", params, ex.getMessage());
            return FALLBACK;
        }
    }

    /** Installs the expansion if PlaceholderAPI is loaded. Returns {@code true} on success. */
    public boolean install() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            this.logger.info("PlaceholderAPI not installed; placeholders disabled");
            return false;
        }
        final boolean registered = register();
        if (registered) this.logger.info("PlaceholderAPI expansion registered: %jexquests_*%");
        else this.logger.warn("PlaceholderAPI expansion registration failed");
        return registered;
    }

    private @NotNull String withPlayer(@Nullable OfflinePlayer player, @NotNull PlayerResolver resolver) {
        if (player == null) return FALLBACK;
        final String resolved = resolver.resolve(player.getUniqueId());
        return resolved != null && !resolved.isBlank() ? resolved : FALLBACK;
    }

    private @NotNull Optional<QuestsPlayer> playerRow(@NotNull UUID uuid) {
        try {
            return this.quests.questsPlayerService().findAsync(uuid)
                    .orTimeout(SYNC_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .join();
        } catch (final RuntimeException ex) {
            return Optional.empty();
        }
    }

    private @NotNull String countActiveQuests(@NotNull UUID uuid) {
        try {
            final var list = this.quests.questService().activeForPlayerAsync(uuid)
                    .orTimeout(SYNC_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .join();
            return Integer.toString(list.size());
        } catch (final RuntimeException ex) {
            return "0";
        }
    }

    private @NotNull String countOwnedPerks(@NotNull UUID uuid) {
        try {
            final var list = this.quests.perkService().ownedAsync(uuid)
                    .orTimeout(SYNC_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .join();
            return Integer.toString(list.size());
        } catch (final RuntimeException ex) {
            return "0";
        }
    }

    private static @NotNull String onOff(boolean value) {
        return value ? "on" : "off";
    }

    @FunctionalInterface
    private interface PlayerResolver {
        @Nullable String resolve(@NotNull UUID uuid);
    }
}
