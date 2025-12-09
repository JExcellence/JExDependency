package com.raindropcentral.rdq.bounty.utility;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.BountyServiceProvider;
import com.raindropcentral.rdq.bounty.DamageTracker;
import com.raindropcentral.rdq.bounty.IBountyService;
import com.raindropcentral.rdq.bounty.claim.ClaimHandler;
import com.raindropcentral.rdq.bounty.claim.ClaimResult;
import com.raindropcentral.rdq.bounty.config.BountySection;
import com.raindropcentral.rdq.bounty.distribution.RewardDistributor;
import com.raindropcentral.rdq.bounty.distribution.RewardDistributorFactory;
import com.raindropcentral.rdq.bounty.type.EClaimMode;
import com.raindropcentral.rdq.bounty.type.EDistributionMode;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for managing bounties in the RDQ system.
 * Automatically uses the correct service implementation (Free or Premium) based on the build.
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyFactory {

    private final static Logger LOGGER = CentralLogger.getLogger("RDQ");
    private final static String FOLDER_PATH = "bounty";
    private final static String FILE_NAME = "bounty.yml";

    private final Map<UUID, Bounty> bountyCache = new ConcurrentHashMap<>();
    private final IBountyService bountyService;
    private final DamageTracker damageTracker;
    private final ClaimHandler claimHandler;
    private final RDQ rdq;

    private BountySection bountyConfiguration;
    private EClaimMode claimMode;
    private EDistributionMode distributionMode;

    /**
     * Creates a new Bounty Factory.
     * Automatically loads the correct service implementation (Free or Premium).
     *
     * @param rdq the RDQ plugin instance
     */
    public BountyFactory(@NotNull RDQ rdq) {
        this.rdq = rdq;
        
        // Automatically get the correct service (Free or Premium)
        this.bountyService = BountyServiceProvider.getInstance();
        
        String serviceType = bountyService.isPremium() ? "Premium" : "Free";
        LOGGER.log(Level.INFO, "BountyFactory initialized with " + serviceType + " service");
        LOGGER.log(Level.INFO, "Max bounties per commissioner: " + bountyService.getMaxBountiesPerCommissioner());
        LOGGER.log(Level.INFO, "Max rewards per bounty: " + bountyService.getMaxBountyRewardsPerTarget());

        // Load configuration
        try {
            var cfgManager = new ConfigManager(this.rdq.getPlugin(), FOLDER_PATH);
            var cfgKeeper = new ConfigKeeper<>(cfgManager, FILE_NAME, BountySection.class);
            bountyConfiguration = cfgKeeper.rootSection;
            LOGGER.log(Level.INFO, "Bounty configuration loaded successfully");
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to load bounty configuration, using defaults", exception);
            bountyConfiguration = new BountySection(new EvaluationEnvironmentBuilder());
        }

        claimMode = bountyConfiguration.getClaimMode();
        distributionMode = bountyConfiguration.getDefaultDistributionMode();
        damageTracker = new DamageTracker(bountyConfiguration.getTrackingWindowInMs());
        claimHandler = new ClaimHandler(damageTracker, claimMode);
        
        // Load active bounties into cache
        loadActiveBounties();
    }

    /**
     * Loads all active bounties into the cache.
     */
    private void loadActiveBounties() {
        bountyService.findAll(0, 1000).thenAccept(bounties -> {
            for (Bounty bounty : bounties) {
                if (bounty.isActive()) {
                    bountyCache.put(bounty.getTargetUniqueId(), bounty);
                }
            }
            LOGGER.log(Level.INFO, "Loaded " + bountyCache.size() + " active bounties into cache");
        }).exceptionally(throwable -> {
            LOGGER.log(Level.SEVERE, "Failed to load active bounties", throwable);
            return null;
        });
    }

    /**
     * Creates a new bounty for a target player.
     *
     * @param targetUniqueId the target player's UUID
     * @param commissionerUniqueId the commissioner's UUID
     * @return a future containing the created bounty
     */
    public CompletableFuture<Bounty> createBounty(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId
    ) {
        return bountyService.create(targetUniqueId, commissionerUniqueId)
                .thenApply(bounty -> {
                    bountyCache.put(targetUniqueId, bounty);
                    LOGGER.log(Level.INFO, "Created bounty for " + targetUniqueId + " by " + commissionerUniqueId);
                    return bounty;
                });
    }

    /**
     * Creates a new bounty with rewards.
     *
     * @param targetUniqueId the target player's UUID
     * @param commissionerUniqueId the commissioner's UUID
     * @param rewards the list of rewards
     * @return a future containing the created bounty
     */
    public CompletableFuture<Bounty> createBounty(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId,
            @NotNull List<BountyReward> rewards
    ) {
        return bountyService.create(targetUniqueId, commissionerUniqueId, rewards)
                .thenApply(bounty -> {
                    bountyCache.put(targetUniqueId, bounty);
                    LOGGER.log(Level.INFO, "Created bounty with " + rewards.size() + " rewards for " + targetUniqueId);
                    return bounty;
                });
    }

    /**
     * Updates an existing bounty.
     *
     * @param bounty the bounty to update
     * @return a future containing the updated bounty
     */
    public CompletableFuture<Bounty> updateBounty(@NotNull Bounty bounty) {
        return bountyService.update(bounty)
                .thenApply(updated -> {
                    bountyCache.put(updated.getTargetUniqueId(), updated);
                    return updated;
                });
    }

    /**
     * Deletes a bounty.
     *
     * @param bounty the bounty to delete
     * @return a future containing the deleted bounty
     */
    public CompletableFuture<Bounty> deleteBounty(@NotNull Bounty bounty) {
        return bountyService.delete(bounty)
                .thenApply(deleted -> {
                    if (deleted) {
                        bountyCache.remove(bounty.getTargetUniqueId());
                        LOGGER.log(Level.INFO, "Deleted bounty for " + bounty.getTargetUniqueId());
                        return bounty;
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Gets a bounty for a specific player.
     *
     * @param uniqueId the player's UUID
     * @return the bounty, or null if not found
     */
    @Nullable
    public Bounty getBounty(@NotNull UUID uniqueId) {
        return bountyCache.get(uniqueId);
    }

    /**
     * Gets a bounty for a specific player asynchronously.
     *
     * @param uniqueId the player's UUID
     * @return a future containing the bounty
     */
    public CompletableFuture<Bounty> getBountyAsync(@NotNull UUID uniqueId) {
        Bounty cached = bountyCache.get(uniqueId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return bountyService.findPlayerBounty(uniqueId)
                .thenApply(bounty -> {
                    if (bounty != null && bounty.isActive()) {
                        bountyCache.put(uniqueId, bounty);
                    }
                    return bounty;
                });
    }

    /**
     * Checks if a player has a bounty.
     *
     * @param uniqueId the player's UUID
     * @return true if the player has an active bounty
     */
    public boolean hasBounty(@NotNull UUID uniqueId) {
        return bountyCache.containsKey(uniqueId);
    }

    /**
     * Checks if a player can create a bounty.
     *
     * @param player the player
     * @return true if the player can create a bounty
     */
    public boolean canCreateBounty(@NotNull Player player) {
        return bountyService.canCreateBounty(player);
    }

    /**
     * Gets all bounties created by a commissioner.
     *
     * @param commissionerUniqueId the commissioner's UUID
     * @return a future containing the list of bounties
     */
    public CompletableFuture<List<Bounty>> getBountiesByCommissioner(@NotNull UUID commissionerUniqueId) {
        return bountyService.findBountiesByCommissioner(commissionerUniqueId);
    }

    /**
     * Gets the total number of bounties.
     *
     * @return a future containing the total count
     */
    public CompletableFuture<Integer> getTotalBounties() {
        return bountyService.getTotalBounties();
    }

    /**
     * Gets the bounty service being used.
     *
     * @return the bounty service
     */
    @NotNull
    public IBountyService getBountyService() {
        return bountyService;
    }

    /**
     * Gets the damage tracker.
     *
     * @return the damage tracker
     */
    @NotNull
    public DamageTracker getDamageTracker() {
        return damageTracker;
    }

    /**
     * Gets the bounty configuration.
     *
     * @return the configuration
     */
    @NotNull
    public BountySection getBountyConfiguration() {
        return bountyConfiguration;
    }

    /**
     * Gets the claim mode.
     *
     * @return the claim mode
     */
    @NotNull
    public EClaimMode getClaimMode() {
        return claimMode;
    }

    /**
     * Checks if the service is premium.
     *
     * @return true if premium, false if free
     */
    public boolean isPremium() {
        return bountyService.isPremium();
    }

    /**
     * Gets the maximum bounties per commissioner.
     *
     * @return the maximum number
     */
    public int getMaxBountiesPerCommissioner() {
        return bountyService.getMaxBountiesPerCommissioner();
    }

    /**
     * Gets the maximum rewards per bounty.
     *
     * @return the maximum number
     */
    public int getMaxRewardsPerBounty() {
        return bountyService.getMaxBountyRewardsPerTarget();
    }

    /**
     * Claims a bounty when a target is killed.
     *
     * @param victimUuid the UUID of the killed target
     * @param lastHitterUuid the UUID of the player who dealt the final blow (may be null)
     * @param deathLocation the location where the target died
     * @return a future containing the claim result
     */
    public CompletableFuture<ClaimResult> claimBounty(
            @NotNull UUID victimUuid,
            @Nullable UUID lastHitterUuid,
            @NotNull Location deathLocation
    ) {
        return getBountyAsync(victimUuid).thenCompose(bounty -> {
            if (bounty == null || !bounty.isActive()) {
                return CompletableFuture.completedFuture(ClaimResult.empty());
            }

            // Determine winner(s) using ClaimHandler
            ClaimResult claimResult = claimHandler.determineClaim(victimUuid, lastHitterUuid);
            
            if (!claimResult.hasWinners()) {
                claimHandler.clearDamage(victimUuid);
                return CompletableFuture.completedFuture(ClaimResult.empty());
            }

            // Mark bounty as claimed
            bounty.claim(claimResult.winners().keySet().iterator().next());
            
            // Distribute rewards to all winners
            RewardDistributor distributor = RewardDistributorFactory.create(distributionMode);
            List<CompletableFuture<Void>> distributionFutures = claimResult.winners().entrySet().stream()
                    .<CompletableFuture<Void>>map(entry -> {
                        UUID winnerUuid = entry.getKey();
                        double proportion = entry.getValue();
                        Player winner = Bukkit.getPlayer(winnerUuid);
                        
                        if (winner != null && winner.isOnline()) {
                            return distributor.distributeRewards(winner, bounty, deathLocation, proportion);
                        }
                        return CompletableFuture.completedFuture(null);
                    })
                    .toList();

            // Wait for all distributions to complete, then update bounty
            return CompletableFuture.allOf(distributionFutures.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> updateBounty(bounty))
                    .thenApply(updated -> {
                        bountyCache.remove(victimUuid);
                        claimHandler.clearDamage(victimUuid);
                        LOGGER.log(Level.INFO, "Bounty claimed on " + victimUuid + " by " + 
                                claimResult.getWinnerCount() + " hunter(s)");
                        return claimResult;
                    });
        });
    }

    /**
     * Gets the claim handler.
     *
     * @return the claim handler
     */
    @NotNull
    public ClaimHandler getClaimHandler() {
        return claimHandler;
    }

    /**
     * Gets the distribution mode.
     *
     * @return the distribution mode
     */
    @NotNull
    public EDistributionMode getDistributionMode() {
        return distributionMode;
    }

    /**
     * Refreshes the bounty cache.
     */
    public void refreshCache() {
        bountyCache.clear();
        loadActiveBounties();
    }
}
