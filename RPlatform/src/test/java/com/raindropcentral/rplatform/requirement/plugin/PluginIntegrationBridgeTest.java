package com.raindropcentral.rplatform.requirement.plugin;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests default helper methods in {@link PluginIntegrationBridge}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PluginIntegrationBridgeTest {

    @Test
    void getValuesFiltersBlankKeysAndKeepsInsertionOrder() {
        final PluginIntegrationBridge bridge = new TestBridge();

        final Map<String, Double> values = bridge.getValues(
            null,
            "mining",
            " ",
            null,
            "fishing"
        );

        assertEquals(Map.of("mining", 6.0D, "fishing", 7.0D), values);
    }

    @Test
    void defaultConsumeReturnsFalse() {
        final PluginIntegrationBridge bridge = new TestBridge();

        assertFalse(bridge.consume(null, "mining", 1.0D));
    }

    private static final class TestBridge implements PluginIntegrationBridge {

        @Override
        public String getIntegrationId() {
            return "test";
        }

        @Override
        public String getPluginName() {
            return "TestPlugin";
        }

        @Override
        public String getCategory() {
            return "SKILLS";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public double getValue(final Player player, final String key) {
            return switch (key) {
                case "mining" -> 6.0D;
                case "fishing" -> 7.0D;
                default -> 0.0D;
            };
        }
    }
}
