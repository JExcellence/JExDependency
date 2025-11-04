package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyServiceProviderTest {

    private final BountyService stubService = new StubBountyService();

    @AfterEach
    void tearDown() {
        BountyServiceProvider.reset();
    }

    @Test
    void registersAndProvidesServiceInstance() {
        assertFalse(BountyServiceProvider.isInitialized());

        BountyServiceProvider.setInstance(stubService);

        assertTrue(BountyServiceProvider.isInitialized());
        assertSame(stubService, BountyServiceProvider.getInstance());

        BountyServiceProvider.reset();
        assertFalse(BountyServiceProvider.isInitialized());
    }

    @Test
    void getInstanceThrowsWhenUninitialized() {
        final IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                BountyServiceProvider::getInstance
        );

        assertEquals("BountyService not initialized", exception.getMessage());
    }

    @Test
    void setInstanceCannotBeCalledTwice() {
        BountyServiceProvider.setInstance(stubService);

        final IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> BountyServiceProvider.setInstance(stubService)
        );

        assertEquals("BountyService already initialized", exception.getMessage());
    }

    private static final class StubBountyService implements BountyService {

        @Override
        public CompletableFuture<List<RBounty>> getAllBounties(
                final int page,
                final int pageSize
        ) {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public CompletableFuture<Optional<RBounty>> getBountyByPlayer(
                final UUID playerUuid
        ) {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public CompletableFuture<RBounty> createBounty(
                final RDQPlayer target,
                final Player commissioner,
                final Set<RewardItem> rewardItems,
                final Map<String, Double> rewardCurrencies
        ) {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public CompletableFuture<Boolean> deleteBounty(final Long bountyId) {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public CompletableFuture<RBounty> updateBounty(
                final RBounty bounty
        ) {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public boolean isPremium() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public int getMaxBountiesPerPlayer() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public int getMaxRewardItems() {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public boolean canCreateBounty(final Player player) {
            throw new UnsupportedOperationException("Stub only");
        }

        @Override
        public CompletableFuture<Integer> getTotalBountyCount() {
            throw new UnsupportedOperationException("Stub only");
        }
    }
}
