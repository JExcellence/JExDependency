package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Perk service that enables flight for the player.
 *
 * <p>When activated, the player gains the ability to fly. When deactivated,
 * flight is disabled and the player returns to normal movement.
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public class FlyPerkService implements PerkEventListener {

    private final PerkManager perkManager;
    private static final String PERK_ID = "fly";

    public FlyPerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        perkEventBus.register(this);
    }

    @Override
    public void onPerkActivated(@NotNull Player player, @NotNull String perkId) {
        if (PERK_ID.equals(perkId)) {
            enableFlight(player);
        }
    }

    @Override
    public void onPerkDeactivated(@NotNull Player player, @NotNull String perkId) {
        if (PERK_ID.equals(perkId)) {
            disableFlight(player);
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

    private void enableFlight(@NotNull Player player) {
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(getFlightSpeed(player));
    }

    private void disableFlight(@NotNull Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setFlySpeed(0.1f);
    }

    private float getFlightSpeed(@NotNull Player player) {
        LoadedPerk perk = perkManager.getPerkRegistry().get(PERK_ID);
        if (perk == null) {
            return 0.1f;
        }

        int amplifier = getPermissionAmplifier(player, perk);
        return 0.1f + (amplifier * 0.05f);
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
