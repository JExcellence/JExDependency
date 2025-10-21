package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared contract tests verifying the invariants expressed by {@link RCoreBackend}.
 */
class RCoreBackendContractTest {

    @Nested
    @DisplayName("Null guard invariants")
    class NullGuardContract {

        @Test
        @DisplayName("findByUuidAsync rejects null identifiers before scheduling")
        void findByUuidAsyncRejectsNullInput() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();

                assertThrows(NullPointerException.class, () -> backend.findByUuidAsync(null));
                assertFalse(backend.wasInvoked("findByUuidAsync"), "Null invocation should not reach executor");
            }
        }

        @Test
        @DisplayName("findByNameAsync rejects null names before scheduling")
        void findByNameAsyncRejectsNullInput() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();

                assertThrows(NullPointerException.class, () -> backend.findByNameAsync(null));
                assertFalse(backend.wasInvoked("findByNameAsync"), "Null invocation should not reach executor");
            }
        }

        @Test
        @DisplayName("createAsync rejects null players before scheduling")
        void createAsyncRejectsNullPlayer() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();

                assertThrows(NullPointerException.class, () -> backend.createAsync(null));
                assertFalse(backend.wasInvoked("createAsync"), "Null invocation should not reach executor");
            }
        }

        @Test
        @DisplayName("updateAsync rejects null players before scheduling")
        void updateAsyncRejectsNullPlayer() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();

                assertThrows(NullPointerException.class, () -> backend.updateAsync(null));
                assertFalse(backend.wasInvoked("updateAsync"), "Null invocation should not reach executor");
            }
        }
    }

    @Nested
    @DisplayName("Executor propagation invariants")
    class ExecutorContract {

        @Test
        @DisplayName("Executor reference must be stable and non-null")
        void executorMustBeStableAndNonNull() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();

                assertNotNull(backend.getExecutor(), "Backend must expose a non-null executor");
                assertSame(fixture.executor(), backend.getExecutor(), "Backend must reuse its declared executor");
            }
        }

        @Test
        @DisplayName("findByUuidAsync completes using the backend executor")
        void findByUuidAsyncRunsOnBackendExecutor() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();
                RPlayer player = new RPlayer(UUID.randomUUID(), "ContractUser");
                backend.registerPlayer(player);

                Optional<RPlayer> result = await(backend.findByUuidAsync(player.getUniqueId()));

                assertTrue(result.isPresent(), "Contract backend should surface stored player instances");
                assertSame(player, result.orElseThrow());
                assertThreadAffinity(backend, "findByUuidAsync");
            }
        }

        @Test
        @DisplayName("findByNameAsync completes using the backend executor")
        void findByNameAsyncRunsOnBackendExecutor() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();
                RPlayer player = new RPlayer(UUID.randomUUID(), "ContractUserTwo");
                backend.registerPlayer(player);

                Optional<RPlayer> result = await(backend.findByNameAsync(player.getPlayerName()));

                assertTrue(result.isPresent(), "Contract backend should surface stored player instances");
                assertSame(player, result.orElseThrow());
                assertThreadAffinity(backend, "findByNameAsync");
            }
        }

        @Test
        @DisplayName("createAsync resolves using the backend executor")
        void createAsyncRunsOnBackendExecutor() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();
                RPlayer player = new RPlayer(UUID.randomUUID(), "ContractNew");

                RPlayer created = await(backend.createAsync(player));

                assertSame(player, created);
                assertThreadAffinity(backend, "createAsync");
                assertTrue(backend.findByUuidAsync(player.getUniqueId()).join().isPresent(),
                        "Created player should be discoverable by UUID");
            }
        }

        @Test
        @DisplayName("updateAsync resolves using the backend executor")
        void updateAsyncRunsOnBackendExecutor() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();
                RPlayer player = new RPlayer(UUID.randomUUID(), "ContractExisting");
                backend.registerPlayer(player);

                RPlayer updated = await(backend.updateAsync(player));

                assertSame(player, updated);
                assertThreadAffinity(backend, "updateAsync");
            }
        }
    }

    @Nested
    @DisplayName("Optional lookup semantics")
    class OptionalSemanticsContract {

        @Test
        @DisplayName("findByUuidAsync returns Optional.empty when the player is missing")
        void findByUuidAsyncReturnsEmptyWhenMissing() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();
                UUID missingId = UUID.randomUUID();

                Optional<RPlayer> result = await(backend.findByUuidAsync(missingId));

                assertTrue(result.isEmpty(), "Lookup APIs must return Optional.empty for missing players");
                assertThreadAffinity(backend, "findByUuidAsync");
            }
        }

        @Test
        @DisplayName("findByNameAsync normalizes lookup keys and preserves Optional contract")
        void findByNameAsyncReturnsExpectedOptional() {
            try (BackendFixture fixture = newBackendFixture()) {
                InstrumentedBackend backend = fixture.backend();
                RPlayer player = new RPlayer(UUID.randomUUID(), "ContractOpt");
                backend.registerPlayer(player);

                Optional<RPlayer> result = await(backend.findByNameAsync(player.getPlayerName().toLowerCase(Locale.ROOT)));

                assertTrue(result.isPresent(), "Lookup APIs must return Optional.of when the player exists");
                assertSame(player, result.orElseThrow());
                assertThreadAffinity(backend, "findByNameAsync");
            }
        }
    }

    private static void assertThreadAffinity(InstrumentedBackend backend, String methodName) {
        List<String> threads = backend.threadsFor(methodName);
        assertFalse(threads.isEmpty(), "No threads were captured for method: " + methodName);
        for (String threadName : threads) {
            assertEquals(backend.executorThreadName(), threadName,
                    "Async pipeline should complete on the backend executor");
        }
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for future completion", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new AssertionError("Failed to complete future", ex);
        }
    }

    private static BackendFixture newBackendFixture() {
        String executorThreadName = "rcore-backend-contract-" + UUID.randomUUID();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(executorThreadName);
            thread.setDaemon(true);
            return thread;
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        return new BackendFixture(executorThreadName, executor, new InstrumentedBackend(executor, executorThreadName));
    }

    private record BackendFixture(String executorThreadName, ExecutorService executor, InstrumentedBackend backend)
            implements AutoCloseable {

        BackendFixture {
            Objects.requireNonNull(executorThreadName, "executorThreadName");
            Objects.requireNonNull(executor, "executor");
            Objects.requireNonNull(backend, "backend");
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }

    private static final class InstrumentedBackend implements RCoreBackend {

        private final String executorThreadName;
        private final ExecutorService executor;
        private final Map<String, List<String>> threadsByMethod = new ConcurrentHashMap<>();
        private final Map<UUID, RPlayer> playersById = new ConcurrentHashMap<>();
        private final Map<String, RPlayer> playersByName = new ConcurrentHashMap<>();
        private final Map<String, Boolean> invokedMethods = new ConcurrentHashMap<>();

        private InstrumentedBackend(ExecutorService executor, String executorThreadName) {
            this.executor = Objects.requireNonNull(executor, "executor");
            this.executorThreadName = Objects.requireNonNull(executorThreadName, "executorThreadName");
        }

        @Override
        public ExecutorService getExecutor() {
            return executor;
        }

        @Override
        public CompletableFuture<Optional<RPlayer>> findByUuidAsync(UUID uniqueId) {
            Objects.requireNonNull(uniqueId, "uniqueId");
            invokedMethods.put("findByUuidAsync", Boolean.TRUE);
            return CompletableFuture.supplyAsync(() -> {
                recordThread("findByUuidAsync");
                return Optional.ofNullable(playersById.get(uniqueId));
            }, executor);
        }

        @Override
        public CompletableFuture<Optional<RPlayer>> findByNameAsync(String playerName) {
            Objects.requireNonNull(playerName, "playerName");
            invokedMethods.put("findByNameAsync", Boolean.TRUE);
            return CompletableFuture.supplyAsync(() -> {
                recordThread("findByNameAsync");
                return Optional.ofNullable(playersByName.get(playerName.toLowerCase(Locale.ROOT)));
            }, executor);
        }

        @Override
        public CompletableFuture<RPlayer> createAsync(RPlayer player) {
            Objects.requireNonNull(player, "player");
            invokedMethods.put("createAsync", Boolean.TRUE);
            return CompletableFuture.supplyAsync(() -> {
                recordThread("createAsync");
                registerPlayer(player);
                return player;
            }, executor);
        }

        @Override
        public CompletableFuture<RPlayer> updateAsync(RPlayer player) {
            Objects.requireNonNull(player, "player");
            invokedMethods.put("updateAsync", Boolean.TRUE);
            return CompletableFuture.supplyAsync(() -> {
                recordThread("updateAsync");
                registerPlayer(player);
                return player;
            }, executor);
        }

        private void registerPlayer(RPlayer player) {
            playersById.put(player.getUniqueId(), player);
            playersByName.put(player.getPlayerName().toLowerCase(Locale.ROOT), player);
        }

        private void recordThread(String methodName) {
            threadsByMethod.computeIfAbsent(methodName, key -> Collections.synchronizedList(new ArrayList<>()))
                    .add(Thread.currentThread().getName());
        }

        private boolean wasInvoked(String methodName) {
            return Boolean.TRUE.equals(invokedMethods.get(methodName));
        }

        private List<String> threadsFor(String methodName) {
            return threadsByMethod.containsKey(methodName)
                    ? List.copyOf(threadsByMethod.get(methodName))
                    : List.of();
        }

        private String executorThreadName() {
            return executorThreadName;
        }
    }
}
