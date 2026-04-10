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
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.NationActionResult;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paginated multi-select town list used to finalize nation formation.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationFormationSelectionView extends BaseView {

    private static final List<Integer> TOWN_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    );
    private static final int PAGE_SIZE = TOWN_SLOTS.size();
    private static final String PAGE_KEY = "nation_selection_page";

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the nation-formation selection view.
     */
    public TownNationFormationSelectionView() {
        super(TownNationView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_selection_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "p   f   n",
            "r        "
        };
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        final String draftNationName = this.resolveDraftNationName(render);
        final TownRuntimeService runtimeService = this.plugin.get(render).getTownRuntimeService();
        if (town == null || draftNationName == null || runtimeService == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            render.layoutSlot('r', this.createReturnItem(player)).onClick(SlotClickContext::back);
            return;
        }

        final List<RTown> eligibleTowns = runtimeService.getEligibleNationFormationTowns(town);
        final LinkedHashSet<UUID> selectedTownUuids = new LinkedHashSet<>(TownNationViewSupport.readUuidList(
            render,
            TownNationViewSupport.SELECTED_TOWN_UUIDS_KEY
        ));
        selectedTownUuids.removeIf(selectedTownUuid -> eligibleTowns.stream().noneMatch(t -> t.getTownUUID().equals(selectedTownUuid)));

        final int maxPage = Math.max(0, (eligibleTowns.size() - 1) / PAGE_SIZE);
        final int page = Math.max(0, Math.min(this.resolvePage(render), maxPage));
        final int startIndex = page * PAGE_SIZE;
        final int endIndex = Math.min(eligibleTowns.size(), startIndex + PAGE_SIZE);

        render.layoutSlot('s', this.createSummaryItem(player, draftNationName, eligibleTowns.size(), selectedTownUuids.size(), runtimeService.getTownNationMinTowns()));
        render.layoutSlot('p', this.createPageItem(player, "previous", page > 0))
            .onClick(clickContext -> {
                if (page > 0) {
                    clickContext.openForPlayer(
                        TownNationFormationSelectionView.class,
                        this.createNavigationData(clickContext, town, draftNationName, selectedTownUuids, page - 1)
                    );
                }
            });
        render.layoutSlot('n', this.createPageItem(player, "next", page < maxPage))
            .onClick(clickContext -> {
                if (page < maxPage) {
                    clickContext.openForPlayer(
                        TownNationFormationSelectionView.class,
                        this.createNavigationData(clickContext, town, draftNationName, selectedTownUuids, page + 1)
                    );
                }
            });
        render.layoutSlot('f', this.createFinalizeItem(player, selectedTownUuids.size(), runtimeService.getTownNationMinTowns()))
            .onClick(clickContext -> this.handleFinalize(clickContext, town, draftNationName, selectedTownUuids));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(TownNationView.class, this.reopenNationRoot(clickContext, town)));

        for (int index = 0; index < TOWN_SLOTS.size(); index++) {
            final int slot = TOWN_SLOTS.get(index);
            final int townIndex = startIndex + index;
            if (townIndex >= endIndex) {
                render.slot(slot).renderWith(() -> this.createEmptyItem(player));
                continue;
            }

            final RTown alliedTown = eligibleTowns.get(townIndex);
            final boolean selected = selectedTownUuids.contains(alliedTown.getTownUUID());
            render.slot(slot).renderWith(() -> this.createTownItem(player, alliedTown, selected))
                .onClick(clickContext -> this.toggleTownSelection(
                    clickContext,
                    town,
                    draftNationName,
                    selectedTownUuids,
                    page,
                    alliedTown.getTownUUID()
                ));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void toggleTownSelection(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTown town,
        final @NotNull String draftNationName,
        final @NotNull LinkedHashSet<UUID> selectedTownUuids,
        final int page,
        final @NotNull UUID toggledTownUuid
    ) {
        final LinkedHashSet<UUID> updatedSelection = new LinkedHashSet<>(selectedTownUuids);
        if (!updatedSelection.add(toggledTownUuid)) {
            updatedSelection.remove(toggledTownUuid);
        }
        clickContext.openForPlayer(
            TownNationFormationSelectionView.class,
            this.createNavigationData(clickContext, town, draftNationName, updatedSelection, page)
        );
    }

    private void handleFinalize(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTown town,
        final @NotNull String draftNationName,
        final @NotNull LinkedHashSet<UUID> selectedTownUuids
    ) {
        final NationActionResult result = this.plugin.get(clickContext)
            .getTownRuntimeService()
            .createNation(clickContext.getPlayer(), draftNationName, selectedTownUuids);
        this.sendResultMessage(clickContext.getPlayer(), result);
        clickContext.openForPlayer(TownNationView.class, this.reopenNationRoot(clickContext, town));
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.townUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.townUuid.get(context));
    }

    private @Nullable String resolveDraftNationName(final @NotNull Context context) {
        final Map<String, Object> data = TownNationViewSupport.copyInitialData(context);
        return data != null && data.get(TownNationViewSupport.DRAFT_NATION_NAME_KEY) instanceof String draftNationName
            ? draftNationName
            : null;
    }

    private int resolvePage(final @NotNull Context context) {
        final Map<String, Object> data = TownNationViewSupport.copyInitialData(context);
        if (data == null) {
            return 0;
        }
        final Object rawPage = data.get(PAGE_KEY);
        if (rawPage instanceof Number pageNumber) {
            return Math.max(0, pageNumber.intValue());
        }
        if (rawPage instanceof String serializedPage) {
            try {
                return Math.max(0, Integer.parseInt(serializedPage.trim()));
            } catch (final NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private @NotNull Map<String, Object> createNavigationData(
        final @NotNull Context context,
        final @NotNull RTown town,
        final @NotNull String draftNationName,
        final @NotNull LinkedHashSet<UUID> selectedTownUuids,
        final int page
    ) {
        final Map<String, Object> data = this.reopenNationRoot(context, town);
        data.put(TownNationViewSupport.DRAFT_NATION_NAME_KEY, draftNationName);
        data.put(TownNationViewSupport.SELECTED_TOWN_UUIDS_KEY, new ArrayList<>(selectedTownUuids));
        data.put(PAGE_KEY, page);
        return data;
    }

    private @NotNull Map<String, Object> reopenNationRoot(final @NotNull Context context, final @NotNull RTown town) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownNationViewSupport.copyInitialData(context);
        if (copiedData != null) {
            data.putAll(TownNationViewSupport.stripTransientData(copiedData));
        }
        data.put("plugin", this.plugin.get(context));
        data.put("town_uuid", town.getTownUUID());
        return data;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull String draftNationName,
        final int eligibleTownCount,
        final int selectedTownCount,
        final int minimumTowns
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "nation_name", draftNationName,
                    "eligible_town_count", eligibleTownCount,
                    "selected_town_count", selectedTownCount,
                    "minimum_towns", minimumTowns,
                    "total_towns", selectedTownCount + 1
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTownItem(
        final @NotNull Player player,
        final @NotNull RTown alliedTown,
        final boolean selected
    ) {
        return UnifiedBuilderFactory.item(selected ? Material.LIME_DYE : Material.GRAY_DYE)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("town_name", alliedTown.getTownName())
                .build()
                .component())
            .setLore(this.i18n("entry." + (selected ? "selected" : "available") + ".lore", player)
                .withPlaceholders(Map.of(
                    "town_name", alliedTown.getTownName(),
                    "nexus_level", alliedTown.getNexusLevel()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPageItem(
        final @NotNull Player player,
        final @NotNull String direction,
        final boolean enabled
    ) {
        final Material material = enabled ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE;
        final String loreKey = "page." + direction + (enabled ? ".lore" : ".locked.lore");
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("page." + direction + ".name", player).build().component())
            .setLore(this.i18n(loreKey, player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFinalizeItem(
        final @NotNull Player player,
        final int selectedTownCount,
        final int minimumTowns
    ) {
        final boolean ready = selectedTownCount + 1 >= minimumTowns;
        return UnifiedBuilderFactory.item(ready ? Material.LIME_DYE : Material.BARRIER)
            .setName(this.i18n("finalize.name", player).build().component())
            .setLore(this.i18n("finalize." + (ready ? "ready" : "locked") + ".lore", player)
                .withPlaceholders(Map.of(
                    "selected_town_count", selectedTownCount,
                    "minimum_towns", minimumTowns,
                    "total_towns", selectedTownCount + 1
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(ready)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
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

    private void sendResultMessage(final @NotNull Player player, final @NotNull NationActionResult result) {
        final RNation nation = result.nation();
        new I18n.Builder("town_nation_shared.messages.create." + TownNationViewSupport.toActionMessageKey(result.status()), player)
            .includePrefix()
            .withPlaceholders(Map.of("nation_name", nation == null ? "-" : nation.getNationName()))
            .build()
            .sendMessage();
    }
}
