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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated snapshot history browser for one player.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminRollbackSnapshotHistoryView extends APaginatedView<RRollbackSnapshot> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the snapshot history browser.
     */
    public StorageAdminRollbackSnapshotHistoryView() {
        super(StorageAdminRollbackPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_rollback_history_ui";
    }

    /**
     * Returns title placeholders.
     *
     * @param context open context
     * @return placeholder map
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
     * Loads snapshot history for the selected player.
     *
     * @param context active context
     * @return async newest-first snapshot list
     */
    @Override
    protected @NotNull CompletableFuture<List<RRollbackSnapshot>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final UUID targetUuid = this.resolveTargetUuid(context);
        if (targetUuid == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final RDR plugin = this.rdr.get(context);
        return CompletableFuture.completedFuture(
            plugin.getStorageRollbackService() == null ? List.of() : plugin.getStorageRollbackService().getPlayerSnapshots(targetUuid)
        );
    }

    /**
     * Renders one snapshot history entry.
     *
     * @param context active context
     * @param builder slot builder
     * @param index zero-based entry index
     * @param entry snapshot entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RRollbackSnapshot entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminRollbackTargetView.class,
                Map.of(
                    "plugin", this.rdr.get(clickContext),
                    "snapshotId", entry.getId(),
                    "targetUuid", entry.getTargetPlayerUuid(),
                    "targetName", this.resolveTargetName(clickContext)
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
        final int snapshotCount = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        if (!StorageAdminRollbackViewSupport.hasRollbackAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, this.resolveTargetName(render), snapshotCount));
        if (snapshotCount < 1) {
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
        final @NotNull Player viewer,
        final @NotNull RRollbackSnapshot snapshot
    ) {
        final String triggerName = this.i18n("entry.trigger." + snapshot.getTriggerType().getTranslationKey(), viewer)
            .build()
            .getI18nVersionWrapper()
            .asPlaceholder();

        final String actorName = snapshot.getActorName() == null || snapshot.getActorName().isBlank()
            ? this.i18n("entry.none", viewer).build().getI18nVersionWrapper().asPlaceholder()
            : snapshot.getActorName();

        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("entry.name", viewer)
                .withPlaceholders(Map.of(
                    "trigger", triggerName,
                    "created_at", StorageAdminRollbackViewSupport.formatTimestamp(snapshot.getCreatedAt())
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", viewer)
                .withPlaceholders(Map.of(
                    "snapshot_id", String.valueOf(snapshot.getId()),
                    "trigger", triggerName,
                    "created_at", StorageAdminRollbackViewSupport.formatTimestamp(snapshot.getCreatedAt()),
                    "actor_name", actorName,
                    "from_world", this.orNone(viewer, snapshot.getFromWorldName()),
                    "to_world", this.orNone(viewer, snapshot.getToWorldName()),
                    "target_count", 1 + snapshot.getStorageSnapshots().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull String targetName,
        final int snapshotCount
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "player_name", targetName,
                    "snapshot_count", snapshotCount
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

    private @Nullable UUID resolveTargetUuid(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object targetUuid = data.get("targetUuid");
        return targetUuid instanceof UUID uuid ? uuid : null;
    }

    private @NotNull String resolveTargetName(final @NotNull Context context) {
        final UUID targetUuid = this.resolveTargetUuid(context);
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> data) {
            final Object targetName = data.get("targetName");
            if (targetName instanceof String textValue && !textValue.isBlank()) {
                return textValue;
            }
        }

        return targetUuid == null
            ? this.orNone(context.getPlayer(), null)
            : StorageAdminRollbackViewSupport.resolvePlayerDisplayName(targetUuid, null);
    }

    private @NotNull String orNone(
        final @NotNull Player player,
        final @Nullable String value
    ) {
        if (value == null || value.isBlank()) {
            return this.i18n("entry.none", player).build().getI18nVersionWrapper().asPlaceholder();
        }
        return value;
    }
}
