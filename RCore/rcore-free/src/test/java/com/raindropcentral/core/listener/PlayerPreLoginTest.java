package com.raindropcentral.core.listener;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.core.RCoreFree;
import com.raindropcentral.core.RCoreFreeImpl;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.database.repository.RPlayerRepository;
import com.raindropcentral.core.service.RPlayerStatisticService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.type.EStatisticType;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class PlayerPreLoginTest {

    private static final String PLUGIN_NAMESPACE = "RCore-Free-Test";

    private ServerMock server;
    private RecordingExecutor executor;
    private RCoreFree plugin;
    private RCoreFreeImpl implementation;
    private RPlayerRepository repository;
    private PlayerPreLogin listener;
    private TestLogHandler logHandler;
    private Logger logger;
    private boolean originalUseParentHandlers;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.executor = new RecordingExecutor();
        this.plugin = mock(RCoreFree.class);
        this.implementation = mock(RCoreFreeImpl.class);
        this.repository = mock(RPlayerRepository.class);

        when(this.plugin.getImpl()).thenReturn(this.implementation);
        when(this.plugin.getName()).thenReturn(PLUGIN_NAMESPACE);
        when(this.implementation.getExecutor()).thenReturn(this.executor);
        when(this.implementation.getRPlayerRepository()).thenReturn(this.repository);

        this.listener = new PlayerPreLogin(this.plugin);

        this.logger = CentralLogger.getLogger(PlayerPreLogin.class);
        this.logHandler = new TestLogHandler();
        this.originalUseParentHandlers = this.logger.getUseParentHandlers();
        this.originalLevel = this.logger.getLevel();
        this.logger.setUseParentHandlers(false);
        this.logger.setLevel(Level.ALL);
        this.logger.addHandler(this.logHandler);
    }

    @AfterEach
    void tearDown() {
        this.logger.removeHandler(this.logHandler);
        this.logger.setUseParentHandlers(this.originalUseParentHandlers);
        this.logger.setLevel(this.originalLevel);
        this.executor.shutdownNow();
        MockBukkit.unmock();
    }

    @Test
    void onAsyncPreLoginCreatesNewPlayerWithDefaults() {
        final UUID uniqueId = UUID.randomUUID();
        final String playerName = "NewJoiner";

        when(this.repository.findByAttributesAsync(argThat(map -> uniqueId.equals(map.get("uniqueId")))))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(this.repository.createAsync(any())).thenAnswer(invocation -> {
            final RPlayer created = invocation.getArgument(0);
            return CompletableFuture.completedFuture(created);
        });

        final AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
                playerName,
                InetAddress.getLoopbackAddress(),
                uniqueId
        );

        this.listener.onAsyncPreLogin(event);

        final int expectedCore = EStatisticType.getCoreDefaults().size();
        final int expectedGameplay = EStatisticType.getGameplayDefaults().size();
        final int expectedSystem = EStatisticType
                .getDefaultValuesForCategory(EStatisticType.StatisticCategory.SYSTEM)
                .size();
        final int expectedProgression = EStatisticType
                .getDefaultValuesForCategory(EStatisticType.StatisticCategory.PROGRESSION)
                .size();
        final int expectedTotal = expectedCore + expectedGameplay + expectedSystem + expectedProgression;

        final RPlayer captured = captureCreatedPlayer();

        assertEquals(playerName, captured.getPlayerName());
        assertNotNull(captured.getPlayerStatistic());
        assertEquals(expectedTotal, captured.getPlayerStatistic().getStatistics().size());

        final Object loginCount = captured.getPlayerStatistic()
                .getStatisticValue(EStatisticType.LOGIN_COUNT.getKey(), PLUGIN_NAMESPACE)
                .orElse(null);
        assertTrue(loginCount instanceof Number);
        assertEquals(1.0d, ((Number) loginCount).doubleValue());

        final Object firstJoinServer = captured.getPlayerStatistic()
                .getStatisticValue(EStatisticType.FIRST_JOIN_SERVER.getKey(), PLUGIN_NAMESPACE)
                .orElse(null);
        assertEquals(PLUGIN_NAMESPACE, firstJoinServer);

        final List<String> infoMessages = this.logHandler.messages(Level.INFO);
        assertTrue(infoMessages.stream().anyMatch(msg -> msg.contains("with " + expectedTotal + " initial statistics")));
        assertTrue(infoMessages.stream().anyMatch(msg -> msg.contains("with " + expectedTotal + " statistics")));
        assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult());
    }

    @Test
    void onAsyncPreLoginUpdatesExistingPlayerWithMissingStatistics() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer existing = new RPlayer(uniqueId, "LegacyName");

        when(this.repository.findByAttributesAsync(argThat(map -> uniqueId.equals(map.get("uniqueId")))))
                .thenReturn(CompletableFuture.completedFuture(existing));
        when(this.repository.updateAsync(any())).thenAnswer(invocation -> {
            final RPlayer updated = invocation.getArgument(0);
            return CompletableFuture.completedFuture(updated);
        });

        final AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
                "ModernName",
                InetAddress.getLoopbackAddress(),
                uniqueId
        );

        this.listener.onAsyncPreLogin(event);

        final int expectedTotal = EStatisticType.getCoreDefaults().size()
                + EStatisticType.getGameplayDefaults().size()
                + EStatisticType.getDefaultValuesForCategory(EStatisticType.StatisticCategory.SYSTEM).size()
                + EStatisticType.getDefaultValuesForCategory(EStatisticType.StatisticCategory.PROGRESSION).size();

        verify(this.repository).updateAsync(existing);
        assertEquals("ModernName", existing.getPlayerName());
        assertNotNull(existing.getPlayerStatistic());
        assertEquals(expectedTotal, existing.getPlayerStatistic().getStatistics().size());

        final List<String> infoMessages = this.logHandler.messages(Level.INFO);
        assertTrue(infoMessages.stream().anyMatch(msg -> msg.contains("with " + expectedTotal + " statistics")));
        assertEquals(AsyncPlayerPreLoginEvent.Result.ALLOWED, event.getLoginResult());
    }

    @Test
    void onAsyncPreLoginDisallowsLoginOnFailure() throws Exception {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer existing = new RPlayer(uniqueId, "FailingUser");
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(existing);
        existing.setPlayerStatistic(statistic);

        when(this.repository.findByAttributesAsync(argThat(map -> uniqueId.equals(map.get("uniqueId")))))
                .thenReturn(CompletableFuture.completedFuture(existing));

        final CompletableFuture<RPlayer> updateFuture = new CompletableFuture<>();
        when(this.repository.updateAsync(existing)).thenReturn(updateFuture);

        final AsyncPlayerPreLoginEvent event = new AsyncPlayerPreLoginEvent(
                "FailingUser",
                InetAddress.getLoopbackAddress(),
                uniqueId
        );

        final CompletableFuture<Void> invocation = CompletableFuture.runAsync(() -> this.listener.onAsyncPreLogin(event));

        verify(this.repository, timeout(1000)).updateAsync(existing);
        assertFalse(invocation.isDone(), "Listener should wait for the persistence pipeline to complete");

        updateFuture.completeExceptionally(new IllegalStateException("database offline"));
        invocation.join();

        assertEquals(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, event.getLoginResult());
        assertEquals(Component.text("Player data could not be processed. Please try again."), event.kickMessage());
        assertTrue(this.executor.getRunCount() > 0, "Executor should process the asynchronous pipeline");

        final List<LogRecord> severeRecords = this.logHandler.records(Level.SEVERE);
        assertFalse(severeRecords.isEmpty(), "Failure path should log at SEVERE level");
        assertTrue(severeRecords.stream().anyMatch(record -> record.getMessage().contains("Error handling pre-login")));
    }

    private RPlayer captureCreatedPlayer() {
        final ArgumentCaptor<RPlayer> captor = ArgumentCaptor.forClass(RPlayer.class);
        verify(this.repository).createAsync(captor.capture());
        return captor.getValue();
    }

    private static final class RecordingExecutor extends AbstractExecutorService {

        private final ExecutorService delegate;
        private final AtomicInteger runCount = new AtomicInteger();

        private RecordingExecutor() {
            this.delegate = Executors.newSingleThreadExecutor(runnable -> {
                final Thread thread = new Thread(runnable, "PlayerPreLoginTest");
                thread.setDaemon(true);
                return thread;
            });
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
        public boolean awaitTermination(final long timeout, final @NotNull TimeUnit unit) throws InterruptedException {
            return this.delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(final @NotNull Runnable command) {
            this.delegate.execute(() -> {
                this.runCount.incrementAndGet();
                command.run();
            });
        }

        int getRunCount() {
            return this.runCount.get();
        }
    }

    private static final class TestLogHandler extends Handler {

        private final CopyOnWriteArrayList<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
            // No-op
        }

        @Override
        public void close() {
            // No-op
        }

        List<String> messages(final Level level) {
            return this.records.stream()
                    .filter(record -> record.getLevel().equals(level))
                    .map(LogRecord::getMessage)
                    .toList();
        }

        List<LogRecord> records(final Level level) {
            return this.records.stream()
                    .filter(record -> record.getLevel().equals(level))
                    .toList();
        }
    }
}
