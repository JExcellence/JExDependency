package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.bounty.BountyService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Free version of bounty service with in-memory storage.
 * <p>
 * This implementation provides limited functionality suitable for the free version:
 * <ul>
 * <li>In-memory CRUD operations (data is lost on restart)</li>
 * <li>Limited bounties per player</li>
 * <li>Limited reward items</li>
 * <li>Async operations using completed futures</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class FreeBountyService implements BountyService {

    private static final int MAX_BOUNTIES_FREE = 1;
    private static final int MAX_REWARD_ITEMS_FREE = 5;

    private final Map<Long, RBounty> bounties = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(int page, int pageSize) {
        List<RBounty> bountyList = new ArrayList<>(bounties.values());
        int start = (page - 1) * pageSize;
        if (start >= bountyList.size()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        int end = Math.min(start + pageSize, bountyList.size());
        return CompletableFuture.completedFuture(Collections.unmodifiableList(bountyList.subList(start, end)));
    }

    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(@NotNull UUID playerUuid) {
        Optional<RBounty> foundBounty = bounties.values().stream()
                .filter(bounty -> bounty.getPlayer().getUniqueId().equals(playerUuid))
                .findFirst();
        return CompletableFuture.completedFuture(foundBounty);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> createBounty(
            @NotNull RDQPlayer target,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    ) {
        if (!canCreateBounty(commissioner)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Bounty limit reached for this player."));
        }
        if (getMaxRewardItems() != -1 && rewardItems.size() > getMaxRewardItems()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Reward item limit of " + getMaxRewardItems() + " exceeded."));
        }

        RBounty bounty = new RBounty(target, commissioner, rewardItems, rewardCurrencies);
        long newId = idGenerator.incrementAndGet();

        // Simulate JPA setting the ID on the entity after creation.
        try {
            Field idField = RBounty.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(bounty, newId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to set bounty ID via reflection", e));
        }

        bounties.put(newId, bounty);
        return CompletableFuture.completedFuture(bounty);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId) {
        RBounty removed = bounties.remove(bountyId);
        return CompletableFuture.completedFuture(removed != null);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(@NotNull RBounty bounty) {
        if (bounty.getId() == null || !bounties.containsKey(bounty.getId())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bounty with ID " + bounty.getId() + " not found for update."));
        }
        bounties.put(bounty.getId(), bounty);
        return CompletableFuture.completedFuture(bounty);
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    @Override
    public int getMaxBountiesPerPlayer() {
        return MAX_BOUNTIES_FREE;
    }

    @Override
    public int getMaxRewardItems() {
        return MAX_REWARD_ITEMS_FREE;
    }

    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        if (getMaxBountiesPerPlayer() == -1) {
            return true;
        }
        long count = bounties.values().stream()
                .filter(bounty -> bounty.getCommissioner().equals(player.getUniqueId()))
                .count();
        return count < getMaxBountiesPerPlayer();
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return CompletableFuture.completedFuture(bounties.size());
    }
}