package com.raindropcentral.rdq.service.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.service.BountyService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Premium version of bounty service with full database integration.
 * <p>
 * This implementation provides complete functionality:
 * <ul>
 * <li>Full CRUD operations on bounties</li>
 * <li>Unlimited bounties per player</li>
 * <li>Unlimited reward items</li>
 * <li>Database persistence</li>
 * <li>Async operations</li>
 * </ul>
 * </p>
 * <p>
 * {@link com.raindropcentral.rdq.RDQPremiumImpl} registers the service after stage&nbsp;3 repository
 * wiring completes. All asynchronous methods rely on the executor initialised during stage&nbsp;1 of
 * the enable pipeline (virtual threads with a fixed-pool fallback) and expect callers to respect the
 * {@link com.raindropcentral.rdq.RDQ#runSync(Runnable)} boundary when handing results back to Bukkit.
 * The service consumes the same repository contracts documented for the free edition so cross-module
 * contributors can align behaviour while evolving premium features.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class PremiumBountyService implements BountyService {

    private static final int MAX_BOUNTIES_PREMIUM = -1;
    private static final int MAX_REWARD_ITEMS_PREMIUM = -1;

    private final RBountyRepository bountyRepository;
    private final RDQPlayerRepository playerRepository;

    /**
     * Creates the premium bounty service backed by the configured repositories.
     *
     * @param bountyRepository repository handling persistence for bounty entities
     * @param playerRepository repository resolving {@link RDQPlayer} metadata required when creating bounties
     */
    public PremiumBountyService(
            final @NotNull RBountyRepository bountyRepository,
            final @NotNull RDQPlayerRepository playerRepository
    ) {
        this.bountyRepository = bountyRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Retrieves a paginated list of all active bounties.
     *
     * @param page     the page to retrieve, starting at {@code 1}
     * @param pageSize the number of results to include per page
     * @return future resolving to the page of bounties supplied by the repository
     */
    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(final int page, final int pageSize) {
        return this.bountyRepository.findAllAsync(page, pageSize);
    }

    /**
     * Loads the bounty associated with the provided player identifier, if one exists.
     *
     * @param playerUuid unique identifier of the player that may have an active bounty
     * @return future resolving to an optional bounty for the player
     */
    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(final @NotNull UUID playerUuid) {
        return this.bountyRepository
                .findByAttributesAsync(Map.of("player.uniqueId", playerUuid))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Creates and persists a new bounty for the supplied target and commissioner.
     *
     * @param target            the player receiving the bounty
     * @param commissioner      the player placing the bounty
     * @param rewardItems       the reward items offered for the bounty
     * @param rewardCurrencies  the reward currencies offered for the bounty
     * @return future resolving to the newly persisted bounty instance
     */
    @Override
    public @NotNull CompletableFuture<RBounty> createBounty(
            final @NotNull RDQPlayer target,
            final @NotNull Player commissioner,
            final @NotNull Set<RewardItem> rewardItems,
            final @NotNull Map<String, Double> rewardCurrencies
    ) {
        final RBounty bounty = new RBounty(target, commissioner);
        bounty.setRewardItems(rewardItems);

        return this.bountyRepository.createAsync(bounty);
    }

    /**
     * Deletes the bounty referenced by the provided identifier.
     *
     * @param bountyId the identifier of the bounty to remove
     * @return future resolving to {@code true} when the bounty is deleted, {@code false} otherwise
     */
    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(final @NotNull Long bountyId) {
        return this.bountyRepository.deleteAsync(bountyId);
    }

    /**
     * Updates the supplied bounty with the repository.
     *
     * @param bounty the bounty entity to update
     * @return future resolving to the updated bounty
     */
    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(final @NotNull RBounty bounty) {
        return this.bountyRepository.updateAsync(bounty);
    }

    /**
     * Indicates that this service instance represents the premium implementation.
     *
     * @return {@code true} because premium features are always enabled for this service
     */
    @Override
    public boolean isPremium() {
        return true;
    }

    /**
     * Reports the maximum number of bounties a player may have in the premium edition.
     *
     * @return {@code -1} to denote no enforced limit
     */
    @Override
    public int getMaxBountiesPerPlayer() {
        return MAX_BOUNTIES_PREMIUM;
    }

    /**
     * Reports the maximum number of reward items allowed per bounty in the premium edition.
     *
     * @return {@code -1} to denote no enforced limit
     */
    @Override
    public int getMaxRewardItems() {
        return MAX_REWARD_ITEMS_PREMIUM;
    }

    /**
     * Determines whether the provided player can create a bounty.
     *
     * @param player the player attempting to create a bounty
     * @return {@code true} because the premium edition does not restrict bounty creation
     */
    @Override
    public boolean canCreateBounty(final @NotNull Player player) {
        return true;
    }

    /**
     * Counts the total number of bounties stored in the repository.
     *
     * @return future resolving to the number of tracked bounties
     */
    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return this.bountyRepository
                .findAllAsync(1, Integer.MAX_VALUE)
                .thenApply(List::size);
    }
}