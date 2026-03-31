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

package com.raindropcentral.rdq.requirement;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.requirement.lifecycle.LifecycleRegistry;

import java.util.logging.Logger;

/**
 * One-time setup for RDQ requirement system integration.
 *
 * <p>Call {@link #initialize()} once during plugin startup.
 */
public final class RDQRequirementSetup {

    private static final Logger LOGGER = Logger.getLogger("RDQ");

    private RDQRequirementSetup() {}

    /**
     * Initializes RDQ's requirement system integration.
 *
 * <p>This registers validators, lifecycle hooks, and section adapters. Call once in onEnable().
     */
    public static void initialize() {
        try {
            LOGGER.info("Initializing RDQ requirement system integration...");

            // Register section adapter for YAML config parsing
            LOGGER.info("Registering section adapter...");
            RequirementFactory.getInstance().registerSectionAdapter(
                BaseRequirementSection.class,
                new RDQRequirementSectionAdapter()
            );
            LOGGER.info("Registered BaseRequirementSection adapter");

            // Register validators for all requirement types
            LOGGER.info("Registering validators...");
            RDQRequirementValidators.registerAll();
            LOGGER.info("Registered requirement validators");

            // Register lifecycle hook for logging
            LOGGER.info("Registering lifecycle hook...");
            LifecycleRegistry.getInstance().registerHook(new RDQRequirementLifecycleHook());
            LOGGER.info("Registered lifecycle hook");

            LOGGER.info("RDQ requirement system integration complete");
            LOGGER.info("Use RequirementFactory.fromSection() to convert YAML configs to requirements");
        } catch (Throwable t) {
            LOGGER.severe("Failed to initialize RDQ requirement system: " + t.getMessage());
            t.printStackTrace();
            throw t;
        }
    }
}
