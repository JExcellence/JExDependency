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
import com.raindropcentral.rdt.utils.TownProtectionCategory;
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
 * Protection editor for allowed/restricted world-event rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownToggleProtectionsView extends AbstractTownProtectionView {

    private static final int[] PROTECTION_SLOTS = {11, 12, 13, 14, 15};

    /**
     * Creates the on/off protections editor.
     */
    public TownToggleProtectionsView() {
        super(TownProtectionsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_toggle_protections_ui";
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
     * Renders summary, scope switching, and binary protection entries.
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

        final List<TownProtections> protections = TownProtections.editableValues(TownProtectionCategory.BINARY_TOGGLE);
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
            this.createProtectionNavigationData(clickContext, TownProtectionCategory.BINARY_TOGGLE)
        );
    }

    private void handleProtectionClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownProtections protection
    ) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null || this.plugin.get(clickContext).getTownRuntimeService() == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        if (!this.ensureProtectionAccess(clickContext, town)) {
            return;
        }

        final RTownChunk scopedChunk = this.resolveChunk(clickContext);
        if (scopedChunk == null) {
            final String nextRoleId = this.nextTownRoleId(town.getProtectionRoleId(protection));
            final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setTownProtectionRoleId(
                town,
                protection,
                nextRoleId
            );
            if (!updated) {
                this.sendUpdateFailedMessage(clickContext);
                return;
            }
            this.sendUpdatedMessage(clickContext, protection, nextRoleId);
            clickContext.update();
            return;
        }

        final String nextRoleId = this.nextChunkRoleId(scopedChunk.getProtectionRoleId(protection));
        final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkProtectionRoleId(
            scopedChunk,
            protection,
            nextRoleId
        );
        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        this.sendUpdatedMessage(clickContext, protection, nextRoleId);
        clickContext.update();
    }

    private @NotNull String nextTownRoleId(final @NotNull String currentRoleId) {
        return currentRoleId.equals(RTown.PUBLIC_ROLE_ID) ? RTown.RESTRICTED_ROLE_ID : RTown.PUBLIC_ROLE_ID;
    }

    private @Nullable String nextChunkRoleId(final @Nullable String currentRoleId) {
        if (currentRoleId == null) {
            return RTown.RESTRICTED_ROLE_ID;
        }
        return currentRoleId.equals(RTown.RESTRICTED_ROLE_ID) ? RTown.PUBLIC_ROLE_ID : null;
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
        final String configuredRoleId = scopedChunk == null
            ? town.getProtectionRoleId(protection)
            : scopedChunk.getProtectionRoleId(protection);
        final String effectiveRoleId = configuredRoleId == null ? town.getProtectionRoleId(protection) : configuredRoleId;

        return UnifiedBuilderFactory.item(this.resolveMaterial(configuredRoleId, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(protection, context.getPlayer()))
            .setLore(this.i18n("entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "state", this.resolveBinaryStateDisplay(configuredRoleId, context.getPlayer()),
                    "effective_state", this.resolveBinaryStateDisplay(effectiveRoleId, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void sendUpdatedMessage(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownProtections protection,
        final @Nullable String roleId
    ) {
        new I18n.Builder(this.getKey() + ".updated", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "protection", this.protectionNameText(protection, clickContext.getPlayer()),
                "state", this.resolveBinaryStateDisplay(roleId, clickContext.getPlayer())
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

    private @NotNull Material resolveMaterial(final @Nullable String configuredRoleId, final boolean editable) {
        if (!editable) {
            return Material.GRAY_DYE;
        }
        if (configuredRoleId == null) {
            return Material.YELLOW_DYE;
        }
        return configuredRoleId.equals(RTown.PUBLIC_ROLE_ID) ? Material.LIME_DYE : Material.RED_DYE;
    }
}
