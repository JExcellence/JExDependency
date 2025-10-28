package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.*;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerkManagerIntegrationTest {

    private PerkManager perkManager;
    private PerkRegistry perkRegistry;
    private PerkTypeRegistry typeRegistry;
    private PerkCache perkCache;
    private CooldownService cooldownService;

    @Mock
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        typeRegistry = new PerkTypeRegistry();
        perkRegistry = new PerkRegistry(typeRegistry);
        perkCache = new PerkCache();
        cooldownService = new CooldownService();
        perkManager = new PerkManager(perkRegistry, perkCache, cooldownService);

        typeRegistry.register(new ToggleablePerkType());
        typeRegistry.register(new EventPerkType());
    }

    @Test
    void testActivatePerk() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        perkRegistry.register(config);

        ActivationResult result = perkManager.activate(mockPlayer, "speed");
        assertTrue(result.success());
    }

    @Test
    void testDeactivatePerk() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        perkRegistry.register(config);
        perkManager.activate(mockPlayer, "speed");

        DeactivationResult result = perkManager.deactivate(mockPlayer, "speed");
        assertTrue(result.success());
    }

    @Test
    void testIsActive() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        perkRegistry.register(config);
        perkManager.activate(mockPlayer, "speed");

        assertTrue(perkManager.isActive(mockPlayer, "speed"));
    }

    @Test
    void testGetActivePerks() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        perkRegistry.register(config);
        perkManager.activate(mockPlayer, "speed");

        List<LoadedPerk> activePerks = perkManager.getActivePerks(mockPlayer);
        assertEquals(1, activePerks.size());
        assertEquals("speed", activePerks.get(0).getId());
    }

    @Test
    void testGetAllPerks() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        perkRegistry.register(config);

        List<LoadedPerk> allPerks = perkManager.getAllPerks();
        assertEquals(1, allPerks.size());
    }

    @Test
    void testClearPlayerState() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        perkRegistry.register(config);
        perkManager.activate(mockPlayer, "speed");
        perkManager.clearPlayerState(mockPlayer);

        assertFalse(perkManager.isActive(mockPlayer, "speed"));
    }
}
