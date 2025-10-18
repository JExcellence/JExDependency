package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;

import java.util.concurrent.CompletableFuture;

public interface BountyPersistence {
    CompletableFuture<RBounty> createAsync(RBounty bounty);
    CompletableFuture<Void> deleteByIdAsync(long bountyId);
    default CompletableFuture<Void> deleteAsync(RBounty bounty) {
        return deleteByIdAsync(bounty.getId());
    }
}