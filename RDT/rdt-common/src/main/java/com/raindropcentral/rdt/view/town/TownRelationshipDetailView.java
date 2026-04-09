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
import com.raindropcentral.rdt.service.TownRelationshipChangeResult;
import com.raindropcentral.rdt.service.TownRelationshipViewEntry;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownRelationshipState;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
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

import java.util.Map;
import java.util.UUID;

/**
 * Detail view for managing one relationship target from the viewing town's diplomacy browser.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownRelationshipDetailView extends com.raindropcentral.rplatform.view.BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<UUID> targetTownUuid = initialState("target_town_uuid");

    /**
     * Creates the relationship-detail view.
     */
    public TownRelationshipDetailView() {
        super(TownRelationshipsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_relationship_detail_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "  n a h  ",
            "    i    ",
            "         ",
            "         ",
            "r        "
        };
    }

    /**
     * Renders the relationship summary and state-action controls.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final TownRelationshipViewEntry relationship = this.resolveRelationship(render);
        if (relationship == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            render.layoutSlot('r', this.createReturnItem(player)).onClick(clickContext -> clickContext.back());
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(render, relationship));
        render.layoutSlot('n', this.createActionItem(render, relationship, TownRelationshipState.NEUTRAL))
            .onClick(clickContext -> this.handleRelationshipChange(clickContext, TownRelationshipState.NEUTRAL));
        render.layoutSlot('a', this.createActionItem(render, relationship, TownRelationshipState.ALLIED))
            .onClick(clickContext -> this.handleRelationshipChange(clickContext, TownRelationshipState.ALLIED));
        render.layoutSlot('h', this.createActionItem(render, relationship, TownRelationshipState.HOSTILE))
            .onClick(clickContext -> this.handleRelationshipChange(clickContext, TownRelationshipState.HOSTILE));
        render.layoutSlot('i', this.createInfoItem(render, relationship));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(clickContext -> clickContext.back());
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

    private void handleRelationshipChange(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownRelationshipState requestedState
    ) {
        final RTown town = this.resolveTown(clickContext);
        final RTown targetTown = this.resolveTargetTown(clickContext);
        if (town == null || targetTown == null || this.plugin.get(clickContext).getTownRuntimeService() == null) {
            this.sendResultMessage(clickContext, requestedState, null, "failed");
            return;
        }
        if (!this.plugin.get(clickContext).getTownRuntimeService().hasTownPermission(
            clickContext.getPlayer(),
            town,
            TownPermissions.MANAGE_RELATIONSHIPS
        )) {
            this.sendResultMessage(clickContext, requestedState, this.resolveRelationship(clickContext), "no_permission");
            return;
        }

        final TownRelationshipChangeResult result = this.plugin.get(clickContext).getTownRuntimeService().changeTownRelationship(
            town,
            targetTown,
            requestedState
        );
        this.sendResultMessage(clickContext, requestedState, result.relationship(), result.status().getTranslationKey());
        clickContext.update();
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.townUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.townUuid.get(context));
    }

    private @Nullable RTown resolveTargetTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.targetTownUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.targetTownUuid.get(context));
    }

    private TownRelationshipViewEntry resolveRelationship(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        final RTown targetTown = this.resolveTargetTown(context);
        if (town == null || targetTown == null || this.plugin.get(context).getTownRuntimeService() == null) {
            return null;
        }
        return this.plugin.get(context).getTownRuntimeService().getTownRelationshipViewEntry(town, targetTown);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Context context,
        final @NotNull TownRelationshipViewEntry relationship
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", context.getPlayer())
                .withPlaceholder("town_name", relationship.targetTown().getTownName())
                .build()
                .component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", relationship.targetTown().getTownName(),
                    "effective_relationship", this.relationshipStateText(relationship.effectiveState(), context.getPlayer()),
                    "confirmed_relationship", this.relationshipStateText(relationship.confirmedState(), context.getPlayer()),
                    "pending_state", this.pendingStateText(relationship, context.getPlayer()),
                    "cooldown", TownOverviewView.formatDurationMillis(relationship.cooldownRemainingMillis())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInfoItem(
        final @NotNull Context context,
        final @NotNull TownRelationshipViewEntry relationship
    ) {
        final int unlockLevel = this.plugin.get(context).getTownRuntimeService() == null
            ? 5
            : this.plugin.get(context).getTownRuntimeService().getTownRelationshipUnlockLevel();
        final boolean canManage = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().hasTownPermission(
                context.getPlayer(),
                relationship.sourceTown(),
                TownPermissions.MANAGE_RELATIONSHIPS
            );
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("info.name", context.getPlayer()).build().component())
            .setLore(this.i18n("info.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "source_nexus_level", relationship.sourceTown().getNexusLevel(),
                    "target_nexus_level", relationship.targetTown().getNexusLevel(),
                    "unlock_level", unlockLevel,
                    "source_unlock_state", this.sharedStatusText(relationship.sourceUnlocked() ? "unlocked" : "locked", context.getPlayer()),
                    "target_unlock_state", this.sharedStatusText(relationship.targetUnlocked() ? "unlocked" : "locked", context.getPlayer()),
                    "manage_state", this.sharedManageText(canManage ? "allowed" : "blocked", context.getPlayer()),
                    "pending_direction", this.pendingDirectionText(relationship, context.getPlayer())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createActionItem(
        final @NotNull Context context,
        final @NotNull TownRelationshipViewEntry relationship,
        final @NotNull TownRelationshipState relationshipState
    ) {
        final boolean canManage = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().hasTownPermission(
                context.getPlayer(),
                relationship.sourceTown(),
                TownPermissions.MANAGE_RELATIONSHIPS
            );
        final String loreKey = !canManage
            ? "actions.no_permission.lore"
            : relationship.lockedByLevel()
                ? "actions.locked.lore"
                : relationship.cooldownRemainingMillis() > 0L
                    ? "actions.cooldown.lore"
                    : relationship.effectiveState() == relationshipState && !relationship.hasPendingRequest()
                        ? "actions.active.lore"
                        : "actions.available.lore";
        return UnifiedBuilderFactory.item(this.resolveActionMaterial(relationshipState, canManage, relationship))
            .setName(this.i18n("actions." + relationshipState.getTranslationKey() + ".name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "relationship", this.relationshipStateText(relationshipState, context.getPlayer()),
                    "pending_state", this.pendingStateText(relationship, context.getPlayer()),
                    "cooldown", TownOverviewView.formatDurationMillis(relationship.cooldownRemainingMillis()),
                    "target_town", relationship.targetTown().getTownName()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveActionMaterial(
        final @NotNull TownRelationshipState relationshipState,
        final boolean canManage,
        final @NotNull TownRelationshipViewEntry relationship
    ) {
        if (!canManage || relationship.lockedByLevel()) {
            return Material.GRAY_DYE;
        }
        return switch (relationshipState) {
            case NEUTRAL -> Material.WHITE_DYE;
            case ALLIED -> Material.LIME_DYE;
            case HOSTILE -> Material.RED_DYE;
        };
    }

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void sendResultMessage(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownRelationshipState requestedState,
        final TownRelationshipViewEntry relationship,
        final @NotNull String resultKey
    ) {
        final String targetTownName = relationship == null ? "-" : relationship.targetTown().getTownName();
        final long cooldownRemainingMillis = relationship == null ? 0L : relationship.cooldownRemainingMillis();
        new I18n.Builder(this.getKey() + ".messages." + resultKey, clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "relationship", this.relationshipStateText(requestedState, clickContext.getPlayer()),
                "target_town", targetTownName,
                "cooldown", TownOverviewView.formatDurationMillis(cooldownRemainingMillis)
            ))
            .build()
            .sendMessage();
    }

    private @NotNull String relationshipStateText(
        final @NotNull TownRelationshipState relationshipState,
        final @NotNull Player player
    ) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder("town_relationship_shared.states." + relationshipState.getTranslationKey(), player)
                .build()
                .component()
        );
    }

    private @NotNull String pendingStateText(
        final @NotNull TownRelationshipViewEntry relationship,
        final @NotNull Player player
    ) {
        return relationship.pendingState() == null
            ? this.sharedPendingText("none", player)
            : this.relationshipStateText(relationship.pendingState(), player);
    }

    private @NotNull String pendingDirectionText(
        final @NotNull TownRelationshipViewEntry relationship,
        final @NotNull Player player
    ) {
        if (!relationship.hasPendingRequest()) {
            return this.sharedPendingText("none", player);
        }
        return this.sharedPendingText(relationship.pendingRequestedBySource() ? "outgoing" : "incoming", player);
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

    private @NotNull String sharedManageText(final @NotNull String suffix, final @NotNull Player player) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder("town_relationship_shared.manage." + suffix, player).build().component()
        );
    }
}
