package com.raindropcentral.rplatform.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.raindropcentral.rplatform.requirement.json.RequirementMixin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for requirement types with dynamic type registration.
 */
public final class RequirementRegistry {

    private static final Logger LOGGER = Logger.getLogger(RequirementRegistry.class.getName());
    private static final RequirementRegistry INSTANCE = new RequirementRegistry();

    private final Map<String, RequirementType> requirementTypes = new ConcurrentHashMap<>();
    private final Map<String, PluginRequirementProvider> providers = new ConcurrentHashMap<>();
    
    private volatile ObjectMapper objectMapper;

    private RequirementRegistry() {
        // Built-in types are registered by RPlatform.initialize()
        // No initialization needed here to avoid circular dependencies
    }

    @NotNull
    public static RequirementRegistry getInstance() {
        return INSTANCE;
    }

    // ==================== Type Registration ====================

    public void registerType(@NotNull RequirementType type) {
        String key = type.id().toUpperCase();
        requirementTypes.put(key, type);
        
        LOGGER.info(String.format(
            "Registered requirement type: %s (plugin: %s)",
            type.id(), type.pluginId()
        ));
        
        if (objectMapper != null) {
            objectMapper.registerSubtypes(new NamedType(type.implementationClass(), type.id()));
        }
    }

    public void unregisterType(@NotNull String typeName) {
        requirementTypes.remove(typeName.toUpperCase());
        LOGGER.info("Unregistered requirement type: " + typeName);
    }

    @Nullable
    public RequirementType getRequirementType(@NotNull String typeName) {
        return requirementTypes.get(typeName.toUpperCase());
    }

    @NotNull
    public Map<String, RequirementType> getRequirementTypes() {
        return Map.copyOf(requirementTypes);
    }

    public boolean isRegistered(@NotNull String typeName) {
        return requirementTypes.containsKey(typeName.toUpperCase());
    }

    @Nullable
    public Class<? extends AbstractRequirement> getImplementationClass(@NotNull String typeName) {
        RequirementType type = getRequirementType(typeName);
        return type != null ? type.implementationClass() : null;
    }

    // ==================== Provider Management ====================

    public void registerProvider(@NotNull PluginRequirementProvider provider) {
        String pluginId = provider.getPluginId();
        
        if (providers.containsKey(pluginId)) {
            LOGGER.warning("Provider already registered for plugin: " + pluginId);
            return;
        }
        
        providers.put(pluginId, provider);
        provider.register();
        
        LOGGER.info("Registered requirement provider: " + pluginId + " with types: " + provider.getRequirementTypes().keySet());
    }

    public void unregisterProvider(@NotNull String pluginId) {
        PluginRequirementProvider provider = providers.remove(pluginId);
        if (provider != null) {
            provider.unregister();
            LOGGER.info("Unregistered requirement provider: " + pluginId);
        }
    }

    @Nullable
    public PluginRequirementProvider getProvider(@NotNull String pluginId) {
        return providers.get(pluginId);
    }

    @NotNull
    public Map<String, PluginRequirementProvider> getProviders() {
        return Map.copyOf(providers);
    }

    // ==================== ObjectMapper Configuration ====================

    @NotNull
    public ObjectMapper configureObjectMapper(@NotNull ObjectMapper mapper) {
        this.objectMapper = mapper;
        mapper.addMixIn(AbstractRequirement.class, RequirementMixin.class);
        
        for (RequirementType type : requirementTypes.values()) {
            mapper.registerSubtypes(new NamedType(type.implementationClass(), type.id()));
        }
        
        return mapper;
    }

    // ==================== Utility Methods ====================

    @NotNull
    public RegistryStatistics getStatistics() {
        return new RegistryStatistics(
            requirementTypes.size(),
            providers.size()
        );
    }

    public record RegistryStatistics(
        int totalTypes,
        int providers
    ) {
        @Override
        public String toString() {
            return String.format(
                "Registry Statistics: %d types, %d providers",
                totalTypes, providers
            );
        }
    }
}
