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

package com.raindropcentral.rds.database.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.items.json.ItemParser;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.bukkit.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists a single logical shop and its chest anchor locations.
 *
 * <p>A shop may occupy one chest block or two adjacent chest blocks when upgraded to a
 * double chest. Both blocks still represent the same logical shop entity.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "shops")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the Shop API type.
 */
public class Shop extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("RDS");

    @Column(name = "owner_uuid", unique = false, nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID owner_uuid;

    @Column(name = "shop_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location shop_location;

    @Column(name = "secondary_shop_location", unique = false, nullable = true)
    @Convert(converter = LocationConverter.class)
    private Location secondary_shop_location;

    @OneToMany(
            mappedBy = "shop",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<Bank> bankEntries = new ArrayList<>();

    @Column(name = "shop_items", unique = false, nullable = false, columnDefinition = "LONGTEXT")
    private String itemsJson = "[]";

    @Column(name = "admin_shop", unique = false, nullable = false)
    private boolean admin_shop = false;

    @Column(name = "trusted_players", unique = false, nullable = false, columnDefinition = "LONGTEXT")
    private String trustedPlayersJson = "{}";

    @Column(name = "tax_debt", unique = false, nullable = false, columnDefinition = "LONGTEXT")
    private String taxDebtJson = "{}";

    @OneToMany(
            mappedBy = "shop",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("createdAt DESC, id DESC")
    private List<ShopLedgerEntry> ledgerEntries = new ArrayList<>();

    @Transient
    private List<AbstractItem> cachedItems = new ArrayList<>();

    @Transient
    private Map<UUID, ShopTrustStatus> cachedTrustedPlayers;

    @Transient
    private Map<String, Double> cachedTaxDebt;

    /**
     * Creates a new shop.
     */
    public Shop() {
    }

    /**
     * Creates a new shop.
     *
     * @param owner_uuid owner identifier for the new shop
     * @param shop_location primary chest location for the new shop
     */
    public Shop(UUID owner_uuid, Location shop_location) {
        this.owner_uuid = owner_uuid;
        this.shop_location = shop_location;
        this.secondary_shop_location = null;
        this.bankEntries = new ArrayList<>();
        this.ledgerEntries = new ArrayList<>();
        this.taxDebtJson = "{}";
        setItems(List.of());
    }

    /**
     * Returns the owner.
     *
     * @return the owner
     */
    public UUID getOwner() {
        return this.owner_uuid;
    }

    /**
     * Returns the shop location.
     *
     * @return the shop location
     */
    public Location getShopLocation() {
        return this.shop_location;
    }

    /**
     * Returns the optional second chest location used when this shop is a double chest.
     *
     * @return the second chest block location, or {@code null} when this shop is still single-wide
     */
    public @Nullable Location getSecondaryShopLocation() {
        return this.secondary_shop_location;
    }

    /**
     * Updates the optional second chest location for this shop.
     *
     * @param secondaryShopLocation the second chest block location, or {@code null} to clear it
     */
    public void setSecondaryShopLocation(
            final @Nullable Location secondaryShopLocation
    ) {
        this.secondary_shop_location = secondaryShopLocation;
    }

    /**
     * Indicates whether this shop currently spans two chest blocks.
     *
     * @return {@code true} when a secondary chest location is present
     */
    public boolean isDoubleChest() {
        return this.secondary_shop_location != null;
    }

    /**
     * Checks whether the provided block location belongs to this shop.
     *
     * @param location block location to test
     * @return {@code true} when the location matches either chest half of this shop
     */
    public boolean occupiesLocation(
            final @Nullable Location location
    ) {
        return Objects.equals(this.shop_location, location)
                || Objects.equals(this.secondary_shop_location, location);
    }

    /**
     * Returns the number of placed shop blocks represented by this shop.
     *
     * @return {@code 2} for a double chest shop, otherwise {@code 1}
     */
    public int getShopBlockCount() {
        return this.isDoubleChest() ? 2 : 1;
    }

    /**
     * Returns the bank.
     *
     * @return the bank
     */
    public double getBank() {
        return this.getBankAmount("vault");
    }

    /**
     * Adds bank.
     *
     * @param bank Vault amount to deposit
     * @return the add bank result
     */
    public double addBank(double bank) {
        return this.addBank("vault", bank);
    }
    
    /**
     * Returns the bank entries.
     *
     * @return the bank entries
     */
    public @NotNull List<Bank> getBankEntries() {
        return List.copyOf(this.bankEntries);
    }

    /**
     * Returns the bank currency count.
     *
     * @return the bank currency count
     */
    public int getBankCurrencyCount() {
        return this.bankEntries.size();
    }

    /**
     * Gets bankAmount.
     */
    public double getBankAmount(
            final @NotNull String currencyType
    ) {
        final Bank bankEntry = this.findBankEntry(currencyType);
        return bankEntry == null ? 0D : bankEntry.getAmount();
    }

    /**
     * Executes addBank.
     */
    public double addBank(
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return this.getBankAmount(currencyType);
        }

        Bank bankEntry = this.findBankEntry(currencyType);
        if (bankEntry == null) {
            bankEntry = new Bank(this, currencyType, 0D);
            this.bankEntries.add(bankEntry);
        }
        return bankEntry.deposit(amount);
    }

    /**
     * Executes withdrawBank.
     */
    public boolean withdrawBank(
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        final Bank bankEntry = this.findBankEntry(currencyType);
        if (bankEntry == null || bankEntry.getAmount() + 1.0E-6D < amount) {
            return false;
        }

        bankEntry.withdraw(amount);
        if (bankEntry.getAmount() <= 1.0E-6D) {
            this.bankEntries.remove(bankEntry);
        }

        return true;
    }

    /**
     * Returns per-currency outstanding tax debt tracked for this shop.
     *
     * @return normalized tax debt map where keys are currency ids and values are positive owed amounts
     */
    public @NotNull Map<String, Double> getTaxDebtEntries() {
        final Map<String, Double> normalized = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> entry : this.getMutableTaxDebtEntries().entrySet()) {
            final String currencyType = normalizeCurrencyType(entry.getKey());
            final double amount = entry.getValue() == null ? 0D : Math.max(0D, entry.getValue());
            if (currencyType == null || amount <= 1.0E-6D) {
                continue;
            }

            normalized.put(currencyType, amount);
        }
        return normalized;
    }

    /**
     * Returns outstanding tax debt for one currency.
     *
     * @param currencyType currency identifier
     * @return owed amount for the provided currency, or {@code 0.0} when none is tracked
     */
    public double getTaxDebtAmount(
            final @NotNull String currencyType
    ) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        if (normalizedCurrencyType == null) {
            return 0D;
        }

        return this.getTaxDebtEntries().getOrDefault(normalizedCurrencyType, 0D);
    }

    /**
     * Adds outstanding tax debt for one currency.
     *
     * @param currencyType currency identifier
     * @param amount amount to add
     * @return updated owed amount for that currency
     */
    public double addTaxDebt(
            final @NotNull String currencyType,
            final double amount
    ) {
        return this.addTaxDebt(currencyType, amount, -1D);
    }

    /**
     * Adds outstanding tax debt for one currency with an optional cap.
     *
     * <p>When {@code maximumAmount} is positive, the stored debt for {@code currencyType}
     * will not increase beyond that value. Non-positive cap values are treated as unlimited.</p>
     *
     * @param currencyType currency identifier
     * @param amount amount to add
     * @param maximumAmount maximum debt amount allowed for this currency on this shop
     * @return updated owed amount for that currency
     */
    public double addTaxDebt(
            final @NotNull String currencyType,
            final double amount,
            final double maximumAmount
    ) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        if (normalizedCurrencyType == null || amount <= 1.0E-6D) {
            return this.getTaxDebtAmount(currencyType);
        }

        final Map<String, Double> mutableTaxDebt = this.getMutableTaxDebtEntries();
        final double existingAmount = mutableTaxDebt.getOrDefault(normalizedCurrencyType, 0D);
        if (maximumAmount > 0D && existingAmount >= maximumAmount - 1.0E-6D) {
            return existingAmount;
        }

        final double updatedAmount = maximumAmount > 0D
                ? Math.min(maximumAmount, Math.max(0D, existingAmount + amount))
                : Math.max(0D, existingAmount + amount);
        mutableTaxDebt.put(normalizedCurrencyType, updatedAmount);
        this.setTaxDebtEntries(mutableTaxDebt);
        return updatedAmount;
    }

    /**
     * Reduces outstanding tax debt for one currency.
     *
     * @param currencyType currency identifier
     * @param amount amount to remove
     * @return remaining owed amount for that currency after the reduction
     */
    public double reduceTaxDebt(
            final @NotNull String currencyType,
            final double amount
    ) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        if (normalizedCurrencyType == null || amount <= 1.0E-6D) {
            return this.getTaxDebtAmount(currencyType);
        }

        final Map<String, Double> mutableTaxDebt = this.getMutableTaxDebtEntries();
        final double existingAmount = mutableTaxDebt.getOrDefault(normalizedCurrencyType, 0D);
        if (existingAmount <= 1.0E-6D) {
            return 0D;
        }

        final double remainingAmount = Math.max(0D, existingAmount - amount);
        if (remainingAmount <= 1.0E-6D) {
            mutableTaxDebt.remove(normalizedCurrencyType);
        } else {
            mutableTaxDebt.put(normalizedCurrencyType, remainingAmount);
        }

        this.setTaxDebtEntries(mutableTaxDebt);
        return remainingAmount;
    }

    /**
     * Clears all tracked tax debt for this shop.
     */
    public void clearTaxDebt() {
        this.setTaxDebtEntries(Map.of());
    }

    /**
     * Indicates whether this shop has any unpaid tax debt.
     *
     * @return {@code true} when one or more positive debt entries exist
     */
    public boolean hasTaxDebt() {
        return !this.getTaxDebtEntries().isEmpty();
    }

    /**
     * Indicates whether this shop is bankrupt due to unpaid taxes.
     *
     * @return {@code true} when unpaid tax debt exists
     */
    public boolean isBankrupt() {
        return this.hasTaxDebt();
    }

    /**
     * Forces all stored shop items to {@link ShopItem.AvailabilityMode#NEVER}.
     *
     * @return {@code true} when at least one item availability mode changed
     */
    public boolean forceItemsNeverAvailability() {
        final List<AbstractItem> currentItems = this.getItems();
        if (currentItems.isEmpty()) {
            return false;
        }

        final List<AbstractItem> updatedItems = new ArrayList<>(currentItems.size());
        boolean changed = false;
        for (final AbstractItem item : currentItems) {
            if (item instanceof ShopItem shopItem) {
                if (shopItem.getAvailabilityMode() != ShopItem.AvailabilityMode.NEVER) {
                    updatedItems.add(shopItem.withAvailabilityMode(ShopItem.AvailabilityMode.NEVER));
                    changed = true;
                } else {
                    updatedItems.add(shopItem);
                }
                continue;
            }

            updatedItems.add(item);
        }

        if (changed) {
            this.setItems(updatedItems);
        }
        return changed;
    }

    /**
     * Returns the items.
     *
     * @return the items
     */
    public List<AbstractItem> getItems() {
        if (this.cachedItems == null) {
            this.cachedItems = new ArrayList<>();
        }

        if (
                this.cachedItems.isEmpty() &&
                this.itemsJson != null &&
                !this.itemsJson.isBlank() &&
                !"[]".equals(this.itemsJson.trim())
        ) {
            try {
                this.cachedItems = ItemParser.parseList(this.itemsJson);
            } catch (Exception e) {
                LOGGER.error("Failed to parse shop items JSON", e);
                throw new RuntimeException("Failed to parse shop items", e);
            }
        }

        return new ArrayList<>(this.cachedItems);
    }

    /**
     * Updates the items.
     *
     * @param items item payloads to serialize or assign
     */
    public void setItems(final List<? extends AbstractItem> items) {
        final List<AbstractItem> safeItems = new ArrayList<>();
        if (items != null) {
            for (AbstractItem item : items) {
                if (item != null) {
                    safeItems.add(item);
                }
            }
        }

        this.cachedItems = safeItems;

        try {
            this.itemsJson = ItemParser.serializeList(safeItems);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize shop items", e);
            throw new RuntimeException("Failed to serialize shop items", e);
        }
    }

    /**
     * Indicates whether admin shop.
     *
     * @return {@code true} if admin shop; otherwise {@code false}
     */
    public boolean isAdminShop() {
        return this.admin_shop;
    }

    /**
     * Updates the admin shop.
     *
     * @param adminShop whether the shop should be treated as an admin shop
     */
    public void setAdminShop(final boolean adminShop) {
        this.admin_shop = adminShop;
    }

    /**
     * Returns the stored item count.
     *
     * @return the stored item count
     */
    public int getStoredItemCount() {
        return getItems().size();
    }

    /**
     * Returns the trusted players.
     *
     * @return the trusted players
     */
    public @NotNull Map<UUID, ShopTrustStatus> getTrustedPlayers() {
        if (this.cachedTrustedPlayers == null) {
            this.cachedTrustedPlayers = this.parseTrustedPlayers();
        }

        return new HashMap<>(this.cachedTrustedPlayers);
    }

    /**
     * Returns the ledger entries.
     *
     * @return the ledger entries
     */
    public @NotNull List<ShopLedgerEntry> getLedgerEntries() {
        if (this.ledgerEntries == null) {
            this.ledgerEntries = new ArrayList<>();
        }

        return List.copyOf(this.ledgerEntries);
    }

    /**
     * Executes addLedgerEntry.
     */
    public void addLedgerEntry(
            final @NotNull ShopLedgerEntry ledgerEntry
    ) {
        if (this.ledgerEntries == null) {
            this.ledgerEntries = new ArrayList<>();
        }

        ledgerEntry.setShop(this);
        this.ledgerEntries.add(0, ledgerEntry);
    }

    /**
     * Returns the ledger entry count.
     *
     * @return the ledger entry count
     */
    public int getLedgerEntryCount() {
        return this.ledgerEntries == null ? 0 : this.ledgerEntries.size();
    }

    /**
     * Gets ledgerEntryCount.
     */
    public int getLedgerEntryCount(
            final @NotNull ShopLedgerType ledgerType
    ) {
        if (this.ledgerEntries == null || this.ledgerEntries.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (final ShopLedgerEntry ledgerEntry : this.ledgerEntries) {
            if (ledgerEntry != null && ledgerEntry.getEntryType() == ledgerType) {
                count++;
            }
        }

        return count;
    }

    /**
     * Sets trustedPlayers.
     */
    public void setTrustedPlayers(
            final @Nullable Map<UUID, ShopTrustStatus> trustedPlayers
    ) {
        final Map<UUID, ShopTrustStatus> safeTrustedPlayers = new HashMap<>();
        if (trustedPlayers != null) {
            for (final Map.Entry<UUID, ShopTrustStatus> entry : trustedPlayers.entrySet()) {
                final UUID playerId = entry.getKey();
                final ShopTrustStatus status = entry.getValue();
                if (playerId == null || status == null || status == ShopTrustStatus.PUBLIC || this.isOwner(playerId)) {
                    continue;
                }

                safeTrustedPlayers.put(playerId, status);
            }
        }

        this.cachedTrustedPlayers = safeTrustedPlayers;

        try {
            this.trustedPlayersJson = ItemParser.getObjectMapper().writeValueAsString(safeTrustedPlayers);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize trusted players", e);
            throw new RuntimeException("Failed to serialize trusted players", e);
        }
    }

    /**
     * Gets trustStatus.
     */
    public @NotNull ShopTrustStatus getTrustStatus(
            final @NotNull UUID playerId
    ) {
        if (this.isOwner(playerId)) {
            return ShopTrustStatus.TRUSTED;
        }

        return this.getTrustedPlayers().getOrDefault(playerId, ShopTrustStatus.PUBLIC);
    }

    /**
     * Sets trustStatus.
     */
    public void setTrustStatus(
            final @NotNull UUID playerId,
            final @NotNull ShopTrustStatus status
    ) {
        if (this.isOwner(playerId)) {
            return;
        }

        final Map<UUID, ShopTrustStatus> trustedPlayers = this.getTrustedPlayers();
        if (status == ShopTrustStatus.PUBLIC) {
            trustedPlayers.remove(playerId);
        } else {
            trustedPlayers.put(playerId, status);
        }

        this.setTrustedPlayers(trustedPlayers);
    }

    /**
     * Gets trustedPlayerCount.
     */
    public int getTrustedPlayerCount(
            final @NotNull ShopTrustStatus status
    ) {
        int count = 0;
        for (final ShopTrustStatus trustedStatus : this.getTrustedPlayers().values()) {
            if (trustedStatus == status) {
                count++;
            }
        }

        return count;
    }

    /**
     * Executes canAccessOverview.
     */
    public boolean canAccessOverview(
            final @NotNull UUID playerId
    ) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId) != ShopTrustStatus.PUBLIC;
    }

    /**
     * Executes canSupply.
     */
    public boolean canSupply(
            final @NotNull UUID playerId
    ) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId).hasSupplyAccess();
    }

    /**
     * Executes canManage.
     */
    public boolean canManage(
            final @NotNull UUID playerId
    ) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId).hasFullAccess();
    }

    /**
     * Indicates whether owner.
     *
     * @param playerId player identifier to evaluate
     * @return {@code true} if owner; otherwise {@code false}
     */
    public boolean isOwner(final UUID playerId) {
        return Objects.equals(this.owner_uuid, playerId);
    }

    private @NotNull Map<UUID, ShopTrustStatus> parseTrustedPlayers() {
        if (this.trustedPlayersJson == null || this.trustedPlayersJson.isBlank()) {
            return new HashMap<>();
        }

        try {
            final Map<UUID, ShopTrustStatus> parsed = ItemParser.getObjectMapper().readValue(
                    this.trustedPlayersJson,
                    new TypeReference<Map<UUID, ShopTrustStatus>>() {
                    }
            );
            return parsed == null ? new HashMap<>() : new HashMap<>(parsed);
        } catch (Exception e) {
            LOGGER.error("Failed to parse trusted players JSON", e);
            throw new RuntimeException("Failed to parse trusted players", e);
        }
    }

    private @NotNull Map<String, Double> getMutableTaxDebtEntries() {
        if (this.cachedTaxDebt == null) {
            this.cachedTaxDebt = this.parseTaxDebtEntries();
        }

        return new LinkedHashMap<>(this.cachedTaxDebt);
    }

    private void setTaxDebtEntries(
            final @NotNull Map<String, Double> taxDebtEntries
    ) {
        final Map<String, Double> normalizedTaxDebt = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> entry : taxDebtEntries.entrySet()) {
            final String currencyType = normalizeCurrencyType(entry.getKey());
            if (currencyType == null || entry.getValue() == null) {
                continue;
            }

            final double normalizedAmount = Math.max(0D, entry.getValue());
            if (normalizedAmount <= 1.0E-6D) {
                continue;
            }

            normalizedTaxDebt.put(currencyType, normalizedAmount);
        }

        this.cachedTaxDebt = normalizedTaxDebt;
        try {
            this.taxDebtJson = ItemParser.getObjectMapper().writeValueAsString(normalizedTaxDebt);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize tax debt entries", e);
            throw new RuntimeException("Failed to serialize tax debt entries", e);
        }
    }

    private @NotNull Map<String, Double> parseTaxDebtEntries() {
        if (this.taxDebtJson == null || this.taxDebtJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            final Map<String, Double> parsed = ItemParser.getObjectMapper().readValue(
                    this.taxDebtJson,
                    new TypeReference<Map<String, Double>>() {
                    }
            );
            if (parsed == null || parsed.isEmpty()) {
                return new LinkedHashMap<>();
            }

            final Map<String, Double> normalized = new LinkedHashMap<>();
            for (final Map.Entry<String, Double> entry : parsed.entrySet()) {
                final String currencyType = normalizeCurrencyType(entry.getKey());
                if (currencyType == null || entry.getValue() == null) {
                    continue;
                }

                final double amount = Math.max(0D, entry.getValue());
                if (amount <= 1.0E-6D) {
                    continue;
                }

                normalized.put(currencyType, amount);
            }
            return normalized;
        } catch (Exception e) {
            LOGGER.error("Failed to parse tax debt JSON", e);
            throw new RuntimeException("Failed to parse tax debt JSON", e);
        }
    }

    private static @Nullable String normalizeCurrencyType(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return null;
        }

        return currencyType.trim().toLowerCase(Locale.ROOT);
    }

    private @Nullable Bank findBankEntry(
            final @NotNull String currencyType
    ) {
        if (this.bankEntries == null) {
            this.bankEntries = new ArrayList<>();
        }

        for (final Bank bankEntry : this.bankEntries) {
            if (bankEntry != null && bankEntry.matchesCurrencyType(currencyType)) {
                return bankEntry;
            }
        }

        return null;
    }
}
