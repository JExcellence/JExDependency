package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RankSystemFactoryTest {

    @Test
    void initializeAsyncExecutesPipelineInOrderAndLogsSummary() throws Exception {
        final ExecutorService executor = new DirectExecutorService();
        final RDQ rdq = mock(RDQ.class);
        when(rdq.getExecutor()).thenReturn(executor);

        final RRankTree rankTree = mock(RRankTree.class);
        final RRank defaultRank = mock(RRank.class);
        when(defaultRank.getIdentifier()).thenReturn("default-rank");

        final Map<String, RRankTree> rankTrees = new HashMap<>();
        rankTrees.put("tree-1", rankTree);
        final Map<String, Map<String, RRank>> ranks = new HashMap<>();
        final Map<String, RRank> treeRanks = new HashMap<>();
        treeRanks.put("rank-1", mock(RRank.class));
        treeRanks.put("rank-2", mock(RRank.class));
        ranks.put("tree-1", treeRanks);

        final RankSystemState loadedState = RankSystemState.builder()
                .rankTrees(rankTrees)
                .ranks(ranks)
                .defaultRank(defaultRank)
                .build();

        final Logger logger = Logger.getLogger(RankSystemFactory.class.getName());
        final TestLogHandler handler = new TestLogHandler();
        final boolean originalUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        try (MockedConstruction<RankConfigurationLoader> loaderConstruction = mockConstruction(RankConfigurationLoader.class);
             MockedConstruction<RankValidationService> validationConstruction = mockConstruction(RankValidationService.class);
             MockedConstruction<RankEntityService> entityConstruction = mockConstruction(RankEntityService.class)) {

            final RankSystemFactory factory = new RankSystemFactory(rdq);

            final RankConfigurationLoader loader = loaderConstruction.constructed().get(0);
            final RankValidationService validator = validationConstruction.constructed().get(0);
            final RankEntityService entityService = entityConstruction.constructed().get(0);

            final CompletableFuture<RankSystemState> loadFuture = new CompletableFuture<>();
            final CompletableFuture<Void> validateConfigFuture = new CompletableFuture<>();
            final CompletableFuture<Void> defaultFuture = new CompletableFuture<>();
            final CompletableFuture<Void> treeFuture = new CompletableFuture<>();
            final CompletableFuture<Void> ranksFuture = new CompletableFuture<>();
            final CompletableFuture<Void> connectionsFuture = new CompletableFuture<>();
            final CompletableFuture<Void> validateSystemFuture = new CompletableFuture<>();

            when(loader.loadAllAsync(executor)).thenReturn(loadFuture);
            when(validator.validateConfigurationsAsync(any(), eq(executor))).thenReturn(validateConfigFuture);
            when(entityService.createDefaultRankAsync(any(), eq(executor))).thenReturn(defaultFuture);
            when(entityService.createRankTreesAsync(any(), eq(executor))).thenReturn(treeFuture);
            when(entityService.createRanksAsync(any(), eq(executor))).thenReturn(ranksFuture);
            when(entityService.establishConnectionsAsync(any(), eq(executor))).thenReturn(connectionsFuture);
            when(validator.validateSystemAsync(any(), eq(executor))).thenReturn(validateSystemFuture);

            final CompletableFuture<Void> initialization = factory.initializeAsync();

            assertFalse(initialization.isDone());
            assertTrue(isInitializing(factory));

            loadFuture.complete(loadedState);
            verify(validator).validateConfigurationsAsync(same(loadedState), eq(executor));
            assertSame(loadedState, currentState(factory));

            validateConfigFuture.complete(null);
            verify(entityService).createDefaultRankAsync(same(loadedState), eq(executor));

            defaultFuture.complete(null);
            verify(entityService).createRankTreesAsync(same(loadedState), eq(executor));

            treeFuture.complete(null);
            verify(entityService).createRanksAsync(same(loadedState), eq(executor));

            ranksFuture.complete(null);
            verify(entityService).establishConnectionsAsync(same(loadedState), eq(executor));

            connectionsFuture.complete(null);
            verify(validator).validateSystemAsync(same(loadedState), eq(executor));

            validateSystemFuture.complete(null);

            assertFalse(initialization.isCompletedExceptionally());
            initialization.join();
            assertFalse(isInitializing(factory));
            assertSame(loadedState, currentState(factory));

            final InOrder inOrder = inOrder(loader, validator, entityService);
            inOrder.verify(loader).loadAllAsync(executor);
            inOrder.verify(validator).validateConfigurationsAsync(same(loadedState), eq(executor));
            inOrder.verify(entityService).createDefaultRankAsync(same(loadedState), eq(executor));
            inOrder.verify(entityService).createRankTreesAsync(same(loadedState), eq(executor));
            inOrder.verify(entityService).createRanksAsync(same(loadedState), eq(executor));
            inOrder.verify(entityService).establishConnectionsAsync(same(loadedState), eq(executor));
            inOrder.verify(validator).validateSystemAsync(same(loadedState), eq(executor));

            final List<LogRecord> records = handler.records();
            assertTrue(records.stream().anyMatch(record -> "Rank system initialization started".equals(record.getMessage())));
            assertTrue(records.stream().anyMatch(record -> "=== Rank System Summary ===".equals(record.getMessage())));
            assertTrue(records.stream().anyMatch(record -> "===========================".equals(record.getMessage())));

            final LogRecord treeRecord = records.stream()
                    .filter(record -> "Rank Trees: {0}".equals(record.getMessage()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(1, ((Number) treeRecord.getParameters()[0]).intValue());

            final LogRecord totalRecord = records.stream()
                    .filter(record -> "Total Ranks: {0}".equals(record.getMessage()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(2, ((Number) totalRecord.getParameters()[0]).intValue());

            final LogRecord defaultRecord = records.stream()
                    .filter(record -> "Default Rank: {0}".equals(record.getMessage()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("default-rank", defaultRecord.getParameters()[0]);
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalUseParentHandlers);
        }
    }

    @Test
    void initializeAsyncResetsStateWhenStageFails() throws Exception {
        final ExecutorService executor = new DirectExecutorService();
        final RDQ rdq = mock(RDQ.class);
        when(rdq.getExecutor()).thenReturn(executor);

        final RankSystemState loadedState = RankSystemState.builder()
                .rankTrees(Map.of("tree-1", mock(RRankTree.class)))
                .ranks(Map.of("tree-1", Map.of("rank-1", mock(RRank.class))))
                .defaultRank(mock(RRank.class))
                .build();

        try (MockedConstruction<RankConfigurationLoader> loaderConstruction = mockConstruction(RankConfigurationLoader.class);
             MockedConstruction<RankValidationService> validationConstruction = mockConstruction(RankValidationService.class);
             MockedConstruction<RankEntityService> entityConstruction = mockConstruction(RankEntityService.class)) {

            final RankSystemFactory factory = new RankSystemFactory(rdq);

            final RankConfigurationLoader loader = loaderConstruction.constructed().get(0);
            final RankValidationService validator = validationConstruction.constructed().get(0);
            final RankEntityService entityService = entityConstruction.constructed().get(0);

            when(loader.loadAllAsync(executor)).thenReturn(CompletableFuture.completedFuture(loadedState));
            when(validator.validateConfigurationsAsync(any(), eq(executor)))
                    .thenReturn(CompletableFuture.completedFuture(null));
            when(entityService.createDefaultRankAsync(any(), eq(executor)))
                    .thenReturn(CompletableFuture.completedFuture(null));
            when(entityService.createRankTreesAsync(any(), eq(executor)))
                    .thenReturn(CompletableFuture.completedFuture(null));
            when(entityService.createRanksAsync(any(), eq(executor)))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("rank creation failed")));

            final CompletableFuture<Void> future = factory.initializeAsync();

            assertTrue(future.isCompletedExceptionally());
            final CompletionException thrown = assertThrows(CompletionException.class, future::join);
            assertTrue(thrown.getCause() instanceof IllegalStateException);
            assertFalse(isInitializing(factory));

            final RankSystemState stateAfterFailure = currentState(factory);
            assertTrue(stateAfterFailure.rankTrees().isEmpty());
            assertTrue(stateAfterFailure.ranks().isEmpty());
            assertNull(stateAfterFailure.defaultRank());

            verify(entityService, never()).establishConnectionsAsync(any(), eq(executor));
            verify(validator, never()).validateSystemAsync(any(), eq(executor));
        }
    }

    @Test
    void accessorsExposeDefensiveCopies() throws Exception {
        final ExecutorService executor = new DirectExecutorService();
        final RDQ rdq = mock(RDQ.class);
        when(rdq.getExecutor()).thenReturn(executor);

        final RRankTree tree = mock(RRankTree.class);
        final RRank defaultRank = mock(RRank.class);

        final Map<String, RRankTree> rankTrees = new HashMap<>();
        rankTrees.put("tree-1", tree);

        final Map<String, Map<String, RRank>> ranks = new HashMap<>();
        final Map<String, RRank> treeRanks = new HashMap<>();
        treeRanks.put("rank-1", mock(RRank.class));
        ranks.put("tree-1", treeRanks);

        final RankSystemState populatedState = RankSystemState.builder()
                .rankTrees(rankTrees)
                .ranks(ranks)
                .defaultRank(defaultRank)
                .build();

        final RankSystemFactory factory = new RankSystemFactory(rdq);
        setState(factory, populatedState);

        assertTrue(factory.isInitialized());

        final Map<String, RRankTree> rankTreeView = factory.getRankTrees();
        assertEquals(rankTrees, rankTreeView);
        assertNotSame(rankTrees, rankTreeView);
        assertThrows(UnsupportedOperationException.class, () -> rankTreeView.put("new-tree", mock(RRankTree.class)));

        final Map<String, Map<String, RRank>> ranksView = factory.getRanks();
        assertEquals(ranks.keySet(), ranksView.keySet());
        assertNotSame(ranks, ranksView);
        assertThrows(UnsupportedOperationException.class, () -> ranksView.put("new-tree", Map.of()));

        final Map<String, RRank> copiedTreeRanks = ranksView.get("tree-1");
        assertEquals(treeRanks, copiedTreeRanks);
        assertNotSame(treeRanks, copiedTreeRanks);
        assertThrows(UnsupportedOperationException.class, () -> copiedTreeRanks.put("extra", mock(RRank.class)));

        assertSame(defaultRank, factory.getDefaultRank());
    }

    private static boolean isInitializing(final RankSystemFactory factory) throws Exception {
        final Field field = RankSystemFactory.class.getDeclaredField("initializing");
        field.setAccessible(true);
        return field.getBoolean(factory);
    }

    private static RankSystemState currentState(final RankSystemFactory factory) throws Exception {
        final Field field = RankSystemFactory.class.getDeclaredField("state");
        field.setAccessible(true);
        return (RankSystemState) field.get(factory);
    }

    private static void setState(final RankSystemFactory factory, final RankSystemState state) throws Exception {
        final Field field = RankSystemFactory.class.getDeclaredField("state");
        field.setAccessible(true);
        field.set(factory, state);
    }

    private static final class DirectExecutorService extends java.util.concurrent.AbstractExecutorService {

        private final AtomicBoolean shutdown = new AtomicBoolean();

        @Override
        public void shutdown() {
            shutdown.set(true);
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown.set(true);
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown.get();
        }

        @Override
        public boolean isTerminated() {
            return shutdown.get();
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return shutdown.get();
        }

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    }

    private static final class TestLogHandler extends Handler {

        private final CopyOnWriteArrayList<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        List<LogRecord> records() {
            return List.copyOf(records);
        }
    }
}
