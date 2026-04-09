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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.utils.TownColorUtil;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the town ownership boss bar for online players.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownBossBarService {

    private static final long UPDATE_PERIOD_TICKS = 20L;
    private static final int PLAYTIME_FLUSH_INTERVAL_UPDATES = 60;
    private static final int TARGET_BLOCK_DISTANCE_BLOCKS = 16;

    private final RDT plugin;
    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dirtyTownPlaytimeTicks = new ConcurrentHashMap<>();
    private CancellableTaskHandle refreshTask;
    private int updateCounter;

    /**
     * Creates the boss-bar service.
     *
     * @param plugin active plugin runtime
     */
    public TownBossBarService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Starts periodic boss-bar refreshes.
     */
    public void start() {
        if (this.plugin.getScheduler() == null || this.refreshTask != null) {
            return;
        }
        this.refreshTask = this.plugin.getScheduler().runRepeating(this::refreshOnlinePlayers, UPDATE_PERIOD_TICKS, UPDATE_PERIOD_TICKS);
    }

    /**
     * Stops the service and hides any visible bars.
     */
    public void shutdown() {
        this.flushDirtyTownPlaytime();
        if (this.refreshTask != null) {
            this.refreshTask.cancel();
            this.refreshTask = null;
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.clearPlayer(player);
        }
        this.activeBars.clear();
    }

    /**
     * Clears any visible boss bar for the supplied player.
     *
     * @param player player whose bar should be hidden
     */
    public void clearPlayer(final @NotNull Player player) {
        final BossBar bossBar = this.activeBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    /**
     * Refreshes one player's displayed town bar.
     *
     * @param player player to refresh
     */
    public void refreshPlayer(final @NotNull Player player) {
        if (!player.isOnline()) {
            this.clearPlayer(player);
            return;
        }

        final RDTPlayer playerData = this.plugin.getTownRuntimeService() == null
            ? null
            : this.plugin.getTownRuntimeService().getPlayerData(player.getUniqueId());
        if (playerData != null && !playerData.isBossBarEnabled()) {
            this.clearPlayer(player);
            return;
        }

        final BossBarRenderState renderState = this.resolveRenderState(player);

        this.activeBars.compute(player.getUniqueId(), (playerId, existingBar) -> {
            if (existingBar == null) {
                final BossBar bossBar = BossBar.bossBar(
                    this.buildTitleComponent(player, renderState),
                    renderState.progress(),
                    renderState.color(),
                    BossBar.Overlay.PROGRESS
                );
                player.showBossBar(bossBar);
                return bossBar;
            }

            existingBar.name(this.buildTitleComponent(player, renderState));
            existingBar.progress(renderState.progress());
            existingBar.color(renderState.color());
            player.showBossBar(existingBar);
            return existingBar;
        });
    }

    BossBarRenderState resolveRenderState(final @NotNull Player player) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RTown targetedNexusTown = this.findTargetedNexusTown(player, runtimeService);
        if (targetedNexusTown != null && runtimeService != null) {
            final NexusCombatSnapshot combatSnapshot = runtimeService.getNexusCombatSnapshot(targetedNexusTown);
            return new BossBarRenderState(
                "town_boss_bar.nexus.title",
                Map.of(
                    "town_name", targetedNexusTown.getTownName(),
                    "current_health", this.formatStatValue(combatSnapshot.currentHealth()),
                    "max_health", this.formatStatValue(combatSnapshot.maxHealth()),
                    "defense", this.formatStatValue(combatSnapshot.defense()),
                    "nexus_level", combatSnapshot.nexusLevel()
                ),
                combatSnapshot.progress(),
                TownColorUtil.toBossBarColor(targetedNexusTown.getTownColorHex())
            );
        }

        final Location location = player.getLocation();
        final int chunkX = TownRuntimeService.toChunkCoordinate(location.getBlockX());
        final int chunkZ = TownRuntimeService.toChunkCoordinate(location.getBlockZ());
        final RTown town = runtimeService == null ? null : runtimeService.getTownAt(location);
        if (town == null) {
            return new BossBarRenderState(
                "town_boss_bar.territory.wilderness",
                Map.of("chunk_x", chunkX, "chunk_z", chunkZ),
                1.0F,
                BossBar.Color.WHITE
            );
        }

        return new BossBarRenderState(
            "town_boss_bar.territory.claimed",
            Map.of("town_name", town.getTownName(), "chunk_x", chunkX, "chunk_z", chunkZ),
            this.resolveProgress(town),
            TownColorUtil.toBossBarColor(town.getTownColorHex())
        );
    }

    private void refreshOnlinePlayers() {
        this.refreshPlayers(Bukkit.getOnlinePlayers());
        this.updateCounter++;
        if (this.updateCounter >= PLAYTIME_FLUSH_INTERVAL_UPDATES) {
            this.flushDirtyTownPlaytime();
            this.updateCounter = 0;
        }
    }

    void refreshPlayers(final @NotNull Iterable<? extends Player> players) {
        final ISchedulerAdapter scheduler = this.plugin.getScheduler();
        if (scheduler == null) {
            return;
        }
        for (final Player player : players) {
            scheduler.runAtEntity(player, () -> this.refreshPlayerContext(player));
        }
    }

    private float resolveProgress(final RTown town) {
        if (town == null) {
            return 1.0F;
        }

        final int maxChunks = Math.max(1, this.plugin.getDefaultConfig().getGlobalMaxChunkLimit());
        final float progress = (float) town.getChunks().size() / (float) maxChunks;
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    private @NotNull Component buildTitleComponent(
        final @NotNull Player player,
        final @NotNull BossBarRenderState renderState
    ) {
        return new I18n.Builder(renderState.translationKey(), player)
            .withPlaceholders(renderState.placeholders())
            .build()
            .component();
    }

    private @Nullable RTown findTargetedNexusTown(
        final @NotNull Player player,
        final @Nullable TownRuntimeService runtimeService
    ) {
        if (runtimeService == null) {
            return null;
        }

        final Block targetBlock = player.getTargetBlockExact(TARGET_BLOCK_DISTANCE_BLOCKS);
        return targetBlock == null ? null : runtimeService.findNexusTown(targetBlock.getLocation());
    }

    private @NotNull String formatStatValue(final double value) {
        final double roundedValue = Math.rint(value);
        if (Math.abs(value - roundedValue) < 0.000_1D) {
            return String.valueOf((long) roundedValue);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void trackTownPlaytime(final @NotNull Player player) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return;
        }

        final RTown townAtLocation = runtimeService.getTownAt(player.getLocation());
        final RDTPlayer playerData = runtimeService.getPlayerData(player.getUniqueId());
        if (townAtLocation == null
            || playerData == null
            || !Objects.equals(playerData.getTownUUID(), townAtLocation.getTownUUID())) {
            return;
        }

        townAtLocation.addAggregateTownPlaytimeTicks(UPDATE_PERIOD_TICKS);
        this.dirtyTownPlaytimeTicks.merge(townAtLocation.getTownUUID(), UPDATE_PERIOD_TICKS, Long::sum);
    }

    private void refreshPlayerContext(final @NotNull Player player) {
        this.trackTownPlaytime(player);
        this.refreshPlayer(player);
    }

    private void flushDirtyTownPlaytime() {
        if (this.plugin.getTownRepository() == null || this.dirtyTownPlaytimeTicks.isEmpty()) {
            return;
        }

        for (final UUID townUuid : this.dirtyTownPlaytimeTicks.keySet()) {
            final RTown town = this.plugin.getTownRuntimeService() == null
                ? null
                : this.plugin.getTownRuntimeService().getTown(townUuid);
            if (town != null) {
                this.plugin.getTownRepository().update(town);
            }
        }
        this.dirtyTownPlaytimeTicks.clear();
    }

    record BossBarRenderState(
        @NotNull String translationKey,
        @NotNull Map<String, Object> placeholders,
        float progress,
        @NotNull BossBar.Color color
    ) {

        BossBarRenderState {
            translationKey = Objects.requireNonNull(translationKey, "translationKey");
            placeholders = Map.copyOf(Objects.requireNonNull(placeholders, "placeholders"));
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            color = Objects.requireNonNull(color, "color");
        }
    }
}
