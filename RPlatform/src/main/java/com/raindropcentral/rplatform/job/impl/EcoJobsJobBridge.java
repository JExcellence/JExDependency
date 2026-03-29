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

import com.raindropcentral.rplatform.job.JobBridge;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EcoJobs implementation of {@link com.raindropcentral.rplatform.job.JobBridge}.
 *
 * <p>The bridge reflects EcoJobs API entrypoints at runtime to read job levels without requiring a
 * compile-time dependency on the plugin API.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public final class EcoJobsJobBridge extends AbstractReflectionJobBridge {

    private static final Logger LOGGER = Logger.getLogger(EcoJobsJobBridge.class.getName());
    private static final String INTEGRATION_ID = "ecojobs";
    private static final String PLUGIN_NAME = "EcoJobs";
    private static final String[] API_CLASS_NAMES = {
        "com.willfp.ecojobs.api.EcoJobsAPI",
        "com.willfp.ecojobs.api.EcoJobsApi",
        "com.willfp.ecojobs.api.EcoJobs"
    };
    private static final String[] JOB_CLASS_NAMES = {
        "com.willfp.ecojobs.jobs.Jobs",
        "com.willfp.ecojobs.api.jobs.Jobs",
        "com.willfp.ecojobs.jobs.Job"
    };

    private @Nullable Plugin plugin;
    private @Nullable Object api;

    /**
     * Creates an EcoJobs bridge.
     */
    public EcoJobsJobBridge() {
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
        final Plugin installedPlugin = resolvePlugin(PLUGIN_NAME, "EcoJobs");
        if (installedPlugin == null || !installedPlugin.isEnabled()) {
            this.plugin = null;
            this.api = null;
            return false;
        }

        this.plugin = installedPlugin;
        if (this.api != null) {
            return true;
        }

        this.api = resolveApi(installedPlugin);
        return this.api != null;
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
            final Object rawLevel = resolveJobLevel(player, jobId.trim());
            final Double numericLevel = asDouble(rawLevel);
            return numericLevel == null ? 0.0D : numericLevel;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve EcoJobs level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    @Override
    public @NotNull List<JobBridge.JobDescriptor> getAvailableJobs(@NotNull Player player) {
        if (!isAvailable()) {
            return List.of();
        }

        final List<Object> jobs = this.resolveAllJobObjects();
        if (jobs.isEmpty()) {
            return List.of();
        }

        final List<JobBridge.JobDescriptor> descriptors = new ArrayList<>();
        for (final Object job : jobs) {
            final String id = resolveJobIdentifier(job);
            final String displayName = resolveJobDisplayName(job);
            if (id.isBlank() || displayName.isBlank()) {
                continue;
            }

            descriptors.add(new JobBridge.JobDescriptor(
                    this.getIntegrationId(),
                    this.getPluginName(),
                    id,
                    displayName
            ));
        }

        return descriptors.stream()
                .sorted(Comparator.comparing(JobBridge.JobDescriptor::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public boolean addJobLevels(@NotNull Player player, @NotNull String jobId, int amount) {
        if (!isAvailable() || jobId.isBlank() || amount <= 0) {
            return false;
        }

        try {
            final Object resolvedApi = this.api;
            final Object jobObject = resolveJobObject(jobId.trim());
            if (resolvedApi == null || jobObject == null) {
                return false;
            }

            final int currentLevel = Math.max((int) Math.floor(getJobLevel(player, jobId.trim())), 0);
            final int targetLevel = currentLevel + amount;
            invokeOptional(resolvedApi, "setJobLevel", player, jobObject, targetLevel);
            invokeOptional(resolvedApi, "setLevel", player, jobObject, targetLevel);
            invokeOptional(resolvedApi, "setJobLevel", player.getUniqueId(), jobObject, targetLevel);
            invokeOptional(resolvedApi, "setLevel", player.getUniqueId(), jobObject, targetLevel);

            return getJobLevel(player, jobId.trim()) >= targetLevel;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to add EcoJobs levels for " + player.getName(), exception);
            return false;
        }
    }

    private @Nullable Object resolveApi(final @NotNull Plugin installedPlugin) {
        for (final String className : API_CLASS_NAMES) {
            final Class<?> apiClass = loadClass(installedPlugin, className);
            if (apiClass == null) {
                continue;
            }

            final Object resolvedApi = firstNonNull(
                invokeStaticOptional(apiClass, "getInstance"),
                invokeStaticOptional(apiClass, "getAPI"),
                invokeStaticOptional(apiClass, "get")
            );
            if (resolvedApi != null) {
                return resolvedApi;
            }
        }

        return firstNonNull(
            invokeOptional(installedPlugin, "getApi"),
            invokeOptional(installedPlugin, "getAPI"),
            readFieldOptional(installedPlugin, "api")
        );
    }

    private @Nullable Object resolveJobLevel(final @NotNull Player player, final @NotNull String jobId) {
        final Object resolvedApi = this.api;
        if (resolvedApi == null) {
            return null;
        }

        Object level = firstNonNull(
            invokeOptional(resolvedApi, "getJobLevel", player, jobId),
            invokeOptional(resolvedApi, "getLevel", player, jobId),
            invokeOptional(resolvedApi, "getPlayerJobLevel", player, jobId),
            invokeOptional(resolvedApi, "getJobLevel", player.getUniqueId(), jobId),
            invokeOptional(resolvedApi, "getLevel", player.getUniqueId(), jobId)
        );
        if (asDouble(level) != null) {
            return level;
        }

        final Object jobObject = resolveJobObject(jobId);
        if (jobObject != null) {
            level = firstNonNull(
                invokeOptional(resolvedApi, "getJobLevel", player, jobObject),
                invokeOptional(resolvedApi, "getLevel", player, jobObject),
                invokeOptional(resolvedApi, "getPlayerJobLevel", player, jobObject),
                invokeOptional(resolvedApi, "getJobLevel", player.getUniqueId(), jobObject),
                invokeOptional(resolvedApi, "getLevel", player.getUniqueId(), jobObject)
            );
            if (asDouble(level) != null) {
                return level;
            }
        }

        final Object user = firstNonNull(
            invokeOptional(resolvedApi, "getUser", player),
            invokeOptional(resolvedApi, "getUser", player.getUniqueId()),
            invokeOptional(resolvedApi, "getPlayer", player),
            invokeOptional(resolvedApi, "getPlayerData", player),
            invokeOptional(resolvedApi, "getProfile", player),
            invokeOptional(resolvedApi, "getProfile", player.getUniqueId())
        );
        if (user == null) {
            return null;
        }

        level = firstNonNull(
            invokeOptional(user, "getJobLevel", jobId),
            invokeOptional(user, "getLevel", jobId),
            invokeOptional(user, "getJob", jobId)
        );
        if (asDouble(level) != null) {
            return level;
        }

        if (jobObject != null) {
            level = firstNonNull(
                invokeOptional(user, "getJobLevel", jobObject),
                invokeOptional(user, "getLevel", jobObject),
                invokeOptional(user, "getJob", jobObject)
            );
            if (asDouble(level) != null) {
                return level;
            }
        }

        final Object levelMap = firstNonNull(
            invokeOptional(user, "getJobLevels"),
            invokeOptional(user, "getLevels"),
            readFieldOptional(user, "jobLevels"),
            readFieldOptional(user, "levels")
        );
        return resolveFromNamedMap(levelMap, jobId);
    }

    private @Nullable Object resolveJobObject(final @NotNull String jobId) {
        final Object resolvedApi = this.api;
        if (resolvedApi == null) {
            return null;
        }

        Object job = firstNonNull(
            invokeOptional(resolvedApi, "getJob", jobId),
            invokeOptional(resolvedApi, "getJobById", jobId),
            invokeOptional(resolvedApi, "getJobByID", jobId),
            invokeOptional(resolvedApi, "resolveJob", jobId)
        );
        if (job != null) {
            return job;
        }

        final Plugin installedPlugin = this.plugin;
        if (installedPlugin == null) {
            return null;
        }

        for (final String className : JOB_CLASS_NAMES) {
            final Class<?> jobClass = loadClass(installedPlugin, className);
            if (jobClass == null) {
                continue;
            }

            job = firstNonNull(
                invokeStaticOptional(jobClass, "getByID", jobId),
                invokeStaticOptional(jobClass, "getById", jobId),
                invokeStaticOptional(jobClass, "getByName", jobId),
                invokeStaticOptional(jobClass, "getJob", jobId),
                invokeStaticOptional(jobClass, "of", jobId),
                invokeStaticOptional(jobClass, "valueOf", jobId.toUpperCase(Locale.ROOT))
            );
            if (job != null) {
                return job;
            }

            final Object values = invokeStaticOptional(jobClass, "values");
            if (values instanceof Object[] arrayValues) {
                job = resolveNamedEntry(Arrays.asList(arrayValues), jobId);
                if (job != null) {
                    return job;
                }
            }
        }

        return null;
    }

    private @NotNull List<Object> resolveAllJobObjects() {
        final Plugin installedPlugin = this.plugin;
        if (installedPlugin == null) {
            return List.of();
        }

        for (final String className : JOB_CLASS_NAMES) {
            final Class<?> jobClass = loadClass(installedPlugin, className);
            if (jobClass == null) {
                continue;
            }

            final Object values = invokeStaticOptional(jobClass, "values");
            if (values instanceof Object[] arrayValues && arrayValues.length > 0) {
                return Arrays.asList(arrayValues);
            }
        }

        return List.of();
    }

    private @NotNull String resolveJobIdentifier(final @NotNull Object job) {
        final Object resolved = firstNonNull(
                invokeOptional(job, "getId"),
                invokeOptional(job, "getID"),
                invokeOptional(job, "getKey"),
                invokeOptional(job, "getIdentifier"),
                invokeOptional(job, "getName")
        );
        return resolved == null ? "" : resolved.toString();
    }

    private @NotNull String resolveJobDisplayName(final @NotNull Object job) {
        final Object resolved = firstNonNull(
                invokeOptional(job, "getDisplayName"),
                invokeOptional(job, "getFormattedName"),
                invokeOptional(job, "getName")
        );
        return resolved == null ? "" : resolved.toString();
    }
}
