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

package com.raindropcentral.rplatform.skill;

import com.raindropcentral.rplatform.skill.impl.AuraSkillsSkillBridge;
import com.raindropcentral.rplatform.skill.impl.EcoSkillsSkillBridge;
import com.raindropcentral.rplatform.skill.impl.McMMOSkillBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared bridge contract for skills-plugin integrations.
 *
 * <p>This bridge mirrors the runtime discovery style used by {@code RProtectionBridge}, letting
 * RPlatform query external skills plugins without compile-time dependencies on their APIs.</p>
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public interface SkillBridge {

    /**
     * Default bridge discovery order.
     */
    List<SkillBridge> DEFAULT_BRIDGES = List.of(
        new EcoSkillsSkillBridge(),
        new AuraSkillsSkillBridge(),
        new McMMOSkillBridge()
    );

    /**
     * Gets the normalized integration identifier used in configuration.
     *
     * @return lower-case integration identifier (for example {@code "ecoskills"})
     */
    @NotNull String getIntegrationId();

    /**
     * Gets the plugin name used for runtime availability checks.
     *
     * @return external plugin name
     */
    @NotNull String getPluginName();

    /**
     * Checks whether the bridged plugin is installed, enabled, and reachable.
     *
     * @return {@code true} when bridge calls are available
     */
    boolean isAvailable();

    /**
     * Lists skills currently exposed by the bridged plugin for the supplied player context.
     *
     * @param player player opening the skill picker
     * @return immutable list of available skills
     * @throws NullPointerException if {@code player} is {@code null}
     */
    default @NotNull List<SkillDescriptor> getAvailableSkills(@NotNull Player player) {
        return List.of();
    }

    /**
     * Resolves a player's current level for a skill identifier.
     *
     * @param player player to inspect
     * @param skillId skill identifier to query
     * @return resolved level, or {@code 0} when unavailable
     * @throws NullPointerException if {@code player} or {@code skillId} is {@code null}
     */
    double getSkillLevel(@NotNull Player player, @NotNull String skillId);

    /**
     * Resolves multiple skill levels in one call.
     *
     * @param player player to inspect
     * @param skillIds skill identifiers to resolve
     * @return map of skill identifier to resolved level
     * @throws NullPointerException if {@code player} or {@code skillIds} is {@code null}
     */
    default @NotNull Map<String, Double> getSkillLevels(@NotNull Player player, @NotNull String... skillIds) {
        final Map<String, Double> values = new LinkedHashMap<>();
        for (final String skillId : skillIds) {
            if (skillId == null || skillId.isBlank()) {
                continue;
            }
            values.put(skillId, getSkillLevel(player, skillId));
        }
        return values;
    }

    /**
     * Consumes a skill level amount from a player.
     *
     * <p>Most skills plugins do not support reducing levels through public APIs. Bridges should
     * return {@code false} unless an explicit and safe write-path exists.</p>
     *
     * @param player player to modify
     * @param skillId skill identifier to consume
     * @param amount level amount to consume
     * @return {@code true} when consumption succeeded
     * @throws NullPointerException if {@code player} or {@code skillId} is {@code null}
     */
    default boolean consumeSkillLevel(@NotNull Player player, @NotNull String skillId, double amount) {
        return false;
    }

    /**
     * Adds one or more levels to a player skill.
     *
     * <p>Write support is optional. Bridges should return {@code false} when the external API does
     * not provide a safe additive level grant path.</p>
     *
     * @param player player to modify
     * @param skillId skill identifier to add levels to
     * @param amount number of levels to add
     * @return {@code true} when the level grant succeeds
     * @throws NullPointerException if {@code player} or {@code skillId} is {@code null}
     */
    default boolean addSkillLevels(@NotNull Player player, @NotNull String skillId, int amount) {
        return false;
    }

    /**
     * Detects the first available skill bridge from the default list.
     *
     * @return first available bridge, or {@code null} when no supported plugin is available
     */
    static @Nullable SkillBridge getBridge() {
        for (final SkillBridge bridge : getAvailableBridges()) {
            return bridge;
        }
        return null;
    }

    /**
     * Resolves a specific skill bridge by integration identifier.
     *
     * @param integrationId integration identifier or alias
     * @return matching available bridge, or {@code null} when not available
     */
    static @Nullable SkillBridge getBridge(@Nullable String integrationId) {
        if (integrationId == null || integrationId.isBlank()) {
            return null;
        }

        final String normalized = switch (integrationId.trim().toLowerCase(Locale.ROOT)) {
            case "auto" -> "auto";
            case "eco_skill", "eco-skills", "ecoskill" -> "ecoskills";
            case "aura", "aura_skill", "aura-skills" -> "auraskills";
            default -> integrationId.trim().toLowerCase(Locale.ROOT);
        };

        if ("auto".equals(normalized)) {
            return getBridge();
        }

        for (final SkillBridge bridge : DEFAULT_BRIDGES) {
            if (!bridge.isAvailable()) {
                continue;
            }

            final String bridgeId = bridge.getIntegrationId().toLowerCase(Locale.ROOT);
            final String pluginName = bridge.getPluginName().toLowerCase(Locale.ROOT);
            if (bridgeId.equals(normalized) || pluginName.equals(normalized)) {
                return bridge;
            }
        }
        return null;
    }

    /**
     * Returns the default bridge list in discovery order.
     *
     * @return immutable default bridge list
     */
    static @NotNull List<SkillBridge> getDefaultBridges() {
        return DEFAULT_BRIDGES;
    }

    /**
     * Returns all available skill bridges in discovery order.
     *
     * @return immutable list of available bridges
     */
    static @NotNull List<SkillBridge> getAvailableBridges() {
        final List<SkillBridge> availableBridges = new ArrayList<>();
        for (final SkillBridge bridge : DEFAULT_BRIDGES) {
            if (bridge.isAvailable()) {
                availableBridges.add(bridge);
            }
        }
        return List.copyOf(availableBridges);
    }

    /**
     * Immutable skill descriptor used by claim-cookie skill pickers.
     *
     * @param integrationId normalized bridge integration identifier
     * @param pluginName plugin name backing the skill
     * @param skillId normalized skill identifier
     * @param displayName user-facing skill label
     */
    record SkillDescriptor(
            @NotNull String integrationId,
            @NotNull String pluginName,
            @NotNull String skillId,
            @NotNull String displayName
    ) {
    }
}
