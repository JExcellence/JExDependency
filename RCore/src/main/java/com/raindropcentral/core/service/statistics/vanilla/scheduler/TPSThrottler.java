/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.statistics.vanilla.scheduler;

import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Manages TPS-based throttling for vanilla statistics collection.
 *
 * <p>This throttler monitors server TPS (ticks per second) and adjusts collection
 * behavior to minimize performance impact during server load:
 * <ul>
 *   <li>Pauses collection when TPS drops below pause threshold (default: 15.0)</li>
 *   <li>Reduces collection frequency when TPS drops below reduce threshold (default: 18.0)</li>
 *   <li>Resumes normal collection when TPS recovers above 19.0 for at least 30 seconds</li>
 * </ul>
 *
 * <p>The throttler uses Paper's TPS API to access real-time server performance metrics.
 * TPS values are averaged over 1 minute for stability.
 *
 * <h2>Throttling States</h2>
 * <ul>
 *   <li><b>NORMAL</b>: TPS &gt;= 19.0, full collection frequency</li>
 *   <li><b>REDUCED</b>: 15.0 &lt;= TPS &lt; 18.0, 50% collection frequency</li>
 *   <li><b>PAUSED</b>: TPS &lt; 15.0, no collection</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TPSThrottler throttler = new TPSThrottler(config);
 * 
 * // Check before collection
 * if (throttler.shouldCollect()) {
 *     performCollection();
 * }
 * 
 * // Get current state
 * ThrottleState state = throttler.getCurrentState();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class TPSThrottler {

    private static final Logger LOGGER = Logger.getLogger(TPSThrottler.class.getName());
    
    /**
     * Minimum duration (in seconds) that TPS must remain above recovery threshold
     * before resuming normal collection.
     */
    private static final int RECOVERY_DURATION_SECONDS = 30;
    
    /**
     * TPS threshold above which normal collection resumes after throttling.
     */
    private static final double RECOVERY_THRESHOLD = 19.0;

    private final VanillaStatisticConfig config;
    private final Server server;

    private ThrottleState currentState;
    private Instant lastStateChange;
    private Instant recoveryStartTime;

    /**
     * Creates a new TPS throttler.
     *
     * @param config the vanilla statistic configuration
     */
    public TPSThrottler(final @NotNull VanillaStatisticConfig config) {
        this.config = config;
        this.server = Bukkit.getServer();
        this.currentState = ThrottleState.NORMAL;
        this.lastStateChange = Instant.now();
        this.recoveryStartTime = null;
    }

    /**
     * Determines whether collection should proceed based on current TPS.
     *
     * <p>This method checks the current server TPS and applies throttling rules:
     * <ul>
     *   <li>Returns false if TPS is below pause threshold</li>
     *   <li>Returns true/false randomly (50% chance) if TPS is below reduce threshold</li>
     *   <li>Returns true if TPS is above reduce threshold</li>
     * </ul>
     *
     * <p>State transitions are logged for monitoring purposes.
     *
     * @return true if collection should proceed, false if it should be skipped
     */
    public boolean shouldCollect() {
        if (!config.isTpsThrottlingEnabled()) {
            return true;
        }

        double currentTps = getCurrentTps();
        ThrottleState newState = determineState(currentTps);

        // Handle state transitions
        if (newState != currentState) {
            handleStateTransition(currentState, newState, currentTps);
            currentState = newState;
            lastStateChange = Instant.now();
        }

        // Apply throttling based on state
        return switch (currentState) {
            case PAUSED -> false;
            case REDUCED -> ThreadLocalRandom.current().nextBoolean(); // 50% chance
            case NORMAL -> true;
        };
    }

    /**
     * Determines the appropriate throttle state based on current TPS.
     *
     * @param tps the current TPS value
     * @return the appropriate throttle state
     */
    private @NotNull ThrottleState determineState(double tps) {
        double pauseThreshold = config.getTpsPauseThreshold();
        double reduceThreshold = config.getTpsReduceThreshold();

        // Check for pause condition
        if (tps < pauseThreshold) {
            recoveryStartTime = null;
            return ThrottleState.PAUSED;
        }

        // Check for reduce condition
        if (tps < reduceThreshold) {
            recoveryStartTime = null;
            return ThrottleState.REDUCED;
        }

        // Check for recovery condition
        if (tps >= RECOVERY_THRESHOLD) {
            if (currentState == ThrottleState.NORMAL) {
                return ThrottleState.NORMAL;
            }

            // Start recovery timer if not already started
            if (recoveryStartTime == null) {
                recoveryStartTime = Instant.now();
                return currentState; // Stay in current state
            }

            // Check if recovery duration has elapsed
            long secondsSinceRecovery = Instant.now().getEpochSecond() - 
                                       recoveryStartTime.getEpochSecond();
            
            if (secondsSinceRecovery >= RECOVERY_DURATION_SECONDS) {
                recoveryStartTime = null;
                return ThrottleState.NORMAL;
            }
        }

        // Maintain current state if no clear transition
        return currentState;
    }

    /**
     * Handles state transitions and logs appropriate messages.
     *
     * @param oldState the previous state
     * @param newState the new state
     * @param tps      the current TPS value
     */
    private void handleStateTransition(
        final @NotNull ThrottleState oldState,
        final @NotNull ThrottleState newState,
        double tps
    ) {
        String message = String.format(
            "TPS throttle state changed: %s -> %s (TPS: %.2f)",
            oldState, newState, tps
        );

        switch (newState) {
            case PAUSED -> LOGGER.warning(message + " - Collection PAUSED");
            case REDUCED -> LOGGER.warning(message + " - Collection REDUCED to 50%");
            case NORMAL -> LOGGER.info(message + " - Collection RESUMED");
        }
    }

    /**
     * Gets the current server TPS.
     *
     * <p>Uses Paper's TPS API to retrieve the 1-minute average TPS.
     * Falls back to 20.0 if TPS data is unavailable.
     *
     * @return the current TPS (1-minute average)
     */
    private double getCurrentTps() {
        try {
            // Paper API: getTPS() returns [1min, 5min, 15min]
            double[] tpsArray = server.getTPS();
            if (tpsArray != null && tpsArray.length > 0) {
                return tpsArray[0]; // 1-minute average
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to retrieve TPS: " + e.getMessage());
        }

        // Fallback to perfect TPS if unavailable
        return 20.0;
    }

    /**
     * Gets the current throttle state.
     *
     * @return the current state
     */
    public @NotNull ThrottleState getCurrentState() {
        return currentState;
    }

    /**
     * Gets the time of the last state change.
     *
     * @return the last state change time
     */
    public @NotNull Instant getLastStateChange() {
        return lastStateChange;
    }

    /**
     * Gets the current server TPS for monitoring purposes.
     *
     * @return the current TPS (1-minute average)
     */
    public double getTps() {
        return getCurrentTps();
    }

    /**
     * Checks if the throttler is currently in recovery mode.
     *
     * <p>Recovery mode means TPS is above the recovery threshold but
     * the required recovery duration has not yet elapsed.
     *
     * @return true if in recovery mode
     */
    public boolean isInRecovery() {
        return recoveryStartTime != null;
    }

    /**
     * Gets the remaining recovery time in seconds.
     *
     * @return remaining seconds until recovery completes, or 0 if not in recovery
     */
    public long getRemainingRecoverySeconds() {
        if (recoveryStartTime == null) {
            return 0;
        }

        long elapsed = Instant.now().getEpochSecond() - recoveryStartTime.getEpochSecond();
        long remaining = RECOVERY_DURATION_SECONDS - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Forces a state reset to NORMAL.
     *
     * <p>This method is primarily for testing and administrative commands.
     * Use with caution as it bypasses normal throttling logic.
     */
    public void forceReset() {
        currentState = ThrottleState.NORMAL;
        lastStateChange = Instant.now();
        recoveryStartTime = null;
        LOGGER.info("TPS throttler forcibly reset to NORMAL state");
    }

    /**
     * Represents the current throttling state.
     */
    public enum ThrottleState {
        /**
         * Normal operation - no throttling applied.
         * TPS &gt;= 19.0
         */
        NORMAL,

        /**
         * Reduced frequency - collection occurs at 50% rate.
         * 15.0 &lt;= TPS &lt; 18.0
         */
        REDUCED,

        /**
         * Paused - no collection occurs.
         * TPS &lt; 15.0
         */
        PAUSED
    }
}
