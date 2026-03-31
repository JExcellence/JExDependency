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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a requirement type that can be dynamically registered.
 *
 * @param id the unique identifier for this requirement type (e.g., "EVOLUTION_LEVEL")
 * @param pluginId the plugin that provides this requirement type
 * @param implementationClass the class that implements this requirement
 */
public record RequirementType(
    @NotNull String id,
    @NotNull String pluginId,
    @NotNull Class<? extends AbstractRequirement> implementationClass
) {
    public RequirementType {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        Objects.requireNonNull(implementationClass, "implementationClass cannot be null");
    }
    
    /**
     * Creates a core RPlatform requirement type.
     */
    @NotNull
    public static RequirementType core(
        @NotNull String id,
        @NotNull Class<? extends AbstractRequirement> clazz
    ) {
        return new RequirementType(id, "rplatform", clazz);
    }
    
    /**
     * Creates a plugin requirement type.
     */
    @NotNull
    public static RequirementType plugin(
        @NotNull String id,
        @NotNull String pluginId,
        @NotNull Class<? extends AbstractRequirement> clazz
    ) {
        return new RequirementType(id, pluginId, clazz);
    }
    
    /**
     * Gets the full qualified type name (pluginId:id).
     */
    @NotNull
    public String getQualifiedName() {
        return pluginId + ":" + id;
    }
}
