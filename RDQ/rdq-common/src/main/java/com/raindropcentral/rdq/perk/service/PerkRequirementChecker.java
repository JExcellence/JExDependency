package com.raindropcentral.rdq.perk.service;

import com.raindropcentral.rdq.perk.PerkRequirement;
import com.raindropcentral.rdq.rank.repository.PlayerRankRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class PerkRequirementChecker {

    private static final Logger LOGGER = Logger.getLogger(PerkRequirementChecker.class.getName());

    private final PlayerRankRepository rankRepository;

    public PerkRequirementChecker(@NotNull PlayerRankRepository rankRepository) {
        this.rankRepository = rankRepository;
    }

    @NotNull
    public CompletableFuture<Boolean> checkAll(@NotNull UUID playerId, @NotNull List<PerkRequirement> requirements) {
        if (requirements.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        var futures = requirements.stream()
            .map(req -> check(playerId, req))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream().allMatch(f -> f.join()));
    }

    @NotNull
    public CompletableFuture<Boolean> check(@NotNull UUID playerId, @NotNull PerkRequirement requirement) {
        return switch (requirement) {
            case PerkRequirement.RankRequired(var rankId) -> checkRankRequirement(playerId, rankId);
            case PerkRequirement.PermissionRequired(var permission) -> checkPermissionRequirement(playerId, permission);
            case PerkRequirement.CurrencyRequired(var currency, var amount) -> checkCurrencyRequirement(playerId, currency, amount);
            case PerkRequirement.LevelRequired(var level) -> checkLevelRequirement(playerId, level);
        };
    }

    private CompletableFuture<Boolean> checkRankRequirement(@NotNull UUID playerId, @NotNull String rankId) {
        return rankRepository.hasUnlockedRankAsync(playerId, rankId);
    }

    private CompletableFuture<Boolean> checkPermissionRequirement(@NotNull UUID playerId, @NotNull String permission) {
        return CompletableFuture.supplyAsync(() -> {
            var player = org.bukkit.Bukkit.getPlayer(playerId);
            return player != null && player.hasPermission(permission);
        });
    }

    private CompletableFuture<Boolean> checkCurrencyRequirement(
        @NotNull UUID playerId,
        @NotNull String currency,
        @NotNull java.math.BigDecimal amount
    ) {
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> checkLevelRequirement(@NotNull UUID playerId, int level) {
        return CompletableFuture.supplyAsync(() -> {
            var player = org.bukkit.Bukkit.getPlayer(playerId);
            return player != null && player.getLevel() >= level;
        });
    }

    @NotNull
    public List<PerkRequirement> getUnmetRequirements(
        @NotNull UUID playerId,
        @NotNull List<PerkRequirement> requirements
    ) {
        return requirements.stream()
            .filter(req -> !check(playerId, req).join())
            .toList();
    }
}
