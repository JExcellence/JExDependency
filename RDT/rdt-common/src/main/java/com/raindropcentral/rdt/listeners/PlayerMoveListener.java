package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PlayerMoveListener implements Listener {

    private final RDT plugin;

    public PlayerMoveListener(RDT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(@NonNull PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        int fromChunkX = event.getFrom().getChunk().getX();
        int fromChunkZ = event.getFrom().getChunk().getZ();
        int toChunkX = event.getTo().getChunk().getX();
        int toChunkZ = event.getTo().getChunk().getZ();
        if (fromChunkX == toChunkX && fromChunkZ == toChunkZ) return;
        this.plugin.getBossBarFactory().run(player, toChunkX, toChunkZ);
    }
}