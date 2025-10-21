package com.raindropcentral.core;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.core.api.bukkit.RCoreBukkitServiceRegistrar;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.server.RServer;
import com.raindropcentral.core.database.repository.RPlayerRepository;
import com.raindropcentral.core.database.repository.RPlayerStatisticRepository;
import com.raindropcentral.core.database.repository.RServerRepository;
import com.raindropcentral.core.database.repository.RStatisticRepository;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.RPlatform;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RCoreFreeImplTest {

    private ServerMock server;
    private TestRCoreFreePlugin plugin;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.load(TestRCoreFreePlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void lifecycleRegistersServicesAndRunsSyncTasksOnMainThread() throws Exception {
        final AtomicReference<RPlatform> platformRef = new AtomicReference<>();
        final AtomicReference<Thread> componentThread = new AtomicReference<>();
        final AtomicReference<Thread> metricsThread = new AtomicReference<>();

        final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        try (MockedStatic<RCoreBukkitServiceRegistrar> registrar = mockStatic(RCoreBukkitServiceRegistrar.class, CALLS_REAL_METHODS);
             MockedConstruction<RPlatform> platformConstruction = mockConstruction(RPlatform.class, (mock, context) -> {
                 platformRef.set(mock);
                 when(mock.initialize()).thenReturn(CompletableFuture.completedFuture(null));
                 when(mock.getEntityManagerFactory()).thenReturn(entityManagerFactory);
                 doAnswer(invocation -> {
                     metricsThread.set(Thread.currentThread());
                     return null;
                 }).when(mock).initializeMetrics(25809);
             });
             MockedConstruction<CommandFactory> commandFactoryConstruction = mockConstruction(CommandFactory.class, (mock, context) ->
                     doAnswer(invocation -> {
                         componentThread.set(Thread.currentThread());
                         return null;
                     }).when(mock).registerAllCommandsAndListeners()
             );
             MockedConstruction<RPlayerRepository> playerRepositoryConstruction = mockConstruction(RPlayerRepository.class);
             MockedConstruction<RPlayerStatisticRepository> playerStatisticRepositoryConstruction = mockConstruction(RPlayerStatisticRepository.class);
             MockedConstruction<RStatisticRepository> statisticRepositoryConstruction = mockConstruction(RStatisticRepository.class);
             MockedConstruction<RServerRepository> serverRepositoryConstruction = mockConstruction(RServerRepository.class, (mock, context) ->
                     when(mock.createAsync(any(RServer.class))).thenAnswer(invocation -> {
                         final RServer serverEntity = invocation.getArgument(0);
                         return CompletableFuture.completedFuture(serverEntity);
                     })
             )) {

            final RCoreFreeImpl impl = new RCoreFreeImpl(this.plugin);

            impl.onLoad();

            registrar.verify(() -> RCoreBukkitServiceRegistrar.register(this.plugin, impl, ServicePriority.Normal));

            assertNotNull(platformRef.get(), "Platform should be created during onLoad");
            assertSame(platformRef.get(), impl.getPlatform(), "Platform reference should be cached on the implementation");

            final RegisteredServiceProvider<RCoreService> registration = this.server.getServicesManager().getRegistration(RCoreService.class);
            assertNotNull(registration, "Service registration should be visible after onLoad");
            assertSame(this.plugin, registration.getPlugin(), "Registered provider must belong to the plugin");

            impl.onEnable();

            final CompletableFuture<Void> enableFuture = readEnableFuture(impl);
            assertNotNull(enableFuture, "Enable future should be created when the lifecycle starts");
            enableFuture.orTimeout(1, TimeUnit.SECONDS);
            while (!enableFuture.isDone()) {
                this.server.getScheduler().performOneTick();
            }
            enableFuture.join();

            assertNotNull(componentThread.get(), "Component initialization should run via runSync");
            assertSame(Thread.currentThread(), componentThread.get(), "runSync should marshal work back to the primary thread");

            assertNotNull(metricsThread.get(), "Metrics initialization should be executed");
            assertSame(Thread.currentThread(), metricsThread.get(), "Metrics must initialize on the primary thread");

            final Map<String, Boolean> pluginDetections = impl.getEnabledSupportedPlugins();
            assertEquals(15, pluginDetections.size(), "All supported plugins should be evaluated");

            impl.onDisable();

            registrar.verify(() -> RCoreBukkitServiceRegistrar.unregister(this.plugin));
            assertTrue(impl.getExecutor().isShutdown(), "Executor should be shut down during disable");
            assertNull(this.server.getServicesManager().getRegistration(RCoreService.class), "Service should be unregistered after disable");
        }
    }

    @Test
    void backendMethodsDelegateToRepositoriesAndEnforceNullGuards() throws Exception {
        final RCoreFreeImpl impl = new RCoreFreeImpl(this.plugin);

        final RPlayerRepository playerRepository = mock(RPlayerRepository.class);
        setField(impl, "rPlayerRepository", playerRepository);

        final UUID uniqueId = UUID.randomUUID();
        final RPlayer player = new RPlayer(uniqueId, "Lifecycle");

        final AtomicReference<Thread> uuidThread = new AtomicReference<>();
        final CompletableFuture<Optional<RPlayer>> uuidFuture = CompletableFuture.supplyAsync(() -> {
            uuidThread.set(Thread.currentThread());
            return Optional.of(player);
        }, impl.getExecutor());
        when(playerRepository.findByUuidAsync(uniqueId)).thenReturn(uuidFuture);

        final CompletableFuture<Optional<RPlayer>> resolvedByUuid = impl.findByUuidAsync(uniqueId);
        assertSame(uuidFuture, resolvedByUuid, "Backend should return the repository future");
        assertEquals(Optional.of(player), resolvedByUuid.join());
        assertNotNull(uuidThread.get(), "UUID lookup should execute asynchronously");
        assertTrue(uuidThread.get().isVirtual(), "UUID lookup should run on the virtual-thread executor");

        assertThrows(NullPointerException.class, () -> impl.findByUuidAsync(null), "Null UUIDs must be rejected");

        final AtomicReference<Thread> nameThread = new AtomicReference<>();
        final CompletableFuture<Optional<RPlayer>> nameFuture = CompletableFuture.supplyAsync(() -> {
            nameThread.set(Thread.currentThread());
            return Optional.of(player);
        }, impl.getExecutor());
        when(playerRepository.findByNameAsync("Lifecycle")).thenReturn(nameFuture);

        final CompletableFuture<Optional<RPlayer>> resolvedByName = impl.findByNameAsync("Lifecycle");
        assertSame(nameFuture, resolvedByName, "Backend should delegate name lookups to the repository");
        assertEquals(Optional.of(player), resolvedByName.join());
        assertNotNull(nameThread.get(), "Name lookup should execute asynchronously");
        assertTrue(nameThread.get().isVirtual(), "Name lookup should run on the variant executor");

        assertThrows(NullPointerException.class, () -> impl.findByNameAsync(null), "Null player names must be rejected");

        final AtomicReference<Thread> createThread = new AtomicReference<>();
        final CompletableFuture<RPlayer> createFuture = CompletableFuture.supplyAsync(() -> {
            createThread.set(Thread.currentThread());
            return player;
        }, impl.getExecutor());
        when(playerRepository.createAsync(player)).thenReturn(createFuture);

        final CompletableFuture<RPlayer> created = impl.createAsync(player);
        assertSame(createFuture, created, "Create should use the repository future");
        assertEquals(player, created.join());
        assertNotNull(createThread.get(), "Create should execute asynchronously");
        assertTrue(createThread.get().isVirtual(), "Create should run on the variant executor");

        assertThrows(NullPointerException.class, () -> impl.createAsync(null), "Null players must be rejected on create");

        final AtomicReference<Thread> updateThread = new AtomicReference<>();
        final CompletableFuture<RPlayer> updateFuture = CompletableFuture.supplyAsync(() -> {
            updateThread.set(Thread.currentThread());
            return player;
        }, impl.getExecutor());
        when(playerRepository.updateAsync(player)).thenReturn(updateFuture);

        final CompletableFuture<RPlayer> updated = impl.updateAsync(player);
        assertSame(updateFuture, updated, "Update should use the repository future");
        assertEquals(player, updated.join());
        assertNotNull(updateThread.get(), "Update should execute asynchronously");
        assertTrue(updateThread.get().isVirtual(), "Update should run on the variant executor");

        assertThrows(NullPointerException.class, () -> impl.updateAsync(null), "Null players must be rejected on update");

        impl.onDisable();
    }

    private CompletableFuture<Void> readEnableFuture(final RCoreFreeImpl impl) throws Exception {
        final Field field = RCoreFreeImpl.class.getDeclaredField("enableFuture");
        field.setAccessible(true);
        return (CompletableFuture<Void>) field.get(impl);
    }

    private void setField(final RCoreFreeImpl impl, final String name, final Object value) throws Exception {
        final Field field = RCoreFreeImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(impl, value);
    }

    @Plugin(name = "TestRCoreFree")
    public static class TestRCoreFreePlugin extends RCoreFree {
        @Override
        public void onLoad() {
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable() {
        }
    }
}
