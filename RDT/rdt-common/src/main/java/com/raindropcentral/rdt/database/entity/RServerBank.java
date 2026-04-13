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
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent singleton aggregate that stores all collected RDT taxes.
 *
 * <p>The server bank owns both configured currency balances and confiscated item storage so tax
 * collection remains durable across restarts while still exposing one admin-only UI.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_server_bank")
public class RServerBank extends BaseEntity {

    /** Stable singleton key used for the one persisted server-bank row. */
    public static final String DEFAULT_BANK_KEY = "PRIMARY";

    @Column(name = "bank_key", nullable = false, unique = true, length = 32)
    private String bankKey;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_server_bank_balances", joinColumns = @JoinColumn(name = "server_bank_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> currencyBalances = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "shared_storage", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> sharedStorage = new LinkedHashMap<>();

    /**
     * Creates the default singleton server-bank aggregate.
     */
    public RServerBank() {
        this(DEFAULT_BANK_KEY);
    }

    /**
     * Creates a server-bank aggregate with an explicit key.
     *
     * @param bankKey stable singleton key
     */
    public RServerBank(final @NotNull String bankKey) {
        this.bankKey = normalizeBankKey(bankKey);
    }

    /**
     * Returns the stable singleton bank key.
     *
     * @return normalized bank key
     */
    public @NotNull String getBankKey() {
        return this.bankKey;
    }

    /**
     * Returns a defensive copy of stored currency balances.
     *
     * @return copied currency balances
     */
    public @NotNull Map<String, Double> getCurrencyBalances() {
        return new LinkedHashMap<>(this.currencyBalances);
    }

    /**
     * Returns the stored balance for one currency identifier.
     *
     * @param currencyId currency identifier
     * @return stored balance
     */
    public double getCurrencyBalance(final @NotNull String currencyId) {
        return this.currencyBalances.getOrDefault(normalizeCurrencyId(currencyId), 0.0D);
    }

    /**
     * Replaces the stored balance for one currency identifier.
     *
     * @param currencyId currency identifier
     * @param amount replacement amount
     */
    public void setCurrencyBalance(final @NotNull String currencyId, final double amount) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (amount <= 0.0D) {
            this.currencyBalances.remove(normalizedCurrencyId);
            return;
        }
        this.currencyBalances.put(normalizedCurrencyId, amount);
    }

    /**
     * Deposits one amount into a stored server-bank currency balance.
     *
     * @param currencyId currency identifier
     * @param amount amount to add
     * @return updated balance
     */
    public double depositCurrency(final @NotNull String currencyId, final double amount) {
        if (amount <= 0.0D) {
            return this.getCurrencyBalance(currencyId);
        }
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        final double updatedBalance = this.getCurrencyBalance(normalizedCurrencyId) + amount;
        this.currencyBalances.put(normalizedCurrencyId, updatedBalance);
        return updatedBalance;
    }

    /**
     * Withdraws one amount from a stored server-bank currency balance.
     *
     * @param currencyId currency identifier
     * @param amount amount to subtract
     * @return {@code true} when the stored balance covered the withdrawal
     */
    public boolean withdrawCurrency(final @NotNull String currencyId, final double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        final double currentBalance = this.getCurrencyBalance(normalizedCurrencyId);
        if (currentBalance + 1.0E-6D < amount) {
            return false;
        }
        final double remainingBalance = currentBalance - amount;
        if (remainingBalance <= 1.0E-6D) {
            this.currencyBalances.remove(normalizedCurrencyId);
        } else {
            this.currencyBalances.put(normalizedCurrencyId, remainingBalance);
        }
        return true;
    }

    /**
     * Returns the persisted shared item storage snapshot.
     *
     * @return copied shared storage snapshot
     */
    public @NotNull Map<String, ItemStack> getSharedStorage() {
        return new LinkedHashMap<>(this.sharedStorage);
    }

    /**
     * Replaces the persisted shared item storage snapshot.
     *
     * @param sharedStorage replacement storage snapshot
     */
    public void setSharedStorage(final @NotNull Map<String, ItemStack> sharedStorage) {
        this.sharedStorage = new LinkedHashMap<>(Objects.requireNonNull(sharedStorage, "sharedStorage"));
    }

    /**
     * Returns the stored item debt entry for one material key.
     *
     * @param itemKey normalized item key
     * @return stored item stack, or {@code null} when absent
     */
    public @Nullable ItemStack getStoredItem(final @NotNull String itemKey) {
        return this.sharedStorage.get(normalizeItemKey(itemKey));
    }

    /**
     * Replaces one stored item entry using a normalized material key.
     *
     * @param itemKey normalized material key
     * @param itemStack replacement item stack, or {@code null} to clear the entry
     */
    public void setStoredItem(final @NotNull String itemKey, final @Nullable ItemStack itemStack) {
        final String normalizedItemKey = normalizeItemKey(itemKey);
        if (itemStack == null || itemStack.isEmpty()) {
            this.sharedStorage.remove(normalizedItemKey);
            return;
        }
        this.sharedStorage.put(normalizedItemKey, itemStack.clone());
    }

    private static @NotNull String normalizeBankKey(final @NotNull String bankKey) {
        final String normalized = Objects.requireNonNull(bankKey, "bankKey").trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("bankKey cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeCurrencyId(final @NotNull String currencyId) {
        final String normalized = Objects.requireNonNull(currencyId, "currencyId").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("currencyId cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeItemKey(final @NotNull String itemKey) {
        final String normalized = Objects.requireNonNull(itemKey, "itemKey").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("itemKey cannot be blank");
        }
        return normalized;
    }
}
