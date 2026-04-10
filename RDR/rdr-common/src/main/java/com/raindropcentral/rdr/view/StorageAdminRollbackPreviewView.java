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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated snapshot preview and restore view.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminRollbackPreviewView
    extends APaginatedView<StorageAdminRollbackPreviewView.PreviewEntry> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the preview and restore view.
     */
    public StorageAdminRollbackPreviewView() {
        super(StorageAdminRollbackTargetView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_rollback_preview_ui";
    }

    /**
     * Returns title placeholders.
     *
     * @param context open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext context) {
        return Map.of("target_label", this.resolveTargetLabel(context));
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            " r  s    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    /**
     * Loads the preview entries for the selected snapshot target.
     *
     * @param context active context
     * @return async preview entries
     */
    @Override
    protected @NotNull CompletableFuture<List<PreviewEntry>> getAsyncPaginationSource(final @NotNull Context context) {
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final PreviewTargetData targetData = this.resolvePreviewTargetData(context);
        return CompletableFuture.completedFuture(targetData == null ? List.of() : targetData.entries());
    }

    /**
     * Renders one preview slot entry.
     *
     * @param context active context
     * @param builder slot builder
     * @param index zero-based entry index
     * @param entry preview entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull PreviewEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry));
    }

    /**
     * Renders summary, restore, and empty-state controls.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final PreviewTargetData targetData = this.resolvePreviewTargetData(render);
        render.layoutSlot('s', this.createSummaryItem(render, player, targetData));
        render.layoutSlot('r', this.createRestoreItem(player, targetData))
            .onClick(this::handleRestoreClick);

        if (targetData == null || this.getPagination(render).source() == null || this.getPagination(render).source().isEmpty()) {
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

    private void handleRestoreClick(final @NotNull SlotClickContext clickContext) {
        clickContext.setCancelled(true);
        final Player actor = clickContext.getPlayer();
        final RDR plugin = this.rdr.get(clickContext);
        final StorageRollbackService rollbackService = plugin.getStorageRollbackService();
        final Long snapshotId = this.resolveSnapshotId(clickContext);
        final String targetType = this.resolveTargetType(clickContext);

        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(actor)
            || rollbackService == null
            || snapshotId == null
            || targetType == null) {
            this.sendRollbackMessage(actor, "storage.rollback.message.restore_invalid_target", Map.of());
            return;
        }

        rollbackService.restoreSnapshotTargetAsync(
                actor,
                snapshotId,
                targetType,
                this.resolveStorageSnapshotId(clickContext)
            )
            .whenComplete((result, throwable) -> plugin.getScheduler().runSync(() -> {
                if (throwable != null || result == null) {
                    this.sendRollbackMessage(actor, "storage.rollback.message.restore_failed", Map.of());
                    return;
                }

                switch (result.status()) {
                    case SUCCESS -> {
                        if (result.storageKey() == null) {
                            this.sendRollbackMessage(actor, "storage.rollback.message.restore_player_success", Map.of(
                                "player_name", this.resolveTargetName(clickContext),
                                "closed_views", result.closedViews()
                            ));
                        } else {
                            this.sendRollbackMessage(actor, "storage.rollback.message.restore_storage_success", Map.of(
                                "player_name", this.resolveTargetName(clickContext),
                                "storage_key", result.storageKey(),
                                "closed_views", result.closedViews()
                            ));
                        }
                        this.openTargetView(actor, plugin, clickContext);
                    }
                    case SNAPSHOT_MISSING -> this.sendRollbackMessage(actor, "storage.rollback.message.restore_snapshot_missing", Map.of());
                    case TARGET_OFFLINE -> this.sendRollbackMessage(actor, "storage.rollback.message.restore_target_offline", Map.of(
                        "player_name", result.targetDisplayName() == null ? this.resolveTargetName(clickContext) : result.targetDisplayName()
                    ));
                    case INVALID_TARGET -> this.sendRollbackMessage(actor, "storage.rollback.message.restore_invalid_target", Map.of());
                    case STORAGE_SNAPSHOT_MISSING -> this.sendRollbackMessage(actor, "storage.rollback.message.restore_storage_snapshot_missing", Map.of());
                    case STORAGE_MISSING -> this.sendRollbackMessage(actor, "storage.rollback.message.restore_storage_missing", Map.of(
                        "storage_key", result.storageKey() == null ? this.resolveTargetLabel(clickContext) : result.storageKey()
                    ));
                    case SAFETY_SNAPSHOT_FAILED -> this.sendRollbackMessage(actor, "storage.rollback.message.restore_safety_failed", Map.of(
                        "player_name", result.targetDisplayName() == null ? this.resolveTargetName(clickContext) : result.targetDisplayName()
                    ));
                }
            }));
    }

    private void openTargetView(
        final @NotNull Player actor,
        final @NotNull RDR plugin,
        final @NotNull Context context
    ) {
        final Long snapshotId = this.resolveSnapshotId(context);
        final UUID targetUuid = this.resolveTargetUuid(context);
        if (snapshotId == null || targetUuid == null) {
            return;
        }

        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("plugin", plugin);
        initialData.put("snapshotId", snapshotId);
        initialData.put("targetUuid", targetUuid);
        initialData.put("targetName", this.resolveTargetName(context));
        plugin.getViewFrame().open(StorageAdminRollbackTargetView.class, actor, initialData);
    }

    private void sendRollbackMessage(
        final @NotNull Player player,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        this.i18n(key, player)
            .withPlaceholders(placeholders)
            .build()
            .sendMessage();
    }

    private @Nullable PreviewTargetData resolvePreviewTargetData(final @NotNull Context context) {
        final RRollbackSnapshot snapshot = this.resolveSnapshot(context);
        final String targetType = this.resolveTargetType(context);
        if (snapshot == null || targetType == null) {
            return null;
        }

        if (StorageRollbackService.TARGET_TYPE_PLAYER_SET.equals(targetType)) {
            final List<PreviewEntry> entries = new ArrayList<>();
            this.addPreviewEntries(context.getPlayer(), entries, "entry.container.main_inventory", snapshot.getMainInventory(), 36);
            this.addPreviewEntries(context.getPlayer(), entries, "entry.container.armor", snapshot.getArmorInventory(), 4);
            entries.add(new PreviewEntry(
                this.i18n("entry.container.offhand", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder(),
                "1",
                snapshot.getOffhandItem()
            ));
            this.addPreviewEntries(context.getPlayer(), entries, "entry.container.ender_chest", snapshot.getEnderChestInventory(), 27);

            return new PreviewTargetData(
                this.resolveTargetLabel(context),
                StorageRollbackService.TARGET_TYPE_PLAYER_SET,
                36 + 4 + 1 + 27,
                snapshot.getMainInventory().size()
                    + snapshot.getArmorInventory().size()
                    + (snapshot.getOffhandItem() == null ? 0 : 1)
                    + snapshot.getEnderChestInventory().size(),
                null,
                0,
                0,
                entries
            );
        }

        final RRollbackStorageSnapshot storageSnapshot = this.resolveStorageSnapshot(snapshot, this.resolveStorageSnapshotId(context));
        if (storageSnapshot == null) {
            return null;
        }

        final List<PreviewEntry> entries = new ArrayList<>();
        this.addPreviewEntries(context.getPlayer(), entries, "entry.container.storage", storageSnapshot.getInventory(), storageSnapshot.getInventorySize());
        return new PreviewTargetData(
            storageSnapshot.getStorageKey(),
            StorageRollbackService.TARGET_TYPE_STORAGE,
            storageSnapshot.getInventorySize(),
            storageSnapshot.getInventory().size(),
            storageSnapshot.getHotkey(),
            storageSnapshot.getTrustedPlayers().size(),
            storageSnapshot.getTaxDebtEntries().size(),
            entries
        );
    }

    private void addPreviewEntries(
        final @NotNull Player viewer,
        final @NotNull List<PreviewEntry> entries,
        final @NotNull String containerKey,
        final @NotNull Map<Integer, ItemStack> contents,
        final int size
    ) {
        final String containerLabel = this.i18n(containerKey, viewer).build().getI18nVersionWrapper().asPlaceholder();
        for (int slotIndex = 0; slotIndex < size; slotIndex++) {
            entries.add(new PreviewEntry(containerLabel, Integer.toString(slotIndex + 1), contents.get(slotIndex)));
        }
    }

    private @NotNull ItemStack createEntryItem(
        final @NotNull Player player,
        final @NotNull PreviewEntry entry
    ) {
        if (entry.itemStack() == null || entry.itemStack().isEmpty()) {
            return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(this.i18n("entry.empty_name", player)
                    .withPlaceholders(Map.of(
                        "container", entry.containerLabel(),
                        "slot_label", entry.slotLabel()
                    ))
                    .build()
                    .component())
                .setLore(this.i18n("entry.empty_lore", player)
                    .withPlaceholders(Map.of(
                        "container", entry.containerLabel(),
                        "slot_label", entry.slotLabel()
                    ))
                    .build()
                    .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        return UnifiedBuilderFactory.item(entry.itemStack().clone())
            .setLore(this.i18n("entry.filled_lore", player)
                .withPlaceholders(Map.of(
                    "container", entry.containerLabel(),
                    "slot_label", entry.slotLabel(),
                    "material", this.formatMaterial(entry.itemStack().getType())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Context context,
        final @NotNull Player player,
        final @Nullable PreviewTargetData targetData
    ) {
        if (targetData == null) {
            return this.createEmptyItem(player);
        }

        final String hotkey = targetData.hotkey() == null
            ? this.i18n("entry.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : String.valueOf(targetData.hotkey());

        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "player_name", this.resolveTargetName(context),
                    "target_label", targetData.displayLabel(),
                    "target_type", this.resolveTargetTypeLabel(player, targetData.targetType()),
                    "slot_count", targetData.slotCount(),
                    "filled_slots", targetData.filledSlots(),
                    "hotkey", hotkey,
                    "trusted_players", targetData.trustedPlayers(),
                    "tax_entries", targetData.taxEntries()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRestoreItem(
        final @NotNull Player player,
        final @Nullable PreviewTargetData targetData
    ) {
        if (targetData == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("restore.unavailable.name", player).build().component())
                .setLore(this.i18n("restore.unavailable.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        }

        return UnifiedBuilderFactory.item(Material.LIME_DYE)
            .setName(this.i18n("restore.ready.name", player).build().component())
            .setLore(this.i18n("restore.ready.lore", player)
                .withPlaceholders(Map.of(
                    "target_label", targetData.displayLabel(),
                    "target_type", this.resolveTargetTypeLabel(player, targetData.targetType())
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

    private @NotNull String resolveTargetTypeLabel(final @NotNull Player player, final @NotNull String targetType) {
        return this.i18n(
            StorageRollbackService.TARGET_TYPE_PLAYER_SET.equals(targetType) ? "entry.type.player_set" : "entry.type.storage",
            player
        ).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String resolveTargetLabel(final @NotNull Context context) {
        final String targetType = this.resolveTargetType(context);
        if (StorageRollbackService.TARGET_TYPE_PLAYER_SET.equals(targetType)) {
            return this.i18n("entry.target.player_set", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder();
        }

        final RRollbackSnapshot snapshot = this.resolveSnapshot(context);
        final RRollbackStorageSnapshot storageSnapshot = snapshot == null ? null : this.resolveStorageSnapshot(snapshot, this.resolveStorageSnapshotId(context));
        return storageSnapshot == null ? this.i18n("entry.none", context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder() : storageSnapshot.getStorageKey();
    }

    private @Nullable RRollbackSnapshot resolveSnapshot(final @NotNull Context context) {
        final Long snapshotId = this.resolveSnapshotId(context);
        if (snapshotId == null) {
            return null;
        }

        final RDR plugin = this.rdr.get(context);
        return plugin.getStorageRollbackService() == null ? null : plugin.getStorageRollbackService().getSnapshot(snapshotId);
    }

    private @Nullable RRollbackStorageSnapshot resolveStorageSnapshot(
        final @NotNull RRollbackSnapshot snapshot,
        final @Nullable Long storageSnapshotId
    ) {
        if (storageSnapshotId == null) {
            return null;
        }

        for (final RRollbackStorageSnapshot storageSnapshot : snapshot.getStorageSnapshots()) {
            if (storageSnapshotId.equals(storageSnapshot.getId())) {
                return storageSnapshot;
            }
        }
        return null;
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

    private @Nullable String resolveTargetType(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object targetType = data.get("targetType");
        return targetType instanceof String textValue ? textValue : null;
    }

    private @Nullable Long resolveStorageSnapshotId(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object storageSnapshotId = data.get("storageSnapshotId");
        if (!(storageSnapshotId instanceof Long value) || value < 0L) {
            return null;
        }
        return value;
    }

    private @NotNull String formatMaterial(final @NotNull Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    protected record PreviewEntry(
        @NotNull String containerLabel,
        @NotNull String slotLabel,
        @Nullable ItemStack itemStack
    ) {
    }

    private record PreviewTargetData(
        @NotNull String displayLabel,
        @NotNull String targetType,
        int slotCount,
        int filledSlots,
        @Nullable Integer hotkey,
        int trustedPlayers,
        int taxEntries,
        @NotNull List<PreviewEntry> entries
    ) {
    }
}
