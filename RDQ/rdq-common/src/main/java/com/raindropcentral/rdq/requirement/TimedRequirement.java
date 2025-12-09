package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    public TimedRequirement(
            @NotNull AbstractRequirement delegate,
            long timeLimitSeconds
    ) {
        this(delegate, timeLimitSeconds * 1000, true, null);
    }

    @JsonCreator
    public TimedRequirement(
            @JsonProperty("delegate") @NotNull AbstractRequirement delegate,
            @JsonProperty("timeLimitMillis") long timeLimitMillis,
            @JsonProperty("autoStart") @Nullable Boolean autoStart,
            @JsonProperty("description") @Nullable String description
    ) {
        super(Type.TIME_BASED);

        if (timeLimitMillis <= 0) {
            throw new IllegalArgumentException("Time limit must be positive: " + timeLimitMillis);
        }

        this.delegate = delegate;
        this.timeLimitMillis = timeLimitMillis;
        this.autoStart = autoStart != null ? autoStart : true;
        this.description = description;
    }

    @JsonIgnore
    @NotNull
    public static TimedRequirement fromTimeConfig(
            @NotNull AbstractRequirement delegate,
            @Nullable Long timeSeconds,
            @Nullable Long timeMinutes,
            @Nullable Long timeHours,
            @Nullable Long timeDays,
            @Nullable Boolean autoStart,
            @Nullable String description
    ) {
        var timeValuesCount = 0;
        var timeLimitMillis = 0L;

        if (timeSeconds != null && timeSeconds > 0) {
            timeValuesCount++;
            timeLimitMillis = TimeUnit.SECONDS.toMillis(timeSeconds);
        }
        if (timeMinutes != null && timeMinutes > 0) {
            timeValuesCount++;
            timeLimitMillis = TimeUnit.MINUTES.toMillis(timeMinutes);
        }
        if (timeHours != null && timeHours > 0) {
            timeValuesCount++;
            timeLimitMillis = TimeUnit.HOURS.toMillis(timeHours);
        }
        if (timeDays != null && timeDays > 0) {
            timeValuesCount++;
            timeLimitMillis = TimeUnit.DAYS.toMillis(timeDays);
        }

        if (timeValuesCount == 0) {
            throw new IllegalArgumentException("At least one time limit must be specified (seconds, minutes, hours, or days).");
        }
        if (timeValuesCount > 1) {
            throw new IllegalArgumentException("Only one time limit can be specified at a time.");
        }

        return new TimedRequirement(delegate, timeLimitMillis, autoStart, description);
    }

    public void start() {
        this.startTimeMillis.compareAndSet(-1L, System.currentTimeMillis());
    }

    public void reset() {
        this.startTimeMillis.set(-1L);
    }

    /**
     * Checks whether the delegate requirement is met before the configured deadline.
     *
     * @param player the player whose progress should be evaluated
     * @return {@code true} if the delegate is satisfied and the timer has not expired
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        if (this.autoStart && this.startTimeMillis.get() < 0) {
            this.start();
        }

        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return false;
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > this.timeLimitMillis) {
            return false;
        }

        return this.delegate.isMet(player);
    }

    /**
     * Calculates progress factoring in the remaining time alongside delegate progress.
     *
     * @param player the player whose progress should be evaluated
     * @return a progress value scaled by the remaining time before expiry
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        if (this.autoStart && this.startTimeMillis.get() < 0) {
            this.start();
        }

        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return 0.0;
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= this.timeLimitMillis) {
            return 0.0;
        }

        final double timeFactor = (double) (this.timeLimitMillis - elapsed) / this.timeLimitMillis;
        return this.delegate.calculateProgress(player) * timeFactor;
    }

    /**
     * Consumes the delegate requirement only if it has been met within the allotted time.
     *
     * @param player the player for whom the requirement should be consumed
     */
    @Override
    public void consume(final @NotNull Player player) {
        if (this.isMet(player)) {
            this.delegate.consume(player);
        }
    }

    /**
     * Provides the translation key for timed requirement descriptions.
     *
     * @return the translation key representing this requirement type
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.timed";
    }

    /**
     * Retrieves the wrapped delegate requirement.
     *
     * @return the delegate requirement
     */
    @NotNull
    public AbstractRequirement getDelegate() {
        return this.delegate;
    }

    /**
     * Gets the configured time limit in milliseconds.
     *
     * @return the time limit in milliseconds
     */
    public long getTimeLimitMillis() {
        return this.timeLimitMillis;
    }

    /**
     * Gets the configured time limit in seconds.
     *
     * @return the time limit in seconds
     */
    @JsonIgnore
    public long getTimeLimitSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.timeLimitMillis);
    }

    /**
     * Gets the configured time limit in minutes.
     *
     * @return the time limit in minutes
     */
    @JsonIgnore
    public long getTimeLimitMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(this.timeLimitMillis);
    }

    /**
     * Gets the configured time limit in hours.
     *
     * @return the time limit in hours
     */
    @JsonIgnore
    public long getTimeLimitHours() {
        return TimeUnit.MILLISECONDS.toHours(this.timeLimitMillis);
    }

    /**
     * Gets the configured time limit in days.
     *
     * @return the time limit in days
     */
    @JsonIgnore
    public long getTimeLimitDays() {
        return TimeUnit.MILLISECONDS.toDays(this.timeLimitMillis);
    }

    /**
     * Obtains the start time in epoch milliseconds.
     *
     * @return the time at which the timer began, or {@code -1} if it has not started
     */
    public long getStartTimeMillis() {
        return this.startTimeMillis.get();
    }

    /**
     * Indicates whether the timer starts automatically when evaluated.
     *
     * @return {@code true} if the timer starts automatically
     */
    public boolean isAutoStart() {
        return this.autoStart;
    }

    /**
     * Provides the optional human readable description associated with the requirement.
     *
     * @return the configured description, or {@code null} if absent
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Determines whether the timer has been started.
     *
     * @return {@code true} if the timer has already begun counting down
     */
    @JsonIgnore
    public boolean isStarted() {
        return this.startTimeMillis.get() >= 0;
    }

    /**
     * Determines whether the time limit has elapsed.
     *
     * @return {@code true} if the timer has expired
     */
    @JsonIgnore
    public boolean isExpired() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return false;
        }
        final long elapsed = System.currentTimeMillis() - startTime;
        return elapsed > this.timeLimitMillis;
    }

    /**
     * Calculates the remaining time in milliseconds before the requirement expires.
     *
     * @return the remaining time in milliseconds, or {@code 0} once expired
     */
    @JsonIgnore
    public long getRemainingTimeMillis() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return this.timeLimitMillis;
        }
        final long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, this.timeLimitMillis - elapsed);
    }

    /**
     * Calculates the remaining time in seconds before the requirement expires.
     *
     * @return the remaining time in seconds
     */
    @JsonIgnore
    public long getRemainingTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.getRemainingTimeMillis());
    }

    /**
     * Calculates the elapsed time in milliseconds since the timer started.
     *
     * @return the elapsed time in milliseconds, or {@code 0} if the timer has not started
     */
    @JsonIgnore
    public long getElapsedTimeMillis() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Calculates the elapsed time in seconds since the timer started.
     *
     * @return the elapsed time in seconds
     */
    @JsonIgnore
    public long getElapsedTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.getElapsedTimeMillis());
    }

    /**
     * Formats the start time into a human readable string.
     *
     * @return a formatted representation of the start time, or {@code "Not started"} if unavailable
     */
    @JsonIgnore
    @NotNull
    public String getFormattedStartTime() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return "Not started";
        }
        final LocalDateTime startDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(startTime),
                ZoneId.systemDefault()
        );
        return startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Formats the remaining time into a compact human readable string.
     *
     * @return a formatted string representing the remaining time, or {@code "Expired"} once elapsed
     */
    @JsonIgnore
    @NotNull
    public String getFormattedRemainingTime() {
        final long remainingMillis = this.getRemainingTimeMillis();
        if (remainingMillis <= 0) {
            return "Expired";
        }

        final Duration duration = Duration.ofMillis(remainingMillis);
        final long days = duration.toDays();
        final long hours = duration.toHoursPart();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();

        final StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Validates that the timed requirement is correctly configured.
     *
     * @throws IllegalStateException if the delegate is missing or an invalid time limit is set
     */
    @JsonIgnore
    public void validate() {
        if (this.delegate == null) {
            throw new IllegalStateException("Delegate requirement cannot be null.");
        }
        if (this.timeLimitMillis <= 0) {
            throw new IllegalStateException("Time limit must be positive: " + this.timeLimitMillis);
        }

        if (this.delegate instanceof final TimedRequirement timedDelegate) {
            timedDelegate.validate();
        }
    }
}