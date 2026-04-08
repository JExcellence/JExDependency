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

package com.raindropcentral.rdr.database.repository;

import com.raindropcentral.rdr.database.entity.RServerBank;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository for persisted trade-tax server bank balances and transaction ledgers.
 *
 * <p>This repository stores a single global bank row and exposes currency-scoped deposit/withdraw
 * operations against the bank's internal multi-currency balance map.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRServerBank extends BaseRepository<RServerBank, Long> {

    private static final double EPSILON = 1.0E-6D;
    private static final String GLOBAL_BANK_SCOPE = "global";
    private static final String DEFAULT_CURRENCY = "vault";
    private static final String UNSPECIFIED_NOTE = "unspecified";

    /**
     * Creates a repository for {@link RServerBank} entities.
     *
     * @param executorService executor used for asynchronous repository operations
     * @param entityManagerFactory entity manager factory backing persistence operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRServerBank(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RServerBank.class);
    }

    /**
     * Finds the global server-bank row.
     *
     * @return global bank row, or {@code null} when not yet created
     */
    public @Nullable RServerBank findGlobalBank() {
        return this.executeInTransaction(RRServerBank::findGlobalBank);
    }

    /**
     * Finds or creates the global server-bank row.
     *
     * @return global bank row
     */
    public @NotNull RServerBank findOrCreateGlobalBank() {
        return this.executeInTransaction(entityManager -> {
            RServerBank bank = findGlobalBankForUpdate(entityManager);
            if (bank != null) {
                return bank;
            }

            bank = new RServerBank(GLOBAL_BANK_SCOPE);
            entityManager.persist(bank);
            return bank;
        });
    }

    /**
     * Returns the currently stored balance for one currency.
     *
     * @param currencyType currency identifier
     * @return non-negative currency balance
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public double getBalance(final @NotNull String currencyType) {
        final String normalizedCurrency = normalizeCurrencyType(currencyType);
        final RServerBank bank = this.findGlobalBank();
        return bank == null ? 0.0D : bank.getBalance(normalizedCurrency);
    }

    /**
     * Returns all currently stored balances keyed by currency identifier.
     *
     * @return immutable currency balance map
     */
    public @NotNull Map<String, Double> findBalances() {
        final RServerBank bank = this.findGlobalBank();
        return bank == null ? Map.of() : bank.getBalances();
    }

    /**
     * Deposits amount into one currency balance and appends a ledger transaction.
     *
     * @param currencyType currency identifier
     * @param amount positive amount to deposit
     * @param actorUuid optional actor UUID that initiated the deposit
     * @param tradeUuid optional trade UUID linked to the deposit
     * @param note transaction note
     * @return updated balance after deposit
     * @throws NullPointerException if {@code currencyType} or {@code note} is {@code null}
     */
    public double deposit(
        final @NotNull String currencyType,
        final double amount,
        final @Nullable UUID actorUuid,
        final @Nullable UUID tradeUuid,
        final @NotNull String note
    ) {
        final String normalizedCurrency = normalizeCurrencyType(currencyType);
        final String normalizedNote = normalizeNote(note);
        if (amount <= EPSILON) {
            return this.getBalance(normalizedCurrency);
        }

        return this.executeInTransaction(entityManager -> {
            RServerBank bank = findGlobalBankForUpdate(entityManager);
            if (bank == null) {
                bank = new RServerBank(GLOBAL_BANK_SCOPE);
                entityManager.persist(bank);
            }

            final double updatedBalance = bank.deposit(normalizedCurrency, amount);
            bank.appendLedgerEntry(new RServerBank.LedgerEntry(
                System.currentTimeMillis(),
                normalizedCurrency,
                RServerBank.TransactionType.DEPOSIT,
                amount,
                updatedBalance,
                actorUuid == null ? null : actorUuid.toString(),
                tradeUuid == null ? null : tradeUuid.toString(),
                normalizedNote
            ));
            return updatedBalance;
        });
    }

    /**
     * Withdraws up to the requested amount from one currency balance and appends a ledger transaction.
     *
     * @param currencyType currency identifier
     * @param amount requested amount to withdraw
     * @param actorUuid optional actor UUID that initiated the withdrawal
     * @param tradeUuid optional trade UUID linked to the withdrawal
     * @param note transaction note
     * @return actual withdrawn amount
     * @throws NullPointerException if {@code currencyType} or {@code note} is {@code null}
     */
    public double withdraw(
        final @NotNull String currencyType,
        final double amount,
        final @Nullable UUID actorUuid,
        final @Nullable UUID tradeUuid,
        final @NotNull String note
    ) {
        final String normalizedCurrency = normalizeCurrencyType(currencyType);
        final String normalizedNote = normalizeNote(note);
        if (amount <= EPSILON) {
            return 0.0D;
        }

        return this.executeInTransaction(entityManager -> {
            final RServerBank bank = findGlobalBankForUpdate(entityManager);
            if (bank == null || bank.getBalance(normalizedCurrency) <= EPSILON) {
                return 0.0D;
            }

            final double withdrawnAmount = bank.withdraw(normalizedCurrency, amount);
            if (withdrawnAmount <= EPSILON) {
                return 0.0D;
            }

            bank.appendLedgerEntry(new RServerBank.LedgerEntry(
                System.currentTimeMillis(),
                normalizedCurrency,
                RServerBank.TransactionType.WITHDRAW,
                withdrawnAmount,
                bank.getBalance(normalizedCurrency),
                actorUuid == null ? null : actorUuid.toString(),
                tradeUuid == null ? null : tradeUuid.toString(),
                normalizedNote
            ));
            return withdrawnAmount;
        });
    }

    /**
     * Withdraws the full available balance from one currency.
     *
     * @param currencyType currency identifier
     * @param actorUuid optional actor UUID that initiated the withdrawal
     * @param tradeUuid optional trade UUID linked to the withdrawal
     * @param note transaction note
     * @return actual withdrawn amount
     * @throws NullPointerException if {@code currencyType} or {@code note} is {@code null}
     */
    public double withdrawAll(
        final @NotNull String currencyType,
        final @Nullable UUID actorUuid,
        final @Nullable UUID tradeUuid,
        final @NotNull String note
    ) {
        final String normalizedCurrency = normalizeCurrencyType(currencyType);
        final String normalizedNote = normalizeNote(note);
        return this.executeInTransaction(entityManager -> {
            final RServerBank bank = findGlobalBankForUpdate(entityManager);
            if (bank == null) {
                return 0.0D;
            }

            final double currentBalance = bank.getBalance(normalizedCurrency);
            if (currentBalance <= EPSILON) {
                return 0.0D;
            }

            final double withdrawnAmount = bank.withdraw(normalizedCurrency, currentBalance);
            if (withdrawnAmount <= EPSILON) {
                return 0.0D;
            }

            bank.appendLedgerEntry(new RServerBank.LedgerEntry(
                System.currentTimeMillis(),
                normalizedCurrency,
                RServerBank.TransactionType.WITHDRAW,
                withdrawnAmount,
                bank.getBalance(normalizedCurrency),
                actorUuid == null ? null : actorUuid.toString(),
                tradeUuid == null ? null : tradeUuid.toString(),
                normalizedNote
            ));
            return withdrawnAmount;
        });
    }

    /**
     * Returns the newest ledger entries for one currency.
     *
     * @param currencyType currency identifier
     * @param limit maximum amount of entries to return
     * @return immutable newest-first ledger entries
     * @throws NullPointerException if {@code currencyType} is {@code null}
     */
    public @NotNull List<RServerBank.LedgerEntry> findRecentLedgerEntries(
        final @NotNull String currencyType,
        final int limit
    ) {
        final RServerBank bank = this.findGlobalBank();
        if (bank == null) {
            return List.of();
        }
        return bank.getRecentLedgerEntriesForCurrency(normalizeCurrencyType(currencyType), limit);
    }

    private static @Nullable RServerBank findGlobalBank(final @NotNull EntityManager entityManager) {
        return entityManager.createQuery(
                """
                    select bankEntry
                    from RServerBank bankEntry
                    where bankEntry.bankScope = :scope
                    """,
                RServerBank.class
            )
            .setParameter("scope", GLOBAL_BANK_SCOPE)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private static @Nullable RServerBank findGlobalBankForUpdate(
        final @NotNull EntityManager entityManager
    ) {
        return entityManager.createQuery(
                """
                    select bankEntry
                    from RServerBank bankEntry
                    where bankEntry.bankScope = :scope
                    """,
                RServerBank.class
            )
            .setParameter("scope", GLOBAL_BANK_SCOPE)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private static @NotNull String normalizeCurrencyType(final @NotNull String rawCurrencyType) {
        final String normalized = Objects.requireNonNull(rawCurrencyType, "rawCurrencyType")
            .trim()
            .toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? DEFAULT_CURRENCY : normalized;
    }

    private static @NotNull String normalizeNote(final @NotNull String rawNote) {
        final String normalized = Objects.requireNonNull(rawNote, "rawNote").trim();
        return normalized.isEmpty() ? UNSPECIFIED_NOTE : normalized;
    }
}
