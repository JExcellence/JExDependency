package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PlayerRespawnListener implements Listener {

    private final RDT plugin;

    public PlayerRespawnListener(RDT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(@NonNull PlayerRespawnEvent event) {
        this.plugin.getBossBarFactory().run(
                event.getPlayer(),
                event.getPlayer().getChunk().getX(),
                event.getPlayer().getChunk().getZ()
        );
    }
}