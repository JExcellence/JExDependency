package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for tracking damage dealt to players for bounty attribution.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyDamageListener implements Listener {

    private final RDQ rdq;

    public BountyDamageListener(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        double damage = event.getFinalDamage();
        rdq.getBountyFactory().getDamageTracker().recordDamage(victim.getUniqueId(), attacker.getUniqueId(), damage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        rdq.getBountyFactory().getDamageTracker().clearDamage(event.getPlayer().getUniqueId());
    }

    /**
     * Extracts the attacking player from various damage sources.
     * Handles direct attacks, projectiles, and tamed entities.
     */
    private Player getAttacker(@NotNull EntityDamageByEntityEvent event) {
        var damager = event.getDamager();

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile) {
            var shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        if (damager instanceof Wolf wolf && wolf.isTamed()) {
            var owner = wolf.getOwner();
            if (owner instanceof Player player) {
                return player;
            }
        }

        return null;
    }
}
