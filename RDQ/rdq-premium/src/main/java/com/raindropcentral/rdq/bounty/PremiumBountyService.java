package com.raindropcentral.rdq.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.RDQPremiumImpl;
import com.raindropcentral.rdq.bounty.config.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.BountyHunterRepository;
import com.raindropcentral.rdq.database.repository.BountyRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    
    private final BountyRepository bountyRepository;
    private final BountyHunterRepository hunterStatsRepository;
    private final BountySection config;

    /**
     * Constructor for ServiceLoader that extracts repositories from RDQPremiumImpl.
     * Initializes the service with repositories and configuration from the RDQ instance.
     */
    public PremiumBountyService(@NotNull RDQPremiumImpl impl) {
        // Access the RDQ instance from the impl
        RDQ rdq = impl.getRdq();
        
        if (rdq == null) {
            throw new IllegalStateException("RDQ instance not initialized in RDQPremiumImpl");
        }
        
        // Extract repositories from RDQ
        this.bountyRepository = rdq.getBountyRepository();
        this.hunterStatsRepository = rdq.getBountyHunterRepository();
        this.config = rdq.getBountyFactory().getBountyConfiguration();
        
        // Set as singleton instance
        instance = this;
    }

    /**
     * Creates a new Premium Bounty Service with dependencies.
     *
     * @param bountyRepository the bounty repository for database operations
     * @param hunterStatsRepository the hunter stats repository
     * @param config the bounty configuration
     */
    private PremiumBountyService(
            @NotNull BountyRepository bountyRepository,
            @NotNull BountyHunterRepository hunterStatsRepository,
            @NotNull BountySection config
    ) {
        this.bountyRepository = bountyRepository;
        this.hunterStatsRepository = hunterStatsRepository;
        this.config = config;
    }

    /**
     * Initializes the Premium Bounty Service with required dependencies.
     * This must be called before using the service.
     *
     * @param bountyRepository the bounty repository
     * @param hunterStatsRepository the hunter stats repository
     * @param config the bounty configuration
     * @return the initialized service instance
     */
    public static PremiumBountyService initialize(
            @NotNull BountyRepository bountyRepository,
            @NotNull BountyHunterRepository hunterStatsRepository,
            @NotNull BountySection config
    ) {
        if (instance == null) {
            instance = new PremiumBountyService(bountyRepository, hunterStatsRepository, config);
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

    /**
     * Checks if dependencies are initialized.
     */
    private void checkInitialized() {
        if (bountyRepository == null || hunterStatsRepository == null || config == null) {
            throw new IllegalStateException("PremiumBountyService not properly initialized");
        }
    }

    @Override
    public CompletableFuture<List<Bounty>> findAll(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            checkInitialized();
            try {
                return bountyRepository.findAll(page, pageSize);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to fetch bounties", e);
                throw new RuntimeException("Failed to fetch bounties", e);
            }
        });
    }

    @Override
    public CompletableFuture<Bounty> findPlayerBounty(@NotNull UUID uniqueId) {
        return bountyRepository.findByAttributesAsync(Map.of("targetUniqueId", uniqueId));
    }

    @Override
    public CompletableFuture<List<Bounty>> findBountiesByCommissioner(@NotNull UUID commissionerUniqueId) {
        return bountyRepository.findListByAttributesAsync(Map.of("commissionerUniqueId", commissionerUniqueId));
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
                // Validate commissioner hasn't exceeded their limit
                List<Bounty> existingBounties = bountyRepository.findListByAttributes(Map.of("commissionerUniqueId", commissionerUniqueId));
                int maxBounties = config.getMaxBountiesPerCommissioner();
                
                if (existingBounties.size() >= maxBounties) {
                    throw new IllegalStateException(
                        "Commissioner has reached maximum bounty limit: " + maxBounties
                    );
                }
                
                // Validate reward count
                int maxRewards = config.getMaxRewardsPerBounty();
                if (rewards.size() > maxRewards) {
                    throw new IllegalArgumentException(
                        "Too many rewards. Maximum allowed: " + maxRewards
                    );
                }
                
                // Create bounty
                Bounty bounty = new Bounty(targetUniqueId, commissionerUniqueId, rewards);
                
                // Add rewards
                for (BountyReward reward : rewards) {
                    bounty.addReward(reward);
                }
                
                // Persist to database
                Bounty savedBounty = bountyRepository.create(bounty);
                
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
            try {
                Bounty updated = bountyRepository.update(bounty);
                LOGGER.log(Level.FINE, "Updated bounty: " + bounty.getId());
                return updated;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to update bounty: " + bounty.getId(), e);
                throw new RuntimeException("Failed to update bounty", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(@NotNull Bounty bounty) {
        if (bounty.getId() == null) {
            return CompletableFuture.completedFuture(false);
        }

        return bountyRepository.deleteAsync(bounty.getId());
    }

    @Override
    public CompletableFuture<Integer> getTotalBounties() {
        return bountyRepository.findAllAsync(0, Integer.MAX_VALUE).thenApply(bounties -> bounties.size());
    }

    @Override
    public CompletableFuture<BountyHunter> getBountyHunter(@NotNull RDQPlayer player) {
        return hunterStatsRepository.findByAttributesAsync(Map.of("player", player));
    }

    @Override
    public CompletableFuture<List<BountyHunter>> getTopHunters(int limit, @NotNull String orderBy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return hunterStatsRepository.findAll(0, Integer.MAX_VALUE).stream().sorted(Comparator.comparing(BountyHunter::getHighestBountyValue)).toList();
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
                BountyHunter stats = hunterStatsRepository.findByAttributes(Map.of("player", player));

                if (stats == null) {
                    BountyHunter newStats = new BountyHunter(player);
                    newStats.setBountiesClaimed(0);
                    newStats.setTotalRewardValue(0.0);
                    return newStats;
                }
                
                // Update stats
                stats.setBountiesClaimed(stats.getBountiesClaimed() + 1);
                stats.setTotalRewardValue(stats.getTotalRewardValue() + rewardValue);
                stats.setLastClaimTimestamp(System.currentTimeMillis());
                
                // Save updated stats
                BountyHunter updated = hunterStatsRepository.create(stats);
                
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
                BountyHunter stats = hunterStatsRepository.findByAttributes(Map.of("player.uniqueId", uniqueId));
                
                if (stats == null) {
                    return 1; // Default level
                }
                
                // Calculate level based on total bounties claimed
                // Formula: level = 1 + (totalClaimed / 10)
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
            List<Bounty> existingBounties = bountyRepository.findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()));
            int maxBounties = config.getMaxBountiesPerCommissioner();
            return existingBounties.size() < maxBounties;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check bounty creation permission", e);
            return false;
        }
    }

    @Override
    public int getMaxBountiesPerCommissioner() {
        return config.getMaxBountiesPerCommissioner();
    }

    @Override
    public int getMaxBountyRewardsPerTarget() {
        return config.getMaxRewardsPerBounty();
    }
}
