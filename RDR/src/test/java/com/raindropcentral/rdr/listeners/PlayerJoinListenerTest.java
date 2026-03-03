/*
 * PlayerJoinListenerTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.listeners;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.service.scoreboard.StorageSidebarScoreboardService;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerJoinListenerTest {

    private static final Logger TEST_LOGGER = Logger.getLogger("RDR-Test");

    @Test
    void createsNewProfileWithConfiguredStartingStorages() {
        final UUID uniqueId = UUID.randomUUID();
        final TrackingPlayerProfileRepository repository = new TrackingPlayerProfileRepository();
        final ConfigSection config = this.createConfig(2);
        final Player player = this.createPlayer(uniqueId);
        final PlayerJoinEvent event = new PlayerJoinEvent(player, Component.empty());

        new PlayerJoinListener(() -> repository, () -> config, TEST_LOGGER).onPlayerJoin(event);

        final RDRPlayer createdPlayer = repository.createdPlayer;
        assertNotNull(createdPlayer);
        assertEquals(uniqueId, createdPlayer.getIdentifier());

        final List<RStorage> storages = createdPlayer.getStorages();
        assertEquals(2, storages.size());
        assertEquals("storage-1", storages.get(0).getStorageKey());
        assertEquals("storage-2", storages.get(1).getStorageKey());
        assertEquals(54, storages.get(0).getInventorySize());
    }

    @Test
    void doesNotCreateNewProfileWhenPlayerAlreadyExists() {
        final UUID uniqueId = UUID.randomUUID();
        final TrackingPlayerProfileRepository repository = new TrackingPlayerProfileRepository();
        repository.existingPlayer = new RDRPlayer(uniqueId);

        final ConfigSection config = this.createConfig(2);
        final Player player = this.createPlayer(uniqueId);
        final PlayerJoinEvent event = new PlayerJoinEvent(player, Component.empty());

        new PlayerJoinListener(() -> repository, () -> config, TEST_LOGGER).onPlayerJoin(event);

        assertNull(repository.createdPlayer);
        assertEquals(0, repository.createCalls);
    }

    @Test
    void restoresSidebarScoreboardWhenExistingProfileHasPreferenceEnabled() {
        final UUID uniqueId = UUID.randomUUID();
        final TrackingPlayerProfileRepository repository = new TrackingPlayerProfileRepository();
        final RDRPlayer existingPlayer = new RDRPlayer(uniqueId);
        existingPlayer.setSidebarScoreboardEnabled(true);
        repository.existingPlayer = existingPlayer;

        final ConfigSection config = this.createConfig(2);
        final Player player = this.createPlayer(uniqueId);
        final PlayerJoinEvent event = new PlayerJoinEvent(player, Component.empty());
        final TrackingStorageSidebarScoreboardService scoreboardService = new TrackingStorageSidebarScoreboardService();

        new PlayerJoinListener(() -> repository, () -> config, () -> scoreboardService, TEST_LOGGER).onPlayerJoin(event);

        assertSame(player, scoreboardService.enabledPlayer);
        assertEquals(0, repository.createCalls);
    }

    private ConfigSection createConfig(final int initialProvisionedStorages) {
        return new ConfigSection(new EvaluationEnvironmentBuilder()) {
            @Override
            public int getInitialProvisionedStorages() {
                return initialProvisionedStorages;
            }
        };
    }

    private Player createPlayer(final UUID uniqueId) {
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> uniqueId.hashCode();
                        case "toString" -> "PlayerProxy[" + uniqueId + "]";
                        default -> null;
                    };
                }

                return switch (method.getName()) {
                    case "getUniqueId" -> uniqueId;
                    case "getName" -> "TestPlayer";
                    default -> this.defaultValue(method.getReturnType());
                };
            }
        );
    }

    private Object defaultValue(final Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class TrackingPlayerProfileRepository implements PlayerProfileRepository {

        private RDRPlayer existingPlayer;
        private RDRPlayer createdPlayer;
        private int createCalls;

        @Override
        public RDRPlayer findByPlayer(final UUID playerUuid) {
            if (this.existingPlayer == null) {
                return null;
            }
            return this.existingPlayer.getIdentifier().equals(playerUuid) ? this.existingPlayer : null;
        }

        @Override
        public CompletableFuture<RDRPlayer> createAsync(final RDRPlayer player) {
            this.createdPlayer = player;
            this.createCalls++;
            return CompletableFuture.completedFuture(player);
        }
    }

    private static final class TrackingStorageSidebarScoreboardService extends StorageSidebarScoreboardService {

        private Player enabledPlayer;

        private TrackingStorageSidebarScoreboardService() {
            super(null);
        }

        @Override
        public void enable(final Player player) {
            this.enabledPlayer = player;
        }
    }
}
