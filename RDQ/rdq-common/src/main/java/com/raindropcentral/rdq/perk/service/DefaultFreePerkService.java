package com.raindropcentral.rdq.perk.service;

import com.raindropcentral.rdq.api.FreePerkService;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerkEntity;
import com.raindropcentral.rdq.perk.*;
import com.raindropcentral.rdq.perk.repository.*;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DefaultFreePerkService implements FreePerkService {

    private static final Logger LOGGER = Logger.getLogger(DefaultFreePerkService.class.getName());
    protected static final int MAX_ACTIVE_PERKS_FREE = 1;

    protected final PerkRepository perkRepository;
    protected final PlayerPerkRepository playerPerkRepository;
    protected final PerkRequirementChecker requirementChecker;

    public DefaultFreePerkService(
        @NotNull PerkRepository perkRepository,
        @NotNull PlayerPerkRepository playerPerkRepository,
        @NotNull PerkRequirementChecker requirementChecker
    ) {
        this.perkRepository = perkRepository;
        this.playerPerkRepository = playerPerkRepository;
        this.requirementChecker = requirementChecker;
    }

    @Override
    public CompletableFuture<List<Perk>> getAvailablePerks(@NotNull UUID playerId) {
        var enabledPerks = perkRepository.findEnabled();
        var futures = enabledPerks.stream()
            .map(perk -> requirementChecker.checkAll(playerId, perk.requirements())
                .thenApply(met -> met ? perk : null))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList());
    }

    @Override
    public CompletableFuture<List<Perk>> getAllPerks() {
        return CompletableFuture.completedFuture(perkRepository.findEnabled());
    }

    @Override
    public CompletableFuture<Optional<Perk>> getPerk(@NotNull String perkId) {
        return CompletableFuture.completedFuture(perkRepository.findById(perkId));
    }

    @Override
    public CompletableFuture<Boolean> unlockPerk(@NotNull UUID playerId, @NotNull String perkId) {
        var perkOpt = perkRepository.findById(perkId);
        if (perkOpt.isEmpty()) {
            LOGGER.warning("Attempted to unlock non-existent perk: " + perkId);
            return CompletableFuture.completedFuture(false);
        }

        var perk = perkOpt.get();
        if (!perk.enabled()) {
            return CompletableFuture.completedFuture(false);
        }

        return requirementChecker.checkAll(playerId, perk.requirements())
            .thenCompose(requirementsMet -> {
                if (!requirementsMet) {
                    return CompletableFuture.completedFuture(false);
                }

                return playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, perkId)
                    .thenCompose(existing -> {
                        if (existing.isPresent() && existing.get().unlocked()) {
                            return CompletableFuture.completedFuture(false);
                        }

                        if (existing.isPresent()) {
                            var entity = existing.get();
                            entity.setUnlocked(true);
                            return playerPerkRepository.updateAsync(entity).thenApply(saved -> true);
                        } else {
                            var entity = PlayerPerkEntity.create(playerId, perkId);
                            entity.setUnlocked(true);
                            return playerPerkRepository.createAsync(entity).thenApply(saved -> true);
                        }
                    });
            });
    }

    @Override
    public CompletableFuture<Boolean> activatePerk(@NotNull UUID playerId, @NotNull String perkId) {
        var perkOpt = perkRepository.findById(perkId);
        if (perkOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        var perk = perkOpt.get();

        return playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, perkId)
            .thenCompose(existing -> {
                if (existing.isEmpty() || !existing.get().unlocked()) {
                    return CompletableFuture.completedFuture(false);
                }

                var entity = existing.get();
                if (entity.isOnCooldown()) {
                    return CompletableFuture.completedFuture(false);
                }

                if (entity.active()) {
                    return CompletableFuture.completedFuture(false);
                }

                return playerPerkRepository.countActiveByPlayerIdAsync(playerId)
                    .thenCompose(activeCount -> {
                        if (activeCount >= getMaxActivePerks()) {
                            return playerPerkRepository.deactivateAllByPlayerIdAsync(playerId)
                                .thenCompose(v -> activateEntity(entity, perk));
                        }
                        return activateEntity(entity, perk);
                    });
            });
    }

    protected int getMaxActivePerks() {
        return MAX_ACTIVE_PERKS_FREE;
    }

    protected CompletableFuture<Boolean> activateEntity(@NotNull PlayerPerkEntity entity, @NotNull Perk perk) {
        entity.setActive(true);
        return playerPerkRepository.updateAsync(entity).thenApply(saved -> true);
    }

    @Override
    public CompletableFuture<Boolean> deactivatePerk(@NotNull UUID playerId, @NotNull String perkId) {
        var perkOpt = perkRepository.findById(perkId);
        if (perkOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        var perk = perkOpt.get();

        return playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, perkId)
            .thenCompose(existing -> {
                if (existing.isEmpty() || !existing.get().active()) {
                    return CompletableFuture.completedFuture(false);
                }

                var entity = existing.get();
                entity.setActive(false);

                if (perk.hasCooldown()) {
                    entity.setCooldownExpiry(Instant.now().plusSeconds(perk.cooldownSeconds()));
                }

                return playerPerkRepository.updateAsync(entity).thenApply(saved -> true);
            });
    }

    @Override
    public CompletableFuture<Optional<Duration>> getCooldownRemaining(@NotNull UUID playerId, @NotNull String perkId) {
        return playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, perkId)
            .thenApply(existing -> {
                if (existing.isEmpty()) {
                    return Optional.empty();
                }
                var entity = existing.get();
                if (!entity.isOnCooldown()) {
                    return Optional.empty();
                }
                return Optional.of(Duration.between(Instant.now(), entity.cooldownExpiry()));
            });
    }

    @Override
    public CompletableFuture<Optional<PlayerPerkState>> getPlayerPerkState(@NotNull UUID playerId, @NotNull String perkId) {
        return playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, perkId)
            .thenApply(existing -> existing.map(PlayerPerkEntity::toState));
    }

    @Override
    public CompletableFuture<List<PlayerPerkState>> getPlayerPerks(@NotNull UUID playerId) {
        return playerPerkRepository.findByPlayerIdAsync(playerId)
            .thenApply(entities -> entities.stream()
                .map(PlayerPerkEntity::toState)
                .toList());
    }

    @Override
    public CompletableFuture<Void> cleanupPlayer(@NotNull UUID playerId) {
        return playerPerkRepository.deactivateAllByPlayerIdAsync(playerId)
            .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<Void> reload() {
        return CompletableFuture.completedFuture(null);
    }
}
