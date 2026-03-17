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

package com.raindropcentral.rplatform.version;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Centralizes environment detection for the running server, eagerly resolving the platform type,.
 * package version, and Minecraft branding details once and exposing them via a thread-safe
 * singleton. Detection leverages lightweight reflection checks and Bukkit access during
 * construction, with the results cached for the lifetime of the JVM. Concurrency is managed via a
 * double-checked locking pattern so the expensive detection work executes at most once across
 * threads.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ServerEnvironment {

    /**
     * Shared logger used to report detection results and any fallback resolution steps taken.
     */
    private static final Logger LOGGER = Logger.getLogger(ServerEnvironment.class.getName());

    /**
     * Lazily initialized singleton instance guarded by {@link #getInstance()} using double-checked.
     * locking semantics.
     */
    private static volatile ServerEnvironment instance;

    private final ServerType serverType;
    private final String serverVersion;
    private final String minecraftVersion;
    private final boolean modernVersion;

    private ServerEnvironment() {
        this.serverType = detectServerType();
        this.minecraftVersion = detectMinecraftVersion();
        this.serverVersion = detectServerVersion();
        this.modernVersion = isVersionModern();

        logEnvironmentInfo();
    }

    /**
     * Provides access to the cached {@link ServerEnvironment} instance, performing detection only.
     * on the first call while ensuring concurrent threads see a fully initialized environment.
     *
     * @return the memoized environment snapshot for the running server
     */
    public static @NotNull ServerEnvironment getInstance() {
        if (instance == null) {
            synchronized (ServerEnvironment.class) {
                if (instance == null) {
                    instance = new ServerEnvironment();
                }
            }
        }
        return instance;
    }

    /**
     * Gets serverType.
     */
    public @NotNull ServerType getServerType() {
        return serverType;
    }

    /**
     * Gets serverVersion.
     */
    public @NotNull String getServerVersion() {
        return serverVersion;
    }

    /**
     * Gets minecraftVersion.
     */
    public @NotNull String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Returns whether modern.
     */
    public boolean isModern() {
        return modernVersion;
    }

    /**
     * Returns whether paper.
     */
    public boolean isPaper() {
        return serverType == ServerType.PAPER || serverType == ServerType.PURPUR;
    }

    /**
     * Returns whether folia.
     */
    public boolean isFolia() {
        return serverType == ServerType.FOLIA;
    }

    /**
     * Returns whether spigot.
     */
    public boolean isSpigot() {
        return serverType == ServerType.SPIGOT;
    }

    /**
     * Returns whether class.
     */
    public boolean hasClass(final @NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns whether versionAtLeast.
     */
    public boolean isVersionAtLeast(final @NotNull String targetVersion) {
        return serverVersion.compareTo(targetVersion) >= 0;
    }

    /**
     * Determines the active server distribution by testing for Folia, Paper, and Purpur marker.
     * classes, defaulting to {@link ServerType#SPIGOT} when none match.
     *
     * @return the detected {@link ServerType}, or {@link ServerType#SPIGOT} when no specific
     * distribution is discovered
     */
    private @NotNull ServerType detectServerType() {
        if (hasClass("io.papermc.paper.threadedregions.RegionizedServer")) {
            return ServerType.FOLIA;
        }

        if (hasClass("com.destroystokyo.paper.ParticleBuilder")) {
            if (hasClass("org.purpurmc.purpur.PurpurConfig")) {
                return ServerType.PURPUR;
            }
            return ServerType.PAPER;
        }

        return ServerType.SPIGOT;
    }

    /**
     * Resolves the Minecraft client branding string reported by Bukkit, falling back to.
     * {@code "unknown"} when the version cannot be read.
     *
     * @return the Minecraft version string, or {@code "unknown"} if detection fails
     */
    private @NotNull String detectMinecraftVersion() {
        try {
            return Bukkit.getVersion();
        } catch (final Exception e) {
            LOGGER.warning("Failed to detect Minecraft version: " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Extracts the Bukkit package version from the server implementation name, returning.
     * {@code "unknown"} when the value is absent or an error occurs.
     *
     * @return the server package version identifier, or {@code "unknown"} if unavailable
     */
    private @NotNull String detectServerVersion() {
        try {
            final String packageName = Bukkit.getServer().getClass().getPackage().getName();
            final String[] parts = packageName.split("\\.");
            
            if (parts.length > 3) {
                return parts[3];
            }
        } catch (final Exception e) {
            LOGGER.warning("Failed to detect server version: " + e.getMessage());
        }
        
        return "unknown";
    }

    /**
     * Evaluates whether the detected server package version represents a modern (1.13+) release,.
     * treating {@code "unknown"} versions as modern to preserve compatibility with future or
     * unrecognized builds.
     *
     * @return {@code true} when the server should be treated as modern, otherwise {@code false}
     */
    private boolean isVersionModern() {
        if ("unknown".equals(serverVersion)) {
            return true;
        }
        return serverVersion.compareTo("v1_13") >= 0;
    }

    private void logEnvironmentInfo() {
        LOGGER.info("=== Server Environment ===");
        LOGGER.info("Type: " + serverType);
        LOGGER.info("Version: " + serverVersion);
        LOGGER.info("Minecraft: " + minecraftVersion);
        LOGGER.info("Modern: " + modernVersion);
        LOGGER.info("=========================");
    }

    /**
     * Enumerates supported server distributions used to tailor feature toggles and compatibility.
     * checks across the platform.
     */
    public enum ServerType {
        FOLIA,
        PAPER,
        PURPUR,
        SPIGOT
    }
}
