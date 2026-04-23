package de.jexcellence.quests.perk;

import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.event.PerkActivatedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Wires Bukkit events into {@link PerkRuntimeService} — every effect
 * the shipped perk pack advertises in its {@code behaviour:} section
 * is resolved here at runtime rather than in the service layer, which
 * keeps the purely async DB code free of Bukkit event dependencies.
 */
public final class PerkRuntimeListener implements Listener {

    private final PerkRuntimeService runtime;

    public PerkRuntimeListener(@NotNull JExQuests quests) {
        this.runtime = quests.perkRuntime();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        this.runtime.refreshAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        // Strip lingering flight from our TOGGLE grant so a player who
        // logs out with flight active doesn't come back flying if the
        // perk has since been disabled / revoked.
        final Player player = event.getPlayer();
        if (player.getGameMode().name().equals("SURVIVAL") || player.getGameMode().name().equals("ADVENTURE")) {
            player.setAllowFlight(false);
        }
        this.runtime.forget(player.getUniqueId());
    }

    /**
     * Nullifies damage when an owned PASSIVE perk declares
     * {@code cancelDamageCause: <CAUSE>} matching the incoming event.
     * Covers {@code FALL} today; forward-compatible with any other
     * {@code DamageCause} name a future perk wants to suppress.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (this.runtime.cancelsDamage(player.getUniqueId(), event.getCause().name())) {
            event.setCancelled(true);
        }
    }

    /** Soulbind — suppresses death-drop when KEEP_INVENTORY is active. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(@NotNull PlayerDeathEvent event) {
        if (this.runtime.keepsInventory(event.getEntity().getUniqueId())) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    /** Scholar's Focus — multiplies XP gain while the boost window is open. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onXpGain(@NotNull PlayerExpChangeEvent event) {
        final double multiplier = this.runtime.xpMultiplier(event.getPlayer().getUniqueId());
        if (multiplier <= 1.0) return;
        event.setAmount((int) Math.max(0, Math.round(event.getAmount() * multiplier)));
    }

    /** Drives the ACTIVE-kind one-shot path. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPerkActivated(@NotNull PerkActivatedEvent event) {
        this.runtime.onActivate(event.snapshot().playerUuid(), event.snapshot().perkIdentifier());
    }
}
