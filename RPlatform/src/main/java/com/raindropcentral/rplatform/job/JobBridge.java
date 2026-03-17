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

package com.raindropcentral.rplatform.job;

import com.raindropcentral.rplatform.job.impl.EcoJobsJobBridge;
import com.raindropcentral.rplatform.job.impl.JobsRebornJobBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared bridge contract for jobs-plugin integrations.
 *
 * <p>The interface follows the same runtime-discovery style as {@code RProtectionBridge}, allowing
 * requirements to integrate with optional job plugins without direct dependencies.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public interface JobBridge {

    /**
     * Default bridge discovery order.
     */
    List<JobBridge> DEFAULT_BRIDGES = List.of(
        new EcoJobsJobBridge(),
        new JobsRebornJobBridge()
    );

    /**
     * Gets the normalized integration identifier used in configuration.
     *
     * @return lower-case integration identifier (for example {@code "jobsreborn"})
     */
    @NotNull String getIntegrationId();

    /**
     * Gets the plugin name used for runtime availability checks.
     *
     * @return external plugin name
     */
    @NotNull String getPluginName();

    /**
     * Checks whether the bridged plugin is installed, enabled, and reachable.
     *
     * @return {@code true} when bridge calls are available
     */
    boolean isAvailable();

    /**
     * Resolves a player's level for a specific job.
     *
     * @param player player to inspect
     * @param jobId job identifier to query
     * @return resolved level, or {@code 0} when unavailable
     * @throws NullPointerException if {@code player} or {@code jobId} is {@code null}
     */
    double getJobLevel(@NotNull Player player, @NotNull String jobId);

    /**
     * Resolves multiple job levels in one call.
     *
     * @param player player to inspect
     * @param jobIds job identifiers to resolve
     * @return map of job identifier to resolved level
     * @throws NullPointerException if {@code player} or {@code jobIds} is {@code null}
     */
    default @NotNull Map<String, Double> getJobLevels(@NotNull Player player, @NotNull String... jobIds) {
        final Map<String, Double> values = new LinkedHashMap<>();
        for (final String jobId : jobIds) {
            if (jobId == null || jobId.isBlank()) {
                continue;
            }
            values.put(jobId, getJobLevel(player, jobId));
        }
        return values;
    }

    /**
     * Consumes a job level amount from a player.
     *
     * <p>Most job plugins do not expose safe level-consumption APIs, so bridges should return
     * {@code false} unless an explicit write-path is available.</p>
     *
     * @param player player to modify
     * @param jobId job identifier to consume
     * @param amount amount of levels to consume
     * @return {@code true} when consumption succeeded
     * @throws NullPointerException if {@code player} or {@code jobId} is {@code null}
     */
    default boolean consumeJobLevel(@NotNull Player player, @NotNull String jobId, double amount) {
        return false;
    }

    /**
     * Detects the first available job bridge from the default list.
     *
     * @return first available bridge, or {@code null} when no supported plugin is available
     */
    static @Nullable JobBridge getBridge() {
        for (final JobBridge bridge : DEFAULT_BRIDGES) {
            if (bridge.isAvailable()) {
                return bridge;
            }
        }
        return null;
    }

    /**
     * Resolves a specific job bridge by integration identifier.
     *
     * @param integrationId integration identifier or alias
     * @return matching available bridge, or {@code null} when unavailable
     */
    static @Nullable JobBridge getBridge(@Nullable String integrationId) {
        if (integrationId == null || integrationId.isBlank()) {
            return null;
        }

        final String normalized = switch (integrationId.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> "auto";
            case "jobs", "job", "jobs_reborn", "jobs-reborn" -> "jobsreborn";
            case "eco_job", "eco-jobs", "ecojob" -> "ecojobs";
            default -> integrationId.trim().toLowerCase(Locale.ROOT);
        };

        if ("auto".equals(normalized)) {
            return getBridge();
        }

        for (final JobBridge bridge : DEFAULT_BRIDGES) {
            if (!bridge.isAvailable()) {
                continue;
            }

            final String bridgeId = bridge.getIntegrationId().toLowerCase(Locale.ROOT);
            final String pluginName = bridge.getPluginName().toLowerCase(Locale.ROOT);
            if (bridgeId.equals(normalized) || pluginName.equals(normalized)) {
                return bridge;
            }
        }
        return null;
    }

    /**
     * Returns the default bridge list in discovery order.
     *
     * @return immutable default bridge list
     */
    static @NotNull List<JobBridge> getDefaultBridges() {
        return DEFAULT_BRIDGES;
    }
}
