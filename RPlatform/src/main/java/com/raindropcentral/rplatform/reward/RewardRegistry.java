package com.raindropcentral.rplatform.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents the RewardRegistry API type.
 */
public final class RewardRegistry {

    private static final Logger LOGGER = Logger.getLogger(RewardRegistry.class.getName());
    private static final RewardRegistry INSTANCE = new RewardRegistry();

    private final Map<String, RewardType> rewardTypes = new ConcurrentHashMap<>();
    private final Map<String, PluginRewardProvider> providers = new ConcurrentHashMap<>();

    private RewardRegistry() {}

    /**
     * Gets instance.
     */
    public static RewardRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Executes registerType.
     */
    public void registerType(@NotNull RewardType type) {
        String key = type.id();
        if (rewardTypes.containsKey(key)) {
            LOGGER.warning("Reward type already registered: " + key);
            return;
        }
        rewardTypes.put(key, type);
        LOGGER.info("Registered reward type: " + type.getQualifiedName());
    }

    /**
     * Executes unregisterType.
     */
    public void unregisterType(@NotNull String typeName) {
        RewardType removed = rewardTypes.remove(typeName);
        if (removed != null) {
            LOGGER.info("Unregistered reward type: " + typeName);
        }
    }

    /**
     * Executes registerProvider.
     */
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

    /**
     * Executes unregisterProvider.
     */
    public void unregisterProvider(@NotNull String pluginId) {
        PluginRewardProvider provider = providers.remove(pluginId);
        if (provider != null) {
            provider.unregister();
            LOGGER.info("Unregistered reward provider: " + pluginId);
        }
    }

    /**
     * Gets rewardType.
     */
    public RewardType getRewardType(@NotNull String typeName) {
        return rewardTypes.get(typeName);
    }

    /**
     * Returns whether registered.
     */
    public boolean isRegistered(@NotNull String typeName) {
        return rewardTypes.containsKey(typeName);
    }

    /**
     * Gets rewardTypes.
     */
    public Map<String, RewardType> getRewardTypes() {
        return Map.copyOf(rewardTypes);
    }

    /**
     * Gets providers.
     */
    public Map<String, PluginRewardProvider> getProviders() {
        return Map.copyOf(providers);
    }

    /**
     * Gets provider.
     */
    public PluginRewardProvider getProvider(@NotNull String pluginId) {
        return providers.get(pluginId);
    }

    /**
     * Gets implementationClass.
     */
    public Class<? extends AbstractReward> getImplementationClass(@NotNull String typeName) {
        RewardType type = rewardTypes.get(typeName);
        return type != null ? type.implementationClass() : null;
    }

    /**
     * Executes configureObjectMapper.
     */
    public ObjectMapper configureObjectMapper(@NotNull ObjectMapper mapper) {
        // The @JsonSubTypes annotation on AbstractReward already handles subtype registration
        // No need to manually register subtypes here
        return mapper;
    }
}
