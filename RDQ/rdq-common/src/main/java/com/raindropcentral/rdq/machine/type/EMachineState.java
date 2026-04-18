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
 * Enumeration defining the operational states a machine can be in.
 *
 * <p>Machine states control the behavior and functionality of machines:
 * <ul>
 *     <li>{@link #INACTIVE} - Machine is off and not processing</li>
 *     <li>{@link #ACTIVE} - Machine is on and actively processing</li>
 *     <li>{@link #PAUSED} - Machine is temporarily paused (e.g., waiting for resources)</li>
 *     <li>{@link #ERROR} - Machine encountered an error and stopped</li>
 *     <li>{@link #DISABLED} - Machine is globally disabled by administrators</li>
 * </ul>
 *
 * <p>State transitions:
 * <pre>
 * INACTIVE ←→ ACTIVE
 *     ↓         ↓
 *  DISABLED   PAUSED
 *               ↓
 *            ERROR
 * </pre>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public enum EMachineState {
    
    /**
     * Machine is turned off and not processing any operations.
     *
     * <p>In this state:
     * <ul>
     *     <li>No fuel is consumed</li>
     *     <li>No crafting operations occur</li>
     *     <li>Machine can be configured and modified</li>
     *     <li>Can be toggled to ACTIVE by owner or trusted players</li>
     * </ul>
     */
    INACTIVE("inactive", "Inactive", Material.GRAY_DYE, "§7", false, true),
    
    /**
     * Machine is turned on and actively processing operations.
     *
     * <p>In this state:
     * <ul>
     *     <li>Fuel is consumed per operation</li>
     *     <li>Crafting cycles execute automatically</li>
     *     <li>Recipe configuration is locked</li>
     *     <li>Can be toggled to INACTIVE by owner or trusted players</li>
     * </ul>
     */
    ACTIVE("active", "Active", Material.LIME_DYE, "§a", true, true),
    
    /**
     * Machine is temporarily paused due to missing resources or conditions.
     *
     * <p>In this state:
     * <ul>
     *     <li>No fuel is consumed</li>
     *     <li>Machine waits for required resources</li>
     *     <li>Automatically resumes when conditions are met</li>
     *     <li>Can be manually toggled to INACTIVE</li>
     * </ul>
     *
     * <p>Common pause reasons:
     * <ul>
     *     <li>Insufficient fuel</li>
     *     <li>Missing crafting materials</li>
     *     <li>Output storage full</li>
     * </ul>
     */
    PAUSED("paused", "Paused", Material.YELLOW_DYE, "§e", false, true),
    
    /**
     * Machine encountered an error and stopped operation.
     *
     * <p>In this state:
     * <ul>
     *     <li>No operations can occur</li>
     *     <li>Requires manual intervention to resolve</li>
     *     <li>Error details are logged and displayed</li>
     *     <li>Must be reset to INACTIVE before restarting</li>
     * </ul>
     *
     * <p>Common error causes:
     * <ul>
     *     <li>Invalid recipe configuration</li>
     *     <li>Corrupted machine data</li>
     *     <li>Multi-block structure broken</li>
     * </ul>
     */
    ERROR("error", "Error", Material.ORANGE_DYE, "§6", false, true),
    
    /**
     * Machine is globally disabled by server administrators.
     *
     * <p>In this state:
     * <ul>
     *     <li>No operations can occur</li>
     *     <li>Players cannot interact with the machine</li>
     *     <li>Machine type is disabled server-wide</li>
     *     <li>Only administrators can re-enable</li>
     * </ul>
     */
    DISABLED("disabled", "Disabled", Material.BARRIER, "§c", false, false);
    
    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final String colorCode;
    private final boolean isProcessing;
    private final boolean isInteractable;
    
    /**
     * Constructs a machine state with specified properties.
     *
     * @param identifier      unique string identifier for the state
     * @param displayName     human-readable display name
     * @param iconMaterial    material to use as icon indicator
     * @param colorCode       color code for text formatting
     * @param isProcessing    whether the machine processes operations in this state
     * @param isInteractable  whether players can interact with the machine in this state
     */
    EMachineState(
        final @NotNull String identifier,
        final @NotNull String displayName,
        final @NotNull Material iconMaterial,
        final @NotNull String colorCode,
        final boolean isProcessing,
        final boolean isInteractable
    ) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.colorCode = colorCode;
        this.isProcessing = isProcessing;
        this.isInteractable = isInteractable;
    }
    
    /**
     * Gets the unique identifier for this state.
     *
     * @return the state identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }
    
    /**
     * Gets the display name for this state.
     *
     * @return the human-readable state name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Gets the material to use as an icon for this state.
     *
     * @return the icon material
     */
    public @NotNull Material getIconMaterial() {
        return this.iconMaterial;
    }
    
    /**
     * Gets the color code for this state.
     *
     * @return the color code string (e.g., "§a")
     */
    public @NotNull String getColorCode() {
        return this.colorCode;
    }
    
    /**
     * Checks if the machine processes operations in this state.
     *
     * @return true if the machine is actively processing, false otherwise
     */
    public boolean isProcessing() {
        return this.isProcessing;
    }
    
    /**
     * Checks if players can interact with the machine in this state.
     *
     * @return true if the machine is interactable, false otherwise
     */
    public boolean isInteractable() {
        return this.isInteractable;
    }
    
    /**
     * Gets the localization key for this state's display name.
     *
     * @return the i18n key for the state name
     */
    public @NotNull String getDisplayNameKey() {
        return "machine.state." + this.identifier + ".name";
    }
    
    /**
     * Gets the localization key for this state's description.
     *
     * @return the i18n key for the state description
     */
    public @NotNull String getDescriptionKey() {
        return "machine.state." + this.identifier + ".description";
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
     * Finds a machine state by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the matching state, or null if not found
     */
    public static EMachineState fromIdentifier(final @NotNull String identifier) {
        for (final EMachineState state : values()) {
            if (state.getIdentifier().equalsIgnoreCase(identifier)) {
                return state;
            }
        }
        return null;
    }
}
