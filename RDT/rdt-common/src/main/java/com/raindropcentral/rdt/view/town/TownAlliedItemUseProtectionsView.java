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
import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated editor for allied item-use access, including a bulk action and paginated per-item
 * overrides.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownAlliedItemUseProtectionsView extends AbstractTownProtectionView {

    public static final String VIEW_KEY = "allied_item_use";

    private static final String PAGE_KEY = "allied_item_use_page";
    private static final int[] PROTECTION_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    /**
     * Creates the allied item-use protections editor.
     */
    public TownAlliedItemUseProtectionsView() {
        super(TownAlliedProtectionsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_allied_item_use_protections_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "a   s   m",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "         ",
            "   <p>   "
        };
    }

    /**
     * Renders summary, bulk controls, pagination controls, and item-use entries.
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

        final List<TownProtections> itemUseProtections = TownProtections.itemUseActionValues();
        final int maxPage = this.resolveMaxPage(itemUseProtections.size());
        final int currentPage = this.resolveCurrentPage(render, maxPage);

        render.layoutSlot('a', this.createBulkItem(render))
            .onClick(this::handleBulkClick);
        render.layoutSlot('s', this.createSummaryItem(render));
        render.layoutSlot('m', this.createScopeItem(render))
            .onClick(this::handleScopeClick);

        this.renderPaginationControls(render, player, currentPage, maxPage, itemUseProtections.size());

        final int startIndex = currentPage * PROTECTION_SLOTS.length;
        final int endIndex = Math.min(startIndex + PROTECTION_SLOTS.length, itemUseProtections.size());
        for (int index = startIndex; index < endIndex; index++) {
            final TownProtections protection = itemUseProtections.get(index);
            final int slot = PROTECTION_SLOTS[index - startIndex];
            render.slot(slot).renderWith(() -> this.createProtectionItem(render, protection))
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

    private void renderPaginationControls(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final int currentPage,
        final int maxPage,
        final int itemCount
    ) {
        render.layoutSlot('p', this.createPageIndicatorItem(player, currentPage, maxPage, itemCount));
        if (currentPage > 0) {
            render.layoutSlot('<', new Previous().getHead(player))
                .onClick(clickContext -> this.openPage(clickContext, currentPage - 1));
        }
        if (currentPage < maxPage) {
            render.layoutSlot('>', new Next().getHead(player))
                .onClick(clickContext -> this.openPage(clickContext, currentPage + 1));
        }
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

    private void handleBulkClick(final @NotNull SlotClickContext clickContext) {
        final RTown town = this.resolveTown(clickContext);
        if (town == null || this.plugin.get(clickContext).getTownRuntimeService() == null) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        if (!this.ensureProtectionAccess(clickContext, town)) {
            return;
        }

        final RTownChunk scopedChunk = this.resolveChunk(clickContext);
        final List<TownProtections> bulkProtections = TownProtections.itemUseBulkValues();
        final String stateDisplay;
        final boolean updated;
        if (scopedChunk == null) {
            final boolean nextAllowed = !this.plugin.get(clickContext).getTownRuntimeService().isAlliedProtectionAllowed(
                town,
                null,
                TownProtections.ITEM_USE
            );
            updated = this.plugin.get(clickContext).getTownRuntimeService().setTownAlliedProtectionAllowed(
                town,
                bulkProtections,
                nextAllowed
            );
            stateDisplay = this.resolveAlliedStateDisplay(nextAllowed, clickContext.getPlayer());
        } else {
            final Boolean nextAllowed = this.nextChunkState(
                scopedChunk.getConfiguredAlliedProtectionAllowed(TownProtections.ITEM_USE)
            );
            updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkAlliedProtectionAllowed(
                scopedChunk,
                bulkProtections,
                nextAllowed
            );
            stateDisplay = this.resolveAlliedStateDisplay(nextAllowed, clickContext.getPlayer());
        }

        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }

        this.sendBulkUpdatedMessage(clickContext, stateDisplay);
        clickContext.update();
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
        final String stateDisplay;
        final boolean updated;
        if (scopedChunk == null) {
            final boolean nextAllowed = !this.plugin.get(clickContext).getTownRuntimeService().isAlliedProtectionAllowed(
                town,
                null,
                protection
            );
            updated = this.plugin.get(clickContext).getTownRuntimeService().setTownAlliedProtectionAllowed(
                town,
                protection,
                nextAllowed
            );
            stateDisplay = this.resolveAlliedStateDisplay(nextAllowed, clickContext.getPlayer());
        } else {
            final Boolean nextAllowed = this.nextChunkState(scopedChunk.getConfiguredAlliedProtectionAllowed(protection));
            updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkAlliedProtectionAllowed(
                scopedChunk,
                protection,
                nextAllowed
            );
            stateDisplay = this.resolveAlliedStateDisplay(nextAllowed, clickContext.getPlayer());
        }

        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }
        this.sendUpdatedMessage(clickContext, protection, stateDisplay);
        clickContext.update();
    }

    private void openPage(final @NotNull SlotClickContext clickContext, final int page) {
        clickContext.openForPlayer(TownAlliedItemUseProtectionsView.class, this.createItemUseNavigationData(clickContext, page));
    }

    private @Nullable Boolean nextChunkState(final @Nullable Boolean currentState) {
        if (currentState == null) {
            return Boolean.TRUE;
        }
        return currentState ? Boolean.FALSE : null;
    }

    private @NotNull Map<String, Object> createItemUseNavigationData(final @NotNull Context context, final int page) {
        final Map<String, Object> data = new LinkedHashMap<>(this.createProtectionNavigationData(context, null, VIEW_KEY));
        data.put(PAGE_KEY, Math.max(0, page));
        return data;
    }

    private int resolveCurrentPage(final @NotNull Context context, final int maxPage) {
        final Integer rawPage = this.asInteger(this.extractData(context) == null ? null : this.extractData(context).get(PAGE_KEY));
        if (rawPage == null) {
            return 0;
        }
        return Math.max(0, Math.min(rawPage, maxPage));
    }

    private int resolveMaxPage(final int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        return (itemCount - 1) / PROTECTION_SLOTS.length;
    }

    private @NotNull ItemStack createBulkItem(final @NotNull Context context) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        final RTownChunk scopedChunk = this.resolveChunk(context);
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
            .setName(this.i18n("bulk.name", context.getPlayer()).build().component())
            .setLore(this.i18n("bulk.lore", context.getPlayer())
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

    private @NotNull ItemStack createProtectionItem(
        final @NotNull Context context,
        final @NotNull TownProtections protection
    ) {
        final RTown town = this.resolveTown(context);
        if (town == null) {
            return this.createMissingItem(context.getPlayer());
        }

        final RTownChunk scopedChunk = this.resolveChunk(context);
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

    private @NotNull ItemStack createPageIndicatorItem(
        final @NotNull Player player,
        final int currentPage,
        final int maxPage,
        final int itemCount
    ) {
        return UnifiedBuilderFactory.item(Material.PAPER)
            .setName(this.i18n("page.name", player)
                .withPlaceholders(Map.of(
                    "current_page", currentPage + 1,
                    "total_pages", Math.max(1, maxPage + 1)
                ))
                .build()
                .component())
            .setLore(this.i18n("page.lore", player)
                .withPlaceholders(Map.of(
                    "items_count", itemCount,
                    "current_page", currentPage + 1,
                    "total_pages", Math.max(1, maxPage + 1)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void sendBulkUpdatedMessage(final @NotNull SlotClickContext clickContext, final @NotNull String stateDisplay) {
        new I18n.Builder(this.getKey() + ".updated_all", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholder("state", stateDisplay)
            .build()
            .sendMessage();
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
