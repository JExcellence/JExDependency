package com.raindropcentral.core.api.bukkit;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.core.api.RCoreBackend;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCoreBukkitServiceRegistrarTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private FakeBackend backend;
    private Logger logger;
    private CapturingHandler handler;
    private List<LogRecord> logRecords;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.backend = new FakeBackend();
        this.logRecords = new CopyOnWriteArrayList<>();
        this.logger = CentralLogger.getLogger(RCoreBukkitServiceRegistrar.class);
        this.handler = new CapturingHandler(this.logRecords);
        this.handler.setLevel(Level.ALL);
        this.logger.addHandler(this.handler);
        this.logger.setLevel(Level.ALL);
    }

    @AfterEach
    void tearDown() {
        if (this.logger != null && this.handler != null) {
            this.logger.removeHandler(this.handler);
        }
        if (this.backend != null) {
            this.backend.shutdown();
        }
        MockBukkit.unmock();
    }

    @Test
    void registerPublishesServiceAndLogs() {
        ServicePriority priority = ServicePriority.High;

        RCoreService service = RCoreBukkitServiceRegistrar.register(this.plugin, this.backend, priority);

        assertNotNull(service, "Register should return the adapter instance");
        assertEquals(RCoreAdapter.class, service.getClass(), "Registrar must expose the RCoreAdapter implementation");

        ServicesManager services = this.server.getServicesManager();
        RegisteredServiceProvider<RCoreService> registration = services.getRegistration(RCoreService.class);

        assertNotNull(registration, "Registration should be visible through the ServicesManager");
        assertSame(this.plugin, registration.getPlugin(), "Registered provider must belong to the supplied plugin");
        assertSame(service, registration.getProvider(), "Registered provider must match the returned adapter");
        assertEquals(priority, registration.getPriority(), "Registration priority should mirror the requested value");

        assertTrue(this.logRecords.stream().anyMatch(record ->
                record.getLevel() == Level.INFO && record.getMessage().contains("Registered RCoreService")),
                "Registration should emit an info log entry");
    }

    @Test
    void unregisterRemovesServicesAndLogs() {
        RCoreBukkitServiceRegistrar.register(this.plugin, this.backend, ServicePriority.Normal);
        this.logRecords.clear();

        RCoreBukkitServiceRegistrar.unregister(this.plugin);

        ServicesManager services = this.server.getServicesManager();
        assertNull(services.getRegistration(RCoreService.class),
                "All RCoreService registrations should be cleared after unregister");

        assertTrue(this.logRecords.stream().anyMatch(record ->
                record.getLevel() == Level.INFO && record.getMessage().contains("Unregistered services")),
                "Unregister should emit an info log entry");
    }

    @Test
    void registerRequiresPlugin() {
        assertThrows(NullPointerException.class,
                () -> RCoreBukkitServiceRegistrar.register(null, this.backend, ServicePriority.Normal));
    }

    @Test
    void registerRequiresBackend() {
        assertThrows(NullPointerException.class,
                () -> RCoreBukkitServiceRegistrar.register(this.plugin, null, ServicePriority.Normal));
    }

    @Test
    void registerRequiresPriority() {
        assertThrows(NullPointerException.class,
                () -> RCoreBukkitServiceRegistrar.register(this.plugin, this.backend, null));
    }

    @Test
    void unregisterRequiresPlugin() {
        assertThrows(NullPointerException.class, () -> RCoreBukkitServiceRegistrar.unregister(null));
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogRecord> records;

        private CapturingHandler(final List<LogRecord> records) {
            this.records = records;
        }

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
    }

    private static final class FakeBackend implements RCoreBackend {

        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        @Override
        public ExecutorService getExecutor() {
            return this.executor;
        }

        @Override
        public CompletableFuture<Optional<RPlayer>> findByUuidAsync(final UUID uniqueId) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<Optional<RPlayer>> findByNameAsync(final String playerName) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<RPlayer> createAsync(final RPlayer player) {
            return CompletableFuture.completedFuture(player);
        }

        @Override
        public CompletableFuture<RPlayer> updateAsync(final RPlayer player) {
            return CompletableFuture.completedFuture(player);
        }

        void shutdown() {
            this.executor.shutdownNow();
        }
    }
}
