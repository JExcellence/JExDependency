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
