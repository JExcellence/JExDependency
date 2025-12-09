package com.raindropcentral.rdq2.perk.runtime;

import com.raindropcentral.rdq2.perk.config.PerkConfig;
import com.raindropcentral.rdq2.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class EventPerkType implements PerkType {

    @Override
    public @NotNull String getTypeId() {
        return EPerkType.EVENT_TRIGGERED.name();
    }

    @Override
    public boolean canHandle(@NotNull PerkConfig config) {
        return config.perkType() == EPerkType.EVENT_TRIGGERED;
    }

    @Override
    public @NotNull LoadedPerk createLoadedPerk(@NotNull PerkConfig config) {
        return new LoadedPerk(config, this);
    }

    @Override
    public boolean activate(@NotNull Player player, @NotNull LoadedPerk perk) {
        return true;
    }

    @Override
    public boolean deactivate(@NotNull Player player, @NotNull LoadedPerk perk) {
        return true;
    }

    @Override
    public void trigger(@NotNull Player player, @NotNull LoadedPerk perk) {
        try {
            String triggerEvent = (String) perk.config().metadata().getOrDefault("triggerEvent", "UNKNOWN");
            int amplifier = getPermissionAmplifier(player, perk);
            
            applyTriggerEffect(player, perk, amplifier);
        } catch (Exception e) {
        }
    }

    private void applyTriggerEffect(@NotNull Player player, @NotNull LoadedPerk perk, int amplifier) {
        String effectType = (String) perk.config().metadata().getOrDefault("effectType", "");
        if (effectType.isEmpty()) {
            return;
        }

        PotionEffectType potionType = PotionEffectType.getByName(effectType);
        if (potionType == null) {
            return;
        }

        Long durationSeconds = perk.config().durationSeconds();
        int duration = durationSeconds != null ? (int) (durationSeconds * 20) : 100;

        PotionEffect effect = new PotionEffect(potionType, duration, amplifier, false, false);
        player.addPotionEffect(effect);
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
