package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PermissionScalingService {

    public int getAmplifier(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (java.util.Map.Entry<String, Integer> entry : perk.config().permissionAmplifiers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }

    public long getCooldown(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (java.util.Map.Entry<String, Long> entry : perk.config().permissionCooldowns().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return perk.config().cooldownSeconds() != null ? perk.config().cooldownSeconds() : 0;
    }

    public long getDuration(@NotNull Player player, @NotNull LoadedPerk perk) {
        Long baseDuration = perk.config().durationSeconds();
        if (baseDuration == null || baseDuration <= 0) {
            return Long.MAX_VALUE;
        }
        return baseDuration;
    }

    public boolean hasPermissionScaling(@NotNull LoadedPerk perk) {
        return !perk.config().permissionAmplifiers().isEmpty() || 
               !perk.config().permissionCooldowns().isEmpty();
    }
}
