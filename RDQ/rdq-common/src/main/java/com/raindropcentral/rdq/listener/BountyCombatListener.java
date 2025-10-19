package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listener that proxies combat events to the bounty manager so that damage and
 * kills can be attributed to the correct players.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class BountyCombatListener implements Listener {

    private final RDQ rdq;

    /**
     * Creates a new listener that delegates bounty combat events to the supplied RDQ instance.
     *
     * @param rdq the plugin entry point providing access to the bounty manager
     */
    public BountyCombatListener(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Records damage dealt between players so the bounty manager can track the latest combat interactions.
     *
     * @param event the combat event describing the damage exchange between the attacking and target player
     */
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

    /**
     * Handles player deaths by forwarding the killed player to the bounty manager for reward evaluation.
     *
     * @param event the player death event containing the player whose bounty status should be updated
     */
    @EventHandler
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        this.rdq.getBountyManager().handleBountyKill(event.getEntity());
    }
}
