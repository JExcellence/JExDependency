/*
package com.raindropcentral.rdq2.perk.effect;

import com.raindropcentral.rdq2.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq2.perk.runtime.PerkRuntime;
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
        // TODO: Implement when PerkRuntime API is complete
        return false;
    }

    @NotNull
    public Optional<PerkRuntime> getActiveFlightPerk(@NotNull UUID playerId) {
        // TODO: Implement when PerkRuntime API is complete
        return Optional.empty();
    }

    public boolean isFlightAllowedInCombat(@NotNull UUID playerId) {
        // TODO: Implement when PerkRuntime API is complete
        return false;
    }

    public void handleCombatEnter(@NotNull Player player) {
        // TODO: Implement when PerkRuntime API is complete
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
*/
