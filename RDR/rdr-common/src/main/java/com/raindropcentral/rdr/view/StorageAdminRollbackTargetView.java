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
import com.raindropcentral.rdr.database.entity.RRollbackSnapshot;
import com.raindropcentral.rdr.database.entity.RRollbackStorageSnapshot;
import com.raindropcentral.rdr.service.StorageRollbackService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Browser for snapshot restore targets.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminRollbackTargetView
    extends APaginatedView<StorageAdminRollbackTargetView.SnapshotTargetEntry> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the snapshot target browser.
     */
    public StorageAdminRollbackTargetView() {
        super(StorageAdminRollbackSnapshotHistoryView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_rollback_target_ui";
    }

    /**
     * Returns title placeholders.
     *
     * @param context open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext context) {
        return Map.of("player_name", this.resolveTargetName(context));
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
     * Loads snapshot targets for the selected snapshot.
     *
     * @param context active context
     * @return async target list
     */
    @Override
    protected @NotNull CompletableFuture<List<SnapshotTargetEntry>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final RRollbackSnapshot snapshot = this.resolveSnapshot(context);
        if (snapshot == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<SnapshotTargetEntry> entries = new ArrayList<>();
        entries.add(new SnapshotTargetEntry(
            StorageRollbackService.TARGET_TYPE_PLAYER_SET,
            null,
            this.i18n("entry.target.player_set", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder(),
            36 + 4 + 1 + 27,
            snapshot.getMainInventory().size()
                + snapshot.getArmorInventory().size()
                + (snapshot.getOffhandItem() == null ? 0 : 1)
                + snapshot.getEnderChestInventory().size(),
            null,
            0,
            0
        ));

        for (final RRollbackStorageSnapshot storageSnapshot : snapshot.getStorageSnapshots()) {
            entries.add(new SnapshotTargetEntry(
                StorageRollbackService.TARGET_TYPE_STORAGE,
                storageSnapshot.getId(),
                storageSnapshot.getStorageKey(),
                storageSnapshot.getInventorySize(),
                storageSnapshot.getInventory().size(),
                storageSnapshot.getHotkey(),
                storageSnapshot.getTrustedPlayers().size(),
                storageSnapshot.getTaxDebtEntries().size()
            ));
        }
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one snapshot target entry.
     *
     * @param context active context
     * @param builder slot builder
     * @param index zero-based entry index
     * @param entry entry payload
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull SnapshotTargetEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminRollbackPreviewView.class,
                Map.of(
                    "plugin", this.rdr.get(clickContext),
                    "snapshotId", this.resolveSnapshotId(clickContext),
                    "targetUuid", this.resolveTargetUuid(clickContext),
                    "targetName", this.resolveTargetName(clickContext),
                    "targetType", entry.targetType(),
                    "storageSnapshotId", entry.storageSnapshotId() == null ? -1L : entry.storageSnapshotId()
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
        final int targetCount = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final RRollbackSnapshot snapshot = this.resolveSnapshot(render);
        render.layoutSlot('s', this.createSummaryItem(player, snapshot, targetCount));
        if (targetCount < 1) {
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
        final @NotNull Player player,
        final @NotNull SnapshotTargetEntry entry
    ) {
        final String hotkey = entry.hotkey() == null
            ? this.i18n("entry.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : String.valueOf(entry.hotkey());

        return UnifiedBuilderFactory.item(
                StorageRollbackService.TARGET_TYPE_PLAYER_SET.equals(entry.targetType())
                    ? Material.PLAYER_HEAD
                    : Material.CHEST
            )
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "target_label", entry.displayLabel(),
                    "target_type", this.resolveTargetTypeLabel(player, entry.targetType())
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "target_label", entry.displayLabel(),
                    "slot_count", entry.slotCount(),
                    "filled_slots", entry.filledSlots(),
                    "hotkey", hotkey,
                    "trusted_players", entry.trustedPlayers(),
                    "tax_entries", entry.taxEntries()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @Nullable RRollbackSnapshot snapshot,
        final int targetCount
    ) {
        final String snapshotId = snapshot == null || snapshot.getId() == null ? "?" : String.valueOf(snapshot.getId());
        final String createdAt = snapshot == null
            ? this.i18n("entry.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : StorageAdminRollbackViewSupport.formatTimestamp(snapshot.getCreatedAt());

        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "player_name", this.resolveTargetNameFallback(player, snapshot),
                    "snapshot_id", snapshotId,
                    "created_at", createdAt,
                    "target_count", targetCount
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

    private @NotNull String resolveTargetTypeLabel(
        final @NotNull Player player,
        final @NotNull String targetType
    ) {
        return this.i18n(
            StorageRollbackService.TARGET_TYPE_PLAYER_SET.equals(targetType) ? "entry.type.player_set" : "entry.type.storage",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @Nullable RRollbackSnapshot resolveSnapshot(final @NotNull Context context) {
        final Long snapshotId = this.resolveSnapshotId(context);
        if (snapshotId == null) {
            return null;
        }

        final RDR plugin = this.rdr.get(context);
        return plugin.getStorageRollbackService() == null ? null : plugin.getStorageRollbackService().getSnapshot(snapshotId);
    }

    private @Nullable Long resolveSnapshotId(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object snapshotId = data.get("snapshotId");
        return snapshotId instanceof Long value ? value : null;
    }

    private @Nullable UUID resolveTargetUuid(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object targetUuid = data.get("targetUuid");
        return targetUuid instanceof UUID uuid ? uuid : null;
    }

    private @NotNull String resolveTargetName(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> data) {
            final Object targetName = data.get("targetName");
            if (targetName instanceof String textValue && !textValue.isBlank()) {
                return textValue;
            }
        }

        final UUID targetUuid = this.resolveTargetUuid(context);
        return targetUuid == null
            ? this.i18n("entry.none", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder()
            : StorageAdminRollbackViewSupport.resolvePlayerDisplayName(targetUuid, null);
    }

    private @NotNull String resolveTargetNameFallback(
        final @NotNull Player player,
        final @Nullable RRollbackSnapshot snapshot
    ) {
        if (snapshot == null) {
            return this.i18n("entry.none", player).build().getI18nVersionWrapper().asPlaceholder();
        }
        return StorageAdminRollbackViewSupport.resolvePlayerDisplayName(
            snapshot.getTargetPlayerUuid(),
            snapshot.getLastKnownPlayerName()
        );
    }

    /**
     * Immutable snapshot target entry.
     *
     * @param targetType target type token
     * @param storageSnapshotId storage child identifier, or {@code null} for player-set entries
     * @param displayLabel target display label
     * @param slotCount total saved slots represented by the target
     * @param filledSlots occupied saved slot count
     * @param hotkey saved hotkey, or {@code null} when none exists
     * @param trustedPlayers saved trusted-player count
     * @param taxEntries saved tax-entry count
     */
    protected record SnapshotTargetEntry(
        @NotNull String targetType,
        @Nullable Long storageSnapshotId,
        @NotNull String displayLabel,
        int slotCount,
        int filledSlots,
        @Nullable Integer hotkey,
        int trustedPlayers,
        int taxEntries
    ) {
    }
}
