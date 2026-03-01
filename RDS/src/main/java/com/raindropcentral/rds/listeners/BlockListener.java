package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopBlock;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@SuppressWarnings("unused")
public class BlockListener implements Listener {

    private final RDS rds;
    public BlockListener(RDS rds) {
        this.rds = rds;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        if (event ==  null) return;
        ItemStack item = event.getItemInHand();
        if (ShopBlock.equals(this.rds, item)) {
            UUID uuid = ShopBlock.getOwner(this.rds, item);
            if (uuid == null) {
                new I18n.Builder("block_listener.error.invalid_owner_uuid", event.getPlayer())
                        .includePrefix()
                        .build()
                        .sendMessage();
                return;
            }
            var rPlayer = this.rds.getPlayerRepository().findByPlayer(event.getPlayer().getUniqueId());
            placeShopItem(
                    event.getPlayer(),
                    rPlayer,
                    event.getBlock().getLocation()
            );
        }
    }

    private void placeShopItem(Player player, RDSPlayer rPlayer, Location shop_location) {
        if (player == null) return;
        if (rPlayer == null) return;
        var shop = new Shop(player.getUniqueId(), shop_location);
        this.rds.getShopRepository().create(shop);
        rPlayer.addShop(1);
        this.rds.getPlayerRepository().update(rPlayer);
    }
}
