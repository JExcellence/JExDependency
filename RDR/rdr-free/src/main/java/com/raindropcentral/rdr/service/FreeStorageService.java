package com.raindropcentral.rdr.service;

import com.raindropcentral.rdr.configs.ConfigSection;
import org.jetbrains.annotations.NotNull;

/**
 * Free-edition storage service restrictions for RDR.
 *
 * <p>The free edition caps player storage ownership at three total storages and disallows storage
 * configuration edits.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class FreeStorageService implements StorageService {

    private static final int MAXIMUM_STORAGES = 3;
    /**
     * Returns the effective free-edition player storage cap.
     *
     * @param config active RDR configuration
     * @return configured storage cap limited to three total storages
     * @throws NullPointerException if {@code config} is {@code null}
     */
    @Override
    public int getMaximumStorages(final @NotNull ConfigSection config) {
        return Math.min(config.getMaxStorages(), MAXIMUM_STORAGES);
    }

    /**
     * Returns whether players may edit storage settings in the free edition.
     *
     * @return {@code false} because free players cannot change storage settings
     */
    @Override
    public boolean canChangeStorageSettings() {
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
