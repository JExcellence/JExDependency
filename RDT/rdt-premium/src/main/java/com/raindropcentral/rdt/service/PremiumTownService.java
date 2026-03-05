package com.raindropcentral.rdt.service;

/**
 * Premium-edition behavior for RDT.
 *
 * <p>The premium edition allows runtime configuration overrides loaded from the plugin data
 * folder.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class PremiumTownService implements TownService {

    /**
     * Returns whether premium editions allow runtime config changes.
     *
     * @return {@code true} because premium editions can change configs
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
