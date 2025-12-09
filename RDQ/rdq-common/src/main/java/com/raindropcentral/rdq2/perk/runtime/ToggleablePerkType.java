package com.raindropcentral.rdq2.perk.runtime;

import com.raindropcentral.rdq2.perk.config.PerkConfig;
import com.raindropcentral.rdq2.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class ToggleablePerkType implements PerkType {

    @Override
    public @NotNull String getTypeId() {
        return EPerkType.TOGGLEABLE_PASSIVE.name();
    }

    @Override
    public boolean canHandle(@NotNull PerkConfig config) {
        return config.perkType() == EPerkType.TOGGLEABLE_PASSIVE;
    }

    @Override
    public @NotNull LoadedPerk createLoadedPerk(@NotNull PerkConfig config) {
        return new LoadedPerk(config, this);
    }

    @Override
    public boolean activate(@NotNull Player player, @NotNull LoadedPerk perk) {
        try {
            String effectType = (String) perk.config().metadata().getOrDefault("effectType", "SPEED");
            int amplifier = getPermissionAmplifier(player, perk);
            int duration = getDurationTicks(perk);

            PotionEffectType potionType = PotionEffectType.getByName(effectType);
            if (potionType == null) {
                return false;
            }

            PotionEffect effect = new PotionEffect(potionType, duration, amplifier, false, false);
            player.addPotionEffect(effect);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deactivate(@NotNull Player player, @NotNull LoadedPerk perk) {
        try {
            String effectType = (String) perk.config().metadata().getOrDefault("effectType", "SPEED");
            PotionEffectType potionType = PotionEffectType.getByName(effectType);
            if (potionType == null) {
                return false;
            }
            player.removePotionEffect(potionType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void trigger(@NotNull Player player, @NotNull LoadedPerk perk) {
    }

    private int getPermissionAmplifier(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (java.util.Map.Entry<String, Integer> entry : perk.config().permissionAmplifiers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private int getDurationTicks(@NotNull LoadedPerk perk) {
        Long durationSeconds = perk.config().durationSeconds();
        if (durationSeconds == null || durationSeconds <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) (durationSeconds * 20);
    }
}
