package com.raindropcentral.rds.service;

import com.raindropcentral.rds.configs.ConfigSection;
import org.jetbrains.annotations.NotNull;

/**
 * Premium-edition shop service behaviour for RDS.
 *
 * <p>The premium edition preserves the configured shop limits, allows unlimited admin shops, and
 * permits plugin-wide config changes.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class PremiumShopService implements ShopService {

    /**
     * Returns the configured premium-edition player shop cap.
     *
     * @param config active RDS configuration
     * @return configured player shop cap
     * @throws NullPointerException if {@code config} is {@code null}
     */
    @Override
    public int getMaximumShops(final @NotNull ConfigSection config) {
        return config.getMaxShops();
    }

    /**
     * Returns the premium-edition server admin shop cap.
     *
     * @return {@code -1} because premium admin shops are unlimited
     */
    @Override
    public int getMaximumAdminShops() {
        return -1;
    }

    /**
     * Returns whether plugin-wide config changes are allowed in the premium edition.
     *
     * @return {@code true} because premium may change plugin config values
     */
    @Override
    public boolean canChangeConfigs() {
        return true;
    }

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} because this service represents the premium edition
     */
    @Override
    public boolean isPremium() {
        return true;
    }
}
