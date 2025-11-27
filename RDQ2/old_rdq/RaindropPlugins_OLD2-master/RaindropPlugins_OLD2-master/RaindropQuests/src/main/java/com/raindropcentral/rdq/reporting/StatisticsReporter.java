package com.raindropcentral.rdq.reporting;

import com.raindropcentral.rdq.RDQImpl;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the collection and asynchronous reporting of statistics for the RaindropQuests plugin.
 * <p>
 * This class is responsible for scheduling and executing metrics collection tasks,
 * which can be extended to report plugin usage, player activity, or other analytics.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class StatisticsReporter {

    /**
     * The main RaindropQuests plugin instance.
     */
    private final RDQImpl rdq;

    /**
     * Constructs a new {@code StatisticsReporter} for the given plugin instance.
     *
     * @param rdq The main RaindropQuests plugin instance
     */
    public StatisticsReporter(
            final @NotNull RDQImpl rdq
    ) {
        this.rdq = rdq;
    }

    /**
     * Starts the asynchronous reporting of statistics.
     * <p>
     * Schedules the {@link #collectAndReportMetrics()} method to run asynchronously using Bukkit's scheduler.
     * </p>
     */
    public void startReporting() {
        Bukkit.getScheduler().runTaskAsynchronously(this.rdq.getImpl(),
                this::collectAndReportMetrics
        );
    }

    /**
     * Collects and reports plugin metrics.
     * <p>
     * This method should be extended to gather and send relevant statistics.
     * Currently, it is a placeholder for future implementation.
     * </p>
     */
    private void collectAndReportMetrics() {

    }
}
