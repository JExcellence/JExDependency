package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rdt.utils.Type;
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

    private final RDT plugin;
    public BlockListener(RDT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        //TODO Fix this to actually check for nexus block
        //String blockName = event.getBlock().getType().name().toLowerCase().replace('_', ' ');
        //event.getPlayer().sendMessage("you broke a " + blockName + " block");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        ItemStack item = event.getItemInHand();
        if (Nexus.equals(this.plugin, item)) {
            UUID uuid = Nexus.getTownUUID(this.plugin,item);
            if (uuid == null) {
                event.getPlayer().sendMessage("unable to convert to uuid");
                return;
            }
            RDTPlayer rdtPlayer = this.plugin.getPlayerRepository().findByPlayer(event.getPlayer().getUniqueId());
            placeNexus(
                    event.getPlayer(),
                    rdtPlayer,
                    uuid,
                    Nexus.getTownName(this.plugin,item),
                    event.getBlock().getLocation()
            );
        }
    }

    private void placeNexus(Player player, RDTPlayer rPlayer, UUID town_uuid, String town_name, Location nexus_location) {
        RTown rTown = new RTown(
                town_uuid,
                player.getUniqueId(),
                town_name,
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                this.plugin.getDefaultConfig().getFoundingCost(),
                nexus_location
        );
        rTown.addChunk(new RChunk(
                rTown,
                player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ(),
                Type.NEXUS
        ));
        this.plugin.getIrsFactory().run(rTown);
        rPlayer.setTownUUID(town_uuid);
        this.plugin.getPlayerRepository().update(rPlayer);
        // Save new RTown entity to a database
        this.plugin.getTownRepository().create(rTown);
        player.sendMessage("Town: " + town_name + " created!");
        // Update boss bar
        this.plugin.getBossBarFactory().run(
                player,
                player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ()
        );
    }
}