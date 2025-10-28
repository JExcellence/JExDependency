package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.ToggleablePerkType;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleablePerkTypeTest {

    @Mock
    private Player player;

    private ToggleablePerkType perkType;
    private PerkConfig config;
    private LoadedPerk loadedPerk;

    @BeforeEach
    void setUp() {
        perkType = new ToggleablePerkType();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        Map<String, Integer> permissionAmplifiers = new HashMap<>();
        permissionAmplifiers.put("rdq.perk.speed.level1", 0);
        permissionAmplifiers.put("rdq.perk.speed.level2", 1);
        permissionAmplifiers.put("rdq.perk.speed.level3", 2);

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
            permissionAmplifiers
        );

        loadedPerk = new LoadedPerk(config, perkType);
    }

    @Test
    void testGetTypeId() {
        assertEquals("TOGGLEABLE", perkType.getTypeId());
    }

    @Test
    void testCanHandle() {
        assertTrue(perkType.canHandle(config));

        Map<String, Object> metadata = new HashMap<>();
        PerkConfig eventConfig = new PerkConfig(
            "test",
            "Test",
            null,
            EPerkType.EVENT_TRIGGERED,
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

        assertFalse(perkType.canHandle(eventConfig));
    }

    @Test
    void testCreateLoadedPerk() {
        LoadedPerk loaded = perkType.createLoadedPerk(config);
        assertNotNull(loaded);
        assertEquals(config, loaded.config());
        assertEquals(perkType, loaded.type());
    }

    @Test
    void testActivateWithDefaultAmplifier() {
        when(player.hasPermission(anyString())).thenReturn(false);

        boolean result = perkType.activate(player, loadedPerk);

        assertTrue(result);
        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testActivateWithPermissionAmplifier() {
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(true);

        boolean result = perkType.activate(player, loadedPerk);

        assertTrue(result);
        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testDeactivate() {
        boolean result = perkType.deactivate(player, loadedPerk);

        assertTrue(result);
        verify(player).removePotionEffect(PotionEffectType.SPEED);
    }

    @Test
    void testDeactivateWithInvalidEffectType() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "INVALID_EFFECT");

        PerkConfig invalidConfig = new PerkConfig(
            "invalid",
            "Invalid",
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

        LoadedPerk invalidPerk = new LoadedPerk(invalidConfig, perkType);
        boolean result = perkType.deactivate(player, invalidPerk);

        assertFalse(result);
    }

    @Test
    void testGetPermissionAmplifier() {
        when(player.hasPermission("rdq.perk.speed.level1")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(true);
        when(player.hasPermission("rdq.perk.speed.level3")).thenReturn(false);

        boolean result = perkType.activate(player, loadedPerk);

        assertTrue(result);
        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testGetDurationTicksWithValidDuration() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig durationConfig = new PerkConfig(
            "speed",
            "Speed Boost",
            null,
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            0L,
            10L,
            metadata,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        LoadedPerk durationPerk = new LoadedPerk(durationConfig, perkType);
        when(player.hasPermission(anyString())).thenReturn(false);

        boolean result = perkType.activate(player, durationPerk);

        assertTrue(result);
        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testTriggerDoesNothing() {
        perkType.trigger(player, loadedPerk);
        verifyNoInteractions(player);
    }
}
