package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class FreeBountyServiceTest {

    private static final UUID COMMISSIONER_ID = UUID.fromString("2a6d1e17-9f2c-4d5d-9c09-8b079ebacb55");
    private static final UUID TARGET_ID = UUID.fromString("0fd031b7-93d6-48b3-9b3c-9b17d8c33df6");

    private FreeBountyService service;
    private RDQPlayer target;
    private Player commissioner;

    @BeforeEach
    void setUp() {
        this.service = new FreeBountyService();
        this.target = mock(RDQPlayer.class);
        when(target.getUniqueId()).thenReturn(TARGET_ID);

        this.commissioner = mock(Player.class);
        when(commissioner.getUniqueId()).thenReturn(COMMISSIONER_ID);
    }

    @Test
    void createBounty_assignsIdPersistsAndReturnsBounty() {
        RBounty bounty = service.createBounty(
                target,
                commissioner,
                createRewardItems(1),
                Map.of("coins", 50.0)
        ).join();

        assertNotNull(bounty, "Expected a bounty to be returned");
        assertEquals(COMMISSIONER_ID, bounty.getCommissioner(), "Commissioner UUID should match the mocked player");
        assertEquals(TARGET_ID, bounty.getPlayer().getUniqueId(), "Target UUID should match the mocked RDQ player");
        assertEquals(1L, extractIdentifier(bounty), "Reflection ID assignment should start at 1");

        List<RBounty> stored = service.getAllBounties(1, 10).join();
        assertEquals(1, stored.size(), "Service should persist a single bounty entry");
        assertSame(bounty, stored.getFirst(), "Persisted bounty should be the same instance returned");
        assertEquals(1, service.getTotalBountyCount().join(), "Total bounty count should report the persisted bounty");
    }

    @Test
    void createBounty_failsWhenCommissionerExceededLimit() {
        service.createBounty(target, commissioner, createRewardItems(1), Map.of()).join();

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> service.createBounty(target, commissioner, createRewardItems(1), Map.of()).join(),
                "Expected exceeding the free bounty limit to fail"
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause(), "Failure should propagate as IllegalStateException");
    }

    @Test
    void createBounty_failsWhenRewardLimitExceeded() {
        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> service.createBounty(target, commissioner, createRewardItems(6), Map.of()).join(),
                "Expected reward limit violation to fail"
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause(), "Failure should propagate as IllegalStateException");
    }

    @Test
    void getAllBounties_respectsPaginationBoundaries() {
        FreeBountyService localService = new FreeBountyService();
        createBounty(localService, TARGET_ID, COMMISSIONER_ID);
        createBounty(localService, UUID.fromString("fe4e1a33-02a8-41f9-8f0e-828ff31b3f0b"), UUID.fromString("7c292b7d-5c84-4d3c-b570-42512c2127f4"));
        createBounty(localService, UUID.fromString("4e4f7cc7-87ce-4d2d-8f55-6b137c6da584"), UUID.fromString("8ad1c73f-8b94-41ea-8b72-96efb7ae1e17"));

        assertEquals(2, localService.getAllBounties(1, 2).join().size(), "First page should contain two bounties");
        assertEquals(1, localService.getAllBounties(2, 2).join().size(), "Second page should contain the remaining bounty");
        assertTrue(localService.getAllBounties(3, 2).join().isEmpty(), "Out-of-range page should be empty");
    }

    @Test
    void getBountyByPlayer_returnsMatchingBounty() {
        RBounty bounty = service.createBounty(target, commissioner, createRewardItems(1), Map.of()).join();

        Optional<RBounty> found = service.getBountyByPlayer(TARGET_ID).join();
        assertTrue(found.isPresent(), "Expected to find a bounty for the mocked player");
        assertSame(bounty, found.orElseThrow(), "Returned bounty should be identical to the stored instance");

        assertTrue(service.getBountyByPlayer(UUID.randomUUID()).join().isEmpty(), "Unknown player should not have a bounty");
    }

    @Test
    void deleteBounty_returnsRemovalOutcome() {
        RBounty bounty = service.createBounty(target, commissioner, createRewardItems(1), Map.of()).join();

        assertTrue(service.deleteBounty(bounty.getId()).join(), "Existing bounty should be deleted successfully");
        assertFalse(service.deleteBounty(bounty.getId()).join(), "Deleting the same bounty again should report false");
        assertEquals(0, service.getTotalBountyCount().join(), "All bounties should have been removed");
    }

    @Test
    void updateBounty_updatesPersistedState() {
        RBounty bounty = service.createBounty(target, commissioner, createRewardItems(1), Map.of("coins", 10.0)).join();
        Map<String, Double> updatedCurrencies = new LinkedHashMap<>();
        updatedCurrencies.put("emeralds", 5.0);
        bounty.setRewardCurrencies(updatedCurrencies);

        RBounty updated = service.updateBounty(bounty).join();
        assertEquals(updatedCurrencies, updated.getRewardCurrencies(), "Updated currencies should persist in the bounty");
    }

    @Test
    void updateBounty_failsForUnknownIdentifier() {
        RBounty orphan = new RBounty(target, commissioner, createRewardItems(1), Map.of());
        setIdentifier(orphan, 999L);

        CompletionException exception = assertThrows(
                CompletionException.class,
                () -> service.updateBounty(orphan).join(),
                "Updating a non-existent bounty should fail"
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause(), "Failure should propagate as IllegalArgumentException");
    }

    @Test
    void serviceAccessorsReflectFreeTierConstraints() {
        assertFalse(service.isPremium(), "Free service should report non-premium");
        assertEquals(1, service.getMaxBountiesPerPlayer(), "Free service should allow a single bounty per commissioner");
        assertEquals(5, service.getMaxRewardItems(), "Free service should limit reward items to five");
        assertEquals(0, service.getTotalBountyCount().join(), "Initial bounty count should be zero");
    }

    private RBounty createBounty(FreeBountyService localService, UUID targetId, UUID commissionerId) {
        RDQPlayer localTarget = mock(RDQPlayer.class);
        when(localTarget.getUniqueId()).thenReturn(targetId);

        Player localCommissioner = mock(Player.class);
        when(localCommissioner.getUniqueId()).thenReturn(commissionerId);

        return localService.createBounty(localTarget, localCommissioner, createRewardItems(1), Map.of()).join();
    }

    private Set<RewardItem> createRewardItems(int count) {
        Set<RewardItem> rewardItems = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            rewardItems.add(mock(RewardItem.class));
        }
        return rewardItems;
    }

    private long extractIdentifier(RBounty bounty) {
        try {
            Field idField = bounty.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            Object value = idField.get(bounty);
            assertNotNull(value, "Identifier should have been assigned through reflection");
            return (Long) value;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to extract bounty identifier", e);
            throw new AssertionError("Unreachable", e);
        }
    }

    private void setIdentifier(RBounty bounty, long id) {
        try {
            Field idField = bounty.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(bounty, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set bounty identifier", e);
        }
    }
}
