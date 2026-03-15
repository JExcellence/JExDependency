package com.raindropcentral.rdr.configs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link StoreRequirementIconSection} icon parsing behavior.
 */
class StoreRequirementIconSectionTest {

    @Test
    void defaultsToPaperWhenTypeIsMissingOrBlank() {
        assertEquals("PAPER", new StoreRequirementIconSection(null).getType());
        assertEquals("PAPER", new StoreRequirementIconSection("   ").getType());
    }

    @Test
    void trimsConfiguredIconType() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            icon:
              type: "  DIAMOND  "
            """);

        final StoreRequirementIconSection iconSection = StoreRequirementIconSection.fromConfigurationSection(
            configuration.getConfigurationSection("icon")
        );

        assertEquals("DIAMOND", iconSection.getType());
    }
}
