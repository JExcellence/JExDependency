package com.raindropcentral.rdq.bounty.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.raindropcentral.rdq.api.FreeBountyService;
import com.raindropcentral.rdq.bounty.*;
import com.raindropcentral.rdq.bounty.config.BountyConfig;
import com.raindropcentral.rdq.bounty.economy.EconomyService;
import com.raindropcentral.rdq.bounty.repository.BountyRepository;
import com.raindropcentral.rdq.bounty.repository.HunterStatsRepository;
import com.raindropcentral.rdq.database.entity.bounty.BountyEntity;
import com.raindropcentral.rdq.database.entity.bounty.HunterStatsEntity;
import com.raindropcentral.rdq.shared.error.RDQError;
import com.raindropcentral.rdq.shared.error.RDQException;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultFreeBountyService implements FreeBountyService {

    private static final Logger LOGGER = Logger.getLogger(DefaultFreeBountyService.class.getName());

    protected final BountyRepository bountyRepository;
    protected final HunterStatsRepository statsRepository;
    protected final EconomyService economyService;
    protected final BountyConfig config;
    protected final Cache<UUID, List<Bounty>> bountyCache;

    public DefaultFreeBountyService(
        @NotNull BountyRepository bountyRepository,
        @NotNull HunterStatsRepository statsRepository,
        @NotNull EconomyService economyService,
        @NotNull BountyConfig config
    ) {
        this.bountyRepository = bountyRepository;
        this.statsRepository = statsRepository;
        this.economyService = economyService;
        this.config = config;
        this.bountyCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    }


    @Override
    public CompletableFuture<Bounty> createBounty(@NotNull BountyRequest request) {
        var amount = request.amount();

        if (amount.compareTo(config.minAmount()) < 0) {
            return CompletableFuture.failedFuture(
                new RDQException(new RDQError.InvalidAmount(config.minAmount(), config.maxAmount()))
            );
        }
        if (amount.compareTo(config.maxAmount()) > 0) {
            return CompletableFuture.failedFuture(
                new RDQException(new RDQError.InvalidAmount(config.minAmount(), config.maxAmount()))
            );
        }

        return economyService.withdraw(request.placerId(), amount, request.currency())
            .thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.failedFuture(
                        new RDQException(new RDQError.InsufficientFunds(amount, BigDecimal.ZERO))
                    );
                }

                var expiresAt = config.expirationHours() > 0
                    ? Instant.now().plus(Duration.ofHours(config.expirationHours()))
                    : null;

                var entity = BountyEntity.create(
                    request.placerId(),
                    request.targetId(),
                    amount,
                    request.currency(),
                    expiresAt
                );

                bountyCache.invalidate(request.targetId());

                var placerName = getPlayerName(request.placerId());
                return bountyRepository.createBountyAsync(entity)
                    .thenCompose(saved -> recordBountyPlaced(request.placerId(), placerName, amount)
                        .thenApply(v -> saved));
            });
    }

    @Override
    public CompletableFuture<ClaimResult> claimBounty(@NotNull UUID hunterId, @NotNull UUID targetId) {
        return bountyRepository.findAllActiveByTargetAsync(targetId)
            .thenCompose(bounties -> {
                if (bounties.isEmpty()) {
                    return CompletableFuture.failedFuture(
                        new RDQException(new RDQError.NotFound("bounty", targetId.toString()))
                    );
                }

                var total = bounties.stream()
                    .map(Bounty::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                var currency = bounties.getFirst().currency();

                return claimAllBounties(bounties, hunterId)
                    .thenCompose(claimed -> {
                        if (claimed.isEmpty()) {
                            return CompletableFuture.failedFuture(
                                new RDQException(new RDQError.NotFound("bounty", targetId.toString()))
                            );
                        }

                        bountyCache.invalidate(targetId);

                        var hunterName = getPlayerName(hunterId);
                        var targetName = getPlayerName(targetId);

                        return economyService.deposit(hunterId, total, currency)
                            .thenCompose(v -> recordBountyClaimed(hunterId, hunterName, total))
                            .thenCompose(v -> recordDeath(targetId, targetName))
                            .thenApply(v -> new ClaimResult(claimed.getFirst(), total, getDistributionMode()));
                    });
            });
    }

    protected DistributionMode getDistributionMode() {
        return DistributionMode.INSTANT;
    }


    @Override
    public CompletableFuture<List<Bounty>> getActiveBounties() {
        return bountyRepository.findAllActiveAsync();
    }

    @Override
    public CompletableFuture<List<Bounty>> getBountiesOnPlayer(@NotNull UUID targetId) {
        var cached = bountyCache.getIfPresent(targetId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return bountyRepository.findAllActiveByTargetAsync(targetId)
            .thenApply(bounties -> {
                bountyCache.put(targetId, bounties);
                return bounties;
            });
    }

    @Override
    public CompletableFuture<List<Bounty>> getBountiesPlacedBy(@NotNull UUID placerId) {
        return bountyRepository.findByPlacerIdAsync(placerId);
    }

    @Override
    public CompletableFuture<Optional<HunterStats>> getHunterStats(@NotNull UUID playerId) {
        return statsRepository.findByPlayerIdAsync(playerId)
            .thenApply(opt -> opt.map(this::toHunterStats));
    }

    @Override
    public CompletableFuture<List<HunterStats>> getLeaderboard(int limit) {
        return statsRepository.findTopByBountiesClaimedAsync(Math.min(limit, 100))
            .thenApply(list -> list.stream()
                .map(this::toHunterStats)
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Boolean> cancelBounty(@NotNull UUID placerId, long bountyId) {
        return bountyRepository.findEntityByIdAsync(bountyId)
            .thenCompose(opt -> {
                if (opt.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }

                var entity = opt.get();
                if (!entity.placerId().equals(placerId)) {
                    return CompletableFuture.completedFuture(false);
                }

                if (!entity.isActive()) {
                    return CompletableFuture.completedFuture(false);
                }

                entity.cancel();
                bountyCache.invalidate(entity.targetId());

                return bountyRepository.updateBountyAsync(entity)
                    .thenCompose(saved -> economyService.deposit(placerId, entity.amount(), entity.currency()))
                    .thenApply(v -> true);
            });
    }

    @Override
    public CompletableFuture<Void> expireOldBounties() {
        return bountyRepository.findExpiredAsync()
            .thenCompose(expired -> {
                if (expired.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }

                var refundFutures = expired.stream()
                    .map(bounty -> {
                        bountyCache.invalidate(bounty.targetId());
                        // Need to get entity to expire it
                        return bountyRepository.findEntityByIdAsync(bounty.id())
                            .thenCompose(opt -> {
                                if (opt.isEmpty()) return CompletableFuture.completedFuture(false);
                                var entity = opt.get();
                                entity.expire();
                                return bountyRepository.updateBountyAsync(entity)
                                    .thenCompose(v -> economyService.deposit(bounty.placerId(), bounty.amount(), bounty.currency()));
                            });
                    })
                    .toList();

                return CompletableFuture.allOf(refundFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        if (!expired.isEmpty()) {
                            LOGGER.info("Expired " + expired.size() + " bounties and refunded placers");
                        }
                        return null;
                    });
            });
    }

    /**
     * Claims all bounties for a hunter.
     */
    protected CompletableFuture<List<Bounty>> claimAllBounties(List<Bounty> bounties, UUID hunterId) {
        var claimFutures = bounties.stream()
            .map(bounty -> bountyRepository.findEntityByIdAsync(bounty.id())
                .thenCompose(opt -> {
                    if (opt.isEmpty()) return CompletableFuture.completedFuture(bounty);
                    var entity = opt.get();
                    entity.claim(hunterId);
                    return bountyRepository.updateBountyAsync(entity);
                }))
            .toList();

        return CompletableFuture.allOf(claimFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> bounties);
    }

    /**
     * Records a bounty placement in hunter stats.
     */
    protected CompletableFuture<Void> recordBountyPlaced(UUID placerId, String playerName, BigDecimal amount) {
        return statsRepository.findByPlayerIdAsync(placerId)
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    HunterStatsEntity stats = opt.get();
                    stats.incrementBountiesPlaced(amount);
                    return statsRepository.updateAsync(stats);
                } else {
                    HunterStatsEntity stats = HunterStatsEntity.create(placerId, playerName);
                    stats.incrementBountiesPlaced(amount);
                    return statsRepository.createAsync(stats);
                }
            })
            .thenApply(v -> null);
    }

    /**
     * Records a bounty claim in hunter stats.
     */
    protected CompletableFuture<Void> recordBountyClaimed(UUID hunterId, String playerName, BigDecimal amount) {
        return statsRepository.findByPlayerIdAsync(hunterId)
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    HunterStatsEntity stats = opt.get();
                    stats.incrementBountiesClaimed(amount);
                    return statsRepository.updateAsync(stats);
                } else {
                    HunterStatsEntity stats = HunterStatsEntity.create(hunterId, playerName);
                    stats.incrementBountiesClaimed(amount);
                    return statsRepository.createAsync(stats);
                }
            })
            .thenApply(v -> null);
    }

    /**
     * Records a death in hunter stats.
     */
    protected CompletableFuture<Void> recordDeath(UUID targetId, String playerName) {
        return statsRepository.findByPlayerIdAsync(targetId)
            .thenCompose(opt -> {
                if (opt.isPresent()) {
                    HunterStatsEntity stats = opt.get();
                    stats.incrementDeaths();
                    return statsRepository.updateAsync(stats);
                } else {
                    HunterStatsEntity stats = HunterStatsEntity.create(targetId, playerName);
                    stats.incrementDeaths();
                    return statsRepository.createAsync(stats);
                }
            })
            .thenApply(v -> null);
    }

    /**
     * Converts a HunterStatsEntity to a HunterStats domain object.
     */
    protected HunterStats toHunterStats(HunterStatsEntity entity) {
        return new HunterStats(
            entity.playerId(),
            entity.playerName(),
            entity.bountiesPlaced(),
            entity.bountiesClaimed(),
            entity.deaths(),
            entity.totalEarned(),
            entity.totalSpent()
        );
    }

    protected String getPlayerName(@NotNull UUID playerId) {
        var player = Bukkit.getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }
        var offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : playerId.toString().substring(0, 8);
    }

    public void invalidateCache(@NotNull UUID targetId) {
        bountyCache.invalidate(targetId);
    }

    public void clearCache() {
        bountyCache.invalidateAll();
    }
}
