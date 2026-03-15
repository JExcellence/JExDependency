package com.raindropcentral.core.service.statistics.aggregation;

import com.raindropcentral.core.service.statistics.delivery.AggregatedStatistics;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Computes aggregate statistics from collected data.
 * Provides server-wide totals, percentiles, and rate calculations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsAggregator {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    // Cached statistics for aggregation
    private final Map<UUID, Map<String, Object>> playerStatistics;
    private final Map<String, List<Double>> statisticValues;

    /**
     * Executes StatisticsAggregator.
     */
    public StatisticsAggregator() {
        this.playerStatistics = new ConcurrentHashMap<>();
        this.statisticValues = new ConcurrentHashMap<>();
    }

    /**
     * Updates cached statistics from queued statistics.
     *
     * @param statistics the statistics to cache
     */
    public void updateCache(final @NotNull Collection<QueuedStatistic> statistics) {
        for (QueuedStatistic stat : statistics) {
            playerStatistics
                .computeIfAbsent(stat.playerUuid(), k -> new ConcurrentHashMap<>())
                .put(stat.statisticKey(), stat.value());

            if (stat.value() instanceof Number num) {
                statisticValues
                    .computeIfAbsent(stat.statisticKey(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(num.doubleValue());
            }
        }
    }

    /**
     * Computes server-wide aggregate statistics.
     *
     * @return the aggregated statistics
     */
    public AggregatedStatistics computeServerAggregates() {
        int totalPlayers = playerStatistics.size();
        double avgPlaytime = computeAveragePlaytime();
        double economyVolume = computeTotalEconomyVolume();
        int questCompletions = computeTotalQuestCompletions();

        Map<String, Object> customAggregates = new HashMap<>();
        customAggregates.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        customAggregates.put("totalBlocksBroken", computeSum("mc_total_blocks_broken"));
        customAggregates.put("totalMobKills", computeSum("mc_total_mob_kills"));
        customAggregates.put("totalDistanceTraveled", computeSum("mc_total_distance"));

        return AggregatedStatistics.builder()
            .timestamp(System.currentTimeMillis())
            .totalPlayersTracked(totalPlayers)
            .averagePlaytimeMs(avgPlaytime)
            .totalEconomyVolume(economyVolume)
            .totalQuestCompletions(questCompletions)
            .customAggregates(customAggregates)
            .build();
    }


    /**
     * Computes percentiles for a specific statistic.
     *
     * @param statisticKey the statistic key
     * @param percentiles  the percentiles to compute (e.g., 50, 90, 99)
     * @return map of percentile to value
     */
    public Map<Double, Double> computePercentiles(
        final @NotNull String statisticKey,
        final double... percentiles
    ) {
        List<Double> values = statisticValues.get(statisticKey);
        if (values == null || values.isEmpty()) {
            Map<Double, Double> empty = new HashMap<>();
            for (double p : percentiles) {
                empty.put(p, 0.0);
            }
            return empty;
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        Map<Double, Double> result = new HashMap<>();
        for (double percentile : percentiles) {
            int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            result.put(percentile, sorted.get(index));
        }

        return result;
    }

    /**
     * Computes rate statistics (per hour).
     *
     * @return map of rate names to values
     */
    public Map<String, Double> computeRates() {
        Map<String, Double> rates = new HashMap<>();

        // Compute kills per hour
        double totalKills = computeSum("mc_total_mob_kills");
        double totalPlaytimeHours = computeSum("mc_play_one_minute") / 60.0;
        if (totalPlaytimeHours > 0) {
            rates.put("killsPerHour", totalKills / totalPlaytimeHours);
        }

        // Compute blocks per session
        double totalBlocks = computeSum("mc_total_blocks_broken");
        int sessions = playerStatistics.size();
        if (sessions > 0) {
            rates.put("blocksPerSession", totalBlocks / sessions);
        }

        // Compute deaths per hour
        double totalDeaths = computeSum("mc_deaths");
        if (totalPlaytimeHours > 0) {
            rates.put("deathsPerHour", totalDeaths / totalPlaytimeHours);
        }

        return rates;
    }

    /**
     * Computes the sum of a numeric statistic across all players.
     */
    private double computeSum(final String statisticKey) {
        List<Double> values = statisticValues.get(statisticKey);
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Computes average playtime across all tracked players.
     */
    private double computeAveragePlaytime() {
        List<Double> playtimes = statisticValues.get("mc_play_one_minute");
        if (playtimes == null || playtimes.isEmpty()) {
            return 0.0;
        }
        return playtimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 60_000; // Convert to ms
    }

    /**
     * Computes total economy volume from economy-related statistics.
     */
    private double computeTotalEconomyVolume() {
        double volume = 0.0;
        for (String key : statisticValues.keySet()) {
            if (key.contains("economy") || key.contains("money") || key.contains("balance")) {
                volume += computeSum(key);
            }
        }
        return volume;
    }

    /**
     * Computes total quest completions.
     */
    private int computeTotalQuestCompletions() {
        List<Double> completions = statisticValues.get("quests_completed");
        if (completions == null || completions.isEmpty()) {
            return 0;
        }
        return (int) completions.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Gets statistics for a specific player.
     *
     * @param playerUuid the player UUID
     * @return the player's statistics or empty map
     */
    public Map<String, Object> getPlayerStatistics(final @NotNull UUID playerUuid) {
        return playerStatistics.getOrDefault(playerUuid, Map.of());
    }

    /**
     * Clears cached statistics for a player.
     *
     * @param playerUuid the player UUID
     */
    public void clearPlayerCache(final @NotNull UUID playerUuid) {
        playerStatistics.remove(playerUuid);
    }

    /**
     * Clears all cached statistics.
     */
    public void clearCache() {
        playerStatistics.clear();
        statisticValues.clear();
    }

    /**
     * Gets the number of tracked players.
     *
     * @return player count
     */
    public int getTrackedPlayerCount() {
        return playerStatistics.size();
    }

    /**
     * Gets all tracked statistic keys.
     *
     * @return set of statistic keys
     */
    public Set<String> getTrackedStatisticKeys() {
        return new HashSet<>(statisticValues.keySet());
    }
}
