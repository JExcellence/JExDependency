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
 * Configuration section that maps permissions to cooldown durations expressed in seconds.
 *
 * <p>Each permission can define its own cooldown override while a default value ensures fallbacks for
 * players without specific permissions. The section selects the smallest positive cooldown (or zero
 * for no cooldown) when multiple permissions apply, supporting fast-track permissions that reduce or
 * remove cooldowns entirely.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class PermissionCooldownSection extends APermissionBasedSection<Long> {

    /**
     * Default cooldown in seconds applied when no permission override matches ({@code defaultCooldownSeconds}).
     */
    private Long defaultCooldownSeconds;

    /**
     * Backwards compatible alias sourced from {@code defaultCooldown} to support legacy perk files.
     */
    private Long defaultCooldown;

    /**
     * Mapping of permission nodes to cooldown overrides from {@code permissionCooldowns}.
     */
    private Map<String, Long> permissionCooldowns;

    /**
     * Creates the cooldown section backed by permission-aware overrides.
     *
     * @param evaluationEnvironmentBuilder mapper environment shared across sections
     */
    public PermissionCooldownSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Returns the cooldown duration to apply when no permission override matches.
     *
     * @return default cooldown in seconds, falling back to {@code 0L}
     */
    public Long getDefaultCooldownSeconds() {
        return this.getDefaultValue();
    }

    /**
     * Exposes the configured permission override map.
     *
     * @return map linking permission nodes to cooldown durations in seconds
     */
    public Map<String, Long> getPermissionCooldowns() {
        return this.getPermissionValues();
    }

    /**
     * Looks up the cooldown associated with a specific permission string.
     *
     * @param permission permission node to inspect
     * @return configured cooldown or {@code null} when the permission is absent
     */
    public Long getCooldownForPermission(
        final String permission
    ) {
        return this.getValueForPermission(permission);
    }

    /**
     * Indicates whether the configuration defines any permission-based overrides.
     *
     * @return {@code true} when overrides exist
     */
    public boolean hasPermissionCooldowns() {
        return this.hasPermissionValues();
    }

    /**
     * Computes the cooldown for the supplied player, respecting overrides and bounds.
     *
     * @param player player whose cooldown should be calculated
     * @return resolved cooldown in seconds
     */
    public Long getEffectiveCooldown(
        final Player player
    ) {
        return this.getEffectiveValue(player);
    }

    /**
     * Computes the cooldown for an already-collected permission set.
     *
     * @param playerPermissions permissions associated with the player
     * @return resolved cooldown in seconds
     */
    public Long getEffectiveCooldown(
        final Set<String> playerPermissions
    ) {
        return this.getEffectiveValue(playerPermissions);
    }


    /**
     * Provides the default cooldown when no permission override applies.
     *
     * @return configured default cooldown or {@code 0L}
     */
    @Override
    protected Long getDefaultValue() {
        if (this.defaultCooldownSeconds != null) {
            return this.defaultCooldownSeconds;
        }

        if (this.defaultCooldown != null) {
            return this.defaultCooldown;
        }

        return 0L;
    }

    /**
     * Supplies a copy of the permission-to-cooldown map.
     *
     * @return mutable copy of configured cooldown overrides
     */
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

    /**
     * Uses the "best" strategy by default so shorter cooldowns take precedence.
     *
     * @return {@code true} indicating the resolver should search for the optimal value
     */
    @Override
    protected boolean getDefaultUseBestValue() {

        return true;
    }

    /**
     * Chooses the lower non-zero cooldown so high-tier permissions reduce waiting time.
     *
     * @param current   current best cooldown
     * @param candidate candidate cooldown under evaluation
     * @return smallest cooldown, favouring {@code 0L} when present
     */
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

    /**
     * Determines if the candidate cooldown is preferable to the current best.
     *
     * @param candidate candidate cooldown
     * @param current   current best cooldown
     * @return {@code true} when {@code candidate} shortens the cooldown window
     */
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

    /**
     * Ensures cooldown values are non-null and not negative.
     *
     * @param value cooldown value to validate
     * @return {@code true} when the cooldown is zero or positive
     */
    @Override
    protected boolean isValidValue(
        final Long value
    ) {
        return value != null && value >= 0;
    }
}
