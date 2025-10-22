package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPersistenceTest {

    private static RDQPlayer newPlayer() {
        return new RDQPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"), "PlayerOne");
    }

    @Test
    void updateAsyncReturnsProvidedFuture() {
        final CompletableFuture<Void> expected = CompletableFuture.completedFuture(null);
        final PlayerPersistence persistence = player -> expected;

        final CompletableFuture<Void> actual = persistence.updateAsync(newPlayer());

        assertSame(expected, actual, "Adapters should pass through the futures they create.");
        assertTrue(actual.isDone(), "The completed future should remain in its original state.");
    }

    @Test
    void updateAsyncReturnsProvidedExceptionalFuture() {
        final CompletableFuture<Void> expected = CompletableFuture.failedFuture(new IllegalStateException("boom"));
        final PlayerPersistence persistence = player -> expected;

        final CompletableFuture<Void> actual = persistence.updateAsync(newPlayer());

        assertSame(expected, actual, "Adapters should not wrap exceptional futures.");
        assertTrue(actual.isCompletedExceptionally(), "The returned future must preserve the exceptional completion.");
    }
}
