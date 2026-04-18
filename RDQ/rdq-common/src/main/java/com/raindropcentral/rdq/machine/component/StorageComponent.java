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

package com.raindropcentral.rdq.machine.component;

import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.database.entity.machine.MachineStorage;
import com.raindropcentral.rdq.machine.type.EStorageType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Component responsible for managing machine virtual storage.
 *
 * <p>This component handles item deposits, withdrawals, and storage queries.
 * It manages unlimited virtual storage capacity and tracks items by type
 * and storage category (INPUT, OUTPUT, FUEL).
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class StorageComponent {

    private final Machine machine;

    /**
     * Constructs a new Storage component.
     *
     * @param machine the machine entity this component manages
     */
    public StorageComponent(final @NotNull Machine machine) {
        this.machine = machine;
    }

    /**
     * Deposits items into the machine storage.
     *
     * <p>If an entry for the item type and storage type already exists,
     * the quantity is added to the existing entry. Otherwise, a new
     * storage entry is created.
     *
     * @param item        the ItemStack to deposit
     * @param storageType the type of storage to deposit into
     * @return true if deposit was successful, false otherwise
     */
    public boolean deposit(
        final @NotNull ItemStack item,
        final @NotNull EStorageType storageType
    ) {
        if (item.getType() == Material.AIR || item.getAmount() <= 0) {
            return false;
        }

        final String itemData = serializeItem(item);
        final Optional<MachineStorage> existingStorage = findStorage(itemData, storageType);

        if (existingStorage.isPresent()) {
            // Add to existing storage
            existingStorage.get().addQuantity(item.getAmount());
        } else {
            // Create new storage entry
            final MachineStorage newStorage = new MachineStorage(
                machine,
                itemData,
                item.getAmount(),
                storageType
            );
            machine.addStorage(newStorage);
        }

        return true;
    }

    /**
     * Withdraws items from the machine storage.
     *
     * <p>This method attempts to withdraw the specified amount of the given
     * material from the specified storage type. If insufficient quantity exists,
     * the withdrawal fails and returns null.
     *
     * @param material    the material type to withdraw
     * @param amount      the amount to withdraw
     * @param storageType the type of storage to withdraw from
     * @return the withdrawn ItemStack, or null if insufficient quantity
     */
    @Nullable
    public ItemStack withdraw(
        final @NotNull Material material,
        final int amount,
        final @NotNull EStorageType storageType
    ) {
        if (amount <= 0) {
            return null;
        }

        // Find storage entry for this material
        final Optional<MachineStorage> storage = machine.getStorage().stream()
            .filter(s -> s.getStorageType() == storageType)
            .filter(s -> deserializeItemType(s.getItemData()) == material)
            .findFirst();

        if (storage.isEmpty()) {
            return null;
        }

        final MachineStorage storageEntry = storage.get();
        if (storageEntry.getQuantity() < amount) {
            return null;
        }

        // Remove quantity
        storageEntry.removeQuantity(amount);

        // Remove entry if empty
        if (storageEntry.isEmpty()) {
            machine.removeStorage(storageEntry);
        }

        return new ItemStack(material, amount);
    }

    /**
     * Gets the contents of a specific storage type.
     *
     * <p>Returns a map of materials to their quantities for the specified
     * storage type.
     *
     * @param storageType the type of storage to query
     * @return map of materials to quantities
     */
    @NotNull
    public Map<Material, Integer> getContents(final @NotNull EStorageType storageType) {
        final Map<Material, Integer> contents = new HashMap<>();

        for (final MachineStorage storage : machine.getStorage()) {
            if (storage.getStorageType() == storageType) {
                final Material material = deserializeItemType(storage.getItemData());
                contents.put(material, storage.getQuantity());
            }
        }

        return contents;
    }

    /**
     * Gets all storage contents across all storage types.
     *
     * @return map of materials to quantities
     */
    @NotNull
    public Map<Material, Integer> getAllContents() {
        final Map<Material, Integer> contents = new HashMap<>();

        for (final MachineStorage storage : machine.getStorage()) {
            final Material material = deserializeItemType(storage.getItemData());
            contents.merge(material, storage.getQuantity(), Integer::sum);
        }

        return contents;
    }

    /**
     * Gets the quantity of a specific material in a storage type.
     *
     * @param material    the material to check
     * @param storageType the storage type to check
     * @return the quantity available, or 0 if none
     */
    public int getQuantity(
        final @NotNull Material material,
        final @NotNull EStorageType storageType
    ) {
        return machine.getStorage().stream()
            .filter(s -> s.getStorageType() == storageType)
            .filter(s -> deserializeItemType(s.getItemData()) == material)
            .mapToInt(MachineStorage::getQuantity)
            .sum();
    }

    /**
     * Checks if the storage contains at least the specified amount of a material.
     *
     * @param material    the material to check
     * @param amount      the required amount
     * @param storageType the storage type to check
     * @return true if sufficient quantity exists, false otherwise
     */
    public boolean hasAmount(
        final @NotNull Material material,
        final int amount,
        final @NotNull EStorageType storageType
    ) {
        return getQuantity(material, storageType) >= amount;
    }

    /**
     * Checks if the storage contains the required ingredients.
     *
     * <p>This method checks if all ingredients in the provided array exist
     * in the INPUT storage with sufficient quantities.
     *
     * @param ingredients the required ingredients
     * @return true if all ingredients are available, false otherwise
     */
    public boolean hasIngredients(final @NotNull ItemStack[] ingredients) {
        final Map<Material, Integer> required = new HashMap<>();

        // Count required materials
        for (final ItemStack item : ingredients) {
            if (item != null && item.getType() != Material.AIR) {
                required.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        // Check if all required materials are available
        for (final Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (!hasAmount(entry.getKey(), entry.getValue(), EStorageType.INPUT)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Consumes ingredients from INPUT storage.
     *
     * <p>This method removes the specified ingredients from INPUT storage.
     * It should only be called after verifying ingredients are available
     * using {@link #hasIngredients(ItemStack[])}.
     *
     * @param ingredients the ingredients to consume
     * @return true if consumption was successful, false otherwise
     */
    public boolean consumeIngredients(final @NotNull ItemStack[] ingredients) {
        final Map<Material, Integer> toConsume = new HashMap<>();

        // Count materials to consume
        for (final ItemStack item : ingredients) {
            if (item != null && item.getType() != Material.AIR) {
                toConsume.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }

        // Consume each material
        for (final Map.Entry<Material, Integer> entry : toConsume.entrySet()) {
            final ItemStack withdrawn = withdraw(entry.getKey(), entry.getValue(), EStorageType.INPUT);
            if (withdrawn == null) {
                // This shouldn't happen if hasIngredients was called first
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the total storage capacity used.
     *
     * <p>Since storage is unlimited, this returns the total number of items
     * stored across all storage types.
     *
     * @return the total number of items stored
     */
    public int getTotalStorageUsed() {
        return machine.getStorage().stream()
            .mapToInt(MachineStorage::getQuantity)
            .sum();
    }

    /**
     * Clears all storage of a specific type.
     *
     * @param storageType the storage type to clear
     * @return list of ItemStacks that were cleared
     */
    @NotNull
    public List<ItemStack> clearStorage(final @NotNull EStorageType storageType) {
        final List<ItemStack> cleared = new ArrayList<>();
        final List<MachineStorage> toRemove = new ArrayList<>();

        for (final MachineStorage storage : machine.getStorage()) {
            if (storage.getStorageType() == storageType) {
                final Material material = deserializeItemType(storage.getItemData());
                cleared.add(new ItemStack(material, storage.getQuantity()));
                toRemove.add(storage);
            }
        }

        toRemove.forEach(machine::removeStorage);
        return cleared;
    }

    /**
     * Finds a storage entry matching the item data and storage type.
     *
     * @param itemData    the serialized item data
     * @param storageType the storage type
     * @return optional containing the storage entry if found
     */
    @NotNull
    private Optional<MachineStorage> findStorage(
        final @NotNull String itemData,
        final @NotNull EStorageType storageType
    ) {
        return machine.getStorage().stream()
            .filter(s -> s.getStorageType() == storageType)
            .filter(s -> s.getItemData().equals(itemData))
            .findFirst();
    }

    /**
     * Serializes an ItemStack to a string representation.
     *
     * <p>This is a simplified serialization that stores only the material type.
     * In production, this should use a proper serialization library to handle
     * metadata, enchantments, etc.
     *
     * @param item the ItemStack to serialize
     * @return the serialized string
     */
    @NotNull
    private String serializeItem(final @NotNull ItemStack item) {
        return item.getType().name();
    }

    /**
     * Deserializes item data to extract the material type.
     *
     * @param itemData the serialized item data
     * @return the material type
     */
    @NotNull
    private Material deserializeItemType(final @NotNull String itemData) {
        try {
            return Material.valueOf(itemData);
        } catch (final IllegalArgumentException e) {
            return Material.AIR;
        }
    }
}
