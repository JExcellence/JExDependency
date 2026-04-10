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

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RRollbackSnapshot;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated selector of all known RDR players for rollback history browsing.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminRollbackPlayerView
    extends APaginatedView<StorageAdminRollbackPlayerView.RollbackPlayerEntry> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the rollback player selector.
     */
    public StorageAdminRollbackPlayerView() {
        super(StorageAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_rollback_player_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    /**
     * Resolves the player selector entries.
     *
     * @param context active context
     * @return async player-entry list
     */
    @Override
    protected @NotNull CompletableFuture<List<RollbackPlayerEntry>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final RDR plugin = this.rdr.get(context);
        if (plugin.getPlayerRepository() == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Map<UUID, Integer> snapshotCounts = new HashMap<>();
        final Map<UUID, RRollbackSnapshot> latestSnapshots = new HashMap<>();
        if (plugin.getRollbackSnapshotRepository() != null) {
            for (final RRollbackSnapshot snapshot : plugin.getRollbackSnapshotRepository().findAllWithPlayer()) {
                snapshotCounts.merge(snapshot.getTargetPlayerUuid(), 1, Integer::sum);
                latestSnapshots.putIfAbsent(snapshot.getTargetPlayerUuid(), snapshot);
            }
        }

        final Map<UUID, RollbackPlayerEntry> entriesByPlayerId = new HashMap<>();
        for (final RDRPlayer playerData : plugin.getPlayerRepository().findAllPlayers()) {
            final UUID playerId = playerData.getIdentifier();
            final RRollbackSnapshot latestSnapshot = latestSnapshots.get(playerId);
            entriesByPlayerId.put(playerId, new RollbackPlayerEntry(
                playerId,
                StorageAdminRollbackViewSupport.resolvePlayerDisplayName(
                    playerId,
                    latestSnapshot == null ? null : latestSnapshot.getLastKnownPlayerName()
                ),
                snapshotCounts.getOrDefault(playerId, 0),
                latestSnapshot
            ));
        }

        for (final Map.Entry<UUID, RRollbackSnapshot> entry : latestSnapshots.entrySet()) {
            entriesByPlayerId.putIfAbsent(entry.getKey(), new RollbackPlayerEntry(
                entry.getKey(),
                StorageAdminRollbackViewSupport.resolvePlayerDisplayName(
                    entry.getKey(),
                    entry.getValue().getLastKnownPlayerName()
                ),
                snapshotCounts.getOrDefault(entry.getKey(), 0),
                entry.getValue()
            ));
        }

        final List<RollbackPlayerEntry> entries = new ArrayList<>(entriesByPlayerId.values());
        entries.sort(Comparator.comparing(RollbackPlayerEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one rollback player entry.
     *
     * @param context active context
     * @param builder slot builder
     * @param index zero-based entry index
     * @param entry current entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RollbackPlayerEntry entry
    ) {
        builder.withItem(this.createEntryItem(context, entry))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminRollbackSnapshotHistoryView.class,
                Map.of(
                    "plugin", this.rdr.get(clickContext),
                    "targetUuid", entry.playerId(),
                    "targetName", entry.displayName()
                )
            ));
    }

    /**
     * Renders summary and empty-state content.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final int trackedPlayers = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        int totalSnapshots = 0;
        if (this.getPagination(render).source() != null) {
            for (final Object entry : this.getPagination(render).source()) {
                if (entry instanceof RollbackPlayerEntry rollbackPlayerEntry) {
                    totalSnapshots += rollbackPlayerEntry.snapshotCount();
                }
            }
        }

        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, trackedPlayers, totalSnapshots));
        if (trackedPlayers < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Context context,
        final @NotNull RollbackPlayerEntry entry
    ) {
        final Player viewer = context.getPlayer();
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.playerId());
        final RRollbackSnapshot latestSnapshot = entry.latestSnapshot();

        final String latestTrigger = latestSnapshot == null
            ? this.i18n("entry.none", viewer).build().getI18nVersionWrapper().asPlaceholder()
            : this.i18n("entry.trigger." + latestSnapshot.getTriggerType().getTranslationKey(), viewer)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
        final String latestSaved = latestSnapshot == null
            ? this.i18n("entry.none", viewer).build().getI18nVersionWrapper().asPlaceholder()
            : StorageAdminRollbackViewSupport.formatTimestamp(latestSnapshot.getCreatedAt());

        final ItemStack playerHead = UnifiedBuilderFactory.unifiedHead(offlinePlayer).build();
        return UnifiedBuilderFactory.item(playerHead)
            .setName(this.i18n("entry.name", viewer)
                .withPlaceholder("player_name", entry.displayName())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", viewer)
                .withPlaceholders(Map.of(
                    "player_name", entry.displayName(),
                    "player_uuid", entry.playerId().toString(),
                    "snapshot_count", entry.snapshotCount(),
                    "latest_trigger", latestTrigger,
                    "latest_saved", latestSaved
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int trackedPlayers,
        final int totalSnapshots
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "tracked_players", trackedPlayers,
                    "total_snapshots", totalSnapshots
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.empty.name", player).build().component())
            .setLore(this.i18n("feedback.empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.access_denied.name", player).build().component())
            .setLore(this.i18n("feedback.access_denied.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Immutable rollback player selector entry.
     *
     * @param playerId player UUID
     * @param displayName display name shown in the UI
     * @param snapshotCount saved snapshot count for the player
     * @param latestSnapshot latest saved snapshot, or {@code null} when none exists
     */
    protected record RollbackPlayerEntry(
        @NotNull UUID playerId,
        @NotNull String displayName,
        int snapshotCount,
        @Nullable RRollbackSnapshot latestSnapshot
    ) {
    }
}
