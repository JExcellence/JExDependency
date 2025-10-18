package com.raindropcentral.rplatform.utility.heads;

/**
 * Enumeration of filter types for categorizing head entities in the database.
 * <p>
 * This enum is used to distinguish between different usages or categories of heads,
 * such as decorative heads, inventory heads, or player heads.
 * </p>
 *
 * <ul>
 *     <li>{@link #DECORATION} - Heads used primarily for decorative purposes.</li>
 *     <li>{@link #INVENTORY} - Heads intended for use in inventories or as items.</li>
 *     <li>{@link #PLAYER} - Heads representing player entities.</li>
 * </ul>
 *
 * @version 1.0.0
 * @since TBD
 * @author JExcellence
 */
public enum EHeadFilter {

    /**
     * Heads used primarily for decorative purposes.
     */
    DECORATION,

    /**
     * Heads intended for use in inventories or as items.
     */
    INVENTORY,

    /**
     * Heads representing player entities.
     */
    PLAYER,
}
