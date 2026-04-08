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
 *
 * <p>Supports both the new RequirementType system and legacy string-based registration
 * for backward compatibility.
 */
public final class RequirementRegistry {

    private static final Logger LOGGER = Logger.getLogger(RequirementRegistry.class.getName());
    private static final RequirementRegistry INSTANCE = new RequirementRegistry();

    // New system - maps type names to RequirementType records
    private final Map<String, RequirementType> requirementTypes = new ConcurrentHashMap<>();
    
    // Plugin providers
    private final Map<String, PluginRequirementProvider> providers = new ConcurrentHashMap<>();
    
    private volatile ObjectMapper objectMapper;

    private RequirementRegistry() {
        // Built-in types are registered by RPlatform.initialize()
        // No initialization needed here to avoid circular dependencies
    }

    /**
     * Gets instance.
     */
    @NotNull
    public static RequirementRegistry getInstance() {
        return INSTANCE;
    }

    // ==================== Type Registration ====================

    /**
     * Registers a requirement type with full metadata.
     *
     * @param type the requirement type to register
     */
    public void registerType(@NotNull RequirementType type) {
        String key = type.id().toUpperCase();
        requirementTypes.put(key, type);
        
        if (objectMapper != null) {
            objectMapper.registerSubtypes(new NamedType(type.implementationClass(), type.id()));
        }
    }

    /**
     * Unregisters a requirement type.
     *
     * @param typeName the type name to unregister
     */
    public void unregisterType(@NotNull String typeName) {
        requirementTypes.remove(typeName.toUpperCase());
    }

    /**
     * Gets a requirement type by name.
     *
     * @param typeName the type name
     * @return the requirement type, or null if not registered
     */
    @Nullable
    public RequirementType getRequirementType(@NotNull String typeName) {
        return requirementTypes.get(typeName.toUpperCase());
    }

    /**
     * Gets all registered requirement types.
     *
     * @return map of type names to requirement types
     */
    @NotNull
    public Map<String, RequirementType> getRequirementTypes() {
        return Map.copyOf(requirementTypes);
    }

    /**
     * Checks if a type is registered.
     *
     * @param typeName the type name
     * @return true if registered
     */
    public boolean isRegistered(@NotNull String typeName) {
        return requirementTypes.containsKey(typeName.toUpperCase());
    }

    /**
     * Gets the implementation class for a requirement type.
     *
     * @param typeName the type name
     * @return the implementation class, or null if not registered
     */
    @Nullable
    public Class<? extends AbstractRequirement> getImplementationClass(@NotNull String typeName) {
        RequirementType type = getRequirementType(typeName);
        return type != null ? type.implementationClass() : null;
    }

    // ==================== Provider Management ====================

    /**
     * Registers a plugin requirement provider.
 *
 * <p>This registers all requirement types from the provider and calls its onRegister callback.
     *
     * @param provider the provider to register
     */
    public void registerProvider(@NotNull PluginRequirementProvider provider) {
        String pluginId = provider.getPluginId();
        
        if (providers.containsKey(pluginId)) {
            LOGGER.warning("Provider already registered for plugin: " + pluginId);
            return;
        }
        
        providers.put(pluginId, provider);
        provider.register();
    }

    /**
     * Unregisters a plugin requirement provider.
     *
     * @param pluginId the plugin ID to unregister
     */
    public void unregisterProvider(@NotNull String pluginId) {
        PluginRequirementProvider provider = providers.remove(pluginId);
        if (provider != null) {
            provider.unregister();
        }
    }

    /**
     * Gets a registered provider by plugin ID.
     *
     * @param pluginId the plugin ID
     * @return the provider, or null if not registered
     */
    @Nullable
    public PluginRequirementProvider getProvider(@NotNull String pluginId) {
        return providers.get(pluginId);
    }

    /**
     * Gets all registered providers.
     *
     * @return map of plugin IDs to providers
     */
    @NotNull
    public Map<String, PluginRequirementProvider> getProviders() {
        return Map.copyOf(providers);
    }

    // ==================== ObjectMapper Configuration ====================

    /**
     * Configures an ObjectMapper with the RequirementMixin and all registered types.
     *
     * @param mapper the ObjectMapper to configure
     * @return the configured ObjectMapper
     */
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

    /**
     * Gets statistics about registered requirements.
     *
     * @return statistics record
     */
    @NotNull
    public RegistryStatistics getStatistics() {
        return new RegistryStatistics(
            requirementTypes.size(),
            providers.size()
        );
    }

    /**
     * Statistics about the requirement registry.
     */
    public record RegistryStatistics(
        int totalTypes,
        int providers
    ) {
        /**
         * Executes toString.
         */
        @Override
        public String toString() {
            return String.format(
                "Registry Statistics: %d types, %d providers",
                totalTypes, providers
            );
        }
    }
}
