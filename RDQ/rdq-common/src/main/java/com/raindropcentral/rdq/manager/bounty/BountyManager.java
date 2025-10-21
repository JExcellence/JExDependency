package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interface defining the contract for managing all bounty-related operations.
 * <p>
 * Implementations are responsible for creating, tracking, and resolving bounties,
 * including reward distribution, damage tracking, and updating player displays.
 * </p>
 */
public interface BountyManager {

    /**
     * Creates a new bounty for the specified target player.
     *
     * @param targetPlayer     the player who is the target of the bounty
     * @param commissioner     the player who places the bounty
     * @param rewardItems      the set of item rewards for the bounty
     * @param rewardCurrencies the map of currency rewards for the bounty
     */
    void createBounty(
            @NotNull RDQPlayer targetPlayer,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    );

    /**
     * Removes an active bounty and its associated tracking data.
     *
     * @param targetUniqueId the UUID of the player whose bounty should be removed
     */
    void removeBounty(@NotNull UUID targetUniqueId);

    /**
     * Tracks damage dealt by an attacker to a bounty target.
     *
     * @param targetUniqueId   the UUID of the bounty target
     * @param attackerUniqueId the UUID of the attacker
     * @param damage           the amount of damage dealt
     */
    void trackDamage(@NotNull UUID targetUniqueId, @NotNull UUID attackerUniqueId, double damage);

    /**
     * Handles the event when a player with an active bounty is killed.
     *
     * @param killedPlayer the player who was killed
     */
    void handleBountyKill(@NotNull Player killedPlayer);

    /**
     * Adds item rewards to an existing bounty.
     *
     * @param bounty the bounty to add rewards to
     * @param items  the list of item stacks to add as rewards
     * @return the updated bounty
     */
    @NotNull RBounty addItemRewards(@NotNull RBounty bounty, @NotNull List<ItemStack> items);

    /**
     * Adds a currency reward to an existing bounty.
     *
     * @param bounty       the bounty to add the currency reward to
     * @param currencyName the name of the currency
     * @param amount       the amount to add
     * @return the updated bounty
     */
    @NotNull RBounty addCurrencyReward(@NotNull RBounty bounty, @NotNull String currencyName, double amount);

    /**
     * Updates the display name of a player to indicate bounty status.
     *
     * @param playerUniqueId the UUID of the player to update
     */
    void updateBountyPlayerDisplay(@NotNull UUID playerUniqueId);

    /**
     * Checks if a player currently has an active bounty.
     *
     * @param playerUniqueId the UUID of the player to check
     * @return true if the player has an active bounty, false otherwise
     */
    boolean hasActiveBounty(@NotNull UUID playerUniqueId);

    /**
     * Retrieves the active bounty for a player, if any.
     *
     * @param playerUniqueId the UUID of the player
     * @return the RBounty instance, or null if none exists
     */
    @Nullable RBounty getBounty(@NotNull UUID playerUniqueId);

    /**
     * Gives a set of reward items to a player, handling inventory overflow.
     *
     * @param player      the player to receive the items
     * @param rewardItems the set of RewardItem to give
     */
    void giveRewardItemsToPlayer(@NotNull Player player, @NotNull Set<RewardItem> rewardItems);
}