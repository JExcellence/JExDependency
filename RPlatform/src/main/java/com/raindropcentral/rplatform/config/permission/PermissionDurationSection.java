package com.raindropcentral.rplatform.config.permission;

import com.raindropcentral.rplatform.config.DurationSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration section that maps permissions to effect durations by leveraging {@link DurationSection}.
 * <p>
 * Administrators can combine structured duration keys with free-form strings to describe how long a
 * perk lasts for each permission tier. Optional minimum and maximum bounds ensure durations remain
 * within acceptable limits while the resolver selects the longest applicable duration when multiple
 * permissions apply.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class PermissionDurationSection extends APermissionBasedSection<Long> {

    /**
     * Default duration descriptor from {@code defaultDuration} used when no permissions apply.
     */
    private DurationSection defaultDuration;

    /**
     * Map of permission nodes to {@link DurationSection} overrides sourced from {@code permissionDurations}.
     */
    private Map<String, DurationSection> permissionDurations;

    /**
     * Optional duration bound taken from {@code maxDuration} limiting the longest allowed effect.
     */
    private DurationSection maxDuration;

    /**
     * Optional duration bound from {@code minDuration} ensuring effects meet a minimum length.
     */
    private DurationSection minDuration;

    /**
     * Creates the permission-aware duration section.
     *
     * @param evaluationEnvironmentBuilder mapper environment shared across sections
     */
    public PermissionDurationSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Returns the default duration in seconds applied when no permission override matches.
     *
     * @return default duration in seconds or {@code 0L}
     */
    public Long getDefaultDurationSeconds() {
        return this.getDefaultValue();
    }

    /**
     * Provides access to the configured default {@link DurationSection}.
     *
     * @return configured duration section or an empty placeholder when absent
     */
    public DurationSection getDefaultDuration() {
        return
            this.defaultDuration != null ?
            this.defaultDuration :
            new DurationSection(new EvaluationEnvironmentBuilder());
    }
    
    /**
     * Exposes the permission overrides as raw second values.
     *
     * @return map of permission nodes to duration values in seconds
     */
    public Map<String, Long> getPermissionDurationsSeconds() {
        return this.getPermissionValues();
    }

    /**
     * Returns the {@link DurationSection} instances for each configured permission node.
     *
     * @return map linking permissions to fully parsed duration sections
     */
    public Map<String, DurationSection> getPermissionDurations() {
        final Map<String, DurationSection> durations = new HashMap<>();

        if (
            this.permissionDurations != null
        ) {
            durations.putAll(this.permissionDurations);
        }
        
        return durations;
    }
    
    /**
     * Returns the maximum allowed duration in seconds if configured.
     *
     * @return maximum duration or {@code null} when no limit is set
     */
    public Long getMaxDurationSeconds() {
        if (
            this.maxDuration != null &&
            this.maxDuration.hasDuration()
        ) {
            return this.maxDuration.getSeconds();
        }
        return null;
    }

    /**
     * Returns the minimum allowed duration in seconds if configured.
     *
     * @return minimum duration or {@code null} when no limit is set
     */
    public Long getMinDurationSeconds() {
        if (
            this.minDuration != null &&
            this.minDuration.hasDuration()
        ) {
            return this.minDuration.getSeconds();
        }
        return null;
    }
    
    /**
     * Provides the raw {@link DurationSection} describing the maximum duration.
     *
     * @return duration section enforcing the maximum, or {@code null}
     */
    public DurationSection getMaxDuration() {
        return this.maxDuration;
    }

    /**
     * Provides the raw {@link DurationSection} describing the minimum duration.
     *
     * @return duration section enforcing the minimum, or {@code null}
     */
    public DurationSection getMinDuration() {
        return this.minDuration;
    }

    /**
     * Calculates the duration to apply for the supplied player.
     *
     * @param player player whose duration should be computed
     * @return resolved duration in seconds after applying bounds
     */
    public Long getEffectiveDuration(
        final Player player
    ) {
        return this.getEffectiveValue(player);
    }

    /**
     * Calculates the duration for a pre-collected permission set.
     *
     * @param playerPermissions permissions associated with the player
     * @return resolved duration in seconds after applying bounds
     */
    public Long getEffectiveDuration(final Set<String> playerPermissions) {
        return this.getEffectiveValue(playerPermissions);
    }

    /**
     * Generates a formatted duration string for the supplied player, prioritizing configured
     * {@link DurationSection} formatting when available.
     *
     * @param player player whose permissions should be evaluated
     * @return human-readable representation of the effective duration
     */
    public String getFormattedEffectiveDuration(
        final Player player
    ) {
        if (
            player == null
        ) {
            return this.getFormattedEffectiveDuration((Set<String>) null);
        }
        
        final Set<String> playerPermissions = this.extractPlayerPermissions(player);
        return this.getFormattedEffectiveDuration(playerPermissions);
    }
    
    /**
     * Generates a formatted duration string for a pre-collected permission set.
     *
     * @param playerPermissions permissions already associated with the player
     * @return human-readable representation of the effective duration
     */
    public String getFormattedEffectiveDuration(
        final Set<String> playerPermissions
    ) {
        final Long seconds = this.getEffectiveDuration(playerPermissions);
        if (
            seconds == 0L
        ) {
            return "Permanent";
        }
        
        if (
            playerPermissions != null
        ) {
            final Map<String, DurationSection> durations = this.getPermissionDurations();
            for (
                final String permission : playerPermissions
            ) {
                final DurationSection duration = durations.get(permission);
                if (
                    duration != null &&
                    duration.hasDuration()
                ) {
                    return duration.getFormattedDuration();
                }
            }
        }
        
        final DurationSection defaultDuration = this.getDefaultDuration();
        if (
            defaultDuration != null &&
            defaultDuration.hasDuration()
        ) {
            return defaultDuration.getFormattedDuration();
        }
        
        return this.formatSeconds(seconds);
    }
    
    /**
     * Retrieves the duration in seconds associated with a specific permission node.
     *
     * @param permission permission node to inspect
     * @return configured duration in seconds or {@code null}
     */
    public Long getDurationForPermission(final String permission) {
        return this.getValueForPermission(permission);
    }

    /**
     * Retrieves the {@link DurationSection} configured for the supplied permission node.
     *
     * @param permission permission node to inspect
     * @return duration section when present, otherwise {@code null}
     */
    public DurationSection getDurationSectionForPermission(
        final String permission
    ) {
        if (
            permission == null ||
            permission.trim().isEmpty()
        ) {
            return null;
        }
        
        final Map<String, DurationSection> durations = this.getPermissionDurations();
        return durations.get(permission);
    }
    
    /**
     * Indicates whether at least one permission-specific duration override exists.
     *
     * @return {@code true} when overrides have been configured
     */
    public boolean hasPermissionDurations() {
        return this.hasPermissionValues();
    }

    /**
     * Determines whether a duration falls within the configured minimum and maximum bounds.
     *
     * @param durationSeconds duration in seconds to test
     * @return {@code true} when {@code durationSeconds} satisfies the configured constraints
     */
    public boolean isDurationWithinBounds(
        final Long durationSeconds
    ) {
        if (
            durationSeconds == null
        ) {
            return false;
        }
        
        final Long minDuration = this.getMinDurationSeconds();
        final Long maxDuration = this.getMaxDurationSeconds();
        
        if (
            minDuration != null &&
            durationSeconds < minDuration
        ) {
            return false;
        }
        
        if (
            maxDuration != null &&
            durationSeconds > maxDuration
        ) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Clamps the supplied duration to the configured minimum and maximum bounds.
     *
     * @param durationSeconds duration in seconds to clamp; may be {@code null}
     * @return duration in seconds after clamping or the default duration when {@code null}
     */
    public Long clampDuration(
        final Long durationSeconds
    ) {
        if (
            durationSeconds == null
        ) {
            return this.getDefaultDurationSeconds();
        }
        
        Long result = durationSeconds;
        
        final Long minDuration = this.getMinDurationSeconds();
        final Long maxDuration = this.getMaxDurationSeconds();
        
        if (
            minDuration != null &&
            result < minDuration
        ) {
            result = minDuration;
        }
        
        if (
            maxDuration != null &&
            result > maxDuration
        ) {
            result = maxDuration;
        }
        
        return result;
    }
    
    
    /**
     * Provides the default duration in seconds when no permission override applies.
     *
     * @return default duration or {@code 0L}
     */
    @Override
    protected Long getDefaultValue() {
        if (
            this.defaultDuration != null &&
            this.defaultDuration.hasDuration()
        ) {
            return this.defaultDuration.getSeconds();
        }
        return 0L;
    }

    /**
     * Supplies the permission-to-duration map expressed in seconds.
     *
     * @return mutable copy of permission overrides converted to seconds
     */
    @Override
    protected Map<String, Long> getPermissionValues() {
        final Map<String, Long> durations = new HashMap<>();

        if (
            this.permissionDurations != null
        ) {
            for (
                final Map.Entry<String, DurationSection> entry : this.permissionDurations.entrySet()
            ) {
                if (
                    entry.getValue() != null
                ) {
                    durations.put(entry.getKey(), entry.getValue().getSeconds());
                }
            }
        }
        
        return durations;
    }

    /**
     * Uses the "best" strategy so longer durations win when multiple permissions match.
     *
     * @return {@code true} indicating the resolver should search for the optimal value
     */
    @Override
    protected boolean getDefaultUseBestValue() {

        return true;
    }

    /**
     * Chooses the longer duration when multiple permissions apply.
     *
     * @param current   current best duration in seconds
     * @param candidate candidate duration in seconds
     * @return longer of {@code current} and {@code candidate}
     */
    @Override
    protected Long chooseBestValue(
        final Long current,
        final Long candidate
    ) {
        return Math.max(current, candidate);
    }

    /**
     * Determines whether the candidate duration is longer than the current best.
     *
     * @param candidate candidate duration in seconds
     * @param current   current best duration in seconds
     * @return {@code true} when {@code candidate} extends the duration
     */
    @Override
    protected boolean isBetterValue(
        final Long candidate,
        final Long current
    ) {
        return candidate > current;
    }

    /**
     * Applies configured bounds to the resolved duration.
     *
     * @param value duration in seconds prior to clamping
     * @return duration in seconds after enforcing bounds
     */
    @Override
    protected Long applyBounds(
        final Long value
    ) {
        return this.clampDuration(value);
    }

    /**
     * Ensures duration values are non-null and non-negative.
     *
     * @param value duration in seconds to validate
     * @return {@code true} when {@code value} is zero or positive
     */
    @Override
    protected boolean isValidValue(
        final Long value
    ) {
        return
            value != null &&
            value >= 0;
    }

    /**
     * Validates duration sections and ensures configured bounds are coherent.
     *
     * @throws IllegalStateException when bounds are negative, inverted, or nested duration sections fail validation
     */
    @Override
    protected void performAdditionalValidation() {
        if (
            this.defaultDuration != null
        ) {
            this.defaultDuration.validate();
        }
        
        if (
            this.minDuration != null
        ) {
            this.minDuration.validate();
        }
        
        if (
            this.maxDuration != null
        ) {
            this.maxDuration.validate();
        }
        
        final Long minDuration = this.getMinDurationSeconds();
        final Long maxDuration = this.getMaxDurationSeconds();
        
        if (
            minDuration != null &&
            minDuration < 0
        ) {
            throw new IllegalStateException("Minimum duration cannot be negative: " + minDuration);
        }
        
        if (
            maxDuration != null &&
            maxDuration < 0
        ) {
            throw new IllegalStateException("Maximum duration cannot be negative: " + maxDuration);
        }
        
        if (
            minDuration != null &&
            maxDuration != null &&
            minDuration > maxDuration
        ) {
            throw new IllegalStateException("Minimum duration (" + minDuration + ") cannot be greater than maximum duration (" + maxDuration + ")");
        }
        
        final Map<String, DurationSection> durations = this.getPermissionDurations();
        for (
            final Map.Entry<String, DurationSection> entry : durations.entrySet()
        ) {
            if (
                entry.getValue() != null
            ) {
                entry.getValue().validate();
            }
        }
    }
    
    /**
     * Formats seconds into a human-readable string.
     *
     * @param totalSeconds the total seconds to format
     * @return formatted duration string
     */
    private String formatSeconds(
        final long totalSeconds
    ) {
        if (
            totalSeconds == 0L
        ) {
            return "Permanent";
        }
        
        final StringBuilder sb = new StringBuilder();
        
        final long days = totalSeconds / 86400L;
        final long hours = (totalSeconds % 86400L) / 3600L;
        final long minutes = (totalSeconds % 3600L) / 60L;
        final long seconds = totalSeconds % 60L;
        
        if (
            days > 0
        ) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        
        if (
            hours > 0
        ) {
            if (
                !sb.isEmpty()
            ) {
                sb.append(" ");
            }
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        
        if (
            minutes > 0
        ) {
            if (
                !sb.isEmpty()
            ) {
                sb.append(" ");
            }
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        
        if (
            seconds > 0
        ) {
            if (
                !sb.isEmpty()
            ) {
                sb.append(" ");
            }
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        
        return sb.toString();
    }
}