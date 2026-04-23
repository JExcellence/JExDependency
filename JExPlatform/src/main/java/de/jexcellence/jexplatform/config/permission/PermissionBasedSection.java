package de.jexcellence.jexplatform.config.permission;

import de.jexcellence.configmapper.sections.ConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base for configuration sections that expose values gated by permissions.
 *
 * <p>Implementations declare a default value alongside permission-specific overrides.
 * When a player is evaluated, their effective permissions are inspected to resolve the
 * most appropriate override, optionally applying bounds and validation. This shared
 * logic is used by duration, cooldown, and amplifier sections.
 *
 * @param <T> the value type returned by the section
 * @author JExcellence
 * @since 1.0.0
 */
@CSAlways
public abstract class PermissionBasedSection<T> extends ConfigSection {

    private Boolean enabled;
    private Boolean useBestValue;

    /**
     * Creates a permission-based section with the provided evaluation environment.
     *
     * @param evaluationEnvironmentBuilder shared expression context
     */
    protected PermissionBasedSection(
            @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Returns whether permission-aware overrides are evaluated.
     *
     * @return {@code true} when overrides are enabled (default)
     */
    public boolean getEnabled() {
        return enabled != null ? enabled : true;
    }

    /**
     * Returns whether the resolver should search for the best override value.
     *
     * @return {@code true} when best-value resolution is active
     */
    public boolean getUseBestValue() {
        return useBestValue != null ? useBestValue : getDefaultUseBestValue();
    }

    /**
     * Computes the effective value for the player by evaluating their permissions.
     *
     * @param player the player to evaluate (may be {@code null})
     * @return the resolved value, or the default when no overrides apply
     */
    public @NotNull T getEffectiveValue(@Nullable Player player) {
        if (player == null) {
            return getDefaultValue();
        }
        return getEffectiveValue(extractPlayerPermissions(player));
    }

    /**
     * Computes the effective value for a pre-resolved set of permissions.
     *
     * @param playerPermissions the player's relevant permissions
     * @return the override value, or the default when none applies
     */
    public @NotNull T getEffectiveValue(@Nullable Set<String> playerPermissions) {
        if (!getEnabled() || playerPermissions == null || playerPermissions.isEmpty()) {
            return getDefaultValue();
        }

        var permissionValues = getPermissionValues();
        if (permissionValues.isEmpty()) {
            return getDefaultValue();
        }

        T effectiveValue = null;

        for (var permission : playerPermissions) {
            var value = permissionValues.get(permission);
            if (value != null) {
                if (effectiveValue == null) {
                    effectiveValue = value;
                } else if (getUseBestValue()) {
                    effectiveValue = chooseBestValue(effectiveValue, value);
                }
            }
        }

        var result = effectiveValue != null ? effectiveValue : getDefaultValue();
        return applyBounds(result);
    }

    /**
     * Returns the permission node responsible for the effective value.
     *
     * @param player the player to evaluate
     * @return the matching permission, or {@code null} when the default is used
     */
    public @Nullable String getEffectivePermission(@Nullable Player player) {
        if (player == null) {
            return null;
        }

        var playerPermissions = extractPlayerPermissions(player);
        var permissionValues = getPermissionValues();
        if (permissionValues.isEmpty()) {
            return null;
        }

        String effectivePermission = null;
        T effectiveValue = null;

        for (var permission : playerPermissions) {
            var value = permissionValues.get(permission);
            if (value != null) {
                if (effectiveValue == null) {
                    effectivePermission = permission;
                    effectiveValue = value;
                } else if (getUseBestValue() && isBetterValue(value, effectiveValue)) {
                    effectivePermission = permission;
                    effectiveValue = value;
                } else if (!getUseBestValue()) {
                    break;
                }
            }
        }

        return effectivePermission;
    }

    /**
     * Checks whether the player has any permissions mapping to configured overrides.
     *
     * @param player the player to check
     * @return {@code true} when at least one override applies
     */
    public boolean hasRelevantPermissions(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        var playerPermissions = extractPlayerPermissions(player);
        var permissionValues = getPermissionValues();
        for (var permission : playerPermissions) {
            if (permissionValues.containsKey(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value mapped to the given permission node.
     *
     * @param permission the permission to look up
     * @return the configured value, or {@code null} when not found
     */
    public @Nullable T getValueForPermission(@Nullable String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return null;
        }
        return getPermissionValues().get(permission);
    }

    /**
     * Returns whether any permission overrides are configured.
     *
     * @return {@code true} when overrides are present
     */
    public boolean hasPermissionValues() {
        return !getPermissionValues().isEmpty();
    }

    /**
     * Validates the default value and all permission overrides.
     *
     * @throws IllegalStateException when a value is invalid or a permission key is blank
     */
    public void validate() {
        var defaultValue = getDefaultValue();
        if (!isValidValue(defaultValue)) {
            throw new IllegalStateException("Invalid default value: " + defaultValue);
        }

        for (var entry : getPermissionValues().entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new IllegalStateException("Permission cannot be null or empty");
            }
            if (!isValidValue(entry.getValue())) {
                throw new IllegalStateException(
                        "Invalid value for permission '" + entry.getKey() + "': " + entry.getValue());
            }
        }

        performAdditionalValidation();
    }

    // ── Abstract hooks ─────────────────────────────────────────────────────────

    /**
     * Returns the default value when no permissions match.
     *
     * @return the default value
     */
    protected abstract @NotNull T getDefaultValue();

    /**
     * Returns the permission-to-value override map.
     *
     * @return the permission values map
     */
    protected abstract @NotNull Map<String, T> getPermissionValues();

    /**
     * Returns the default for the best-value resolution strategy.
     *
     * @return {@code true} to search all permissions for the best value
     */
    protected abstract boolean getDefaultUseBestValue();

    /**
     * Selects the better of two values when multiple permissions apply.
     *
     * @param current   the current best value
     * @param candidate the candidate to compare
     * @return the better value
     */
    protected abstract @NotNull T chooseBestValue(@NotNull T current, @NotNull T candidate);

    /**
     * Determines whether a candidate value is better than the current best.
     *
     * @param candidate the candidate value
     * @param current   the current value
     * @return {@code true} if the candidate is strictly better
     */
    protected abstract boolean isBetterValue(@NotNull T candidate, @NotNull T current);

    // ── Overridable hooks ──────────────────────────────────────────────────────

    /**
     * Applies bounds or constraints to a resolved value.
     *
     * @param value the value to bound
     * @return the bounded value (default: unchanged)
     */
    protected @NotNull T applyBounds(@NotNull T value) {
        return value;
    }

    /**
     * Checks whether a value is valid for this section type.
     *
     * @param value the value to validate
     * @return {@code true} if valid (default: non-null)
     */
    protected boolean isValidValue(@Nullable T value) {
        return value != null;
    }

    /**
     * Hook for implementation-specific validation.
     *
     * @throws IllegalStateException when additional validation fails
     */
    protected void performAdditionalValidation() {
        // Default: no additional validation
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    /**
     * Extracts player permissions that match configured overrides, including wildcards.
     *
     * @param player the player to inspect
     * @return set of matching permission nodes
     */
    protected @NotNull Set<String> extractPlayerPermissions(@NotNull Player player) {
        var relevant = new HashSet<String>();
        var configured = getPermissionValues();
        if (configured.isEmpty()) {
            return relevant;
        }

        for (var info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            var perm = info.getPermission();
            if (configured.containsKey(perm)) {
                relevant.add(perm);
                continue;
            }
            for (var configuredPerm : configured.keySet()) {
                if (matchesWildcard(perm, configuredPerm)) {
                    relevant.add(configuredPerm);
                }
            }
        }

        return relevant;
    }

    /**
     * Checks whether a player's permission satisfies a configured pattern
     * (supports trailing {@code *} wildcards).
     *
     * @param playerPermission     the granted permission
     * @param configuredPermission the configured pattern
     * @return {@code true} on match
     */
    protected boolean matchesWildcard(@Nullable String playerPermission,
                                      @Nullable String configuredPermission) {
        if (playerPermission == null || configuredPermission == null) {
            return false;
        }
        if (playerPermission.equals(configuredPermission)) {
            return true;
        }
        if (configuredPermission.endsWith("*")) {
            var prefix = configuredPermission.substring(0, configuredPermission.length() - 1);
            return playerPermission.startsWith(prefix);
        }
        if (playerPermission.endsWith("*")) {
            var prefix = playerPermission.substring(0, playerPermission.length() - 1);
            return configuredPermission.startsWith(prefix);
        }
        return false;
    }
}
