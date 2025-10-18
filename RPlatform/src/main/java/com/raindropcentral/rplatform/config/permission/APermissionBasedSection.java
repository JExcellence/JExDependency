package com.raindropcentral.rplatform.config.permission;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for permission-based configuration sections.
 * <p>
 * This class provides common functionality for sections that need to resolve
 * values based on player permissions, such as cooldowns, durations, amplifiers, etc.
 * It handles permission extraction, wildcard matching, and effective value calculation.
 * </p>
 *
 * @param <T> the type of value this section resolves (Long, Integer, etc.)
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public abstract class APermissionBasedSection<T> extends AConfigSection {
    
    /**
     * Whether this permission-based system is enabled at all.
     * YAML key: "enabled"
     */
    private Boolean enabled;
    
    /**
     * Whether to use the "best" value when a player has multiple applicable permissions.
     * The definition of "best" depends on the implementation (longest duration, shortest cooldown, etc.).
     * YAML key: "useBestValue"
     */
    private Boolean useBestValue;
    
    /**
     * Constructs a new APermissionBasedSection.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    protected APermissionBasedSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Gets whether this permission-based system is enabled.
     *
     * @return true if enabled, false otherwise. Defaults to true.
     */
    public Boolean getEnabled() {
        return this.enabled != null ? this.enabled : true;
    }
    
    /**
     * Gets whether to use the "best" value when multiple permissions apply.
     *
     * @return true to use best value, false to use first match. Defaults to implementation-specific.
     */
    public Boolean getUseBestValue() {
        return this.useBestValue != null ? this.useBestValue : getDefaultUseBestValue();
    }
    
    /**
     * Gets the effective value for a player based on their permissions.
     * This method automatically extracts the player's permissions and determines
     * the appropriate value.
     *
     * @param player the player to check value for
     * @return the effective value
     */
    public T getEffectiveValue(final Player player) {
        if (
            player == null
        ) {
            return this.getDefaultValue();
        }
        
        return this.getEffectiveValue(
            this.extractPlayerPermissions(player)
        );
    }
    
    /**
     * Gets the effective value based on a set of permissions.
     *
     * @param playerPermissions the permissions to check
     * @return the effective value
     */
    public T getEffectiveValue(
        final Set<String> playerPermissions
    ) {
        if (
            ! this.getEnabled()
        ) {
            return this.getDefaultValue();
        }
        
        if (
            playerPermissions == null ||
            playerPermissions.isEmpty()
        ) {
            return this.getDefaultValue();
        }
        
        final Map<String, T> permissionValues = this.getPermissionValues();
        if (
            permissionValues.isEmpty()
        ) {
            return this.getDefaultValue();
        }
        
        T effectiveValue = null;
        
        for (
            final String permission : playerPermissions
        ) {
            final T value = permissionValues.get(permission);
            if (
                value != null
            ) {
                if (
                    effectiveValue == null
                ) {
                    effectiveValue = value;
                } else if (
                    this.getUseBestValue()
                ) {
                    effectiveValue = this.chooseBestValue(effectiveValue, value);
                }
            }
        }
        
        T result = effectiveValue != null ? effectiveValue : this.getDefaultValue();
        
        return this.applyBounds(result);
    }
    
    /**
     * Gets the specific permission that determines the player's value.
     *
     * @param player the player to check
     * @return the permission that determines the value, or null if using default
     */
    public String getEffectivePermission(
        final Player player
    ) {
        if (
            player == null
        ) {
            return null;
        }
        
        final Set<String> playerPermissions = this.extractPlayerPermissions(player);
        final Map<String, T> permissionValues = this.getPermissionValues();
        
        if (
            permissionValues.isEmpty()
        ) {
            return null;
        }
        
        String effectivePermission = null;
        T effectiveValue = null;
        
        for (
            final String permission : playerPermissions) {
            final T value = permissionValues.get(permission);
            if (
                value != null
            ) {
                if (
                    effectiveValue == null
                ) {
                    effectivePermission = permission;
                    effectiveValue = value;
                } else if (
                    this.getUseBestValue() &&
                    this.isBetterValue(value, effectiveValue)
                ) {
                    effectivePermission = permission;
                    effectiveValue = value;
                } else if (
                    ! this.getUseBestValue()
                ) {
                    break;
                }
            }
        }
        
        return effectivePermission;
    }
    
    /**
     * Checks if a player has any permissions that would affect their value.
     *
     * @param player the player to check
     * @return true if the player has any relevant permissions, false otherwise
     */
    public boolean hasRelevantPermissions(
        final Player player
    ) {
        if (
            player == null
        ) {
            return false;
        }
        
        final Set<String> playerPermissions = this.extractPlayerPermissions(player);
        final Map<String, T> permissionValues = this.getPermissionValues();
        
        for (
            final String permission : playerPermissions
        ) {
            if (
                permissionValues.containsKey(permission))
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the value for a specific permission.
     *
     * @param permission the permission to check
     * @return the value for the permission, or null if not found
     */
    public T getValueForPermission(
        final String permission
    ) {
        if (
            permission == null ||
            permission.trim().isEmpty())
        {
            return null;
        }
        
        final Map<String, T> values = this.getPermissionValues();
        return values.get(permission);
    }
    
    /**
     * Checks if any permission-specific values are configured.
     *
     * @return true if permission values exist, false otherwise
     */
    public boolean hasPermissionValues() {
        return ! this.getPermissionValues().isEmpty();
    }
    
    /**
     * Extracts all permissions from a player that are relevant to this configuration.
     * This includes both explicit permissions and wildcard permissions.
     *
     * @param player the player to extract permissions from
     * @return a set of relevant permissions
     */
    protected Set<String> extractPlayerPermissions(
        final Player player
    ) {
        final Set<String> relevantPermissions = new HashSet<>();
        final Map<String, T> configuredValues = this.getPermissionValues();
        
        if (
            configuredValues.isEmpty())
        {
            return relevantPermissions;
        }
        
        final Set<PermissionAttachmentInfo> playerPermissions = player.getEffectivePermissions();
        
        for (
            final PermissionAttachmentInfo permissionInfo : playerPermissions
        ) {
            if (
                ! permissionInfo.getValue())
            {
                continue;
            }
            
            final String permission = permissionInfo.getPermission();
            
            if (
                configuredValues.containsKey(permission))
            {
                relevantPermissions.add(permission);
                continue;
            }
            
            for (
                final String configuredPermission : configuredValues.keySet()
            ) {
                if (
                    this.matchesWildcard(permission, configuredPermission))
                {
                    relevantPermissions.add(configuredPermission);
                }
            }
        }
        
        return relevantPermissions;
    }
    
    /**
     * Checks if a player permission matches a configured permission pattern.
     * Supports basic wildcard matching with '*' at the end of permissions.
     *
     * @param playerPermission     the permission the player has
     * @param configuredPermission the permission pattern from configuration
     * @return true if the permissions match, false otherwise
     */
    protected boolean matchesWildcard(
        final String playerPermission,
        final String configuredPermission
    ) {
        if (
            playerPermission == null ||
            configuredPermission == null
        ) {
            return false;
        }
        
        if (
            playerPermission.equals(configuredPermission))
        {
            return true;
        }
        
        if (
            configuredPermission.endsWith("*"))
        {
            final String prefix = configuredPermission.substring(0, configuredPermission.length() - 1);
            return playerPermission.startsWith(prefix);
        }
        
        if (
            playerPermission.endsWith("*"))
        {
            final String prefix = playerPermission.substring(0, playerPermission.length() - 1);
            return configuredPermission.startsWith(prefix);
        }
        
        return false;
    }
    
    /**
     * Validates the configuration of this section.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validate() {
        final T defaultValue = this.getDefaultValue();
        if (
            ! this.isValidValue(defaultValue))
        {
            throw new IllegalStateException("Invalid default value: " + defaultValue);
        }
        
        final Map<String, T> permissionValues = this.getPermissionValues();
        for (
            final Map.Entry<String, T> entry : permissionValues.entrySet()
        ) {
            if (
                entry.getKey() == null ||
                entry.getKey().trim().isEmpty())
            {
                throw new IllegalStateException("Permission cannot be null or empty");
            }
            
            if (
                ! this.isValidValue(entry.getValue()))
            {
                throw new IllegalStateException("Invalid value for permission '" + entry.getKey() + "': " + entry.getValue());
            }
        }
        
        this.performAdditionalValidation();
    }
    
    
    /**
     * Gets the default value when no permissions match.
     *
     * @return the default value
     */
    protected abstract T getDefaultValue();
    
    /**
     * Gets the map of permission to value mappings.
     *
     * @return the permission values map
     */
    protected abstract Map<String, T> getPermissionValues();
    
    /**
     * Gets the default value for the "use best value" setting.
     * Different implementations may have different defaults.
     *
     * @return the default use best value setting
     */
    protected abstract boolean getDefaultUseBestValue();
    
    /**
     * Chooses the better value between two options when multiple permissions apply.
     * For example, longest duration, shortest cooldown, highest amplifier, etc.
     *
     * @param current the current best value
     * @param candidate the candidate value to compare
     * @return the better value
     */
    protected abstract T chooseBestValue(
        T current,
        T candidate
    );
    
    /**
     * Checks if one value is better than another.
     * This is used for determining the effective permission.
     *
     * @param candidate the candidate value
     * @param current the current value
     * @return true if candidate is better than current
     */
    protected abstract boolean isBetterValue(
        T candidate,
        T current
    );
    
    /**
     * Applies bounds/constraints to a value if supported by the implementation.
     * Default implementation returns the value unchanged.
     *
     * @param value the value to apply bounds to
     * @return the bounded value
     */
    protected T applyBounds(
        final T value
    ) {
        return value;
    }
    
    /**
     * Checks if a value is valid for this section type.
     * Default implementation checks for null and performs basic validation.
     *
     * @param value the value to validate
     * @return true if valid, false otherwise
     */
    protected boolean isValidValue(
        final T value
    ) {
        return value != null;
    }
    
    /**
     * Performs additional validation specific to the implementation.
     * Default implementation does nothing.
     *
     * @throws IllegalStateException if validation fails
     */
    protected void performAdditionalValidation() {
    }
}