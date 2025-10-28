package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PerkManager {

    private final PerkRegistry perkRegistry;
    private final PerkCache perkCache;
    private final CooldownService cooldownService;

    public PerkManager(
        @NotNull PerkRegistry perkRegistry,
        @NotNull PerkCache perkCache,
        @NotNull CooldownService cooldownService
    ) {
        this.perkRegistry = perkRegistry;
        this.perkCache = perkCache;
        this.cooldownService = cooldownService;
    }

    public @NotNull CompletableFuture<ActivationResult> activateAsync(
        @NotNull Player player,
        @NotNull String perkId
    ) {
        return CompletableFuture.supplyAsync(() -> activate(player, perkId));
    }

    public @NotNull ActivationResult activate(@NotNull Player player, @NotNull String perkId) {
        LoadedPerk perk = perkRegistry.get(perkId);
        if (perk == null) {
            return ActivationResult.failure("Perk not found: " + perkId);
        }

        if (!perk.config().enabled()) {
            return ActivationResult.failure("Perk is disabled");
        }

        if (cooldownService.isOnCooldown(player, perkId)) {
            long remaining = cooldownService.getRemainingCooldown(player, perkId);
            return ActivationResult.failure("Perk on cooldown for " + remaining + " seconds");
        }

        boolean success = perk.type().activate(player, perk);
        if (success) {
            PlayerPerkState state = perkCache.getOrCreate(player);
            state.setActivationTime(perkId, System.currentTimeMillis());
        }

        return success ? ActivationResult.succeed() : ActivationResult.failure("Activation failed");
    }

    public @NotNull CompletableFuture<DeactivationResult> deactivateAsync(
        @NotNull Player player,
        @NotNull String perkId
    ) {
        return CompletableFuture.supplyAsync(() -> deactivate(player, perkId));
    }

    public @NotNull DeactivationResult deactivate(@NotNull Player player, @NotNull String perkId) {
        LoadedPerk perk = perkRegistry.get(perkId);
        if (perk == null) {
            return DeactivationResult.failed("Perk not found: " + perkId);
        }

        boolean success = perk.type().deactivate(player, perk);
        if (success) {
            PlayerPerkState state = perkCache.getOrCreate(player);
            state.setActivationTime(perkId, 0);
        }

        return success ? DeactivationResult.succeed() : DeactivationResult.failed("Deactivation failed");
    }

    public boolean isActive(@NotNull Player player, @NotNull String perkId) {
        PlayerPerkState state = perkCache.get(player);
        if (state == null) {
            return false;
        }
        return state.isActive(perkId);
    }

    public @NotNull List<LoadedPerk> getActivePerks(@NotNull Player player) {
        List<LoadedPerk> active = new ArrayList<>();
        PlayerPerkState state = perkCache.get(player);
        if (state != null) {
            for (LoadedPerk perk : perkRegistry.getAll()) {
                if (state.isActive(perk.getId())) {
                    active.add(perk);
                }
            }
        }
        return active;
    }

    public @Nullable LoadedPerk getPerk(@NotNull String perkId) {
        return perkRegistry.get(perkId);
    }

    public @NotNull List<LoadedPerk> getAllPerks() {
        return perkRegistry.getAll();
    }

    public void clearPlayerState(@NotNull Player player) {
        clearPlayerState(player.getUniqueId());
    }

    public void clearPlayerState(@NotNull UUID playerId) {
        perkCache.invalidate(playerId);
        cooldownService.clearAllCooldowns(playerId);
    }
}
