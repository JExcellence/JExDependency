package com.raindropcentral.rdq.player;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RDQPlayer")
class RDQPlayerTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create player with current timestamp")
        void shouldCreatePlayerWithCurrentTimestamp() {
            var id = UUID.randomUUID();
            var name = "TestPlayer";
            var before = Instant.now();

            var player = RDQPlayer.create(id, name);

            var after = Instant.now();

            assertEquals(id, player.id());
            assertEquals(name, player.name());
            assertNotNull(player.firstJoin());
            assertNotNull(player.lastSeen());
            assertTrue(player.firstJoin().compareTo(before) >= 0);
            assertTrue(player.firstJoin().compareTo(after) <= 0);
            assertEquals(player.firstJoin(), player.lastSeen());
        }

        @Test
        @DisplayName("should throw on null id")
        void shouldThrowOnNullId() {
            assertThrows(NullPointerException.class, () -> RDQPlayer.create(null, "Test"));
        }

        @Test
        @DisplayName("should throw on null name")
        void shouldThrowOnNullName() {
            assertThrows(NullPointerException.class, () -> RDQPlayer.create(UUID.randomUUID(), null));
        }
    }

    @Nested
    @DisplayName("updateName()")
    class UpdateNameTests {

        @Test
        @DisplayName("should update player name")
        void shouldUpdatePlayerName() {
            var player = RDQPlayer.create(UUID.randomUUID(), "OldName");

            player.updateName("NewName");

            assertEquals("NewName", player.name());
        }

        @Test
        @DisplayName("should throw on null name")
        void shouldThrowOnNullName() {
            var player = RDQPlayer.create(UUID.randomUUID(), "Test");

            assertThrows(NullPointerException.class, () -> player.updateName(null));
        }
    }

    @Nested
    @DisplayName("updateLastSeen()")
    class UpdateLastSeenTests {

        @Test
        @DisplayName("should update last seen timestamp")
        void shouldUpdateLastSeenTimestamp() throws InterruptedException {
            var player = RDQPlayer.create(UUID.randomUUID(), "Test");
            var originalLastSeen = player.lastSeen();

            Thread.sleep(10);
            player.updateLastSeen();

            assertTrue(player.lastSeen().isAfter(originalLastSeen));
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("should be equal when ids match")
        void shouldBeEqualWhenIdsMatch() {
            var id = UUID.randomUUID();
            var player1 = RDQPlayer.create(id, "Player1");
            var player2 = new RDQPlayer(id, "Player2", Instant.now(), Instant.now());

            assertEquals(player1, player2);
            assertEquals(player1.hashCode(), player2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when ids differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            var player1 = RDQPlayer.create(UUID.randomUUID(), "Player1");
            var player2 = RDQPlayer.create(UUID.randomUUID(), "Player1");

            assertNotEquals(player1, player2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            var player = RDQPlayer.create(UUID.randomUUID(), "Test");

            assertNotEquals(null, player);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            var player = RDQPlayer.create(UUID.randomUUID(), "Test");

            assertNotEquals("string", player);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should contain id and name")
        void shouldContainIdAndName() {
            var id = UUID.randomUUID();
            var name = "TestPlayer";
            var player = RDQPlayer.create(id, name);

            var result = player.toString();

            assertTrue(result.contains(id.toString()));
            assertTrue(result.contains(name));
        }
    }
}
