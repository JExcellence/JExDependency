package de.jexcellence.economy.database.entity;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTest {

        @Test
        void constructorWithExplicitValuesInitializesFields() {
                final UUID uniqueId = UUID.randomUUID();
                final String playerName = "TestPlayer";

                final User user = new User(uniqueId, playerName);

                assertEquals(uniqueId, user.getUniqueId(),
                        "Constructor should retain the provided unique identifier");
                assertEquals(playerName, user.getPlayerName(),
                        "Constructor should retain the provided player name");
        }

        @Test
        void constructorWithPlayerCopiesBukkitInformation() {
                final UUID uniqueId = UUID.randomUUID();
                final String playerName = "BukkitUser";
                final Player player = mock(Player.class);
                when(player.getUniqueId()).thenReturn(uniqueId);
                when(player.getName()).thenReturn(playerName);

                final User user = new User(player);

                assertEquals(uniqueId, user.getUniqueId(),
                        "Constructor should copy the player's unique identifier");
                assertEquals(playerName, user.getPlayerName(),
                        "Constructor should copy the player's current name");
        }

        @Test
        void constructorRejectsNullUniqueIdentifier() {
                assertThrows(IllegalArgumentException.class, () -> new User(null, "Name"),
                        "Constructor should reject a null UUID");
        }

        @Test
        void constructorRejectsNullPlayerName() {
                assertThrows(IllegalArgumentException.class, () -> new User(UUID.randomUUID(), null),
                        "Constructor should reject a null player name");
        }

        @Test
        void constructorRejectsBlankPlayerName() {
                assertThrows(IllegalArgumentException.class, () -> new User(UUID.randomUUID(), "  "),
                        "Constructor should reject a blank player name");
        }

        @Test
        void constructorRejectsNullPlayer() {
                assertThrows(IllegalArgumentException.class, () -> new User((Player) null),
                        "Constructor should reject a null player reference");
        }

        @Test
        void constructorRejectsPlayerWithoutIdentifier() {
                final Player player = mock(Player.class);
                when(player.getUniqueId()).thenReturn(null);
                when(player.getName()).thenReturn("ValidName");

                assertThrows(IllegalArgumentException.class, () -> new User(player),
                        "Constructor should reject a player without a UUID");
        }

        @Test
        void constructorRejectsPlayerWithoutName() {
                final Player player = mock(Player.class);
                when(player.getUniqueId()).thenReturn(UUID.randomUUID());
                when(player.getName()).thenReturn(null);

                assertThrows(IllegalArgumentException.class, () -> new User(player),
                        "Constructor should reject a player without a name");
        }

        @Test
        void constructorRejectsPlayerWithBlankName() {
                final Player player = mock(Player.class);
                when(player.getUniqueId()).thenReturn(UUID.randomUUID());
                when(player.getName()).thenReturn(" \t\n ");

                assertThrows(IllegalArgumentException.class, () -> new User(player),
                        "Constructor should reject a player whose name is blank");
        }

        @Test
        void setPlayerNameUpdatesValue() {
                final User user = new User(UUID.randomUUID(), "Initial");
                user.setPlayerName("UpdatedName");

                assertEquals("UpdatedName", user.getPlayerName(),
                        "Setter should update the stored player name");
        }

        @Test
        void setPlayerNameRejectsNull() {
                final User user = new User(UUID.randomUUID(), "Initial");

                assertThrows(IllegalArgumentException.class, () -> user.setPlayerName(null),
                        "Setter should reject null names");
        }

        @Test
        void setPlayerNameRejectsBlank() {
                final User user = new User(UUID.randomUUID(), "Initial");

                assertThrows(IllegalArgumentException.class, () -> user.setPlayerName("   "),
                        "Setter should reject blank names");
        }

        @Test
        void equalsAndHashCodeDependOnUniqueIdentifier() {
                final UUID sharedId = UUID.randomUUID();
                final User left = new User(sharedId, "First");
                final User right = new User(sharedId, "Second");
                final User different = new User(UUID.randomUUID(), "Other");

                assertEquals(left, right, "Users with the same UUID should be equal");
                assertEquals(left.hashCode(), right.hashCode(),
                        "Users with the same UUID should share a hash code");
                assertNotEquals(left, different, "Users with different UUIDs should not be equal");
                assertNotEquals(left, new Object(), "User should not equal arbitrary objects");
                assertNotEquals(left, null, "User should not equal null");
        }

        @Test
        void toStringIncludesIdentifierAndPlayerName() {
                final UUID uniqueId = UUID.randomUUID();
                final String playerName = "DescriptiveName";
                final User user = new User(uniqueId, playerName);

                final String description = user.toString();

                assertTrue(description.contains(uniqueId.toString()),
                        "toString should include the UUID for debugging clarity");
                assertTrue(description.contains(playerName),
                        "toString should include the player name for debugging clarity");
        }

        @Test
        void defaultConstructorLeavesFieldsUninitializedForJpa() throws Exception {
                final Constructor<User> constructor = User.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                final User user = constructor.newInstance();

                final Field uniqueIdField = User.class.getDeclaredField("uniqueId");
                uniqueIdField.setAccessible(true);
                final Field playerNameField = User.class.getDeclaredField("playerName");
                playerNameField.setAccessible(true);

                assertNull(uniqueIdField.get(user),
                        "JPA constructor should not initialize the unique identifier");
                assertNull(playerNameField.get(user),
                        "JPA constructor should not initialize the player name");
        }
}

