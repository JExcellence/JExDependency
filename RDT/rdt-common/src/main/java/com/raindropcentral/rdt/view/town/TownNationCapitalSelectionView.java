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
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated member selector for promoting a new nation capital.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationCapitalSelectionView extends APaginatedView<RTown> {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<UUID> nationUuid = initialState("nation_uuid");

    /**
     * Creates the capital-selection view.
     */
    public TownNationCapitalSelectionView() {
        super(TownNationView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_capital_ui";
    }

    @Override
    protected @NotNull CompletableFuture<List<RTown>> getAsyncPaginationSource(final @NotNull Context context) {
        final RNation nation = this.resolveNation(context);
        return nation == null || this.plugin.get(context).getTownRuntimeService() == null
            ? CompletableFuture.completedFuture(List.of())
            : CompletableFuture.completedFuture(
                this.plugin.get(context).getTownRuntimeService().getNationMemberTowns(nation).stream()
                    .filter(town -> !town.getTownUUID().equals(nation.getCapitalTownUuid()))
                    .toList()
            );
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RTown entry
    ) {
        builder.withItem(this.createTownItem(context, entry))
            .onClick(clickContext -> this.handlePromote(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RNation nation = this.resolveNation(render);
        if (nation == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        final int eligibleCount = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        render.slot(4).renderWith(() -> this.createSummaryItem(player, nation, eligibleCount));
        if (eligibleCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handlePromote(final @NotNull SlotClickContext clickContext, final @NotNull RTown targetTown) {
        final RTown town = this.resolveTown(clickContext);
        final RNation nation = this.resolveNation(clickContext);
        if (town == null || nation == null) {
            clickContext.back();
            return;
        }

        final NationActionResult result = this.plugin.get(clickContext)
            .getTownRuntimeService()
            .promoteNationCapital(clickContext.getPlayer(), nation, targetTown.getTownUUID());
        new I18n.Builder("town_nation_shared.messages.promote." + TownNationViewSupport.toActionMessageKey(result.status()), clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(Map.of(
                "nation_name", nation.getNationName(),
                "target_town", targetTown.getTownName()
            ))
            .build()
            .sendMessage();
        clickContext.openForPlayer(TownNationView.class, Map.of("plugin", this.plugin.get(clickContext), "town_uuid", town.getTownUUID()));
    }

    private @Nullable RNation resolveNation(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.nationUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getNation(this.nationUuid.get(context));
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.townUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.townUuid.get(context));
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final @NotNull RNation nation, final int eligibleCount) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "nation_name", nation.getNationName(),
                    "eligible_town_count", eligibleCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTownItem(final @NotNull Context context, final @NotNull RTown town) {
        return UnifiedBuilderFactory.item(Material.LIME_DYE)
            .setName(this.i18n("entry.name", context.getPlayer())
                .withPlaceholder("town_name", town.getTownName())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "nexus_level", town.getNexusLevel()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_DYE)
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
}
