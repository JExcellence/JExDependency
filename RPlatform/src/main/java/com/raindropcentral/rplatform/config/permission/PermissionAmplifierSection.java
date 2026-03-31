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

import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration section that maps permission nodes to potion or perk amplifier values.
 *
 * <p>Administrators can declare a default amplifier alongside permission-specific overrides, optional
 * minimum and maximum bounds, and a strategy for selecting the strongest applicable amplifier. This
 * ensures perk strength scales predictably with the permission hierarchy while preventing
 * out-of-range values.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class PermissionAmplifierSection extends APermissionBasedSection<Integer> {

    /**
     * Default amplifier level applied when no permission override matches ({@code defaultAmplifier}).
     */
    private Integer defaultAmplifier;

    /**
     * Mapping of permission nodes to amplifier values sourced from {@code permissionAmplifiers}.
     */
    private Map<String, Integer> permissionAmplifiers;

    /**
     * Optional upper bound defined via {@code maxAmplifier} to clamp runaway amplifier values.
     */
    private Integer maxAmplifier;
    /**
     * Optional lower bound defined via {@code minAmplifier} so debuffs do not drop below a floor.
     */
    private Integer minAmplifier;

    /**
     * Creates the amplifier section backed by permission-aware overrides.
     *
     * @param evaluationEnvironmentBuilder mapper environment shared across sections
     */
    public PermissionAmplifierSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Returns the amplifier level used when no permission override applies.
     *
     * @return configured default amplifier or {@code 0}
     */
    public Integer getDefaultAmplifier() {
        return this.getDefaultValue();
    }

    /**
     * Exposes the configured permission overrides.
     *
     * @return map containing permission nodes and their amplifier values
     */
    public Map<String, Integer> getPermissionAmplifiers() {
        return this.getPermissionValues();
    }

    /**
     * Looks up the amplifier associated with a specific permission string.
     *
     * @param permission permission node to query
     * @return amplifier when configured; {@code null} otherwise
     */
    public Integer getAmplifierForPermission(
        final String permission
    ) {
        return this.getValueForPermission(permission);
    }

    /**
     * Indicates whether the configuration defines any permission-specific overrides.
     *
     * @return {@code true} when at least one override exists
     */
    public boolean hasPermissionAmplifiers() {
        return this.hasPermissionValues();
    }

    /**
     * Calculates the amplifier to use for the supplied player.
     *
     * @param player player whose amplifier should be computed
     * @return resolved amplifier respecting defaults and bounds
     */
    public Integer getEffectiveAmplifier(
        final Player player
    ) {
        return this.getEffectiveValue(player);
    }

    /**
     * Calculates the amplifier for an already-collected permission set.
     *
     * @param playerPermissions permissions associated with the player
     * @return resolved amplifier respecting defaults and bounds
     */
    public Integer getEffectiveAmplifier(
        final Set<String> playerPermissions
    ) {
        return this.getEffectiveValue(playerPermissions);
    }

    /**
     * Returns the configured maximum amplifier constraint.
     *
     * @return maximum amplifier or {@code null} when the constraint is disabled
     */
    public Integer getMaxAmplifier() {
        return this.maxAmplifier;
    }

    /**
     * Returns the configured minimum amplifier constraint.
     *
     * @return minimum amplifier or {@code null} when the constraint is disabled
     */
    public Integer getMinAmplifier() {
        return this.minAmplifier;
    }

    /**
     * Determines whether the provided amplifier falls within the configured bounds.
     *
     * @param amplifier amplifier to verify
     * @return {@code true} when the amplifier satisfies the configured constraints
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
     * Clamps the amplifier to the configured minimum and maximum.
     *
     * @param amplifier amplifier level to clamp; may be {@code null}
     * @return amplifier after applying bounds or the default when {@code amplifier} is {@code null}
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

    /**
     * Provides the default amplifier when no permission matches.
     *
     * @return default amplifier or {@code 0}
     */
    @Override
    protected Integer getDefaultValue() {
        if (
            this.defaultAmplifier != null
        ) {
            return this.defaultAmplifier;
        }

        return 0;
    }

    /**
     * Supplies the permission-to-amplifier map, copying the configured entries to avoid side effects.
     *
     * @return mutable copy of the configured amplifier overrides
     */
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

    /**
     * Indicates that the section should prefer the highest amplifier when multiple permissions apply.
     *
     * @return {@code true} so the resolver selects the strongest amplifier
     */
    @Override
    protected boolean getDefaultUseBestValue() {
        return true;
    }

    /**
     * Chooses the larger amplifier when multiple permissions match.
     *
     * @param current   current best amplifier
     * @param candidate candidate amplifier under evaluation
     * @return greater of {@code current} and {@code candidate}
     */
    @Override
    protected Integer chooseBestValue(
        final Integer current,
        final Integer candidate
    ) {
        return Math.max(current, candidate);
    }

    /**
     * Determines if the candidate amplifier is stronger than the current best value.
     *
     * @param candidate candidate amplifier
     * @param current   current best amplifier
     * @return {@code true} when {@code candidate} exceeds {@code current}
     */
    @Override
    protected boolean isBetterValue(
        final Integer candidate,
        final Integer current
    ) {
        return candidate > current;
    }

    /**
     * Applies the configured bounds to the resolved amplifier.
     *
     * @param value amplifier before clamping
     * @return amplifier after enforcing bounds
     */
    @Override
    protected Integer applyBounds(
        final Integer value
    ) {
        return this.clampAmplifier(value);
    }

    /**
     * Validates that amplifier values are non-null and non-negative.
     *
     * @param value amplifier to validate
     * @return {@code true} when the amplifier is zero or positive
     */
    @Override
    protected boolean isValidValue(
        final Integer value
    ) {
        return value != null && value >= 0;
    }

    /**
     * Ensures configured bounds are internally consistent.
     *
     * @throws IllegalStateException when a bound is negative or the minimum exceeds the maximum
     */
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
