/*
package com.raindropcentral.rdq2.view.perks;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.perk.RPerk;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.manager.perk.PerkManager;
import com.raindropcentral.rdq2.perk.runtime.LoadedPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Confirmation view for unlocking a perk.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 *//*

public class PerkUnlockView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkUnlockView.class.getName());

    // Slot positions
    private static final int PERK_INFO_SLOT = 13;
    private static final int CONFIRM_SLOT = 20;
    private static final int CANCEL_SLOT = 24;
    private static final int COST_INFO_SLOT = 22;

    // States
    private final State<RDQ> rdq = initialState("plugin");
    private final State<String> perkId = initialState("perkId");
    private final State<RDQPlayer> player = initialState("player");

    @Override
    protected String getKey() {
        return "perk_unlock_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final String id = this.perkId.get(openContext);
        final RDQ plugin = this.rdq.get(openContext);
        final LoadedPerk perk = plugin.getPerkRegistry().get(id);

        final String perkName = perk != null ?
                this.i18n(perk.config().displayName(), openContext.getPlayer()).build().toString() :
                id;

        return Map.of("perk_name", perkName);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        try {
            final RDQ plugin = this.rdq.get(render);
            final String id = this.perkId.get(render);
            final RDQPlayer rdqPlayer = this.player.get(render);
            final LoadedPerk perk = plugin.getPerkRegistry().get(id);

            if (perk == null) {
                player.sendMessage(this.i18n("perk.error.not_found", player)
                        .with("perk", id)
                        .build().toString());
                player.closeInventory();
                return;
            }

            // Perk info
            render.slot(PERK_INFO_SLOT)
                    .withItem(this.createPerkInfoItem(player, perk));

            // Cost info (if any)
            render.slot(COST_INFO_SLOT)
                    .withItem(this.createCostInfoItem(player));

            // Confirm button
            render.slot(CONFIRM_SLOT)
                    .withItem(this.createConfirmButton(player))
                    .onClick(click -> {
                        // Unlock the perk
                        final RPerk rPerk = plugin.getPerkRepository()
                                .findByAttributes(Map.of("identifier", id));
                        
                        if (rPerk != null && rdqPlayer != null) {
                            final PerkManager perkManager = plugin.getPerkInitializationManager().getPerkManager();
                            perkManager.getPerkStateService().grantPerk(rdqPlayer, rPerk, false);
                            
                            player.sendMessage(this.i18n("perk.messages.unlocked", player)
                                    .with("perk_name", perk.config().displayName())
                                    .build().toString());
                        }

                        click.closeForPlayer();
                        plugin.getViewFrame().open(
                                PerkDetailView.class,
                                player,
                                Map.of(
                                        "plugin", plugin,
                                        "perkId", id,
                                        "player", rdqPlayer
                                )
                        );
                    });

            // Cancel button
            render.slot(CANCEL_SLOT)
                    .withItem(this.createCancelButton(player))
                    .onClick(click -> {
                        click.closeForPlayer();
                        plugin.getViewFrame().open(
                                PerkDetailView.class,
                                player,
                                Map.of(
                                        "plugin", plugin,
                                        "perkId", id,
                                        "player", rdqPlayer
                                )
                        );
                    });

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering perk unlock view", e);
        }
    }

    private ItemStack createPerkInfoItem(@NotNull Player player, @NotNull LoadedPerk perk) {
        final List<Component> lore = new ArrayList<>();

        // Description
        final String desc = perk.config().description();
        if (desc != null && !desc.isEmpty()) {
            lore.add(this.i18n(desc, player).build().component());
            lore.add(Component.empty());
        }

        lore.add(Component.text("§7Category: §f" + perk.config().category().getDisplayName()));
        lore.add(Component.text("§7Priority: §f" + perk.config().priority()));

        return UnifiedBuilderFactory.item(Material.valueOf(perk.config().iconMaterial()))
                .setName(this.i18n(perk.config().displayName(), player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    private ItemStack createCostInfoItem(@NotNull Player player) {
        final List<Component> lore = new ArrayList<>();
        this.i18n("perk.unlock_ui.cost_info.lore", player)
                .build()
                .splitLines()
                .forEach(lore::add);

        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("perk.unlock_ui.cost_info.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createConfirmButton(@NotNull Player player) {
        final List<Component> lore = new ArrayList<>();
        this.i18n("perk.unlock_ui.confirm.lore", player)
                .build()
                .splitLines()
                .forEach(lore::add);

        return UnifiedBuilderFactory.item(Material.LIME_DYE)
                .setName(this.i18n("perk.unlock_ui.confirm.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createCancelButton(@NotNull Player player) {
        final List<Component> lore = new ArrayList<>();
        this.i18n("perk.unlock_ui.cancel.lore", player)
                .build()
                .splitLines()
                .forEach(lore::add);

        return UnifiedBuilderFactory.item(Material.RED_DYE)
                .setName(this.i18n("perk.unlock_ui.cancel.name", player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
*/
