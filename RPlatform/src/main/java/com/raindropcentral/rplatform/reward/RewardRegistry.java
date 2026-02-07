package com.raindropcentral.rplatform.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class RewardRegistry {

    private static final Logger LOGGER = Logger.getLogger(RewardRegistry.class.getName());
    private static final RewardRegistry INSTANCE = new RewardRegistry();

    private final Map<String, RewardType> rewardTypes = new ConcurrentHashMap<>();
    private final Map<String, PluginRewardProvider> providers = new ConcurrentHashMap<>();

    private RewardRegistry() {}

    public static RewardRegistry getInstance() {
        return INSTANCE;
    }

    public void registerType(@NotNull RewardType type) {
        String key = type.id();
        if (rewardTypes.containsKey(key)) {
            LOGGER.warning("Reward type already registered: " + key);
            return;
        }
        rewardTypes.put(key, type);
        LOGGER.info("Registered reward type: " + type.getQualifiedName());
    }

    public void unregisterType(@NotNull String typeName) {
        RewardType removed = rewardTypes.remove(typeName);
        if (removed != null) {
            LOGGER.info("Unregistered reward type: " + typeName);
        }
    }

    public void registerProvider(@NotNull PluginRewardProvider provider) {
        String pluginId = provider.getPluginId();
        if (providers.containsKey(pluginId)) {
            LOGGER.warning("Provider already registered: " + pluginId);
            return;
        }
        providers.put(pluginId, provider);
        provider.register();
        LOGGER.info("Registered reward provider: " + pluginId);
    }

    public void unregisterProvider(@NotNull String pluginId) {
        PluginRewardProvider provider = providers.remove(pluginId);
        if (provider != null) {
            provider.unregister();
            LOGGER.info("Unregistered reward provider: " + pluginId);
        }
    }

    public RewardType getRewardType(@NotNull String typeName) {
        return rewardTypes.get(typeName);
    }

    public boolean isRegistered(@NotNull String typeName) {
        return rewardTypes.containsKey(typeName);
    }

    public Map<String, RewardType> getRewardTypes() {
        return Map.copyOf(rewardTypes);
    }

    public Map<String, PluginRewardProvider> getProviders() {
        return Map.copyOf(providers);
    }

    public PluginRewardProvider getProvider(@NotNull String pluginId) {
        return providers.get(pluginId);
    }

    public Class<? extends AbstractReward> getImplementationClass(@NotNull String typeName) {
        RewardType type = rewardTypes.get(typeName);
        return type != null ? type.implementationClass() : null;
    }

    public ObjectMapper configureObjectMapper(@NotNull ObjectMapper mapper) {
        mapper.registerSubtypes(
            com.raindropcentral.rplatform.reward.impl.ItemReward.class,
            com.raindropcentral.rplatform.reward.impl.CurrencyReward.class,
            com.raindropcentral.rplatform.reward.impl.ExperienceReward.class,
            com.raindropcentral.rplatform.reward.impl.CommandReward.class,
            com.raindropcentral.rplatform.reward.impl.CompositeReward.class,
            com.raindropcentral.rplatform.reward.impl.ChoiceReward.class,
            com.raindropcentral.rplatform.reward.impl.PermissionReward.class,
            com.raindropcentral.rplatform.reward.impl.TeleportReward.class,
            com.raindropcentral.rplatform.reward.impl.ParticleReward.class,
            com.raindropcentral.rplatform.reward.impl.VanishingChestReward.class
        );
        return mapper;
    }
}
