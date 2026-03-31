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

import com.raindropcentral.rplatform.requirement.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider for built-in requirement types.
 *
 * <p>This provider registers all the standard requirement types that come with RPlatform.
 *
 * @author ItsRainingHP, JExcellence
 * @since 2.0.0
 * @version 1.0.0
 */
public final class BuiltInRequirementProvider implements PluginRequirementProvider {

    private static final String PLUGIN_ID = "RPlatform";
    private final Map<String, RequirementType> types = new HashMap<>();

    /**
     * Executes BuiltInRequirementProvider.
     */
    public BuiltInRequirementProvider() {
        // Register all built-in types
        types.put("ITEM", new RequirementType("ITEM", PLUGIN_ID, ItemRequirement.class));
        types.put("CURRENCY", new RequirementType("CURRENCY", PLUGIN_ID, CurrencyRequirement.class));
        types.put("EXPERIENCE_LEVEL", new RequirementType("EXPERIENCE_LEVEL", PLUGIN_ID, ExperienceLevelRequirement.class));
        types.put("PERMISSION", new RequirementType("PERMISSION", PLUGIN_ID, PermissionRequirement.class));
        types.put("LOCATION", new RequirementType("LOCATION", PLUGIN_ID, LocationRequirement.class));
        types.put("PLAYTIME", new RequirementType("PLAYTIME", PLUGIN_ID, PlaytimeRequirement.class));
        types.put("COMPOSITE", new RequirementType("COMPOSITE", PLUGIN_ID, CompositeRequirement.class));
        types.put("CHOICE", new RequirementType("CHOICE", PLUGIN_ID, ChoiceRequirement.class));
        types.put("TIME_BASED", new RequirementType("TIME_BASED", PLUGIN_ID, TimedRequirement.class));
        types.put("PLUGIN", new RequirementType("PLUGIN", PLUGIN_ID, PluginRequirement.class));
    }

    /**
     * Gets pluginId.
     */
    @Override
    @NotNull
    public String getPluginId() {
        return PLUGIN_ID;
    }

    /**
     * Gets requirementTypes.
     */
    @Override
    @NotNull
    public Map<String, RequirementType> getRequirementTypes() {
        return Map.copyOf(types);
    }

    /**
     * Executes register.
     */
    @Override
    public void register() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (RequirementType type : types.values()) {
            registry.registerType(type);
        }
    }

    /**
     * Executes unregister.
     */
    @Override
    public void unregister() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (String typeId : types.keySet()) {
            registry.unregisterType(typeId);
        }
    }

    /**
     * Initializes the built-in requirement types.
 *
 * <p>This should be called during RPlatform initialization, before any requirement parsing occurs.
     */
    public static void initialize() {
        BuiltInRequirementProvider provider = new BuiltInRequirementProvider();
        RequirementRegistry.getInstance().registerProvider(provider);
    }
}
