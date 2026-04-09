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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Protection editor for role-threshold-based player interaction rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownRoleProtectionsView extends AbstractTownProtectionView {

    private static final int[] PROTECTION_SLOTS = {10, 12, 14, 16, 21, 23, 25};

    /**
     * Creates the role-based protections editor.
     */
    public TownRoleProtectionsView() {
        super(TownProtectionsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_role_protections_ui";
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
     * Renders summary, scope switching, and role-based protection entries.
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
        render.slot(31).renderWith(() -> this.createAlliedProtectionsItem(render))
            .onClick(this::handleAlliedProtectionsClick);

        final List<TownProtections> protections = TownProtections.editableValues(TownProtectionCategory.ROLE_BASED);
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
            this.createProtectionNavigationData(clickContext, TownProtectionCategory.ROLE_BASED)
        );
    }

    private void handleAlliedProtectionsClick(final @NotNull SlotClickContext clickContext) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null || !this.ensureProtectionAccess(clickContext, town)) {
            return;
        }

        clickContext.openForPlayer(
            TownAlliedProtectionsView.class,
            this.createProtectionNavigationData(clickContext, null, TownAlliedProtectionsView.VIEW_KEY)
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
                TownSwitchProtectionsView.class,
                this.createProtectionNavigationData(clickContext, TownProtectionCategory.ROLE_BASED, TownSwitchProtectionsView.VIEW_KEY)
            );
            return;
        }
        if (protection == TownProtections.ITEM_USE) {
            clickContext.openForPlayer(
                TownItemUseProtectionsView.class,
                this.createProtectionNavigationData(clickContext, TownProtectionCategory.ROLE_BASED, TownItemUseProtectionsView.VIEW_KEY)
            );
            return;
        }
        if (this.plugin.get(clickContext).getTownRuntimeService() == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }

        final List<String> roleOrder = new ArrayList<>(this.plugin.get(clickContext).getTownRuntimeService().getProtectionRoleOrder(town));
        final RTownChunk scopedChunk = this.resolveChunk(clickContext);
        if (scopedChunk == null) {
            final String nextRoleId = this.nextRoleId(roleOrder, town.getProtectionRoleId(protection), false);
            final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setTownProtectionRoleId(
                town,
                protection,
                nextRoleId
            );
            if (!updated) {
                this.sendUpdateFailedMessage(clickContext);
                return;
            }
            this.sendUpdatedMessage(clickContext, protection, this.resolveRoleDisplay(town, nextRoleId, clickContext.getPlayer()));
            clickContext.update();
            return;
        }

        final String nextRoleId = this.nextRoleId(roleOrder, scopedChunk.getProtectionRoleId(protection), true);
        final boolean updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkProtectionRoleId(
            scopedChunk,
            protection,
            nextRoleId
        );
        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        this.sendUpdatedMessage(clickContext, protection, this.resolveRoleDisplay(town, nextRoleId, clickContext.getPlayer()));
        clickContext.update();
    }

    private @Nullable String nextRoleId(
        final @NotNull List<String> roleOrder,
        final @Nullable String currentRoleId,
        final boolean allowInherit
    ) {
        final List<String> cycle = new ArrayList<>();
        if (allowInherit) {
            cycle.add(null);
        }
        cycle.addAll(roleOrder);
        int currentIndex = cycle.indexOf(currentRoleId);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        return cycle.get((currentIndex + 1) % cycle.size());
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
        final String configuredRoleId = scopedChunk == null
            ? town.getProtectionRoleId(protection)
            : scopedChunk.getProtectionRoleId(protection);

        return UnifiedBuilderFactory.item(this.resolveMaterial(protection, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(protection, context.getPlayer()))
            .setLore(this.i18n("entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "role", this.resolveRoleDisplay(town, configuredRoleId, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town)
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
        final String configuredRoleId = scopedChunk == null
            ? town.getConfiguredProtectionRoleId(TownProtections.ITEM_USE)
            : scopedChunk.getProtectionRoleId(TownProtections.ITEM_USE);
        final String effectiveRoleId = this.resolveEffectiveRoleId(town, scopedChunk, TownProtections.ITEM_USE);

        return UnifiedBuilderFactory.item(this.resolveMaterial(TownProtections.ITEM_USE, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(TownProtections.ITEM_USE, context.getPlayer()))
            .setLore(this.i18n("item_use_entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "role", this.resolveRoleDisplay(town, configuredRoleId, context.getPlayer()),
                    "effective_role", this.resolveRoleDisplay(town, effectiveRoleId, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town),
                    "action_count", TownProtections.itemUseActionValues().size()
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
        final String configuredRoleId = scopedChunk == null
            ? town.getConfiguredProtectionRoleId(TownProtections.SWITCH_ACCESS)
            : scopedChunk.getProtectionRoleId(TownProtections.SWITCH_ACCESS);
        final String effectiveRoleId = this.resolveEffectiveRoleId(town, scopedChunk, TownProtections.SWITCH_ACCESS);

        return UnifiedBuilderFactory.item(this.resolveMaterial(TownProtections.SWITCH_ACCESS, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(TownProtections.SWITCH_ACCESS, context.getPlayer()))
            .setLore(this.i18n("switch_entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "role", this.resolveRoleDisplay(town, configuredRoleId, context.getPlayer()),
                    "effective_role", this.resolveRoleDisplay(town, effectiveRoleId, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town),
                    "action_count", TownProtections.switchActionValues().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createAlliedProtectionsItem(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        final boolean editable = this.protectionEditingUnlocked(context, town);
        return UnifiedBuilderFactory.item(editable ? Material.LIME_BANNER : Material.GRAY_DYE)
            .setName(this.i18n("allied_entry.name", context.getPlayer()).build().component())
            .setLore(this.i18n("allied_entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "scope", this.resolveScopeDisplay(context),
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
        final @NotNull String roleDisplay
    ) {
        new I18n.Builder(this.getKey() + ".updated", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "protection", this.protectionNameText(protection, clickContext.getPlayer()),
                "role", roleDisplay
            ))
            .build()
            .sendMessage();
    }

    private @NotNull String resolveEffectiveRoleId(
        final @NotNull RTown town,
        final @Nullable RTownChunk scopedChunk,
        final @NotNull TownProtections protection
    ) {
        final String chunkRoleId = scopedChunk == null ? null : this.resolveChunkRoleId(scopedChunk, protection);
        return chunkRoleId == null ? town.getProtectionRoleId(protection) : chunkRoleId;
    }

    private @Nullable String resolveChunkRoleId(
        final @NotNull RTownChunk scopedChunk,
        final @NotNull TownProtections protection
    ) {
        TownProtections currentProtection = protection;
        while (currentProtection != null) {
            final String configuredRoleId = scopedChunk.getProtectionRoleId(currentProtection);
            if (configuredRoleId != null) {
                return configuredRoleId;
            }
            currentProtection = currentProtection.getFallbackProtection();
        }
        return null;
    }

    private void sendUpdateFailedMessage(final @NotNull SlotClickContext clickContext) {
        new I18n.Builder(this.getKey() + ".update_failed", clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull Material resolveMaterial(final @NotNull TownProtections protection, final boolean editable) {
        if (!editable) {
            return Material.GRAY_DYE;
        }
        return switch (protection) {
            case BREAK_BLOCK -> Material.DIAMOND_PICKAXE;
            case PLACE_BLOCK -> Material.GRASS_BLOCK;
            case SWITCH_ACCESS -> Material.REDSTONE_TORCH;
            case ITEM_USE -> Material.ENDER_PEARL;
            default -> Material.LIME_STAINED_GLASS_PANE;
        };
    }
}
