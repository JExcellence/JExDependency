package com.raindropcentral.rdq.listener;


import com.raindropcentral.rdq.RDQ;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class BountyJoinListener implements Listener {

    private final RDQ rdq;

    public BountyJoinListener(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        this.rdq.getBountyManager().updateBountyPlayerDisplay(event.getPlayer().getUniqueId());
    }
}