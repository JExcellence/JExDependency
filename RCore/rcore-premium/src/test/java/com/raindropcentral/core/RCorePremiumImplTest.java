package com.raindropcentral.core;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.server.RServer;
import com.raindropcentral.core.database.repository.RPlayerRepository;
import com.raindropcentral.core.database.repository.RPlayerStatisticRepository;
import com.raindropcentral.core.database.repository.RServerRepository;
import com.raindropcentral.core.database.repository.RStatisticRepository;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RCorePremiumImplTest {

    @Test
    void lifecycleRegistersServicesAfterIntegrationsAndTearsDownOnDisable() {
        final RCorePremium plugin = mock(RCorePremium.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("Lifecycle"));

        final ServicesManager servicesManager = mock(ServicesManager.class);
        final PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.isPluginEnabled(anyString())).thenReturn(false);

        final Server server = mock(Server.class);
        when(server.getServicesManager()).thenReturn(servicesManager);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getName()).thenReturn("UnitTest");
        when(plugin.getServer()).thenReturn(server);

        final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        final AtomicReference<RCorePremiumImpl> implRef = new AtomicReference<>();

        try (MockedStatic<CentralLogger> centralLogger = mockStatic(CentralLogger.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedConstruction<RPlatform> platforms = mockConstruction(RPlatform.class, (mockPlatform, context) -> {
                 when(mockPlatform.getEntityManagerFactory()).thenReturn(entityManagerFactory);
             });
             MockedConstruction<CommandFactory> commandFactories = mockConstruction(CommandFactory.class);
             MockedConstruction<RPlayerRepository> playerRepos = mockConstruction(RPlayerRepository.class);
             MockedConstruction<RPlayerStatisticRepository> statisticRepos = mockConstruction(RPlayerStatisticRepository.class);
             MockedConstruction<RStatisticRepository> metadataRepos = mockConstruction(RStatisticRepository.class);
             MockedConstruction<RServerRepository> serverRepos = mockConstruction(RServerRepository.class, (mockRepo, context) -> {
                 when(mockRepo.createAsync(any(RServer.class))).thenAnswer(invocation -> {
                     final RServer input = invocation.getArgument(0, RServer.class);
                     return CompletableFuture.completedFuture(input);
                 });
             })) {

            centralLogger.when(() -> CentralLogger.initialize(any())).thenAnswer(invocation -> null);
            bukkit.when(Bukkit::getServer).thenReturn(server);

            doAnswer(invocation -> {
                final RCorePremiumImpl impl = implRef.get();
                assertNotNull(impl, "Implementation should be initialized before registering services");
                assertEquals(15, impl.getEnabledSupportedPlugins().size(),
                        "Service registration must wait until integrations are scanned");
                return null;
            }).when(servicesManager).register(eq(RCoreService.class), any(RCoreService.class), eq(plugin), eq(ServicePriority.Normal));

            final RCorePremiumImpl impl = new RCorePremiumImpl(plugin);
            implRef.set(impl);

            impl.onLoad();
            assertEquals(1, platforms.constructed().size(), "Platform should be created during load");
            final RPlatform platform = platforms.constructed().getFirst();
            verify(platform, never()).initialize();

            impl.onEnable();

            assertEquals(15, impl.getEnabledSupportedPlugins().size(),
                    "All supported integrations should be recorded before service publication");

            final CommandFactory commandFactory = commandFactories.constructed().getFirst();
            verify(commandFactory).registerAllCommandsAndListeners();
            verify(platform).initialize();
            verify(platform).initializeMetrics(25809);

            final ArgumentCaptor<RCoreService> serviceCaptor = ArgumentCaptor.forClass(RCoreService.class);
            verify(servicesManager).register(eq(RCoreService.class), serviceCaptor.capture(), eq(plugin), eq(ServicePriority.Normal));

            final RServerRepository serverRepository = serverRepos.constructed().getFirst();
            verify(serverRepository).createAsync(any(RServer.class));

            impl.onDisable();

            verify(servicesManager).unregister(eq(RCoreService.class), eq(serviceCaptor.getValue()));
            assertTrue(impl.getExecutor().isShutdown(), "Executor should be shutdown during disable");
        }
    }

    @Test
    void backendApiGuardsInputsAndDelegatesToRepositories() {
        final RCorePremium plugin = mock(RCorePremium.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("Backend"));

        final ServicesManager servicesManager = mock(ServicesManager.class);
        final PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.isPluginEnabled(anyString())).thenReturn(false);

        final Server server = mock(Server.class);
        when(server.getServicesManager()).thenReturn(servicesManager);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getName()).thenReturn("BackendTest");
        when(plugin.getServer()).thenReturn(server);

        final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        final AtomicReference<ExecutorService> repoExecutor = new AtomicReference<>();
        final AtomicReference<String> byUuidThread = new AtomicReference<>();
        final AtomicReference<String> byNameThread = new AtomicReference<>();
        final AtomicReference<String> createThread = new AtomicReference<>();
        final AtomicReference<String> updateThread = new AtomicReference<>();

        try (MockedStatic<CentralLogger> centralLogger = mockStatic(CentralLogger.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedConstruction<RPlatform> platforms = mockConstruction(RPlatform.class, (mockPlatform, context) -> {
                 when(mockPlatform.getEntityManagerFactory()).thenReturn(entityManagerFactory);
             });
             MockedConstruction<CommandFactory> commandFactories = mockConstruction(CommandFactory.class);
             MockedConstruction<RPlayerRepository> playerRepos = mockConstruction(RPlayerRepository.class, (mockRepo, context) -> {
                 final ExecutorService executor = (ExecutorService) context.arguments().get(0);
                 repoExecutor.set(executor);
                 when(mockRepo.findByUuidAsync(any(UUID.class))).thenAnswer(invocation -> scheduleOptional(executor, byUuidThread, invocation.getArgument(0, UUID.class)));
                 when(mockRepo.findByNameAsync(anyString())).thenAnswer(invocation -> scheduleOptional(executor, byNameThread, invocation.getArgument(0, String.class)));
                 when(mockRepo.createAsync(any(RPlayer.class))).thenAnswer(invocation -> schedulePlayer(executor, createThread, invocation.getArgument(0, RPlayer.class)));
                 when(mockRepo.updateAsync(any(RPlayer.class))).thenAnswer(invocation -> schedulePlayer(executor, updateThread, invocation.getArgument(0, RPlayer.class)));
             });
             MockedConstruction<RPlayerStatisticRepository> statisticRepos = mockConstruction(RPlayerStatisticRepository.class);
             MockedConstruction<RStatisticRepository> metadataRepos = mockConstruction(RStatisticRepository.class);
             MockedConstruction<RServerRepository> serverRepos = mockConstruction(RServerRepository.class, (mockRepo, context) -> {
                 when(mockRepo.createAsync(any(RServer.class))).thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, RServer.class)));
             })) {

            centralLogger.when(() -> CentralLogger.initialize(any())).thenAnswer(invocation -> null);
            bukkit.when(Bukkit::getServer).thenReturn(server);

            final RCorePremiumImpl impl = new RCorePremiumImpl(plugin);

            assertThrows(NullPointerException.class, () -> impl.findByUuidAsync(null));
            assertThrows(NullPointerException.class, () -> impl.findByNameAsync(null));
            assertThrows(NullPointerException.class, () -> impl.createAsync(null));
            assertThrows(NullPointerException.class, () -> impl.updateAsync(null));

            assertThrows(IllegalStateException.class, () -> impl.findByNameAsync("BeforeInit"));

            impl.onLoad();
            assertThrows(IllegalStateException.class, () -> impl.createAsync(new RPlayer(UUID.randomUUID(), "PostLoad")));

            impl.onEnable();

            final UUID uniqueId = UUID.randomUUID();
            final RPlayer player = new RPlayer(uniqueId, "Delegation");

            final CompletableFuture<Optional<RPlayer>> byUuidFuture = impl.findByUuidAsync(uniqueId);
            final CompletableFuture<Optional<RPlayer>> byNameFuture = impl.findByNameAsync("Delegation");
            final CompletableFuture<RPlayer> created = impl.createAsync(player);
            final CompletableFuture<RPlayer> updated = impl.updateAsync(player);

            assertEquals(uniqueId, byUuidFuture.join().orElseThrow().getUniqueId());
            assertEquals("Delegation", byNameFuture.join().orElseThrow().getPlayerName());
            assertSame(player, created.join());
            assertSame(player, updated.join());

            assertNotNull(repoExecutor.get(), "Repository executor should be captured during construction");
            assertSame(repoExecutor.get(), impl.getExecutor(), "Repositories must receive the premium executor");

            assertNotNull(byUuidThread.get(), "UUID lookup should capture executor thread");
            assertNotEquals(Thread.currentThread().getName(), byUuidThread.get(), "UUID lookup should execute asynchronously");
            assertNotNull(byNameThread.get(), "Name lookup should capture executor thread");
            assertNotEquals(Thread.currentThread().getName(), byNameThread.get(), "Name lookup should execute asynchronously");
            assertNotNull(createThread.get(), "Create should capture executor thread");
            assertNotEquals(Thread.currentThread().getName(), createThread.get(), "Create should execute asynchronously");
            assertNotNull(updateThread.get(), "Update should capture executor thread");
            assertNotEquals(Thread.currentThread().getName(), updateThread.get(), "Update should execute asynchronously");

            final RPlayerRepository playerRepository = playerRepos.constructed().getFirst();
            verify(playerRepository).findByUuidAsync(uniqueId);
            verify(playerRepository).findByNameAsync("Delegation");
            verify(playerRepository).createAsync(player);
            verify(playerRepository).updateAsync(player);

            impl.onDisable();
        }
    }

    private static CompletableFuture<Optional<RPlayer>> scheduleOptional(final ExecutorService executor,
                                                                         final AtomicReference<String> threadCapture,
                                                                         final Object argument) {
        final CompletableFuture<Optional<RPlayer>> future = new CompletableFuture<>();
        executor.submit(() -> {
            threadCapture.set(Thread.currentThread().getName());
            if (argument instanceof UUID uniqueId) {
                future.complete(Optional.of(new RPlayer(uniqueId, "Delegation")));
            } else if (argument instanceof String name) {
                future.complete(Optional.of(new RPlayer(UUID.randomUUID(), name)));
            } else {
                future.complete(Optional.empty());
            }
        });
        return future;
    }

    private static CompletableFuture<RPlayer> schedulePlayer(final ExecutorService executor,
                                                             final AtomicReference<String> threadCapture,
                                                             final RPlayer player) {
        final CompletableFuture<RPlayer> future = new CompletableFuture<>();
        executor.submit(() -> {
            threadCapture.set(Thread.currentThread().getName());
            future.complete(player);
        });
        return future;
    }
}
