package com.raindropcentral.rdq.reward;

import com.raindropcentral.rplatform.reward.CoreRewardTypes;
import com.raindropcentral.rplatform.reward.config.RewardFactory;

import java.util.logging.Logger;

public final class RDQRewardSetup {

    private static final Logger LOGGER = Logger.getLogger(RDQRewardSetup.class.getName());
    private static boolean initialized = false;

    private RDQRewardSetup() {}

    public static void initialize() {
        if (initialized) {
            LOGGER.warning("RDQ Reward System already initialized");
            return;
        }

        CoreRewardTypes.registerAll();
        
        RewardFactory.getInstance().registerSectionAdapter(
            BaseRewardSection.class,
            new RDQRewardSectionAdapter()
        );

        initialized = true;
        LOGGER.info("RDQ Reward System initialized successfully");
    }
}
