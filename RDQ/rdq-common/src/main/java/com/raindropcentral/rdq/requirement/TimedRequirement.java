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

/**
 * Enhanced requirement that must be fulfilled within a specified time limit.
 * <p>
 * The {@code TimedRequirement} wraps a delegate {@link AbstractRequirement} and enforces that it must be
 * completed within a given time window. The timer starts automatically when first checked or can be
 * started explicitly via {@link #start()}.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
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

    public TimedRequirement(
            final @NotNull AbstractRequirement delegate,
            final long timeLimitSeconds
    ) {
        this(delegate, timeLimitSeconds * 1000, true, null);
    }

    @JsonCreator
    public TimedRequirement(
            @JsonProperty("delegate") final @NotNull AbstractRequirement delegate,
            @JsonProperty("timeLimitMillis") final long timeLimitMillis,
            @JsonProperty("autoStart") final @Nullable Boolean autoStart,
            @JsonProperty("description") final @Nullable String description
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
            final @NotNull AbstractRequirement delegate,
            final @Nullable Long timeSeconds,
            final @Nullable Long timeMinutes,
            final @Nullable Long timeHours,
            final @Nullable Long timeDays,
            final @Nullable Boolean autoStart,
            final @Nullable String description
    ) {
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

    public void start() {
        this.startTimeMillis.compareAndSet(-1L, System.currentTimeMillis());
    }

    public void reset() {
        this.startTimeMillis.set(-1L);
    }

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

    @Override
    public void consume(final @NotNull Player player) {
        if (this.isMet(player)) {
            this.delegate.consume(player);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.timed";
    }

    @NotNull
    public AbstractRequirement getDelegate() {
        return this.delegate;
    }

    public long getTimeLimitMillis() {
        return this.timeLimitMillis;
    }

    @JsonIgnore
    public long getTimeLimitSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.timeLimitMillis);
    }

    @JsonIgnore
    public long getTimeLimitMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(this.timeLimitMillis);
    }

    @JsonIgnore
    public long getTimeLimitHours() {
        return TimeUnit.MILLISECONDS.toHours(this.timeLimitMillis);
    }

    @JsonIgnore
    public long getTimeLimitDays() {
        return TimeUnit.MILLISECONDS.toDays(this.timeLimitMillis);
    }

    public long getStartTimeMillis() {
        return this.startTimeMillis.get();
    }

    public boolean isAutoStart() {
        return this.autoStart;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @JsonIgnore
    public boolean isStarted() {
        return this.startTimeMillis.get() >= 0;
    }

    @JsonIgnore
    public boolean isExpired() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return false;
        }
        final long elapsed = System.currentTimeMillis() - startTime;
        return elapsed > this.timeLimitMillis;
    }

    @JsonIgnore
    public long getRemainingTimeMillis() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return this.timeLimitMillis;
        }
        final long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, this.timeLimitMillis - elapsed);
    }

    @JsonIgnore
    public long getRemainingTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.getRemainingTimeMillis());
    }

    @JsonIgnore
    public long getElapsedTimeMillis() {
        final long startTime = this.startTimeMillis.get();
        if (startTime < 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    @JsonIgnore
    public long getElapsedTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(this.getElapsedTimeMillis());
    }

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