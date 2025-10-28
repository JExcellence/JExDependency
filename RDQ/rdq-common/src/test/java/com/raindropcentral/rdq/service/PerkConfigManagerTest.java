package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.config.PerkConfigManager;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PerkConfigManagerTest {

    private PerkConfigManager configManager;
    private PerkConfig config;

    @BeforeEach
    void setUp() {
        configManager = new PerkConfigManager();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        config = new PerkConfig(
            "speed",
            "Speed Boost",
            "Increases your movement speed",
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            0L,
            0L,
            metadata,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );
    }

    @Test
    void testGetConfigAfterLoad() {
        configManager.cache.put(config.id(), config);

        PerkConfig retrieved = configManager.getConfig("speed");

        assertNotNull(retrieved);
        assertEquals("speed", retrieved.id());
    }

    @Test
    void testGetNonexistentConfig() {
        PerkConfig retrieved = configManager.getConfig("nonexistent");

        assertNull(retrieved);
    }

    @Test
    void testGetConfigsByCategory() {
        configManager.cache.put(config.id(), config);

        List<PerkConfig> configs = configManager.getConfigsByCategory(EPerkCategory.MOVEMENT);

        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals("speed", configs.get(0).id());
    }

    @Test
    void testGetConfigsByCategoryEmpty() {
        List<PerkConfig> configs = configManager.getConfigsByCategory(EPerkCategory.COMBAT);

        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void testGetAllConfigs() {
        configManager.cache.put(config.id(), config);

        List<PerkConfig> configs = configManager.getAllConfigs();

        assertNotNull(configs);
        assertEquals(1, configs.size());
    }

    @Test
    void testGetAllConfigsEmpty() {
        List<PerkConfig> configs = configManager.getAllConfigs();

        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void testInvalidateCache() {
        configManager.cache.put(config.id(), config);
        assertNotNull(configManager.getConfig("speed"));

        configManager.invalidateCache("speed");

        assertNull(configManager.getConfig("speed"));
    }

    @Test
    void testInvalidateAll() {
        configManager.cache.put(config.id(), config);
        assertNotNull(configManager.getConfig("speed"));

        configManager.invalidateAll();

        assertNull(configManager.getConfig("speed"));
    }

    @Test
    void testRegisterReloadListener() {
        boolean[] listenerCalled = {false};
        Runnable listener = () -> listenerCalled[0] = true;

        configManager.registerReloadListener(listener);
        configManager.onReload();

        assertTrue(listenerCalled[0]);
    }

    @Test
    void testOnReloadClearsCache() {
        configManager.cache.put(config.id(), config);
        assertNotNull(configManager.getConfig("speed"));

        configManager.onReload();

        assertNull(configManager.getConfig("speed"));
    }

    @Test
    void testMultipleConfigs() {
        configManager.cache.put(config.id(), config);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("effectType", "JUMP");

        PerkConfig jumpConfig = new PerkConfig(
            "jump",
            "Jump Boost",
            "Increases your jump height",
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "FEATHER",
            100,
            true,
            0L,
            0L,
            metadata2,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        configManager.cache.put(jumpConfig.id(), jumpConfig);

        List<PerkConfig> configs = configManager.getAllConfigs();

        assertEquals(2, configs.size());
    }

    @Test
    void testGetConfigsByCategoryMultiple() {
        configManager.cache.put(config.id(), config);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("effectType", "JUMP");

        PerkConfig jumpConfig = new PerkConfig(
            "jump",
            "Jump Boost",
            null,
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "FEATHER",
            100,
            true,
            0L,
            0L,
            metadata2,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        configManager.cache.put(jumpConfig.id(), jumpConfig);

        List<PerkConfig> movementConfigs = configManager.getConfigsByCategory(EPerkCategory.MOVEMENT);

        assertEquals(2, movementConfigs.size());
    }

    @Test
    void testCacheMaximumSize() {
        for (int i = 0; i < 1001; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("effectType", "SPEED");

            PerkConfig testConfig = new PerkConfig(
                "perk_" + i,
                "Perk " + i,
                null,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkCategory.MOVEMENT,
                "SUGAR",
                100,
                true,
                0L,
                0L,
                metadata,
                List.of(),
                List.of(),
                new HashMap<>(),
                new HashMap<>()
            );

            configManager.cache.put(testConfig.id(), testConfig);
        }

        assertTrue(configManager.getAllConfigs().size() <= 1000);
    }
}
