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

package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Represents the PlaytimeRequirement API type.
 */
public final class PlaytimeRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(PlaytimeRequirement.class.getName());

    @JsonProperty("requiredPlaytimeSeconds")
    private final long requiredPlaytimeSeconds;

    @JsonProperty("worldPlaytimeRequirements")
    private final Map<String, Long> worldPlaytimeRequirements;

    @JsonProperty("useTotalPlaytime")
    private final boolean useTotalPlaytime;

    @JsonProperty("description")
    private final String description;

    @JsonIgnore
    private transient final Map<String, World> worldCache = new ConcurrentHashMap<>();

    /**
     * Executes PlaytimeRequirement.
     */
    public PlaytimeRequirement(long requiredPlaytimeSeconds) {
        this(requiredPlaytimeSeconds, null, true, null);
    }

    /**
     * Executes PlaytimeRequirement.
     */
    @JsonCreator
    public PlaytimeRequirement(@JsonProperty("requiredPlaytimeSeconds") long requiredPlaytimeSeconds,
                              @JsonProperty("worldPlaytimeRequirements") @Nullable Map<String, Long> worldPlaytimeRequirements,
                              @JsonProperty("useTotalPlaytime") @Nullable Boolean useTotalPlaytime,
                              @JsonProperty("description") @Nullable String description) {
        super("PLAYTIME");

        if (requiredPlaytimeSeconds < 0) throw new IllegalArgumentException("Required playtime cannot be negative.");

        this.requiredPlaytimeSeconds = requiredPlaytimeSeconds;
        this.worldPlaytimeRequirements = worldPlaytimeRequirements != null ? new HashMap<>(worldPlaytimeRequirements) : new HashMap<>();
        this.useTotalPlaytime = useTotalPlaytime != null ? useTotalPlaytime : true;
        this.description = description;

        if (!this.useTotalPlaytime && this.worldPlaytimeRequirements.isEmpty()) {
            throw new IllegalArgumentException("World playtime requirements cannot be empty when not using total playtime");
        }
    }

    /**
     * Returns whether met.
     */
    @Override
    public boolean isMet(@NotNull Player player) {
        if (useTotalPlaytime) return getTotalPlaytimeSeconds(player) >= requiredPlaytimeSeconds;
        else return checkWorldPlaytimeRequirements(player);
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public double calculateProgress(@NotNull Player player) {
        if (useTotalPlaytime) {
            if (requiredPlaytimeSeconds <= 0) return 1.0;
            return Math.min(1.0, (double) getTotalPlaytimeSeconds(player) / requiredPlaytimeSeconds);
        } else {
            return calculateWorldPlaytimeProgress(player);
        }
    }

    /**
     * Executes consume.
     */
    @Override
    public void consume(@NotNull Player player) {}

    /**
     * Gets descriptionKey.
     */
    @Override
    @NotNull
    public String getDescriptionKey() { return "requirement.playtime"; }

    /**
     * Gets totalPlaytimeSeconds.
     */
    @JsonIgnore
    public long getTotalPlaytimeSeconds(@NotNull Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
    }

    /**
     * Gets worldPlaytimeSeconds.
     */
    @JsonIgnore
    public long getWorldPlaytimeSeconds(@NotNull Player player, @NotNull String worldName) {
        var world = getCachedWorld(worldName);
        if (world == null) return 0;
        try {
            return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        } catch (Exception e) { return 0; }
    }

    /**
     * Gets requiredPlaytimeSeconds.
     */
    public long getRequiredPlaytimeSeconds() { return requiredPlaytimeSeconds; }

    /**
     * Gets requiredPlaytimeMinutes.
     */
    @JsonIgnore
    public long getRequiredPlaytimeMinutes() { return TimeUnit.SECONDS.toMinutes(requiredPlaytimeSeconds); }

    /**
     * Gets requiredPlaytimeHours.
     */
    @JsonIgnore
    public long getRequiredPlaytimeHours() { return TimeUnit.SECONDS.toHours(requiredPlaytimeSeconds); }

    /**
     * Gets requiredPlaytimeDays.
     */
    @JsonIgnore
    public long getRequiredPlaytimeDays() { return TimeUnit.SECONDS.toDays(requiredPlaytimeSeconds); }

    /**
     * Gets worldPlaytimeRequirements.
     */
    @NotNull
    public Map<String, Long> getWorldPlaytimeRequirements() { return new HashMap<>(worldPlaytimeRequirements); }

    /**
     * Returns whether useTotalPlaytime.
     */
    public boolean isUseTotalPlaytime() { return useTotalPlaytime; }

    /**
     * Gets description.
     */
    @Nullable
    public String getDescription() { return description; }

    /**
     * Gets formattedRequiredPlaytime.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedRequiredPlaytime() {
        if (!this.useTotalPlaytime) return this.formatWorldRequirements();
        return formatDuration(this.requiredPlaytimeSeconds);
    }

    /**
     * Gets formattedCurrentPlaytime.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedCurrentPlaytime(final @NotNull Player player) {
        if (!this.useTotalPlaytime) return formatCurrentWorldPlaytime(player);
        return formatDuration(getTotalPlaytimeSeconds(player));
    }

    /**
     * Executes formatDuration.
     */
    @JsonIgnore
    @NotNull
    public static String formatDuration(final long seconds) {
        if (seconds <= 0) return "0s";
        final long days = TimeUnit.SECONDS.toDays(seconds);
        final long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        final long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        final long remainingSeconds = seconds % 60;

        final StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (remainingSeconds > 0 && days == 0) sb.append(remainingSeconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Executes validate.
     */
    @JsonIgnore
    public void validate() {
        if (this.requiredPlaytimeSeconds < 0) throw new IllegalStateException("Required playtime cannot be negative.");
        if (!this.useTotalPlaytime && this.worldPlaytimeRequirements.isEmpty())
            throw new IllegalStateException("World playtime requirements cannot be empty when not using total playtime");

        for (final Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty())
                throw new IllegalStateException("World name cannot be null or empty");
            if (entry.getValue() < 0)
                throw new IllegalStateException("World playtime requirement cannot be negative: " + entry.getValue());
        }
    }

    @Nullable
    private World getCachedWorld(@NotNull String worldName) {
        return worldCache.computeIfAbsent(worldName, Bukkit::getWorld);
    }

    private boolean checkWorldPlaytimeRequirements(@NotNull Player player) {
        for (var entry : worldPlaytimeRequirements.entrySet()) {
            if (getWorldPlaytimeSeconds(player, entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    private double calculateWorldPlaytimeProgress(@NotNull Player player) {
        if (worldPlaytimeRequirements.isEmpty()) return 1.0;
        var totalProgress = 0.0;
        var validRequirements = 0;

        for (var entry : worldPlaytimeRequirements.entrySet()) {
            var requiredSeconds = entry.getValue();
            if (requiredSeconds <= 0) totalProgress += 1.0;
            else totalProgress += Math.min(1.0, (double) getWorldPlaytimeSeconds(player, entry.getKey()) / requiredSeconds);
            validRequirements++;
        }
        return validRequirements > 0 ? totalProgress / validRequirements : 1.0;
    }

    @NotNull
    private String formatWorldRequirements() {
        if (this.worldPlaytimeRequirements.isEmpty()) return "No world requirements";
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Long> entry : this.worldPlaytimeRequirements.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append(": ").append(formatDuration(entry.getValue()));
        }
        return sb.toString();
    }

    @NotNull
    private String formatCurrentWorldPlaytime(final @NotNull Player player) {
        if (this.worldPlaytimeRequirements.isEmpty()) return "No world requirements";
        final StringBuilder sb = new StringBuilder();
        for (final String worldName : this.worldPlaytimeRequirements.keySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(worldName).append(": ").append(formatDuration(getWorldPlaytimeSeconds(player, worldName)));
        }
        return sb.toString();
    }
}
