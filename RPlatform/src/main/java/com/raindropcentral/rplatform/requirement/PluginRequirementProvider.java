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

import java.util.Map;

/**
 * Interface for plugins to register their custom requirement types.
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * public class OneBlockRequirementProvider implements PluginRequirementProvider {
 *     @Override
 *     public String getPluginId() {
 *         return "jexoneblock";
 *     }
 *
 *     @Override
 *     public Map<String, RequirementType> getRequirementTypes() {
 *         return Map.of(
 *             "EVOLUTION_LEVEL", RequirementType.plugin("EVOLUTION_LEVEL", "jexoneblock", EvolutionLevelRequirement.class),
 *             "BLOCKS_BROKEN", RequirementType.plugin("BLOCKS_BROKEN", "jexoneblock", BlocksBrokenRequirement.class)
 *         );
 *     }
 * }
 * }</pre>
 */
public interface PluginRequirementProvider {

    /**
     * Executes this member.
     */
    @NotNull
    String getPluginId();

    /**
     * Executes this member.
     */
    @NotNull
    Map<String, RequirementType> getRequirementTypes();

    /**
     * Executes onRegister.
     */
    default void onRegister() {}

    /**
     * Executes onUnregister.
     */
    default void onUnregister() {}

    /**
     * Executes register.
     */
    default void register() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (RequirementType type : getRequirementTypes().values()) {
            registry.registerType(type);
        }
        onRegister();
    }

    /**
     * Executes unregister.
     */
    default void unregister() {
        RequirementRegistry registry = RequirementRegistry.getInstance();
        for (String typeName : getRequirementTypes().keySet()) {
            registry.unregisterType(typeName);
        }
        onUnregister();
    }
}
