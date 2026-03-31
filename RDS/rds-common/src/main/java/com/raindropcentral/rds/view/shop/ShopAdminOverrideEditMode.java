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

package com.raindropcentral.rds.view.shop;

import org.jetbrains.annotations.NotNull;

/**
 * Input-edit modes for admin player/group override anvil workflows.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum ShopAdminOverrideEditMode {
    PLAYER_MAX_SHOPS,
    PLAYER_DISCOUNT,
    GROUP_NAME,
    GROUP_MAX_SHOPS,
    GROUP_DISCOUNT;

    /**
     * Resolves an edit mode from raw user/interface data.
     *
     * @param rawValue serialized mode value
     * @return matching mode, or {@link #GROUP_NAME} when unknown
     */
    public static @NotNull ShopAdminOverrideEditMode fromRaw(
            final @NotNull String rawValue
    ) {
        for (final ShopAdminOverrideEditMode value : values()) {
            if (value.name().equalsIgnoreCase(rawValue)) {
                return value;
            }
        }
        return GROUP_NAME;
    }
}
