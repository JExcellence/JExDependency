package com.raindropcentral.rdq.service.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class PremiumBountyServiceTest {

    private RBountyRepository bountyRepository;
    private PremiumBountyService service;

    @BeforeEach
    void setUp() {
        this.bountyRepository = mock(RBountyRepository.class);
        this.service = new PremiumBountyService(bountyRepository);
    }

    @Test
    void getAllBounties_delegatesToRepository() {
        List<RBounty> expectedBounties = List.of(mock(RBounty.class));
        CompletableFuture<List<RBounty>> repositoryFuture = CompletableFuture.completedFuture(expectedBounties);
        when(bountyRepository.findAllAsync(2, 25)).thenReturn(repositoryFuture);

        CompletableFuture<List<RBounty>> result = service.getAllBounties(2, 25);

        assertSame(repositoryFuture, result, "Service should return the repository future");
        verify(bountyRepository).findAllAsync(2, 25);
    }

    @Test
    void getBountyByPlayer_delegatesToRepository() {
        UUID playerId = UUID.fromString("3b2f47c4-544f-4f92-b64d-0d2b6d5cd0fe");
        CompletableFuture<Optional<RBounty>> repositoryFuture = CompletableFuture.completedFuture(Optional.of(mock(RBounty.class)));
        when(bountyRepository.findByPlayerAsync(playerId)).thenReturn(repositoryFuture);

        CompletableFuture<Optional<RBounty>> result = service.getBountyByPlayer(playerId);

        assertSame(repositoryFuture, result, "Service should return the repository future");
        verify(bountyRepository).findByPlayerAsync(playerId);
    }

    @Test
    void createBounty_constructsEntityAndDelegatesToRepository() {
        RDQPlayer target = mock(RDQPlayer.class);
        Player commissioner = mock(Player.class);
        UUID commissionerId = UUID.fromString("2f4e6eb5-6a8b-4a32-b75c-4bf23fd57151");
        when(commissioner.getUniqueId()).thenReturn(commissionerId);

        Set<RewardItem> rewardItems = new LinkedHashSet<>();
        rewardItems.add(mock(RewardItem.class));
        Map<String, Double> rewardCurrencies = new LinkedHashMap<>();
        rewardCurrencies.put("coins", 25.0);

        CompletableFuture<RBounty> repositoryFuture = new CompletableFuture<>();
        when(bountyRepository.createAsync(any())).thenAnswer(invocation -> {
            RBounty created = invocation.getArgument(0);
            repositoryFuture.complete(created);
            return repositoryFuture;
        });

        CompletableFuture<RBounty> result = service.createBounty(target, commissioner, rewardItems, rewardCurrencies);

        assertSame(repositoryFuture, result, "Service should return the repository future");

        ArgumentCaptor<RBounty> bountyCaptor = ArgumentCaptor.forClass(RBounty.class);
        verify(bountyRepository).createAsync(bountyCaptor.capture());
        RBounty createdBounty = bountyCaptor.getValue();

        assertSame(target, createdBounty.getPlayer(), "Target should be assigned to the bounty");
        assertEquals(commissionerId, createdBounty.getCommissioner(), "Commissioner UUID should be captured");
        assertEquals(rewardItems, createdBounty.getRewardItems(), "Reward items should match the provided collection");
        assertEquals(rewardCurrencies, createdBounty.getRewardCurrencies(), "Reward currencies should match the provided map");
    }

    @Test
    void deleteBounty_delegatesToRepository() {
        Long bountyId = 42L;
        CompletableFuture<Boolean> repositoryFuture = CompletableFuture.completedFuture(Boolean.TRUE);
        when(bountyRepository.deleteAsync(bountyId)).thenReturn(repositoryFuture);

        CompletableFuture<Boolean> result = service.deleteBounty(bountyId);

        assertSame(repositoryFuture, result, "Service should return the repository future");
        verify(bountyRepository).deleteAsync(bountyId);
    }

    @Test
    void updateBounty_delegatesToRepository() {
        RBounty bounty = mock(RBounty.class);
        CompletableFuture<RBounty> repositoryFuture = CompletableFuture.completedFuture(bounty);
        when(bountyRepository.updateAsync(bounty)).thenReturn(repositoryFuture);

        CompletableFuture<RBounty> result = service.updateBounty(bounty);

        assertSame(repositoryFuture, result, "Service should return the repository future");
        verify(bountyRepository).updateAsync(bounty);
    }

    @Test
    void getTotalBountyCount_usesRepositoryResultSize() {
        List<RBounty> storedBounties = List.of(mock(RBounty.class), mock(RBounty.class));
        when(bountyRepository.findAllAsync(0, 1000)).thenReturn(CompletableFuture.completedFuture(storedBounties));

        CompletableFuture<Integer> result = service.getTotalBountyCount();

        assertEquals(2, result.join(), "Service should translate repository results into the count");
        verify(bountyRepository).findAllAsync(0, 1000);
    }

    @Test
    void helperMethodsReflectPremiumDefaults() {
        assertTrue(service.isPremium(), "Premium service should report premium status");
        assertEquals(-1, service.getMaxBountiesPerPlayer(), "Premium service should allow unlimited bounties");
        assertEquals(-1, service.getMaxRewardItems(), "Premium service should allow unlimited reward items");

        Player player = mock(Player.class);
        assertTrue(service.canCreateBounty(player), "Premium service should allow bounty creation by default");
    }
}
