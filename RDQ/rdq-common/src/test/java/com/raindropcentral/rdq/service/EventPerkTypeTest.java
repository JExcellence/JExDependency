package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.EventPerkType;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
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
class EventPerkTypeTest {

    @Mock
    private Player player;

    private EventPerkType perkType;
    private PerkConfig config;
    private LoadedPerk loadedPerk;

    @BeforeEach
    void setUp() {
        perkType = new EventPerkType();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("triggerEvent", "PLAYER_MOVE");
        metadata.put("effectType", "SPEED");

        Map<String, Integer> permissionAmplifiers = new HashMap<>();
        permissionAmplifiers.put("rdq.perk.event.level1", 0);
        permissionAmplifiers.put("rdq.perk.event.level2", 1);

        config = new PerkConfig(
            "event_speed",
            "Event Speed",
            "Speed boost on event trigger",
            EPerkType.EVENT_TRIGGERED,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            0L,
            5L,
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
        assertEquals("EVENT", perkType.getTypeId());
    }

    @Test
    void testCanHandle() {
        assertTrue(perkType.canHandle(config));

        Map<String, Object> metadata = new HashMap<>();
        PerkConfig toggleConfig = new PerkConfig(
            "test",
            "Test",
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

        assertFalse(perkType.canHandle(toggleConfig));
    }

    @Test
    void testCreateLoadedPerk() {
        LoadedPerk loaded = perkType.createLoadedPerk(config);
        assertNotNull(loaded);
        assertEquals(config, loaded.config());
        assertEquals(perkType, loaded.type());
    }

    @Test
    void testActivateReturnsTrue() {
        boolean result = perkType.activate(player, loadedPerk);
        assertTrue(result);
    }

    @Test
    void testDeactivateReturnsTrue() {
        boolean result = perkType.deactivate(player, loadedPerk);
        assertTrue(result);
    }

    @Test
    void testTriggerAppliesEffect() {
        when(player.hasPermission(anyString())).thenReturn(false);

        perkType.trigger(player, loadedPerk);

        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testTriggerWithPermissionAmplifier() {
        when(player.hasPermission("rdq.perk.event.level2")).thenReturn(true);

        perkType.trigger(player, loadedPerk);

        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testTriggerWithInvalidEffectType() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("triggerEvent", "PLAYER_MOVE");
        metadata.put("effectType", "");

        PerkConfig invalidConfig = new PerkConfig(
            "invalid",
            "Invalid",
            null,
            EPerkType.EVENT_TRIGGERED,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            0L,
            5L,
            metadata,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        LoadedPerk invalidPerk = new LoadedPerk(invalidConfig, perkType);
        perkType.trigger(player, invalidPerk);

        verifyNoInteractions(player);
    }

    @Test
    void testTriggerWithNullDuration() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("triggerEvent", "PLAYER_MOVE");
        metadata.put("effectType", "SPEED");

        PerkConfig nullDurationConfig = new PerkConfig(
            "null_duration",
            "Null Duration",
            null,
            EPerkType.EVENT_TRIGGERED,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            0L,
            null,
            metadata,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        LoadedPerk nullDurationPerk = new LoadedPerk(nullDurationConfig, perkType);
        when(player.hasPermission(anyString())).thenReturn(false);

        perkType.trigger(player, nullDurationPerk);

        verify(player).addPotionEffect(any(PotionEffect.class));
    }
}
