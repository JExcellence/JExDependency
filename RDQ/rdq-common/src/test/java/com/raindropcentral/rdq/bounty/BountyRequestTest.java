package com.raindropcentral.rdq.bounty;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BountyRequestTest {

    @Test
    void createValidRequest() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(1000);

        var request = new BountyRequest(placerId, targetId, amount, "coins");

        assertEquals(placerId, request.placerId());
        assertEquals(targetId, request.targetId());
        assertEquals(amount, request.amount());
        assertEquals("coins", request.currency());
    }

    @Test
    void rejectSelfTargeting() {
        var playerId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            new BountyRequest(playerId, playerId, BigDecimal.valueOf(100), "coins")
        );
    }

    @Test
    void rejectZeroAmount() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            new BountyRequest(placerId, targetId, BigDecimal.ZERO, "coins")
        );
    }

    @Test
    void rejectNegativeAmount() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            new BountyRequest(placerId, targetId, BigDecimal.valueOf(-100), "coins")
        );
    }

    @Test
    void rejectNullPlacerId() {
        var targetId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () ->
            new BountyRequest(null, targetId, BigDecimal.valueOf(100), "coins")
        );
    }

    @Test
    void rejectNullTargetId() {
        var placerId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () ->
            new BountyRequest(placerId, null, BigDecimal.valueOf(100), "coins")
        );
    }

    @Test
    void factoryMethodCreatesValidRequest() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        var request = BountyRequest.of(placerId, targetId, 500.0, "gold");

        assertEquals(placerId, request.placerId());
        assertEquals(targetId, request.targetId());
        assertEquals(BigDecimal.valueOf(500.0), request.amount());
        assertEquals("gold", request.currency());
    }
}
