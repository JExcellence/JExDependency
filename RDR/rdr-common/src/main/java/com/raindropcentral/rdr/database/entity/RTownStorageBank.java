package com.raindropcentral.rdr.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * Persistent town-owned ledger entry for collected RDR storage taxes.
 *
 * <p>Each row tracks one currency for one protection-plugin town identifier. Mayors can transfer
 * these accrued balances into their external protection-plugin town bank.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(
    name = "rdr_town_storage_bank",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rdr_town_storage_bank_scope",
            columnNames = {"protection_plugin", "town_identifier", "currency_type"}
        )
    }
)
@SuppressWarnings({
    "unused",
    "FieldCanBeLocal",
    "JpaDataSourceORMInspection"
})
public class RTownStorageBank extends BaseEntity {

    @Column(name = "protection_plugin", nullable = false, length = 64)
    private String protectionPlugin;

    @Column(name = "town_identifier", nullable = false, length = 256)
    private String townIdentifier;

    @Column(name = "town_display_name", nullable = false, length = 128)
    private String townDisplayName;

    @Column(name = "currency_type", nullable = false, length = 64)
    private String currencyType;

    @Column(name = "amount", nullable = false)
    private double amount;

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RTownStorageBank() {
    }

    /**
     * Creates a new town tax-bank ledger entry.
     *
     * @param protectionPlugin protection plugin name (for example {@code towny})
     * @param townIdentifier stable town identifier
     * @param townDisplayName town display name
     * @param currencyType normalized currency identifier
     * @param amount starting amount
     * @throws NullPointerException if any string argument is {@code null}
     */
    public RTownStorageBank(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String townDisplayName,
        final @NotNull String currencyType,
        final double amount
    ) {
        this.protectionPlugin = normalizeScopeToken(protectionPlugin, "unknown");
        this.townIdentifier = normalizeScopeToken(townIdentifier, "unknown");
        this.townDisplayName = normalizeDisplayName(townDisplayName, this.townIdentifier);
        this.currencyType = normalizeScopeToken(currencyType, "vault");
        this.amount = Math.max(0.0D, amount);
    }

    /**
     * Returns the normalized protection plugin name for this entry.
     *
     * @return protection plugin name
     */
    public @NotNull String getProtectionPlugin() {
        return this.protectionPlugin;
    }

    /**
     * Returns the stable normalized town identifier for this entry.
     *
     * @return town identifier
     */
    public @NotNull String getTownIdentifier() {
        return this.townIdentifier;
    }

    /**
     * Returns the display name associated with the town identifier.
     *
     * @return town display name
     */
    public @NotNull String getTownDisplayName() {
        return this.townDisplayName;
    }

    /**
     * Updates the display name associated with this town entry.
     *
     * @param townDisplayName replacement town display name
     * @throws NullPointerException if {@code townDisplayName} is {@code null}
     */
    public void setTownDisplayName(final @NotNull String townDisplayName) {
        this.townDisplayName = normalizeDisplayName(townDisplayName, this.townIdentifier);
    }

    /**
     * Returns the normalized currency identifier tracked by this entry.
     *
     * @return currency identifier
     */
    public @NotNull String getCurrencyType() {
        return this.currencyType;
    }

    /**
     * Returns the currently persisted amount for this entry.
     *
     * @return non-negative amount
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * Deposits additional amount into this entry.
     *
     * @param depositAmount amount to add
     * @return updated amount
     */
    public double deposit(final double depositAmount) {
        if (depositAmount <= 0.0D) {
            return this.amount;
        }
        this.amount += depositAmount;
        return this.amount;
    }

    /**
     * Withdraws amount from this entry.
     *
     * @param withdrawAmount amount to subtract
     * @return updated amount
     */
    public double withdraw(final double withdrawAmount) {
        if (withdrawAmount <= 0.0D) {
            return this.amount;
        }
        this.amount = Math.max(0.0D, this.amount - withdrawAmount);
        return this.amount;
    }

    /**
     * Indicates whether this entry belongs to the supplied plugin-town-currency scope.
     *
     * @param protectionPlugin protection plugin name
     * @param townIdentifier town identifier
     * @param currencyType currency identifier
     * @return {@code true} when all scope values match ignoring case/whitespace normalization
     */
    public boolean matchesScope(
        final @Nullable String protectionPlugin,
        final @Nullable String townIdentifier,
        final @Nullable String currencyType
    ) {
        if (protectionPlugin == null || townIdentifier == null || currencyType == null) {
            return false;
        }
        return this.protectionPlugin.equals(normalizeScopeToken(protectionPlugin, "unknown"))
            && this.townIdentifier.equals(normalizeScopeToken(townIdentifier, "unknown"))
            && this.currencyType.equals(normalizeScopeToken(currencyType, "vault"));
    }

    private static @NotNull String normalizeScopeToken(
        final @NotNull String rawToken,
        final @NotNull String fallback
    ) {
        final String normalized = Objects.requireNonNull(rawToken, "rawToken").trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static @NotNull String normalizeDisplayName(
        final @NotNull String rawDisplayName,
        final @NotNull String fallback
    ) {
        final String normalized = Objects.requireNonNull(rawDisplayName, "rawDisplayName").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
