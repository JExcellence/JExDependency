package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class StrengthPerkService implements PerkEventListener {

    private final PerkManager perkManager;
    private final PerkEventBus perkEventBus;

    public StrengthPerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        this.perkEventBus = perkEventBus;
        this.perkEventBus.register(this);
    }

    @Override
    public void onPerkActivated(@NotNull Player player, @NotNull String perkId) {
        if ("strength".equals(perkId)) {
            applyStrengthEffect(player);
        }
    }

    @Override
    public void onPerkDeactivated(@NotNull Player player, @NotNull String perkId) {
        if ("strength".equals(perkId)) {
            removeStrengthEffect(player);
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

    private void applyStrengthEffect(@NotNull Player player) {
        LoadedPerk perk = perkManager.getPerk("strength");
        if (perk == null) {
            return;
        }

        int amplifier = getPermissionAmplifier(player, perk);
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH,
            Integer.MAX_VALUE,
            amplifier,
            false,
            false
        ));
    }

    private void removeStrengthEffect(@NotNull Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
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
