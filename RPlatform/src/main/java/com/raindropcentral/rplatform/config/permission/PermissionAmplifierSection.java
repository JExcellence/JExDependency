package com.raindropcentral.rplatform.config.permission;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration section for permission-based amplifiers.
 * <p>
 * This section handles amplifier configuration based on player permissions,
 * allowing different amplifier levels for different permission groups.
 * It supports a default amplifier for players without specific permissions
 * and permission-specific overrides.
 * </p>
 *
 * @author ItsRainingHP
 * @version 2.0.0
 * @since TBD
 */
@CSAlways
public class PermissionAmplifierSection extends APermissionBasedSection<Integer> {
    
    /**
     * Default amplifier level for players without specific permissions.
     * YAML key: "defaultAmplifier"
     */
    private Integer defaultAmplifier;
    
    /**
     * Map of permissions to their specific amplifier levels.
     * YAML key: "permissionAmplifiers"
     */
    private Map<String, Integer> permissionAmplifiers;
    
    /**
     * Maximum allowed amplifier level to prevent overpowered effects.
     * YAML key: "maxAmplifier"
     */
    private Integer maxAmplifier;
    
    /**
     * Minimum allowed amplifier level.
     * YAML key: "minAmplifier"
     */
    private Integer minAmplifier;
    
    /**
     * Constructs a new PermissionAmplifierSection.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public PermissionAmplifierSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Gets the default amplifier level.
     *
     * @return the default amplifier level, or 0 if not specified
     */
    public Integer getDefaultAmplifier() {
        return this.getDefaultValue();
    }
    
    /**
     * Gets the map of permission-specific amplifiers.
     *
     * @return the map of permission to amplifier level
     */
    public Map<String, Integer> getPermissionAmplifiers() {
        return this.getPermissionValues();
    }
    
    /**
     * Gets the amplifier for a specific permission.
     *
     * @param permission the permission to check
     * @return the amplifier level for the permission, or null if not found
     */
    public Integer getAmplifierForPermission(
        final String permission
    ) {
        return this.getValueForPermission(permission);
    }
    
    /**
     * Checks if any permission-specific amplifiers are configured.
     *
     * @return true if permission amplifiers exist, false otherwise
     */
    public boolean hasPermissionAmplifiers() {
        return this.hasPermissionValues();
    }
    
    /**
     * Gets the effective amplifier for a player based on their permissions.
     *
     * @param player the player to check amplifier for
     * @return the effective amplifier level
     */
    public Integer getEffectiveAmplifier(
        final Player player
    ) {
        return this.getEffectiveValue(player);
    }
    
    /**
     * Gets the effective amplifier based on a set of permissions.
     *
     * @param playerPermissions the permissions to check
     * @return the effective amplifier level
     */
    public Integer getEffectiveAmplifier(
        final Set<String> playerPermissions
    ) {
        return this.getEffectiveValue(playerPermissions);
    }
    
    /**
     * Gets the maximum allowed amplifier level.
     *
     * @return the maximum amplifier level, or null if no limit
     */
    public Integer getMaxAmplifier() {
        return this.maxAmplifier;
    }
    
    /**
     * Gets the minimum allowed amplifier level.
     *
     * @return the minimum amplifier level, or null if no limit
     */
    public Integer getMinAmplifier() {
        return this.minAmplifier;
    }
    
    /**
     * Checks if the given amplifier is within the configured bounds.
     *
     * @param amplifier the amplifier level to check
     * @return true if the amplifier is within bounds, false otherwise
     */
    public boolean isAmplifierWithinBounds(
        final Integer amplifier
    ) {
        if (
            amplifier == null
        ) {
            return false;
        }
        
        if (
            this.minAmplifier != null &&
            amplifier < this.minAmplifier
        ) {
            return false;
        }
        
        if (
            this.maxAmplifier != null &&
            amplifier > this.maxAmplifier
        ) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Clamps an amplifier value to the configured bounds.
     *
     * @param amplifier the amplifier level to clamp
     * @return the clamped amplifier level
     */
    public Integer clampAmplifier(
        final Integer amplifier
    ) {
        if (
            amplifier == null
        ) {
            return this.getDefaultAmplifier();
        }
        
        Integer result = amplifier;
        
        if (
            this.minAmplifier != null &&
            result < this.minAmplifier
        ) {
            result = this.minAmplifier;
        }
        
        if (
            this.maxAmplifier != null &&
            result > this.maxAmplifier
        ) {
            result = this.maxAmplifier;
        }
        
        return result;
    }
    
    @Override
    protected Integer getDefaultValue() {
        if (
            this.defaultAmplifier != null
        ) {
            return this.defaultAmplifier;
        }
        
        return 0;
    }
    
    @Override
    protected Map<String, Integer> getPermissionValues() {
        final Map<String, Integer> amplifiers = new HashMap<>();
        
        if (
            this.permissionAmplifiers != null
        ) {
            amplifiers.putAll(this.permissionAmplifiers);
        }
        
        return amplifiers;
    }
    
    @Override
    protected boolean getDefaultUseBestValue() {
        return true;
    }
    
    @Override
    protected Integer chooseBestValue(
        final Integer current,
        final Integer candidate
    ) {
        return Math.max(current, candidate);
    }
    
    @Override
    protected boolean isBetterValue(
        final Integer candidate,
        final Integer current
    ) {
        return candidate > current;
    }
    
    @Override
    protected Integer applyBounds(
        final Integer value
    ) {
        return this.clampAmplifier(value);
    }
    
    @Override
    protected boolean isValidValue(
        final Integer value
    ) {
        return value != null && value >= 0;
    }
    
    @Override
    protected void performAdditionalValidation() {
        if (
            this.minAmplifier != null &&
            this.minAmplifier < 0
        ) {
            throw new IllegalStateException("Minimum amplifier cannot be negative: " + this.minAmplifier);
        }
        
        if (
            this.maxAmplifier != null &&
            this.maxAmplifier < 0
        ) {
            throw new IllegalStateException("Maximum amplifier cannot be negative: " + this.maxAmplifier);
        }
        
        if (
            this.minAmplifier != null &&
            this.maxAmplifier != null &&
            this.minAmplifier > this.maxAmplifier
        ) {
            throw new IllegalStateException("Minimum amplifier (" + this.minAmplifier + ") cannot be greater than maximum amplifier (" + this.maxAmplifier + ")");
        }
    }
}