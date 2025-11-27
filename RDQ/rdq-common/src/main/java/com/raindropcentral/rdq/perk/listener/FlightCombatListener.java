package com.raindropcentral.rdq.perk.listener;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.perk.effect.FlightHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class FlightCombatListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(FlightCombatListener.class.getName());

    private final FlightHandler flightHandler;
    private final PerkEventBus eventBus;

    /**
     * Constructor for CommandFactory auto-registration.
     */
    public FlightCombatListener(@NotNull RDQCore core) {
        this.flightHandler = new FlightHandler(core.getPerkRegistry());
        this.eventBus = core.getPerkEventBus();
    }

    public FlightCombatListener(@NotNull FlightHandler flightHandler, @NotNull PerkEventBus eventBus) {
        this.flightHandler = flightHandler;
        this.eventBus = eventBus;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        var attacker = getAttacker(event);
        if (attacker == null) return;

        handleCombatForPlayer(victim);
        handleCombatForPlayer(attacker);
    }

    private void handleCombatForPlayer(@NotNull Player player) {
        if (!flightHandler.hasActiveFlight(player.getUniqueId())) return;

        if (flightHandler.isFlightAllowedInCombat(player.getUniqueId())) return;

        var flightPerk = flightHandler.getActiveFlightPerk(player.getUniqueId());
        if (flightPerk.isEmpty()) return;

        flightHandler.handleCombatEnter(player);
        eventBus.fireDeactivation(player, flightPerk.get().perk(), PerkEventBus.DeactivationReason.COMBAT);

        LOGGER.fine(() -> "Disabled flight for " + player.getName() + " due to combat");
    }

    @org.jetbrains.annotations.Nullable
    private Player getAttacker(@NotNull EntityDamageByEntityEvent event) {
        var damager = event.getDamager();

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }

        return null;
    }
}
