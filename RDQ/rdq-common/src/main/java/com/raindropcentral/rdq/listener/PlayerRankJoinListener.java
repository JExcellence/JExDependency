package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerRankJoinListener implements Listener {

    private final RDQ rdq;

    public PlayerRankJoinListener(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final UUID playerUUID = event.getPlayer().getUniqueId();
    }
}