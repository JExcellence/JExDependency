package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.type.EDistributionMode;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating reward distributors based on distribution mode.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class RewardDistributorFactory {

    /**
     * Creates a reward distributor for the specified distribution mode.
     *
     * @param mode the distribution mode
     * @return the appropriate reward distributor
     */
    @NotNull
    public static RewardDistributor create(@NotNull EDistributionMode mode) {
        return switch (mode) {
            case INSTANT -> new InstantRewardDistributor();
            case DROP -> new DropRewardDistributor();
            case CHEST -> new ChestRewardDistributor();
            case VIRTUAL -> new VirtualRewardDistributor();
        };
    }
}
