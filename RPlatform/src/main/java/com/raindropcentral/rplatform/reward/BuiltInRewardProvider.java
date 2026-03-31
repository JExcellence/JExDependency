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

package com.raindropcentral.rplatform.reward;

import com.raindropcentral.rplatform.reward.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents the BuiltInRewardProvider API type.
 */
public final class BuiltInRewardProvider implements PluginRewardProvider {

    private static final Logger LOGGER = Logger.getLogger(BuiltInRewardProvider.class.getName());
    private static final BuiltInRewardProvider INSTANCE = new BuiltInRewardProvider();

    private final Map<String, RewardType> rewardTypes = new HashMap<>();

    private BuiltInRewardProvider() {
        initialize();
    }

    /**
     * Gets instance.
     */
    public static BuiltInRewardProvider getInstance() {
        return INSTANCE;
    }

    private void initialize() {
        rewardTypes.put("ITEM", RewardType.core("ITEM", ItemReward.class));
        rewardTypes.put("CURRENCY", RewardType.core("CURRENCY", CurrencyReward.class));
        rewardTypes.put("EXPERIENCE", RewardType.core("EXPERIENCE", ExperienceReward.class));
        rewardTypes.put("COMMAND", RewardType.core("COMMAND", CommandReward.class));
        rewardTypes.put("COMPOSITE", RewardType.core("COMPOSITE", CompositeReward.class));
        rewardTypes.put("CHOICE", RewardType.core("CHOICE", ChoiceReward.class));
        rewardTypes.put("PERMISSION", RewardType.core("PERMISSION", PermissionReward.class));
        rewardTypes.put("SOUND", RewardType.core("SOUND", SoundReward.class));
        rewardTypes.put("PARTICLE", RewardType.core("PARTICLE", ParticleReward.class));
        rewardTypes.put("TELEPORT", RewardType.core("TELEPORT", TeleportReward.class));
        rewardTypes.put("VANISHING_CHEST", RewardType.core("VANISHING_CHEST", VanishingChestReward.class));
    }

    /**
     * Gets pluginId.
     */
    @Override
    public @NotNull String getPluginId() {
        return "rplatform";
    }

    /**
     * Gets rewardTypes.
     */
    @Override
    public @NotNull Map<String, RewardType> getRewardTypes() {
        return Map.copyOf(rewardTypes);
    }

    /**
     * Executes onRegister.
     */
    @Override
    public void onRegister() {
        LOGGER.info("Registered " + rewardTypes.size() + " built-in reward types");
    }

    /**
     * Executes onUnregister.
     */
    @Override
    public void onUnregister() {
        LOGGER.info("Unregistered built-in reward types");
    }

    /**
     * Executes register.
     */
    public void register() {
        RewardRegistry.getInstance().registerProvider(this);
    }

    /**
     * Executes unregister.
     */
    public void unregister() {
        RewardRegistry.getInstance().unregisterProvider(getPluginId());
    }
}
