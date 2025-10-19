package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Premium version bounty manager with full database integration.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class PremiumBountyManager implements BountyManager {

    private static final int MAX_BOUNTIES_PREMIUM = -1;
    private static final int MAX_REWARD_ITEMS_PREMIUM = -1;

    private final RBountyRepository bountyRepository;
    private final RDQPlayerRepository playerRepository;

    /**
     * Creates a premium bounty manager backed by the provided repositories.
     *
     * @param bountyRepository repository responsible for CRUD operations on {@link RBounty} instances
     * @param playerRepository repository providing access to {@link RDQPlayer} records
     */
    public PremiumBountyManager(
            final @NotNull RBountyRepository bountyRepository,
            final @NotNull RDQPlayerRepository playerRepository
    ) {
        this.bountyRepository = bountyRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Retrieves a paginated list of bounties.
     *
     * @param page     page index to fetch
     * @param pageSize number of entries per page
     * @return future resolving to a list of {@link RBounty} results for the requested page
     */
    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(final int page, final int pageSize) {
        return this.bountyRepository.findAllAsync(page, pageSize);
    }

    /**
     * Attempts to locate a bounty associated with the provided player identifier.
     *
     * @param playerUuid unique identifier of the player to search for
     * @return future resolving to an {@link Optional} containing the bounty when present
     */
    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(final @NotNull UUID playerUuid) {
        return this.bountyRepository
                .findByAttributesAsync(Map.of("player.uniqueId", playerUuid))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Creates a new bounty targeting the provided player with the supplied rewards.
     *
     * @param target            targeted RDQ player
     * @param commissioner      player creating the bounty
     * @param rewardItems       reward items associated with the bounty
     * @param rewardCurrencies  currency rewards applied to the bounty
     * @return future resolving to the persisted {@link RBounty}
     */
    @Override
    public @NotNull CompletableFuture<RBounty> createBounty(
            final @NotNull RDQPlayer target,
            final @NotNull Player commissioner,
            final @NotNull Set<RewardItem> rewardItems,
            final @NotNull Map<String, Double> rewardCurrencies
    ) {
        final RBounty bounty = new RBounty();
        bounty.setPlayer(target);
        bounty.setCommissioner(commissioner.getUniqueId());
        bounty.setRewardItems(rewardItems);

        return this.bountyRepository.createAsync(bounty);
    }

    /**
     * Removes a bounty with the given identifier.
     *
     * @param bountyId identifier of the bounty to delete
     * @return future resolving to {@code true} when deletion succeeds, {@code false} otherwise
     */
    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(final @NotNull Long bountyId) {
        return this.bountyRepository.deleteAsync(bountyId);
    }

    /**
     * Updates an existing bounty entry.
     *
     * @param bounty bounty instance containing the updated state
     * @return future resolving to the persisted {@link RBounty}
     */
    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(final @NotNull RBounty bounty) {
        return this.bountyRepository.updateAsync(bounty);
    }

    /**
     * Obtains the premium limit for active bounties on a single player.
     *
     * @return {@code -1} to indicate an unlimited number of bounties
     */
    @Override
    public int getMaxBountiesPerPlayer() {
        return MAX_BOUNTIES_PREMIUM;
    }

    /**
     * Obtains the premium limit for reward items on a bounty.
     *
     * @return {@code -1} to indicate an unlimited number of reward items
     */
    @Override
    public int getMaxRewardItems() {
        return MAX_REWARD_ITEMS_PREMIUM;
    }

    /**
     * Determines if a player may create a bounty in the premium edition.
     *
     * @param player player attempting to create a bounty
     * @return {@code true} because premium players can always create bounties
     */
    @Override
    public boolean canCreateBounty(final @NotNull Player player) {
        return true;
    }

    /**
     * Counts the total number of bounties present in the system.
     *
     * @return future resolving to the number of stored bounties
     */
    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return this.bountyRepository
                .findAllAsync(1, Integer.MAX_VALUE)
                .thenApply(List::size);
    }
}