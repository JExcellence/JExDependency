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

package com.raindropcentral.rdq.machine.type;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration defining the types of storage classifications for machine inventories.
 *
 * <p>Storage types categorize items within a machine's virtual inventory system,
 * determining how items are used and accessed during machine operations.
 *
 * <p>Storage classifications:
 * <ul>
 *     <li>{@link #INPUT} - Materials consumed during crafting</li>
 *     <li>{@link #OUTPUT} - Items produced by the machine</li>
 *     <li>{@link #FUEL} - Energy sources consumed for operations</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public enum EStorageType {
    
    /**
     * Input storage for crafting materials and ingredients.
     *
     * <p>Characteristics:
     * <ul>
     *     <li>Accepts any item type</li>
     *     <li>Items are consumed during crafting operations</li>
     *     <li>Can be filled manually or via attached hoppers</li>
     *     <li>Unlimited virtual capacity</li>
     * </ul>
     *
     * <p>Usage:
     * <ul>
     *     <li>Machine searches INPUT storage for recipe ingredients</li>
     *     <li>Items are removed when crafting begins</li>
     *     <li>Players can deposit items through GUI or physical storage</li>
     * </ul>
     */
    INPUT(
        "input",
        "Input",
        Material.HOPPER,
        "§e",
        true,
        false
    ),
    
    /**
     * Output storage for crafted items and products.
     *
     * <p>Characteristics:
     * <ul>
     *     <li>Receives items produced by the machine</li>
     *     <li>Items can be withdrawn manually or via attached chests</li>
     *     <li>Unlimited virtual capacity</li>
     *     <li>Read-only for players (cannot deposit)</li>
     * </ul>
     *
     * <p>Usage:
     * <ul>
     *     <li>Machine adds crafted items to OUTPUT storage</li>
     *     <li>Bonus output from upgrades goes here</li>
     *     <li>Players can withdraw items through GUI or physical storage</li>
     * </ul>
     */
    OUTPUT(
        "output",
        "Output",
        Material.CHEST,
        "§a",
        false,
        true
    ),
    
    /**
     * Fuel storage for energy sources.
     *
     * <p>Characteristics:
     * <ul>
     *     <li>Accepts only valid fuel items (configured per machine type)</li>
     *     <li>Fuel is consumed per crafting operation</li>
     *     <li>Can be filled manually through GUI</li>
     *     <li>Stored as energy value rather than physical items</li>
     * </ul>
     *
     * <p>Usage:
     * <ul>
     *     <li>Machine converts fuel items to energy units</li>
     *     <li>Energy is consumed based on operation cost</li>
     *     <li>Efficiency upgrades reduce consumption</li>
     *     <li>Machine pauses when fuel depletes</li>
     * </ul>
     *
     * <p>Valid fuel types (configurable):
     * <ul>
     *     <li>COAL - 100 energy</li>
     *     <li>COAL_BLOCK - 900 energy</li>
     *     <li>LAVA_BUCKET - 2000 energy</li>
     * </ul>
     */
    FUEL(
        "fuel",
        "Fuel",
        Material.COAL,
        "§6",
        true,
        false
    );
    
    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final String colorCode;
    private final boolean acceptsDeposits;
    private final boolean allowsWithdrawals;
    
    /**
     * Constructs a storage type with specified properties.
     *
     * @param identifier         unique string identifier for the storage type
     * @param displayName        human-readable display name
     * @param iconMaterial       material to use as icon indicator
     * @param colorCode          color code for text formatting
     * @param acceptsDeposits    whether players can deposit items into this storage
     * @param allowsWithdrawals  whether players can withdraw items from this storage
     */
    EStorageType(
        final @NotNull String identifier,
        final @NotNull String displayName,
        final @NotNull Material iconMaterial,
        final @NotNull String colorCode,
        final boolean acceptsDeposits,
        final boolean allowsWithdrawals
    ) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.colorCode = colorCode;
        this.acceptsDeposits = acceptsDeposits;
        this.allowsWithdrawals = allowsWithdrawals;
    }
    
    /**
     * Gets the unique identifier for this storage type.
     *
     * @return the storage type identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }
    
    /**
     * Gets the display name for this storage type.
     *
     * @return the human-readable storage type name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Gets the material to use as an icon for this storage type.
     *
     * @return the icon material
     */
    public @NotNull Material getIconMaterial() {
        return this.iconMaterial;
    }
    
    /**
     * Gets the color code for this storage type.
     *
     * @return the color code string (e.g., "§e")
     */
    public @NotNull String getColorCode() {
        return this.colorCode;
    }
    
    /**
     * Checks if this storage type accepts player deposits.
     *
     * @return true if deposits are allowed, false otherwise
     */
    public boolean acceptsDeposits() {
        return this.acceptsDeposits;
    }
    
    /**
     * Checks if this storage type allows player withdrawals.
     *
     * @return true if withdrawals are allowed, false otherwise
     */
    public boolean allowsWithdrawals() {
        return this.allowsWithdrawals;
    }
    
    /**
     * Gets the localization key for this storage type's display name.
     *
     * @return the i18n key for the storage type name
     */
    public @NotNull String getDisplayNameKey() {
        return "machine.storage." + this.identifier + ".name";
    }
    
    /**
     * Gets the localization key for this storage type's description.
     *
     * @return the i18n key for the storage type description
     */
    public @NotNull String getDescriptionKey() {
        return "machine.storage." + this.identifier + ".description";
    }
    
    /**
     * Gets a formatted display name with color code.
     *
     * @return the colored display name
     */
    public @NotNull String getColoredDisplayName() {
        return this.colorCode + this.displayName;
    }
    
    /**
     * Finds a storage type by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the matching storage type, or null if not found
     */
    public static EStorageType fromIdentifier(final @NotNull String identifier) {
        for (final EStorageType type : values()) {
            if (type.getIdentifier().equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        return null;
    }
}
