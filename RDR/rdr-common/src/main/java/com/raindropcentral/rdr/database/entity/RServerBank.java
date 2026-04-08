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

package com.raindropcentral.rdr.database.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent server-wide trade-tax bank containing all tracked currency balances.
 *
 * <p>The bank stores one scoped row that holds a currency-balance map plus an append-only
 * transaction ledger so administrators can audit and withdraw accrued tax amounts.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(
    name = "rdr_server_bank",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rdr_server_bank_scope",
            columnNames = {"bank_scope"}
        )
    }
)
@SuppressWarnings({
    "unused",
    "FieldCanBeLocal",
    "JpaDataSourceORMInspection"
})
/**
 * Represents the RServerBank API type.
 */
public class RServerBank extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RServerBank.class);
    private static final TypeReference<Map<String, Double>> BALANCE_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<LedgerEntry>> LEDGER_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_BANK_SCOPE = "global";
    private static final String DEFAULT_CURRENCY = "vault";
    private static final String UNSPECIFIED_NOTE = "unspecified";

    @Column(name = "bank_scope", nullable = false, length = 64)
    private String bankScope;

    @Column(name = "balances_json", nullable = false, columnDefinition = "LONGTEXT")
    private String balancesJson = "{}";

    @Column(name = "ledger_json", nullable = false, columnDefinition = "LONGTEXT")
    private String ledgerJson = "[]";

    /**
     * Supported server-bank ledger transaction types.
     */
    public enum TransactionType {
        /**
         * Currency was deposited into the server bank.
         */
        DEPOSIT,
        /**
         * Currency was withdrawn from the server bank.
         */
        WITHDRAW
    }

    /**
     * Immutable persisted server-bank ledger entry payload.
     *
     * @param recordedAtEpochMilli epoch-millisecond timestamp of the transaction
     * @param currencyType normalized currency identifier
     * @param transactionType ledger transaction type
     * @param amount transaction amount
     * @param resultingBalance resulting balance for {@code currencyType}
     * @param actorUuid optional actor UUID string for auditing
     * @param tradeUuid optional trade UUID string for trade-tax references
     * @param note free-form transaction note
     */
    public record LedgerEntry(
        long recordedAtEpochMilli,
        @NotNull String currencyType,
        @NotNull TransactionType transactionType,
        double amount,
        double resultingBalance,
        @Nullable String actorUuid,
        @Nullable String tradeUuid,
        @NotNull String note
    ) {

        /**
         * Creates a normalized server-bank ledger entry.
         *
         * @param recordedAtEpochMilli epoch-millisecond timestamp of the transaction
         * @param currencyType normalized currency identifier
         * @param transactionType ledger transaction type
         * @param amount transaction amount
         * @param resultingBalance resulting balance for {@code currencyType}
         * @param actorUuid optional actor UUID string for auditing
         * @param tradeUuid optional trade UUID string for trade-tax references
         * @param note free-form transaction note
         */
        public LedgerEntry {
            recordedAtEpochMilli = Math.max(0L, recordedAtEpochMilli);
            currencyType = normalizeCurrencyType(currencyType);
            transactionType = transactionType == null ? TransactionType.DEPOSIT : transactionType;
            amount = sanitizeAmount(amount);
            resultingBalance = sanitizeAmount(resultingBalance);
            actorUuid = normalizeOptionalToken(actorUuid);
            tradeUuid = normalizeOptionalToken(tradeUuid);
            note = normalizeNote(note);
        }

        private static @Nullable String normalizeOptionalToken(final @Nullable String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return null;
            }
            return rawValue.trim().toLowerCase(Locale.ROOT);
        }

        private static @NotNull String normalizeNote(final @Nullable String rawNote) {
            if (rawNote == null || rawNote.isBlank()) {
                return UNSPECIFIED_NOTE;
            }
            return rawNote.trim();
        }
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RServerBank() {
    }

    /**
     * Creates a new server-bank entity for one scope.
     *
     * @param bankScope bank scope identifier (for example {@code global})
     * @throws NullPointerException if {@code bankScope} is {@code null}
     */
    public RServerBank(final @NotNull String bankScope) {
        this.bankScope = normalizeBankScopeToken(bankScope);
        this.balancesJson = "{}";
        this.ledgerJson = "[]";
    }

    /**
     * Returns the normalized bank scope identifier.
     *
     * @return bank scope
     */
    public @NotNull String getBankScope() {
        return this.bankScope;
    }

    /**
     * Returns immutable normalized balances keyed by currency identifier.
     *
     * @return immutable currency balance map
     */
    public @NotNull Map<String, Double> getBalances() {
        return Map.copyOf(parseBalances(this.balancesJson));
    }

    /**
     * Returns current balance for one currency identifier.
     *
     * @param currencyType currency identifier
     * @return non-negative currency balance
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public double getBalance(final @NotNull String currencyType) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        return parseBalances(this.balancesJson).getOrDefault(normalizedCurrencyType, 0.0D);
    }

    /**
     * Deposits amount into one currency balance.
     *
     * @param currencyType currency identifier
     * @param depositAmount amount to add
     * @return updated balance for the currency
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public double deposit(
        final @NotNull String currencyType,
        final double depositAmount
    ) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        final Map<String, Double> balances = parseBalances(this.balancesJson);
        final double currentBalance = balances.getOrDefault(normalizedCurrencyType, 0.0D);
        final double normalizedDeposit = sanitizeAmount(depositAmount);
        if (normalizedDeposit <= 0.0D) {
            return currentBalance;
        }

        final double updatedBalance = currentBalance + normalizedDeposit;
        balances.put(normalizedCurrencyType, updatedBalance);
        this.balancesJson = serializeBalances(balances);
        return updatedBalance;
    }

    /**
     * Withdraws up to the requested amount from one currency balance.
     *
     * @param currencyType currency identifier
     * @param withdrawAmount requested amount to remove
     * @return actual withdrawn amount
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public double withdraw(
        final @NotNull String currencyType,
        final double withdrawAmount
    ) {
        final double normalizedWithdraw = sanitizeAmount(withdrawAmount);
        if (normalizedWithdraw <= 0.0D) {
            return 0.0D;
        }

        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        final Map<String, Double> balances = parseBalances(this.balancesJson);
        final double currentBalance = balances.getOrDefault(normalizedCurrencyType, 0.0D);
        if (currentBalance <= 0.0D) {
            return 0.0D;
        }

        final double actualWithdrawn = Math.min(currentBalance, normalizedWithdraw);
        final double updatedBalance = Math.max(0.0D, currentBalance - actualWithdrawn);
        if (updatedBalance <= 0.0D) {
            balances.remove(normalizedCurrencyType);
        } else {
            balances.put(normalizedCurrencyType, updatedBalance);
        }
        this.balancesJson = serializeBalances(balances);
        return actualWithdrawn;
    }

    /**
     * Returns immutable parsed ledger entries for this bank.
     *
     * @return immutable ledger entries
     */
    public @NotNull List<LedgerEntry> getLedgerEntries() {
        return List.copyOf(parseLedgerEntries(this.ledgerJson));
    }

    /**
     * Returns immutable parsed ledger entries for one currency.
     *
     * @param currencyType currency identifier
     * @return immutable currency-specific ledger entries
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public @NotNull List<LedgerEntry> getLedgerEntriesForCurrency(final @NotNull String currencyType) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        final List<LedgerEntry> filteredEntries = new ArrayList<>();
        for (final LedgerEntry ledgerEntry : parseLedgerEntries(this.ledgerJson)) {
            if (normalizedCurrencyType.equals(ledgerEntry.currencyType())) {
                filteredEntries.add(ledgerEntry);
            }
        }
        return List.copyOf(filteredEntries);
    }

    /**
     * Returns up to the newest ledger entries across all currencies.
     *
     * @param limit maximum amount of entries to return
     * @return immutable newest-first ledger entry list
     */
    public @NotNull List<LedgerEntry> getRecentLedgerEntries(final int limit) {
        return mapToRecentEntries(parseLedgerEntries(this.ledgerJson), limit);
    }

    /**
     * Returns up to the newest ledger entries for one currency.
     *
     * @param currencyType currency identifier
     * @param limit maximum amount of entries to return
     * @return immutable newest-first currency-specific ledger entry list
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public @NotNull List<LedgerEntry> getRecentLedgerEntriesForCurrency(
        final @NotNull String currencyType,
        final int limit
    ) {
        return mapToRecentEntries(this.getLedgerEntriesForCurrency(currencyType), limit);
    }

    /**
     * Returns the number of ledger entries recorded for one currency.
     *
     * @param currencyType currency identifier
     * @return ledger entry count
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public int getLedgerEntryCountForCurrency(final @NotNull String currencyType) {
        return this.getLedgerEntriesForCurrency(currencyType).size();
    }

    /**
     * Appends one ledger transaction entry to the persisted JSON payload.
     *
     * @param ledgerEntry ledger entry to append
     * @throws NullPointerException if {@code ledgerEntry} is {@code null}
     */
    public void appendLedgerEntry(final @NotNull LedgerEntry ledgerEntry) {
        final List<LedgerEntry> entries = parseLedgerEntries(this.ledgerJson);
        entries.add(Objects.requireNonNull(ledgerEntry, "ledgerEntry"));
        this.ledgerJson = serializeLedgerEntries(entries);
    }

    private static @NotNull List<LedgerEntry> mapToRecentEntries(
        final @NotNull List<LedgerEntry> sourceEntries,
        final int limit
    ) {
        final int normalizedLimit = Math.max(0, limit);
        if (normalizedLimit == 0 || sourceEntries.isEmpty()) {
            return List.of();
        }

        final List<LedgerEntry> sortedEntries = new ArrayList<>(sourceEntries);
        sortedEntries.sort(Comparator.comparingLong(LedgerEntry::recordedAtEpochMilli).reversed());
        return List.copyOf(sortedEntries.subList(0, Math.min(normalizedLimit, sortedEntries.size())));
    }

    private static @NotNull Map<String, Double> parseBalances(final @Nullable String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            final Map<String, Double> decoded = OBJECT_MAPPER.readValue(rawJson, BALANCE_TYPE);
            if (decoded == null || decoded.isEmpty()) {
                return new LinkedHashMap<>();
            }

            final Map<String, Double> normalized = new LinkedHashMap<>();
            for (final Map.Entry<String, Double> entry : decoded.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                final double amount = sanitizeAmount(entry.getValue());
                if (amount <= 0.0D) {
                    continue;
                }
                normalized.put(normalizeCurrencyType(entry.getKey()), amount);
            }
            return normalized;
        } catch (final IOException exception) {
            LOGGER.warn("Failed to parse server-bank balances payload", exception);
            return new LinkedHashMap<>();
        }
    }

    private static @NotNull String serializeBalances(final @NotNull Map<String, Double> balances) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Objects.requireNonNull(balances, "balances"));
        } catch (final IOException exception) {
            LOGGER.warn("Failed to serialize server-bank balances payload", exception);
            return "{}";
        }
    }

    private static @NotNull List<LedgerEntry> parseLedgerEntries(final @Nullable String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new ArrayList<>();
        }

        try {
            final List<LedgerEntry> decoded = OBJECT_MAPPER.readValue(rawJson, LEDGER_TYPE);
            if (decoded == null || decoded.isEmpty()) {
                return new ArrayList<>();
            }

            final List<LedgerEntry> normalized = new ArrayList<>(decoded.size());
            for (final LedgerEntry entry : decoded) {
                if (entry != null) {
                    normalized.add(entry);
                }
            }
            return normalized;
        } catch (final IOException exception) {
            LOGGER.warn("Failed to parse server-bank ledger payload", exception);
            return new ArrayList<>();
        }
    }

    private static @NotNull String serializeLedgerEntries(final @NotNull List<LedgerEntry> entries) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Objects.requireNonNull(entries, "entries"));
        } catch (final IOException exception) {
            LOGGER.warn("Failed to serialize server-bank ledger payload", exception);
            return "[]";
        }
    }

    private static @NotNull String normalizeBankScopeToken(final @NotNull String rawScopeToken) {
        final String normalized = Objects.requireNonNull(rawScopeToken, "rawScopeToken").trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? DEFAULT_BANK_SCOPE : normalized;
    }

    private static @NotNull String normalizeCurrencyType(final @NotNull String rawCurrencyType) {
        final String normalized = Objects.requireNonNull(rawCurrencyType, "rawCurrencyType").trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? DEFAULT_CURRENCY : normalized;
    }

    private static double sanitizeAmount(final @Nullable Double rawAmount) {
        if (rawAmount == null || !Double.isFinite(rawAmount)) {
            return 0.0D;
        }
        return Math.max(0.0D, rawAmount);
    }
}
