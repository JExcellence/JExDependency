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

import com.raindropcentral.rdr.database.entity.RTownStorageBank;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Repository for persisted town storage-tax bank ledgers.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRTownStorageBank extends BaseRepository<RTownStorageBank, Long> {

    private static final double EPSILON = 1.0E-6D;

    /**
     * Creates a repository for {@link RTownStorageBank} entities.
     *
     * @param executorService executor used for asynchronous repository operations
     * @param entityManagerFactory entity manager factory backing persistence operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRTownStorageBank(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RTownStorageBank.class);
    }

    /**
     * Finds a town tax-bank entry for the supplied scope.
     *
     * @param protectionPlugin protection plugin name
     * @param townIdentifier town identifier
     * @param currencyType currency identifier
     * @return matching entry, or {@code null} when none exists
     * @throws NullPointerException if any argument is {@code null}
     */
    public @Nullable RTownStorageBank findByScope(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String currencyType
    ) {
        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final String normalizedCurrency = normalizeToken(currencyType, "vault");

        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                """
                    select bankEntry
                    from RTownStorageBank bankEntry
                    where bankEntry.protectionPlugin = :protectionPlugin
                      and bankEntry.townIdentifier = :townIdentifier
                      and bankEntry.currencyType = :currencyType
                    """,
                RTownStorageBank.class
            )
            .setParameter("protectionPlugin", normalizedPlugin)
            .setParameter("townIdentifier", normalizedTown)
            .setParameter("currencyType", normalizedCurrency)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }

    /**
     * Finds all town tax-bank entries for the supplied plugin+town scope.
     *
     * @param protectionPlugin protection plugin name
     * @param townIdentifier town identifier
     * @return immutable list of entries sorted by currency identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull List<RTownStorageBank> findByTown(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier
    ) {
        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final List<RTownStorageBank> entries = this.executeInTransaction(entityManager -> entityManager.createQuery(
                """
                    select bankEntry
                    from RTownStorageBank bankEntry
                    where bankEntry.protectionPlugin = :protectionPlugin
                      and bankEntry.townIdentifier = :townIdentifier
                    """,
                RTownStorageBank.class
            )
            .setParameter("protectionPlugin", normalizedPlugin)
            .setParameter("townIdentifier", normalizedTown)
            .getResultList());
        entries.sort(Comparator.comparing(RTownStorageBank::getCurrencyType, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    /**
     * Deposits amount into the scoped town tax-bank entry, creating it when absent.
     *
     * @param protectionPlugin protection plugin name
     * @param townIdentifier town identifier
     * @param townDisplayName town display name
     * @param currencyType currency identifier
     * @param amount positive amount to deposit
     * @return updated amount after the deposit
     * @throws NullPointerException if any string argument is {@code null}
     */
    public double deposit(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String townDisplayName,
        final @NotNull String currencyType,
        final double amount
    ) {
        if (amount <= EPSILON) {
            final RTownStorageBank existing = this.findByScope(protectionPlugin, townIdentifier, currencyType);
            return existing == null ? 0.0D : existing.getAmount();
        }

        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final String normalizedCurrency = normalizeToken(currencyType, "vault");

        return this.executeInTransaction(entityManager -> {
            RTownStorageBank entry = entityManager.createQuery(
                    """
                        select bankEntry
                        from RTownStorageBank bankEntry
                        where bankEntry.protectionPlugin = :protectionPlugin
                          and bankEntry.townIdentifier = :townIdentifier
                          and bankEntry.currencyType = :currencyType
                        """,
                    RTownStorageBank.class
                )
                .setParameter("protectionPlugin", normalizedPlugin)
                .setParameter("townIdentifier", normalizedTown)
                .setParameter("currencyType", normalizedCurrency)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
            if (entry == null) {
                entry = new RTownStorageBank(
                    normalizedPlugin,
                    normalizedTown,
                    townDisplayName,
                    normalizedCurrency,
                    amount
                );
                entityManager.persist(entry);
                return entry.getAmount();
            }

            entry.setTownDisplayName(townDisplayName);
            return entry.deposit(amount);
        });
    }

    /**
     * Withdraws and clears all available amount for the scoped town tax-bank entry.
     *
     * @param protectionPlugin protection plugin name
     * @param townIdentifier town identifier
     * @param currencyType currency identifier
     * @return withdrawn amount, or {@code 0.0} when nothing was available
     * @throws NullPointerException if any argument is {@code null}
     */
    public double withdrawAll(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String currencyType
    ) {
        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final String normalizedCurrency = normalizeToken(currencyType, "vault");

        return this.executeInTransaction(entityManager -> {
            final RTownStorageBank entry = entityManager.createQuery(
                    """
                        select bankEntry
                        from RTownStorageBank bankEntry
                        where bankEntry.protectionPlugin = :protectionPlugin
                          and bankEntry.townIdentifier = :townIdentifier
                          and bankEntry.currencyType = :currencyType
                        """,
                    RTownStorageBank.class
                )
                .setParameter("protectionPlugin", normalizedPlugin)
                .setParameter("townIdentifier", normalizedTown)
                .setParameter("currencyType", normalizedCurrency)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
            if (entry == null || entry.getAmount() <= EPSILON) {
                return 0.0D;
            }

            final double withdrawnAmount = entry.getAmount();
            entry.withdraw(withdrawnAmount);
            if (entry.getAmount() <= EPSILON) {
                entityManager.remove(entry);
            }
            return withdrawnAmount;
        });
    }

    /**
     * Withdraws up to the requested amount from the scoped town tax-bank entry.
     *
     * @param protectionPlugin protection plugin name
     * @param townIdentifier town identifier
     * @param currencyType currency identifier
     * @param amount requested amount to withdraw
     * @return actual withdrawn amount
     * @throws NullPointerException if any string argument is {@code null}
     */
    public double withdraw(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String currencyType,
        final double amount
    ) {
        if (amount <= EPSILON) {
            return 0.0D;
        }

        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final String normalizedCurrency = normalizeToken(currencyType, "vault");

        return this.executeInTransaction(entityManager -> {
            final RTownStorageBank entry = entityManager.createQuery(
                    """
                        select bankEntry
                        from RTownStorageBank bankEntry
                        where bankEntry.protectionPlugin = :protectionPlugin
                          and bankEntry.townIdentifier = :townIdentifier
                          and bankEntry.currencyType = :currencyType
                        """,
                    RTownStorageBank.class
                )
                .setParameter("protectionPlugin", normalizedPlugin)
                .setParameter("townIdentifier", normalizedTown)
                .setParameter("currencyType", normalizedCurrency)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
            if (entry == null || entry.getAmount() <= EPSILON) {
                return 0.0D;
            }

            final double withdrawAmount = Math.min(entry.getAmount(), amount);
            entry.withdraw(withdrawAmount);
            if (entry.getAmount() <= EPSILON) {
                entityManager.remove(entry);
            }
            return withdrawAmount;
        });
    }

    private static @NotNull String normalizeToken(
        final @NotNull String rawToken,
        final @NotNull String fallback
    ) {
        final String normalized = Objects.requireNonNull(rawToken, "rawToken").trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }
}
