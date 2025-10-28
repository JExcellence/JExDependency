package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.PermissionScalingService;
import com.raindropcentral.rdq.perk.runtime.ToggleablePerkType;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionScalingServiceTest {

    @Mock
    private Player player;

    private PermissionScalingService scalingService;
    private PerkConfig config;
    private LoadedPerk loadedPerk;

    @BeforeEach
    void setUp() {
        scalingService = new PermissionScalingService();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        Map<String, Integer> permissionAmplifiers = new HashMap<>();
        permissionAmplifiers.put("rdq.perk.speed.level1", 0);
        permissionAmplifiers.put("rdq.perk.speed.level2", 1);
        permissionAmplifiers.put("rdq.perk.speed.level3", 2);

        Map<String, Long> permissionCooldowns = new HashMap<>();
        permissionCooldowns.put("rdq.perk.speed.vip", 0L);
        permissionCooldowns.put("rdq.perk.speed.premium", 0L);

        config = new PerkConfig(
            "speed",
            "Speed Boost",
            "Increases your movement speed",
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            true,
            10L,
            0L,
            metadata,
            List.of(),
            List.of(),
            permissionCooldowns,
            permissionAmplifiers
        );

        loadedPerk = new LoadedPerk(config, new ToggleablePerkType());
    }

    @Test
    void testGetAmplifierWithoutPermission() {
        when(player.hasPermission("rdq.perk.speed.level1")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level3")).thenReturn(false);

        int amplifier = scalingService.getAmplifier(player, loadedPerk);

        assertEquals(0, amplifier);
    }

    @Test
    void testGetAmplifierWithLevel1Permission() {
        when(player.hasPermission("rdq.perk.speed.level1")).thenReturn(true);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level3")).thenReturn(false);

        int amplifier = scalingService.getAmplifier(player, loadedPerk);

        assertEquals(0, amplifier);
    }

    @Test
    void testGetAmplifierWithLevel2Permission() {
        when(player.hasPermission("rdq.perk.speed.level1")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(true);
        when(player.hasPermission("rdq.perk.speed.level3")).thenReturn(false);

        int amplifier = scalingService.getAmplifier(player, loadedPerk);

        assertEquals(1, amplifier);
    }

    @Test
    void testGetAmplifierWithLevel3Permission() {
        when(player.hasPermission("rdq.perk.speed.level1")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.level3")).thenReturn(true);

        int amplifier = scalingService.getAmplifier(player, loadedPerk);

        assertEquals(2, amplifier);
    }

    @Test
    void testGetAmplifierWithMultiplePermissions() {
        when(player.hasPermission("rdq.perk.speed.level1")).thenReturn(true);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(true);
        when(player.hasPermission("rdq.perk.speed.level3")).thenReturn(false);

        int amplifier = scalingService.getAmplifier(player, loadedPerk);

        assertTrue(amplifier >= 0);
    }

    @Test
    void testGetCooldownWithoutPermission() {
        when(player.hasPermission("rdq.perk.speed.vip")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.premium")).thenReturn(false);

        long cooldown = scalingService.getCooldown(player, loadedPerk);

        assertEquals(10L, cooldown);
    }

    @Test
    void testGetCooldownWithVipPermission() {
        when(player.hasPermission("rdq.perk.speed.vip")).thenReturn(true);
        when(player.hasPermission("rdq.perk.speed.premium")).thenReturn(false);

        long cooldown = scalingService.getCooldown(player, loadedPerk);

        assertEquals(0L, cooldown);
    }

    @Test
    void testGetCooldownWithPremiumPermission() {
        when(player.hasPermission("rdq.perk.speed.vip")).thenReturn(false);
        when(player.hasPermission("rdq.perk.speed.premium")).thenReturn(true);

        long cooldown = scalingService.getCooldown(player, loadedPerk);

        assertEquals(0L, cooldown);
    }

    @Test
    void testGetDurationWithValidDuration() {
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
            30L,
            metadata,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        LoadedPerk durationPerk = new LoadedPerk(durationConfig, new ToggleablePerkType());

        long duration = scalingService.getDuration(player, durationPerk);

        assertEquals(30L, duration);
    }

    @Test
    void testGetDurationWithNullDuration() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig nullDurationConfig = new PerkConfig(
            "speed",
            "Speed Boost",
            null,
            EPerkType.TOGGLEABLE_PASSIVE,
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

        LoadedPerk nullDurationPerk = new LoadedPerk(nullDurationConfig, new ToggleablePerkType());

        long duration = scalingService.getDuration(player, nullDurationPerk);

        assertEquals(Long.MAX_VALUE, duration);
    }

    @Test
    void testGetDurationWithZeroDuration() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig zeroDurationConfig = new PerkConfig(
            "speed",
            "Speed Boost",
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

        LoadedPerk zeroDurationPerk = new LoadedPerk(zeroDurationConfig, new ToggleablePerkType());

        long duration = scalingService.getDuration(player, zeroDurationPerk);

        assertEquals(Long.MAX_VALUE, duration);
    }

    @Test
    void testHasPermissionScaling() {
        assertTrue(scalingService.hasPermissionScaling(loadedPerk));
    }

    @Test
    void testHasNoPermissionScaling() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig noScalingConfig = new PerkConfig(
            "speed",
            "Speed Boost",
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

        LoadedPerk noScalingPerk = new LoadedPerk(noScalingConfig, new ToggleablePerkType());

        assertFalse(scalingService.hasPermissionScaling(noScalingPerk));
    }
}
