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

import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Protection editor for allied-town access to player-action rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownAlliedProtectionsView extends AbstractTownProtectionView {

    public static final String VIEW_KEY = "allied_root";

    private static final int[] PROTECTION_SLOTS = {10, 12, 14, 16, 21, 23, 25};

    /**
     * Creates the allied-access protections editor.
     */
    public TownAlliedProtectionsView() {
        super(TownRoleProtectionsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_allied_protections_ui";
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
            "         ",
            "         ",
            "         ",
            "    m    ",
            "         "
        };
    }

    /**
     * Renders summary, scope switching, and allied-access protection entries.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        if (town == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.slot(4).renderWith(() -> this.createSummaryItem(render));
        render.slot(40).renderWith(() -> this.createScopeItem(render))
            .onClick(this::handleScopeClick);

        final List<TownProtections> protections = TownProtections.editableValues(
            com.raindropcentral.rdt.utils.TownProtectionCategory.ROLE_BASED
        );
        for (int index = 0; index < protections.size() && index < PROTECTION_SLOTS.length; index++) {
            final TownProtections protection = protections.get(index);
            render.slot(PROTECTION_SLOTS[index]).renderWith(() -> this.createProtectionItem(render, protection))
                .onClick(clickContext -> this.handleProtectionClick(clickContext, protection));
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

    private void handleScopeClick(final @NotNull SlotClickContext clickContext) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null || !this.ensureProtectionAccess(clickContext, town)) {
            return;
        }

        clickContext.openForPlayer(
            TownProtectionScopeView.class,
            this.createProtectionNavigationData(clickContext, null, VIEW_KEY)
        );
    }

    private void handleProtectionClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownProtections protection
    ) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        if (!this.ensureProtectionAccess(clickContext, town)) {
            return;
        }
        if (protection == TownProtections.SWITCH_ACCESS) {
            clickContext.openForPlayer(
                TownAlliedSwitchProtectionsView.class,
                this.createProtectionNavigationData(clickContext, null, TownAlliedSwitchProtectionsView.VIEW_KEY)
            );
            return;
        }
        if (protection == TownProtections.ITEM_USE) {
            clickContext.openForPlayer(
                TownAlliedItemUseProtectionsView.class,
                this.createProtectionNavigationData(clickContext, null, TownAlliedItemUseProtectionsView.VIEW_KEY)
            );
            return;
        }
        if (this.plugin.get(clickContext).getTownRuntimeService() == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }

        final RTownChunk scopedChunk = this.resolveChunk(clickContext);
        if (scopedChunk == null) {
            final boolean nextAllowed = !this.plugin.get(clickContext).getTownRuntimeService().isAlliedProtectionAllowed(
                town,
                null,
                protection
            );
            final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setTownAlliedProtectionAllowed(
                town,
                protection,
                nextAllowed
            );
            if (!updated) {
                this.sendUpdateFailedMessage(clickContext);
                return;
            }
            this.sendUpdatedMessage(clickContext, protection, this.resolveAlliedStateDisplay(nextAllowed, clickContext.getPlayer()));
            clickContext.update();
            return;
        }

        final Boolean nextAllowed = this.nextChunkState(scopedChunk.getConfiguredAlliedProtectionAllowed(protection));
        final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkAlliedProtectionAllowed(
            scopedChunk,
            protection,
            nextAllowed
        );
        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        this.sendUpdatedMessage(clickContext, protection, this.resolveAlliedStateDisplay(nextAllowed, clickContext.getPlayer()));
        clickContext.update();
    }

    private @Nullable Boolean nextChunkState(final @Nullable Boolean currentState) {
        if (currentState == null) {
            return Boolean.TRUE;
        }
        return currentState ? Boolean.FALSE : null;
    }

    private @NotNull ItemStack createProtectionItem(
        final @NotNull Context context,
        final @NotNull TownProtections protection
    ) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        final RTownChunk scopedChunk = this.resolveChunk(context);
        if (protection == TownProtections.SWITCH_ACCESS) {
            return this.createSwitchItem(context, town, scopedChunk);
        }
        if (protection == TownProtections.ITEM_USE) {
            return this.createItemUseItem(context, town, scopedChunk);
        }

        final Boolean configuredState = scopedChunk == null
            ? town.getConfiguredAlliedProtectionAllowed(protection)
            : scopedChunk.getConfiguredAlliedProtectionAllowed(protection);
        final boolean effectiveState = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().isAlliedProtectionAllowed(town, scopedChunk, protection);

        return UnifiedBuilderFactory.item(this.resolveMaterial(configuredState, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(protection, context.getPlayer()))
            .setLore(this.i18n("entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "configured_state", this.resolveAlliedStateDisplay(configuredState, context.getPlayer()),
                    "effective_state", this.resolveAlliedStateDisplay(effectiveState, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSwitchItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @Nullable RTownChunk scopedChunk
    ) {
        final Boolean configuredState = scopedChunk == null
            ? town.getConfiguredAlliedProtectionAllowed(TownProtections.SWITCH_ACCESS)
            : scopedChunk.getConfiguredAlliedProtectionAllowed(TownProtections.SWITCH_ACCESS);
        final boolean effectiveState = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().isAlliedProtectionAllowed(
                town,
                scopedChunk,
                TownProtections.SWITCH_ACCESS
            );

        return UnifiedBuilderFactory.item(this.resolveMaterial(configuredState, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(TownProtections.SWITCH_ACCESS, context.getPlayer()))
            .setLore(this.i18n("switch_entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "configured_state", this.resolveAlliedStateDisplay(configuredState, context.getPlayer()),
                    "effective_state", this.resolveAlliedStateDisplay(effectiveState, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town),
                    "action_count", TownProtections.switchActionValues().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createItemUseItem(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @Nullable RTownChunk scopedChunk
    ) {
        final Boolean configuredState = scopedChunk == null
            ? town.getConfiguredAlliedProtectionAllowed(TownProtections.ITEM_USE)
            : scopedChunk.getConfiguredAlliedProtectionAllowed(TownProtections.ITEM_USE);
        final boolean effectiveState = this.plugin.get(context).getTownRuntimeService() != null
            && this.plugin.get(context).getTownRuntimeService().isAlliedProtectionAllowed(
                town,
                scopedChunk,
                TownProtections.ITEM_USE
            );

        return UnifiedBuilderFactory.item(this.resolveMaterial(configuredState, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(TownProtections.ITEM_USE, context.getPlayer()))
            .setLore(this.i18n("item_use_entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "configured_state", this.resolveAlliedStateDisplay(configuredState, context.getPlayer()),
                    "effective_state", this.resolveAlliedStateDisplay(effectiveState, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town),
                    "action_count", TownProtections.itemUseActionValues().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void sendUpdatedMessage(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownProtections protection,
        final @NotNull String stateDisplay
    ) {
        new I18n.Builder(this.getKey() + ".updated", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "protection", this.protectionNameText(protection, clickContext.getPlayer()),
                "state", stateDisplay
            ))
            .build()
            .sendMessage();
    }

    private void sendUpdateFailedMessage(final @NotNull SlotClickContext clickContext) {
        new I18n.Builder(this.getKey() + ".update_failed", clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull Material resolveMaterial(final @Nullable Boolean configuredState, final boolean editable) {
        if (!editable) {
            return Material.GRAY_DYE;
        }
        if (configuredState == null) {
            return Material.YELLOW_DYE;
        }
        return configuredState ? Material.LIME_DYE : Material.RED_DYE;
    }
}
