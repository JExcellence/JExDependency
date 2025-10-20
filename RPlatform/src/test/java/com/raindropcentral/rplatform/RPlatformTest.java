package com.raindropcentral.rplatform;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.raindropcentral.rplatform.placeholder.PAPIHook;
import com.raindropcentral.rplatform.metrics.MetricsManager;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

class RPlatformTest {

    private TestPlatformPlugin plugin;
    private RPlatform platform;
    private ImmediateScheduler scheduler;
    private TestLogHandler platformLogHandler;
    private TestLogHandler pluginLogHandler;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(TestPlatformPlugin.class);
        MockBukkit.load(PlaceholderAPIStub.class);
        PAPIHook.reset();

        platform = new RPlatform(plugin);
        scheduler = new ImmediateScheduler();
        setField(platform, "scheduler", ISchedulerAdapter.class, scheduler);

        platformLogHandler = new TestLogHandler();
        pluginLogHandler = new TestLogHandler();
        platform.getLogger().getJavaLogger().addHandler(platformLogHandler);
        plugin.getLogger().addHandler(pluginLogHandler);
    }

    @AfterEach
    void tearDown() {
        platform.getLogger().getJavaLogger().removeHandler(platformLogHandler);
        plugin.getLogger().removeHandler(pluginLogHandler);
        MockBukkit.unmock();
    }

    @Test
    void initializeProvisionsDatabaseAndSetsState() {
        final EntityManagerFactory factory = Mockito.mock(EntityManagerFactory.class);
        try (MockedConstruction<JEHibernate> ignored = Mockito.mockConstruction(
                JEHibernate.class,
                (mock, context) -> Mockito.when(mock.getEntityManagerFactory()).thenReturn(factory)
        )) {
            CompletableFuture<Void> initialization = platform.initialize();
            initialization.join();
        }

        Path databaseFile = plugin.getDataFolder().toPath().resolve("database").resolve("hibernate.properties");
        assertTrue(Files.exists(databaseFile), "hibernate.properties should be saved during initialization");
        assertTrue(platform.isInitialized(), "Initialization flag should be true");
        assertNotNull(platform.getTranslationManager(), "TranslationManager should be created");
        assertNotNull(platform.getCommandUpdater(), "CommandUpdater should be created");
        assertSame(factory, platform.getEntityManagerFactory(), "EntityManagerFactory should originate from JEHibernate mock");
        assertTrue(scheduler.getAsyncInvocations() > 0, "Scheduler should execute async initialization work");
        assertTrue(platformLogHandler.containsMessage("Initializing RPlatform"));
        assertTrue(platformLogHandler.containsMessage("RPlatform initialized successfully"));
    }

    @Test
    void initializeMetricsCreatesManagerAndLogs() {
        platform.initializeMetrics(1234);

        MetricsManagerAccessor metricsAccessor = new MetricsManagerAccessor(platform);
        assertTrue(metricsAccessor.isMetricsManagerPresent(), "MetricsManager should be created when service ID is positive");
        assertTrue(platformLogHandler.containsMessage("Metrics initialized with service ID: 1234"));
    }

    @Test
    void initializePlaceholdersRegistersAndShutdownUnregisters() {
        platform.initializePlaceholders("test");

        assertEquals(1, PAPIHook.getRegisterCalls(), "Placeholder registration should invoke the stub expansion");
        assertTrue(platformLogHandler.containsMessage("PlaceholderAPI integration initialized"));

        platform.shutdown();

        assertEquals(1, PAPIHook.getUnregisterCalls(), "Shutdown should unregister the placeholder expansion");
        assertTrue(platformLogHandler.containsMessage("Shutting down RPlatform"));
    }

    @Test
    void detectPremiumVersionSetsFlag() {
        assertFalse(platform.isPremiumVersion(), "Premium flag should default to false");

        platform.detectPremiumVersion(RPlatformTest.class, "premium-marker.txt");

        assertTrue(platform.isPremiumVersion(), "Detecting the premium marker should flip the flag");
        assertTrue(platformLogHandler.containsMessage("Premium version detected"));
    }

    @Test
    void gettersExposePlatformState() {
        assertSame(plugin, platform.getPlugin());
        assertNotNull(platform.getPlatformAPI());
        assertNotNull(platform.getPlatformType());
        assertSame(scheduler, platform.getScheduler());
        assertNotNull(platform.getServiceRegistry());
        assertNotNull(platform.getLogger());
        assertSame(plugin.getDataFolder(), platform.getPlugin().getDataFolder());
    }

    private static <T> void setField(final Object target, final String fieldName, final Class<T> fieldType, final T value) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(target.getClass(), MethodHandles.lookup());
            VarHandle handle = lookup.findVarHandle(target.getClass(), fieldName, fieldType);
            handle.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set field: " + fieldName, e);
        }
    }

    private static final class MetricsManagerAccessor {

        private final MetricsManager metricsManager;

        private MetricsManagerAccessor(final RPlatform platform) {
            this.metricsManager = getField(platform, "metricsManager", MetricsManager.class);
        }

        boolean isMetricsManagerPresent() {
            return metricsManager != null;
        }
    }

    private static <T> T getField(final Object target, final String fieldName, final Class<T> fieldType) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(target.getClass(), MethodHandles.lookup());
            VarHandle handle = lookup.findVarHandle(target.getClass(), fieldName, fieldType);
            @SuppressWarnings("unchecked")
            T value = (T) handle.get(target);
            return value;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read field: " + fieldName, e);
        }
    }

    private static final class ImmediateScheduler implements ISchedulerAdapter {

        private int asyncInvocations;

        @Override
        public void runSync(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runAsync(@NotNull Runnable task) {
            asyncInvocations++;
            task.run();
        }

        @Override
        public void runDelayed(@NotNull Runnable task, long delayTicks) {
            task.run();
        }

        @Override
        public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
            task.run();
        }

        @Override
        public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runAtLocation(@NotNull org.bukkit.Location location, @NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runGlobal(@NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                task.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
            return future;
        }

        int getAsyncInvocations() {
            return asyncInvocations;
        }
    }

    public static class TestPlatformPlugin extends JavaPlugin {

        private Path dataFolder;

        @Override
        public void onLoad() {
            ensureDataFolder();
        }

        @Override
        public void onDisable() {
            if (dataFolder != null) {
                try {
                    Files.walkFileTree(dataFolder, new SimpleFileVisitor<>() {
                        @Override
                        public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return java.nio.file.FileVisitResult.CONTINUE;
                        }

                        @Override
                        public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return java.nio.file.FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public @NotNull File getDataFolder() {
            ensureDataFolder();
            return dataFolder.toFile();
        }

        private void ensureDataFolder() {
            if (dataFolder == null) {
                try {
                    dataFolder = Files.createTempDirectory("rplatform-test");
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to create temp data folder", e);
                }
            }
        }
    }

    public static class PlaceholderAPIStub extends JavaPlugin {
    }

    private static final class TestLogHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null && record.getMessage() != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        boolean containsMessage(final String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }
    }
}
