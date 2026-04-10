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
import com.raindropcentral.rplatform.database.converter.ItemStackSlotMapConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted full-state snapshot of one RDR storage at one rollback point in time.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(name = "rdr_rollback_storage_snapshot")
public class RRollbackStorageSnapshot extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RRollbackStorageSnapshot.class);
    private static final TypeReference<Map<UUID, StorageTrustStatus>> TRUSTED_PLAYERS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Double>> TAX_DEBT_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private RRollbackSnapshot snapshot;

    @Column(name = "original_storage_id")
    private Long originalStorageId;

    @Column(name = "storage_key", nullable = false, length = 64)
    private String storageKey;

    @Column(name = "inventory_size", nullable = false)
    private int inventorySize;

    @Column(name = "hotkey")
    private Integer hotkey;

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "inventory", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> inventory = new HashMap<>();

    @Column(name = "trusted_players", nullable = false, columnDefinition = "LONGTEXT")
    private String trustedPlayersJson = "{}";

    @Column(name = "tax_debt", nullable = false, columnDefinition = "LONGTEXT")
    private String taxDebtJson = "{}";

    /**
     * Creates one storage snapshot for the supplied rollback parent.
     *
     * @param snapshot owning rollback snapshot
     * @param originalStorageId original live storage identifier, or {@code null} when unavailable
     * @param storageKey logical storage key
     * @param inventorySize saved storage size
     * @param inventory sparse slot-indexed contents
     * @param hotkey saved hotkey, or {@code null} when none was assigned
     * @param trustedPlayers trusted-player map saved with the snapshot
     * @param taxDebtEntries tax-debt map saved with the snapshot
     */
    public RRollbackStorageSnapshot(
        final @NotNull RRollbackSnapshot snapshot,
        final @Nullable Long originalStorageId,
        final @NotNull String storageKey,
        final int inventorySize,
        final @NotNull Map<Integer, ItemStack> inventory,
        final @Nullable Integer hotkey,
        final @NotNull Map<UUID, StorageTrustStatus> trustedPlayers,
        final @NotNull Map<String, Double> taxDebtEntries
    ) {
        this.originalStorageId = originalStorageId;
        this.storageKey = normalizeStorageKey(storageKey);
        this.inventorySize = validateInventorySize(inventorySize);
        this.hotkey = hotkey;
        this.setInventory(inventory);
        this.setTrustedPlayers(trustedPlayers);
        this.setTaxDebtEntries(taxDebtEntries);
        Objects.requireNonNull(snapshot, "snapshot cannot be null").addStorageSnapshot(this);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RRollbackStorageSnapshot() {}

    /**
     * Returns the owning rollback snapshot.
     *
     * @return owning rollback snapshot, or {@code null} when detached in memory
     */
    public @Nullable RRollbackSnapshot getSnapshot() {
        return this.snapshot;
    }

    /**
     * Returns the original live storage identifier.
     *
     * @return live storage identifier, or {@code null} when unavailable
     */
    public @Nullable Long getOriginalStorageId() {
        return this.originalStorageId;
    }

    /**
     * Returns the saved logical storage key.
     *
     * @return normalized storage key
     */
    public @NotNull String getStorageKey() {
        return this.storageKey;
    }

    /**
     * Returns the saved storage size.
     *
     * @return saved inventory size
     */
    public int getInventorySize() {
        return this.inventorySize;
    }

    /**
     * Returns the saved hotkey.
     *
     * @return saved hotkey, or {@code null} when none was assigned
     */
    public @Nullable Integer getHotkey() {
        return this.hotkey;
    }

    /**
     * Returns a defensive copy of the saved inventory contents.
     *
     * @return sparse slot-indexed inventory snapshot
     */
    public @NotNull Map<Integer, ItemStack> getInventory() {
        return Map.copyOf(cloneInventory(this.inventory, this.inventorySize));
    }

    /**
     * Replaces the saved inventory contents.
     *
     * @param inventory replacement sparse slot-indexed contents
     */
    public void setInventory(final @NotNull Map<Integer, ItemStack> inventory) {
        Objects.requireNonNull(inventory, "inventory cannot be null");
        this.inventory = cloneInventory(inventory, this.inventorySize);
    }

    /**
     * Returns a defensive copy of the saved trusted-player map.
     *
     * @return trusted-player status map keyed by player UUID
     */
    public @NotNull Map<UUID, StorageTrustStatus> getTrustedPlayers() {
        return new HashMap<>(parseTrustedPlayers(this.trustedPlayersJson));
    }

    /**
     * Replaces the saved trusted-player map.
     *
     * @param trustedPlayers replacement trusted-player map
     */
    public void setTrustedPlayers(final @NotNull Map<UUID, StorageTrustStatus> trustedPlayers) {
        final Map<UUID, StorageTrustStatus> safeTrustedPlayers = new LinkedHashMap<>();
        for (final Map.Entry<UUID, StorageTrustStatus> entry : Objects.requireNonNull(
            trustedPlayers,
            "trustedPlayers cannot be null"
        ).entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() == StorageTrustStatus.PUBLIC) {
                continue;
            }
            safeTrustedPlayers.put(entry.getKey(), entry.getValue());
        }

        try {
            this.trustedPlayersJson = OBJECT_MAPPER.writeValueAsString(safeTrustedPlayers);
        } catch (final IOException exception) {
            LOGGER.error("Failed to serialize rollback trusted-player snapshot", exception);
            throw new RuntimeException("Failed to serialize rollback trusted-player snapshot", exception);
        }
    }

    /**
     * Returns a defensive copy of the saved tax-debt map.
     *
     * @return saved tax-debt entries keyed by currency identifier
     */
    public @NotNull Map<String, Double> getTaxDebtEntries() {
        return new HashMap<>(parseTaxDebtEntries(this.taxDebtJson));
    }

    /**
     * Replaces the saved tax-debt map.
     *
     * @param taxDebtEntries replacement tax-debt entries
     */
    public void setTaxDebtEntries(final @NotNull Map<String, Double> taxDebtEntries) {
        final Map<String, Double> safeTaxDebt = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> entry : Objects.requireNonNull(
            taxDebtEntries,
            "taxDebtEntries cannot be null"
        ).entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            safeTaxDebt.put(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT), entry.getValue());
        }

        try {
            this.taxDebtJson = OBJECT_MAPPER.writeValueAsString(safeTaxDebt);
        } catch (final IOException exception) {
            LOGGER.error("Failed to serialize rollback tax-debt snapshot", exception);
            throw new RuntimeException("Failed to serialize rollback tax-debt snapshot", exception);
        }
    }

    /**
     * Applies this full saved storage state to one live storage entity.
     *
     * @param storage live storage entity to overwrite
     */
    public void applyTo(final @NotNull RStorage storage) {
        final RStorage validatedStorage = Objects.requireNonNull(storage, "storage cannot be null");
        validatedStorage.setInventory(Map.of());
        validatedStorage.setInventorySize(this.inventorySize);
        validatedStorage.setInventory(this.getInventory());
        validatedStorage.setTrustedPlayers(this.getTrustedPlayers());
        validatedStorage.setTaxDebtEntries(this.getTaxDebtEntries());
        if (this.hotkey == null) {
            validatedStorage.clearHotkey();
        } else {
            validatedStorage.setHotkey(this.hotkey);
        }
        validatedStorage.clearLease();
    }

    void setSnapshotInternal(final @Nullable RRollbackSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    private static @NotNull String normalizeStorageKey(final @NotNull String storageKey) {
        final String normalizedStorageKey = Objects.requireNonNull(storageKey, "storageKey cannot be null").trim();
        if (normalizedStorageKey.isEmpty()) {
            throw new IllegalArgumentException("storageKey cannot be blank");
        }
        return normalizedStorageKey;
    }

    private static int validateInventorySize(final int inventorySize) {
        if (inventorySize < 9 || inventorySize > 54 || inventorySize % 9 != 0) {
            throw new IllegalArgumentException("inventorySize must be a chest size between 9 and 54");
        }
        return inventorySize;
    }

    private static @NotNull Map<Integer, ItemStack> cloneInventory(
        final @NotNull Map<Integer, ItemStack> inventory,
        final int inventorySize
    ) {
        final Map<Integer, ItemStack> copiedInventory = new HashMap<>();
        for (final Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null || slot < 0 || slot >= inventorySize || itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            copiedInventory.put(slot, itemStack.clone());
        }
        return copiedInventory;
    }

    private static @NotNull Map<UUID, StorageTrustStatus> parseTrustedPlayers(final @NotNull String json) {
        try {
            final Map<UUID, StorageTrustStatus> parsed = OBJECT_MAPPER.readValue(json, TRUSTED_PLAYERS_TYPE);
            return parsed == null ? new HashMap<>() : parsed;
        } catch (final IOException exception) {
            LOGGER.error("Failed to parse rollback trusted-player snapshot", exception);
            return new HashMap<>();
        }
    }

    private static @NotNull Map<String, Double> parseTaxDebtEntries(final @NotNull String json) {
        try {
            final Map<String, Double> parsed = OBJECT_MAPPER.readValue(json, TAX_DEBT_TYPE);
            return parsed == null ? new HashMap<>() : parsed;
        } catch (final IOException exception) {
            LOGGER.error("Failed to parse rollback tax-debt snapshot", exception);
            return new HashMap<>();
        }
    }
}
