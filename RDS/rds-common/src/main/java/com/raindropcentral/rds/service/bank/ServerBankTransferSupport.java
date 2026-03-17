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

package com.raindropcentral.rds.service.bank;

import com.raindropcentral.rds.database.entity.Bank;
import com.raindropcentral.rds.database.entity.Shop;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Support helpers for transferring admin-shop balances into the server bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class ServerBankTransferSupport {

    private ServerBankTransferSupport() {
    }

    static @NotNull Map<String, Double> collectTransferableBalances(
            final @NotNull Shop shop
    ) {
        if (!shop.isAdminShop()) {
            return Map.of();
        }

        final Map<String, Double> transferable = new LinkedHashMap<>();
        for (final Bank bankEntry : shop.getBankEntries()) {
            final double amount = bankEntry.getAmount();
            if (amount <= 0D) {
                continue;
            }

            transferable.merge(bankEntry.getCurrencyType(), amount, Double::sum);
        }
        return transferable;
    }
}
