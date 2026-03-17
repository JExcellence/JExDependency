/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdr.service.scoreboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import de.jexcellence.jextranslate.i18n.I18n;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Maintains the opt-in RDR sidebar scoreboard exposed through {@code /rr scoreboard}.
 *
 * <p>The service keeps at most one active sidebar per player, refreshes aggregate storage totals on a
 * repeating schedule, and restores the player's previous scoreboard when the sidebar is disabled.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageSidebarScoreboardService {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final long UPDATE_PERIOD_TICKS = 40L;
    private static final String OBJECTIVE_NAME = "rdrboard";
    private static final List<String> LINE_ENTRIES = List.of("\u00A70", "\u00A71", "\u00A72", "\u00A73");

    private final RDR plugin;
    private final Map<UUID, SidebarState> activeSidebars = new ConcurrentHashMap<>();

    /**
     * Creates the scoreboard service for the supplied plugin.
     *
     * @param plugin active RDR plugin instance
     */
    public StorageSidebarScoreboardService(
        final @NotNull RDR plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts periodic refreshes for every active storage sidebar.
     */
    public void start() {
        this.plugin.getScheduler().runRepeating(
            this::refreshActivePlayers,
            UPDATE_PERIOD_TICKS,
            UPDATE_PERIOD_TICKS
        );
    }

    /**
     * Disables every active storage sidebar and restores each player's previous scoreboard.
     */
    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.disable(player);
        }
        this.activeSidebars.clear();
    }

    /**
     * Enables the storage summary sidebar for the supplied player.
     *
     * @param player player who should receive the sidebar
     */
    public void enable(
        final @NotNull Player player
    ) {
        final SidebarState existingState = this.activeSidebars.get(player.getUniqueId());
        if (existingState != null) {
            player.setScoreboard(existingState.sidebarScoreboard());
            this.refreshPlayer(player, existingState);
            return;
        }

        final Scoreboard sidebarScoreboard = this.createSidebarScoreboard();
        final SidebarState state = new SidebarState(player.getScoreboard(), sidebarScoreboard);
        this.activeSidebars.put(player.getUniqueId(), state);
        player.setScoreboard(sidebarScoreboard);
        this.refreshPlayer(player, state);
    }

    /**
     * Disables the storage sidebar for the supplied player.
     *
     * @param player player whose sidebar should be removed
     */
    public void disable(
        final @NotNull Player player
    ) {
        final SidebarState state = this.activeSidebars.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (player.getScoreboard() == state.sidebarScoreboard()) {
            player.setScoreboard(state.previousScoreboard());
        }
    }

    /**
     * Returns whether the supplied player currently has the RDR storage sidebar enabled.
     *
     * @param player player to inspect
     * @return {@code true} when the player currently has an active storage sidebar
     */
    public boolean isActive(
        final @NotNull Player player
    ) {
        return this.activeSidebars.containsKey(player.getUniqueId());
    }

    private void refreshActivePlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final SidebarState state = this.activeSidebars.get(player.getUniqueId());
            if (state == null) {
                continue;
            }

            this.refreshPlayer(player, state);
        }
    }

    private void refreshPlayer(
        final @NotNull Player player,
        final @NotNull SidebarState state
    ) {
        if (!player.isOnline()) {
            this.activeSidebars.remove(player.getUniqueId());
            return;
        }

        if (player.getScoreboard() != state.sidebarScoreboard()) {
            player.setScoreboard(state.sidebarScoreboard());
        }

        this.renderSidebar(
            state.sidebarScoreboard(),
            player,
            this.buildLines(player)
        );
    }

    private @NotNull List<String> buildLines(
        final @NotNull Player player
    ) {
        final List<RStorage> storages = this.plugin.getStorageRepository() == null
            ? List.of()
            : this.plugin.getStorageRepository().findByPlayerUuid(player.getUniqueId());

        long totalItems = 0L;
        int totalAvailableSlots = 0;
        for (final RStorage storage : storages) {
            totalAvailableSlots += storage.getInventorySize() - storage.getStoredSlotCount();
            for (final var itemStack : storage.getInventory().values()) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    totalItems += itemStack.getAmount();
                }
            }
        }

        final int maxStorages = this.plugin.getMaximumStorages(player, this.plugin.getDefaultConfig());
        final String maxStoragesDisplay = maxStorages > 0
            ? Integer.toString(maxStorages)
            : this.i18nLine("scoreboard_sidebar.unlimited", player, Map.of());

        return List.of(
            this.i18nLine("scoreboard_sidebar.items", player, Map.of("total_items", totalItems)),
            this.i18nLine("scoreboard_sidebar.available_slots", player, Map.of("available_slots", totalAvailableSlots)),
            this.i18nLine("scoreboard_sidebar.storages_obtained", player, Map.of("owned_storages", storages.size())),
            this.i18nLine(
                "scoreboard_sidebar.max_storages",
                player,
                Map.of("max_storages", maxStoragesDisplay)
            )
        );
    }

    private @NotNull Scoreboard createSidebarScoreboard() {
        final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            throw new IllegalStateException("Bukkit scoreboard manager is unavailable");
        }

        final Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        for (int index = 0; index < LINE_ENTRIES.size(); index++) {
            final Team team = scoreboard.registerNewTeam(this.getTeamName(index));
            team.addEntry(LINE_ENTRIES.get(index));
        }

        return scoreboard;
    }

    private void renderSidebar(
        final @NotNull Scoreboard scoreboard,
        final @NotNull Player player,
        final @NotNull List<String> lines
    ) {
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty());
        }

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        objective.displayName(
            new I18n.Builder("scoreboard_sidebar.title", player)
                .build()
                .component()
        );

        for (int index = 0; index < LINE_ENTRIES.size(); index++) {
            final String entry = LINE_ENTRIES.get(index);
            final Team team = this.getOrCreateTeam(scoreboard, index, entry);
            if (index < lines.size()) {
                team.prefix(this.fromLegacyText(lines.get(index)));
                team.suffix(Component.empty());
                objective.getScore(entry).setScore(lines.size() - index);
                continue;
            }

            team.prefix(Component.empty());
            team.suffix(Component.empty());
            scoreboard.resetScores(entry);
        }
    }

    private @NotNull Team getOrCreateTeam(
        final @NotNull Scoreboard scoreboard,
        final int index,
        final @NotNull String entry
    ) {
        final String teamName = this.getTeamName(index);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
        return team;
    }

    private @NotNull String getTeamName(
        final int index
    ) {
        return "rdrl" + index;
    }

    private @NotNull String i18nLine(
        final @NotNull String key,
        final @NotNull Player player,
        final @NotNull Map<String, Object> placeholders
    ) {
        return LEGACY_SERIALIZER.serialize(
            new I18n.Builder(key, player)
                .withPlaceholders(placeholders)
                .build()
                .component()
        );
    }

    private @NotNull Component fromLegacyText(
        final @NotNull String text
    ) {
        return LEGACY_SERIALIZER.deserialize(text);
    }

    private record SidebarState(
        @NotNull Scoreboard previousScoreboard,
        @NotNull Scoreboard sidebarScoreboard
    ) {
    }
}
