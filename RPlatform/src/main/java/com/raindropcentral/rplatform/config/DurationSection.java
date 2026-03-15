package com.raindropcentral.rplatform.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration section that normalizes duration values declared in YAML.
 *
 * <p>The mapper supports both structured entries (for example {@code seconds: 30}) and free-form
 * strings such as {@code 1d 3h 5m}. All parsed values are exposed in seconds while retaining helper
 * accessors for alternative time units. This allows downstream features to work with consistent
 * numeric representations regardless of how administrators describe the duration in configuration
 * files.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class DurationSection extends AConfigSection {

    /**
     * Pattern that captures compound duration strings such as {@code 1w 2d 3h}.
     * The groups are ordered from weeks down to seconds to simplify the seconds aggregation logic.
     */
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:(\\d+)\\s*(?:w|week|weeks)\\s*)?" +
                    "(?:(\\d+)\\s*(?:d|day|days)\\s*)?" +
                    "(?:(\\d+)\\s*(?:h|hr|hour|hours)\\s*)?" +
                    "(?:(\\d+)\\s*(?:m|min|minute|minutes)\\s*)?" +
                    "(?:(\\d+)\\s*(?:s|sec|second|seconds)\\s*)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern used to short-circuit pure numeric values which implicitly represent seconds.
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");

    /**
     * Lookup table mapping textual units to their second multiplier. This supports abbreviated and.
     * long-form tokens so administrators can use their preferred vocabulary.
     */
    private static final Map<String, Long> TIME_UNITS = new HashMap<>();

    static {
        TIME_UNITS.put("s", 1L);
        TIME_UNITS.put("sec", 1L);
        TIME_UNITS.put("second", 1L);
        TIME_UNITS.put("seconds", 1L);
        TIME_UNITS.put("m", 60L);
        TIME_UNITS.put("min", 60L);
        TIME_UNITS.put("minute", 60L);
        TIME_UNITS.put("minutes", 60L);
        TIME_UNITS.put("h", 3600L);
        TIME_UNITS.put("hr", 3600L);
        TIME_UNITS.put("hour", 3600L);
        TIME_UNITS.put("hours", 3600L);
        TIME_UNITS.put("d", 86400L);
        TIME_UNITS.put("day", 86400L);
        TIME_UNITS.put("days", 86400L);
        TIME_UNITS.put("w", 604800L);
        TIME_UNITS.put("week", 604800L);
        TIME_UNITS.put("weeks", 604800L);
    }

    /**
     * Raw duration string as provided via {@code duration:} in YAML.
     */
    private String duration;

    /**
     * Alternative legacy key representing a duration string.
     */
    private String time;

    /**
     * Secondary alias accepted for backwards compatibility with existing data files.
     */
    private String period;

    /**
     * Structured override that defines a fixed number of seconds.
     */
    private Long seconds;

    /**
     * Structured override expressed in minutes.
     */
    private Long minutes;

    /**
     * Structured override expressed in hours.
     */
    private Long hours;

    /**
     * Structured override expressed in days.
     */
    private Long days;

    /**
     * Creates a new duration section configured with the provided evaluation environment so that.
     * embedded expressions resolve consistently with other configuration sections.
     *
     * @param evaluationEnvironmentBuilder builder that supplies the shared expression context
     */
    public DurationSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Resolves the first non-empty raw duration string supplied by any supported key.
     *
     * @return sanitized duration string or {@code null} when all textual entries are blank
     */
    public @Nullable String getRawDuration() {
        if (this.duration != null && !this.duration.trim().isEmpty()) {
            return this.duration.trim();
        }
        if (this.time != null && !this.time.trim().isEmpty()) {
            return this.time.trim();
        }
        if (this.period != null && !this.period.trim().isEmpty()) {
            return this.period.trim();
        }
        return null;
    }

    /**
     * Computes the duration in seconds by prioritizing textual entries, falling back to structured.
     * numeric keys when necessary.
     *
     * @return duration in seconds, defaulting to {@code 0L} when nothing is configured
     */
    public @NotNull Long getSeconds() {
        final String rawDuration = this.getRawDuration();
        if (rawDuration != null) {
            final Long parsed = this.parseDurationString(rawDuration);
            if (parsed != null) {
                return parsed;
            }
        }

        if (this.seconds != null) {
            return this.seconds;
        }
        if (this.minutes != null) {
            return this.minutes * 60L;
        }
        if (this.hours != null) {
            return this.hours * 3600L;
        }
        if (this.days != null) {
            return this.days * 86400L;
        }

        return 0L;
    }

    /**
     * Converts the resolved seconds into milliseconds for APIs that expect millisecond precision.
     *
     * @return total milliseconds represented by this configuration (never {@code null})
     */
    public @NotNull Long getMilliseconds() {
        return this.getSeconds() * 1000L;
    }

    /**
     * Converts the resolved seconds into whole minutes using integer division.
     *
     * @return total minutes represented by this configuration
     */
    public @NotNull Long getMinutes() {
        return this.getSeconds() / 60L;
    }

    /**
     * Converts the resolved seconds into whole hours using integer division.
     *
     * @return total hours represented by this configuration
     */
    public @NotNull Long getHours() {
        return this.getSeconds() / 3600L;
    }

    /**
     * Converts the resolved seconds into whole days using integer division.
     *
     * @return total days represented by this configuration
     */
    public @NotNull Long getDays() {
        return this.getSeconds() / 86400L;
    }

    /**
     * Attempts to parse the supplied duration string into seconds.
 *
 * <p>Compound values (for example {@code 1d 2h}) use {@link #DURATION_PATTERN} while simple
     * {@code value+unit} pairs rely on {@link #parseSimpleDuration(String)}. Numeric-only tokens are
     * treated as seconds by default. Invalid tokens quietly return {@code null} so callers can try
     * additional fallbacks.
     *
     * @param durationStr free-form duration text from configuration
     * @return parsed seconds, or {@code null} when parsing fails
     */
    private @Nullable Long parseDurationString(final @Nullable String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return null;
        }

        final String trimmed = durationStr.trim().toLowerCase();

        if (NUMBER_PATTERN.matcher(trimmed).matches()) {
            try {
                return Long.parseLong(trimmed);
            } catch (final NumberFormatException e) {
                return null;
            }
        }

        final Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return this.parseSimpleDuration(trimmed);
        }

        final long totalSeconds = getTotalSeconds(matcher);
        return totalSeconds > 0 ? totalSeconds : null;
    }

    /**
     * Aggregates the regex matcher groups into a total second count.
     *
     * @param matcher matcher that already satisfied {@link #DURATION_PATTERN}
     * @return total seconds represented by the captured groups
     */
    private long getTotalSeconds(final @NotNull Matcher matcher) {
        long totalSeconds = 0L;

        if (matcher.group(1) != null) {
            totalSeconds += Long.parseLong(matcher.group(1)) * 604800L;
        }
        if (matcher.group(2) != null) {
            totalSeconds += Long.parseLong(matcher.group(2)) * 86400L;
        }
        if (matcher.group(3) != null) {
            totalSeconds += Long.parseLong(matcher.group(3)) * 3600L;
        }
        if (matcher.group(4) != null) {
            totalSeconds += Long.parseLong(matcher.group(4)) * 60L;
        }
        if (matcher.group(5) != null) {
            totalSeconds += Long.parseLong(matcher.group(5));
        }

        return totalSeconds;
    }

    /**
     * Parses simple {@code <number><unit>} pairs (for example {@code 30m}) into seconds.
     *
     * @param durationStr sanitized string containing a single numeric value and unit token
     * @return seconds represented by the pair, or {@code null} when no matching unit exists
     */
    private @Nullable Long parseSimpleDuration(final @NotNull String durationStr) {
        final Pattern simplePattern = Pattern.compile("^(\\d+)\\s*([a-z]+)$", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = simplePattern.matcher(durationStr);

        if (!matcher.matches()) {
            return null;
        }

        try {
            final long value = Long.parseLong(matcher.group(1));
            final String unit = matcher.group(2).toLowerCase();
            final Long multiplier = TIME_UNITS.get(unit);

            if (multiplier != null) {
                return value * multiplier;
            }
        } catch (final NumberFormatException ignored) {
        }

        return null;
    }

    /**
     * Indicates whether the configuration resolves to a non-zero duration.
     *
     * @return {@code true} when at least one supported input produces a positive value
     */
    public boolean hasDuration() {
        return this.getSeconds() > 0L;
    }

    /**
     * Renders the configured duration into a human-readable string such as {@code 2 hours 5 minutes}.
     *
     * @return formatted duration description, or {@code "0 seconds"} when nothing is configured
     */
    public @NotNull String getFormattedDuration() {
        final long totalSeconds = this.getSeconds();

        if (totalSeconds == 0L) {
            return "0 seconds";
        }

        final StringBuilder sb = new StringBuilder();
        final long days = totalSeconds / 86400L;
        final long hours = (totalSeconds % 86400L) / 3600L;
        final long minutes = (totalSeconds % 3600L) / 60L;
        final long seconds = totalSeconds % 60L;

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    /**
     * Validates that the configured duration resolves to a non-negative value and that any textual.
     * representation can be parsed.
     *
     * @throws IllegalStateException when parsing fails or the computed seconds are negative
     */
    public void validate() {
        final Long seconds = this.getSeconds();
        if (seconds < 0) {
            throw new IllegalStateException("Duration cannot be negative: " + seconds + " seconds");
        }

        final String rawDuration = this.getRawDuration();
        if (rawDuration != null && this.parseDurationString(rawDuration) == null) {
            throw new IllegalStateException("Invalid duration format: " + rawDuration);
        }
    }
}
