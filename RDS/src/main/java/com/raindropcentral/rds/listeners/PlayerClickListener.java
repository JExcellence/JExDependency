package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

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
        if (player.getUniqueId().equals(shop.getOwner())) {
            //Shop Owner


        } else {

        }
    }
}
