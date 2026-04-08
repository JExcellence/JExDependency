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

package com.raindropcentral.rdt.requirement;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rplatform.requirement.PluginRequirementProvider;
import com.raindropcentral.rplatform.requirement.RequirementType;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Registers RDT-specific requirement types with the shared requirement system.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDTRequirementProvider implements PluginRequirementProvider {

    private static final String PLUGIN_ID = "rdt";

    private final RDT plugin;
    private final Map<String, RequirementType> requirementTypes;

    /**
     * Creates the provider for one active RDT runtime.
     *
     * @param plugin active RDT runtime
     */
    public RDTRequirementProvider(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.requirementTypes = Map.of(
            "TOWN_PLAYTIME",
            RequirementType.plugin("TOWN_PLAYTIME", PLUGIN_ID, TownPlaytimeRequirement.class)
        );
    }

    /**
     * Returns the provider plugin identifier.
     *
     * @return provider plugin identifier
     */
    @Override
    public @NotNull String getPluginId() {
        return PLUGIN_ID;
    }

    /**
     * Returns the registered requirement types.
     *
     * @return registered requirement types
     */
    @Override
    public @NotNull Map<String, RequirementType> getRequirementTypes() {
        return this.requirementTypes;
    }

    /**
     * Registers the matching config converter and binds the active runtime.
     */
    @Override
    public void onRegister() {
        TownPlaytimeRequirement.bindRuntime(this.plugin);
        RequirementFactory.getInstance().registerConverter("TOWN_PLAYTIME", TownPlaytimeRequirement::fromConfig);
    }

    /**
     * Unregisters the matching config converter and clears the bound runtime.
     */
    @Override
    public void onUnregister() {
        RequirementFactory.getInstance().unregisterConverter("TOWN_PLAYTIME");
        TownPlaytimeRequirement.clearRuntime();
    }
}
