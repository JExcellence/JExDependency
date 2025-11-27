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

/**
 * Enhanced requirement that must be fulfilled within a specified time limit.
 * <p>
 * The {@code TimedRequirement} wraps a delegate {@link AbstractRequirement} and enforces that it must be
 * completed within a given time window. The timer starts automatically when first checked or can be
 * started explicitly via {@link #start()}. If the delegate requirement is not fulfilled before the
 * time expires, the requirement is considered failed.
 * </p>
 * <p>
 * Progress is calculated as the product of the delegate's progress and the remaining time ratio,
 * providing a diminishing progress value as time elapses. Resource consumption is only performed if
 * the requirement is met within the time limit.
 * </p>
 *
 * <ul>
 *   <li>Use this requirement to create time-limited challenges or objectives.</li>
 *   <li>Timer can start automatically on first check or be started explicitly.</li>
 *   <li>Supports flexible time configuration (seconds, minutes, hours, days).</li>
 *   <li>Progress and fulfillment are invalid until the timer is started.</li>
 *   <li>Integrates with RequirementSection for configuration-based creation.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class TimedRequirement extends AbstractRequirement {
    
    /**
     * The underlying requirement that must be fulfilled within the time limit.
     */
    @JsonProperty("delegate")
    private final AbstractRequirement delegate;
    
    /**
     * The time limit (in milliseconds) within which the delegate must be fulfilled.
     */
    @JsonProperty("timeLimitMillis")
    private final long timeLimitMillis;
    
    /**
     * Whether the timer should start automatically on first check.
     */
    @JsonProperty("autoStart")
    private final boolean autoStart;
    
    /**
     * Optional description for this timed requirement.
     */
    @JsonProperty("description")
    private final String description;
    
    /**
     * The timestamp (in milliseconds) when the timer was started, or -1 if not started.
     * This is not serialized as it's runtime state.
     */
    @JsonIgnore
    private volatile long startTimeMillis = -1;
    
    /**
     * Constructs a {@code TimedRequirement} with a delegate requirement and a time limit in seconds (backward compatibility).
     *
     * @param delegate         The underlying requirement that must be met.
     * @param timeLimitSeconds The time limit (in seconds) within which the delegate must be fulfilled.
     */
    public TimedRequirement(
        @NotNull final AbstractRequirement delegate,
        final long timeLimitSeconds
    ) {
        this(delegate, timeLimitSeconds * 1000, true, null);
    }
    
    /**
     * Constructs a {@code TimedRequirement} with full configuration options.
     *
     * @param delegate The underlying requirement that must be met.
     * @param timeLimitMillis The time limit in milliseconds.
     * @param autoStart Whether to start the timer automatically on first check.
     * @param description Optional description for this requirement.
     */
    @JsonCreator
    public TimedRequirement(
        @JsonProperty("delegate") @NotNull final AbstractRequirement delegate,
        @JsonProperty("timeLimitMillis") final long timeLimitMillis,
        @JsonProperty("autoStart") @Nullable final Boolean autoStart,
        @JsonProperty("description") @Nullable final String description
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
    
    /**
     * Factory method to create a TimedRequirement from time configuration values.
     * Useful for RequirementSection integration.
     *
     * @param delegate The underlying requirement.
     * @param timeSeconds Time limit in seconds (can be null).
     * @param timeMinutes Time limit in minutes (can be null).
     * @param timeHours Time limit in hours (can be null).
     * @param timeDays Time limit in days (can be null).
     * @param autoStart Whether to auto-start the timer.
     * @param description Optional description.
     * @return A new TimedRequirement instance.
     * @throws IllegalArgumentException If no time limit is specified or multiple are specified.
     */
    @JsonIgnore
    @NotNull
    public static TimedRequirement fromTimeConfig(
        @NotNull final AbstractRequirement delegate,
        @Nullable final Long timeSeconds,
        @Nullable final Long timeMinutes,
        @Nullable final Long timeHours,
        @Nullable final Long timeDays,
        @Nullable final Boolean autoStart,
        @Nullable final String description
    ) {
        // Count how many time values are specified
        int timeValuesCount = 0;
        long timeLimitMillis = 0;
        
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
    
    /**
     * Starts the timer for the timed requirement.
     * <p>
     * This method should be called when the timed challenge begins.
     * If the timer has already been started, this method has no effect.
     * </p>
     */
    public void start() {
        if (this.startTimeMillis < 0) {
            this.startTimeMillis = System.currentTimeMillis();
        }
    }
    
    /**
     * Resets the timer, allowing it to be started again.
     * <p>
     * This method can be used to restart a timed challenge.
     * </p>
     */
    public void reset() {
        this.startTimeMillis = -1;
    }
    
    /**
     * Checks whether the delegate requirement is met and that it was completed within the time limit.
     * <p>
     * The requirement is not met if the timer hasn't been started (and auto-start is disabled)
     * or if the time limit has expired.
     * </p>
     *
     * @param player The player whose state is checked.
     * @return {@code true} if the delegate is met and the elapsed time is within the allowed limit, {@code false} otherwise.
     */
    @Override
    public boolean isMet(
        @NotNull final Player player
    ) {
        // Auto-start if enabled and not started yet
        if (this.autoStart && this.startTimeMillis < 0) {
            this.start();
        }
        
        if (this.startTimeMillis < 0) {
            return false; // not started yet
        }
        
        final long elapsed = System.currentTimeMillis() - this.startTimeMillis;
        if (elapsed > this.timeLimitMillis) {
            return false; // time expired
        }
        
        return this.delegate.isMet(player);
    }
    
    /**
     * Calculates progress as a combination of the delegate's progress and
     * the proportion of remaining time. As more time elapses, the effective progress diminishes.
     * Returns 0.0 if the timer hasn't been started or if the time limit has expired.
     *
     * @param player The player whose progress is computed.
     * @return A value between 0.0 and 1.0 representing overall progress.
     */
    @Override
    public double calculateProgress(
        @NotNull final Player player
    ) {
        // Auto-start if enabled and not started yet
        if (this.autoStart && this.startTimeMillis < 0) {
            this.start();
        }
        
        if (this.startTimeMillis < 0) {
            return 0.0;
        }
        
        final long elapsed = System.currentTimeMillis() - this.startTimeMillis;
        if (elapsed >= this.timeLimitMillis) {
            return 0.0;
        }
        
        final double timeFactor = (double) (this.timeLimitMillis - elapsed) / this.timeLimitMillis;
        return this.delegate.calculateProgress(player) * timeFactor;
    }
    
    /**
     * Consumes the delegate resources if the timed requirement is met.
     * <p>
     * No resources are consumed if the requirement is not met.
     * </p>
     *
     * @param player The player whose resources are consumed.
     */
    @Override
    public void consume(
        @NotNull final Player player
    ) {
        if (this.isMet(player)) {
            this.delegate.consume(player);
        }
    }
    
    /**
     * Returns the translation key for this requirement's description.
     * <p>
     * This key can be used for localization and user-facing descriptions.
     * </p>
     *
     * @return The language key for this requirement's description, typically {@code "requirement.timed"}.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.timed";
    }
    
    /**
     * Returns the underlying delegate requirement.
     *
     * @return The delegate {@link AbstractRequirement}.
     */
    @NotNull
    public AbstractRequirement getDelegate() {
        return this.delegate;
    }
    
    /**
     * Returns the time limit in milliseconds.
     *
     * @return Time limit in milliseconds.
     */
    public long getTimeLimitMillis() {
        return this.timeLimitMillis;
    }
    
    /**
     * Returns the time limit in seconds.
     *
     * @return Time limit in seconds.
     */
    @JsonIgnore
    public long getTimeLimitSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.timeLimitMillis);
    }
    
    /**
     * Returns the time limit in minutes.
     *
     * @return Time limit in minutes.
     */
    @JsonIgnore
    public long getTimeLimitMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(this.timeLimitMillis);
    }
    
    /**
     * Returns the time limit in hours.
     *
     * @return Time limit in hours.
     */
    @JsonIgnore
    public long getTimeLimitHours() {
        return TimeUnit.MILLISECONDS.toHours(this.timeLimitMillis);
    }
    
    /**
     * Returns the time limit in days.
     *
     * @return Time limit in days.
     */
    @JsonIgnore
    public long getTimeLimitDays() {
        return TimeUnit.MILLISECONDS.toDays(this.timeLimitMillis);
    }
    
    /**
     * Returns the start time in milliseconds, or -1 if not started.
     *
     * @return Start time in milliseconds, or -1 if the timer hasn't been started.
     */
    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }
    
    /**
     * Gets whether the timer auto-starts on first check.
     *
     * @return True if auto-start is enabled, false otherwise.
     */
    public boolean isAutoStart() {
        return this.autoStart;
    }
    
    /**
     * Gets the optional description for this timed requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Checks if the timer has been started.
     *
     * @return True if the timer has been started, false otherwise.
     */
    @JsonIgnore
    public boolean isStarted() {
        return this.startTimeMillis >= 0;
    }
    
    /**
     * Checks if the time limit has expired.
     *
     * @return True if the time limit has expired, false otherwise.
     */
    @JsonIgnore
    public boolean isExpired() {
        if (this.startTimeMillis < 0) {
            return false; // not started yet
        }
        
        final long elapsed = System.currentTimeMillis() - this.startTimeMillis;
        return elapsed > this.timeLimitMillis;
    }
    
    /**
     * Gets the remaining time in milliseconds.
     *
     * @return Remaining time in milliseconds, or the full time limit if not started, or 0 if expired.
     */
    @JsonIgnore
    public long getRemainingTimeMillis() {
        if (this.startTimeMillis < 0) {
            return this.timeLimitMillis; // not started yet
        }
        
        final long elapsed = System.currentTimeMillis() - this.startTimeMillis;
        return Math.max(0, this.timeLimitMillis - elapsed);
    }
    
    /**
     * Gets the remaining time in seconds.
     *
     * @return Remaining time in seconds.
     */
    @JsonIgnore
    public long getRemainingTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.getRemainingTimeMillis());
    }
    
    /**
     * Gets the elapsed time in milliseconds.
     *
     * @return Elapsed time in milliseconds, or 0 if not started.
     */
    @JsonIgnore
    public long getElapsedTimeMillis() {
        if (this.startTimeMillis < 0) {
            return 0; // not started yet
        }
        
        return System.currentTimeMillis() - this.startTimeMillis;
    }
    
    /**
     * Gets the elapsed time in seconds.
     *
     * @return Elapsed time in seconds.
     */
    @JsonIgnore
    public long getElapsedTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.getElapsedTimeMillis());
    }
    
    /**
     * Gets the start time as a formatted string.
     *
     * @return Formatted start time, or "Not started" if not started.
     */
    @JsonIgnore
    @NotNull
    public String getFormattedStartTime() {
        if (this.startTimeMillis < 0) {
            return "Not started";
        }
        
        final LocalDateTime startTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(this.startTimeMillis),
            ZoneId.systemDefault()
        );
        
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Gets the remaining time as a human-readable string.
     *
     * @return Formatted remaining time (e.g., "5m 30s", "2h 15m", "1d 3h").
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
        if (seconds > 0 && days == 0) { // Only show seconds if less than a day
            sb.append(seconds).append("s");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Validates the internal state of this timed requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
     */
    @JsonIgnore
    public void validate() {
        if (this.delegate == null) {
            throw new IllegalStateException("Delegate requirement cannot be null.");
        }
        
        if (this.timeLimitMillis <= 0) {
            throw new IllegalStateException("Time limit must be positive: " + this.timeLimitMillis);
        }
        
        // Validate delegate requirement if it has a validate method
        if (this.delegate instanceof final TimedRequirement timedDelegate) {
            timedDelegate.validate();
        }
    }
}