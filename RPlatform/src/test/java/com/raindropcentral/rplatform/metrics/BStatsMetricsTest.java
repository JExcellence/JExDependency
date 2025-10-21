package com.raindropcentral.rplatform.metrics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BStatsMetricsTest {

    @TempDir
    Path tempDir;

    private JavaPlugin plugin;
    private Logger logger;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        logger = Logger.getLogger("BStatsMetricsTest" + UUID.randomUUID());
        logger.setLevel(Level.ALL);

        File dataFolder = tempDir.resolve("plugin").toFile();
        assertTrue(dataFolder.mkdirs(), "Plugin data folder should be created for tests");

        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder);
        Mockito.when(plugin.getLogger()).thenReturn(logger);
        Mockito.when(plugin.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    @Test
    void constructorRegistersFoliaChartAndCreatesConfig() {
        try (MockedStatic<Bukkit> mockedBukkit = prepareBukkitStatics()) {
            BStatsMetrics metrics = new BStatsMetrics(plugin, 42, true);

            BStatsMetrics.MetricsCore core = getField(metrics, "core", BStatsMetrics.MetricsCore.class);
            @SuppressWarnings("unchecked")
            Set<BStatsMetrics.CustomChart> charts = (Set<BStatsMetrics.CustomChart>) getField(core, "customCharts", Set.class);

            assertEquals(1, charts.size(), "Folia initialization should register the folia chart");
            assertTrue(charts.iterator().next() instanceof BStatsMetrics.SingleLineChart,
                    "Registered chart should be the folia single line chart");

            File configFile = new File(plugin.getDataFolder().getParentFile(), "bStats/config.yml");
            assertTrue(configFile.exists(), "Constructor should persist the bStats configuration file");

            metrics.shutdown();
        }
    }

    @Test
    void addCustomChartRegistersChartOnce() {
        try (MockedStatic<Bukkit> mockedBukkit = prepareBukkitStatics()) {
            BStatsMetrics metrics = new BStatsMetrics(plugin, 101, false);

            BStatsMetrics.CustomChart chart = new BStatsMetrics.SimplePie("test", () -> "value");
            metrics.addCustomChart(chart);
            metrics.addCustomChart(chart);

            BStatsMetrics.MetricsCore core = getField(metrics, "core", BStatsMetrics.MetricsCore.class);
            @SuppressWarnings("unchecked")
            Set<BStatsMetrics.CustomChart> charts = (Set<BStatsMetrics.CustomChart>) getField(core, "customCharts", Set.class);

            assertEquals(1, charts.size(), "Adding the same chart instance twice should not duplicate registration");
            assertTrue(charts.contains(chart), "The provided chart should be registered with the metrics core");

            metrics.shutdown();
        }
    }

    @Test
    void shutdownStopsSchedulerOnce() {
        try (MockedStatic<Bukkit> mockedBukkit = prepareBukkitStatics()) {
            TestLogHandler logHandler = new TestLogHandler();
            logger.addHandler(logHandler);

            BStatsMetrics metrics = new BStatsMetrics(plugin, 202, false);
            BStatsMetrics.MetricsCore core = getField(metrics, "core", BStatsMetrics.MetricsCore.class);

            ScheduledExecutorService originalScheduler = getField(core, "scheduler", ScheduledExecutorService.class);
            originalScheduler.shutdownNow();

            ScheduledExecutorService schedulerMock = Mockito.mock(ScheduledExecutorService.class);
            Mockito.when(schedulerMock.isShutdown()).thenReturn(false, true);
            Mockito.when(schedulerMock.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                    .thenReturn(Mockito.mock(ScheduledFuture.class));
            Mockito.when(schedulerMock.scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.any(TimeUnit.class))).thenReturn(Mockito.mock(ScheduledFuture.class));

            setField(core, "scheduler", ScheduledExecutorService.class, schedulerMock);

            metrics.shutdown();
            metrics.shutdown();

            Mockito.verify(schedulerMock, Mockito.times(1)).shutdown();
            assertTrue(logHandler.contains("[bStats] Shutting down metrics scheduler."));
        }
    }

    @Test
    void duplicateInitializationReusesServerUuid() {
        try (MockedStatic<Bukkit> mockedBukkit = prepareBukkitStatics()) {
            BStatsMetrics firstMetrics = new BStatsMetrics(plugin, 303, false);
            File configFile = new File(plugin.getDataFolder().getParentFile(), "bStats/config.yml");
            YamlConfiguration firstConfig = YamlConfiguration.loadConfiguration(configFile);
            String firstUuid = firstConfig.getString("serverUuid");
            assertNotNull(firstUuid, "Initial configuration should contain a server UUID");
            firstMetrics.shutdown();

            BStatsMetrics secondMetrics = new BStatsMetrics(plugin, 404, false);
            YamlConfiguration secondConfig = YamlConfiguration.loadConfiguration(configFile);
            String secondUuid = secondConfig.getString("serverUuid");
            assertEquals(firstUuid, secondUuid, "Reinitializing metrics should retain the original server UUID");
            secondMetrics.shutdown();
        }
    }

    @Test
    void submitDataIncludesInvalidServiceId() throws Throwable {
        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        ScheduledExecutorService schedulerMock = Mockito.mock(ScheduledExecutorService.class);
        Mockito.when(schedulerMock.isShutdown()).thenReturn(false);
        Mockito.when(schedulerMock.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenReturn(Mockito.mock(ScheduledFuture.class));
        Mockito.when(schedulerMock.scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong(),
                Mockito.any(TimeUnit.class))).thenReturn(Mockito.mock(ScheduledFuture.class));
        Mockito.doAnswer(invocation -> {
            scheduledTask.set(invocation.getArgument(0));
            return null;
        }).when(schedulerMock).execute(Mockito.any(Runnable.class));

        BStatsMetrics.MetricsCore core = new BStatsMetrics.MetricsCore(
                "bukkit",
                "server-uuid",
                -42,
                true,
                builder -> builder.appendField("players", 5),
                builder -> builder.appendField("pluginVersion", "1.0.0"),
                Runnable::run,
                () -> true,
                (message, error) -> {},
                message -> {},
                true,
                true,
                true
        );

        ScheduledExecutorService originalScheduler = getField(core, "scheduler", ScheduledExecutorService.class);
        originalScheduler.shutdownNow();
        setField(core, "scheduler", ScheduledExecutorService.class, schedulerMock);

        MethodHandle submitData = MethodHandles.privateLookupIn(BStatsMetrics.MetricsCore.class, MethodHandles.lookup())
                .findVirtual(BStatsMetrics.MetricsCore.class, "submitData", MethodType.methodType(void.class));
        submitData.invoke(core);

        Runnable task = scheduledTask.get();
        assertNotNull(task, "Submitting data should enqueue an asynchronous send task");

        ByteArrayOutputStream payloadStream = new ByteArrayOutputStream();
        HttpsURLConnection connection = Mockito.mock(HttpsURLConnection.class);
        Mockito.when(connection.getOutputStream()).thenReturn(payloadStream);
        Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

        try (MockedConstruction<URL> mockedUrl = Mockito.mockConstruction(URL.class, (mock, context) ->
                Mockito.when(mock.openConnection()).thenReturn(connection))) {
            task.run();
        }

        String json = decompress(payloadStream.toByteArray());
        assertTrue(json.contains("\"id\":-42"), "Serialized payload should include the negative service ID");
        assertTrue(json.contains("\"serverUUID\":\"server-uuid\""), "Payload should reference the configured server UUID");

        core.shutdown();
    }

    private MockedStatic<Bukkit> prepareBukkitStatics() {
        MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class);
        BukkitScheduler scheduler = Mockito.mock(BukkitScheduler.class);
        BukkitTask task = Mockito.mock(BukkitTask.class);

        mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        Mockito.when(scheduler.runTask(Mockito.eq(plugin), Mockito.any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return task;
        });

        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
        mockedBukkit.when(Bukkit::getOnlineMode).thenReturn(true);
        mockedBukkit.when(Bukkit::getVersion).thenReturn("test-version");
        mockedBukkit.when(Bukkit::getName).thenReturn("test-server");

        return mockedBukkit;
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

    private static <T> void setField(final Object target, final String fieldName, final Class<T> fieldType, final T value) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(target.getClass(), MethodHandles.lookup());
            VarHandle handle = lookup.findVarHandle(target.getClass(), fieldName, fieldType);
            handle.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set field: " + fieldName, e);
        }
    }

    private static String decompress(final byte[] compressed) throws IOException {
        if (compressed.length == 0) {
            return "";
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class TestLogHandler extends Handler {

        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        @Override
        public void publish(final LogRecord record) {
            if (record != null && record.getMessage() != null) {
                output.writeBytes((record.getLevel() + ":" + record.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
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

        boolean contains(final String snippet) {
            return output.toString(StandardCharsets.UTF_8).contains(snippet);
        }
    }
}
