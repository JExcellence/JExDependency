package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class JumpBoostPerkService implements PerkEventListener {

    private final PerkManager perkManager;
    private final PerkEventBus perkEventBus;

    public JumpBoostPerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        this.perkEventBus = perkEventBus;
        this.perkEventBus.register(this);
    }

    @Override
    public void onPerkActivated(@NotNull Player player, @NotNull String perkId) {
        if ("jump_boost".equals(perkId)) {
            applyJumpBoostEffect(player);
        }
    }

    @Override
    public void onPerkDeactivated(@NotNull Player player, @NotNull String perkId) {
        if ("jump_boost".equals(perkId)) {
            removeJumpBoostEffect(player);
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

    private void applyJumpBoostEffect(@NotNull Player player) {
        LoadedPerk perk = perkManager.getPerk("jump_boost");
        if (perk == null) {
            return;
        }

        int amplifier = getPermissionAmplifier(player, perk);
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.JUMP_BOOST,
            Integer.MAX_VALUE,
            amplifier,
            false,
            false
        ));
    }

    private void removeJumpBoostEffect(@NotNull Player player) {
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
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
