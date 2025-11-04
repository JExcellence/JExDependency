package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.perk.runtime.CooldownService;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkRuntime;
import com.raindropcentral.rdq.perk.runtime.PerkStateService;
import com.raindropcentral.rdq.perk.runtime.PerkTriggerService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates the lifecycle of player perks across the RDQ platform, providing a
 * single integration point for loading perk definitions, wiring runtime
 * listeners, and resolving dependencies between perks and other gameplay
 * systems.
 *
 * This is intentionally an interface so there is a single concrete implementation
 * (DefaultPerkManager) and no redundant class hierarchy.
 */
public interface PerkManager {

    PerkRegistry getPerkRegistry();

    PerkStateService getPerkStateService();

    PerkTriggerService getPerkTriggerService();

    void initialize();

    void shutdown();

    default CooldownService getCooldownService() {
        throw new UnsupportedOperationException("Cooldown service is not available in this implementation");
    }

    default @NotNull Optional<PerkRuntime> findRuntime(@NotNull String perkId) {
        return Optional.empty();
    }

    default boolean activate(@NotNull Player player, @NotNull String perkId) {
        return findRuntime(perkId)
                .filter(runtime -> runtime.canActivate(player))
                .map(runtime -> runtime.activate(player))
                .orElse(false);
    }

    default boolean deactivate(@NotNull Player player, @NotNull String perkId) {
        return findRuntime(perkId)
                .map(runtime -> runtime.deactivate(player))
                .orElse(false);
    }

    default boolean isActive(@NotNull Player player, @NotNull String perkId) {
        return findRuntime(perkId)
                .map(runtime -> runtime.isActive(player))
                .orElse(false);
    }

    default void clearPlayerState(@NotNull UUID playerId) {
        // Optional
    }

    default void clearPlayerState(@NotNull Player player) {
        clearPlayerState(player.getUniqueId());
    }
}
