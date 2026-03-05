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
