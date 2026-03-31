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

package com.raindropcentral.rdq.reward;

import com.raindropcentral.rdq.config.utility.RewardSection;
import com.raindropcentral.rplatform.reward.config.RewardFactory;

import java.util.logging.Logger;

/**
 * Represents the RDQRewardSetup API type.
 */
public final class RDQRewardSetup {

    private static final Logger LOGGER = Logger.getLogger(RDQRewardSetup.class.getName());
    private static boolean initialized = false;

    private RDQRewardSetup() {}

    /**
     * Executes initialize.
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warning("RDQ Reward System already initialized");
            return;
        }
        
        // Register adapter for RewardSection (the actual config class used in YAML)
        RewardFactory.getInstance().registerSectionAdapter(
            RewardSection.class,
            new RDQRewardSectionAdapter()
        );

        initialized = true;
        LOGGER.info("RDQ Reward System initialized successfully");
    }
}
