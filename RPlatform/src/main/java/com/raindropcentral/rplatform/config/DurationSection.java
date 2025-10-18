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

@CSAlways
public class DurationSection extends AConfigSection {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:(\\d+)\\s*(?:w|week|weeks)\\s*)?" +
                    "(?:(\\d+)\\s*(?:d|day|days)\\s*)?" +
                    "(?:(\\d+)\\s*(?:h|hr|hour|hours)\\s*)?" +
                    "(?:(\\d+)\\s*(?:m|min|minute|minutes)\\s*)?" +
                    "(?:(\\d+)\\s*(?:s|sec|second|seconds)\\s*)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");

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

    private String duration;
    private String time;
    private String period;
    private Long seconds;
    private Long minutes;
    private Long hours;
    private Long days;

    public DurationSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

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

    public @NotNull Long getMilliseconds() {
        return this.getSeconds() * 1000L;
    }

    public @NotNull Long getMinutes() {
        return this.getSeconds() / 60L;
    }

    public @NotNull Long getHours() {
        return this.getSeconds() / 3600L;
    }

    public @NotNull Long getDays() {
        return this.getSeconds() / 86400L;
    }

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

    public boolean hasDuration() {
        return this.getSeconds() > 0L;
    }

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
