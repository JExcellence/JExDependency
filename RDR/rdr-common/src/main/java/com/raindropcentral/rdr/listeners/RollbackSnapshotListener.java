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

package com.raindropcentral.rdr.listeners;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RollbackTriggerType;
import com.raindropcentral.rdr.service.StorageRollbackService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Creates rollback snapshots for automatic player lifecycle triggers.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@SuppressWarnings("unused")
public class RollbackSnapshotListener implements Listener {

    private final RDR plugin;

    /**
     * Creates the rollback trigger listener for the active runtime.
     *
     * @param plugin active plugin runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public RollbackSnapshotListener(final @NotNull RDR plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Saves a post-join snapshot after login handling settles.
     *
     * @param event player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        if (!this.isCaptureEnabled(RollbackTriggerType.JOIN)) {
            return;
        }

        final Player player = event.getPlayer();
        this.plugin.getScheduler().runDelayed(() -> {
            final Player onlinePlayer = this.plugin.getPlugin().getServer().getPlayer(player.getUniqueId());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }

            final StorageRollbackService rollbackService = this.plugin.getStorageRollbackService();
            if (rollbackService != null) {
                rollbackService.capturePlayerSnapshotAsync(
                    onlinePlayer,
                    RollbackTriggerType.JOIN,
                    null,
                    null,
                    null,
                    onlinePlayer.getWorld().getName()
                );
            }
        }, 1L);
    }

    /**
     * Saves a pre-disconnect snapshot during quit handling.
     *
     * @param event player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final StorageRollbackService rollbackService = this.plugin.getStorageRollbackService();
        if (rollbackService == null) {
            return;
        }

        rollbackService.capturePlayerSnapshotAsync(
            event.getPlayer(),
            RollbackTriggerType.LEAVE,
            null,
            null,
            null,
            event.getPlayer().getWorld().getName()
        );
    }

    /**
     * Saves a death snapshot before the player's death inventory is cleared.
     *
     * @param event player death event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        final StorageRollbackService rollbackService = this.plugin.getStorageRollbackService();
        if (rollbackService == null || !this.isCaptureEnabled(RollbackTriggerType.DEATH)) {
            return;
        }

        rollbackService.capturePlayerSnapshotAsync(
            event.getEntity(),
            RollbackTriggerType.DEATH,
            null,
            null,
            event.getEntity().getWorld().getName(),
            event.getEntity().getWorld().getName()
        );
    }

    /**
     * Saves a snapshot after the player arrives in a new world.
     *
     * @param event player changed-world event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final @NotNull PlayerChangedWorldEvent event) {
        final StorageRollbackService rollbackService = this.plugin.getStorageRollbackService();
        if (rollbackService == null || !this.isCaptureEnabled(RollbackTriggerType.WORLD_CHANGE)) {
            return;
        }

        rollbackService.capturePlayerSnapshotAsync(
            event.getPlayer(),
            RollbackTriggerType.WORLD_CHANGE,
            null,
            null,
            event.getFrom().getName(),
            event.getPlayer().getWorld().getName()
        );
    }

    private boolean isCaptureEnabled(final @NotNull RollbackTriggerType triggerType) {
        final StorageRollbackService rollbackService = this.plugin.getStorageRollbackService();
        return rollbackService != null && rollbackService.isCaptureEnabled(triggerType);
    }
}
