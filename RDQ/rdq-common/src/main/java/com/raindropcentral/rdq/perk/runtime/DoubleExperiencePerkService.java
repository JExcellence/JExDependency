package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Perk service that doubles experience gained by the player.
 *
 * <p>When activated, all experience gained by the player is multiplied by 2.
 * This applies to all sources of experience including mob kills, mining, and smelting.
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public class DoubleExperiencePerkService implements PerkEventListener, Listener {

    private final PerkManager perkManager;
    private static final String PERK_ID = "double_experience";

    public DoubleExperiencePerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        perkEventBus.register(this);
    }

    @EventHandler
    public void onPlayerExpChange(@NotNull PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        final var runtime = perkManager.findRuntime(PERK_ID);
        if (runtime.isEmpty() || !runtime.get().isActive(player)) {
            return;
        }

        LoadedPerk perk = perkManager.getPerkRegistry().get(PERK_ID);
        if (perk == null) {
            return;
        }

        int multiplier = getExperienceMultiplier(player, perk);
        event.setAmount(event.getAmount() * multiplier);
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

    private int getExperienceMultiplier(@NotNull Player player, @NotNull LoadedPerk perk) {
        int baseMultiplier = 2;
        int amplifier = getPermissionAmplifier(player, perk);
        return baseMultiplier + amplifier;
    }

    private int getPermissionAmplifier(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (Map.Entry<String, Integer> entry : perk.config().permissionAmplifiers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }
}
