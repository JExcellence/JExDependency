/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
 * Abstract base for configuration sections that expose values gated by permissions.
 *
 * <p>Implementations declare a default value alongside a map of permission-specific overrides. When a
 * player is evaluated, the section inspects their effective permissions, resolves the most
 * appropriate override, and optionally applies bounds or additional validation. This shared logic is
 * used by duration, cooldown, and amplifier sections to provide consistent semantics across
 * configuration files.
 *
 * @param <T> numeric or comparable type returned by the configuration section
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public abstract class APermissionBasedSection<T> extends AConfigSection {

    /**
     * Toggle represented by the YAML key {@code enabled} that activates permission-aware overrides.
     */
    private Boolean enabled;

    /**
     * Flag from {@code useBestValue} indicating whether the resolver should search for the best match.
     * or simply stop at the first applicable permission.
     */
    private Boolean useBestValue;

    /**
     * Creates a new permission driven section.
     *
     * @param evaluationEnvironmentBuilder evaluation environment shared with the config mapper
     */
    protected APermissionBasedSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Determines whether permission-aware overrides are evaluated.
     *
     * @return {@code true} when overrides are enabled; defaults to {@code true} when unset
     */
    public Boolean getEnabled() {
        return this.enabled != null ? this.enabled : true;
    }

    /**
     * Indicates if the resolver should keep searching for the best override or use the first match.
     *
     * @return {@code true} when the section prefers the best value; falls back to
     * implementation-specific defaults when unset
     */
    public Boolean getUseBestValue() {
        return this.useBestValue != null ? this.useBestValue : getDefaultUseBestValue();
    }

    /**
     * Calculates the effective value for the supplied player by extracting their permissions and.
     * delegating to {@link #getEffectiveValue(Set)}.
     *
     * @param player player whose permissions should be evaluated (may be {@code null})
     * @return resolved value or the default when the player is {@code null} or no overrides apply
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
     * Computes the effective value for a pre-resolved set of permissions.
     *
     * @param playerPermissions permissions already associated with the player
     * @return override value or the default when no override is applicable
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
     * Resolves the permission node that was responsible for the chosen value.
     *
     * @param player player whose matching permission should be returned
     * @return permission string that produced the effective value, or {@code null} when no override was used
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
     * Checks if the supplied player has at least one permission that maps to a configured override.
     *
     * @param player player whose permissions should be inspected
     * @return {@code true} when a configured permission applies
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
     * Resolves the value attached to the provided permission string.
     *
     * @param permission permission node to lookup
     * @return configured value or {@code null} when the permission is blank or missing
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
     * Indicates whether at least one permission override is configured.
     *
     * @return {@code true} when overrides are present
     */
    public boolean hasPermissionValues() {
        return ! this.getPermissionValues().isEmpty();
    }

    /**
     * Extracts all permissions that match configured nodes, including wildcard matches.
     *
     * @param player player whose permission attachments should be inspected
     * @return set of permission nodes that influence this section
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
     * Checks whether the player's permission string satisfies the configured permission pattern.
     *
     * @param playerPermission     permission granted to the player
     * @param configuredPermission permission entry from configuration which may end with {@code *}
     * @return {@code true} when the permission values match or satisfy a wildcard relationship
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
     * Validates that the default value and all permission overrides conform to the expected format.
     *
     * @throws IllegalStateException when a value is {@code null}, out of bounds, or the permission key is blank
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
