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

package com.raindropcentral.rdr.placeholders;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.placeholder.AbstractPlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RDR internal PlaceholderAPI expansion.
 *
 * @author RaindropCentral
 * @version 5.0.0
 */
public final class RDRPlaceholderExpansion extends AbstractPlaceholderExpansion {

    private static final Logger LOGGER = Logger.getLogger(RDRPlaceholderExpansion.class.getName());
    private static final String NONE = "None";

    private final RDR rdr;

    /**
     * Creates a new RDR placeholder expansion.
     *
     * @param rdr active RDR runtime
     * @throws NullPointerException if {@code rdr} is {@code null}
     */
    public RDRPlaceholderExpansion(final @NotNull RDR rdr) {
        super(rdr.getPlugin());
        this.rdr = rdr;
    }

    /**
     * Returns all placeholder keys handled by this expansion.
     *
     * @return supported placeholder keys
     */
    @Override
    protected @NotNull List<String> definePlaceholders() {
        return List.of(
            "storages_max",
            "storages_players",
            "storages_items",
            "storages_tax"
        );
    }

    /**
     * Resolves one RDR placeholder value.
     *
     * @param player online player context
     * @param params placeholder key suffix
     * @return resolved value or empty string for unknown keys
     */
    @Override
    protected @NotNull String resolvePlaceholder(
        final @Nullable Player player,
        final @NotNull String params
    ) {
        final String normalized = params.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "storages_max" -> this.resolveMaximumStorages(player);
            case "storages_players" -> this.resolvePlayerStorages(player);
            case "storages_items" -> this.resolveStoredItems(player);
            case "storages_tax" -> this.resolveStorageTax(player);
            default -> "";
        };
    }

    private @NotNull String resolveMaximumStorages(final @Nullable Player player) {
        try {
            final int maximumStorages = player == null
                ? this.rdr.getMaximumStorages()
                : this.rdr.getMaximumStorages(player);
            return Integer.toString(maximumStorages);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve rdr_storages_max", exception);
            return "0";
        }
    }

    private @NotNull String resolvePlayerStorages(final @Nullable Player player) {
        if (player == null) {
            return "0";
        }

        return Integer.toString(this.getPlayerStorages(player.getUniqueId()).size());
    }

    private @NotNull String resolveStoredItems(final @Nullable Player player) {
        if (player == null) {
            return "0";
        }

        int storedItems = 0;
        for (final RStorage storage : this.getPlayerStorages(player.getUniqueId())) {
            storedItems += Math.max(0, storage.getStoredSlotCount());
        }
        return Integer.toString(storedItems);
    }

    private @NotNull String resolveStorageTax(final @Nullable Player player) {
        if (player == null) {
            return NONE;
        }

        final Map<String, Double> aggregatedDebt = new LinkedHashMap<>();
        for (final RStorage storage : this.getPlayerStorages(player.getUniqueId())) {
            for (final Map.Entry<String, Double> debtEntry : storage.getTaxDebtEntries().entrySet()) {
                final String currencyType = this.normalizeCurrencyType(debtEntry.getKey());
                if (currencyType == null || debtEntry.getValue() == null) {
                    continue;
                }

                final double amount = Math.max(0D, debtEntry.getValue());
                if (amount <= 1.0E-6D) {
                    continue;
                }

                aggregatedDebt.merge(currencyType, amount, Double::sum);
            }
        }

        if (aggregatedDebt.isEmpty()) {
            return NONE;
        }

        final List<String> parts = new ArrayList<>(aggregatedDebt.size());
        for (final Map.Entry<String, Double> debtEntry : aggregatedDebt.entrySet()) {
            parts.add(this.formatCurrency(debtEntry.getKey(), debtEntry.getValue()));
        }
        return String.join(", ", parts);
    }

    private @NotNull List<RStorage> getPlayerStorages(final @NotNull UUID playerId) {
        if (this.rdr.getPlayerRepository() == null) {
            return List.of();
        }

        try {
            final RDRPlayer playerData = this.rdr.getPlayerRepository().findByPlayer(playerId);
            return playerData == null ? List.of() : playerData.getStorages();
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve player storages for placeholders", exception);
            return List.of();
        }
    }

    private @Nullable String normalizeCurrencyType(final @Nullable String rawCurrencyType) {
        if (rawCurrencyType == null) {
            return null;
        }

        final String normalized = rawCurrencyType.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private @NotNull String formatCurrency(
        final @NotNull String currencyType,
        final double amount
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
            return this.rdr.formatVaultCurrency(amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        final String currencyDisplayName = bridge == null
            ? currencyType
            : bridge.getCurrencyDisplayName(currencyType);
        return String.format(Locale.US, "%.2f %s", amount, currencyDisplayName);
    }
}
