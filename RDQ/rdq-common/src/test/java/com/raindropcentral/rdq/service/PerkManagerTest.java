package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.*;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerkManagerTest {

    @Mock
    private Player player;

    @Mock
    private PerkRegistry perkRegistry;

    @Mock
    private PerkCache perkCache;

    @Mock
    private CooldownService cooldownService;

    private PerkManager perkManager;
    private PerkConfig config;
    private LoadedPerk loadedPerk;

    @BeforeEach
    void setUp() {
        perkManager = new PerkManager(perkRegistry, perkCache, cooldownService);

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

        loadedPerk = new LoadedPerk(config, new ToggleablePerkType());

        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @Test
    void testActivateSuccessfully() {
        when(perkRegistry.get("speed")).thenReturn(loadedPerk);
        when(cooldownService.isOnCooldown(player, "speed")).thenReturn(false);
        when(perkCache.getOrCreate(player)).thenReturn(new PlayerPerkState(player.getUniqueId()));

        ActivationResult result = perkManager.activate(player, "speed");

        assertTrue(result.success());
        verify(perkRegistry).get("speed");
        verify(cooldownService).isOnCooldown(player, "speed");
    }

    @Test
    void testActivatePerkNotFound() {
        when(perkRegistry.get("nonexistent")).thenReturn(null);

        ActivationResult result = perkManager.activate(player, "nonexistent");

        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    void testActivatePerkDisabled() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig disabledConfig = new PerkConfig(
            "speed",
            "Speed Boost",
            null,
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.MOVEMENT,
            "SUGAR",
            100,
            false,
            0L,
            0L,
            metadata,
            List.of(),
            List.of(),
            new HashMap<>(),
            new HashMap<>()
        );

        LoadedPerk disabledPerk = new LoadedPerk(disabledConfig, new ToggleablePerkType());
        when(perkRegistry.get("speed")).thenReturn(disabledPerk);

        ActivationResult result = perkManager.activate(player, "speed");

        assertFalse(result.success());
        assertTrue(result.message().contains("disabled"));
    }

    @Test
    void testActivatePerkOnCooldown() {
        when(perkRegistry.get("speed")).thenReturn(loadedPerk);
        when(cooldownService.isOnCooldown(player, "speed")).thenReturn(true);
        when(cooldownService.getRemainingCooldown(player, "speed")).thenReturn(5L);

        ActivationResult result = perkManager.activate(player, "speed");

        assertFalse(result.success());
        assertTrue(result.message().contains("cooldown"));
    }

    @Test
    void testDeactivateSuccessfully() {
        when(perkRegistry.get("speed")).thenReturn(loadedPerk);
        when(perkCache.getOrCreate(player)).thenReturn(new PlayerPerkState(player.getUniqueId()));

        DeactivationResult result = perkManager.deactivate(player, "speed");

        assertTrue(result.success());
        verify(perkRegistry).get("speed");
    }

    @Test
    void testDeactivatePerkNotFound() {
        when(perkRegistry.get("nonexistent")).thenReturn(null);

        DeactivationResult result = perkManager.deactivate(player, "nonexistent");

        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    void testIsActiveWhenNotActive() {
        when(perkCache.get(player)).thenReturn(null);

        boolean active = perkManager.isActive(player, "speed");

        assertFalse(active);
    }

    @Test
    void testIsActiveWhenActive() {
        PlayerPerkState state = new PlayerPerkState(player.getUniqueId());
        state.setActivationTime("speed", System.currentTimeMillis());

        when(perkCache.get(player)).thenReturn(state);

        boolean active = perkManager.isActive(player, "speed");

        assertTrue(active);
    }

    @Test
    void testGetActivePerks() {
        PlayerPerkState state = new PlayerPerkState(player.getUniqueId());
        state.setActivationTime("speed", System.currentTimeMillis());

        when(perkCache.get(player)).thenReturn(state);
        when(perkRegistry.getAll()).thenReturn(List.of(loadedPerk));

        List<LoadedPerk> activePerks = perkManager.getActivePerks(player);

        assertNotNull(activePerks);
        verify(perkRegistry).getAll();
    }

    @Test
    void testGetPerk() {
        when(perkRegistry.get("speed")).thenReturn(loadedPerk);

        LoadedPerk perk = perkManager.getPerk("speed");

        assertNotNull(perk);
        assertEquals("speed", perk.getId());
    }

    @Test
    void testGetAllPerks() {
        when(perkRegistry.getAll()).thenReturn(List.of(loadedPerk));

        List<LoadedPerk> perks = perkManager.getAllPerks();

        assertNotNull(perks);
        assertEquals(1, perks.size());
    }

    @Test
    void testClearPlayerState() {
        perkManager.clearPlayerState(player);

        verify(perkCache).invalidate(player);
        verify(cooldownService).clearAllCooldowns(player);
    }

    @Test
    void testActivateAsync() {
        when(perkRegistry.get("speed")).thenReturn(loadedPerk);
        when(cooldownService.isOnCooldown(player, "speed")).thenReturn(false);
        when(perkCache.getOrCreate(player)).thenReturn(new PlayerPerkState(player.getUniqueId()));

        var future = perkManager.activateAsync(player, "speed");

        assertNotNull(future);
        assertTrue(future.isDone());
    }

    @Test
    void testDeactivateAsync() {
        when(perkRegistry.get("speed")).thenReturn(loadedPerk);
        when(perkCache.getOrCreate(player)).thenReturn(new PlayerPerkState(player.getUniqueId()));

        var future = perkManager.deactivateAsync(player, "speed");

        assertNotNull(future);
        assertTrue(future.isDone());
    }
}
