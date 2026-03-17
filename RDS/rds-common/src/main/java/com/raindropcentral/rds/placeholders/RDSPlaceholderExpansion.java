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

package com.raindropcentral.rds.placeholders;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.service.shop.ShopOwnershipSupport;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.placeholder.AbstractPlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RDS internal PlaceholderAPI expansion.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public final class RDSPlaceholderExpansion extends AbstractPlaceholderExpansion {

    private static final Logger LOGGER = Logger.getLogger(RDSPlaceholderExpansion.class.getName());
    private static final String NONE = "None";

    private final RDS rds;

    /**
     * Creates a new RDS placeholder expansion.
     *
     * @param rds active RDS runtime
     * @throws NullPointerException if {@code rds} is {@code null}
     */
    public RDSPlaceholderExpansion(final @NotNull RDS rds) {
        super(rds.getPlugin());
        this.rds = rds;
    }

    /**
     * Returns all placeholder keys handled by this expansion.
     *
     * @return supported placeholder keys
     */
    @Override
    protected @NotNull List<String> definePlaceholders() {
        return List.of(
            "shops_owned",
            "shops_admin",
            "shops_items",
            "shops_tax"
        );
    }

    /**
     * Resolves one RDS placeholder value.
     *
     * @param player online player context
     * @param params placeholder key suffix
     * @return resolved value or empty string for unknown keys
     */
    @Override
    protected @NonNull String resolvePlaceholder(
        final @Nullable Player player,
        final @NotNull String params
    ) {
        final String normalized = params.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "shops_owned" -> this.resolveOwnedShops(player);
            case "shops_admin" -> this.resolveAdminShops();
            case "shops_items" -> this.resolveSellingItems(player);
            case "shops_tax" -> this.resolveTaxDebt(player);
            default -> "";
        };
    }

    private @NotNull String resolveOwnedShops(final @Nullable Player player) {
        if (player == null || this.rds.getShopRepository() == null) {
            return "0";
        }

        try {
            return Integer.toString(ShopOwnershipSupport.countOwnedPlayerShops(this.rds, player.getUniqueId()));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve rds_shops_owned", exception);
            return "0";
        }
    }

    private @NotNull String resolveAdminShops() {
        if (this.rds.getShopRepository() == null) {
            return "0";
        }

        try {
            return Integer.toString(ShopOwnershipSupport.countAdminShops(this.rds));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve rds_shops_admin", exception);
            return "0";
        }
    }

    private @NotNull String resolveSellingItems(final @Nullable Player player) {
        if (player == null) {
            return "0";
        }

        int totalItems = 0;
        for (final Shop shop : this.getOwnedPlayerShops(player.getUniqueId())) {
            totalItems += Math.max(0, shop.getStoredItemCount());
        }

        return Integer.toString(totalItems);
    }

    private @NotNull String resolveTaxDebt(final @Nullable Player player) {
        if (player == null) {
            return NONE;
        }

        final Map<String, Double> aggregateDebt = new LinkedHashMap<>();
        for (final Shop shop : this.getOwnedPlayerShops(player.getUniqueId())) {
            for (final Map.Entry<String, Double> debtEntry : shop.getTaxDebtEntries().entrySet()) {
                if (debtEntry.getKey() == null || debtEntry.getKey().isBlank() || debtEntry.getValue() == null) {
                    continue;
                }

                final double amount = Math.max(0D, debtEntry.getValue());
                if (amount <= 1.0E-6D) {
                    continue;
                }

                final String currencyType = debtEntry.getKey().trim().toLowerCase(Locale.ROOT);
                aggregateDebt.merge(currencyType, amount, Double::sum);
            }
        }

        if (aggregateDebt.isEmpty()) {
            return NONE;
        }

        if (this.rds.getShopTaxScheduler() != null) {
            try {
                return this.rds.getShopTaxScheduler().formatCurrencySummary(aggregateDebt);
            } catch (Exception exception) {
                LOGGER.log(Level.FINE, "Failed to format debt summary via scheduler; using fallback format", exception);
            }
        }

        final List<String> parts = new ArrayList<>();
        for (final Map.Entry<String, Double> debtEntry : aggregateDebt.entrySet()) {
            parts.add(this.formatCurrency(debtEntry.getKey(), debtEntry.getValue()));
        }
        return parts.isEmpty() ? NONE : String.join(", ", parts);
    }

    private @NotNull List<Shop> getOwnedPlayerShops(final @NotNull UUID ownerId) {
        if (this.rds.getShopRepository() == null) {
            return List.of();
        }

        try {
            final List<Shop> ownedShops = new ArrayList<>();
            for (final Shop shop : this.rds.getShopRepository().findAllShops()) {
                if (shop == null || shop.isAdminShop() || !shop.isOwner(ownerId)) {
                    continue;
                }
                ownedShops.add(shop);
            }
            return ownedShops;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve owned shops for placeholders", exception);
            return List.of();
        }
    }

    private @NotNull String formatCurrency(
        final @NotNull String currencyType,
        final double amount
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
            return this.rds.formatVaultCurrency(amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        final String currencyDisplayName = bridge == null
            ? currencyType
            : bridge.getCurrencyDisplayName(currencyType);
        return String.format(Locale.US, "%.2f %s", amount, currencyDisplayName);
    }
}
