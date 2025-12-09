/*
package com.raindropcentral.rdq2.manager.perk;

import com.raindropcentral.rdq2.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq2.perk.runtime.PerkStateService;
import com.raindropcentral.rdq2.perk.runtime.PerkTriggerService;
import org.jetbrains.annotations.NotNull;

import com.raindropcentral.rdq2.perk.runtime.CooldownService;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

*/
/**
 * Interface for perk management operations.
 *//*

public interface PerkManager {
    
    @NotNull PerkRegistry getPerkRegistry();
    @NotNull PerkStateService getPerkStateService();
    @NotNull PerkTriggerService getPerkTriggerService();
    @NotNull CooldownService getCooldownService();
    
    Optional<com.raindropcentral.rdq2.perk.runtime.PerkRuntime> findRuntime(@NotNull String perkId);
    boolean activate(@NotNull Player player, @NotNull String perkId);
    boolean deactivate(@NotNull Player player, @NotNull String perkId);
    boolean isActive(@NotNull Player player, @NotNull String perkId);
    
    void clearPlayerState(@NotNull UUID playerId);
    void clearPlayerState(@NotNull Player player);
    
    void initialize();
    void shutdown();
}*/
