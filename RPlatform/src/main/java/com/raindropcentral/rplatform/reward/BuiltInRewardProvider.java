package com.raindropcentral.rplatform.reward;

import com.raindropcentral.rplatform.reward.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class BuiltInRewardProvider implements PluginRewardProvider {

    private static final Logger LOGGER = Logger.getLogger(BuiltInRewardProvider.class.getName());
    private static final BuiltInRewardProvider INSTANCE = new BuiltInRewardProvider();

    private final Map<String, RewardType> rewardTypes = new HashMap<>();

    private BuiltInRewardProvider() {
        initialize();
    }

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

    @Override
    public @NotNull String getPluginId() {
        return "rplatform";
    }

    @Override
    public @NotNull Map<String, RewardType> getRewardTypes() {
        return Map.copyOf(rewardTypes);
    }

    @Override
    public void onRegister() {
        LOGGER.info("Registered " + rewardTypes.size() + " built-in reward types");
    }

    @Override
    public void onUnregister() {
        LOGGER.info("Unregistered built-in reward types");
    }

    public void register() {
        RewardRegistry.getInstance().registerProvider(this);
    }

    public void unregister() {
        RewardRegistry.getInstance().unregisterProvider(getPluginId());
    }
}
