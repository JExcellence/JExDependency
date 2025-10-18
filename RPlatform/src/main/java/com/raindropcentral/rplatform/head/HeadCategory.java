package com.raindropcentral.rplatform.head;

/**
 * Defines the categories used to group custom heads across the platform's user interfaces.
 * Each category determines the context in which a head is displayed or retrieved.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum HeadCategory {

    /**
     * Decorative heads primarily surfaced for aesthetic placement in the world.
     */
    DECORATION,

    /**
     * Heads designed for inventory interactions such as menus and quick access slots.
     */
    INVENTORY,

    /**
     * Heads representing players, often shown in leaderboards or profile displays.
     */
    PLAYER
}
