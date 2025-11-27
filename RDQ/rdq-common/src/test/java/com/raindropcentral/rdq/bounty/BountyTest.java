package com.raindropcentral.rdq.bounty;

import com.raindropcentral.rdq.database.entity.bounty.BountyEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BountyTest {

    @Test
    void createActiveBounty() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(1000);

        var bounty = Bounty.create(placerId, targetId, amount, "coins", Instant.now().plus(Duration.ofDays(7)));

        assertNull(bounty.id());
        assertEquals(placerId, bounty.placerId());
        assertEquals(targetId, bounty.targetId());
        assertEquals(amount, bounty.amount());
        assertEquals("coins", bounty.currency());
        assertEquals(BountyStatus.ACTIVE, bounty.status());
        assertTrue(bounty.isActive());
        assertFalse(bounty.isExpired());
        assertNull(bounty.claimedBy());
        assertNull(bounty.claimedAt());
    }

    @Test
    void claimBounty() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var hunterId = UUID.randomUUID();

        // Use BountyEntity for mutation tests
        var entity = BountyEntity.create(placerId, targetId, BigDecimal.valueOf(500), "coins", null);
        entity.claim(hunterId);

        assertEquals(com.raindropcentral.rdq.database.entity.bounty.BountyStatus.CLAIMED, entity.status());
        assertEquals(hunterId, entity.claimedBy());
        assertNotNull(entity.claimedAt());
        assertFalse(entity.isActive());
    }

    @Test
    void expireBounty() {
        var entity = BountyEntity.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100), "coins", null);
        entity.expire();

        assertEquals(com.raindropcentral.rdq.database.entity.bounty.BountyStatus.EXPIRED, entity.status());
        assertFalse(entity.isActive());
    }

    @Test
    void cancelBounty() {
        var entity = BountyEntity.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100), "coins", null);
        entity.cancel();

        assertEquals(com.raindropcentral.rdq.database.entity.bounty.BountyStatus.CANCELLED, entity.status());
        assertFalse(entity.isActive());
    }

    @Test
    void isExpiredWhenPastExpiryDate() {
        var bounty = new Bounty(
            1L,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            "coins",
            BountyStatus.ACTIVE,
            Instant.now().minus(Duration.ofDays(8)),
            Instant.now().minus(Duration.ofDays(1)),
            null,
            null
        );

        assertTrue(bounty.isExpired());
        assertFalse(bounty.isActive());
    }

    @Test
    void isNotExpiredWhenNoExpiryDate() {
        var bounty = Bounty.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100), "coins", null);

        assertFalse(bounty.isExpired());
        assertTrue(bounty.isActive());
    }
}
