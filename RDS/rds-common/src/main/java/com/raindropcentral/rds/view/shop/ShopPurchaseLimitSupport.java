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
import com.raindropcentral.rds.database.entity.ShopLedgerType;
import com.raindropcentral.rds.items.ShopItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Provides purchase-limit evaluation support for shop customer purchases.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ShopPurchaseLimitSupport {

    private ShopPurchaseLimitSupport() {
    }

    static @NotNull PurchaseLimitCheck checkLimit(
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull UUID playerId,
            final int requestedAmount
    ) {
        return checkLimit(shop, item, playerId, requestedAmount, LocalDateTime.now());
    }

    static @NotNull PurchaseLimitCheck checkLimit(
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull UUID playerId,
            final int requestedAmount,
            final @NotNull LocalDateTime now
    ) {
        return checkLimit(
                shop,
                item.getEntryId(),
                item.getItem().getType().name(),
                item.hasPurchaseLimit() ? item.getPurchaseLimitAmount() : null,
                item.hasPurchaseLimit() ? item.getPurchaseLimitWindowMinutes() : null,
                playerId,
                requestedAmount,
                now
        );
    }

    static @NotNull PurchaseLimitCheck checkLimit(
            final @NotNull Shop shop,
            final @NotNull UUID itemEntryId,
            final @Nullable String fallbackItemType,
            final @Nullable Integer limitAmount,
            final @Nullable Integer windowMinutes,
            final @NotNull UUID playerId,
            final int requestedAmount,
            final @NotNull LocalDateTime now
    ) {
        if (limitAmount == null || limitAmount < 1 || windowMinutes == null || windowMinutes < 1) {
            return PurchaseLimitCheck.unlimited();
        }

        final int purchasedInWindow = getPurchasedAmountInWindow(
                shop,
                itemEntryId,
                fallbackItemType,
                windowMinutes,
                playerId,
                now
        );
        final int remainingAmount = Math.max(0, limitAmount - purchasedInWindow);
        final boolean allowed = requestedAmount > 0 && requestedAmount <= remainingAmount;
        return new PurchaseLimitCheck(
                true,
                allowed,
                limitAmount,
                windowMinutes,
                purchasedInWindow,
                remainingAmount
        );
    }

    /**
     * Resolves the remaining purchasable amount for a player and shop item at the current time.
     *
     * @param shop target shop
     * @param item target item
     * @param playerId target player id
     * @return remaining amount allowed in the active window, or {@link Integer#MAX_VALUE} when unlimited
     */
    public static int getRemainingQuota(
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull UUID playerId
    ) {
        return getRemainingQuota(shop, item, playerId, LocalDateTime.now());
    }

    /**
     * Resolves the remaining purchasable amount for a player and shop item at a provided timestamp.
     *
     * @param shop target shop
     * @param item target item
     * @param playerId target player id
     * @param now current timestamp reference
     * @return remaining amount allowed in the active window, or {@link Integer#MAX_VALUE} when unlimited
     */
    public static int getRemainingQuota(
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull UUID playerId,
            final @NotNull LocalDateTime now
    ) {
        final PurchaseLimitCheck check = checkLimit(
                shop,
                item.getEntryId(),
                item.getItem().getType().name(),
                item.hasPurchaseLimit() ? item.getPurchaseLimitAmount() : null,
                item.hasPurchaseLimit() ? item.getPurchaseLimitWindowMinutes() : null,
                playerId,
                1,
                now
        );
        return check.configured() ? check.remainingAmount() : Integer.MAX_VALUE;
    }

    private static int getPurchasedAmountInWindow(
            final @NotNull Shop shop,
            final @NotNull UUID itemEntryId,
            final @Nullable String fallbackItemType,
            final int windowMinutes,
            final @NotNull UUID playerId,
            final @NotNull LocalDateTime now
    ) {
        final LocalDateTime cutoff = now.minusMinutes(windowMinutes);
        int purchasedAmount = 0;
        for (final ShopLedgerEntry entry : shop.getLedgerEntries()) {
            if (entry.getEntryType() != ShopLedgerType.PURCHASE) {
                continue;
            }
            if (!playerId.equals(entry.getActorId())) {
                continue;
            }
            if (!matchesItem(entry, itemEntryId, fallbackItemType)) {
                continue;
            }

            final LocalDateTime createdAt = entry.getCreatedAt();
            if (createdAt != null && createdAt.isBefore(cutoff)) {
                continue;
            }

            purchasedAmount += normalizeItemAmount(entry.getItemAmount());
        }

        return Math.max(0, purchasedAmount);
    }

    private static boolean matchesItem(
            final @NotNull ShopLedgerEntry entry,
            final @NotNull UUID itemEntryId,
            final @Nullable String fallbackItemType
    ) {
        final UUID ledgerEntryId = entry.getItemEntryId();
        if (ledgerEntryId != null) {
            return ledgerEntryId.equals(itemEntryId);
        }

        final String ledgerItemType = entry.getItemType();
        return fallbackItemType != null
                && ledgerItemType != null
                && ledgerItemType.equalsIgnoreCase(fallbackItemType);
    }

    private static int normalizeItemAmount(
            final Integer itemAmount
    ) {
        return itemAmount == null ? 1 : Math.max(1, itemAmount);
    }

    record PurchaseLimitCheck(
            boolean configured,
            boolean allowed,
            int limitAmount,
            int windowMinutes,
            int purchasedAmount,
            int remainingAmount
    ) {
        private static @NotNull PurchaseLimitCheck unlimited() {
            return new PurchaseLimitCheck(false, true, -1, -1, 0, Integer.MAX_VALUE);
        }
    }
}
