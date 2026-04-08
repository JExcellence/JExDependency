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
import com.raindropcentral.rdt.utils.TownProtectionCategory;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Protection category hub for town-global or chunk-specific security settings.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownProtectionsView extends AbstractTownProtectionView {

    /**
     * Creates the protections hub.
     */
    public TownProtectionsView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_protections_ui";
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
            "   r t   ",
            "         ",
            "    m    ",
            "         "
        };
    }

    /**
     * Renders summary, scope switching, and the two protection categories.
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

        render.layoutSlot('s', this.createSummaryItem(render));
        render.layoutSlot('r', this.createCategoryItem(render, TownProtectionCategory.ROLE_BASED))
            .onClick(clickContext -> this.handleCategoryClick(clickContext, TownProtectionCategory.ROLE_BASED));
        render.layoutSlot('t', this.createCategoryItem(render, TownProtectionCategory.BINARY_TOGGLE))
            .onClick(clickContext -> this.handleCategoryClick(clickContext, TownProtectionCategory.BINARY_TOGGLE));
        render.layoutSlot('m', this.createScopeItem(render))
            .onClick(this::handleScopeClick);
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

    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext clickContext) {
        final Map<String, Object> originChunkData = this.createOriginChunkNavigationData(clickContext);
        if (originChunkData != null) {
            clickContext.openForPlayer(TownChunkView.class, originChunkData);
            return;
        }
        super.handleBackButtonClick(clickContext);
    }

    static @NotNull Class<? extends View> resolveCategoryViewClass(final @NotNull TownProtectionCategory category) {
        return switch (category) {
            case ROLE_BASED -> TownRoleProtectionsView.class;
            case BINARY_TOGGLE -> TownToggleProtectionsView.class;
        };
    }

    private void handleScopeClick(final @NotNull SlotClickContext clickContext) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null || !this.ensureProtectionAccess(clickContext, town)) {
            return;
        }

        clickContext.openForPlayer(TownProtectionScopeView.class, this.createProtectionNavigationData(clickContext, null));
    }

    private void handleCategoryClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownProtectionCategory category
    ) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null || !this.ensureProtectionAccess(clickContext, town)) {
            return;
        }

        clickContext.openForPlayer(
            resolveCategoryViewClass(category),
            this.createProtectionNavigationData(clickContext, category)
        );
    }

    private @NotNull ItemStack createCategoryItem(
        final @NotNull Context context,
        final @NotNull TownProtectionCategory category
    ) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        return UnifiedBuilderFactory.item(this.resolveMaterial(category, this.protectionEditingUnlocked(context, town)))
            .setName(this.categoryNameComponent(category, context.getPlayer()))
            .setLore(this.i18n("category." + category.getTranslationKey() + ".lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "scope", this.resolveScopeDisplay(context),
                    "access_state", this.resolveAccessStateDisplay(context, town)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveMaterial(final @NotNull TownProtectionCategory category, final boolean editable) {
        if (!editable) {
            return Material.GRAY_DYE;
        }
        return category == TownProtectionCategory.ROLE_BASED ? Material.OAK_DOOR : Material.REDSTONE_TORCH;
    }
}
