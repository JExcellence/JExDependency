package de.jexcellence.jexplatform.config.permission;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.jexplatform.config.DurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permission-gated duration configuration section.
 *
 * <p>Resolves a duration (in seconds) for a player based on their permissions.
 * Uses {@link DurationSection} for both the default and per-permission values,
 * supporting compound formats like {@code "1d 3h 5m"}.
 *
 * <p>When multiple permissions match and best-value resolution is active, the
 * <strong>longest</strong> duration wins. Optional min/max bounds clamp the
 * resolved value.
 *
 * <pre>{@code
 * # YAML example
 * duration:
 *   enabled: true
 *   useBestValue: true
 *   maxDuration: 604800   # 1 week cap
 *   minDuration: 60       # 1 minute floor
 *   defaultDuration:
 *     duration: "1h"
 *   permissionDurations:
 *     "myplugin.duration.vip":
 *       duration: "6h"
 *     "myplugin.duration.elite":
 *       duration: "1d"
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public class PermissionDurationSection extends PermissionBasedSection<Long> {

    private DurationSection defaultDuration;
    private Map<String, DurationSection> permissionDurations;
    private Long maxDuration;
    private Long minDuration;

    /**
     * Creates a duration section with the provided evaluation environment.
     *
     * @param evaluationEnvironmentBuilder shared expression context
     */
    public PermissionDurationSection(
            @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    // ── Domain accessors ──────────────────────────────────────────────────────

    /**
     * Returns the effective duration in seconds for the given player.
     *
     * <p>Convenience alias for {@link #getEffectiveValue(Player)}.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return the resolved duration in seconds
     */
    public long getEffectiveDuration(@Nullable Player player) {
        return getEffectiveValue(player);
    }

    /**
     * Returns the effective duration in milliseconds for the given player.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return the resolved duration in milliseconds
     */
    public long getEffectiveDurationMillis(@Nullable Player player) {
        return getEffectiveValue(player) * 1000L;
    }

    /**
     * Returns the effective duration formatted as a human-readable string.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return a formatted duration like {@code "2 hours 30 minutes"}
     */
    public @NotNull String getFormattedEffectiveDuration(@Nullable Player player) {
        return formatSeconds(getEffectiveValue(player));
    }

    /**
     * Returns the configured maximum duration in seconds, or {@code null} when unbounded.
     *
     * @return the upper bound, or {@code null}
     */
    public @Nullable Long getMaxDuration() {
        return maxDuration;
    }

    /**
     * Returns the configured minimum duration in seconds, or {@code null} when unbounded.
     *
     * @return the lower bound, or {@code null}
     */
    public @Nullable Long getMinDuration() {
        return minDuration;
    }

    /**
     * Checks whether a duration value falls within the configured bounds.
     *
     * @param seconds the value to check in seconds
     * @return {@code true} when the value is within bounds (or no bounds are set)
     */
    public boolean isDurationWithinBounds(long seconds) {
        if (minDuration != null && seconds < minDuration) {
            return false;
        }
        return maxDuration == null || seconds <= maxDuration;
    }

    // ── Abstract hook implementations ─────────────────────────────────────────

    @Override
    protected @NotNull Long getDefaultValue() {
        return defaultDuration != null ? defaultDuration.getSeconds() : 0L;
    }

    @Override
    protected @NotNull Map<String, Long> getPermissionValues() {
        if (permissionDurations == null || permissionDurations.isEmpty()) {
            return new HashMap<>();
        }
        return permissionDurations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getSeconds()
                ));
    }

    @Override
    protected boolean getDefaultUseBestValue() {
        return true;
    }

    @Override
    protected @NotNull Long chooseBestValue(@NotNull Long current, @NotNull Long candidate) {
        return Math.max(current, candidate);
    }

    @Override
    protected boolean isBetterValue(@NotNull Long candidate, @NotNull Long current) {
        return candidate > current;
    }

    // ── Overridable hooks ─────────────────────────────────────────────────────

    @Override
    protected @NotNull Long applyBounds(@NotNull Long value) {
        var result = value;
        if (minDuration != null && result < minDuration) {
            result = minDuration;
        }
        if (maxDuration != null && result > maxDuration) {
            result = maxDuration;
        }
        return result;
    }

    @Override
    protected boolean isValidValue(@Nullable Long value) {
        return value != null && value >= 0L;
    }

    @Override
    protected void performAdditionalValidation() {
        if (minDuration != null && maxDuration != null && minDuration > maxDuration) {
            throw new IllegalStateException(
                    "minDuration (" + minDuration + ") cannot exceed maxDuration ("
                            + maxDuration + ")");
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private @NotNull String formatSeconds(long totalSeconds) {
        if (totalSeconds == 0L) {
            return "0 seconds";
        }

        var sb = new StringBuilder();
        var d = totalSeconds / 86400L;
        var h = (totalSeconds % 86400L) / 3600L;
        var m = (totalSeconds % 3600L) / 60L;
        var s = totalSeconds % 60L;

        if (d > 0) {
            sb.append(d).append(d == 1 ? " day" : " days");
        }
        if (h > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(h).append(h == 1 ? " hour" : " hours");
        }
        if (m > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(m).append(m == 1 ? " minute" : " minutes");
        }
        if (s > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(s).append(s == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }
}
