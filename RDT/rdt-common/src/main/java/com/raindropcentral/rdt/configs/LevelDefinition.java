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

package com.raindropcentral.rdt.configs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Immutable config snapshot for one configured level target.
 *
 * @param level target level reached after successful completion
 * @param requirements normalized requirement definitions keyed by identifier
 * @param rewards normalized reward definitions keyed by identifier
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record LevelDefinition(
    int level,
    @NotNull Map<String, Map<String, Object>> requirements,
    @NotNull Map<String, Map<String, Object>> rewards
) {

    /**
     * Creates an immutable level-definition snapshot.
     *
     * @param level target level reached after leveling up
     * @param requirements normalized requirement definitions
     * @param rewards normalized reward definitions
     */
    public LevelDefinition {
        level = Math.max(1, level);
        requirements = LevelConfigSupport.deepCopyDefinitionMap(requirements);
        rewards = LevelConfigSupport.deepCopyDefinitionMap(rewards);
    }

    /**
     * Returns a defensive deep copy of the configured requirement definitions.
     *
     * @return copied requirement definitions
     */
    public @NotNull Map<String, Map<String, Object>> getRequirements() {
        return LevelConfigSupport.deepCopyDefinitionMap(this.requirements);
    }

    /**
     * Returns a defensive deep copy of the configured reward definitions.
     *
     * @return copied reward definitions
     */
    public @NotNull Map<String, Map<String, Object>> getRewards() {
        return LevelConfigSupport.deepCopyDefinitionMap(this.rewards);
    }

    /**
     * Returns whether this level contains no requirements and no rewards.
     *
     * @return {@code true} when the definition is empty
     */
    public boolean isEmpty() {
        return this.requirements.isEmpty() && this.rewards.isEmpty();
    }

    /**
     * Returns a deep-copied replacement of this definition with the supplied rewards.
     *
     * @param updatedRewards replacement reward definitions
     * @return copied definition with updated rewards
     */
    public @NotNull LevelDefinition withRewards(final @Nullable Map<String, Map<String, Object>> updatedRewards) {
        return new LevelDefinition(this.level, this.requirements, updatedRewards == null ? Map.of() : updatedRewards);
    }
}
