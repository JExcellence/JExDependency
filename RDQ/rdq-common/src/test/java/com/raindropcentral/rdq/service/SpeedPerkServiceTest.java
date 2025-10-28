package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.config.PerkConfig;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.runtime.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpeedPerkServiceTest {

    @Mock
    private Player player;

    @Mock
    private PerkManager perkManager;

    @Mock
    private PerkEventBus perkEventBus;

    private SpeedPerkService speedPerkService;
    private PerkConfig config;
    private LoadedPerk loadedPerk;

    @BeforeEach
    void setUp() {
        speedPerkService = new SpeedPerkService(perkManager, perkEventBus);

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
    }

    @Test
    void testOnPerkActivatedWithSpeedPerk() {
        when(perkManager.getPerk("speed")).thenReturn(loadedPerk);
        when(player.hasPermission(any())).thenReturn(false);

        speedPerkService.onPerkActivated(player, "speed");

        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testOnPerkActivatedWithDifferentPerk() {
        speedPerkService.onPerkActivated(player, "jump");

        verifyNoInteractions(player);
    }

    @Test
    void testOnPerkDeactivatedWithSpeedPerk() {
        speedPerkService.onPerkDeactivated(player, "speed");

        verify(player).removePotionEffect(PotionEffectType.SPEED);
    }

    @Test
    void testOnPerkDeactivatedWithDifferentPerk() {
        speedPerkService.onPerkDeactivated(player, "jump");

        verifyNoInteractions(player);
    }

    @Test
    void testOnPerkTriggeredDoesNothing() {
        speedPerkService.onPerkTriggered(player, "speed");

        verifyNoInteractions(player);
    }

    @Test
    void testOnPerkCooldownStartDoesNothing() {
        speedPerkService.onPerkCooldownStart(player, "speed", 10);

        verifyNoInteractions(player);
    }

    @Test
    void testOnPerkCooldownEndDoesNothing() {
        speedPerkService.onPerkCooldownEnd(player, "speed");

        verifyNoInteractions(player);
    }

    @Test
    void testApplySpeedEffectWithPermissionAmplifier() {
        when(perkManager.getPerk("speed")).thenReturn(loadedPerk);
        when(player.hasPermission("rdq.perk.speed.level2")).thenReturn(true);

        speedPerkService.onPerkActivated(player, "speed");

        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testApplySpeedEffectWithoutPermission() {
        when(perkManager.getPerk("speed")).thenReturn(loadedPerk);
        when(player.hasPermission(any())).thenReturn(false);

        speedPerkService.onPerkActivated(player, "speed");

        verify(player).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void testApplySpeedEffectWithNullPerk() {
        when(perkManager.getPerk("speed")).thenReturn(null);

        speedPerkService.onPerkActivated(player, "speed");

        verifyNoInteractions(player);
    }

    @Test
    void testRemoveSpeedEffect() {
        speedPerkService.onPerkDeactivated(player, "speed");

        verify(player).removePotionEffect(PotionEffectType.SPEED);
    }

    @Test
    void testServiceRegistersWithEventBus() {
        verify(perkEventBus).register(speedPerkService);
    }

    @Test
    void testMultipleActivationDeactivationCycles() {
        when(perkManager.getPerk("speed")).thenReturn(loadedPerk);
        when(player.hasPermission(any())).thenReturn(false);

        speedPerkService.onPerkActivated(player, "speed");
        speedPerkService.onPerkDeactivated(player, "speed");
        speedPerkService.onPerkActivated(player, "speed");

        verify(player, times(2)).addPotionEffect(any(PotionEffect.class));
        verify(player, times(1)).removePotionEffect(PotionEffectType.SPEED);
    }
}
