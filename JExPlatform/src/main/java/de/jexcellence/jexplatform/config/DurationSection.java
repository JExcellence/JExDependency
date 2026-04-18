package de.jexcellence.jexplatform.config;

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
 * <p>Supports both structured entries ({@code seconds: 30}) and free-form strings
 * ({@code 1d 3h 5m}). All parsed values are exposed in seconds with helper accessors
 * for alternative time units.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public class DurationSection extends AConfigSection {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:(\\d+)\\s*(?:w|week|weeks)\\s*)?"
                    + "(?:(\\d+)\\s*(?:d|day|days)\\s*)?"
                    + "(?:(\\d+)\\s*(?:h|hr|hour|hours)\\s*)?"
                    + "(?:(\\d+)\\s*(?:m|min|minute|minutes)\\s*)?"
                    + "(?:(\\d+)\\s*(?:s|sec|second|seconds)\\s*)?",
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

    /**
     * Creates a duration section with the provided evaluation environment.
     *
     * @param evaluationEnvironmentBuilder shared expression context
     */
    public DurationSection(@NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Returns the first non-empty raw duration string from any supported key.
     *
     * @return sanitized duration string, or {@code null} when all entries are blank
     */
    public @Nullable String getRawDuration() {
        if (duration != null && !duration.trim().isEmpty()) {
            return duration.trim();
        }
        if (time != null && !time.trim().isEmpty()) {
            return time.trim();
        }
        if (period != null && !period.trim().isEmpty()) {
            return period.trim();
        }
        return null;
    }

    /**
     * Computes the duration in seconds, prioritizing textual entries over structured
     * numeric keys.
     *
     * @return duration in seconds (default {@code 0L})
     */
    public @NotNull Long getSeconds() {
        var raw = getRawDuration();
        if (raw != null) {
            var parsed = parseDurationString(raw);
            if (parsed != null) {
                return parsed;
            }
        }

        if (seconds != null) return seconds;
        if (minutes != null) return minutes * 60L;
        if (hours != null) return hours * 3600L;
        if (days != null) return days * 86400L;

        return 0L;
    }

    /**
     * Converts the resolved seconds to milliseconds.
     *
     * @return total milliseconds
     */
    public @NotNull Long getMilliseconds() {
        return getSeconds() * 1000L;
    }

    /**
     * Converts the resolved seconds to whole minutes.
     *
     * @return total minutes
     */
    public @NotNull Long getMinutes() {
        return getSeconds() / 60L;
    }

    /**
     * Converts the resolved seconds to whole hours.
     *
     * @return total hours
     */
    public @NotNull Long getHours() {
        return getSeconds() / 3600L;
    }

    /**
     * Converts the resolved seconds to whole days.
     *
     * @return total days
     */
    public @NotNull Long getDays() {
        return getSeconds() / 86400L;
    }

    /**
     * Indicates whether the configuration resolves to a non-zero duration.
     *
     * @return {@code true} when the duration is positive
     */
    public boolean hasDuration() {
        return getSeconds() > 0L;
    }

    /**
     * Renders the duration into a human-readable string such as {@code "2 hours 5 minutes"}.
     *
     * @return formatted duration, or {@code "0 seconds"} when unconfigured
     */
    public @NotNull String getFormattedDuration() {
        var totalSeconds = getSeconds();
        if (totalSeconds == 0L) {
            return "0 seconds";
        }

        var sb = new StringBuilder();
        var d = totalSeconds / 86400L;
        var h = (totalSeconds % 86400L) / 3600L;
        var m = (totalSeconds % 3600L) / 60L;
        var s = totalSeconds % 60L;

        if (d > 0) sb.append(d).append(d == 1 ? " day" : " days");
        if (h > 0) { if (!sb.isEmpty()) sb.append(" "); sb.append(h).append(h == 1 ? " hour" : " hours"); }
        if (m > 0) { if (!sb.isEmpty()) sb.append(" "); sb.append(m).append(m == 1 ? " minute" : " minutes"); }
        if (s > 0) { if (!sb.isEmpty()) sb.append(" "); sb.append(s).append(s == 1 ? " second" : " seconds"); }

        return sb.toString();
    }

    /**
     * Validates that the configured duration is non-negative and parseable.
     *
     * @throws IllegalStateException when parsing fails or seconds are negative
     */
    public void validate() {
        var secs = getSeconds();
        if (secs < 0) {
            throw new IllegalStateException("Duration cannot be negative: " + secs + " seconds");
        }
        var raw = getRawDuration();
        if (raw != null && parseDurationString(raw) == null) {
            throw new IllegalStateException("Invalid duration format: " + raw);
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private @Nullable Long parseDurationString(@Nullable String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }

        var trimmed = str.trim().toLowerCase();

        if (NUMBER_PATTERN.matcher(trimmed).matches()) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        var matcher = DURATION_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return parseSimpleDuration(trimmed);
        }

        var total = extractTotalSeconds(matcher);
        return total > 0 ? total : null;
    }

    private long extractTotalSeconds(@NotNull Matcher matcher) {
        var total = 0L;
        if (matcher.group(1) != null) total += Long.parseLong(matcher.group(1)) * 604800L;
        if (matcher.group(2) != null) total += Long.parseLong(matcher.group(2)) * 86400L;
        if (matcher.group(3) != null) total += Long.parseLong(matcher.group(3)) * 3600L;
        if (matcher.group(4) != null) total += Long.parseLong(matcher.group(4)) * 60L;
        if (matcher.group(5) != null) total += Long.parseLong(matcher.group(5));
        return total;
    }

    private @Nullable Long parseSimpleDuration(@NotNull String str) {
        var simplePattern = Pattern.compile("^(\\d+)\\s*([a-z]+)$", Pattern.CASE_INSENSITIVE);
        var matcher = simplePattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }
        try {
            var value = Long.parseLong(matcher.group(1));
            var unit = matcher.group(2).toLowerCase();
            var multiplier = TIME_UNITS.get(unit);
            return multiplier != null ? value * multiplier : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
