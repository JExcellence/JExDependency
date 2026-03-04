/*
 * ShopAdminView.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
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

/**
 * Renders the RDS admin landing view.
 */
public class ShopAdminView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the admin landing view.
     */
    public ShopAdminView() {
        super();
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                "    s    ",
                "    b    ",
                "         ",
                "         ",
                "         "
        };
    }

    /**
     * Renders the admin landing controls.
     *
     * @param render render context for this menu
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!player.hasPermission(ADMIN_COMMAND_PERMISSION)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player));

        render.layoutSlot('b', this.createServerBankButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ServerBankView.class,
                        java.util.Map.of("plugin", this.rds.get(clickContext))
                ));
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.COMMAND_BLOCK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createServerBankButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_BLOCK)
                .setName(this.i18n("actions.server_bank.name", player).build().component())
                .setLore(this.i18n("actions.server_bank.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.locked.name", player).build().component())
                .setLore(this.i18n("feedback.locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
