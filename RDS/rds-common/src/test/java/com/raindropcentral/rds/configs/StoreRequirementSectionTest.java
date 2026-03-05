package com.raindropcentral.rds.configs;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import com.raindropcentral.rplatform.requirement.config.RequirementFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests store requirement section behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class StoreRequirementSectionTest {

    @Test
    void normalizesFlatItemRequirementMapsForRequirementFactory() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            supplies:
              type: "ITEM"
              icon:
                type: "DIRT"
              requiredItems:
                dirt:
                  type: "DIRT"
                  amount: 32
            """);

        final StoreRequirementSection section = StoreRequirementSection.fromConfigurationSection(
            "supplies",
            configuration.getConfigurationSection("supplies")
        );

        final Map<String, Object> requirementMap = section.toRequirementMap();
        assertEquals("ITEM", section.getType());
        assertEquals("ITEM", requirementMap.get("type"));
        assertInstanceOf(List.class, requirementMap.get("requiredItems"));
        assertEquals(1, ((List<?>) requirementMap.get("requiredItems")).size());
    }

    @Test
    void infersItemTypeFromNestedRequirementSections() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            supplies:
              icon:
                type: "DIRT"
              itemRequirement:
                consumeOnComplete: true
                requiredItems:
                  dirt:
                    type: "DIRT"
                    amount: 32
            """);

        final StoreRequirementSection section = StoreRequirementSection.fromConfigurationSection(
            "supplies",
            configuration.getConfigurationSection("supplies")
        );

        final Map<String, Object> requirementMap = section.toRequirementMap();
        assertEquals("ITEM", section.getType());
        assertEquals("ITEM", requirementMap.get("type"));
        assertInstanceOf(List.class, requirementMap.get("requiredItems"));
        assertEquals(Boolean.TRUE, requirementMap.get("consumeOnComplete"));
    }

    @Test
    void normalizesCoreRequirementTypesForRequirementFactory() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            currency_gate:
              icon:
                type: "GOLD_INGOT"
              currencyRequirement:
                requiredCurrencies:
                  vault: 1250.0
                consumeOnComplete: true
            experience_gate:
              icon:
                type: "EXPERIENCE_BOTTLE"
              experienceRequirement:
                requiredLevel: 15
                requiredType: "LEVEL"
            permission_gate:
              icon:
                type: "PAPER"
              permissionRequirement:
                requiredPermissions:
                  - "rds.store.buy"
                requireAll: true
            location_gate:
              icon:
                type: "COMPASS"
              locationRequirement:
                world: "world"
                x: 10
                y: 64
                z: 20
                distance: 5
            playtime_gate:
              icon:
                type: "CLOCK"
              playtimeRequirement:
                worlds:
                  - "world"
                  - "world_nether"
                worldPlaytimeMinutes: 30
            composite_gate:
              icon:
                type: "CRAFTING_TABLE"
              compositeRequirement:
                compositeOperator: "MINIMUM"
                minimumRequired: 1
                subRequirements:
                  level_gate:
                    experienceRequirement:
                      requiredLevel: 5
                  permission_gate:
                    permissionRequirement:
                      requiredPermissions:
                        - "rds.store.vip"
            choice_gate:
              icon:
                type: "HOPPER"
              choiceRequirement:
                minimumRequired: 1
                choices:
                  money:
                    currencyRequirement:
                      requiredCurrencies:
                        vault: 1000.0
                  access:
                    permissionRequirement:
                      requiredPermissions:
                        - "rds.store.choice"
            timed_gate:
              icon:
                type: "CLOCK"
              timeBasedRequirement:
                timeConstraintSeconds: 300
                delegate:
                  permissionRequirement:
                    requiredPermissions:
                      - "rds.store.timed"
            plugin_gate:
              icon:
                type: "BEACON"
              pluginRequirement:
                plugin: "jobsreborn"
                category: "JOBS"
                requiredValues:
                  miner: 10
            """);

        final RequirementFactory factory = RequirementFactory.getInstance();
        final Map<String, String> expectedTypes = Map.of(
            "currency_gate", "CURRENCY",
            "experience_gate", "EXPERIENCE_LEVEL",
            "permission_gate", "PERMISSION",
            "location_gate", "LOCATION",
            "playtime_gate", "PLAYTIME",
            "composite_gate", "COMPOSITE",
            "choice_gate", "CHOICE",
            "timed_gate", "TIME_BASED",
            "plugin_gate", "PLUGIN"
        );

        for (final Map.Entry<String, String> entry : expectedTypes.entrySet()) {
            final StoreRequirementSection section = StoreRequirementSection.fromConfigurationSection(
                entry.getKey(),
                configuration.getConfigurationSection(entry.getKey())
            );

            final Map<String, Object> requirementMap = section.toRequirementMap();
            assertEquals(entry.getValue(), section.getType());
            assertEquals(entry.getValue(), factory.fromMap(requirementMap).getTypeId());
        }

        final StoreRequirementSection locationSection = StoreRequirementSection.fromConfigurationSection(
            "location_gate",
            configuration.getConfigurationSection("location_gate")
        );
        final StoreRequirementSection playtimeSection = StoreRequirementSection.fromConfigurationSection(
            "playtime_gate",
            configuration.getConfigurationSection("playtime_gate")
        );
        assertEquals(
            Double.class,
            ((Map<?, ?>) locationSection.toRequirementMap().get("requiredCoordinates")).get("x").getClass()
        );
        assertEquals(
            Long.class,
            ((Map<?, ?>) playtimeSection.toRequirementMap().get("worldPlaytimeRequirements")).get("world").getClass()
        );
    }
}
