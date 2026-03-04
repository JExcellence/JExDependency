package com.raindropcentral.rds.service;

import com.raindropcentral.rds.configs.ConfigSection;
import org.jetbrains.annotations.NotNull;

/**
 * Free-edition shop service restrictions for RDS.
 *
 * <p>The free edition caps player shop purchases at three total shops, limits the server to two
 * admin shops, and disallows plugin-wide config changes.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class FreeShopService implements ShopService {

    private static final int MAXIMUM_SHOPS = 3;
    private static final int MAXIMUM_ADMIN_SHOPS = 2;

    /**
     * Returns the effective free-edition player shop cap.
     *
     * @param config active RDS configuration
     * @return configured shop cap limited to three purchases
     * @throws NullPointerException if {@code config} is {@code null}
     */
    @Override
    public int getMaximumShops(final @NotNull ConfigSection config) {
        final int configuredMaximum = config.getMaxShops();
        return configuredMaximum <= 0 ? MAXIMUM_SHOPS : Math.min(configuredMaximum, MAXIMUM_SHOPS);
    }

    /**
     * Returns the free-edition server admin shop cap.
     *
     * @return {@code 2} because free servers are limited to two admin shops
     */
    @Override
    public int getMaximumAdminShops() {
        return MAXIMUM_ADMIN_SHOPS;
    }

    /**
     * Returns whether plugin-wide config changes are allowed in the free edition.
     *
     * @return {@code false} because free cannot change plugin config values
     */
    @Override
    public boolean canChangeConfigs() {
        return false;
    }

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code false} because this service represents the free edition
     */
    @Override
    public boolean isPremium() {
        return false;
    }
}
