package com.raindropcentral.rdq.player;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.shared.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPlayerDataService")
class DefaultPlayerDataServiceTest {

    @Mock
    private PlayerRepository repository;

    @Mock
    private CacheManager cacheManager;

    private DefaultPlayerDataService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPlayerDataService(repository, cacheManager);
    }

    @Nested
    @DisplayName("getOrCreatePlayer()")
    class GetOrCreatePlayerTests {

        @Test
        @DisplayName("should return cached player when available")
        void shouldReturnCachedPlayerWhenAvailable() {
            var playerId = UUID.randomUUID();
            var playerName = "CachedPlayer";
            var cachedPlayer = RDQPlayer.create(playerId, playerName);

            when(cacheManager.getPlayer(playerId)).thenReturn(cachedPlayer);

            var result = service.getOrCreatePlayer(playerId, playerName).join();

            assertEquals(cachedPlayer, result);
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("should update name when cached player has different name")
        void shouldUpdateNameWhenCachedPlayerHasDifferentName() {
            var playerId = UUID.randomUUID();
            var oldName = "OldName";
            var newName = "NewName";
            var cachedPlayer = RDQPlayer.create(playerId, oldName);

            when(cacheManager.getPlayer(playerId)).thenReturn(cachedPlayer);
            when(repository.save(any())).thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

            var result = service.getOrCreatePlayer(playerId, newName).join();

            assertEquals(newName, result.name());
            verify(repository).save(any());
            verify(cacheManager).putPlayer(any());
        }

        @Test
        @DisplayName("should load from repository when not cached")
        void shouldLoadFromRepositoryWhenNotCached() {
            var playerId = UUID.randomUUID();
            var playerName = "ExistingPlayer";
            var existingPlayer = RDQPlayer.create(playerId, playerName);

            when(cacheManager.getPlayer(playerId)).thenReturn(null);
            when(repository.findById(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(existingPlayer)));
            when(repository.save(any())).thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

            var result = service.getOrCreatePlayer(playerId, playerName).join();

            assertEquals(existingPlayer.id(), result.id());
            verify(cacheManager).putPlayer(any());
        }

        @Test
        @DisplayName("should create new player when not found")
        void shouldCreateNewPlayerWhenNotFound() {
            var playerId = UUID.randomUUID();
            var playerName = "NewPlayer";

            when(cacheManager.getPlayer(playerId)).thenReturn(null);
            when(repository.findById(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
            when(repository.save(any())).thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

            var result = service.getOrCreatePlayer(playerId, playerName).join();

            assertEquals(playerId, result.id());
            assertEquals(playerName, result.name());
            verify(repository).save(any());
            verify(cacheManager).putPlayer(any());
        }

        @Test
        @DisplayName("should throw on null playerId")
        void shouldThrowOnNullPlayerId() {
            assertThrows(NullPointerException.class, () -> service.getOrCreatePlayer(null, "Test"));
        }

        @Test
        @DisplayName("should throw on null playerName")
        void shouldThrowOnNullPlayerName() {
            assertThrows(NullPointerException.class, () -> service.getOrCreatePlayer(UUID.randomUUID(), null));
        }
    }

    @Nested
    @DisplayName("getPlayer()")
    class GetPlayerTests {

        @Test
        @DisplayName("should return cached player when available")
        void shouldReturnCachedPlayerWhenAvailable() {
            var playerId = UUID.randomUUID();
            var cachedPlayer = RDQPlayer.create(playerId, "CachedPlayer");

            when(cacheManager.getPlayer(playerId)).thenReturn(cachedPlayer);

            var result = service.getPlayer(playerId).join();

            assertTrue(result.isPresent());
            assertEquals(cachedPlayer, result.get());
            verify(repository, never()).findById(any());
        }

        @Test
        @DisplayName("should load from repository when not cached")
        void shouldLoadFromRepositoryWhenNotCached() {
            var playerId = UUID.randomUUID();
            var existingPlayer = RDQPlayer.create(playerId, "ExistingPlayer");

            when(cacheManager.getPlayer(playerId)).thenReturn(null);
            when(repository.findById(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(existingPlayer)));

            var result = service.getPlayer(playerId).join();

            assertTrue(result.isPresent());
            assertEquals(existingPlayer, result.get());
            verify(cacheManager).putPlayer(existingPlayer);
        }

        @Test
        @DisplayName("should return empty when player not found")
        void shouldReturnEmptyWhenPlayerNotFound() {
            var playerId = UUID.randomUUID();

            when(cacheManager.getPlayer(playerId)).thenReturn(null);
            when(repository.findById(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

            var result = service.getPlayer(playerId).join();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("savePlayer()")
    class SavePlayerTests {

        @Test
        @DisplayName("should save player and update cache")
        void shouldSavePlayerAndUpdateCache() {
            var player = RDQPlayer.create(UUID.randomUUID(), "TestPlayer");

            when(repository.save(player)).thenReturn(CompletableFuture.completedFuture(player));

            service.savePlayer(player).join();

            verify(repository).save(player);
            verify(cacheManager).putPlayer(player);
        }

        @Test
        @DisplayName("should throw on null player")
        void shouldThrowOnNullPlayer() {
            assertThrows(NullPointerException.class, () -> service.savePlayer(null));
        }
    }

    @Nested
    @DisplayName("cleanupPlayerCache()")
    class CleanupPlayerCacheTests {

        @Test
        @DisplayName("should cleanup cache for player")
        void shouldCleanupCacheForPlayer() {
            var playerId = UUID.randomUUID();

            service.cleanupPlayerCache(playerId);

            verify(cacheManager).cleanupPlayer(playerId);
        }

        @Test
        @DisplayName("should throw on null playerId")
        void shouldThrowOnNullPlayerId() {
            assertThrows(NullPointerException.class, () -> service.cleanupPlayerCache(null));
        }
    }

    @Nested
    @DisplayName("playerExists()")
    class PlayerExistsTests {

        @Test
        @DisplayName("should return true when player is cached")
        void shouldReturnTrueWhenPlayerIsCached() {
            var playerId = UUID.randomUUID();
            var cachedPlayer = RDQPlayer.create(playerId, "CachedPlayer");

            when(cacheManager.getPlayer(playerId)).thenReturn(cachedPlayer);

            var result = service.playerExists(playerId).join();

            assertTrue(result);
            verify(repository, never()).exists(any());
        }

        @Test
        @DisplayName("should check repository when not cached")
        void shouldCheckRepositoryWhenNotCached() {
            var playerId = UUID.randomUUID();

            when(cacheManager.getPlayer(playerId)).thenReturn(null);
            when(repository.exists(playerId)).thenReturn(CompletableFuture.completedFuture(true));

            var result = service.playerExists(playerId).join();

            assertTrue(result);
            verify(repository).exists(playerId);
        }
    }
}
