package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RCoreAdapterTest {

    @Test
    void findPlayerOfflineDelegatesToUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer player = new RPlayer(uniqueId, "TestPlayer");

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(player));
            backend.queueFindByUuid(Optional.of(player));

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Optional<RPlayer> byUuid = adapter.findPlayerAsync(uniqueId).join();
            final Optional<RPlayer> byOffline = adapter.findPlayerAsync(mockOffline(uniqueId)).join();

            assertTrue(byUuid.isPresent());
            assertTrue(byOffline.isPresent());
            assertEquals(byUuid.get().getUniqueId(), byOffline.get().getUniqueId());
            assertEquals(List.of(uniqueId, uniqueId), backend.getFindByUuidCalls());
        }
    }

    @Test
    void findPlayerByNameDelegatesToBackend() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer player = new RPlayer(uniqueId, "Lookup");

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByName(Optional.of(player));

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Optional<RPlayer> result = adapter.findPlayerByNameAsync("Lookup").join();

            assertTrue(result.isPresent());
            assertEquals("Lookup", backend.getFindByNameCalls().getFirst());
        }
    }

    @Test
    void playerExistsAsyncUsesOptionalPresenceAndExecutor() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer player = new RPlayer(uniqueId, "Presence");

        try (FakeBackend backend = new FakeBackend()) {
            final RCoreAdapter adapter = new RCoreAdapter(backend);

            final CompletableFuture<Optional<RPlayer>> pending = backend.queuePendingFindByUuid();
            final CompletableFuture<Boolean> result = adapter.playerExistsAsync(uniqueId);

            pending.complete(Optional.of(player));

            assertTrue(result.join());
            assertTrue(backend.getLastExecutorThreadName().startsWith("fake-backend-"));
            assertEquals(List.of(uniqueId), backend.getFindByUuidCalls());

            backend.queueFindByUuid(Optional.empty());
            assertFalse(adapter.playerExistsAsync(uniqueId).join());
        }
    }

    @Test
    void playerExistsOfflineMatchesUuidResult() {
        final UUID uniqueId = UUID.randomUUID();

        try (FakeBackend backend = new FakeBackend()) {
            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final OfflinePlayer offline = mockOffline(uniqueId);

            backend.queueFindByUuid(Optional.of(new RPlayer(uniqueId, "Alpha")));
            backend.queueFindByUuid(Optional.of(new RPlayer(uniqueId, "Alpha")));
            assertTrue(adapter.playerExistsAsync(uniqueId).join());
            assertTrue(adapter.playerExistsAsync(offline).join());

            backend.queueFindByUuid(Optional.empty());
            backend.queueFindByUuid(Optional.empty());
            assertFalse(adapter.playerExistsAsync(uniqueId).join());
            assertFalse(adapter.playerExistsAsync(offline).join());

            assertEquals(List.of(uniqueId, uniqueId, uniqueId, uniqueId), backend.getFindByUuidCalls());
        }
    }

    @Test
    void createPlayerSeedsStatisticsBeforeBackendCall() {
        final UUID uniqueId = UUID.randomUUID();

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.empty());

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Optional<RPlayer> created = adapter.createPlayerAsync(uniqueId, "Creator").join();

            assertTrue(created.isPresent());
            assertNotNull(created.get().getPlayerStatistic());
            assertEquals(1, backend.getCreateCalls().size());
            assertTrue(backend.getCreateStatisticSeeded().getFirst());
        }
    }

    @Test
    void createPlayerLogsSevereOnFailure() {
        final UUID uniqueId = UUID.randomUUID();

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.empty());
            final CompletableFuture<RPlayer> pendingCreate = backend.queuePendingCreate();

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Logger logger = CentralLogger.getLogger(RCoreAdapter.class);
            final TestLogHandler handler = new TestLogHandler();
            final boolean previous = logger.getUseParentHandlers();
            logger.addHandler(handler);
            logger.setUseParentHandlers(false);
            try {
                final CompletableFuture<Optional<RPlayer>> result = adapter.createPlayerAsync(uniqueId, "Creator");
                final RuntimeException failure = new RuntimeException("boom");
                pendingCreate.completeExceptionally(failure);

                final CompletionException completionException = assertThrows(CompletionException.class, result::join);
                assertSame(failure, completionException.getCause());
                assertTrue(handler.contains(Level.SEVERE, "Failed to create player"));
            } finally {
                logger.removeHandler(handler);
                logger.setUseParentHandlers(previous);
            }
        }
    }

    @Test
    void findPlayerAsyncLogsWarningOnFailure() {
        final UUID uniqueId = UUID.randomUUID();

        try (FakeBackend backend = new FakeBackend()) {
            final CompletableFuture<Optional<RPlayer>> pending = backend.queuePendingFindByUuid();
            final RCoreAdapter adapter = new RCoreAdapter(backend);

            final Logger logger = CentralLogger.getLogger(RCoreAdapter.class);
            final TestLogHandler handler = new TestLogHandler();
            final boolean previous = logger.getUseParentHandlers();
            logger.addHandler(handler);
            logger.setUseParentHandlers(false);
            try {
                final CompletableFuture<Optional<RPlayer>> future = adapter.findPlayerAsync(uniqueId);
                final IllegalStateException failure = new IllegalStateException("lookup failed");
                pending.completeExceptionally(failure);

                final CompletionException completionException = assertThrows(CompletionException.class, future::join);
                assertSame(failure, completionException.getCause());
                assertTrue(handler.contains(Level.WARNING, "Failed to retrieve player"));
            } finally {
                logger.removeHandler(handler);
                logger.setUseParentHandlers(previous);
            }
        }
    }

    @Test
    void updatePlayerAsyncDelegatesToBackend() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer player = new RPlayer(uniqueId, "Updater");

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueUpdateResult(player);

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Optional<RPlayer> result = adapter.updatePlayerAsync(player).join();

            assertTrue(result.isPresent());
            assertEquals(player, backend.getUpdateCalls().getFirst());
        }
    }

    @Test
    void findPlayerStatisticsOfflineMatchesUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer playerForUuid = playerWithStatistic(uniqueId, "kills", "rdc", 5);
        final RPlayer playerForOffline = playerWithStatistic(uniqueId, "kills", "rdc", 5);

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(playerForUuid));
            backend.queueFindByUuid(Optional.of(playerForOffline));

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Optional<RPlayerStatistic> byUuid = adapter.findPlayerStatisticsAsync(uniqueId).join();
            final Optional<RPlayerStatistic> byOffline = adapter.findPlayerStatisticsAsync(mockOffline(uniqueId)).join();

            assertTrue(byUuid.isPresent());
            assertTrue(byOffline.isPresent());
            assertEquals(byUuid.get().getStatistics().size(), byOffline.get().getStatistics().size());
        }
    }

    @Test
    void findStatisticValueOfflineMatchesUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer playerForUuid = playerWithStatistic(uniqueId, "wins", "rdc", 3);
        final RPlayer playerForOffline = playerWithStatistic(uniqueId, "wins", "rdc", 3);

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(playerForUuid));
            backend.queueFindByUuid(Optional.of(playerForOffline));

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final Optional<Object> byUuid = adapter.findStatisticValueAsync(uniqueId, "wins", "rdc").join();
            final Optional<Object> byOffline = adapter.findStatisticValueAsync(mockOffline(uniqueId), "wins", "rdc").join();

            assertEquals(byUuid, byOffline);
        }
    }

    @Test
    void hasStatisticOfflineMatchesUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer playerForUuid = playerWithStatistic(uniqueId, "quests", "rdc", 1);
        final RPlayer playerForOffline = playerWithStatistic(uniqueId, "quests", "rdc", 1);

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(playerForUuid));
            backend.queueFindByUuid(Optional.of(playerForOffline));

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final boolean byUuid = adapter.hasStatisticAsync(uniqueId, "quests", "rdc").join();
            final boolean byOffline = adapter.hasStatisticAsync(mockOffline(uniqueId), "quests", "rdc").join();

            assertTrue(byUuid);
            assertEquals(byUuid, byOffline);
        }
    }

    @Test
    void removeStatisticOfflineMatchesUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer playerForUuid = playerWithStatistic(uniqueId, "blocks", "rdc", 12);
        final RPlayer playerForOffline = playerWithStatistic(uniqueId, "blocks", "rdc", 12);

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(playerForUuid));
            backend.queueUpdateResult(playerForUuid);
            backend.queueFindByUuid(Optional.of(playerForOffline));
            backend.queueUpdateResult(playerForOffline);

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            assertTrue(adapter.removeStatisticAsync(uniqueId, "blocks", "rdc").join());
            assertTrue(adapter.removeStatisticAsync(mockOffline(uniqueId), "blocks", "rdc").join());
            assertEquals(2, backend.getUpdateCalls().size());
        }
    }

    @Test
    void addOrReplaceStatisticOfflineMatchesUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer playerForUuid = new RPlayer(uniqueId, "Adder");
        final RPlayer playerForOffline = new RPlayer(uniqueId, "Adder");
        final TestStatistic statisticForUuid = new TestStatistic("blocks_mined", "rdc", 42);
        final TestStatistic statisticForOffline = new TestStatistic("blocks_mined", "rdc", 84);

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(playerForUuid));
            backend.queueUpdateResult(playerForUuid);
            backend.queueFindByUuid(Optional.of(playerForOffline));
            backend.queueUpdateResult(playerForOffline);

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            assertTrue(adapter.addOrReplaceStatisticAsync(uniqueId, statisticForUuid).join());
            assertTrue(adapter.addOrReplaceStatisticAsync(mockOffline(uniqueId), statisticForOffline).join());
            assertEquals(2, backend.getUpdateCalls().size());
            assertNotNull(playerForUuid.getPlayerStatistic());
            assertNotNull(playerForOffline.getPlayerStatistic());
        }
    }

    @Test
    void getStatisticCountOfflineMatchesUuid() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer playerForUuid = playerWithStatistic(uniqueId, "coins", "rdc", 99);
        playerForUuid.getPlayerStatistic().addOrReplaceStatistic(new TestStatistic("gems", "rdc", 12));
        final RPlayer playerForOffline = playerWithStatistic(uniqueId, "coins", "rdc", 99);
        playerForOffline.getPlayerStatistic().addOrReplaceStatistic(new TestStatistic("gems", "rdc", 12));

        try (FakeBackend backend = new FakeBackend()) {
            backend.queueFindByUuid(Optional.of(playerForUuid));
            backend.queueFindByUuid(Optional.of(playerForOffline));

            final RCoreAdapter adapter = new RCoreAdapter(backend);
            final long byUuid = adapter.getStatisticCountForPluginAsync(uniqueId, "rdc").join();
            final long byOffline = adapter.getStatisticCountForPluginAsync(mockOffline(uniqueId), "rdc").join();

            assertEquals(2L, byUuid);
            assertEquals(byUuid, byOffline);
        }
    }

    @Test
    void apiVersionRemainsStable() {
        try (FakeBackend backend = new FakeBackend()) {
            final RCoreAdapter adapter = new RCoreAdapter(backend);
            assertEquals("2.0.0", adapter.getApiVersion());
        }
    }

    private static OfflinePlayer mockOffline(final UUID uniqueId) {
        final OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.getUniqueId()).thenReturn(uniqueId);
        when(offline.getName()).thenReturn("Offline" + uniqueId.toString().substring(0, 5));
        return offline;
    }

    private static RPlayer playerWithStatistic(
            final UUID uniqueId,
            final String identifier,
            final String plugin,
            final Object value
    ) {
        final RPlayer player = new RPlayer(uniqueId, "Player" + identifier);
        final RPlayerStatistic statistic = new RPlayerStatistic(player);
        player.setPlayerStatistic(statistic);
        statistic.addOrReplaceStatistic(new TestStatistic(identifier, plugin, value));
        return player;
    }

    private static final class FakeBackend implements RCoreBackend, AutoCloseable {

        private final RecordingExecutorService executor;
        private final Queue<CompletableFuture<Optional<RPlayer>>> findByUuidResponses = new ArrayDeque<>();
        private final Queue<CompletableFuture<Optional<RPlayer>>> findByNameResponses = new ArrayDeque<>();
        private final Queue<CompletableFuture<RPlayer>> createResponses = new ArrayDeque<>();
        private final Queue<CompletableFuture<RPlayer>> updateResponses = new ArrayDeque<>();
        private final List<UUID> findByUuidCalls = new ArrayList<>();
        private final List<String> findByNameCalls = new ArrayList<>();
        private final List<RPlayer> createCalls = new ArrayList<>();
        private final List<Boolean> createStatisticSeeded = new ArrayList<>();
        private final List<RPlayer> updateCalls = new ArrayList<>();

        private FakeBackend() {
            this.executor = new RecordingExecutorService("fake-backend");
        }

        @Override
        public @NotNull ExecutorService getExecutor() {
            return this.executor;
        }

        @Override
        public @NotNull CompletableFuture<Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
            this.findByUuidCalls.add(uniqueId);
            return poll(this.findByUuidResponses, "findByUuid");
        }

        @Override
        public @NotNull CompletableFuture<Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
            this.findByNameCalls.add(playerName);
            return poll(this.findByNameResponses, "findByName");
        }

        @Override
        public @NotNull CompletableFuture<RPlayer> createAsync(final @NotNull RPlayer player) {
            this.createCalls.add(player);
            this.createStatisticSeeded.add(player.getPlayerStatistic() != null
                    && player.getPlayerStatistic().getPlayer() == player);
            return poll(this.createResponses, "create", player);
        }

        @Override
        public @NotNull CompletableFuture<RPlayer> updateAsync(final @NotNull RPlayer player) {
            this.updateCalls.add(player);
            return poll(this.updateResponses, "update", player);
        }

        private <T> CompletableFuture<T> poll(final Queue<CompletableFuture<T>> queue, final String name) {
            final CompletableFuture<T> future = queue.poll();
            if (future != null) {
                return future;
            }
            return CompletableFuture.failedFuture(new AssertionError("No future queued for " + name));
        }

        private <T> CompletableFuture<T> poll(
                final Queue<CompletableFuture<T>> queue,
                final String name,
                final T defaultValue
        ) {
            final CompletableFuture<T> future = queue.poll();
            if (future != null) {
                return future;
            }
            return CompletableFuture.completedFuture(defaultValue);
        }

        CompletableFuture<Optional<RPlayer>> queuePendingFindByUuid() {
            final CompletableFuture<Optional<RPlayer>> future = new CompletableFuture<>();
            this.findByUuidResponses.add(future);
            return future;
        }

        CompletableFuture<RPlayer> queuePendingCreate() {
            final CompletableFuture<RPlayer> future = new CompletableFuture<>();
            this.createResponses.add(future);
            return future;
        }

        void queueFindByUuid(final Optional<RPlayer> response) {
            this.findByUuidResponses.add(CompletableFuture.completedFuture(response));
        }

        void queueFindByName(final Optional<RPlayer> response) {
            this.findByNameResponses.add(CompletableFuture.completedFuture(response));
        }

        void queueCreateResult(final RPlayer player) {
            this.createResponses.add(CompletableFuture.completedFuture(player));
        }

        void queueUpdateResult(final RPlayer player) {
            this.updateResponses.add(CompletableFuture.completedFuture(player));
        }

        List<UUID> getFindByUuidCalls() {
            return List.copyOf(this.findByUuidCalls);
        }

        List<String> getFindByNameCalls() {
            return List.copyOf(this.findByNameCalls);
        }

        List<RPlayer> getCreateCalls() {
            return List.copyOf(this.createCalls);
        }

        List<Boolean> getCreateStatisticSeeded() {
            return List.copyOf(this.createStatisticSeeded);
        }

        List<RPlayer> getUpdateCalls() {
            return List.copyOf(this.updateCalls);
        }

        String getLastExecutorThreadName() {
            return this.executor.getLastThreadName();
        }

        @Override
        public void close() {
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class RecordingExecutorService extends AbstractExecutorService {

        private final ExecutorService delegate;
        private final AtomicReference<String> lastThreadName = new AtomicReference<>();

        private RecordingExecutorService(final String threadName) {
            final AtomicInteger counter = new AtomicInteger();
            final ThreadFactory factory = r -> {
                final Thread thread = new Thread(r);
                thread.setName(threadName + "-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            this.delegate = Executors.newSingleThreadExecutor(factory);
        }

        private String getLastThreadName() {
            return this.lastThreadName.get();
        }

        @Override
        public void shutdown() {
            this.delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return this.delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return this.delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return this.delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            return this.delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(final Runnable command) {
            Objects.requireNonNull(command, "command");
            this.delegate.execute(() -> {
                this.lastThreadName.set(Thread.currentThread().getName());
                command.run();
            });
        }
    }

    private static final class TestStatistic extends RAbstractStatistic {

        private final Object value;

        private TestStatistic(final String identifier, final String plugin, final Object value) {
            super(identifier, plugin);
            this.value = value;
        }

        @Override
        public @NotNull Object getValue() {
            return this.value;
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null) {
                this.records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private boolean contains(final Level level, final String snippet) {
            return this.records.stream()
                    .filter(record -> record.getLevel().equals(level))
                    .anyMatch(record -> record.getMessage() != null && record.getMessage().contains(snippet));
        }
    }
}
