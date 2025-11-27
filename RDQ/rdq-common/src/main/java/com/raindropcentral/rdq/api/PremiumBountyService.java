package com.raindropcentral.rdq.api;

import com.raindropcentral.rdq.bounty.DistributionMode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Premium edition bounty service with advanced features.
 *
 * <p>Extends the base bounty service with support for multiple distribution
 * modes including CHEST, DROP, and VIRTUAL.
 *
 * @see BountyService
 * @see FreeBountyService
 * @see DistributionMode
 */
public non-sealed interface PremiumBountyService extends BountyService {

    /**
     * Gets all available distribution modes for bounty rewards.
     *
     * @return the list of enabled distribution modes
     */
    @NotNull
    List<DistributionMode> getAvailableDistributionModes();

    /**
     * Checks if a specific distribution mode is enabled.
     *
     * @param mode the distribution mode to check
     * @return true if the mode is enabled
     */
    boolean isDistributionModeEnabled(@NotNull DistributionMode mode);
}
