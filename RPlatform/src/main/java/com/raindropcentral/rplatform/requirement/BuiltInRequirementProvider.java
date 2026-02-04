package com.raindropcentral.rplatform.requirement;

import com.raindropcentral.rplatform.requirement.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Provider for built-in requirement types.
 * <p>
 * This provider registers all the standard requirement types that come with RPlatform.
 * </p>
 */
public final class BuiltInRequirementProvider implements PluginRequirementProvider {

    private static final String PLUGIN_ID = "RPlatform";
    private final Map<String, RequirementType> types = new HashMap<>();

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
    }

    @Override
    @NotNull
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    @NotNull
    public Map<String, RequirementType> getRequirementTypes() {
        return Map.copyOf(types);
    }

    @Override
    public void register() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (RequirementType type : types.values()) {
            registry.registerType(type);
        }
    }

    @Override
    public void unregister() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (String typeId : types.keySet()) {
            registry.unregisterType(typeId);
        }
    }

    /**
     * Initializes the built-in requirement types.
     * <p>
     * This should be called during RPlatform initialization, before any requirement parsing occurs.
     * </p>
     */
    public static void initialize() {
        BuiltInRequirementProvider provider = new BuiltInRequirementProvider();
        RequirementRegistry.getInstance().registerProvider(provider);
    }
}
