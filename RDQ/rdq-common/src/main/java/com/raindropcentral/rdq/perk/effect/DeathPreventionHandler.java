package com.raindropcentral.rdq.perk.effect;

import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkRuntime;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public final class DeathPreventionHandler {

    private final PerkRegistry perkRegistry;

    public DeathPreventionHandler(@NotNull PerkRegistry perkRegistry) {
        this.perkRegistry = perkRegistry;
    }

    @NotNull
    public Optional<DeathPreventionResult> tryPreventDeath(@NotNull Player player) {
        var playerId = player.getUniqueId();

        var activeRuntime = perkRegistry.getActiveForPlayer(playerId).stream()
            .filter(runtime -> runtime.perk().effect() instanceof PerkEffect.DeathPrevention)
            .findFirst();

        if (activeRuntime.isEmpty()) {
            return Optional.empty();
        }

        var runtime = activeRuntime.get();
        var effect = (PerkEffect.DeathPrevention) runtime.perk().effect();

        runtime.deactivate(player);

        player.setHealth(effect.healthOnSave());

        return Optional.of(new DeathPreventionResult(
            runtime.perk().id(),
            runtime.perk().displayNameKey(),
            effect.healthOnSave()
        ));
    }

    public boolean hasActiveDeathPrevention(@NotNull UUID playerId) {
        return perkRegistry.getActiveForPlayer(playerId).stream()
            .anyMatch(runtime -> runtime.perk().effect() instanceof PerkEffect.DeathPrevention);
    }

    @NotNull
    public Optional<PerkRuntime> getActiveDeathPreventionPerk(@NotNull UUID playerId) {
        return perkRegistry.getActiveForPlayer(playerId).stream()
            .filter(runtime -> runtime.perk().effect() instanceof PerkEffect.DeathPrevention)
            .findFirst();
    }

    public record DeathPreventionResult(
        @NotNull String perkId,
        @NotNull String perkNameKey,
        int healthRestored
    ) {}
}
