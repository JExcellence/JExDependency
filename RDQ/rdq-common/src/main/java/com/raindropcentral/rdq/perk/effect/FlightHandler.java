package com.raindropcentral.rdq.perk.effect;

import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkRuntime;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public final class FlightHandler {

    private final PerkRegistry perkRegistry;

    public FlightHandler(@NotNull PerkRegistry perkRegistry) {
        this.perkRegistry = perkRegistry;
    }

    public boolean hasActiveFlight(@NotNull UUID playerId) {
        return perkRegistry.getActiveForPlayer(playerId).stream()
            .anyMatch(runtime -> runtime.perk().effect() instanceof PerkEffect.Flight);
    }

    @NotNull
    public Optional<PerkRuntime> getActiveFlightPerk(@NotNull UUID playerId) {
        return perkRegistry.getActiveForPlayer(playerId).stream()
            .filter(runtime -> runtime.perk().effect() instanceof PerkEffect.Flight)
            .findFirst();
    }

    public boolean isFlightAllowedInCombat(@NotNull UUID playerId) {
        return getActiveFlightPerk(playerId)
            .map(runtime -> runtime.perk().effect())
            .filter(effect -> effect instanceof PerkEffect.Flight)
            .map(effect -> ((PerkEffect.Flight) effect).allowInCombat())
            .orElse(false);
    }

    public void handleCombatEnter(@NotNull Player player) {
        var playerId = player.getUniqueId();
        var flightPerk = getActiveFlightPerk(playerId);

        if (flightPerk.isEmpty()) return;

        var runtime = flightPerk.get();
        var effect = (PerkEffect.Flight) runtime.perk().effect();

        if (!effect.allowInCombat()) {
            runtime.deactivate(player);
        }
    }

    public void restoreFlightIfAllowed(@NotNull Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        if (hasActiveFlight(player.getUniqueId())) {
            player.setAllowFlight(true);
        }
    }

    public void removeFlight(@NotNull Player player) {
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }
}
