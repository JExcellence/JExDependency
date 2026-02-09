package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@SuppressWarnings("unused")
public class PlayerJoinListener implements Listener {

    private final RDS rds;

    public PlayerJoinListener(RDS rds) {
        this.rds = rds;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event == null) return;
        final Player player = event.getPlayer();

        var rPlayer = this.rds.getPlayerRepository().findByPlayer(player.getUniqueId());

        if (rPlayer == null) {
            var newPlayer = new RDSPlayer(player.getUniqueId());
            this.rds.getPlayerRepository().createAsync(newPlayer);
        }
    }
}
