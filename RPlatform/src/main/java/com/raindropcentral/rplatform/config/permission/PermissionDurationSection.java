package com.raindropcentral.rplatform.config.permission;

import com.raindropcentral.rplatform.config.DurationSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration section for permission-based perk effect durations.
 * <p>
 * This section handles duration configuration based on player permissions,
 * allowing different effect durations for different permission groups.
 * It supports a default duration for players without specific permissions
 * and permission-specific overrides using flexible duration parsing.
 * </p>
 * <p>
 * This is used for perks that have time-limited effects, where different
 * permission levels can grant longer or shorter effect durations.
 * </p>
 *
 * @author ItsRainingHP
 * @version 2.0.0
 * @since TBD
 */
@CSAlways
public class PermissionDurationSection extends APermissionBasedSection<Long> {
    
    /**
     * Default effect duration for players without specific permissions.
     * YAML key: "defaultDuration"
     */
    private DurationSection defaultDuration;
    
    /**
     * Map of permissions to their specific effect durations.
     * YAML key: "permissionDurations"
     */
    private Map<String, DurationSection> permissionDurations;
    
    /**
     * Maximum allowed duration to prevent overpowered effects.
     * YAML key: "maxDuration"
     */
    private DurationSection maxDuration;
    
    /**
     * Minimum allowed duration.
     * YAML key: "minDuration"
     */
    private DurationSection minDuration;
    
    /**
     * Constructs a new PermissionDurationSection.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public PermissionDurationSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Gets the default duration in seconds.
     *
     * @return the default duration in seconds, or 0 if not specified
     */
    public Long getDefaultDurationSeconds() {
        return this.getDefaultValue();
    }
    
    /**
     * Gets the default duration section.
     *
     * @return the default duration section, or a new empty one if not configured
     */
    public DurationSection getDefaultDuration() {
        return
            this.defaultDuration != null ?
            this.defaultDuration :
            new DurationSection(new EvaluationEnvironmentBuilder());
    }
    
    /**
     * Gets the map of permission-specific durations in seconds.
     *
     * @return the map of permission to duration in seconds
     */
    public Map<String, Long> getPermissionDurationsSeconds() {
        return this.getPermissionValues();
    }
    
    /**
     * Gets the map of permission-specific duration sections.
     *
     * @return the map of permission to duration sections
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
     * Gets the maximum allowed duration in seconds.
     *
     * @return the maximum duration in seconds, or null if no limit
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
     * Gets the minimum allowed duration in seconds.
     *
     * @return the minimum duration in seconds, or null if no limit
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
     * Gets the maximum duration section.
     *
     * @return the maximum duration section, or null if not configured
     */
    public DurationSection getMaxDuration() {
        return this.maxDuration;
    }
    
    /**
     * Gets the minimum duration section.
     *
     * @return the minimum duration section, or null if not configured
     */
    public DurationSection getMinDuration() {
        return this.minDuration;
    }
    
    /**
     * Gets the effective duration for a player based on their permissions.
     *
     * @param player the player to check duration for
     * @return the effective duration in seconds
     */
    public Long getEffectiveDuration(
        final Player player
    ) {
        return this.getEffectiveValue(player);
    }
    
    /**
     * Gets the effective duration based on a set of permissions.
     *
     * @param playerPermissions the permissions to check
     * @return the effective duration in seconds
     */
    public Long getEffectiveDuration(final Set<String> playerPermissions) {
        return this.getEffectiveValue(playerPermissions);
    }
    
    /**
     * Gets a formatted string representation of the effective duration for a player.
     *
     * @param player the player to check duration for
     * @return formatted duration string
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
     * Gets a formatted string representation of the effective duration.
     *
     * @param playerPermissions the permissions the player has
     * @return formatted duration string
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
     * Gets the duration for a specific permission.
     *
     * @param permission the permission to check
     * @return the duration in seconds for the permission, or null if not found
     */
    public Long getDurationForPermission(final String permission) {
        return this.getValueForPermission(permission);
    }
    
    /**
     * Gets the duration section for a specific permission.
     *
     * @param permission the permission to check
     * @return the duration section for the permission, or null if not found
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
     * Checks if any permission-specific durations are configured.
     *
     * @return true if permission durations exist, false otherwise
     */
    public boolean hasPermissionDurations() {
        return this.hasPermissionValues();
    }
    
    /**
     * Checks if the given duration is within the configured bounds.
     *
     * @param durationSeconds the duration in seconds to check
     * @return true if the duration is within bounds, false otherwise
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
     * Clamps a duration value to the configured bounds.
     *
     * @param durationSeconds the duration in seconds to clamp
     * @return the clamped duration in seconds
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
    
    @Override
    protected boolean getDefaultUseBestValue() {
        
        return true;
    }
    
    @Override
    protected Long chooseBestValue(
        final Long current,
        final Long candidate
    ) {
        return Math.max(current, candidate);
    }
    
    @Override
    protected boolean isBetterValue(
        final Long candidate,
        final Long current
    ) {
        return candidate > current;
    }
    
    @Override
    protected Long applyBounds(
        final Long value
    ) {
        return this.clampDuration(value);
    }
    
    @Override
    protected boolean isValidValue(
        final Long value
    ) {
        return
            value != null &&
            value >= 0;
    }
    
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