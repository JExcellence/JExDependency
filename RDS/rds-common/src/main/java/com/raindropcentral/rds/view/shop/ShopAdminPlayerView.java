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

import java.util.Map;

/**
 * RDS admin player-management landing view.
 *
 * <p>This panel links player/group override editors, shop force-controls, and the global
 * force-close action for open RDS views.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminPlayerView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the admin player-management landing view.
     */
    public ShopAdminPlayerView() {
        super(ShopAdminView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_player_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                "  p g o  ",
                "    f a  ",
                "         "
        };
    }

    /**
     * Renders admin player-management controls.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(13).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player));

        render.layoutSlot('p', this.createPlayerOverridesItem(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminPlayerSelectView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('g', this.createGroupOverridesItem(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminGroupEditView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('o', this.createOpenAsOwnerItem(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminShopControlView.class,
                        Map.of(
                                "plugin", this.rds.get(clickContext),
                                "actionMode", ShopAdminShopControlMode.OPEN_AS_OWNER.name()
                        )
                ));

        render.layoutSlot('f', this.createForceCloseShopItem(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminShopControlView.class,
                        Map.of(
                                "plugin", this.rds.get(clickContext),
                                "actionMode", ShopAdminShopControlMode.FORCE_CLOSE_SHOP.name()
                        )
                ));

        render.layoutSlot('a', this.createForceCloseAllViewsItem(player))
                .onClick(this::handleForceCloseAllViewsClick);
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleForceCloseAllViewsClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.access_denied_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final int closedViews = ShopAdminForceCloseSupport.closeAllRdsViews(this.rds.get(clickContext));
        this.i18n("feedback.force_close_all_views", clickContext.getPlayer())
                .withPlaceholder("closed_views", closedViews)
                .includePrefix()
                .build()
                .sendMessage();
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

    private @NotNull ItemStack createPlayerOverridesItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                .setName(this.i18n("actions.player_overrides.name", player).build().component())
                .setLore(this.i18n("actions.player_overrides.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createGroupOverridesItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
                .setName(this.i18n("actions.group_overrides.name", player).build().component())
                .setLore(this.i18n("actions.group_overrides.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createOpenAsOwnerItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.SPYGLASS)
                .setName(this.i18n("actions.open_as_owner.name", player).build().component())
                .setLore(this.i18n("actions.open_as_owner.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createForceCloseShopItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("actions.force_close_shop.name", player).build().component())
                .setLore(this.i18n("actions.force_close_shop.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createForceCloseAllViewsItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.REDSTONE_BLOCK)
                .setName(this.i18n("actions.force_close_all_views.name", player).build().component())
                .setLore(this.i18n("actions.force_close_all_views.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.access_denied.name", player).build().component())
                .setLore(this.i18n("feedback.access_denied.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private boolean hasAdminAccess(
            final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }
}
