/*
package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RDQPlayerStatistic;
import com.raindropcentral.rcore.service.RDQPlayerStatisticService;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rplatform.enumeration.EStatisticType;
import com.raindropcentral.rplatform.logger.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

//TODO NEEDS TO BE REDONE, figured that it doesnt work to use same statistics in here.
   gonna need core player for rcore and rdq player for rdq and access adapter to use statistics in here

*/
/**
 * Service for automatically tracking perk-related statistics.
 * <p>
 * This service provides comprehensive tracking of perk usage, activation counts,
 * cooldown violations, and other perk-related metrics. It integrates with the
 * {@link RDQPlayerStatisticService} to maintain persistent statistics.
 * </p>
 *
 * <h3>Tracked Statistics:</h3>
 * <ul>
 *     <li>Total perk activations across all perks</li>
 *     <li>Individual perk activation counts</li>
 *     <li>Perk usage time tracking</li>
 *     <li>Cooldown violation tracking</li>
 *     <li>Most used perk identification</li>
 *     <li>Perk purchase and ownership tracking</li>
 * </ul>
 *
 * @author JExcellence
 *//*

@SuppressWarnings("unused")
public class RPerkStatisticService {
    
    */
/**
     * Plugin name for perk statistics.
     *//*

    private static final String PLUGIN_NAME = "RDQImpl";
    
    */
/**
     * Cache for tracking active perk sessions (player -> perk -> start time).
     *//*

    private static final Map<String, Map<String, Long>> activePerkSessions = new ConcurrentHashMap<>();
    
    */
/**
     * Initializes perk statistics for a new player.
     *
     * @param playerStatistic the player statistic container
     *//*

    public static void initializePerkStatistics(
        final @NotNull RDQPlayerStatistic playerStatistic
    ) {
        
        Map<String, Object> perkDefaults = EStatisticType.getPerkDefaults();
        RDQPlayerStatisticService.addStatisticsBulk(
            playerStatistic,
            perkDefaults,
            PLUGIN_NAME
        );
        
        CentralLogger.getLogger(RPerkStatisticService.class.getName()).info(
            "Initialized perk statistics for player with " + perkDefaults.size() + " default perk statistics"
        );
    }
    
    */
/**
     * Records a perk activation and updates relevant statistics.
     *
     * @param player the player who activated the perk
     * @param perk   the perk that was activated
     *//*

    public static void recordPerkActivation(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        final RDQPlayerStatistic playerStatistic = player.getPlayerStatistic();
        if (
            playerStatistic == null
        ) {
            CentralLogger.getLogger(RPerkStatisticService.class.getName()).warning(
                "Cannot record perk activation: Player " + player.getPlayerName() + " has no statistics"
            );
            return;
        }
        
        final String perkId      = perk.getIdentifier();
        final long   currentTime = System.currentTimeMillis();
        
        // Update total perk activations
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            EStatisticType.TOTAL_PERKS_ACTIVATED.getKey(),
            PLUGIN_NAME,
            1.0
        );
        
        // Update individual perk activation count
        final String perkCountKey = EStatisticType.getPerkActivationCountKey(perkId);
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            perkCountKey,
            PLUGIN_NAME,
            1.0
        );
        
        // Update last perk activation timestamp
        RDQPlayerStatisticService.addOrUpdateStatistic(
            playerStatistic,
            EStatisticType.LAST_PERK_ACTIVATION.getKey(),
            PLUGIN_NAME,
            currentTime
        );
        
        // Update individual perk last used timestamp
        final String perkLastUsedKey = EStatisticType.getPerkLastUsedKey(perkId);
        RDQPlayerStatisticService.addOrUpdateStatistic(
            playerStatistic,
            perkLastUsedKey,
            PLUGIN_NAME,
            currentTime
        );
        
        // Start tracking usage time for this perk
        startPerkUsageTracking(
            player.getUniqueId().toString(),
            perkId,
            currentTime
        );
        
        // Update most used perk
        updateMostUsedPerk(
            playerStatistic,
            perkId
        );
        
        // Increment active perks count
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            EStatisticType.ACTIVE_PERKS_COUNT.getKey(),
            PLUGIN_NAME,
            1.0
        );
        
        CentralLogger.getLogger(RPerkStatisticService.class.getName()).info(
            "Recorded perk activation for player " + player.getPlayerName() + " with perk " + perkId
        );
    }
    
    */
/**
     * Records a perk deactivation and updates relevant statistics.
     *
     * @param player the player who deactivated the perk
     * @param perk   the perk that was deactivated
     *//*

    public static void recordPerkDeactivation(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        final RDQPlayerStatistic playerStatistic = player.getPlayerStatistic();
        if (playerStatistic == null) {
            return;
        }
        
        final String perkId   = perk.getIdentifier();
        final String playerId = player.getUniqueId().toString();
        
        // Stop tracking usage time and add to total
        final long usageTime = stopPerkUsageTracking(
            playerId,
            perkId
        );
        if (
            usageTime > 0
        ) {
            // Update total perk usage time
            RDQPlayerStatisticService.incrementNumericStatistic(
                playerStatistic,
                EStatisticType.PERK_USAGE_TIME.getKey(),
                PLUGIN_NAME,
                (double) usageTime
            );
            
            // Update individual perk usage time
            final String perkUsageTimeKey = EStatisticType.getPerkUsageTimeKey(perkId);
            RDQPlayerStatisticService.incrementNumericStatistic(
                playerStatistic,
                perkUsageTimeKey,
                PLUGIN_NAME,
                (double) usageTime
            );
        }
        
        // Decrement active perks count
        final Optional<Double> currentActive = RDQPlayerStatisticService.getStatisticValue(
            playerStatistic,
            EStatisticType.ACTIVE_PERKS_COUNT.getKey(),
            PLUGIN_NAME,
            Double.class
        );
        
        if (
            currentActive.isPresent() && currentActive.get() > 0
        ) {
            RDQPlayerStatisticService.addOrUpdateStatistic(
                playerStatistic,
                EStatisticType.ACTIVE_PERKS_COUNT.getKey(),
                PLUGIN_NAME,
                currentActive.get() - 1.0
            );
        }
        
        CentralLogger.getLogger(RPerkStatisticService.class.getName()).info(
            "Recorded perk deactivation for player " + player.getPlayerName() +
            " with perk " + perkId + " (usage time: " + usageTime + "ms)"
        );
    }
    
    */
/**
     * Records a cooldown violation when a player tries to activate a perk during cooldown.
     *
     * @param player the player who violated the cooldown
     * @param perk   the perk that was attempted to be activated
     *//*

    public static void recordCooldownViolation(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        final RDQPlayerStatistic playerStatistic = player.getPlayerStatistic();
        if (playerStatistic == null) {
            return;
        }
        
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            EStatisticType.PERK_COOLDOWN_VIOLATIONS.getKey(),
            PLUGIN_NAME,
            1.0
        );
        
        CentralLogger.getLogger(RPerkStatisticService.class.getName()).info(
            "Recorded cooldown violation for player " + player.getPlayerName() + " with perk " + perk.getIdentifier()
        );
    }
    
    */
/**
     * Records a perk purchase.
     *
     * @param player the player who purchased the perk
     * @param perk   the perk that was purchased
     * @param cost   the cost of the perk
     *//*

    public static void recordPerkPurchase(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final double cost
    ) {
        
        final RDQPlayerStatistic playerStatistic = player.getPlayerStatistic();
        if (playerStatistic == null) {
            return;
        }
        
        // Increment total perks purchased
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            EStatisticType.TOTAL_PERKS_PURCHASED.getKey(),
            PLUGIN_NAME,
            1.0
        );
        
        // Increment total perks owned
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            EStatisticType.TOTAL_PERKS_OWNED.getKey(),
            PLUGIN_NAME,
            1.0
        );
        
        // Add to total money spent on perks
        RDQPlayerStatisticService.incrementNumericStatistic(
            playerStatistic,
            EStatisticType.PERK_MONEY_SPENT.getKey(),
            PLUGIN_NAME,
            cost
        );
        
        CentralLogger.getLogger(RPerkStatisticService.class.getName()).info(
            "Recorded perk purchase for player " + player.getPlayerName() +
            " with perk " + perk.getIdentifier() + " for cost " + cost
        );
    }
    
    */
/**
     * Gets the activation count for a specific perk.
     *
     * @param playerStatistic the player statistic container
     * @param perkIdentifier  the perk identifier
     *
     * @return the activation count, or 0 if not found
     *//*

    public static double getPerkActivationCount(
        final @NotNull RDQPlayerStatistic playerStatistic,
        final @NotNull String perkIdentifier
    ) {
        
        final String perkCountKey = EStatisticType.getPerkActivationCountKey(perkIdentifier);
        return RDQPlayerStatisticService.getStatisticValue(
            playerStatistic,
            perkCountKey,
            PLUGIN_NAME,
            Double.class
        ).orElse(0.0);
    }
    
    */
/**
     * Gets the total usage time for a specific perk.
     *
     * @param playerStatistic the player statistic container
     * @param perkIdentifier  the perk identifier
     *
     * @return the total usage time in milliseconds, or 0 if not found
     *//*

    public static double getPerkUsageTime(
        final @NotNull RDQPlayerStatistic playerStatistic,
        final @NotNull String perkIdentifier
    ) {
        
        final String perkUsageTimeKey = EStatisticType.getPerkUsageTimeKey(perkIdentifier);
        return RDQPlayerStatisticService.getStatisticValue(
            playerStatistic,
            perkUsageTimeKey,
            PLUGIN_NAME,
            Double.class
        ).orElse(0.0);
    }
    
    */
/**
     * Gets the last used timestamp for a specific perk.
     *
     * @param playerStatistic the player statistic container
     * @param perkIdentifier  the perk identifier
     *
     * @return the last used timestamp, or null if not found
     *//*

    public static @Nullable Long getPerkLastUsed(
        final @NotNull RDQPlayerStatistic playerStatistic,
        final @NotNull String perkIdentifier
    ) {
        
        final String perkLastUsedKey = EStatisticType.getPerkLastUsedKey(perkIdentifier);
        return RDQPlayerStatisticService.getStatisticValue(
            playerStatistic,
            perkLastUsedKey,
            PLUGIN_NAME,
            Long.class
        ).orElse(null);
    }
    
    */
/**
     * Exports all perk statistics for a player.
     *
     * @param playerStatistic the player statistic container
     *
     * @return a map of perk statistic keys to their values
     *//*

    public static Map<String, Object> exportPerkStatistics(
        final @NotNull RDQPlayerStatistic playerStatistic
    ) {
        
        Map<String, Map<String, Object>> allStats = RDQPlayerStatisticService.exportStatistics(playerStatistic);
        return allStats.getOrDefault(
            PLUGIN_NAME,
            new HashMap<>()
        );
    }
    
    */
/**
     * Starts tracking usage time for a perk.
     *
     * @param playerId  the player ID
     * @param perkId    the perk identifier
     * @param startTime the start time
     *//*

    private static void startPerkUsageTracking(
        final @NotNull String playerId,
        final @NotNull String perkId,
        final long startTime
    ) {
        
        activePerkSessions.computeIfAbsent(
                              playerId,
                              k -> new ConcurrentHashMap<>()
                          )
                          .put(
                              perkId,
                              startTime
                          );
    }
    
    */
/**
     * Stops tracking usage time for a perk and returns the duration.
     *
     * @param playerId the player ID
     * @param perkId   the perk identifier
     *
     * @return the usage duration in milliseconds, or 0 if not tracked
     *//*

    private static long stopPerkUsageTracking(
        final @NotNull String playerId,
        final @NotNull String perkId
    ) {
        
        final Map<String, Long> playerSessions = activePerkSessions.get(playerId);
        if (
            playerSessions != null
        ) {
            final Long startTime = playerSessions.remove(perkId);
            if (startTime != null) {
                return System.currentTimeMillis() - startTime;
            }
        }
        return 0L;
    }
    
    */
/**
     * Updates the most used perk statistic.
     *
     * @param playerStatistic the player statistic container
     * @param perkId          the perk identifier that was just used
     *//*

    private static void updateMostUsedPerk(
        final @NotNull RDQPlayerStatistic playerStatistic,
        final @NotNull String perkId
    ) {
        
        final double currentCount = getPerkActivationCount(
            playerStatistic,
            perkId
        );
        
        // Get current most used perk
        final String currentMostUsed = RDQPlayerStatisticService.getStatisticValue(
            playerStatistic,
            EStatisticType.MOST_USED_PERK.getKey(),
            PLUGIN_NAME,
            String.class
        ).orElse("");
        
        // If no most used perk set, or this perk has more activations, update it
        if (
            currentMostUsed.isEmpty()
        ) {
            RDQPlayerStatisticService.addOrUpdateStatistic(
                playerStatistic,
                EStatisticType.MOST_USED_PERK.getKey(),
                PLUGIN_NAME,
                perkId
            );
        } else if (
                   ! currentMostUsed.equals(perkId)
        ) {
            final double mostUsedCount = getPerkActivationCount(
                playerStatistic,
                currentMostUsed
            );
            if (currentCount > mostUsedCount) {
                RDQPlayerStatisticService.addOrUpdateStatistic(
                    playerStatistic,
                    EStatisticType.MOST_USED_PERK.getKey(),
                    PLUGIN_NAME,
                    perkId
                );
            }
        }
    }
    
    */
/**
     * Cleans up tracking data for a player (call when player disconnects).
     *
     * @param playerId the player ID
     *//*

    public static void cleanupPlayerTracking(
        final @NotNull String playerId
    ) {
        
        activePerkSessions.remove(playerId);
    }
    
}*/
