package com.raindropcentral.rdr.database.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link RTradeDelivery} payload normalization and claim lifecycle behavior.
 */
class RTradeDeliveryTest {

    @Test
    void normalizesPayloadsAndReturnsDefensiveCopies() {
        final Map<Integer, ItemStack> itemPayload = new LinkedHashMap<>();
        itemPayload.put(-1, null);
        itemPayload.put(1, null);

        final Map<String, Double> currencyPayload = new LinkedHashMap<>();
        currencyPayload.put(" Vault ", 25.0D);
        currencyPayload.put("raindrops", -5.0D);
        currencyPayload.put(" ", 8.0D);
        currencyPayload.put(null, 10.0D);

        final RTradeDelivery delivery = new RTradeDelivery(
            UUID.randomUUID(),
            UUID.randomUUID(),
            itemPayload,
            currencyPayload
        );

        final Map<Integer, ItemStack> firstItemRead = delivery.getItemPayload();
        final Map<Integer, ItemStack> secondItemRead = delivery.getItemPayload();
        final Map<String, Double> currencies = delivery.getCurrencyPayload();

        assertTrue(firstItemRead.isEmpty());
        assertTrue(secondItemRead.isEmpty());

        assertEquals(Map.of("vault", 25.0D), currencies);
        assertEquals(TradeDeliveryStatus.PENDING, delivery.getStatus());
        assertNotNull(delivery.getCreatedAt());
        assertNull(delivery.getClaimedAt());
        assertFalse(delivery.isEmpty());
    }

    @Test
    void updatesStatusAndTimestampWhenClaimed() {
        final RTradeDelivery delivery = new RTradeDelivery(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Map.of(),
            Map.of("vault", 10.0D)
        );
        final LocalDateTime claimedAt = LocalDateTime.now().plusSeconds(1);

        delivery.markClaimed(claimedAt);

        assertEquals(TradeDeliveryStatus.CLAIMED, delivery.getStatus());
        assertEquals(claimedAt, delivery.getClaimedAt());
        assertThrows(NullPointerException.class, () -> delivery.markClaimed(null));
    }

    @Test
    void reportsEmptyOnlyWhenBothPayloadsAreEmpty() {
        final RTradeDelivery delivery = new RTradeDelivery(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Map.of(),
            Map.of()
        );

        assertTrue(delivery.isEmpty());

        delivery.setCurrencyPayload(Map.of("vault", 12.0D));
        assertFalse(delivery.isEmpty());

        delivery.setCurrencyPayload(Map.of());
        final Map<Integer, ItemStack> emptyItemPayload = new LinkedHashMap<>();
        emptyItemPayload.put(0, null);
        delivery.setItemPayload(emptyItemPayload);
        assertTrue(delivery.isEmpty());
    }
}
