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

package com.raindropcentral.rplatform.requirement.plugin;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests town integration registration and category detection in {@link PluginIntegrationRegistry}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PluginIntegrationRegistryTest {

    @Test
    void getBridgeNormalizesRdtIdentifiers() {
        final PluginIntegrationRegistry registry = PluginIntegrationRegistry.getInstance();
        final PluginIntegrationBridge originalBridge = registry.getBridges().get("rdt");
        final PluginIntegrationBridge expectedBridge = new TestTownBridge();
        registry.registerBridge(expectedBridge);

        try {
            assertSame(expectedBridge, registry.getBridge("rdt"));
            assertSame(expectedBridge, registry.getBridge("R-D_T"));
            assertSame(expectedBridge, registry.getBridge("raindroptowns"));
        } finally {
            if (originalBridge != null) {
                registry.registerBridge(originalBridge);
            } else {
                registry.unregisterBridge("rdt");
            }
        }
    }

    @Test
    void getBridgeNormalizesTownyAndHuskIdentifiers() {
        final PluginIntegrationRegistry registry = PluginIntegrationRegistry.getInstance();
        final PluginIntegrationBridge originalTownyBridge = registry.getBridges().get("towny");
        final PluginIntegrationBridge originalHuskBridge = registry.getBridges().get("husktowns");
        final PluginIntegrationBridge townyBridge = new TestTownBridge("towny", "Towny");
        final PluginIntegrationBridge huskBridge = new TestTownBridge("husktowns", "HuskTowns");
        registry.registerBridge(townyBridge);
        registry.registerBridge(huskBridge);

        try {
            assertSame(townyBridge, registry.getBridge("Towny"));
            assertSame(townyBridge, registry.getBridge("townyadvanced"));
            assertSame(huskBridge, registry.getBridge("Husk-Towns"));
            assertSame(huskBridge, registry.getBridge("husk_town"));
        } finally {
            restoreBridge(registry, "towny", originalTownyBridge);
            restoreBridge(registry, "husktowns", originalHuskBridge);
        }
    }

    @Test
    void detectBridgePrefersRdtForTownCategory() {
        final PluginIntegrationRegistry registry = PluginIntegrationRegistry.getInstance();
        final PluginIntegrationBridge originalRdtBridge = registry.getBridges().get("rdt");
        final PluginIntegrationBridge originalTownyBridge = registry.getBridges().get("towny");
        final PluginIntegrationBridge expectedBridge = new TestTownBridge("rdt", "RDT");
        registry.registerBridge(expectedBridge);
        registry.registerBridge(new TestTownBridge("towny", "Towny"));

        try {
            assertSame(expectedBridge, registry.detectBridge("TOWNS"));
            assertSame(expectedBridge, registry.detectBridge(" towns "));
        } finally {
            restoreBridge(registry, "rdt", originalRdtBridge);
            restoreBridge(registry, "towny", originalTownyBridge);
        }
    }

    @Test
    void detectBridgeFallsBackToTownyWhenRdtIsUnavailable() {
        final PluginIntegrationRegistry registry = PluginIntegrationRegistry.getInstance();
        final PluginIntegrationBridge originalRdtBridge = registry.getBridges().get("rdt");
        final PluginIntegrationBridge originalTownyBridge = registry.getBridges().get("towny");
        final PluginIntegrationBridge unavailableRdtBridge = new UnavailableTownBridge("rdt", "RDT");
        final PluginIntegrationBridge expectedBridge = new TestTownBridge("towny", "Towny");
        registry.registerBridge(unavailableRdtBridge);
        registry.registerBridge(expectedBridge);

        try {
            assertSame(expectedBridge, registry.detectBridge("TOWNS"));
        } finally {
            restoreBridge(registry, "rdt", originalRdtBridge);
            restoreBridge(registry, "towny", originalTownyBridge);
        }
    }

    private static void restoreBridge(
        final PluginIntegrationRegistry registry,
        final String integrationId,
        final PluginIntegrationBridge originalBridge
    ) {
        if (originalBridge != null) {
            registry.registerBridge(originalBridge);
        } else {
            registry.unregisterBridge(integrationId);
        }
    }

    private static class TestTownBridge implements PluginIntegrationBridge {

        private final String integrationId;
        private final String pluginName;

        private TestTownBridge() {
            this("rdt", "RDT");
        }

        private TestTownBridge(final String integrationId, final String pluginName) {
            this.integrationId = integrationId;
            this.pluginName = pluginName;
        }

        @Override
        public String getIntegrationId() {
            return this.integrationId;
        }

        @Override
        public String getPluginName() {
            return this.pluginName;
        }

        @Override
        public String getCategory() {
            return "TOWNS";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public double getValue(final Player player, final String key) {
            return 0.0D;
        }
    }

    private static final class UnavailableTownBridge extends TestTownBridge {

        private UnavailableTownBridge(final String integrationId, final String pluginName) {
            super(integrationId, pluginName);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    }
}
