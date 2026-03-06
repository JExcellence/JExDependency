package com.raindropcentral.rds.database.repository;

import com.raindropcentral.rds.database.entity.RTownShopBank;
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
 * Repository for persisted town shop-tax bank ledgers.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class RRTownShopBank extends BaseRepository<RTownShopBank, Long> {

    private static final double EPSILON = 1.0E-6D;

    /**
     * Creates a repository for {@link RTownShopBank} entities.
     *
     * @param executorService executor used for asynchronous repository operations
     * @param entityManagerFactory entity manager factory backing persistence operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRTownShopBank(
            final @NotNull ExecutorService executorService,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RTownShopBank.class);
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
    public @Nullable RTownShopBank findByScope(
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
                                from RTownShopBank bankEntry
                                where bankEntry.protectionPlugin = :protectionPlugin
                                  and bankEntry.townIdentifier = :townIdentifier
                                  and bankEntry.currencyType = :currencyType
                                """,
                        RTownShopBank.class
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
    public @NotNull List<RTownShopBank> findByTown(
            final @NotNull String protectionPlugin,
            final @NotNull String townIdentifier
    ) {
        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final List<RTownShopBank> entries = this.executeInTransaction(entityManager -> entityManager.createQuery(
                        """
                                select bankEntry
                                from RTownShopBank bankEntry
                                where bankEntry.protectionPlugin = :protectionPlugin
                                  and bankEntry.townIdentifier = :townIdentifier
                                """,
                        RTownShopBank.class
                )
                .setParameter("protectionPlugin", normalizedPlugin)
                .setParameter("townIdentifier", normalizedTown)
                .getResultList());
        entries.sort(Comparator.comparing(RTownShopBank::getCurrencyType, String.CASE_INSENSITIVE_ORDER));
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
            final RTownShopBank existing = this.findByScope(protectionPlugin, townIdentifier, currencyType);
            return existing == null ? 0.0D : existing.getAmount();
        }

        final String normalizedPlugin = normalizeToken(protectionPlugin, "unknown");
        final String normalizedTown = normalizeToken(townIdentifier, "unknown");
        final String normalizedCurrency = normalizeToken(currencyType, "vault");

        return this.executeInTransaction(entityManager -> {
            RTownShopBank entry = entityManager.createQuery(
                            """
                                    select bankEntry
                                    from RTownShopBank bankEntry
                                    where bankEntry.protectionPlugin = :protectionPlugin
                                      and bankEntry.townIdentifier = :townIdentifier
                                      and bankEntry.currencyType = :currencyType
                                    """,
                            RTownShopBank.class
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
                entry = new RTownShopBank(
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
            final RTownShopBank entry = entityManager.createQuery(
                            """
                                    select bankEntry
                                    from RTownShopBank bankEntry
                                    where bankEntry.protectionPlugin = :protectionPlugin
                                      and bankEntry.townIdentifier = :townIdentifier
                                      and bankEntry.currencyType = :currencyType
                                    """,
                            RTownShopBank.class
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
