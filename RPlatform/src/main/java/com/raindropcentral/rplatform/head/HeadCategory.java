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
