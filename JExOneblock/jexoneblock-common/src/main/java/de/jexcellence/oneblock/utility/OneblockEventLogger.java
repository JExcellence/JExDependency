package de.jexcellence.oneblock.utility;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockCore;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OneblockEventLogger {

    private static final Logger LOGGER = Logger.getLogger(OneblockEventLogger.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean enableDebugLogging;
    private final boolean enableStatisticsLogging;
    private final boolean enablePerformanceLogging;

    public OneblockEventLogger() {
        this(false, true, false);
    }

    public OneblockEventLogger(boolean enableDebugLogging, boolean enableStatisticsLogging, boolean enablePerformanceLogging) {
        this.enableDebugLogging = enableDebugLogging;
        this.enableStatisticsLogging = enableStatisticsLogging;
        this.enablePerformanceLogging = enablePerformanceLogging;
    }

    public void logBlockBreak(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            @NotNull Material originalBlock,
            @NotNull Material newBlock,
            @NotNull EEvolutionRarityType rarity,
            double experienceGained,
            @NotNull Location location
    ) {
        if (!enableStatisticsLogging) return;

        var message = String.format(
            "[BLOCK_BREAK] Player: %s | Island: %s | Evolution: %s L%d | Block: %s -> %s | Rarity: %s | XP: %.2f | Total Blocks: %d | Streak: %d | Location: %s",
            player.getName(),
            island.getIdentifier(),
            core.getCurrentEvolution(),
            core.getEvolutionLevel(),
            originalBlock.name(),
            newBlock.name(),
            rarity.getDisplayName(),
            experienceGained,
            core.getTotalBlocksBroken(),
            core.getBreakStreak(),
            formatLocation(location)
        );

        LOGGER.info(message);
    }

    /**
     * Logs generator result events (deprecated - kept for compatibility).
     */
    @Deprecated
    public void logGeneratorResult(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull String resultMessage
    ) {
        if (!enableStatisticsLogging) return;

        var message = String.format(
            "[GENERATOR] Player: %s | Island: %s | Message: %s",
            player.getName(),
            island.getIdentifier(),
            resultMessage
        );

        LOGGER.info(message);
    }
    public void logEvolutionLevelUp(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            int previousLevel,
            int newLevel
    ) {
        var message = String.format(
            "[EVOLUTION_LEVEL_UP] Player: %s | Island: %s | Evolution: %s | Level: %d -> %d | Total XP: %.2f | Total Blocks: %d | Prestige: %d",
            player.getName(),
            island.getIdentifier(),
            core.getCurrentEvolution(),
            previousLevel,
            newLevel,
            core.getEvolutionExperience(),
            core.getTotalBlocksBroken(),
            core.getPrestigeLevel()
        );

        LOGGER.info(message);
    }

    public void logEvolutionChange(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull String previousEvolution,
            @NotNull String newEvolution,
            int evolutionLevel
    ) {
        var message = String.format(
            "[EVOLUTION_CHANGE] Player: %s | Island: %s | Evolution: %s -> %s | Level: %d | Timestamp: %s",
            player.getName(),
            island.getIdentifier(),
            previousEvolution,
            newEvolution,
            evolutionLevel,
            getCurrentTimestamp()
        );

        LOGGER.info(message);
    }

    public void logPrestige(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            int previousPrestigeLevel,
            int newPrestigeLevel,
            long prestigePointsGained
    ) {
        var message = String.format(
            "[PRESTIGE] Player: %s | Island: %s | Prestige: %d -> %d | Points Gained: %d | Total Points: %d | Total Blocks: %d | Evolution Reset: %s L1",
            player.getName(),
            island.getIdentifier(),
            previousPrestigeLevel,
            newPrestigeLevel,
            prestigePointsGained,
            core.getPrestigePoints(),
            core.getTotalBlocksBroken(),
            core.getCurrentEvolution()
        );

        LOGGER.info(message);
    }

    public void logRareBreak(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull EEvolutionRarityType rarity,
            @NotNull Material block,
            @NotNull Location location
    ) {
        if (rarity.getTier() < EEvolutionRarityType.LEGENDARY.getTier()) return;

        var message = String.format(
            "[RARE_BREAK] Player: %s | Island: %s | Rarity: %s (Tier %d) | Block: %s | Location: %s | Timestamp: %s",
            player.getName(),
            island.getIdentifier(),
            rarity.getFormattedName(),
            rarity.getTier(),
            block.name(),
            formatLocation(location),
            getCurrentTimestamp()
        );

        LOGGER.warning(message);
    }

    /**
     * Logs item spawn events.
     */
    public void logItemSpawn(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull List<ItemStack> items,
            @NotNull EEvolutionRarityType rarity,
            @NotNull Location location,
            boolean isChestSpawn
    ) {
        if (!enableStatisticsLogging) return;

        String itemList = items.stream()
            .map(item -> item.getType().name() + "x" + item.getAmount())
            .reduce((a, b) -> a + ", " + b)
            .orElse("NONE");

        String message = String.format(
            "[ITEM_SPAWN] Player: %s | Island: %s | Type: %s | Rarity: %s | Items: [%s] | Location: %s",
            player.getName(),
            island.getIdentifier(),
            isChestSpawn ? "CHEST" : "DROP",
            rarity.getDisplayName(),
            itemList,
            formatLocation(location)
        );

        LOGGER.info(message);
    }

    /**
     * Logs entity spawn events.
     */
    public void logEntitySpawn(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull EntityType entityType,
            @NotNull EEvolutionRarityType rarity,
            @NotNull Location location
    ) {
        if (!enableStatisticsLogging) return;

        String message = String.format(
            "[ENTITY_SPAWN] Player: %s | Island: %s | Entity: %s | Rarity: %s | Location: %s",
            player.getName(),
            island.getIdentifier(),
            entityType.name(),
            rarity.getDisplayName(),
            formatLocation(location)
        );

        LOGGER.info(message);
    }

    /**
     * Logs special event triggers.
     */
    public void logSpecialEvent(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull String eventType,
            @NotNull String eventDetails
    ) {
        String message = String.format(
            "[SPECIAL_EVENT] Player: %s | Island: %s | Event: %s | Details: %s | Timestamp: %s",
            player.getName(),
            island.getIdentifier(),
            eventType,
            eventDetails,
            getCurrentTimestamp()
        );

        LOGGER.info(message);
    }

    // ==================== PROGRESSION EVENTS ====================

    /**
     * Logs progression preview events.
     */
    public void logProgressionPreview(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull EEvolutionRarityType currentRarity,
            @NotNull EEvolutionRarityType previewRarity
    ) {
        if (!enableDebugLogging) return;

        String message = String.format(
            "[PROGRESSION_PREVIEW] Player: %s | Island: %s | Current: %s | Preview: %s",
            player.getName(),
            island.getIdentifier(),
            currentRarity.getDisplayName(),
            previewRarity.getDisplayName()
        );

        LOGGER.fine(message);
    }

    /**
     * Logs streak events.
     */
    public void logStreakEvent(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            @NotNull String streakType
    ) {
        if (!enableStatisticsLogging) return;

        String message = String.format(
            "[STREAK] Player: %s | Island: %s | Type: %s | Current Streak: %d | Max Streak: %d | Evolution: %s L%d",
            player.getName(),
            island.getIdentifier(),
            streakType,
            core.getBreakStreak(),
            core.getMaxBreakStreak(),
            core.getCurrentEvolution(),
            core.getEvolutionLevel()
        );

        LOGGER.info(message);
    }

    // ==================== PERFORMANCE EVENTS ====================

    /**
     * Logs performance metrics for operations.
     */
    public void logPerformanceMetric(
            @NotNull String operation,
            long executionTimeMs,
            @Nullable String additionalInfo
    ) {
        if (!enablePerformanceLogging) return;

        String message = String.format(
            "[PERFORMANCE] Operation: %s | Time: %dms | Info: %s",
            operation,
            executionTimeMs,
            additionalInfo != null ? additionalInfo : "N/A"
        );

        if (executionTimeMs > 100) {
            LOGGER.warning(message + " [SLOW]");
        } else {
            LOGGER.fine(message);
        }
    }

    /**
     * Logs async operation completion.
     */
    public void logAsyncOperation(
            @NotNull String operation,
            boolean success,
            long executionTimeMs,
            @Nullable String errorMessage
    ) {
        if (!enablePerformanceLogging) return;

        String message = String.format(
            "[ASYNC] Operation: %s | Success: %s | Time: %dms | Error: %s",
            operation,
            success,
            executionTimeMs,
            errorMessage != null ? errorMessage : "NONE"
        );

        if (!success) {
            LOGGER.severe(message);
        } else if (executionTimeMs > 500) {
            LOGGER.warning(message + " [SLOW_ASYNC]");
        } else {
            LOGGER.fine(message);
        }
    }

    // ==================== ERROR EVENTS ====================

    /**
     * Logs error events with context.
     */
    public void logError(
            @NotNull String operation,
            @NotNull Throwable error,
            @Nullable Player player,
            @Nullable OneblockIsland island
    ) {
        String playerInfo = player != null ? player.getName() : "UNKNOWN";
        String islandInfo = island != null ? island.getIdentifier() : "UNKNOWN";

        String message = String.format(
            "[ERROR] Operation: %s | Player: %s | Island: %s | Error: %s | Message: %s",
            operation,
            playerInfo,
            islandInfo,
            error.getClass().getSimpleName(),
            error.getMessage()
        );

        LOGGER.log(Level.SEVERE, message, error);
    }

    /**
     * Logs warning events.
     */
    public void logWarning(
            @NotNull String operation,
            @NotNull String warning,
            @Nullable Player player,
            @Nullable OneblockIsland island
    ) {
        String playerInfo = player != null ? player.getName() : "UNKNOWN";
        String islandInfo = island != null ? island.getIdentifier() : "UNKNOWN";

        String message = String.format(
            "[WARNING] Operation: %s | Player: %s | Island: %s | Warning: %s",
            operation,
            playerInfo,
            islandInfo,
            warning
        );

        LOGGER.warning(message);
    }

    // ==================== STATISTICS EVENTS ====================

    /**
     * Logs daily statistics summary.
     */
    public void logDailyStatistics(
            int totalBreaks,
            int uniquePlayers,
            int evolutionAdvances,
            int prestigeEvents,
            @NotNull EEvolutionRarityType highestRarity
    ) {
        if (!enableStatisticsLogging) return;

        String message = String.format(
            "[DAILY_STATS] Total Breaks: %d | Unique Players: %d | Evolution Advances: %d | Prestige Events: %d | Highest Rarity: %s | Date: %s",
            totalBreaks,
            uniquePlayers,
            evolutionAdvances,
            prestigeEvents,
            highestRarity.getDisplayName(),
            getCurrentTimestamp().split(" ")[0] // Just the date part
        );

        LOGGER.info(message);
    }

    /**
     * Logs player session summary.
     */
    public void logPlayerSession(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            int blocksBreken,
            double experienceGained,
            int evolutionLevelsGained,
            long sessionDurationMs
    ) {
        if (!enableStatisticsLogging) return;

        String message = String.format(
            "[SESSION] Player: %s | Island: %s | Blocks: %d | XP: %.2f | Levels: %d | Duration: %dms",
            player.getName(),
            island.getIdentifier(),
            blocksBreken,
            experienceGained,
            evolutionLevelsGained,
            sessionDurationMs
        );

        LOGGER.info(message);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Formats a location for logging.
     */
    @NotNull
    private String formatLocation(@NotNull Location location) {
        return String.format(
            "%s[%d,%d,%d]",
            location.getWorld() != null ? location.getWorld().getName() : "UNKNOWN",
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    /**
     * Gets the current timestamp as a formatted string.
     */
    @NotNull
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * Checks if debug logging is enabled.
     */
    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }

    /**
     * Checks if statistics logging is enabled.
     */
    public boolean isStatisticsLoggingEnabled() {
        return enableStatisticsLogging;
    }

    /**
     * Checks if performance logging is enabled.
     */
    public boolean isPerformanceLoggingEnabled() {
        return enablePerformanceLogging;
    }

    /**
     * Creates a performance timer for measuring operation duration.
     */
    @NotNull
    public PerformanceTimer createPerformanceTimer(@NotNull String operation) {
        return new PerformanceTimer(operation, this);
    }

    /**
     * Simple performance timer utility class.
     */
    public static class PerformanceTimer {
        private final String operation;
        private final OneblockEventLogger logger;
        private final long startTime;

        public PerformanceTimer(@NotNull String operation, @NotNull OneblockEventLogger logger) {
            this.operation = operation;
            this.logger = logger;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * Stops the timer and logs the performance metric.
         */
        public void stop() {
            stop(null);
        }

        /**
         * Stops the timer and logs the performance metric with additional info.
         */
        public void stop(@Nullable String additionalInfo) {
            long duration = System.currentTimeMillis() - startTime;
            logger.logPerformanceMetric(operation, duration, additionalInfo);
        }

        /**
         * Gets the elapsed time without stopping the timer.
         */
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }
}