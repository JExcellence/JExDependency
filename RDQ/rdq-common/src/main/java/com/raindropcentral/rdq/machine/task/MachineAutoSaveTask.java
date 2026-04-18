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

package com.raindropcentral.rdq.machine.task;

import com.raindropcentral.rdq.machine.repository.MachineCache;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Periodic task for auto-saving dirty machine data to the database.
 *
 * <p>This task runs at configured intervals to persist machine state changes
 * to the database, providing crash protection and ensuring data consistency.
 * It operates asynchronously to avoid blocking the main server thread.
 *
 * <p>The task logs statistics about save operations, including the number of
 * machines saved and any errors encountered during the save process.
 *
 * <p>Auto-save is essential for:
 * <ul>
 *   <li>Preventing data loss in case of server crashes</li>
 *   <li>Ensuring machine state persists across server restarts</li>
 *   <li>Maintaining data consistency for active machines</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class MachineAutoSaveTask extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger(MachineAutoSaveTask.class.getName());

    private final MachineCache cache;
    private final boolean logStatistics;

    /**
     * Constructs a new auto-save task.
     *
     * @param cache          the machine cache to save from
     * @param logStatistics  whether to log save statistics
     */
    public MachineAutoSaveTask(
        final @NotNull MachineCache cache,
        final boolean logStatistics
    ) {
        this.cache = cache;
        this.logStatistics = logStatistics;
    }

    /**
     * Executes the auto-save operation.
     *
     * <p>This method is called periodically by the Bukkit scheduler. It triggers
     * an asynchronous save of all dirty machines and logs the results.
     */
    @Override
    public void run() {
        final long startTime = System.currentTimeMillis();
        final int dirtyCount = cache.getDirtyCount();

        if (dirtyCount == 0) {
            if (logStatistics) {
                LOGGER.fine("Auto-save: No dirty machines to save");
            }
            return;
        }

        // Perform auto-save asynchronously
        cache.autoSaveAll().thenAccept(savedCount -> {
            final long duration = System.currentTimeMillis() - startTime;

            if (logStatistics) {
                if (savedCount > 0) {
                    LOGGER.info(String.format(
                        "Auto-save completed: %d/%d machines saved in %dms",
                        savedCount,
                        dirtyCount,
                        duration
                    ));
                } else {
                    LOGGER.warning(String.format(
                        "Auto-save completed: 0/%d machines saved (all failed) in %dms",
                        dirtyCount,
                        duration
                    ));
                }
            }

            // Log cache statistics
            if (logStatistics) {
                logCacheStatistics();
            }
        }).exceptionally(throwable -> {
            LOGGER.severe("Auto-save failed with exception: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Logs cache statistics for monitoring purposes.
     */
    private void logCacheStatistics() {
        final int cacheSize = (int) cache.getCacheSize();
        final int dirtyCount = cache.getDirtyCount();
        final double dirtyPercentage = cacheSize > 0
            ? (dirtyCount * 100.0 / cacheSize)
            : 0.0;

        LOGGER.fine(String.format(
            "Cache statistics: %d machines cached, %d dirty (%.1f%%)",
            cacheSize,
            dirtyCount,
            dirtyPercentage
        ));
    }
}
