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
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
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
 * Lightweight summary view for the shared town bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownBankView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<Boolean> remoteBank = initialState("remote_bank");

    /**
     * Creates the town bank view.
     */
    public TownBankView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_bank_ui";
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
            "    b    ",
            "    r    ",
            "         ",
            "         ",
            "         "
        };
    }

    /**
     * Renders bank summary information.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render);
        if (town == null) {
            render.slot(22).renderWith(() -> this.createMissingTownItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, town));
        render.layoutSlot('b', this.createBalanceItem(player, town));
        render.layoutSlot('r', this.createAccessItem(player, Boolean.TRUE.equals(this.remoteBank.get(render))));
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

    private @Nullable RTown resolveTown(final @NotNull RenderContext render) {
        final UUID resolvedTownUuid = this.townUuid.get(render);
        return resolvedTownUuid == null || this.plugin.get(render).getTownRuntimeService() == null
            ? null
            : this.plugin.get(render).getTownRuntimeService().getTown(resolvedTownUuid);
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final @NotNull RTown town) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "storage_slots", town.getSharedBankStorage().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createBalanceItem(final @NotNull Player player, final @NotNull RTown town) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
            .setName(this.i18n("balance.name", player).build().component())
            .setLore(this.i18n("balance.lore", player)
                .withPlaceholders(Map.of(
                    "vault_balance", town.getBank(),
                    "currency_count", town.getBankCurrencyCount()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createAccessItem(final @NotNull Player player, final boolean remoteAccess) {
        final Material material = remoteAccess ? Material.ENDER_CHEST : Material.BARREL;
        final String key = remoteAccess ? "access.remote" : "access.local";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n(key + ".name", player).build().component())
            .setLore(this.i18n(key + ".lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
