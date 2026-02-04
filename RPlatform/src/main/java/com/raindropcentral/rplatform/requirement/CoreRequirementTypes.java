package com.raindropcentral.rplatform.requirement;

import com.raindropcentral.rplatform.requirement.impl.*;

/**
 * Registers core RPlatform requirement types.
 * <p>
 * This class should be called during RPlatform initialization to register
 * all built-in requirement types.
 * </p>
 */
public final class CoreRequirementTypes {
    
    private CoreRequirementTypes() {}
    
    /**
     * Registers all core requirement types with the registry.
     */
    public static void registerAll() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        
        // Basic requirements
        registry.registerType(RequirementType.core("ITEM", ItemRequirement.class));
        registry.registerType(RequirementType.core("CURRENCY", CurrencyRequirement.class));
        registry.registerType(RequirementType.core("EXPERIENCE_LEVEL", ExperienceLevelRequirement.class));
        registry.registerType(RequirementType.core("PERMISSION", PermissionRequirement.class));
        registry.registerType(RequirementType.core("LOCATION", LocationRequirement.class));
        registry.registerType(RequirementType.core("PLAYTIME", PlaytimeRequirement.class));
        
        // Composite requirements
        registry.registerType(RequirementType.core("COMPOSITE", CompositeRequirement.class));
        registry.registerType(RequirementType.core("CHOICE", ChoiceRequirement.class));
        registry.registerType(RequirementType.core("TIME_BASED", TimedRequirement.class));
        
        // Plugin integration requirement (replaces old JOBS and SKILLS)
        registry.registerType(RequirementType.core("PLUGIN", PluginRequirement.class));
    }
}
