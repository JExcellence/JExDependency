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
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Confirmation view for issuing a bound town nexus item.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class CreateTownConfirmView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<String> draftTownName = initialState("draftTownName");
    private final State<String> draftTownColor = initialState("draftTownColor");

    /**
     * Creates the town-creation confirmation view.
     */
    public CreateTownConfirmView() {
        super(TownHubView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_create_confirm_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return confirmation layout
     */
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

    /**
     * Renders the town summary and confirm/cancel actions.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        render.layoutSlot('s', this.createSummaryItem(player, this.draftTownName.get(render), this.draftTownColor.get(render)));
        render.layoutSlot('c', this.createConfirmItem(player))
            .onClick(clickContext -> this.handleConfirm(clickContext));
        render.layoutSlot('x', this.createCancelItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownHubView.class,
                Map.of("plugin", this.plugin.get(clickContext))
            ));
    }

    /**
     * Cancels vanilla inventory interaction for the menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleConfirm(final @NotNull SlotClickContext clickContext) {
        final RDT rdt = this.plugin.get(clickContext);
        if (rdt.getTownRuntimeService() != null && rdt.getTownRuntimeService().getTownFor(clickContext.getPlayer().getUniqueId()) != null) {
            new I18n.Builder("town_create_confirm_ui.error.already_in_town", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            clickContext.openForPlayer(TownHubView.class, Map.of("plugin", rdt));
            return;
        }

        final ItemStack nexusItem = Nexus.getNexusItem(
            rdt,
            clickContext.getPlayer(),
            UUID.randomUUID(),
            this.draftTownName.get(clickContext),
            this.draftTownColor.get(clickContext)
        );
        clickContext.getPlayer().getInventory().addItem(nexusItem)
            .values()
            .forEach(overflow -> clickContext.getPlayer().getWorld().dropItemNaturally(clickContext.getPlayer().getLocation(), overflow));
        new I18n.Builder("town_create_confirm_ui.confirmed", clickContext.getPlayer())
            .includePrefix()
            .withPlaceholder("town_name", this.draftTownName.get(clickContext))
            .build()
            .sendMessage();
        new I18n.Builder("town_create_confirm_ui.place_hint", clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
        clickContext.closeForPlayer();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull String townName,
        final @NotNull String townColor
    ) {
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", townName,
                    "town_color", townColor
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
}
