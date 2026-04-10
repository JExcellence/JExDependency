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
import com.raindropcentral.rdt.service.NationActionStatus;
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

import java.util.Map;
import java.util.UUID;

/**
 * Confirmation view for destructive nation actions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationConfirmView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<UUID> nationUuid = initialState("nation_uuid");
    private final State<String> confirmAction = initialState("confirm_action");
    private final State<UUID> targetTownUuid = initialState("target_town_uuid");

    /**
     * Creates the nation confirmation view.
     */
    public TownNationConfirmView() {
        super(TownNationView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_confirm_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "   c x   ",
            "         ",
            "         ",
            "         "
        };
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        final RNation nation = this.resolveNation(render);
        render.layoutSlot('s', this.createSummaryItem(player, nation, this.resolveAction(render), this.resolveTargetTown(render)));
        render.layoutSlot('c', this.createConfirmItem(player))
            .onClick(clickContext -> this.handleConfirm(clickContext, town, nation));
        render.layoutSlot('x', this.createCancelItem(player))
            .onClick(clickContext -> {
                if (town != null) {
                    clickContext.openForPlayer(TownNationView.class, Map.of("plugin", this.plugin.get(clickContext), "town_uuid", town.getTownUUID()));
                } else {
                    clickContext.back();
                }
            });
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleConfirm(
        final @NotNull SlotClickContext clickContext,
        final @Nullable RTown town,
        final @Nullable RNation nation
    ) {
        final TownRuntimeService runtimeService = this.plugin.get(clickContext).getTownRuntimeService();
        final String action = this.resolveAction(clickContext);
        final RTown targetTown = this.resolveTargetTown(clickContext);
        final NationActionResult result = runtimeService == null
            ? new NationActionResult(NationActionStatus.INVALID_TARGET, nation)
            : switch (action) {
                case "leave" -> runtimeService.leaveNation(clickContext.getPlayer());
                case "disband" -> nation == null
                    ? new NationActionResult(NationActionStatus.INVALID_TARGET, null)
                    : runtimeService.disbandNation(clickContext.getPlayer(), nation);
                case "kick" -> nation == null || targetTown == null
                    ? new NationActionResult(NationActionStatus.INVALID_TARGET, nation)
                    : runtimeService.kickTownFromNation(clickContext.getPlayer(), nation, targetTown.getTownUUID());
                default -> new NationActionResult(NationActionStatus.INVALID_TARGET, nation);
            };
        this.sendResultMessage(clickContext.getPlayer(), action, result, targetTown);
        if (town != null) {
            clickContext.openForPlayer(TownNationView.class, Map.of("plugin", this.plugin.get(clickContext), "town_uuid", town.getTownUUID()));
        } else {
            clickContext.closeForPlayer();
        }
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.townUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.townUuid.get(context));
    }

    private @Nullable RNation resolveNation(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.nationUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getNation(this.nationUuid.get(context));
    }

    private @Nullable RTown resolveTargetTown(final @NotNull Context context) {
        return this.plugin.get(context).getTownRuntimeService() == null || this.targetTownUuid.get(context) == null
            ? null
            : this.plugin.get(context).getTownRuntimeService().getTown(this.targetTownUuid.get(context));
    }

    private @NotNull String resolveAction(final @NotNull Context context) {
        return this.confirmAction.get(context) == null ? "leave" : this.confirmAction.get(context);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @Nullable RNation nation,
        final @NotNull String action,
        final @Nullable RTown targetTown
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "action", action,
                    "nation_name", nation == null ? "-" : nation.getNationName(),
                    "target_town", targetTown == null ? "-" : targetTown.getTownName()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createConfirmItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.LIME_DYE)
            .setName(this.i18n("confirm.name", player).build().component())
            .setLore(this.i18n("confirm.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(true)
            .build();
    }

    private @NotNull ItemStack createCancelItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.RED_DYE)
            .setName(this.i18n("cancel.name", player).build().component())
            .setLore(this.i18n("cancel.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void sendResultMessage(
        final @NotNull Player player,
        final @NotNull String action,
        final @NotNull NationActionResult result,
        final @Nullable RTown targetTown
    ) {
        final RNation nation = result.nation();
        new I18n.Builder("town_nation_shared.messages." + action + '.' + TownNationViewSupport.toActionMessageKey(result.status()), player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "nation_name", nation == null ? "-" : nation.getNationName(),
                "target_town", targetTown == null ? "-" : targetTown.getTownName()
            ))
            .build()
            .sendMessage();
    }
}
