package com.raindropcentral.rdq.bounty.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.raindropcentral.rdq.RDQCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class DamageTracker implements Listener {

    private static final Duration COMBAT_TIMEOUT = Duration.ofSeconds(15);

    private final Cache<UUID, DamageRecord> lastDamage;

    /**
     * Constructor for CommandFactory auto-registration.
     */
    @SuppressWarnings("unused")
    public DamageTracker(@NotNull RDQCore core) {
        this();
    }

    public DamageTracker() {
        this.lastDamage = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(COMBAT_TIMEOUT)
            .build();
    }

    public record DamageRecord(
        @NotNull UUID attackerId,
        @NotNull String attackerName,
        @NotNull Instant timestamp
    ) {
        public boolean isValid() {
            return Duration.between(timestamp, Instant.now()).compareTo(COMBAT_TIMEOUT) < 0;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        var attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        lastDamage.put(victim.getUniqueId(), new DamageRecord(
            attacker.getUniqueId(),
            attacker.getName(),
            Instant.now()
        ));
    }


    @Nullable
    private Player getAttacker(EntityDamageByEntityEvent event) {
        var damager = event.getDamager();

        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }

        if (damager instanceof org.bukkit.entity.Tameable tameable) {
            if (tameable.getOwner() instanceof Player owner) {
                return owner;
            }
        }

        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastDamage.invalidate(event.getPlayer().getUniqueId());
    }

    @NotNull
    public Optional<DamageRecord> getLastAttacker(@NotNull UUID victimId) {
        var record = lastDamage.getIfPresent(victimId);
        if (record != null && record.isValid()) {
            return Optional.of(record);
        }
        return Optional.empty();
    }

    public void clearRecord(@NotNull UUID victimId) {
        lastDamage.invalidate(victimId);
    }

    public void clearAll() {
        lastDamage.invalidateAll();
    }
}
