package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.*;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerkRegistryTest {

    @Mock
    private PerkTypeRegistry typeRegistry;

    private PerkRegistry perkRegistry;
    private PerkConfig config;
    private PerkType perkType;

    @BeforeEach
    void setUp() {
        perkRegistry = new PerkRegistry(typeRegistry);

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

        perkType = new ToggleablePerkType();
        when(typeRegistry.get("TOGGLEABLE_PASSIVE")).thenReturn(perkType);
    }

    @Test
    void testRegisterPerk() {
        perkRegistry.register(config);

        assertTrue(perkRegistry.isRegistered("speed"));
        assertNotNull(perkRegistry.get("speed"));
    }

    @Test
    void testGetPerk() {
        perkRegistry.register(config);

        LoadedPerk perk = perkRegistry.get("speed");

        assertNotNull(perk);
        assertEquals("speed", perk.getId());
    }

    @Test
    void testGetNonexistentPerk() {
        LoadedPerk perk = perkRegistry.get("nonexistent");

        assertNull(perk);
    }

    @Test
    void testUnregisterPerk() {
        perkRegistry.register(config);
        assertTrue(perkRegistry.isRegistered("speed"));

        perkRegistry.unregister("speed");

        assertFalse(perkRegistry.isRegistered("speed"));
        assertNull(perkRegistry.get("speed"));
    }

    @Test
    void testGetByCategory() {
        perkRegistry.register(config);

        List<LoadedPerk> perks = perkRegistry.getByCategory(EPerkCategory.MOVEMENT);

        assertNotNull(perks);
        assertEquals(1, perks.size());
        assertEquals("speed", perks.get(0).getId());
    }

    @Test
    void testGetByCategoryEmpty() {
        List<LoadedPerk> perks = perkRegistry.getByCategory(EPerkCategory.COMBAT);

        assertNotNull(perks);
        assertTrue(perks.isEmpty());
    }

    @Test
    void testGetAll() {
        perkRegistry.register(config);

        List<LoadedPerk> perks = perkRegistry.getAll();

        assertNotNull(perks);
        assertEquals(1, perks.size());
    }

    @Test
    void testGetAllEmpty() {
        List<LoadedPerk> perks = perkRegistry.getAll();

        assertNotNull(perks);
        assertTrue(perks.isEmpty());
    }

    @Test
    void testIsRegistered() {
        perkRegistry.register(config);

        assertTrue(perkRegistry.isRegistered("speed"));
        assertFalse(perkRegistry.isRegistered("nonexistent"));
    }

    @Test
    void testRegisterMultiplePerks() {
        perkRegistry.register(config);

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

        perkRegistry.register(jumpConfig);

        assertEquals(2, perkRegistry.getAll().size());
        assertTrue(perkRegistry.isRegistered("speed"));
        assertTrue(perkRegistry.isRegistered("jump"));
    }

    @Test
    void testRegisterReloadListener() {
        Runnable listener = mock(Runnable.class);
        perkRegistry.registerReloadListener(listener);

        perkRegistry.onReload();

        verify(listener).run();
    }

    @Test
    void testOnReloadClearsPerks() {
        perkRegistry.register(config);
        assertTrue(perkRegistry.isRegistered("speed"));

        perkRegistry.onReload();

        assertFalse(perkRegistry.isRegistered("speed"));
        assertTrue(perkRegistry.getAll().isEmpty());
    }

    @Test
    void testRegisterWithUnknownType() {
        when(typeRegistry.get("UNKNOWN_TYPE")).thenReturn(null);

        PerkConfig unknownConfig = new PerkConfig(
            "unknown",
            "Unknown",
            null,
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            0L,
            0L,
            new HashMap<>(),
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        assertThrows(IllegalArgumentException.class, () -> perkRegistry.register(unknownConfig));
    }

    @Test
    void testGetByCategoryMultiplePerks() {
        perkRegistry.register(config);

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

        perkRegistry.register(jumpConfig);

        List<LoadedPerk> movementPerks = perkRegistry.getByCategory(EPerkCategory.MOVEMENT);

        assertEquals(2, movementPerks.size());
    }
}
