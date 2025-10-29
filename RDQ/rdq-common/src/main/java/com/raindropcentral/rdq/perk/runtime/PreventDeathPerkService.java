package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Perk service that prevents player death once per cooldown period.
 *
 * <p>When a player with this perk would die, they are instead restored to full health
 * and the perk enters cooldown. This is a one-use-per-cooldown mechanic.
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public class PreventDeathPerkService implements PerkEventListener, Listener {

    private final PerkManager perkManager;
    private final Map<UUID, Long> lastPreventionTime = new HashMap<>();
    private static final String PERK_ID = "prevent_death";

    public PreventDeathPerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        perkEventBus.register(this);
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        final var runtime = perkManager.findRuntime(PERK_ID);
        if (runtime.isEmpty() || !runtime.get().isActive(player)) {
            return;
        }

        LoadedPerk perk = perkManager.getPerkRegistry().get(PERK_ID);
        if (perk == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastPrevention = lastPreventionTime.getOrDefault(playerId, 0L);
        long cooldownMs = getCooldownMillis(player, perk);

        if (currentTime - lastPrevention < cooldownMs) {
            return;
        }

        event.setCancelled(true);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(10.0f);

        lastPreventionTime.put(playerId, currentTime);
        runtime.get().setCooldown(player, getCooldownSeconds(player, perk));
    }

    @Override
    public void onPerkActivated(@NotNull Player player, @NotNull String perkId) {
        if (PERK_ID.equals(perkId)) {
            lastPreventionTime.remove(player.getUniqueId());
        }
    }

    @Override
    public void onPerkDeactivated(@NotNull Player player, @NotNull String perkId) {
        if (PERK_ID.equals(perkId)) {
            lastPreventionTime.remove(player.getUniqueId());
        }
    }

    @Override
    public void onPerkTriggered(@NotNull Player player, @NotNull String perkId) {
    }

    @Override
    public void onPerkCooldownStart(@NotNull Player player, @NotNull String perkId, long durationSeconds) {
    }

    @Override
    public void onPerkCooldownEnd(@NotNull Player player, @NotNull String perkId) {
    }

    private long getCooldownMillis(@NotNull Player player, @NotNull LoadedPerk perk) {
        return getCooldownSeconds(player, perk) * 1000L;
    }

    private long getCooldownSeconds(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (Map.Entry<String, Long> entry : perk.config().permissionCooldowns().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return perk.config().permissionCooldowns().getOrDefault("default", 300L);
    }
}
