package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.ToggleablePerkType;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerkTypeTest {

    private ToggleablePerkType perkType;

    @Mock
    private Player mockPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        perkType = new ToggleablePerkType();
    }

    @Test
    void testToggleablePerkTypeGetTypeId() {
        assertEquals("TOGGLEABLE", perkType.getTypeId());
    }

    @Test
    void testToggleablePerkTypeCanHandle() {
        PerkConfig config = new PerkConfig(
            "test", "Test", "Test Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "PAPER", 100, true, null, null,
            new HashMap<>(), new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        assertTrue(perkType.canHandle(config));
    }

    @Test
    void testToggleablePerkTypeActivate() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        LoadedPerk loaded = perkType.createLoadedPerk(config);
        boolean result = perkType.activate(mockPlayer, loaded);

        assertTrue(result);
        verify(mockPlayer).addPotionEffect(any());
    }

    @Test
    void testToggleablePerkTypeDeactivate() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectType", "SPEED");

        PerkConfig config = new PerkConfig(
            "speed", "Speed", "Speed Perk", EPerkType.TOGGLEABLE_PASSIVE,
            EPerkCategory.UTILITY, "SUGAR", 100, true, null, null,
            metadata, new ArrayList<>(), new ArrayList<>(),
            new HashMap<>(), new HashMap<>()
        );

        LoadedPerk loaded = perkType.createLoadedPerk(config);
        boolean result = perkType.deactivate(mockPlayer, loaded);

        assertTrue(result);
        verify(mockPlayer).removePotionEffect(PotionEffectType.SPEED);
    }
}
