package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.view.shop.ShopCustomerView;
import com.raindropcentral.rds.view.shop.ShopOverviewView;
import me.devnatan.inventoryframework.View;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;

@SuppressWarnings("unused")
public class PlayerClickListener implements Listener {

    private final RDS rds;

    public PlayerClickListener(RDS rds) {
        this.rds = rds;
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event == null) return;
        final var player = event.getPlayer();
        final var block = event.getClickedBlock();
        if (block == null) return;
        final var location = block.getLocation();
        final Shop shop = this.rds.getShopRepository().findByLocation(location);
        if (shop == null) return;
        if (event.getAction().isRightClick()) {
            event.setCancelled(true);
            /*
            //TODO COMMENT OUT TESTING ONLY
            this.rds.getViewFrame().open(
                ShopCustomerView.class,
                player,
                Map.of(
                    "plugin",
                    this.rds,
                    "shopLocation",
                    shop.getShopLocation()
                )
            );
             */
            return;
        }
        final Class<? extends View> viewClass = shop.canAccessOverview(player.getUniqueId())
                ? ShopOverviewView.class
                : ShopCustomerView.class;
        this.rds.getViewFrame().open(
                viewClass,
                player,
                Map.of(
                        "plugin",
                        this.rds,
                        "shopLocation",
                        shop.getShopLocation()
                )
        );
    }
}
