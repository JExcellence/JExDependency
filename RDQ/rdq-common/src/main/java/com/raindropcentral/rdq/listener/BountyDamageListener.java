package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Listener for damage events related to the bounty system.
 * Tracks damage dealt to players with active bounties for claim attribution.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyDamageListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyDamageListener.class.getName());

    private final RDQ rdq;

    public BountyDamageListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)  // Changed to catch cancelled events too
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        // Log ALL damage events for debugging
        LOGGER.info("Damage event detected: " + event.getDamager().getType() + " -> " + event.getEntity().getType() + 
                   " (damage: " + event.getFinalDamage() + ", cancelled: " + event.isCancelled() + ")");
        
        // Only track player-on-player damage
        if (!(event.getEntity() instanceof Player victim)) {
            LOGGER.info("Not a player victim, skipping");
            return;
        }
        
        if (!(event.getDamager() instanceof Player attacker)) {
            LOGGER.info("Not a player attacker, skipping");
            return;
        }

        // Always record damage for all player-vs-player combat
        // The ClaimHandler will check for bounties later during death processing
        double damage = event.getFinalDamage();
        rdq.getBountyFactory().getDamageTracker().recordDamage(
            victim.getUniqueId(),
            attacker.getUniqueId(),
            damage
        );

        LOGGER.info("Recorded " + damage + " PvP damage from " + attacker.getName() + " to " + victim.getName());
    }
}