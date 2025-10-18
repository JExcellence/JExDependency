package com.raindropcentral.rplatform.config.permission;

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration section for permission-based cooldowns.
 * <p>
 * This section handles cooldown configuration based on player permissions,
 * allowing different cooldown periods for different permission groups.
 * It supports a default cooldown for players without specific permissions
 * and permission-specific overrides.
 * </p>
 *
 * @author ItsRainingHP
 * @version 2.0.0
 * @since TBD
 */
@CSAlways
public class PermissionCooldownSection extends APermissionBasedSection<Long> {
    
    /**
     * Default cooldown period in seconds for players without specific permissions.
     * YAML key: "defaultCooldownSeconds"
     */
    private Long defaultCooldownSeconds;
    
    /**
     * Map of permissions to their specific cooldown periods in seconds.
     * YAML key: "permissionCooldowns"
     */
    private Map<String, Long> permissionCooldowns;
    
    /**
     * Constructs a new PermissionCooldownSection.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public PermissionCooldownSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Gets the default cooldown in seconds.
     *
     * @return the default cooldown in seconds, or 0 if not specified
     */
    public Long getDefaultCooldownSeconds() {
        return this.getDefaultValue();
    }
    
    /**
     * Gets the map of permission-specific cooldowns.
     *
     * @return the map of permission to cooldown in seconds
     */
    public Map<String, Long> getPermissionCooldowns() {
        return this.getPermissionValues();
    }
    
    /**
     * Gets the cooldown for a specific permission.
     *
     * @param permission the permission to check
     * @return the cooldown in seconds for the permission, or null if not found
     */
    public Long getCooldownForPermission(
        final String permission
    ) {
        return this.getValueForPermission(permission);
    }
    
    /**
     * Checks if any permission-specific cooldowns are configured.
     *
     * @return true if permission cooldowns exist, false otherwise
     */
    public boolean hasPermissionCooldowns() {
        return this.hasPermissionValues();
    }
    
    /**
     * Gets the effective cooldown for a player based on their permissions.
     *
     * @param player the player to check cooldown for
     * @return the effective cooldown in seconds
     */
    public Long getEffectiveCooldown(
        final Player player
    ) {
        return this.getEffectiveValue(player);
    }
    
    /**
     * Gets the effective cooldown based on a set of permissions.
     *
     * @param playerPermissions the permissions to check
     * @return the effective cooldown in seconds
     */
    public Long getEffectiveCooldown(
        final Set<String> playerPermissions
    ) {
        return this.getEffectiveValue(playerPermissions);
    }
    
    
    @Override
    protected Long getDefaultValue() {
        if (
            this.defaultCooldownSeconds != null
        ) {
            return this.defaultCooldownSeconds;
        }
        
        return 0L;
    }
    
    @Override
    protected Map<String, Long> getPermissionValues() {
        final Map<String, Long> cooldowns = new HashMap<>();
        
        if (
            this.permissionCooldowns != null
        ) {
            cooldowns.putAll(this.permissionCooldowns);
        }
        
        return cooldowns;
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
        if (
            current == 0L
        ) {
            return candidate;
        }
        
        if (
            candidate == 0L
        ) {
            return candidate;
        }
        
        return Math.min(current, candidate);
    }
    
    @Override
    protected boolean isBetterValue(
        final Long candidate,
        final Long current
    ) {
        if (
            candidate == 0L
        ) {
            return true;
        }
        
        if (
            current == 0L
        ) {
            return false;
        }
        
        return candidate < current;
    }
    
    @Override
    protected boolean isValidValue(
        final Long value
    ) {
        return value != null && value >= 0;
    }
}