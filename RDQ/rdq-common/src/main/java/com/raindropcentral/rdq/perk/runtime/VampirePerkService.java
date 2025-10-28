package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Perk service that heals the player when dealing damage to entities.
 *
 * <p>When activated, a percentage of damage dealt to mobs is converted to healing
 * for the player. This encourages aggressive combat playstyle.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 3.2.0
 */
public class VampirePerkService implements PerkEventListener, Listener {

    private final PerkManager perkManager;
    private final PerkEventBus perkEventBus;
    private static final String PERK_ID = "vampire";

    public VampirePerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        this.perkEventBus = perkEventBus;
        this.perkEventBus.register(this);
    }

    @EventHandler
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        if (!perkManager.isActive(player, PERK_ID)) {
            return;
        }

        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity)) {
            return;
        }

        LoadedPerk perk = perkManager.getPerk(PERK_ID);
        if (perk == null) {
            return;
        }

        double damage = event.getDamage();
        double healPercentage = getHealPercentage(player, perk);
        double healAmount = damage * healPercentage;

        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
        player.setHealth(newHealth);
    }

    @Override
    public void onPerkActivated(@NotNull Player player, @NotNull String perkId) {
    }

    @Override
    public void onPerkDeactivated(@NotNull Player player, @NotNull String perkId) {
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

    private double getHealPercentage(@NotNull Player player, @NotNull LoadedPerk perk) {
        double basePercentage = 0.15;
        int amplifier = getPermissionAmplifier(player, perk);
        return basePercentage + (amplifier * 0.05);
    }

    private int getPermissionAmplifier(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (java.util.Map.Entry<String, Integer> entry : perk.config().permissionAmplifiers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }
}
