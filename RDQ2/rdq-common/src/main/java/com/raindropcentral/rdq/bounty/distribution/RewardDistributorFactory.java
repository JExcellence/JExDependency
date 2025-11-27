package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.type.DistributionMode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Factory for creating RewardDistributor instances based on distribution mode.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class RewardDistributorFactory {
    
    private RewardDistributorFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Creates a RewardDistributor for the specified distribution mode.
     *
     * @param mode the distribution mode
     * @return the appropriate reward distributor
     * @throws NullPointerException if mode is null
     */
    public static @NotNull RewardDistributor create(@NotNull DistributionMode mode) {
        Objects.requireNonNull(mode, "mode cannot be null");
        
        return switch (mode) {
            case INSTANT -> new InstantRewardDistributor();
            case VIRTUAL -> new VirtualRewardDistributor();
            case DROP -> new DropRewardDistributor();
            case CHEST -> new ChestRewardDistributor();
        };
    }
}
