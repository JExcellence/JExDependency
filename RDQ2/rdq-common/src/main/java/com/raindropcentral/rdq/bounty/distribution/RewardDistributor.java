package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for distributing bounty rewards to hunters.
 * <p>
 * Different implementations handle different distribution modes:
 * INSTANT, VIRTUAL, DROP, and CHEST.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public interface RewardDistributor {
    
    /**
     * Distributes the rewards from a bounty to the hunter.
     *
     * @param hunter the player receiving the rewards
     * @param bounty the bounty being claimed
     * @param location the location where the target died (may be null for some modes)
     * @return a future that completes when rewards are distributed
     */
    @NotNull CompletableFuture<Void> distributeRewards(
            @NotNull Player hunter,
            @NotNull Bounty bounty,
            @NotNull Location location
    );
}
