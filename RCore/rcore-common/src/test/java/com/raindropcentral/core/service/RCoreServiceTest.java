package com.raindropcentral.core.service;

import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.core.api.RCoreBackend;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCoreServiceTest {

    private StubBackend backend;
    private RCoreService service;
    private TestLogHandler logHandler;
    private Logger logger;

    @BeforeEach
    void setUp() {
        backend = new StubBackend();
        service = new RCoreAdapter(backend);
        logger = Logger.getLogger(RCoreAdapter.class.getName());
        logHandler = new TestLogHandler();
        logger.setUseParentHandlers(false);
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    void tearDown() {
        logger.removeHandler(logHandler);
    }

    @Test
    void shouldRejectNullInputs() {
        final OfflinePlayer offline = mockOffline(UUID.randomUUID());
        final RPlayer player = new RPlayer(UUID.randomUUID(), "Alpha");
        assertThrows(NullPointerException.class, () -> service.findPlayerAsync((UUID) null));
        assertThrows(NullPointerException.class, () -> service.findPlayerAsync((OfflinePlayer) null));
        assertThrows(NullPointerException.class, () -> service.findPlayerByNameAsync(null));
        assertThrows(NullPointerException.class, () -> service.playerExistsAsync((UUID) null));
        assertThrows(NullPointerException.class, () -> service.playerExistsAsync((OfflinePlayer) null));
        assertThrows(NullPointerException.class, () -> service.createPlayerAsync(null, "Alpha"));
        assertThrows(NullPointerException.class, () -> service.createPlayerAsync(UUID.randomUUID(), null));
        assertThrows(NullPointerException.class, () -> service.updatePlayerAsync(null));
        assertThrows(NullPointerException.class, () -> service.findPlayerStatisticsAsync((UUID) null));
        assertThrows(NullPointerException.class, () -> service.findPlayerStatisticsAsync((OfflinePlayer) null));
        assertThrows(NullPointerException.class, () -> service.findStatisticValueAsync(null, "kills", "plugin"));
        assertThrows(NullPointerException.class, () -> service.findStatisticValueAsync(UUID.randomUUID(), null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.findStatisticValueAsync(UUID.randomUUID(), "kills", null));
        assertThrows(NullPointerException.class, () -> service.findStatisticValueAsync((OfflinePlayer) null, "kills", "plugin"));
        assertThrows(NullPointerException.class, () -> service.findStatisticValueAsync(offline, null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.findStatisticValueAsync(offline, "kills", null));
        assertThrows(NullPointerException.class, () -> service.hasStatisticAsync((UUID) null, "kills", "plugin"));
        assertThrows(NullPointerException.class, () -> service.hasStatisticAsync(UUID.randomUUID(), null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.hasStatisticAsync(UUID.randomUUID(), "kills", null));
        assertThrows(NullPointerException.class, () -> service.hasStatisticAsync((OfflinePlayer) null, "kills", "plugin"));
        assertThrows(NullPointerException.class, () -> service.hasStatisticAsync(offline, null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.hasStatisticAsync(offline, "kills", null));
        assertThrows(NullPointerException.class, () -> service.removeStatisticAsync((UUID) null, "kills", "plugin"));
        assertThrows(NullPointerException.class, () -> service.removeStatisticAsync(UUID.randomUUID(), null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.removeStatisticAsync(UUID.randomUUID(), "kills", null));
        assertThrows(NullPointerException.class, () -> service.removeStatisticAsync((OfflinePlayer) null, "kills", "plugin"));
        assertThrows(NullPointerException.class, () -> service.removeStatisticAsync(offline, null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.removeStatisticAsync(offline, "kills", null));
        assertThrows(NullPointerException.class, () -> service.addOrReplaceStatisticAsync((UUID) null, new TestStatistic("id", "plugin", 1))); 
        assertThrows(NullPointerException.class, () -> service.addOrReplaceStatisticAsync(UUID.randomUUID(), null));
        assertThrows(NullPointerException.class, () -> service.addOrReplaceStatisticAsync((OfflinePlayer) null, new TestStatistic("id", "plugin", 1)));
        assertThrows(NullPointerException.class, () -> service.addOrReplaceStatisticAsync(offline, null));
        assertThrows(NullPointerException.class, () -> service.getStatisticCountForPluginAsync((UUID) null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.getStatisticCountForPluginAsync(UUID.randomUUID(), null));
        assertThrows(NullPointerException.class, () -> service.getStatisticCountForPluginAsync((OfflinePlayer) null, "plugin"));
        assertThrows(NullPointerException.class, () -> service.getStatisticCountForPluginAsync(offline, null));
    }

    @Test
    void shouldDelegateFindPlayerAsyncForUuidAndOffline() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        backend.queueFindByUuid(Optional.of(player));
        assertEquals(Optional.of(player), service.findPlayerAsync(id).join());
        assertEquals(List.of(id), backend.getFindByUuidCalls());

        backend.queueFindByUuid(Optional.of(player));
        final OfflinePlayer offline = mockOffline(id);
        assertEquals(Optional.of(player), service.findPlayerAsync(offline).join());
        assertEquals(List.of(id, id), backend.getFindByUuidCalls());
    }

    @Test
    void shouldDelegateFindPlayerByName() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        backend.queueFindByName(Optional.of(player));
        assertEquals(Optional.of(player), service.findPlayerByNameAsync("Alpha").join());
        assertEquals(List.of("Alpha"), backend.getFindByNameCalls());
    }

    @Test
    void shouldReportPlayerExistence() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        backend.queueFindByUuid(Optional.of(player));
        assertTrue(service.playerExistsAsync(id).join());

        backend.queueFindByUuid(Optional.empty());
        final OfflinePlayer offline = mockOffline(id);
        assertFalse(service.playerExistsAsync(offline).join());
    }

    @Test
    void shouldPropagateLookupFailureAndLogWarning() {
        final UUID id = UUID.randomUUID();
        backend.queueFindByUuidException(new IllegalStateException("lookup failed"));
        final CompletableFuture<Boolean> future = service.playerExistsAsync(id);
        assertThrows(CompletionException.class, future::join);
        assertTrue(logHandler.contains(Level.WARNING, id.toString()));
    }

    @Test
    void shouldCreatePlayerWhenMissing() {
        final UUID id = UUID.randomUUID();
        backend.queueFindByUuid(Optional.empty());
        final CompletableFuture<RPlayer> persisted = new CompletableFuture<>();
        backend.queueCreate(persisted);

        final CompletableFuture<Optional<RPlayer>> future = service.createPlayerAsync(id, "Alpha");
        assertEquals(1, backend.getCreatedPlayers().size());
        final RPlayer created = backend.getCreatedPlayers().getFirst();
        assertNotNull(created.getPlayerStatistic());
        assertSame(created, created.getPlayerStatistic().getPlayer());

        persisted.complete(created);
        final Optional<RPlayer> result = future.join();
        assertTrue(result.isPresent());
        assertSame(created, result.orElseThrow());
    }

    @Test
    void shouldWarnWhenCreatingExistingPlayer() {
        final UUID id = UUID.randomUUID();
        backend.queueFindByUuid(Optional.of(new RPlayer(id, "Alpha")));
        final Optional<RPlayer> result = service.createPlayerAsync(id, "Alpha").join();
        assertTrue(result.isEmpty());
        assertTrue(logHandler.contains(Level.WARNING, "already exists"));
        assertTrue(backend.getCreatedPlayers().isEmpty());
    }

    @Test
    void shouldLogSevereWhenCreateFails() {
        final UUID id = UUID.randomUUID();
        backend.queueFindByUuid(Optional.empty());
        backend.queueCreateException(new IllegalStateException("boom"));
        final CompletableFuture<Optional<RPlayer>> future = service.createPlayerAsync(id, "Alpha");
        assertThrows(CompletionException.class, future::join);
        assertTrue(logHandler.contains(Level.SEVERE, id.toString()));
    }

    @Test
    void shouldUpdatePlayerAndReturnOptional() {
        final RPlayer player = new RPlayer(UUID.randomUUID(), "Alpha");
        backend.queueUpdate(CompletableFuture.completedFuture(player));
        final Optional<RPlayer> result = service.updatePlayerAsync(player).join();
        assertTrue(result.isPresent());
        assertSame(player, result.orElseThrow());
        assertEquals(List.of(player), backend.getUpdatedPlayers());
    }

    @Test
    void shouldLogSevereWhenUpdateFails() {
        final RPlayer player = new RPlayer(UUID.randomUUID(), "Alpha");
        backend.queueUpdateException(new IllegalStateException("fail"));
        final CompletableFuture<Optional<RPlayer>> future = service.updatePlayerAsync(player);
        assertThrows(CompletionException.class, future::join);
        assertTrue(logHandler.contains(Level.SEVERE, player.getUniqueId().toString()));
    }

    @Test
    void shouldResolveStatisticsForUuidAndOffline() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        final RPlayerStatistic statistics = attachStatistics(player);
        backend.queueFindByUuid(Optional.of(player));
        assertEquals(Optional.of(statistics), service.findPlayerStatisticsAsync(id).join());

        backend.queueFindByUuid(Optional.of(player));
        final OfflinePlayer offline = mockOffline(id);
        assertEquals(Optional.of(statistics), service.findPlayerStatisticsAsync(offline).join());
    }

    @Test
    void shouldReturnEmptyStatisticsWhenPlayerMissing() {
        backend.queueFindByUuid(Optional.empty());
        assertTrue(service.findPlayerStatisticsAsync(UUID.randomUUID()).join().isEmpty());
    }

    @Test
    void shouldFindStatisticValues() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        final RPlayerStatistic statistics = attachStatistics(player);
        statistics.addOrReplaceStatistic(new TestStatistic("kills", "plugin", 42));

        backend.queueFindByUuid(Optional.of(player));
        assertEquals(Optional.of(42), service.findStatisticValueAsync(id, "kills", "plugin").join());

        backend.queueFindByUuid(Optional.of(player));
        final OfflinePlayer offline = mockOffline(id);
        assertEquals(Optional.of(42), service.findStatisticValueAsync(offline, "kills", "plugin").join());
    }

    @Test
    void shouldReturnEmptyStatisticValueWhenMissing() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        attachStatistics(player);
        backend.queueFindByUuid(Optional.of(player));
        assertTrue(service.findStatisticValueAsync(id, "kills", "plugin").join().isEmpty());
    }

    @Test
    void shouldDetermineStatisticPresence() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        final RPlayerStatistic statistics = attachStatistics(player);
        statistics.addOrReplaceStatistic(new TestStatistic("kills", "plugin", 42));

        backend.queueFindByUuid(Optional.of(player));
        assertTrue(service.hasStatisticAsync(id, "kills", "plugin").join());

        backend.queueFindByUuid(Optional.of(player));
        final OfflinePlayer offline = mockOffline(id);
        assertTrue(service.hasStatisticAsync(offline, "kills", "plugin").join());

        backend.queueFindByUuid(Optional.of(player));
        assertFalse(service.hasStatisticAsync(id, "deaths", "plugin").join());
    }

    @Test
    void shouldReturnFalseForMissingStatisticInRemoval() {
        final UUID id = UUID.randomUUID();
        backend.queueFindByUuid(Optional.empty());
        assertFalse(service.removeStatisticAsync(id, "kills", "plugin").join());

        final RPlayer player = new RPlayer(id, "Alpha");
        attachStatistics(player);
        backend.queueFindByUuid(Optional.of(player));
        assertFalse(service.removeStatisticAsync(id, "kills", "plugin").join());
        assertTrue(backend.getUpdatedPlayers().isEmpty());
    }

    @Test
    void shouldRemoveStatisticAndPersist() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        final RPlayerStatistic statistics = attachStatistics(player);
        statistics.addOrReplaceStatistic(new TestStatistic("kills", "plugin", 42));
        backend.queueFindByUuid(Optional.of(player));
        backend.queueUpdate(CompletableFuture.completedFuture(player));

        assertTrue(service.removeStatisticAsync(id, "kills", "plugin").join());
        assertEquals(List.of(player), backend.getUpdatedPlayers());
        assertFalse(statistics.hasStatistic("kills", "plugin"));
    }

    @Test
    void shouldLogSevereWhenRemovingStatisticFails() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        final RPlayerStatistic statistics = attachStatistics(player);
        statistics.addOrReplaceStatistic(new TestStatistic("kills", "plugin", 42));
        backend.queueFindByUuid(Optional.of(player));
        backend.queueUpdateException(new IllegalStateException("remove"));

        final CompletableFuture<Boolean> future = service.removeStatisticAsync(id, "kills", "plugin");
        assertThrows(CompletionException.class, future::join);
        assertTrue(logHandler.contains(Level.SEVERE, id.toString()));
    }

    @Test
    void shouldAddOrReplaceStatisticAndPersist() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        backend.queueFindByUuid(Optional.of(player));
        backend.queueUpdate(CompletableFuture.completedFuture(player));

        assertTrue(service.addOrReplaceStatisticAsync(id, new TestStatistic("kills", "plugin", 42)).join());
        assertEquals(List.of(player), backend.getUpdatedPlayers());
        assertNotNull(player.getPlayerStatistic());
        assertTrue(player.getPlayerStatistic().hasStatistic("kills", "plugin"));

        backend.queueFindByUuid(Optional.of(player));
        backend.queueUpdate(CompletableFuture.completedFuture(player));
        final OfflinePlayer offline = mockOffline(id);
        assertTrue(service.addOrReplaceStatisticAsync(offline, new TestStatistic("kills", "plugin", 99)).join());
    }

    @Test
    void shouldWarnWhenAddingStatisticToMissingPlayer() {
        final UUID id = UUID.randomUUID();
        backend.queueFindByUuid(Optional.empty());
        assertFalse(service.addOrReplaceStatisticAsync(id, new TestStatistic("kills", "plugin", 42)).join());
        assertTrue(logHandler.contains(Level.WARNING, "non-existent player"));
    }

    @Test
    void shouldLogSevereWhenAddStatisticFails() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        backend.queueFindByUuid(Optional.of(player));
        backend.queueUpdateException(new IllegalStateException("persist"));

        final CompletableFuture<Boolean> future = service.addOrReplaceStatisticAsync(id, new TestStatistic("kills", "plugin", 42));
        assertThrows(CompletionException.class, future::join);
        assertTrue(logHandler.contains(Level.SEVERE, id.toString()));
    }

    @Test
    void shouldCountStatisticsByPlugin() {
        final UUID id = UUID.randomUUID();
        final RPlayer player = new RPlayer(id, "Alpha");
        final RPlayerStatistic statistics = attachStatistics(player);
        statistics.addOrReplaceStatistic(new TestStatistic("kills", "plugin", 42));
        statistics.addOrReplaceStatistic(new TestStatistic("quests", "plugin", 3));
        statistics.addOrReplaceStatistic(new TestStatistic("coins", "other", 12));

        backend.queueFindByUuid(Optional.of(player));
        assertEquals(2L, service.getStatisticCountForPluginAsync(id, "plugin").join());

        backend.queueFindByUuid(Optional.of(player));
        final OfflinePlayer offline = mockOffline(id);
        assertEquals(1L, service.getStatisticCountForPluginAsync(offline, "other").join());
    }

    @Test
    void shouldReturnApiVersion() {
        assertEquals("2.0.0", service.getApiVersion());
    }

    private static OfflinePlayer mockOffline(final UUID id) {
        final OfflinePlayer offline = Mockito.mock(OfflinePlayer.class);
        Mockito.when(offline.getUniqueId()).thenReturn(id);
        Mockito.when(offline.getName()).thenReturn("player-" + id);
        return offline;
    }

    private static RPlayerStatistic attachStatistics(final RPlayer player) {
        final RPlayerStatistic statistics = new RPlayerStatistic(player);
        player.setPlayerStatistic(statistics);
        return statistics;
    }

    private static final class TestStatistic extends RAbstractStatistic {

        private final Object value;

        private TestStatistic(final String identifier, final String plugin, final Object value) {
            super(identifier, plugin);
            this.value = value;
        }

        @Override
        public @NotNull Object getValue() {
            return value;
        }
    }

    private static final class StubBackend implements RCoreBackend {

        private final ExecutorService executor = new DirectExecutorService();
        private final Deque<CompletableFuture<Optional<RPlayer>>> findByUuidResponses = new ArrayDeque<>();
        private final Deque<CompletableFuture<Optional<RPlayer>>> findByNameResponses = new ArrayDeque<>();
        private final Deque<CompletableFuture<RPlayer>> createResponses = new ArrayDeque<>();
        private final Deque<CompletableFuture<RPlayer>> updateResponses = new ArrayDeque<>();
        private final List<UUID> findByUuidCalls = new ArrayList<>();
        private final List<String> findByNameCalls = new ArrayList<>();
        private final Deque<RPlayer> createdPlayers = new ArrayDeque<>();
        private final List<RPlayer> updatedPlayers = new ArrayList<>();

        @Override
        public @NotNull ExecutorService getExecutor() {
            return executor;
        }

        @Override
        public @NotNull CompletableFuture<Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
            Objects.requireNonNull(uniqueId, "uniqueId");
            findByUuidCalls.add(uniqueId);
            return pollOrDefault(findByUuidResponses, CompletableFuture.completedFuture(Optional.empty()));
        }

        @Override
        public @NotNull CompletableFuture<Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
            Objects.requireNonNull(playerName, "playerName");
            findByNameCalls.add(playerName);
            return pollOrDefault(findByNameResponses, CompletableFuture.completedFuture(Optional.empty()));
        }

        @Override
        public @NotNull CompletableFuture<RPlayer> createAsync(final @NotNull RPlayer player) {
            Objects.requireNonNull(player, "player");
            createdPlayers.add(player);
            return pollOrDefault(createResponses, CompletableFuture.completedFuture(player));
        }

        @Override
        public @NotNull CompletableFuture<RPlayer> updateAsync(final @NotNull RPlayer player) {
            Objects.requireNonNull(player, "player");
            updatedPlayers.add(player);
            return pollOrDefault(updateResponses, CompletableFuture.completedFuture(player));
        }

        void queueFindByUuid(final Optional<RPlayer> response) {
            findByUuidResponses.add(CompletableFuture.completedFuture(response));
        }

        void queueFindByUuid(final CompletableFuture<Optional<RPlayer>> future) {
            findByUuidResponses.add(future);
        }

        void queueFindByUuidException(final Throwable throwable) {
            final CompletableFuture<Optional<RPlayer>> future = new CompletableFuture<>();
            future.completeExceptionally(throwable);
            findByUuidResponses.add(future);
        }

        void queueFindByName(final Optional<RPlayer> response) {
            findByNameResponses.add(CompletableFuture.completedFuture(response));
        }

        void queueCreate(final CompletableFuture<RPlayer> future) {
            createResponses.add(future);
        }

        void queueCreateException(final Throwable throwable) {
            final CompletableFuture<RPlayer> future = new CompletableFuture<>();
            future.completeExceptionally(throwable);
            createResponses.add(future);
        }

        void queueUpdate(final CompletableFuture<RPlayer> future) {
            updateResponses.add(future);
        }

        void queueUpdateException(final Throwable throwable) {
            final CompletableFuture<RPlayer> future = new CompletableFuture<>();
            future.completeExceptionally(throwable);
            updateResponses.add(future);
        }

        List<UUID> getFindByUuidCalls() {
            return List.copyOf(findByUuidCalls);
        }

        List<String> getFindByNameCalls() {
            return List.copyOf(findByNameCalls);
        }

        Deque<RPlayer> getCreatedPlayers() {
            return createdPlayers;
        }

        List<RPlayer> getUpdatedPlayers() {
            return List.copyOf(updatedPlayers);
        }

        private static <T> CompletableFuture<T> pollOrDefault(
                final Deque<CompletableFuture<T>> deque,
                final CompletableFuture<T> defaultValue
        ) {
            final CompletableFuture<T> future = deque.pollFirst();
            return future != null ? future : defaultValue;
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {

        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown();
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(final @NotNull Runnable command) {
            command.run();
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean contains(final Level level, final String snippet) {
            return records.stream()
                    .filter(record -> record.getLevel().equals(level))
                    .anyMatch(record -> record.getMessage().contains(snippet));
        }
    }
}
