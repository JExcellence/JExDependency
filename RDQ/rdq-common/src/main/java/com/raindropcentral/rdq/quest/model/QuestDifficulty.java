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

package com.raindropcentral.rdq.quest.model;

import lombok.Getter;

/**
 * Enum representing the difficulty levels for quests.
 *
 * <p>Each difficulty level has an associated reward multiplier and color code
 * for display purposes using MiniMessage formatting.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
public enum QuestDifficulty {
    
    /**
     * Trivial difficulty - easiest quests with base rewards.
     */
    TRIVIAL(1.0, "<white>"),
    
    /**
     * Easy difficulty - simple quests with slightly increased rewards.
     */
    EASY(1.25, "<green>"),
    
    /**
     * Normal difficulty - standard quests with moderate rewards.
     */
    NORMAL(1.5, "<yellow>"),
    
    /**
     * Hard difficulty - challenging quests with significant rewards.
     */
    HARD(2.0, "<gold>"),
    
    /**
     * Extreme difficulty - most challenging quests with maximum rewards.
     */
    EXTREME(3.0, "<red>");

    /**
     * -- GETTER --.
     *  Gets the reward multiplier for this difficulty level.
     *
     */
    private final double rewardMultiplier;
    /**
     * -- GETTER --.
     *  Gets the MiniMessage color code for this difficulty level.
     *
     */
    private final String colorCode;
    
    /**
     * Constructs a quest difficulty level.
     *
     * @param rewardMultiplier the multiplier applied to quest rewards
     * @param colorCode        the MiniMessage color code for display
     */
    QuestDifficulty(final double rewardMultiplier, final String colorCode) {
        this.rewardMultiplier = rewardMultiplier;
        this.colorCode = colorCode;
    }

}
