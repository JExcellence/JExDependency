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

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents shop ledger entry.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "shop_ledger_entries")
@SuppressWarnings({
        "FieldCanBeLocal",
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the ShopLedgerEntry API type.
 */
public class ShopLedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private ShopLedgerType entry_type;

    @Column(name = "currency_type", nullable = false)
    private String currency_type;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "actor_name", nullable = false)
    private String actor_name;

    @Column(name = "actor_uuid")
    @Convert(converter = UUIDConverter.class)
    private UUID actor_uuid;

    @Column(name = "item_type")
    private String item_type;

    @Column(name = "item_entry_id")
    @Convert(converter = UUIDConverter.class)
    private UUID item_entry_id;

    @Column(name = "item_amount")
    private Integer item_amount;

    @Column(name = "counted_shops")
    private Integer counted_shops;

    protected ShopLedgerEntry() {
    }

    /**
     * Creates a new shop ledger entry.
     *
     * @param shop target shop
     * @param entryType entry type
     * @param currencyType currency type
     * @param amount amount
     * @param actorId actor id
     * @param actorName actor name
     * @param itemType item type
     * @param itemEntryId item entry id
     * @param itemAmount item amount
     * @param countedShops counted shops
     */
    public ShopLedgerEntry(
            final @NotNull Shop shop,
            final @NotNull ShopLedgerType entryType,
            final @NotNull String currencyType,
            final double amount,
            final @Nullable UUID actorId,
            final @NotNull String actorName,
            final @Nullable String itemType,
            final @Nullable UUID itemEntryId,
            final @Nullable Integer itemAmount,
            final @Nullable Integer countedShops
    ) {
        this.shop = shop;
        this.entry_type = entryType;
        this.currency_type = currencyType.trim();
        this.amount = Math.max(0D, amount);
        this.actor_uuid = actorId;
        this.actor_name = actorName.isBlank()
                ? (actorId == null ? "Unknown" : actorId.toString())
                : actorName;
        this.item_type = itemType;
        this.item_entry_id = itemEntryId;
        this.item_amount = itemAmount;
        this.counted_shops = countedShops;
    }

    /**
     * Creates a purchase ledger entry without an item entry id (legacy compatibility).
     *
     * @param shop target shop
     * @param actorId actor id
     * @param actorName actor name
     * @param currencyType currency type
     * @param amount total paid amount
     * @param itemType purchased item type label
     * @param itemAmount purchased amount
     * @return created purchase ledger entry
     */
    public static @NotNull ShopLedgerEntry purchase(
            final @NotNull Shop shop,
            final @Nullable UUID actorId,
            final @NotNull String actorName,
            final @NotNull String currencyType,
            final double amount,
            final @NotNull String itemType,
            final int itemAmount
    ) {
        return purchase(shop, actorId, actorName, currencyType, amount, itemType, itemAmount, null);
    }

    /**
     * Creates a purchase ledger entry.
     *
     * @param shop target shop
     * @param actorId actor id
     * @param actorName actor name
     * @param currencyType currency type
     * @param amount total paid amount
     * @param itemType purchased item type label
     * @param itemAmount purchased amount
     * @param itemEntryId purchased item entry id
     * @return created purchase ledger entry
     */
    public static @NotNull ShopLedgerEntry purchase(
            final @NotNull Shop shop,
            final @Nullable UUID actorId,
            final @NotNull String actorName,
            final @NotNull String currencyType,
            final double amount,
            final @NotNull String itemType,
            final int itemAmount,
            final @Nullable UUID itemEntryId
    ) {
        return new ShopLedgerEntry(
                shop,
                ShopLedgerType.PURCHASE,
                currencyType,
                amount,
                actorId,
                actorName,
                itemType,
                itemEntryId,
                Math.max(1, itemAmount),
                null
        );
    }

    /**
     * Executes taxation.
     */
    public static @NotNull ShopLedgerEntry taxation(
            final @NotNull Shop shop,
            final @Nullable UUID actorId,
            final @NotNull String actorName,
            final @NotNull String currencyType,
            final double amount,
            final int countedShops
    ) {
        return new ShopLedgerEntry(
                shop,
                ShopLedgerType.TAXATION,
                currencyType,
                amount,
                actorId,
                actorName,
                null,
                null,
                null,
                Math.max(1, countedShops)
        );
    }

    /**
     * Returns the shop.
     *
     * @return the shop
     */
    public @NotNull Shop getShop() {
        return this.shop;
    }

    /**
     * Sets shop.
     */
    public void setShop(
            final @NotNull Shop shop
    ) {
        this.shop = shop;
    }

    /**
     * Returns the entry type.
     *
     * @return the entry type
     */
    public @NotNull ShopLedgerType getEntryType() {
        return this.entry_type;
    }

    /**
     * Returns the currency type.
     *
     * @return the currency type
     */
    public @NotNull String getCurrencyType() {
        return this.currency_type;
    }

    /**
     * Returns the amount.
     *
     * @return the amount
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * Returns the actor name.
     *
     * @return the actor name
     */
    public @NotNull String getActorName() {
        return this.actor_name;
    }

    /**
     * Returns the actor id.
     *
     * @return the actor id
     */
    public @Nullable UUID getActorId() {
        return this.actor_uuid;
    }

    /**
     * Returns the item type.
     *
     * @return the item type
     */
    public @Nullable String getItemType() {
        return this.item_type;
    }

    /**
     * Returns the item entry id.
     *
     * @return the item entry id
     */
    public @Nullable UUID getItemEntryId() {
        return this.item_entry_id;
    }

    /**
     * Returns the item amount.
     *
     * @return the item amount
     */
    public @Nullable Integer getItemAmount() {
        return this.item_amount;
    }

    /**
     * Returns the counted shops.
     *
     * @return the counted shops
     */
    public @Nullable Integer getCountedShops() {
        return this.counted_shops;
    }
}
