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

package com.raindropcentral.rdr.requirement;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.StoreRequirementSection;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.requirement.config.RequirementSectionAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Registers RDR-specific requirement support with the shared RPlatform requirement system.
 *
 * <p>This setup registers the config-section adapter used to convert RDR store requirement sections
 * into standard RPlatform requirements.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class RDRRequirementSetup {

    private static final Logger LOGGER = Logger.getLogger("RDR");

    private RDRRequirementSetup() {
    }

    /**
     * Initializes RDR requirement integration.
     *
     * @param plugin active RDR plugin instance
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public static void initialize(final @NotNull RDR plugin) {
        Objects.requireNonNull(plugin, "plugin");
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();

        if (requirementFactory.getSectionAdapter(StoreRequirementSection.class) == null) {
            requirementFactory.registerSectionAdapter(
                StoreRequirementSection.class,
                new StoreRequirementSectionAdapter()
            );
        }

        LOGGER.info("Initialized RDR requirement integration");
    }

    /**
     * Removes the RDR requirement provider and section adapter.
     */
    public static void shutdown() {
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();
        requirementFactory.unregisterSectionAdapter(StoreRequirementSection.class);
    }

    private static final class StoreRequirementSectionAdapter
        implements RequirementSectionAdapter<StoreRequirementSection> {

        /**
         * Executes convert.
         */
        @Override
        public @Nullable AbstractRequirement convert(
            final @NotNull StoreRequirementSection section,
            final @Nullable Map<String, Object> context
        ) {
            return RequirementFactory.getInstance().fromMap(section.toRequirementMap());
        }
    }
}
