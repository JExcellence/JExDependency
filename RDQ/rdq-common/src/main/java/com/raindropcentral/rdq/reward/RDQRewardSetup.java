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
