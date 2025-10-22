package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BountyPersistenceTest {

    @Test
    void deleteAsyncDelegatesToDeleteByIdAsync() {
        long expectedId = 42L;
        RBounty bounty = Mockito.mock(RBounty.class);
        Mockito.when(bounty.getId()).thenReturn(expectedId);

        CompletableFuture<Void> deletionFuture = new CompletableFuture<>();
        AtomicLong capturedId = new AtomicLong(-1L);

        BountyPersistence persistence = new BountyPersistence() {
            @Override
            public CompletableFuture<RBounty> createAsync(RBounty bounty) {
                throw new UnsupportedOperationException("Not required for this test");
            }

            @Override
            public CompletableFuture<Void> deleteByIdAsync(long bountyId) {
                capturedId.set(bountyId);
                return deletionFuture;
            }
        };

        CompletableFuture<Void> result = persistence.deleteAsync(bounty);

        assertEquals(expectedId, capturedId.get(), "deleteAsync should forward the bounty identifier");
        assertSame(deletionFuture, result, "deleteAsync should return the same future instance");
    }
}
