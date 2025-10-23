package com.raindropcentral.rdq.service;

import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.core.service.RecordingRCoreService;
import com.raindropcentral.core.service.RecordingRCoreService.MethodKey;
import com.raindropcentral.rcore.database.entity.RPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.rcore.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class RCoreBridgeTest {

    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void fromBukkitDiscoversServiceAndAwaitServiceRetries() {
        assertFalse(RCoreBridge.isAvailable(null));

        RecordingRCoreService service = new RecordingRCoreService();
        Bukkit.getServicesManager().register(RCoreService.class, service, plugin, ServicePriority.Normal);

        RCoreBridge bridge = RCoreBridge.fromBukkit(plugin, null);
        assertNotNull(bridge);
        assertTrue(RCoreBridge.isAvailable(null));

        Bukkit.getServicesManager().unregisterAll(plugin);
        assertFalse(RCoreBridge.isAvailable(null));

        RecordingRCoreService delayedService = new RecordingRCoreService();
        ServiceRegistry registry = new ServiceRegistry();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Bukkit.getServicesManager().register(RCoreService.class, delayedService, plugin, ServicePriority.Normal);
        });

        Optional<RCoreBridge> resolved = RCoreBridge.awaitService(registry, null, true, 5, 100).join();
        assertTrue(resolved.isPresent());
        assertSame(delayedService, resolved.get().getDelegateObject());
    }

    @Test
    void bridgeMethodsForwardArgumentsAndReturnValues() throws Exception {
        RecordingRCoreService service = new RecordingRCoreService();
        RCoreBridge bridge = instantiate(service);

        UUID uuid = UUID.randomUUID();
        OfflinePlayer offlinePlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(offlinePlayer.getUniqueId()).thenReturn(uuid);
        String name = "PlayerOne";
        String identifier = "kills";
        String pluginName = "rdq";
        RAbstractStatistic statistic = Mockito.mock(RAbstractStatistic.class);
        RPlayer player = Mockito.mock(RPlayer.class);
        Mockito.when(player.getUniqueId()).thenReturn(uuid);
        RPlayerStatistic stats = Mockito.mock(RPlayerStatistic.class);

        CompletableFuture<Optional<RPlayer>> findByUuid = CompletableFuture.completedFuture(Optional.empty());
        CompletableFuture<Optional<RPlayer>> findByOffline = CompletableFuture.completedFuture(Optional.ofNullable(null));
        CompletableFuture<Optional<RPlayer>> findByName = CompletableFuture.completedFuture(Optional.empty());
        CompletableFuture<Boolean> existsUuid = CompletableFuture.completedFuture(Boolean.TRUE);
        CompletableFuture<Boolean> existsOffline = CompletableFuture.completedFuture(Boolean.FALSE);
        CompletableFuture<Optional<RPlayer>> create = CompletableFuture.completedFuture(Optional.empty());
        CompletableFuture<Optional<RPlayer>> update = CompletableFuture.completedFuture(Optional.empty());
        CompletableFuture<Optional<RPlayerStatistic>> statsUuid = CompletableFuture.completedFuture(Optional.of(stats));
        CompletableFuture<Optional<RPlayerStatistic>> statsOffline = CompletableFuture.completedFuture(Optional.empty());
        CompletableFuture<Optional<Object>> valueUuid = CompletableFuture.completedFuture(Optional.of("value"));
        CompletableFuture<Optional<Object>> valueOffline = CompletableFuture.completedFuture(Optional.empty());
        CompletableFuture<Boolean> hasUuid = CompletableFuture.completedFuture(Boolean.TRUE);
        CompletableFuture<Boolean> hasOffline = CompletableFuture.completedFuture(Boolean.FALSE);
        CompletableFuture<Boolean> removeUuid = CompletableFuture.completedFuture(Boolean.TRUE);
        CompletableFuture<Boolean> removeOffline = CompletableFuture.completedFuture(Boolean.TRUE);
        CompletableFuture<Boolean> addUuid = CompletableFuture.completedFuture(Boolean.TRUE);
        CompletableFuture<Boolean> addOffline = CompletableFuture.completedFuture(Boolean.TRUE);
        CompletableFuture<Long> countUuid = CompletableFuture.completedFuture(5L);
        CompletableFuture<Long> countOffline = CompletableFuture.completedFuture(3L);

        service.setResponse(MethodKey.FIND_PLAYER_UUID, findByUuid);
        service.setResponse(MethodKey.FIND_PLAYER_OFFLINE, findByOffline);
        service.setResponse(MethodKey.FIND_PLAYER_BY_NAME, findByName);
        service.setResponse(MethodKey.PLAYER_EXISTS_UUID, existsUuid);
        service.setResponse(MethodKey.PLAYER_EXISTS_OFFLINE, existsOffline);
        service.setResponse(MethodKey.CREATE_PLAYER, create);
        service.setResponse(MethodKey.UPDATE_PLAYER, update);
        service.setResponse(MethodKey.FIND_STATISTICS_UUID, statsUuid);
        service.setResponse(MethodKey.FIND_STATISTICS_OFFLINE, statsOffline);
        service.setResponse(MethodKey.FIND_STAT_VALUE_UUID, valueUuid);
        service.setResponse(MethodKey.FIND_STAT_VALUE_OFFLINE, valueOffline);
        service.setResponse(MethodKey.HAS_STAT_UUID, hasUuid);
        service.setResponse(MethodKey.HAS_STAT_OFFLINE, hasOffline);
        service.setResponse(MethodKey.REMOVE_STAT_UUID, removeUuid);
        service.setResponse(MethodKey.REMOVE_STAT_OFFLINE, removeOffline);
        service.setResponse(MethodKey.ADD_OR_REPLACE_UUID, addUuid);
        service.setResponse(MethodKey.ADD_OR_REPLACE_OFFLINE, addOffline);
        service.setResponse(MethodKey.STAT_COUNT_UUID, countUuid);
        service.setResponse(MethodKey.STAT_COUNT_OFFLINE, countOffline);
        service.setApiVersion("2.5.0");

        assertSame(findByUuid, bridge.findPlayerAsync(uuid));
        assertArrayEquals(new Object[]{uuid}, service.getLastArgs(MethodKey.FIND_PLAYER_UUID));

        assertSame(findByOffline, bridge.findPlayerAsync(offlinePlayer));
        assertArrayEquals(new Object[]{offlinePlayer}, service.getLastArgs(MethodKey.FIND_PLAYER_OFFLINE));

        assertSame(findByName, bridge.findPlayerByNameAsync(name));
        assertArrayEquals(new Object[]{name}, service.getLastArgs(MethodKey.FIND_PLAYER_BY_NAME));

        assertSame(existsUuid, bridge.playerExistsAsync(uuid));
        assertArrayEquals(new Object[]{uuid}, service.getLastArgs(MethodKey.PLAYER_EXISTS_UUID));

        assertSame(existsOffline, bridge.playerExistsAsync(offlinePlayer));
        assertArrayEquals(new Object[]{offlinePlayer}, service.getLastArgs(MethodKey.PLAYER_EXISTS_OFFLINE));

        assertSame(create, bridge.createPlayerAsync(uuid, name));
        assertArrayEquals(new Object[]{uuid, name}, service.getLastArgs(MethodKey.CREATE_PLAYER));

        assertSame(update, bridge.updatePlayerAsync(player));
        assertArrayEquals(new Object[]{player}, service.getLastArgs(MethodKey.UPDATE_PLAYER));

        assertSame(statsUuid, bridge.findPlayerStatisticsAsync(uuid));
        assertArrayEquals(new Object[]{uuid}, service.getLastArgs(MethodKey.FIND_STATISTICS_UUID));

        assertSame(statsOffline, bridge.findPlayerStatisticsAsync(offlinePlayer));
        assertArrayEquals(new Object[]{offlinePlayer}, service.getLastArgs(MethodKey.FIND_STATISTICS_OFFLINE));

        assertSame(valueUuid, bridge.findStatisticValueAsync(uuid, identifier, pluginName));
        assertArrayEquals(new Object[]{uuid, identifier, pluginName}, service.getLastArgs(MethodKey.FIND_STAT_VALUE_UUID));

        assertSame(valueOffline, bridge.findStatisticValueAsync(offlinePlayer, identifier, pluginName));
        assertArrayEquals(new Object[]{offlinePlayer, identifier, pluginName}, service.getLastArgs(MethodKey.FIND_STAT_VALUE_OFFLINE));

        assertSame(hasUuid, bridge.hasStatisticAsync(uuid, identifier, pluginName));
        assertArrayEquals(new Object[]{uuid, identifier, pluginName}, service.getLastArgs(MethodKey.HAS_STAT_UUID));

        assertSame(hasOffline, bridge.hasStatisticAsync(offlinePlayer, identifier, pluginName));
        assertArrayEquals(new Object[]{offlinePlayer, identifier, pluginName}, service.getLastArgs(MethodKey.HAS_STAT_OFFLINE));

        assertSame(removeUuid, bridge.removeStatisticAsync(uuid, identifier, pluginName));
        assertArrayEquals(new Object[]{uuid, identifier, pluginName}, service.getLastArgs(MethodKey.REMOVE_STAT_UUID));

        assertSame(removeOffline, bridge.removeStatisticAsync(offlinePlayer, identifier, pluginName));
        assertArrayEquals(new Object[]{offlinePlayer, identifier, pluginName}, service.getLastArgs(MethodKey.REMOVE_STAT_OFFLINE));

        assertSame(addUuid, bridge.addOrReplaceStatisticAsync(uuid, statistic));
        assertArrayEquals(new Object[]{uuid, statistic}, service.getLastArgs(MethodKey.ADD_OR_REPLACE_UUID));

        assertSame(addOffline, bridge.addOrReplaceStatisticAsync(offlinePlayer, statistic));
        assertArrayEquals(new Object[]{offlinePlayer, statistic}, service.getLastArgs(MethodKey.ADD_OR_REPLACE_OFFLINE));

        assertSame(countUuid, bridge.getStatisticCountForPluginAsync(uuid, pluginName));
        assertArrayEquals(new Object[]{uuid, pluginName}, service.getLastArgs(MethodKey.STAT_COUNT_UUID));

        assertSame(countOffline, bridge.getStatisticCountForPluginAsync(offlinePlayer, pluginName));
        assertArrayEquals(new Object[]{offlinePlayer, pluginName}, service.getLastArgs(MethodKey.STAT_COUNT_OFFLINE));

        assertEquals("2.5.0", bridge.getApiVersion());
        assertSame(service, bridge.getDelegateObject());
    }

    @Test
    void bridgeHandlesNonCompletableFutureReturn() throws Exception {
        NonFutureService delegate = new NonFutureService();
        RCoreBridge bridge = instantiate(delegate);

        UUID uuid = UUID.randomUUID();
        CompletableFuture<Optional<RPlayer>> result = bridge.findPlayerAsync(uuid);
        CompletionException ex = assertThrows(CompletionException.class, result::join);
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    void bridgeHandlesDelegateException() throws Exception {
        ThrowingService delegate = new ThrowingService();
        RCoreBridge bridge = instantiate(delegate);

        UUID uuid = UUID.randomUUID();
        CompletableFuture<Optional<RPlayer>> result = bridge.findPlayerAsync(uuid);
        CompletionException ex = assertThrows(CompletionException.class, result::join);
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    void withDefaultTimeoutCompletesExceptionally() throws Exception {
        RecordingRCoreService service = new RecordingRCoreService();
        CompletableFuture<Optional<RPlayer>> pending = new CompletableFuture<>();
        service.setResponse(MethodKey.FIND_PLAYER_UUID, pending);

        RCoreBridge bridge = instantiate(service);
        bridge.withDefaultTimeout(Duration.ofMillis(100));

        CompletableFuture<Optional<RPlayer>> result = bridge.findPlayerAsync(UUID.randomUUID());
        CompletionException ex = assertThrows(CompletionException.class, result::join);
        assertTrue(ex.getCause() instanceof TimeoutException);
        assertSame(service, bridge.getDelegateObject());
    }

    private RCoreBridge instantiate(Object delegate) throws Exception {
        Constructor<RCoreBridge> constructor = RCoreBridge.class.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
        return constructor.newInstance(delegate);
    }

    private static class NonFutureService {
        @SuppressWarnings("unused")
        public Object findPlayerAsync(UUID uniqueId) {
            return "not-a-future";
        }
    }

    private static class ThrowingService {
        @SuppressWarnings("unused")
        public CompletableFuture<Optional<RPlayer>> findPlayerAsync(UUID uniqueId) {
            throw new IllegalStateException("boom");
        }
    }
}
