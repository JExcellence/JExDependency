package com.raindropcentral.rdt.service;

/**
 * Free-edition behavior restrictions for RDT.
 *
 * <p>The free edition always uses bundled config defaults and does not permit runtime
 * configuration overrides.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class FreeTownService implements TownService {

    /**
     * Returns whether free editions allow runtime config changes.
     *
     * @return {@code false} because free editions lock config edits
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
