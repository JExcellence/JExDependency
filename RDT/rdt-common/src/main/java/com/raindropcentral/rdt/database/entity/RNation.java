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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent nation aggregate spanning one capital town and zero or more member towns.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_nations")
public class RNation extends BaseEntity {

    @Column(name = "nation_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID nationUuid;

    @Column(name = "nation_name", nullable = false, unique = true, length = 64)
    private String nationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "nation_status", nullable = false, length = 32)
    private NationStatus status;

    @Column(name = "capital_town_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID capitalTownUuid;

    @Column(name = "initiating_town_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID initiatingTownUuid;

    @Column(name = "initiating_player_uuid")
    @Convert(converter = UUIDConverter.class)
    private UUID initiatingPlayerUuid;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(name = "minimum_town_threshold", nullable = false)
    private int minimumTownThreshold;

    @Column(name = "nation_level")
    private Integer nationLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_nation_level_currency_progress", joinColumns = @JoinColumn(name = "nation_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> levelCurrencyProgress = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_nation_bank_balances", joinColumns = @JoinColumn(name = "nation_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> bankBalances = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "level_item_progress", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> levelItemProgress = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "shared_bank_storage", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> sharedBankStorage = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_nation_tax_currency_debt", joinColumns = @JoinColumn(name = "nation_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> currencyTaxDebt = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "item_tax_debt", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> itemTaxDebt = new LinkedHashMap<>();

    @Column(name = "tax_debt_started_at")
    private Long taxDebtStartedAt;

    @Column(name = "tax_debt_last_warning_at")
    private Long taxDebtLastWarningAt;

    /**
     * Creates a nation record.
     *
     * @param nationUuid nation UUID
     * @param nationName nation name
     * @param capitalTownUuid capital town UUID
     * @param initiatingTownUuid initiating town UUID
     * @param initiatingPlayerUuid initiating player UUID
     * @param minimumTownThreshold minimum required towns including the capital town
     * @param expiresAt pending formation expiry timestamp
     */
    public RNation(
        final @NotNull UUID nationUuid,
        final @NotNull String nationName,
        final @NotNull UUID capitalTownUuid,
        final @NotNull UUID initiatingTownUuid,
        final @Nullable UUID initiatingPlayerUuid,
        final int minimumTownThreshold,
        final long expiresAt
    ) {
        this.nationUuid = Objects.requireNonNull(nationUuid, "nationUuid");
        this.nationName = normalizeNationName(nationName);
        this.status = NationStatus.PENDING;
        this.capitalTownUuid = Objects.requireNonNull(capitalTownUuid, "capitalTownUuid");
        this.initiatingTownUuid = Objects.requireNonNull(initiatingTownUuid, "initiatingTownUuid");
        this.initiatingPlayerUuid = initiatingPlayerUuid;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = Math.max(0L, expiresAt);
        this.minimumTownThreshold = Math.max(2, minimumTownThreshold);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RNation() {
    }

    /**
     * Returns the stable nation identifier.
     *
     * @return nation UUID
     */
    public @NotNull UUID getNationUuid() {
        return Objects.requireNonNull(this.nationUuid, "nationUuid");
    }

    /**
     * Returns the nation name.
     *
     * @return nation name
     */
    public @NotNull String getNationName() {
        return Objects.requireNonNull(this.nationName, "nationName");
    }

    /**
     * Replaces the nation name.
     *
     * @param nationName replacement nation name
     */
    public void setNationName(final @NotNull String nationName) {
        this.nationName = normalizeNationName(nationName);
    }

    /**
     * Returns the nation status.
     *
     * @return nation status
     */
    public @NotNull NationStatus getStatus() {
        return this.status == null ? NationStatus.PENDING : this.status;
    }

    /**
     * Replaces the nation status.
     *
     * @param status replacement nation status
     */
    public void setStatus(final @NotNull NationStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * Returns the capital town UUID.
     *
     * @return capital town UUID
     */
    public @NotNull UUID getCapitalTownUuid() {
        return Objects.requireNonNull(this.capitalTownUuid, "capitalTownUuid");
    }

    /**
     * Replaces the capital town UUID.
     *
     * @param capitalTownUuid replacement capital town UUID
     */
    public void setCapitalTownUuid(final @NotNull UUID capitalTownUuid) {
        this.capitalTownUuid = Objects.requireNonNull(capitalTownUuid, "capitalTownUuid");
    }

    /**
     * Returns the initiating town UUID.
     *
     * @return initiating town UUID
     */
    public @NotNull UUID getInitiatingTownUuid() {
        return Objects.requireNonNull(this.initiatingTownUuid, "initiatingTownUuid");
    }

    /**
     * Returns the initiating player UUID.
     *
     * @return initiating player UUID, or {@code null} when unavailable
     */
    public @Nullable UUID getInitiatingPlayerUuid() {
        return this.initiatingPlayerUuid;
    }

    /**
     * Returns the creation timestamp in epoch milliseconds.
     *
     * @return creation timestamp
     */
    public long getCreatedAtMillis() {
        return Math.max(0L, this.createdAt);
    }

    /**
     * Returns the pending expiry timestamp in epoch milliseconds.
     *
     * @return pending expiry timestamp
     */
    public long getExpiresAt() {
        return Math.max(0L, this.expiresAt);
    }

    /**
     * Replaces the pending expiry timestamp.
     *
     * @param expiresAt replacement expiry timestamp
     */
    public void setExpiresAt(final long expiresAt) {
        this.expiresAt = Math.max(0L, expiresAt);
    }

    /**
     * Returns the minimum required towns including the capital town.
     *
     * @return minimum nation town threshold
     */
    public int getMinimumTownThreshold() {
        return Math.max(2, this.minimumTownThreshold);
    }

    /**
     * Replaces the minimum required towns including the capital town.
     *
     * @param minimumTownThreshold replacement minimum threshold
     */
    public void setMinimumTownThreshold(final int minimumTownThreshold) {
        this.minimumTownThreshold = Math.max(2, minimumTownThreshold);
    }

    /**
     * Returns whether the persisted nation level has been written already.
     *
     * @return {@code true} when the nation level is already persisted
     */
    public boolean hasPersistedNationLevel() {
        return this.nationLevel != null;
    }

    /**
     * Backfills legacy active nations to the level-one baseline when no level was stored yet.
     *
     * @return {@code true} when the nation level changed
     */
    public boolean backfillLegacyNationLevelIfNeeded() {
        if (this.nationLevel != null || !this.isActive()) {
            return false;
        }
        this.nationLevel = 1;
        return true;
    }

    /**
     * Returns the current active nation level.
     *
     * @return active nation level
     */
    public int getNationLevel() {
        return Math.max(1, this.nationLevel == null ? 1 : this.nationLevel);
    }

    /**
     * Replaces the current active nation level.
     *
     * @param nationLevel replacement active nation level
     */
    public void setNationLevel(final int nationLevel) {
        this.nationLevel = Math.max(1, nationLevel);
    }

    /**
     * Returns copied nation-bank balances keyed by normalized currency identifier.
     *
     * @return copied nation-bank balances
     */
    public @NotNull Map<String, Double> getBankBalances() {
        return new LinkedHashMap<>(this.bankBalances);
    }

    /**
     * Returns the stored nation-bank balance for one currency identifier.
     *
     * @param currencyId currency identifier
     * @return stored nation-bank balance
     */
    public double getBankAmount(final @NotNull String currencyId) {
        return this.bankBalances.getOrDefault(normalizeProgressKey(currencyId), 0.0D);
    }

    /**
     * Deposits one amount into a nation-bank balance.
     *
     * @param currencyId currency identifier
     * @param amount amount to add
     * @return updated balance
     */
    public double depositBank(final @NotNull String currencyId, final double amount) {
        if (amount <= 0.0D) {
            return this.getBankAmount(currencyId);
        }
        final String normalizedCurrencyId = normalizeProgressKey(currencyId);
        final double updatedBalance = this.getBankAmount(normalizedCurrencyId) + amount;
        this.bankBalances.put(normalizedCurrencyId, updatedBalance);
        return updatedBalance;
    }

    /**
     * Withdraws one amount from a nation-bank balance.
     *
     * @param currencyId currency identifier
     * @param amount amount to subtract
     * @return {@code true} when the balance covered the withdrawal
     */
    public boolean withdrawBank(final @NotNull String currencyId, final double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        final String normalizedCurrencyId = normalizeProgressKey(currencyId);
        final double currentBalance = this.getBankAmount(normalizedCurrencyId);
        if (currentBalance + 1.0E-6D < amount) {
            return false;
        }
        final double remainingBalance = currentBalance - amount;
        if (remainingBalance <= 1.0E-6D) {
            this.bankBalances.remove(normalizedCurrencyId);
        } else {
            this.bankBalances.put(normalizedCurrencyId, remainingBalance);
        }
        return true;
    }

    /**
     * Returns the persisted shared nation-bank item storage.
     *
     * @return copied shared nation-bank storage
     */
    public @NotNull Map<String, ItemStack> getSharedBankStorage() {
        return new LinkedHashMap<>(this.sharedBankStorage);
    }

    /**
     * Replaces the persisted shared nation-bank item storage.
     *
     * @param sharedBankStorage replacement shared storage snapshot
     */
    public void setSharedBankStorage(final @NotNull Map<String, ItemStack> sharedBankStorage) {
        this.sharedBankStorage = new LinkedHashMap<>(Objects.requireNonNull(sharedBankStorage, "sharedBankStorage"));
    }

    /**
     * Returns copied outstanding nation currency tax debt keyed by normalized currency identifier.
     *
     * @return copied outstanding currency tax debt
     */
    public @NotNull Map<String, Double> getCurrencyTaxDebt() {
        return new LinkedHashMap<>(this.currencyTaxDebt);
    }

    /**
     * Returns stored outstanding tax debt for one currency identifier.
     *
     * @param currencyId currency identifier
     * @return stored outstanding tax debt
     */
    public double getCurrencyTaxDebt(final @NotNull String currencyId) {
        return this.currencyTaxDebt.getOrDefault(normalizeProgressKey(currencyId), 0.0D);
    }

    /**
     * Replaces stored outstanding tax debt for one currency identifier.
     *
     * @param currencyId currency identifier
     * @param amount replacement debt amount
     */
    public void setCurrencyTaxDebt(final @NotNull String currencyId, final double amount) {
        final String normalizedCurrencyId = normalizeProgressKey(currencyId);
        if (amount <= 1.0E-6D) {
            this.currencyTaxDebt.remove(normalizedCurrencyId);
            return;
        }
        this.currencyTaxDebt.put(normalizedCurrencyId, amount);
    }

    /**
     * Returns copied outstanding nation item tax debt keyed by normalized item identifier.
     *
     * @return copied outstanding item tax debt
     */
    public @NotNull Map<String, ItemStack> getItemTaxDebt() {
        return new LinkedHashMap<>(this.itemTaxDebt);
    }

    /**
     * Returns stored outstanding item tax debt for one normalized item identifier.
     *
     * @param itemKey normalized item identifier
     * @return stored outstanding item tax debt, or {@code null} when absent
     */
    public @Nullable ItemStack getItemTaxDebt(final @NotNull String itemKey) {
        return this.itemTaxDebt.get(normalizeProgressKey(itemKey));
    }

    /**
     * Replaces stored outstanding item tax debt for one normalized item identifier.
     *
     * @param itemKey normalized item identifier
     * @param itemStack replacement debt stack, or {@code null} to clear it
     */
    public void setItemTaxDebt(final @NotNull String itemKey, final @Nullable ItemStack itemStack) {
        final String normalizedItemKey = normalizeProgressKey(itemKey);
        if (itemStack == null || itemStack.isEmpty()) {
            this.itemTaxDebt.remove(normalizedItemKey);
            return;
        }
        this.itemTaxDebt.put(normalizedItemKey, itemStack.clone());
    }

    /**
     * Clears every stored nation tax debt entry and warning timestamp.
     */
    public void clearTaxDebt() {
        this.currencyTaxDebt.clear();
        this.itemTaxDebt.clear();
        this.taxDebtStartedAt = null;
        this.taxDebtLastWarningAt = null;
    }

    /**
     * Returns whether the nation currently owes any stored tax debt.
     *
     * @return {@code true} when either currency or item tax debt exists
     */
    public boolean hasTaxDebt() {
        return !this.currencyTaxDebt.isEmpty() || !this.itemTaxDebt.isEmpty();
    }

    /**
     * Returns the timestamp when the nation first entered tax debt.
     *
     * @return debt-start timestamp in epoch milliseconds, or {@code 0} when no timestamp is stored
     */
    public long getTaxDebtStartedAt() {
        return Math.max(0L, this.taxDebtStartedAt == null ? 0L : this.taxDebtStartedAt);
    }

    /**
     * Replaces the timestamp when the nation first entered tax debt.
     *
     * @param taxDebtStartedAt replacement debt-start timestamp
     */
    public void setTaxDebtStartedAt(final long taxDebtStartedAt) {
        this.taxDebtStartedAt = taxDebtStartedAt <= 0L ? null : taxDebtStartedAt;
    }

    /**
     * Returns the timestamp when the nation last received a tax-debt warning.
     *
     * @return warning timestamp in epoch milliseconds, or {@code 0} when none is stored
     */
    public long getTaxDebtLastWarningAt() {
        return Math.max(0L, this.taxDebtLastWarningAt == null ? 0L : this.taxDebtLastWarningAt);
    }

    /**
     * Replaces the timestamp when the nation last received a tax-debt warning.
     *
     * @param taxDebtLastWarningAt replacement warning timestamp
     */
    public void setTaxDebtLastWarningAt(final long taxDebtLastWarningAt) {
        this.taxDebtLastWarningAt = taxDebtLastWarningAt <= 0L ? null : taxDebtLastWarningAt;
    }

    /**
     * Returns stored nation level-item progress.
     *
     * @return stored nation level-item progress
     */
    public @NotNull Map<String, ItemStack> getLevelItemProgress() {
        return new LinkedHashMap<>(this.levelItemProgress);
    }

    /**
     * Returns stored nation level-item progress for one key.
     *
     * @param progressKey progress key to inspect
     * @return stored item progress, or {@code null} when absent
     */
    public @Nullable ItemStack getLevelItemProgress(final @NotNull String progressKey) {
        return this.levelItemProgress.get(normalizeProgressKey(progressKey));
    }

    /**
     * Replaces stored nation level-item progress for one key.
     *
     * @param progressKey progress key to update
     * @param itemStack replacement progress stack, or {@code null} to clear it
     */
    public void setLevelItemProgress(
        final @NotNull String progressKey,
        final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (itemStack == null || itemStack.isEmpty()) {
            this.levelItemProgress.remove(normalizedProgressKey);
            return;
        }
        this.levelItemProgress.put(normalizedProgressKey, itemStack.clone());
    }

    /**
     * Returns stored nation level-currency progress for one key.
     *
     * @param progressKey progress key to inspect
     * @return stored currency progress
     */
    public double getLevelCurrencyProgress(final @NotNull String progressKey) {
        return this.levelCurrencyProgress.getOrDefault(normalizeProgressKey(progressKey), 0.0D);
    }

    /**
     * Returns stored nation level-currency progress.
     *
     * @return stored nation level-currency progress
     */
    public @NotNull Map<String, Double> getLevelCurrencyProgress() {
        return new LinkedHashMap<>(this.levelCurrencyProgress);
    }

    /**
     * Replaces stored nation level-currency progress for one key.
     *
     * @param progressKey progress key to update
     * @param amount replacement currency progress
     */
    public void setLevelCurrencyProgress(final @NotNull String progressKey, final double amount) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (amount <= 0.0D) {
            this.levelCurrencyProgress.remove(normalizedProgressKey);
            return;
        }
        this.levelCurrencyProgress.put(normalizedProgressKey, amount);
    }

    /**
     * Clears nation requirement progress entries by prefix.
     *
     * @param progressKeyPrefix stable progress-key prefix to clear
     */
    public void clearLevelRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String normalizedPrefix = normalizeProgressKey(progressKeyPrefix);
        this.levelItemProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
        this.levelCurrencyProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
    }

    /**
     * Returns whether the nation is actively formed.
     *
     * @return {@code true} when the nation is active
     */
    public boolean isActive() {
        return this.getStatus() == NationStatus.ACTIVE;
    }

    /**
     * Returns whether the nation is still pending formation.
     *
     * @return {@code true} when the nation is pending formation
     */
    public boolean isPending() {
        return this.getStatus() == NationStatus.PENDING;
    }

    /**
     * Normalizes a nation name for case-insensitive lookups.
     *
     * @param nationName raw nation name
     * @return normalized lookup token
     */
    public static @NotNull String normalizeNationLookupName(final @NotNull String nationName) {
        return normalizeNationName(nationName).toLowerCase(Locale.ROOT);
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalized = Objects.requireNonNull(progressKey, "progressKey").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeNationName(final @NotNull String nationName) {
        final String normalized = Objects.requireNonNull(nationName, "nationName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("nationName cannot be blank");
        }
        return normalized;
    }
}
