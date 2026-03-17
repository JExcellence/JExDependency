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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the TimedRequirement API type.
 */
public final class TimedRequirement extends AbstractRequirement {

    @JsonProperty("delegate")
    private final AbstractRequirement delegate;

    @JsonProperty("timeLimitMillis")
    private final long timeLimitMillis;

    @JsonProperty("autoStart")
    private final boolean autoStart;

    @JsonProperty("description")
    private final String description;

    @JsonIgnore
    private final AtomicLong startTimeMillis = new AtomicLong(-1L);

    /**
     * Executes TimedRequirement.
     */
    public TimedRequirement(@NotNull AbstractRequirement delegate, long timeLimitSeconds) {
        this(delegate, timeLimitSeconds * 1000, true, null);
    }

    /**
     * Executes TimedRequirement.
     */
    @JsonCreator
    public TimedRequirement(
            @JsonProperty("delegate") @NotNull AbstractRequirement delegate,
            @JsonProperty("timeLimitMillis") long timeLimitMillis,
            @JsonProperty("autoStart") @Nullable Boolean autoStart,
            @JsonProperty("description") @Nullable String description
    ) {
        super("TIME_BASED");

        if (timeLimitMillis <= 0) throw new IllegalArgumentException("Time limit must be positive: " + timeLimitMillis);

        this.delegate = delegate;
        this.timeLimitMillis = timeLimitMillis;
        this.autoStart = autoStart != null ? autoStart : true;
        this.description = description;
    }

    /**
     * Executes fromTimeConfig.
     */
    @JsonIgnore
    @NotNull
    public static TimedRequirement fromTimeConfig(@NotNull AbstractRequirement delegate,
            @Nullable Long timeSeconds, @Nullable Long timeMinutes, @Nullable Long timeHours, @Nullable Long timeDays,
            @Nullable Boolean autoStart, @Nullable String description) {
        var timeValuesCount = 0;
        var timeLimitMillis = 0L;

        if (timeSeconds != null && timeSeconds > 0) { timeValuesCount++; timeLimitMillis = TimeUnit.SECONDS.toMillis(timeSeconds); }
        if (timeMinutes != null && timeMinutes > 0) { timeValuesCount++; timeLimitMillis = TimeUnit.MINUTES.toMillis(timeMinutes); }
        if (timeHours != null && timeHours > 0) { timeValuesCount++; timeLimitMillis = TimeUnit.HOURS.toMillis(timeHours); }
        if (timeDays != null && timeDays > 0) { timeValuesCount++; timeLimitMillis = TimeUnit.DAYS.toMillis(timeDays); }

        if (timeValuesCount == 0) throw new IllegalArgumentException("At least one time limit must be specified.");
        if (timeValuesCount > 1) throw new IllegalArgumentException("Only one time limit can be specified at a time.");

        return new TimedRequirement(delegate, timeLimitMillis, autoStart, description);
    }

    /**
     * Executes start.
     */
    public void start() { this.startTimeMillis.compareAndSet(-1L, System.currentTimeMillis()); }
    /**
     * Executes reset.
     */
    public void reset() { this.startTimeMillis.set(-1L); }

    /**
     * Returns whether met.
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        if (this.autoStart && this.startTimeMillis.get() < 0) this.start();
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) return false;
        if (System.currentTimeMillis() - startTime > this.timeLimitMillis) return false;
        return this.delegate.isMet(player);
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.autoStart && this.startTimeMillis.get() < 0) this.start();
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) return 0.0;
        final long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= this.timeLimitMillis) return 0.0;
        final double timeFactor = (double) (this.timeLimitMillis - elapsed) / this.timeLimitMillis;
        return this.delegate.calculateProgress(player) * timeFactor;
    }

    /**
     * Executes consume.
     */
    @Override
    public void consume(final @NotNull Player player) {
        if (this.isMet(player)) this.delegate.consume(player);
    }

    /**
     * Gets descriptionKey.
     */
    @Override
    @NotNull
    public String getDescriptionKey() { return "requirement.timed"; }

    /**
     * Gets delegate.
     */
    @NotNull
    public AbstractRequirement getDelegate() { return this.delegate; }

    /**
     * Gets timeLimitMillis.
     */
    public long getTimeLimitMillis() { return this.timeLimitMillis; }

    /**
     * Gets timeLimitSeconds.
     */
    @JsonIgnore
    public long getTimeLimitSeconds() { return TimeUnit.MILLISECONDS.toSeconds(this.timeLimitMillis); }

    /**
     * Gets timeLimitMinutes.
     */
    @JsonIgnore
    public long getTimeLimitMinutes() { return TimeUnit.MILLISECONDS.toMinutes(this.timeLimitMillis); }

    /**
     * Gets timeLimitHours.
     */
    @JsonIgnore
    public long getTimeLimitHours() { return TimeUnit.MILLISECONDS.toHours(this.timeLimitMillis); }

    /**
     * Gets timeLimitDays.
     */
    @JsonIgnore
    public long getTimeLimitDays() { return TimeUnit.MILLISECONDS.toDays(this.timeLimitMillis); }

    /**
     * Gets startTimeMillis.
     */
    public long getStartTimeMillis() { return this.startTimeMillis.get(); }

    /**
     * Returns whether autoStart.
     */
    public boolean isAutoStart() { return this.autoStart; }

    /**
     * Gets description.
     */
    @Nullable
    public String getDescription() { return this.description; }

    /**
     * Returns whether started.
     */
    @JsonIgnore
    public boolean isStarted() { return this.startTimeMillis.get() >= 0; }

    /**
     * Returns whether expired.
     */
    @JsonIgnore
    public boolean isExpired() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) return false;
        return System.currentTimeMillis() - startTime > this.timeLimitMillis;
    }

    /**
     * Gets remainingTimeMillis.
     */
    @JsonIgnore
    public long getRemainingTimeMillis() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) return this.timeLimitMillis;
        return Math.max(0, this.timeLimitMillis - (System.currentTimeMillis() - startTime));
    }

    /**
     * Gets remainingTimeSeconds.
     */
    @JsonIgnore
    public long getRemainingTimeSeconds() { return TimeUnit.MILLISECONDS.toSeconds(this.getRemainingTimeMillis()); }

    /**
     * Gets elapsedTimeMillis.
     */
    @JsonIgnore
    public long getElapsedTimeMillis() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) return 0;
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets elapsedTimeSeconds.
     */
    @JsonIgnore
    public long getElapsedTimeSeconds() { return TimeUnit.MILLISECONDS.toSeconds(this.getElapsedTimeMillis()); }

    /**
     * Gets formattedStartTime.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedStartTime() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) return "Not started";
        final LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        return startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Gets formattedRemainingTime.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedRemainingTime() {
        final long remainingMillis = this.getRemainingTimeMillis();
        if (remainingMillis <= 0) return "Expired";

        final Duration duration = Duration.ofMillis(remainingMillis);
        final long days = duration.toDays();
        final long hours = duration.toHoursPart();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();

        final StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 && days == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Executes validate.
     */
    @JsonIgnore
    public void validate() {
        if (this.delegate == null) throw new IllegalStateException("Delegate requirement cannot be null.");
        if (this.timeLimitMillis <= 0) throw new IllegalStateException("Time limit must be positive: " + this.timeLimitMillis);
        if (this.delegate instanceof final TimedRequirement timedDelegate) timedDelegate.validate();
    }
}
