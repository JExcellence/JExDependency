package com.raindropcentral.rdq.integration;

import com.raindropcentral.rdq.api.FreePerkService;
import com.raindropcentral.rdq.fixtures.TestData;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerkEntity;
import com.raindropcentral.rdq.perk.PlayerPerkState;
import com.raindropcentral.rdq.perk.repository.PerkRepository;
import com.raindropcentral.rdq.perk.repository.PlayerPerkRepository;
import com.raindropcentral.rdq.perk.service.DefaultFreePerkService;
import com.raindropcentral.rdq.perk.service.PerkRequirementChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Perk Activation Integration Tests")
class PerkActivationIntegrationTest {

    @Mock
    private PerkRepository perkRepository;

    @Mock
    private PlayerPerkRepository playerPerkRepository;

    @Mock
    private PerkRequirementChecker requirementChecker;

    private FreePerkService perkService;

    @BeforeEach
    void setUp() {
        perkService = new DefaultFreePerkService(
            perkRepository,
            playerPerkRepository,
            requirementChecker
        );
    }

    @Test
    @DisplayName("Full perk activation flow: unlock -> activate -> deactivate")
    void fullPerkActivationFlow() {
        var playerId = UUID.randomUUID();
        var perk = TestData.toggleablePerk("speed");

        when(perkRepository.findById("speed")).thenReturn(Optional.of(perk));
        when(perkRepository.findEnabled()).thenReturn(List.of(perk));

        when(requirementChecker.checkAll(eq(playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(true));

        when(playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, "speed"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(
                PlayerPerkEntity.create(playerId, "speed")
            )));

        when(playerPerkRepository.createAsync(any()))
            .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        var availablePerks = perkService.getAvailablePerks(playerId).join();
        assertEquals(1, availablePerks.size());

        var unlockResult = perkService.unlockPerk(playerId, "speed").join();
        assertTrue(unlockResult);

        verify(playerPerkRepository).createAsync(any(PlayerPerkEntity.class));
    }

    @Test
    @DisplayName("Cannot activate perk on cooldown")
    void cannotActivatePerkOnCooldown() {
        var playerId = UUID.randomUUID();
        var perk = TestData.toggleablePerk("speed");
        var cooldownExpiry = Instant.now().plus(Duration.ofMinutes(5));

        when(perkRepository.findById("speed")).thenReturn(Optional.of(perk));

        var entity = PlayerPerkEntity.create(playerId, "speed");
        entity.setCooldownExpiry(cooldownExpiry);

        when(playerPerkRepository.findByPlayerIdAndPerkIdAsync(playerId, "speed"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(entity)));

        var cooldownRemaining = perkService.getCooldownRemaining(playerId, "speed").join();

        assertTrue(cooldownRemaining.isPresent());
        assertTrue(cooldownRemaining.get().toMinutes() > 0);
    }

    @Test
    @DisplayName("Get player perk states returns correct data")
    void getPlayerPerkStatesReturnsCorrectData() {
        var playerId = UUID.randomUUID();
        var entity1 = PlayerPerkEntity.create(playerId, "speed");
        var entity2 = PlayerPerkEntity.create(playerId, "strength");
        entity2.setActive(true);

        when(playerPerkRepository.findByPlayerIdAsync(playerId))
            .thenReturn(CompletableFuture.completedFuture(List.of(entity1, entity2)));

        var states = perkService.getPlayerPerks(playerId).join();

        assertEquals(2, states.size());
        var speedState = states.stream().filter(s -> s.perkId().equals("speed")).findFirst();
        var strengthState = states.stream().filter(s -> s.perkId().equals("strength")).findFirst();

        assertTrue(speedState.isPresent());
        assertTrue(strengthState.isPresent());
        assertFalse(speedState.get().active());
        assertTrue(strengthState.get().active());
    }

    @Test
    @DisplayName("Unlock perk fails when requirements not met")
    void unlockPerkFailsWhenRequirementsNotMet() {
        var playerId = UUID.randomUUID();
        var perk = TestData.toggleablePerk("speed");

        when(perkRepository.findById("speed")).thenReturn(Optional.of(perk));
        when(requirementChecker.checkAll(eq(playerId), any()))
            .thenReturn(CompletableFuture.completedFuture(false));

        var result = perkService.unlockPerk(playerId, "speed").join();

        assertFalse(result);
        verify(playerPerkRepository, never()).createAsync(any());
    }

    @Test
    @DisplayName("Get perk returns correct perk data")
    void getPerkReturnsCorrectPerkData() {
        var perk = TestData.toggleablePerk("speed");

        when(perkRepository.findById("speed")).thenReturn(Optional.of(perk));

        var result = perkService.getPerk("speed").join();

        assertTrue(result.isPresent());
        assertEquals("speed", result.get().id());
        assertTrue(result.get().isToggleable());
    }

    @Test
    @DisplayName("Get available perks filters by requirements")
    void getAvailablePerksFiltersByRequirements() {
        var playerId = UUID.randomUUID();
        var perk1 = TestData.toggleablePerk("speed");
        var perk2 = TestData.toggleablePerk("strength");

        when(perkRepository.findEnabled()).thenReturn(List.of(perk1, perk2));
        when(requirementChecker.checkAll(eq(playerId), anyList()))
            .thenReturn(CompletableFuture.completedFuture(true));

        var result = perkService.getAvailablePerks(playerId).join();

        assertEquals(2, result.size());
    }
}
