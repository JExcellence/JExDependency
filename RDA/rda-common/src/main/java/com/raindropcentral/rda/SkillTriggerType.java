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

package com.raindropcentral.rda;

import org.jetbrains.annotations.NotNull;

/**
 * Generic skill trigger families supported by the RDA framework.
 *
 * <p>Triggers are used both to route live gameplay events and to render rate information in the
 * GUI.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public enum SkillTriggerType {

    /**
     * Triggered by block breaking actions.
     */
    BLOCK_BREAK,

    /**
     * Triggered by accumulated block travel.
     */
    BLOCK_TRAVEL,

    /**
     * Triggered by first-time chunk discovery.
     */
    CHUNK_DISCOVERY,

    /**
     * Triggered by player-caused combat damage.
     */
    ENTITY_DAMAGE,

    /**
     * Triggered by damage taken by the player.
     */
    DAMAGE_TAKEN,

    /**
     * Triggered by shield blocking.
     */
    SHIELD_BLOCK,

    /**
     * Triggered by brewed potion retrieval.
     */
    BREW_COLLECT,

    /**
     * Triggered by enchanting table enchants.
     */
    ENCHANT_TABLE,

    /**
     * Triggered by anvil enchant applications.
     */
    ANVIL_ENCHANT,

    /**
     * Triggered by crafting result production.
     */
    CRAFT_RESULT,

    /**
     * Triggered by successful catches.
     */
    FISH_CATCH,

    /**
     * Triggered by successful taming.
     */
    TAME_SUCCESS,

    /**
     * Triggered by owned tame final blows.
     */
    TAMED_KILL,

    /**
     * Triggered by furnace, smoker, or blast furnace result collection.
     */
    FURNACE_COLLECT,

    /**
     * Triggered by honey bottle or honeycomb collection.
     */
    HONEY_COLLECT;

    /**
     * Returns the translation key used for the trigger label.
     *
     * @return trigger translation key
     */
    public @NotNull String getTranslationKey() {
        return "ra_skill_triggers." + this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
