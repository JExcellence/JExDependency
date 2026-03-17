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

package com.raindropcentral.rplatform.job.impl;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JobsReborn implementation of {@link com.raindropcentral.rplatform.job.JobBridge}.
 *
 * <p>The bridge uses reflection against JobsReborn runtime types to query job progression levels
 * without adding a hard API dependency.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public final class JobsRebornJobBridge extends AbstractReflectionJobBridge {

    private static final Logger LOGGER = Logger.getLogger(JobsRebornJobBridge.class.getName());
    private static final String INTEGRATION_ID = "jobsreborn";
    private static final String PLUGIN_NAME = "Jobs";
    private static final String[] JOBS_CLASS_NAMES = {
        "com.gamingmesh.jobs.Jobs"
    };

    private @Nullable Plugin plugin;
    private @Nullable Class<?> jobsClass;

    /**
     * Creates a JobsReborn bridge.
     */
    public JobsRebornJobBridge() {
    }

    /**
     * Gets integrationId.
     */
    @Override
    public @NotNull String getIntegrationId() {
        return INTEGRATION_ID;
    }

    /**
     * Gets pluginName.
     */
    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Returns whether available.
     */
    @Override
    public boolean isAvailable() {
        final Plugin installedPlugin = resolvePlugin(PLUGIN_NAME, "JobsReborn");
        if (installedPlugin == null || !installedPlugin.isEnabled()) {
            this.plugin = null;
            this.jobsClass = null;
            return false;
        }

        this.plugin = installedPlugin;
        if (this.jobsClass != null) {
            return true;
        }

        this.jobsClass = resolveJobsClass(installedPlugin);
        return this.jobsClass != null;
    }

    /**
     * Gets jobLevel.
     */
    @Override
    public double getJobLevel(@NotNull Player player, @NotNull String jobId) {
        if (!isAvailable() || jobId.isBlank()) {
            return 0.0D;
        }

        try {
            final Object jobsPlayer = resolveJobsPlayer(player);
            if (jobsPlayer == null) {
                return 0.0D;
            }

            final Object job = resolveJob(jobId.trim());
            Object level = firstNonNull(
                invokeOptional(jobsPlayer, "getJobLevel", job == null ? jobId.trim() : job),
                invokeOptional(jobsPlayer, "getJobLevel", jobId.trim())
            );
            if (asDouble(level) != null) {
                return asDouble(level);
            }

            final Object progression = firstNonNull(
                invokeOptional(jobsPlayer, "getJobProgression", job == null ? jobId.trim() : job),
                invokeOptional(jobsPlayer, "getJobProgression", jobId.trim())
            );
            final Double activeProgressionLevel = resolveProgressionLevel(progression);
            if (activeProgressionLevel != null) {
                return activeProgressionLevel;
            }

            final Object archivedProgression = firstNonNull(
                invokeOptional(jobsPlayer, "getArchivedJobProgression", job == null ? jobId.trim() : job),
                invokeOptional(jobsPlayer, "getArchivedJobProgression", jobId.trim())
            );
            final Double archivedProgressionLevel = resolveProgressionLevel(archivedProgression);
            if (archivedProgressionLevel != null) {
                return archivedProgressionLevel;
            }

            final Object progressionMap = firstNonNull(
                invokeOptional(jobsPlayer, "getJobProgression"),
                invokeOptional(jobsPlayer, "getJobs"),
                readFieldOptional(jobsPlayer, "jobProgression")
            );
            final Object archivedProgressionMap = firstNonNull(
                invokeOptional(jobsPlayer, "getArchivedJobProgression"),
                invokeOptional(jobsPlayer, "getArchivedJobs"),
                readFieldOptional(jobsPlayer, "archivedJobProgression"),
                readFieldOptional(jobsPlayer, "archivedJobs")
            );

            final Double mappedLevel = resolveMappedProgressionLevel(progressionMap, jobId.trim());
            if (mappedLevel != null) {
                return mappedLevel;
            }

            final Double archivedMappedLevel = resolveMappedProgressionLevel(archivedProgressionMap, jobId.trim());
            return archivedMappedLevel == null ? 0.0D : archivedMappedLevel;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve JobsReborn level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    private @Nullable Class<?> resolveJobsClass(final @NotNull Plugin installedPlugin) {
        for (final String className : JOBS_CLASS_NAMES) {
            final Class<?> resolvedClass = loadClass(installedPlugin, className);
            if (resolvedClass != null) {
                return resolvedClass;
            }
        }
        return null;
    }

    private @Nullable Object resolveJobsPlayer(final @NotNull Player player) {
        final Class<?> resolvedJobsClass = this.jobsClass;
        if (resolvedJobsClass == null) {
            return null;
        }

        final Object playerManager = invokeStaticOptional(resolvedJobsClass, "getPlayerManager");
        if (playerManager == null) {
            return null;
        }

        final Object playerInfo = firstNonNull(
            invokeOptional(playerManager, "getPlayerInfo", player),
            invokeOptional(playerManager, "getPlayerInfo", player.getUniqueId()),
            invokeOptional(playerManager, "getPlayerInfo", player.getName())
        );
        final Object jobsPlayerFromInfo = playerInfo == null
            ? null
            : firstNonNull(
                invokeOptional(playerInfo, "getJobsPlayer"),
                invokeOptional(playerInfo, "getPlayer"),
                readFieldOptional(playerInfo, "jobsPlayer")
            );

        return firstNonNull(
            invokeOptional(playerManager, "getJobsPlayer", player),
            invokeOptional(playerManager, "getJobsPlayer", player.getUniqueId()),
            invokeOptional(playerManager, "getJobsPlayer", player.getName()),
            invokeOptional(playerManager, "getPlayer", player),
            invokeOptional(playerManager, "getPlayer", player.getUniqueId()),
            jobsPlayerFromInfo
        );
    }

    private @Nullable Object resolveJob(final @NotNull String jobId) {
        final Class<?> resolvedJobsClass = this.jobsClass;
        if (resolvedJobsClass == null) {
            return null;
        }

        Object job = firstNonNull(
            invokeStaticOptional(resolvedJobsClass, "getJob", jobId),
            invokeStaticOptional(resolvedJobsClass, "getJob", toTitleCase(jobId)),
            invokeStaticOptional(resolvedJobsClass, "getByName", jobId),
            invokeStaticOptional(resolvedJobsClass, "getByName", toTitleCase(jobId))
        );
        if (job != null) {
            return job;
        }

        final Object jobs = invokeStaticOptional(resolvedJobsClass, "getJobs");
        if (jobs instanceof Iterable<?> iterableJobs) {
            return resolveNamedEntry(iterableJobs, jobId);
        }
        return null;
    }

    private @Nullable Double resolveProgressionLevel(
            final @Nullable Object progression
    ) {
        if (progression == null) {
            return null;
        }

        return asDouble(firstNonNull(
            invokeOptional(progression, "getLevel"),
            invokeOptional(progression, "getCurrentLevel"),
            invokeOptional(progression, "getCurrentLvl"),
            invokeOptional(progression, "getLvl")
        ));
    }

    private @Nullable Double resolveMappedProgressionLevel(
        final @Nullable Object progressionMap,
        final @NotNull String jobId
    ) {
        if (!(progressionMap instanceof Map<?, ?> map)) {
            return null;
        }

        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Object key = entry.getKey();
            if (key == null) {
                continue;
            }

            final String keyName = firstNonNull(
                invokeOptional(key, "getName"),
                invokeOptional(key, "getJobName"),
                key
            ).toString();
            if (!normalizeLookupKey(keyName).equals(normalizeLookupKey(jobId))) {
                continue;
            }

            final Object value = entry.getValue();
            final Double directLevel = asDouble(value);
            if (directLevel != null) {
                return directLevel;
            }

            final Double progressionLevel = asDouble(firstNonNull(
                invokeOptional(value, "getLevel"),
                invokeOptional(value, "getCurrentLevel"),
                invokeOptional(value, "getCurrentLvl"),
                invokeOptional(value, "getLvl")
            ));
            if (progressionLevel != null) {
                return progressionLevel;
            }
        }
        return null;
    }

    private @NotNull String toTitleCase(final @NotNull String value) {
        if (value.isBlank()) {
            return value;
        }

        final String[] words = value.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase());
            }
        }

        return builder.length() == 0 ? value : builder.toString();
    }
}
