package com.raindropcentral.rdq.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.config.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.BountyHunterRepository;
import com.raindropcentral.rdq.database.repository.BountyRepository;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Premium version of the Bounty Service with full functionality.
 * Includes database persistence, configuration support, and unlimited features.
 * 
 * Features:
 * - Configurable bounty limits per commissioner
 * - Configurable reward limits per bounty
 * - Full database persistence
 * - Advanced hunter stats tracking with leaderboards
 * - Hunter level system
 * - Reward distribution system
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class PremiumBountyService implements IBountyService {

    private static final Logger LOGGER = Logger.getLogger(PremiumBountyService.class.getName());

    private static PremiumBountyService instance;
    
    private final RDQ rdq;

    /**
     * Private constructor.
     *
     * @param rdq the RDQ instance
     */
    private PremiumBountyService(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Initializes the Premium Bounty Service with the RDQ instance.
     * This must be called before using the service.
     *
     * @param rdq the RDQ instance
     * @return the initialized service instance
     */
    public static PremiumBountyService initialize(@NotNull RDQ rdq) {
        if (instance == null) {
            instance = new PremiumBountyService(rdq);
            LOGGER.log(Level.INFO, "Premium Bounty Service initialized");
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     * 
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static PremiumBountyService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PremiumBountyService not initialized. Call initialize() first.");
        }
        return instance;
    }

    private BountyRepository getBountyRepository() {
        return rdq.getBountyRepository();
    }

    private BountyHunterRepository getHunterStatsRepository() {
        return rdq.getBountyHunterRepository();
    }

    private BountySection getConfig() {
        try {
            var cfgManager = new ConfigManager(rdq.getPlugin(), "bounty");
            var cfgKeeper = new ConfigKeeper<>(cfgManager, "bounty.yml", BountySection.class);
            return cfgKeeper.rootSection;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load bounty configuration, using defaults", e);
            return new BountySection(new EvaluationEnvironmentBuilder());
        }
    }

    @Override
    public CompletableFuture<List<Bounty>> findAll(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getBountyRepository().findAllByAttributes(Map.of("active", true));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to fetch bounties", e);
                throw new RuntimeException("Failed to fetch bounties", e);
            }
        });
    }

    @Override
    public CompletableFuture<Bounty> findPlayerBounty(@NotNull UUID uniqueId) {
        return CompletableFuture.supplyAsync(
            () -> getBountyRepository().findByAttributes(Map.of("targetUniqueId", uniqueId)).orElse(null),
            this.rdq.getExecutor()
        );
    }

    @Override
    public CompletableFuture<List<Bounty>> findBountiesByCommissioner(@NotNull UUID commissionerUniqueId) {
        return CompletableFuture.supplyAsync(
            () -> getBountyRepository().findAllByAttributes(Map.of("commissionerUniqueId", commissionerUniqueId)),
            this.rdq.getExecutor()
        );
    }

    @Override
    public CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId
    ) {
        return create(targetUniqueId, commissionerUniqueId, List.of());
    }

    @Override
    public CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId,
            @NotNull List<BountyReward> rewards
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Bounty> existingBounties = getBountyRepository().findAllByAttributes(Map.of("commissionerUniqueId", commissionerUniqueId));
                int maxBounties = getConfig().getMaxBountiesPerCommissioner();
                
                if (existingBounties.size() >= maxBounties) {
                    throw new IllegalStateException(
                        "Commissioner has reached maximum bounty limit: " + maxBounties
                    );
                }

                int maxRewards = getConfig().getMaxRewardsPerBounty();
                if (rewards.size() > maxRewards) {
                    throw new IllegalArgumentException(
                        "Too many rewards. Maximum allowed: " + maxRewards
                    );
                }

                Bounty bounty = new Bounty(targetUniqueId, commissionerUniqueId, new ArrayList<>());

                for (BountyReward reward : rewards) {
                    bounty.addReward(reward);
                }

                Bounty savedBounty = getBountyRepository().create(bounty);
                
                LOGGER.log(Level.INFO, "Created bounty for target: " + targetUniqueId + 
                          " by commissioner: " + commissionerUniqueId);
                
                return savedBounty;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create bounty", e);
                throw new RuntimeException("Failed to create bounty", e);
            }
        });
    }

    @Override
    public CompletableFuture<Bounty> update(@NotNull Bounty bounty) {
        return CompletableFuture.supplyAsync(() -> {
            return updateWithRetry(bounty, 3);
        });
    }
    
    private Bounty updateWithRetry(@NotNull Bounty bounty, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Bounty updated = getBountyRepository().update(bounty);
                LOGGER.log(Level.FINE, "Updated bounty: " + bounty.getId() + " (attempt " + attempt + ")");
                return updated;
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("OptimisticLock")) {
                    if (attempt < maxRetries) {
                        LOGGER.log(Level.WARNING, "OptimisticLockException on bounty " + bounty.getId() + 
                                  ", attempt " + attempt + "/" + maxRetries + ". Retrying with fresh entity...");

                        try {
                            // Add a small delay before retry to allow other transactions to complete
                            Thread.sleep(50 * attempt);
                            
                            Bounty freshBounty = getBountyRepository().findById(bounty.getId()).orElse(null);
                            if (freshBounty != null) {
                                // Copy the claim state from the stale bounty to the fresh one
                                freshBounty.setActive(bounty.isActive());
                                if (bounty.getClaimedAt() != null) {
                                    freshBounty.setClaimedAt(bounty.getClaimedAt());
                                }
                                if (bounty.getClaimedBy() != null) {
                                    freshBounty.claim(bounty.getClaimedBy());
                                }
                                bounty = freshBounty;
                                LOGGER.log(Level.INFO, "Retrying with fresh bounty entity (attempt " + attempt + ")");
                                continue;
                            }
                        } catch (Exception fetchEx) {
                            LOGGER.log(Level.WARNING, "Failed to fetch fresh bounty entity for retry", fetchEx);
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to update bounty " + bounty.getId() + 
                                  " after " + maxRetries + " attempts due to OptimisticLockException", e);
                    }
                }
                
                if (attempt == maxRetries) {
                    LOGGER.log(Level.SEVERE, "Failed to update bounty: " + bounty.getId() + 
                              " after " + maxRetries + " attempts", e);
                    throw new RuntimeException("Failed to update bounty", e);
                }
            }
        }
        throw new RuntimeException("Update retry loop completed without success");
    }

    @Override
    public CompletableFuture<Boolean> delete(@NotNull Bounty bounty) {
        if (bounty.getId() == null) {
            return CompletableFuture.completedFuture(false);
        }

        return getBountyRepository().deleteAsync(bounty.getId());
    }

    @Override
    public CompletableFuture<Integer> getTotalBounties() {
        return getBountyRepository().findAllAsync(0, Integer.MAX_VALUE).thenApply(bounties -> bounties.size());
    }

    @Override
    public CompletableFuture<BountyHunter> getBountyHunter(@NotNull RDQPlayer player) {
        return CompletableFuture.supplyAsync(
            () -> getHunterStatsRepository().findByAttributes(Map.of("player", player)).orElse(null),
            this.rdq.getExecutor()
        );
    }

    @Override
    public CompletableFuture<List<BountyHunter>> getTopHunters(int limit, @NotNull String orderBy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getHunterStatsRepository().findAll(0, Integer.MAX_VALUE).stream().sorted(Comparator.comparing(BountyHunter::getHighestBountyValue)).toList();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to get top hunters", e);
                throw new RuntimeException("Failed to get top hunters", e);
            }
        });
    }

    @Override
    public CompletableFuture<BountyHunter> claim(@NotNull RDQPlayer player, double rewardValue) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BountyHunter stats = getHunterStatsRepository().findByAttributes(Map.of("player", player)).orElse(null);

                if (stats == null) {
                    BountyHunter newStats = new BountyHunter(player);
                    newStats.setBountiesClaimed(0);
                    newStats.setTotalRewardValue(0.0);
                    return newStats;
                }

                stats.setBountiesClaimed(stats.getBountiesClaimed() + 1);
                stats.setTotalRewardValue(stats.getTotalRewardValue() + rewardValue);
                stats.setLastClaimTimestamp(System.currentTimeMillis());

                BountyHunter updated = getHunterStatsRepository().create(stats);
                
                LOGGER.log(Level.INFO, "Hunter " + player.getUniqueId() + " claimed bounty worth " + rewardValue);
                
                return updated;
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to process bounty claim for: " + player.getUniqueId(), e);
                throw new RuntimeException("Failed to process bounty claim", e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getHunterLevel(@NotNull UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BountyHunter stats = getHunterStatsRepository().findByAttributes(Map.of("player.uniqueId", uniqueId)).orElse(null);
                
                if (stats == null) {
                    return 1;
                }

                int totalClaimed = stats.getBountiesClaimed();
                return 1 + (totalClaimed / 10);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to calculate hunter level for: " + uniqueId, e);
                return 1;
            }
        });
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        try {
            List<Bounty> existingBounties = getBountyRepository().findAllByAttributes(Map.of("player.uniqueId", player.getUniqueId()));
            int maxBounties = getConfig().getMaxBountiesPerCommissioner();
            return existingBounties.size() < maxBounties;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check bounty creation permission", e);
            return false;
        }
    }

    @Override
    public int getMaxBountiesPerCommissioner() {
        return getConfig().getMaxBountiesPerCommissioner();
    }

    @Override
    public int getMaxBountyRewardsPerTarget() {
        return getConfig().getMaxRewardsPerBounty();
    }
}
