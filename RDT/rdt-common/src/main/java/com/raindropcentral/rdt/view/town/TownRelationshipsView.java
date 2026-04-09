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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.TownRelationshipViewEntry;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
 * Paginated diplomacy browser for the viewing town's relationship with every other town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownRelationshipsView extends APaginatedView<TownRelationshipViewEntry> {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the paginated town-relationship view.
     */
    public TownRelationshipsView() {
        super(TownChunkView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_relationships_ui";
    }

    /**
     * Loads relationship entries for the owning town.
     *
     * @param context current view context
     * @return async relationship-entry list
     */
    @Override
    protected @NotNull CompletableFuture<List<TownRelationshipViewEntry>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RTown town = this.resolveTown(context);
        if (town == null || this.plugin.get(context).getTownRuntimeService() == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return CompletableFuture.completedFuture(
            this.plugin.get(context).getTownRuntimeService().getTownRelationshipViewEntries(town)
        );
    }

    /**
     * Renders one relationship entry.
     *
     * @param context current context
     * @param builder item builder
     * @param index entry index
     * @param entry relationship entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull TownRelationshipViewEntry entry
    ) {
        builder.withItem(this.createRelationshipItem(context, entry))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownRelationshipDetailView.class,
                Map.of(
                    "plugin", this.plugin.get(clickContext),
                    "town_uuid", this.townUuid.get(clickContext),
                    "target_town_uuid", entry.targetTown().getTownUUID()
                )
            ));
    }

    /**
     * Renders the relationship summary item.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        if (town == null || this.plugin.get(render).getTownRuntimeService() == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        final int relationshipCount = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        render.slot(4).renderWith(() -> this.createSummaryItem(render, town, relationshipCount));
        if (relationshipCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels default inventory interaction for the menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.townUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.townUuid.get(context));
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final int relationshipCount
    ) {
        final int unlockLevel = this.plugin.get(context).getTownRuntimeService() == null
            ? 5
            : this.plugin.get(context).getTownRuntimeService().getTownRelationshipUnlockLevel();
        final boolean unlocked = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().areTownRelationshipsUnlocked(town);
        return UnifiedBuilderFactory.item(unlocked ? Material.FILLED_MAP : Material.GRAY_DYE)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "nexus_level", town.getNexusLevel(),
                    "unlock_level", unlockLevel,
                    "relationship_count", relationshipCount,
                    "unlock_state", this.sharedStatusText(unlocked ? "unlocked" : "locked", context.getPlayer())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRelationshipItem(final @NotNull Context context, final @NotNull TownRelationshipViewEntry entry) {
        final Player player = context.getPlayer();
        final int unlockLevel = this.plugin.get(context).getTownRuntimeService() == null
            ? 5
            : this.plugin.get(context).getTownRuntimeService().getTownRelationshipUnlockLevel();
        return UnifiedBuilderFactory.item(this.resolveMaterial(entry))
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("town_name", entry.targetTown().getTownName())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", entry.targetTown().getTownName(),
                    "effective_relationship", this.relationshipStateText(entry.effectiveState(), player),
                    "confirmed_relationship", this.relationshipStateText(entry.confirmedState(), player),
                    "pending_state", this.pendingStateText(entry, player),
                    "pending_direction", this.pendingDirectionText(entry, player),
                    "cooldown", TownOverviewView.formatDurationMillis(entry.cooldownRemainingMillis()),
                    "target_unlock_state", this.sharedStatusText(entry.targetUnlocked() ? "unlocked" : "locked", player),
                    "target_nexus_level", entry.targetTown().getNexusLevel(),
                    "unlock_level", unlockLevel
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveMaterial(final @NotNull TownRelationshipViewEntry entry) {
        if (entry.lockedByLevel()) {
            return Material.GRAY_DYE;
        }
        if (entry.hasPendingRequest()) {
            return Material.ORANGE_DYE;
        }
        return switch (entry.effectiveState()) {
            case ALLIED -> Material.LIME_DYE;
            case HOSTILE -> Material.RED_DYE;
            case NEUTRAL -> Material.WHITE_DYE;
        };
    }

    private @NotNull String relationshipStateText(
        final @NotNull com.raindropcentral.rdt.utils.TownRelationshipState relationshipState,
        final @NotNull Player player
    ) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder("town_relationship_shared.states." + relationshipState.getTranslationKey(), player)
                .build()
                .component()
        );
    }

    private @NotNull String pendingStateText(final @NotNull TownRelationshipViewEntry entry, final @NotNull Player player) {
        return entry.pendingState() == null
            ? this.sharedPendingText("none", player)
            : this.relationshipStateText(entry.pendingState(), player);
    }

    private @NotNull String pendingDirectionText(final @NotNull TownRelationshipViewEntry entry, final @NotNull Player player) {
        if (!entry.hasPendingRequest()) {
            return this.sharedPendingText("none", player);
        }
        return this.sharedPendingText(entry.pendingRequestedBySource() ? "outgoing" : "incoming", player);
    }

    private @NotNull String sharedPendingText(final @NotNull String suffix, final @NotNull Player player) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder("town_relationship_shared.pending." + suffix, player).build().component()
        );
    }

    private @NotNull String sharedStatusText(final @NotNull String suffix, final @NotNull Player player) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder("town_relationship_shared.status." + suffix, player).build().component()
        );
    }
}
