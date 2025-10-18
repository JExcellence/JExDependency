package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public class BountyCombatListener implements Listener {

    private final RDQ rdq;

    public BountyCombatListener(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler
    public void onEntityDamageByEntity(final @NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player)) return;

        this.rdq.getBountyManager().trackDamage(
                target.getUniqueId(),
                event.getDamager().getUniqueId(),
                event.getFinalDamage()
        );
    }

    @EventHandler
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        this.rdq.getBountyManager().handleBountyKill(event.getEntity());
    }
}