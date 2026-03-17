/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests purchase-limit evaluation support used by customer purchases.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopPurchaseLimitSupportTest {

    @Test
    void reportsUnlimitedQuotaWhenNoLimitIsConfigured() {
        final Shop shop = new Shop(UUID.randomUUID(), new Location(null, 10, 64, 10));
        final UUID itemEntryId = UUID.randomUUID();
        final UUID playerId = UUID.randomUUID();

        final ShopPurchaseLimitSupport.PurchaseLimitCheck check = ShopPurchaseLimitSupport.checkLimit(
                shop,
                itemEntryId,
                "LEGACY",
                null,
                null,
                playerId,
                64,
                LocalDateTime.of(2026, 3, 1, 12, 0)
        );

        assertFalse(check.configured());
        assertTrue(check.allowed());
        assertEquals(Integer.MAX_VALUE, check.remainingAmount());
    }

    @Test
    void countsOnlyRecentMatchingPurchasesForSamePlayerAndItem() {
        final Shop shop = new Shop(UUID.randomUUID(), new Location(null, 5, 64, 5));
        final UUID itemEntryId = UUID.randomUUID();
        final UUID otherItemEntryId = UUID.randomUUID();
        final UUID playerId = UUID.randomUUID();
        final UUID otherPlayerId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.of(2026, 3, 1, 12, 0);

        shop.addLedgerEntry(this.createPurchaseEntry(
                shop,
                playerId,
                itemEntryId,
                4,
                now.minusMinutes(5)
        ));
        shop.addLedgerEntry(this.createPurchaseEntry(
                shop,
                playerId,
                itemEntryId,
                1,
                now.minusMinutes(10)
        ));
        shop.addLedgerEntry(this.createPurchaseEntry(
                shop,
                playerId,
                itemEntryId,
                3,
                now.minusMinutes(31)
        ));
        shop.addLedgerEntry(this.createPurchaseEntry(
                shop,
                otherPlayerId,
                itemEntryId,
                2,
                now.minusMinutes(3)
        ));
        shop.addLedgerEntry(this.createPurchaseEntry(
                shop,
                playerId,
                otherItemEntryId,
                2,
                now.minusMinutes(2)
        ));

        final ShopPurchaseLimitSupport.PurchaseLimitCheck denied = ShopPurchaseLimitSupport.checkLimit(
                shop,
                itemEntryId,
                "LEGACY",
                10,
                30,
                playerId,
                6,
                now
        );
        assertTrue(denied.configured());
        assertFalse(denied.allowed());
        assertEquals(10, denied.limitAmount());
        assertEquals(30, denied.windowMinutes());
        assertEquals(5, denied.purchasedAmount());
        assertEquals(5, denied.remainingAmount());

        final ShopPurchaseLimitSupport.PurchaseLimitCheck allowed = ShopPurchaseLimitSupport.checkLimit(
                shop,
                itemEntryId,
                "LEGACY",
                10,
                30,
                playerId,
                5,
                now
        );
        assertTrue(allowed.allowed());
        assertEquals(5, allowed.remainingAmount());
    }

    private @NotNull ShopLedgerEntry createPurchaseEntry(
            final Shop shop,
            final UUID playerId,
            final UUID itemEntryId,
            final int amount,
            final LocalDateTime createdAt
    ) {
        final ShopLedgerEntry entry = ShopLedgerEntry.purchase(
                shop,
                playerId,
                "Player",
                "vault",
                amount,
                "LEGACY",
                amount,
                itemEntryId
        );
        this.setCreatedAt(entry, createdAt);
        return entry;
    }

    private void setCreatedAt(
            final ShopLedgerEntry entry,
            final LocalDateTime createdAt
    ) {
        try {
            final Method method = entry.getClass().getMethod("setCreatedAt", LocalDateTime.class);
            method.invoke(entry, createdAt);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fallback to direct field access for older entity implementations.
        }

        Class<?> currentClass = entry.getClass();
        while (currentClass != null) {
            try {
                final Field field = currentClass.getDeclaredField("createdAt");
                field.setAccessible(true);
                field.set(entry, createdAt);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set createdAt for test ledger entry", exception);
            }
        }

        throw new IllegalStateException("Could not locate createdAt field on ShopLedgerEntry hierarchy");
    }
}
