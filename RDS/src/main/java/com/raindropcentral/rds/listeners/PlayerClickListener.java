package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.view.shop.ShopOverviewView;
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
        var shop = this.rds.getShopRepository().findByLocation(location);
        if (shop == null) return;
        this.rds.getViewFrame().open(
                ShopOverviewView.class,
                player,
                Map.of(
                        "plugin",
                        this.rds,
                        "location",
                        player.getLocation(),
                        "owner",
                        player.getName()
                )
        );
    }
}
