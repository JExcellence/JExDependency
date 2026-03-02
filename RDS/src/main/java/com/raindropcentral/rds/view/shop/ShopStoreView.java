package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ShopStoreView extends BaseView {

    private final State<RDS> rds = initialState("plugin");

    @Override
    protected String getKey() {
        return "shop_store_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                "  o m c  ",
                "         "
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    public void renderNavigationButtons(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        // Root store view does not use a return button.
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final RDSPlayer rdsPlayer = this.getOrCreatePlayer(render);
        final int ownedShops = rdsPlayer.getShops();
        final int availableCurrencyCount = ShopStorePricingSupport
                .getAvailableStoreCosts(plugin, config, ownedShops)
                .size();

        render.layoutSlot('o')
                .renderWith(() -> this.createOwnedShopsItem(player, ownedShops))
                .updateOnStateChange(this.rds);

        render.layoutSlot('m')
                .renderWith(() -> this.createMaxShopsItem(player, config, ownedShops))
                .updateOnStateChange(this.rds);

        render.layoutSlot('c')
                .renderWith(() -> this.createCostsItem(player, availableCurrencyCount))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopStoreCostView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private @NotNull RDSPlayer getOrCreatePlayer(
            final @NotNull Context context
    ) {
        final Player player = context.getPlayer();
        final RDS plugin = this.rds.get(context);
        final RDSPlayer existingPlayer = plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(player.getUniqueId());
        plugin.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    private @NotNull ItemStack createOwnedShopsItem(
            final @NotNull Player player,
            final int ownedShops
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("owned_shops.name", player).build().component())
                .setLore(this.i18n("owned_shops.lore", player)
                        .withPlaceholder("owned_shops", ownedShops)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createMaxShopsItem(
            final @NotNull Player player,
            final @NotNull ConfigSection config,
            final int ownedShops
    ) {
        final boolean limited = config.hasShopLimit();
        final String maxShopsDisplay = limited ? Integer.toString(config.getMaxShops()) : "No limit";

        return UnifiedBuilderFactory.item(limited ? Material.BEACON : Material.LIME_STAINED_GLASS_PANE)
                .setName(this.i18n("max_shops.name", player).build().component())
                .setLore(this.i18n("max_shops.lore", player)
                        .withPlaceholders(Map.of(
                                "owned_shops", ownedShops,
                                "max_shops", maxShopsDisplay
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCostsItem(
            final @NotNull Player player,
            final int currencyCount
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("costs.name", player).build().component())
                .setLore(this.i18n("costs.lore", player)
                        .withPlaceholder("currency_count", currencyCount)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
