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

package com.raindropcentral.rdq.database.entity.machine;

import com.raindropcentral.rdq.machine.type.EStorageType;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a storage entry for a machine.
 *
 * <p>This entity tracks items stored in a machine's virtual storage,
 * including the item data, quantity, and storage type (input, output, or fuel).
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(name = "rdq_machine_storage")
@Getter
@Setter
public class MachineStorage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The machine this storage entry belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    /**
     * Serialized ItemStack data.
     * Contains the item type, metadata, and other properties.
     */
    @Column(name = "item_data", nullable = false, columnDefinition = "TEXT")
    private String itemData;

    /**
     * The quantity of items stored.
     */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /**
     * The type of storage (INPUT, OUTPUT, or FUEL).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 20)
    private EStorageType storageType;

    /**
     * Timestamp when this storage entry was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected MachineStorage() {
    }

    /**
     * Constructs a new {@code MachineStorage} entry.
     *
     * @param machine     the machine this storage belongs to
     * @param itemData    the serialized item data
     * @param quantity    the quantity of items
     * @param storageType the type of storage
     */
    public MachineStorage(
        @NotNull final Machine machine,
        @NotNull final String itemData,
        final int quantity,
        @NotNull final EStorageType storageType
    ) {
        this.machine = machine;
        this.itemData = itemData;
        this.quantity = quantity;
        this.storageType = storageType;
    }

    /**
     * Adds to the quantity of items in this storage entry.
     *
     * @param amount the amount to add
     */
    public void addQuantity(final int amount) {
        this.quantity += amount;
    }

    /**
     * Removes from the quantity of items in this storage entry.
     *
     * @param amount the amount to remove
     * @return true if the removal was successful, false if insufficient quantity
     */
    public boolean removeQuantity(final int amount) {
        if (this.quantity >= amount) {
            this.quantity -= amount;
            return true;
        }
        return false;
    }

    /**
     * Checks if this storage entry is empty.
     *
     * @return true if quantity is 0 or less, false otherwise
     */
    public boolean isEmpty() {
        return quantity <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineStorage that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return machine != null && machine.equals(that.machine) &&
            itemData.equals(that.itemData) &&
            storageType == that.storageType;
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(machine, itemData, storageType);
    }

    @Override
    public String toString() {
        return "MachineStorage{" +
            "id=" + getId() +
            ", machineId=" + (machine != null ? machine.getId() : null) +
            ", quantity=" + quantity +
            ", storageType=" + storageType +
            '}';
    }
}
