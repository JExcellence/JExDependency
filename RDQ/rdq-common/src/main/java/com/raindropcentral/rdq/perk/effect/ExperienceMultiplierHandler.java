package com.raindropcentral.rdq.perk.effect;

import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class ExperienceMultiplierHandler {

    private final PerkRegistry perkRegistry;

    public ExperienceMultiplierHandler(@NotNull PerkRegistry perkRegistry) {
        this.perkRegistry = perkRegistry;
    }

    public double getMultiplier(@NotNull UUID playerId) {
        return perkRegistry.getActiveForPlayer(playerId).stream()
            .map(runtime -> runtime.perk().effect())
            .filter(effect -> effect instanceof PerkEffect.ExperienceMultiplier)
            .map(effect -> (PerkEffect.ExperienceMultiplier) effect)
            .mapToDouble(PerkEffect.ExperienceMultiplier::multiplier)
            .max()
            .orElse(1.0);
    }

    public int applyMultiplier(@NotNull Player player, int originalXp) {
        var multiplier = getMultiplier(player.getUniqueId());
        return (int) Math.round(originalXp * multiplier);
    }

    public boolean hasActiveMultiplier(@NotNull UUID playerId) {
        return perkRegistry.getActiveForPlayer(playerId).stream()
            .anyMatch(runtime -> runtime.perk().effect() instanceof PerkEffect.ExperienceMultiplier);
    }
}
