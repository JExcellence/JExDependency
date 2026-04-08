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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated editor for switch-style town protections, including a bulk action and paginated
 * per-action overrides.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownSwitchProtectionsView extends AbstractTownProtectionView {

    public static final String VIEW_KEY = "switch_actions";

    private static final String PAGE_KEY = "switch_page";
    private static final int[] PROTECTION_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    /**
     * Creates the switch-action protections editor.
     */
    public TownSwitchProtectionsView() {
        super(TownRoleProtectionsView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_switch_protections_ui";
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
     * Renders summary, bulk controls, pagination controls, and switch-action entries.
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

        final List<TownProtections> switchProtections = TownProtections.switchActionValues();
        final int maxPage = this.resolveMaxPage(switchProtections.size());
        final int currentPage = this.resolveCurrentPage(render, maxPage);

        render.layoutSlot('a', this.createBulkItem(render))
            .onClick(this::handleBulkClick);
        render.layoutSlot('s', this.createSummaryItem(render));
        render.layoutSlot('m', this.createScopeItem(render))
            .onClick(this::handleScopeClick);

        this.renderPaginationControls(render, player, currentPage, maxPage, switchProtections.size());

        final int startIndex = currentPage * PROTECTION_SLOTS.length;
        final int endIndex = Math.min(startIndex + PROTECTION_SLOTS.length, switchProtections.size());
        for (int index = startIndex; index < endIndex; index++) {
            final TownProtections protection = switchProtections.get(index);
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
            this.createProtectionNavigationData(clickContext, TownProtectionCategory.ROLE_BASED, VIEW_KEY)
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

        final List<String> roleOrder = new ArrayList<>(this.plugin.get(clickContext).getTownRuntimeService().getProtectionRoleOrder(town));
        final RTownChunk scopedChunk = this.resolveChunk(clickContext);
        final List<TownProtections> bulkProtections = TownProtections.switchBulkValues();
        final String nextRoleId;
        final boolean updated;
        if (scopedChunk == null) {
            nextRoleId = this.nextRoleId(roleOrder, town.getProtectionRoleId(TownProtections.SWITCH_ACCESS), false);
            updated = this.plugin.get(clickContext).getTownRuntimeService().setTownProtectionRoleIds(
                town,
                bulkProtections,
                nextRoleId
            );
        } else {
            nextRoleId = this.nextRoleId(roleOrder, scopedChunk.getProtectionRoleId(TownProtections.SWITCH_ACCESS), true);
            updated = this.plugin.get(clickContext).getTownRuntimeService().setChunkProtectionRoleIds(
                scopedChunk,
                bulkProtections,
                nextRoleId
            );
        }

        if (!updated) {
            this.sendUpdateFailedMessage(clickContext);
            return;
        }

        this.sendBulkUpdatedMessage(clickContext, town, nextRoleId);
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

    private void openPage(final @NotNull SlotClickContext clickContext, final int page) {
        clickContext.openForPlayer(TownSwitchProtectionsView.class, this.createSwitchNavigationData(clickContext, page));
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

    private @NotNull Map<String, Object> createSwitchNavigationData(final @NotNull Context context, final int page) {
        final Map<String, Object> data = new LinkedHashMap<>(
            this.createProtectionNavigationData(context, TownProtectionCategory.ROLE_BASED, VIEW_KEY)
        );
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
        final String configuredRoleId = scopedChunk == null
            ? town.getConfiguredProtectionRoleId(TownProtections.SWITCH_ACCESS)
            : scopedChunk.getProtectionRoleId(TownProtections.SWITCH_ACCESS);
        final String effectiveRoleId = this.resolveEffectiveRoleId(town, scopedChunk, TownProtections.SWITCH_ACCESS);

        return UnifiedBuilderFactory.item(this.resolveMaterial(TownProtections.SWITCH_ACCESS, this.protectionEditingUnlocked(context, town)))
            .setName(this.i18n("bulk.name", context.getPlayer()).build().component())
            .setLore(this.i18n("bulk.lore", context.getPlayer())
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
            ? town.getConfiguredProtectionRoleId(protection)
            : scopedChunk.getProtectionRoleId(protection);
        final String effectiveRoleId = this.resolveEffectiveRoleId(town, scopedChunk, protection);

        return UnifiedBuilderFactory.item(this.resolveMaterial(protection, this.protectionEditingUnlocked(context, town)))
            .setName(this.protectionNameComponent(protection, context.getPlayer()))
            .setLore(this.i18n("entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "role", this.resolveRoleDisplay(town, configuredRoleId, context.getPlayer()),
                    "effective_role", this.resolveRoleDisplay(town, effectiveRoleId, context.getPlayer()),
                    "access_state", this.resolveAccessStateDisplay(context, town)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
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

    private void sendBulkUpdatedMessage(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTown town,
        final @Nullable String roleId
    ) {
        new I18n.Builder(this.getKey() + ".updated_all", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholder("role", this.resolveRoleDisplay(town, roleId, clickContext.getPlayer()))
            .build()
            .sendMessage();
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

    private void sendUpdateFailedMessage(final @NotNull SlotClickContext clickContext) {
        new I18n.Builder(this.getKey() + ".update_failed", clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createPageIndicatorItem(
        final @NotNull Player player,
        final int currentPage,
        final int maxPage,
        final int itemCount
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("page.name", player)
                .withPlaceholders(Map.of(
                    "current_page", currentPage + 1,
                    "total_pages", maxPage + 1
                ))
                .build()
                .component())
            .setLore(this.i18n("page.lore", player)
                .withPlaceholders(Map.of(
                    "current_page", currentPage + 1,
                    "total_pages", maxPage + 1,
                    "items_count", itemCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull Material resolveMaterial(final @NotNull TownProtections protection, final boolean editable) {
        if (!editable) {
            return Material.GRAY_DYE;
        }
        return switch (protection) {
            case SWITCH_ACCESS -> Material.REDSTONE_TORCH;
            case CHEST -> Material.CHEST;
            case SHULKER_BOXES -> Material.SHULKER_BOX;
            case TRAPPED_CHEST -> Material.TRAPPED_CHEST;
            case FURNACE -> Material.FURNACE;
            case BLAST_FURNACE -> Material.BLAST_FURNACE;
            case DISPENSER -> Material.DISPENSER;
            case HOPPER -> Material.HOPPER;
            case DROPPER -> Material.DROPPER;
            case JUKEBOX -> Material.JUKEBOX;
            case STONECUTTER -> Material.STONECUTTER;
            case SMITHING_TABLE -> Material.SMITHING_TABLE;
            case FLETCHING_TABLE -> Material.FLETCHING_TABLE;
            case SMOKER -> Material.SMOKER;
            case LOOM -> Material.LOOM;
            case GRINDSTONE -> Material.GRINDSTONE;
            case COMPOSTER -> Material.COMPOSTER;
            case CARTOGRAPHY_TABLE -> Material.CARTOGRAPHY_TABLE;
            case BELL -> Material.BELL;
            case BARREL -> Material.BARREL;
            case BREWING_STAND -> Material.BREWING_STAND;
            case LEVER -> Material.LEVER;
            case PRESSURE_PLATES -> Material.STONE_PRESSURE_PLATE;
            case BUTTONS -> Material.STONE_BUTTON;
            case WOOD_DOORS -> Material.OAK_DOOR;
            case FENCE_GATES -> Material.OAK_FENCE_GATE;
            case TRAPDOORS -> Material.OAK_TRAPDOOR;
            case MINECARTS -> Material.MINECART;
            case LODESTONE -> Material.LODESTONE;
            case RESPAWN_ANCHOR -> Material.RESPAWN_ANCHOR;
            case TARGET -> Material.TARGET;
            default -> Material.LIME_STAINED_GLASS_PANE;
        };
    }
}
