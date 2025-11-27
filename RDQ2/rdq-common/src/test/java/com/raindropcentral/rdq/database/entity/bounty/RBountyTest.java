package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RBounty entity.
 * Tests state transitions, validation, and defensive copies.
 */
@DisplayName("RBounty Entity Tests")
class RBountyTest {

    private UUID targetUuid;
    private UUID commissionerUuid;
    private UUID hunterUuid;
    private RBounty bounty;

    @BeforeEach
    void setUp() {
        targetUuid = UUID.randomUUID();
        commissionerUuid = UUID.randomUUID();
        hunterUuid = UUID.randomUUID();
        bounty = new RBounty(targetUuid, commissionerUuid);
    }

    @Test
    @DisplayName("Should create bounty with required fields")
    void testBountyCreation() {
        assertNotNull(bounty);
        assertEquals(targetUuid, bounty.getTargetUniqueId());
        assertEquals(commissionerUuid, bounty.getCommissionerUniqueId());
        assertTrue(bounty.isActive());
        assertFalse(bounty.isClaimed());
        assertTrue(bounty.getExpiresAt().isPresent());
    }

    @Test
    @DisplayName("Should transition from active to claimed state")
    void testActiveToClaimedTransition() {
        assertTrue(bounty.isActive());
        assertFalse(bounty.isClaimed());

        bounty.claim(hunterUuid);

        assertFalse(bounty.isActive());
        assertTrue(bounty.isClaimed());
        assertEquals(hunterUuid, bounty.getClaimedBy().orElseThrow());
        assertTrue(bounty.getClaimedAt().isPresent());
    }

    @Test
    @DisplayName("Should transition from active to expired state")
    void testActiveToExpiredTransition() {
        assertTrue(bounty.isActive());
        assertFalse(bounty.isExpired());

        bounty.expire();

        assertFalse(bounty.isActive());
        assertFalse(bounty.isClaimed());
    }

    @Test
    @DisplayName("Should throw exception when claiming already claimed bounty")
    void testClaimAlreadyClaimedBounty() {
        bounty.claim(hunterUuid);

        UUID anotherHunter = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> bounty.claim(anotherHunter));
    }

    @Test
    @DisplayName("Should throw exception when claiming expired bounty")
    void testClaimExpiredBounty() {
        bounty.setExpiresAt(Optional.of(LocalDateTime.now().minusDays(1)));
        assertTrue(bounty.isExpired());

        assertThrows(IllegalStateException.class, () -> bounty.claim(hunterUuid));
    }

    @Test
    @DisplayName("Should throw exception when expiring claimed bounty")
    void testExpireClaimedBounty() {
        bounty.claim(hunterUuid);

        assertThrows(IllegalStateException.class, () -> bounty.expire());
    }

    @Test
    @DisplayName("Should throw exception when claiming with null UUID")
    void testClaimWithNullUuid() {
        assertThrows(NullPointerException.class, () -> bounty.claim(null));
    }

    @Test
    @DisplayName("Should return unmodifiable collections")
    void testDefensiveCopies() {
        Set<RewardItem> rewardItems = bounty.getRewardItems();
        Map<String, Double> rewardCurrencies = bounty.getRewardCurrencies();
        List<BountyReward> rewards = bounty.getRewards();

        assertThrows(UnsupportedOperationException.class, () -> rewardItems.clear());
        assertThrows(UnsupportedOperationException.class, () -> rewardCurrencies.clear());
        assertThrows(UnsupportedOperationException.class, () -> rewards.clear());
    }

    @Test
    @DisplayName("Should validate total estimated value is non-negative")
    void testTotalEstimatedValueValidation() {
        bounty.setTotalEstimatedValue(-100.0);
        assertEquals(0.0, bounty.getTotalEstimatedValue());

        bounty.setTotalEstimatedValue(500.0);
        assertEquals(500.0, bounty.getTotalEstimatedValue());
    }

    @Test
    @DisplayName("Should add reward currency with validation")
    void testAddRewardCurrency() {
        bounty.addRewardCurrency("gold", 100.0);
        assertEquals(100.0, bounty.getRewardCurrencies().get("gold"));

        bounty.addRewardCurrency("gold", 50.0);
        assertEquals(150.0, bounty.getRewardCurrencies().get("gold"));

        // Negative amounts should be ignored
        bounty.addRewardCurrency("silver", -10.0);
        assertFalse(bounty.getRewardCurrencies().containsKey("silver"));
    }

    @Test
    @DisplayName("Should throw exception when adding null currency name")
    void testAddNullCurrencyName() {
        assertThrows(NullPointerException.class, () -> bounty.addRewardCurrency(null, 100.0));
    }

    @Test
    @DisplayName("Should detect expired bounty correctly")
    void testIsExpired() {
        bounty.setExpiresAt(Optional.of(LocalDateTime.now().plusDays(1)));
        assertFalse(bounty.isExpired());

        bounty.setExpiresAt(Optional.of(LocalDateTime.now().minusDays(1)));
        assertTrue(bounty.isExpired());

        bounty.setExpiresAt(Optional.empty());
        assertFalse(bounty.isExpired());
    }

    @Test
    @DisplayName("Should use Optional for nullable fields")
    void testOptionalFields() {
        assertTrue(bounty.getExpiresAt().isPresent());
        assertTrue(bounty.getClaimedBy().isEmpty());
        assertTrue(bounty.getClaimedAt().isEmpty());

        bounty.claim(hunterUuid);

        assertTrue(bounty.getClaimedBy().isPresent());
        assertTrue(bounty.getClaimedAt().isPresent());
    }
}
