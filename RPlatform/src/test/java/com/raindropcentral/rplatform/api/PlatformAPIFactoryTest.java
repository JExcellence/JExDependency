package com.raindropcentral.rplatform.api;

import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformAPIFactoryTest {

    private static final String FOLIA_INDICATOR = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String PAPER_INDICATOR = "io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler";
    private static final String FOLIA_IMPL = "com.raindropcentral.rplatform.api.impl.FoliaPlatformAPI";
    private static final String PAPER_IMPL = "com.raindropcentral.rplatform.api.impl.PaperPlatformAPI";
    private static final String SPIGOT_IMPL = "com.raindropcentral.rplatform.api.impl.SpigotPlatformAPI";

    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
    }

    @Test
    void detectPlatformTypeReturnsFoliaWhenIndicatorPresent() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(FOLIA_INDICATOR, Object.class);

        try (MockedStatic<Class> mocked = mockClassForName(answers)) {
            assertEquals(PlatformType.FOLIA, PlatformAPIFactory.detectPlatformType());
            mocked.verify(() -> Class.forName(FOLIA_INDICATOR));
            mocked.verify(() -> Class.forName(PAPER_INDICATOR), never());
        }
    }

    @Test
    void detectPlatformTypeReturnsPaperWhenFoliaMissing() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(PAPER_INDICATOR, Object.class);

        try (MockedStatic<Class> mocked = mockClassForName(answers)) {
            assertEquals(PlatformType.PAPER, PlatformAPIFactory.detectPlatformType());
            mocked.verify(() -> Class.forName(FOLIA_INDICATOR));
            mocked.verify(() -> Class.forName(PAPER_INDICATOR));
        }
    }

    @Test
    void detectPlatformTypeReturnsSpigotWhenNoIndicatorsPresent() throws Exception {
        try (MockedStatic<Class> mocked = mockClassForName(Collections.emptyMap())) {
            assertEquals(PlatformType.SPIGOT, PlatformAPIFactory.detectPlatformType());
            mocked.verify(() -> Class.forName(FOLIA_INDICATOR));
            mocked.verify(() -> Class.forName(PAPER_INDICATOR));
        }
    }

    @Test
    void createReturnsFoliaImplementationWhenDetected() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(FOLIA_INDICATOR, Object.class);
        answers.put(FOLIA_IMPL, DummyFoliaPlatformAPI.class);

        try (MockedStatic<Class> mocked = mockClassForName(answers)) {
            PlatformAPI api = PlatformAPIFactory.create(plugin);
            assertTrue(api instanceof DummyFoliaPlatformAPI);
            mocked.verify(() -> Class.forName(FOLIA_INDICATOR));
            mocked.verify(() -> Class.forName(FOLIA_IMPL));
        }
    }

    @Test
    void createReturnsPaperImplementationWhenDetected() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(PAPER_INDICATOR, Object.class);
        answers.put(PAPER_IMPL, DummyPaperPlatformAPI.class);

        try (MockedStatic<Class> mocked = mockClassForName(answers)) {
            PlatformAPI api = PlatformAPIFactory.create(plugin);
            assertTrue(api instanceof DummyPaperPlatformAPI);
            mocked.verify(() -> Class.forName(FOLIA_INDICATOR));
            mocked.verify(() -> Class.forName(PAPER_INDICATOR));
            mocked.verify(() -> Class.forName(PAPER_IMPL));
        }
    }

    @Test
    void createReturnsSpigotImplementationWhenDetected() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(SPIGOT_IMPL, DummySpigotPlatformAPI.class);

        try (MockedStatic<Class> mocked = mockClassForName(answers)) {
            PlatformAPI api = PlatformAPIFactory.create(plugin);
            assertTrue(api instanceof DummySpigotPlatformAPI);
            mocked.verify(() -> Class.forName(FOLIA_INDICATOR));
            mocked.verify(() -> Class.forName(PAPER_INDICATOR));
            mocked.verify(() -> Class.forName(SPIGOT_IMPL));
        }
    }

    @Test
    void createFallsBackToPaperWhenFoliaInstantiationFailsAndLogsWarning() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(FOLIA_INDICATOR, Object.class);
        answers.put(FOLIA_IMPL, new ClassNotFoundException("missing Folia"));
        answers.put(PAPER_IMPL, DummyPaperPlatformAPI.class);

        Logger logger = factoryLogger();
        CapturingHandler handler = new CapturingHandler();
        boolean originalParent = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        try (MockedStatic<Class> ignored = mockClassForName(answers)) {
            PlatformAPI api = PlatformAPIFactory.create(plugin);
            assertTrue(api instanceof DummyPaperPlatformAPI);
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalParent);
        }

        LogRecord record = handler.find(Level.WARNING, "FoliaPlatformAPI unavailable, falling back to Paper");
        assertNotNull(record, "Expected warning log for Folia fallback");
    }

    @Test
    void createFallsBackToSpigotWhenPaperInstantiationFailsAndLogsWarning() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(PAPER_INDICATOR, Object.class);
        answers.put(PAPER_IMPL, new ClassNotFoundException("missing Paper"));
        answers.put(SPIGOT_IMPL, DummySpigotPlatformAPI.class);

        Logger logger = factoryLogger();
        CapturingHandler handler = new CapturingHandler();
        boolean originalParent = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        try (MockedStatic<Class> ignored = mockClassForName(answers)) {
            PlatformAPI api = PlatformAPIFactory.create(plugin);
            assertTrue(api instanceof DummySpigotPlatformAPI);
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalParent);
        }

        LogRecord record = handler.find(Level.WARNING, "PaperPlatformAPI unavailable, falling back to Spigot");
        assertNotNull(record, "Expected warning log for Paper fallback");
    }

    @Test
    void createThrowsWhenSpigotInstantiationFailsAndLogsSevere() throws Exception {
        Map<String, Object> answers = new HashMap<>();
        answers.put(SPIGOT_IMPL, new ClassNotFoundException("missing Spigot"));

        Logger logger = factoryLogger();
        CapturingHandler handler = new CapturingHandler();
        boolean originalParent = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        try (MockedStatic<Class> ignored = mockClassForName(answers)) {
            RuntimeException exception = assertThrows(RuntimeException.class, () -> PlatformAPIFactory.create(plugin));
            assertTrue(exception.getMessage().contains("Failed to create platform API"));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(originalParent);
        }

        LogRecord record = handler.find(Level.SEVERE, "SpigotPlatformAPI unavailable");
        assertNotNull(record, "Expected severe log for Spigot failure");
    }

    private static Logger factoryLogger() throws Exception {
        Field field = PlatformAPIFactory.class.getDeclaredField("LOGGER");
        field.setAccessible(true);
        return (Logger) field.get(null);
    }

    private static MockedStatic<Class> mockClassForName(final Map<String, Object> answers) throws Exception {
        MockedStatic<Class> mocked = mockStatic(Class.class);
        mocked.when(() -> Class.forName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            Object result = answers.get(name);
            if (result == null) {
                throw new ClassNotFoundException(name);
            }
            if (result instanceof Class<?> clazz) {
                return clazz;
            }
            if (result instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (result instanceof Error error) {
                throw error;
            }
            if (result instanceof Throwable throwable) {
                throw throwable;
            }
            return result;
        });
        return mocked;
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
            // No-op
        }

        @Override
        public void close() {
            // No-op
        }

        LogRecord find(final Level level, final String message) {
            for (LogRecord record : records) {
                if (record.getLevel().equals(level) && record.getMessage().equals(message)) {
                    return record;
                }
            }
            return null;
        }
    }

    private abstract static class AbstractPlatformStub implements PlatformAPI {

        private final PlatformType type;
        private final ISchedulerAdapter scheduler = new NoopSchedulerAdapter();

        private AbstractPlatformStub(final PlatformType type, final JavaPlugin plugin) {
            this.type = type;
        }

        @Override
        public @NotNull PlatformType getType() {
            return type;
        }

        @Override
        public boolean supportsAdventure() {
            return false;
        }

        @Override
        public boolean supportsFolia() {
            return type == PlatformType.FOLIA;
        }

        @Override
        public void close() {
            // No-op
        }

        @Override
        public void sendMessage(@NotNull Player player, @NotNull Component message) {
            throw unsupported();
        }

        @Override
        public void sendMessages(@NotNull Player player, @NotNull List<Component> messages) {
            throw unsupported();
        }

        @Override
        public void sendActionBar(@NotNull Player player, @NotNull Component message) {
            throw unsupported();
        }

        @Override
        public void sendTitle(@NotNull Player player,
                              @NotNull Component title,
                              @Nullable Component subtitle,
                              int fadeInTicks,
                              int stayTicks,
                              int fadeOutTicks) {
            throw unsupported();
        }

        @Override
        public @NotNull Component getDisplayName(@NotNull Player player) {
            throw unsupported();
        }

        @Override
        public void setDisplayName(@NotNull Player player, @NotNull Component displayName) {
            throw unsupported();
        }

        @Override
        public @Nullable Component getItemDisplayName(@NotNull ItemStack itemStack) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack setItemDisplayName(@NotNull ItemStack itemStack, @Nullable Component displayName) {
            throw unsupported();
        }

        @Override
        public @NotNull List<Component> getItemLore(@NotNull ItemStack itemStack) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack setItemLore(@NotNull ItemStack itemStack, @NotNull List<Component> lore) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack createPlayerHead(@Nullable Player player) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack createPlayerHead(@Nullable OfflinePlayer offlinePlayer) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData, @Nullable Component displayName) {
            throw unsupported();
        }

        @Override
        public @NotNull ItemStack applyCustomTexture(@NotNull ItemStack skull, @NotNull UUID uuid, @NotNull String textureData) {
            throw unsupported();
        }

        @Override
        public @NotNull String getServerVersion() {
            throw unsupported();
        }

        @Override
        public @NotNull ISchedulerAdapter scheduler() {
            return scheduler;
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Test stub");
        }
    }

    private static final class DummyFoliaPlatformAPI extends AbstractPlatformStub {
        private DummyFoliaPlatformAPI(final JavaPlugin plugin) {
            super(PlatformType.FOLIA, plugin);
        }
    }

    private static final class DummyPaperPlatformAPI extends AbstractPlatformStub {
        private DummyPaperPlatformAPI(final JavaPlugin plugin) {
            super(PlatformType.PAPER, plugin);
        }
    }

    private static final class DummySpigotPlatformAPI extends AbstractPlatformStub {
        private DummySpigotPlatformAPI(final JavaPlugin plugin) {
            super(PlatformType.SPIGOT, plugin);
        }
    }

    private static final class NoopSchedulerAdapter implements ISchedulerAdapter {

        @Override
        public void runSync(@NotNull Runnable task) {
            throw unsupported();
        }

        @Override
        public void runAsync(@NotNull Runnable task) {
            throw unsupported();
        }

        @Override
        public void runDelayed(@NotNull Runnable task, long delayTicks) {
            throw unsupported();
        }

        @Override
        public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
            throw unsupported();
        }

        @Override
        public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
            throw unsupported();
        }

        @Override
        public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
            throw unsupported();
        }

        @Override
        public void runGlobal(@NotNull Runnable task) {
            throw unsupported();
        }

        @Override
        public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
            return CompletableFuture.failedFuture(unsupported());
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Test stub");
        }
    }
}

