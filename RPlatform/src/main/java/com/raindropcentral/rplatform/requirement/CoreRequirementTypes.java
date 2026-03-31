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

/**
 * Registers core RPlatform requirement types.
 *
 * <p>This class should be called during RPlatform initialization to register
 * all built-in requirement types.
 *
 * @author ItsRainingHP, JExcellence
 * @since 2.0.0
 * @version 1.0.0
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
        registry.registerType(RequirementType.core("TIME_BASED", TimedRequirement.class));
        
        // Parent requirement options
        registry.registerType(RequirementType.core("COMPOSITE", CompositeRequirement.class));
        registry.registerType(RequirementType.core("CHOICE", ChoiceRequirement.class));
        
        // Plugin integration requirements
        registry.registerType(RequirementType.core("PLUGIN", PluginRequirement.class));
    }
}
