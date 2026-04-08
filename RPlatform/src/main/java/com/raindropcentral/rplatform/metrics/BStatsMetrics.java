/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.metrics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Provides a relocated copy of the bStats metrics implementation that schedules data collection,.
 * builds JSON payloads, and submits them to the bStats service on behalf of a {@link JavaPlugin}.
 * The implementation mirrors the upstream API while respecting the repository's relocation
 * requirements, allowing callers to register charts and control log verbosity at runtime.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class BStatsMetrics {

    private static final String METRICS_VERSION = "3.0.2";
    private static final String REPORT_URL = "https://bStats.org/api/v2/data/%s";
    private static final String BSTATS_FOLDER = "bStats";
    private static final String CONFIG_FILE = "config.yml";

    private final JavaPlugin plugin;
    private final MetricsCore core;

    /**
     * Creates a new metrics facade for the provided plugin.
     *
     * @param plugin   the plugin that owns the metrics instance
     * @param serviceId the bStats service identifier assigned to the plugin
     * @param isFolia  whether the plugin is currently running on Folia, used to register the folia chart
     */
    public BStatsMetrics(final @NotNull JavaPlugin plugin, final int serviceId, final boolean isFolia) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");

        final File configFile = new File(
                new File(plugin.getDataFolder().getParentFile(), BSTATS_FOLDER),
                CONFIG_FILE
        );

        final YamlConfiguration config = loadConfiguration(configFile);
        final boolean enabled = config.getBoolean("enabled", true);
        final String serverUuid = config.getString("serverUuid");
        final boolean logErrors = config.getBoolean("logFailedRequests", false);
        final boolean logSentData = config.getBoolean("logSentData", false);
        final boolean logResponseStatus = config.getBoolean("logResponseStatusText", false);

        this.core = new MetricsCore(
                "bukkit",
                serverUuid,
                serviceId,
                enabled,
                this::appendPlatformData,
                this::appendServiceData,
                task -> Bukkit.getScheduler().runTask(plugin, task),
                plugin::isEnabled,
                (message, error) -> plugin.getLogger().log(Level.WARNING, message, error),
                message -> plugin.getLogger().log(Level.INFO, message),
                logErrors,
                logSentData,
                logResponseStatus
        );

        if (isFolia) {
            addCustomChart(new SingleLineChart("folia", () -> 1));
        }
    }

    /**
     * Registers a custom chart that will be evaluated during the next submission cycle.
     *
     * @param chart the chart to add
     */
    public void addCustomChart(final @NotNull CustomChart chart) {
        core.addCustomChart(chart);
    }

    /**
     * Stops the underlying scheduler from submitting additional metrics payloads.
     */
    public void shutdown() {
        core.shutdown();
    }

    /**
     * Loads the bStats configuration, creating the file with sensible defaults when missing and.
     * logging a warning if the file cannot be written to disk.
     *
     * @param configFile the configuration file to load
     * @return the populated YAML configuration instance
     */
    private @NotNull YamlConfiguration loadConfiguration(final @NotNull File configFile) {
        configFile.getParentFile().mkdirs();
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);

            config.options().setHeader(List.of(
                    "bStats (https://bStats.org) collects anonymous usage statistics.",
                    "This helps plugin authors understand how their plugins are used.",
                    "All data is completely anonymous and cannot be traced back to individual servers.",
                    "You can disable this by setting 'enabled' to false."
            )).copyDefaults(true);

            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save bStats config", e);
            }
        }

        return config;
    }

    /**
     * Appends platform specific data such as player counts, JVM metadata, and server information to.
     * the provided JSON builder.
     *
     * @param builder the JSON builder receiving platform fields
     */
    private void appendPlatformData(final @NotNull JsonBuilder builder) {
        builder.appendField("playerAmount", Bukkit.getOnlinePlayers().size());
        builder.appendField("onlineMode", Bukkit.getOnlineMode() ? 1 : 0);
        builder.appendField("bukkitVersion", Bukkit.getVersion());
        builder.appendField("bukkitName", Bukkit.getName());
        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    /**
     * Appends plugin specific data such as the plugin version to the provided JSON builder.
     *
     * @param builder the JSON builder receiving service fields
     */
    private void appendServiceData(final @NotNull JsonBuilder builder) {
        builder.appendField("pluginVersion", plugin.getPluginMeta().getVersion());
    }

    /**
     * Coordinates the lifecycle of scheduled submissions, including data aggregation and HTTP.
     * transport to the bStats endpoint. Handles logging for lifecycle events and submission
     * outcomes to aid diagnostics.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class MetricsCore {

        private static final int SCHEDULER_POOL_SIZE = 1;
        private static final long INITIAL_DELAY_MIN = TimeUnit.MINUTES.toMillis(3);
        private static final long INITIAL_DELAY_MAX_ADDITIONAL = TimeUnit.MINUTES.toMillis(3);
        private static final long SECOND_DELAY_MAX = TimeUnit.MINUTES.toMillis(30);
        private static final long SUBMIT_INTERVAL = TimeUnit.MINUTES.toMillis(30);

        private final ScheduledExecutorService scheduler;
        private final String platform;
        private final String serverUuid;
        private final int serviceId;
        private final boolean enabled;
        private final Consumer<JsonBuilder> platformDataConsumer;
        private final Consumer<JsonBuilder> serviceDataConsumer;
        private final Consumer<Runnable> taskConsumer;
        private final Supplier<Boolean> serviceEnabledSupplier;
        private final BiConsumer<String, Throwable> errorLogger;
        private final Consumer<String> infoLogger;
        private final boolean logErrors;
        private final boolean logSentData;
        private final boolean logResponseStatus;
        private final Set<CustomChart> customCharts = ConcurrentHashMap.newKeySet();

        /**
         * Creates the metrics core that schedules submission tasks and manages logging.
         *
         * @param platform                the platform identifier reported to bStats
         * @param serverUuid              the unique server UUID stored in the configuration
         * @param serviceId               the metrics service identifier
         * @param enabled                 whether metrics submissions should be scheduled
         * @param platformDataConsumer    callback for populating platform metadata
         * @param serviceDataConsumer     callback for populating plugin specific metadata
         * @param taskConsumer            executor used to run submission tasks on the Bukkit scheduler
         * @param serviceEnabledSupplier  supplier indicating whether the hosting plugin is enabled
         * @param errorLogger             consumer invoked when an exception occurs
         * @param infoLogger              consumer invoked for informational messages
         * @param logErrors               whether caught exceptions should be logged
         * @param logSentData             whether outbound payloads should be logged
         * @param logResponseStatus       whether HTTP responses should be logged
         */
        public MetricsCore(
                final @NotNull String platform,
                final @Nullable String serverUuid,
                final int serviceId,
                final boolean enabled,
                final @NotNull Consumer<JsonBuilder> platformDataConsumer,
                final @NotNull Consumer<JsonBuilder> serviceDataConsumer,
                final @NotNull Consumer<Runnable> taskConsumer,
                final @NotNull Supplier<Boolean> serviceEnabledSupplier,
                final @NotNull BiConsumer<String, Throwable> errorLogger,
                final @NotNull Consumer<String> infoLogger,
                final boolean logErrors,
                final boolean logSentData,
                final boolean logResponseStatus
        ) {
            this.platform = Objects.requireNonNull(platform);
            this.serverUuid = serverUuid;
            this.serviceId = serviceId;
            this.enabled = enabled && serverUuid != null;
            this.platformDataConsumer = Objects.requireNonNull(platformDataConsumer);
            this.serviceDataConsumer = Objects.requireNonNull(serviceDataConsumer);
            this.taskConsumer = Objects.requireNonNull(taskConsumer);
            this.serviceEnabledSupplier = Objects.requireNonNull(serviceEnabledSupplier);
            this.errorLogger = Objects.requireNonNull(errorLogger);
            this.infoLogger = Objects.requireNonNull(infoLogger);
            this.logErrors = logErrors;
            this.logSentData = logSentData;
            this.logResponseStatus = logResponseStatus;

            final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                    SCHEDULER_POOL_SIZE,
                    task -> {
                        final Thread thread = new Thread(task, "bStats-Metrics");
                        thread.setDaemon(true);
                        return thread;
                    }
            );
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            this.scheduler = executor;

            checkRelocation();

            if (this.enabled) {
                infoLogger.accept("[bStats] Enabling metrics. Service ID: " + serviceId);
                startSubmitting();
            } else {
                infoLogger.accept("[bStats] Metrics disabled.");
            }
        }

        /**
         * Adds the supplied chart to the registry so it contributes to future submissions.
         *
         * @param chart the chart to register
         */
        public void addCustomChart(final @NotNull CustomChart chart) {
            customCharts.add(Objects.requireNonNull(chart));
        }

        /**
         * Shuts down the scheduler and logs the action when the core is active.
         */
        public void shutdown() {
            if (!scheduler.isShutdown()) {
                infoLogger.accept("[bStats] Shutting down metrics scheduler.");
                scheduler.shutdown();
            }
        }

        /**
         * Sets up the recurring submission tasks using randomized delays to reduce coordinated.
         * request bursts across servers.
         */
        private void startSubmitting() {
            final Runnable submitTask = () -> {
                if (!enabled || !serviceEnabledSupplier.get()) {
                    scheduler.shutdown();
                    return;
                }
                taskConsumer.accept(this::submitData);
            };

            final long initialDelay = INITIAL_DELAY_MIN +
                    ThreadLocalRandom.current().nextLong(INITIAL_DELAY_MAX_ADDITIONAL + 1);
            final long secondDelay = ThreadLocalRandom.current().nextLong(SECOND_DELAY_MAX + 1);

            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(
                    submitTask,
                    initialDelay + secondDelay,
                    SUBMIT_INTERVAL,
                    TimeUnit.MILLISECONDS
            );
        }

        /**
         * Collects chart data and enqueues the asynchronous HTTP submission job on the scheduler.
         */
        private void submitData() {
            final JsonBuilder baseJson = new JsonBuilder();
            platformDataConsumer.accept(baseJson);

            final JsonBuilder serviceJson = new JsonBuilder();
            serviceDataConsumer.accept(serviceJson);

            final List<JsonBuilder.JsonObject> chartData = customCharts.stream()
                    .map(chart -> chart.getRequestJsonObject(errorLogger, logErrors))
                    .filter(Objects::nonNull)
                    .toList();

            if (!chartData.isEmpty()) {
                serviceJson.appendField("customCharts", chartData.toArray(new JsonBuilder.JsonObject[0]));
            }

            serviceJson.appendField("id", serviceId);
            baseJson.appendField("service", serviceJson.build());
            baseJson.appendField("serverUUID", serverUuid);
            baseJson.appendField("metricsVersion", METRICS_VERSION);

            final JsonBuilder.JsonObject data = baseJson.build();
            scheduler.execute(() -> sendDataAsync(data));
        }

        /**
         * Sends the prepared payload while capturing and optionally logging exceptions using the.
         * provided error consumer.
         *
         * @param data the payload to transmit
         */
        private void sendDataAsync(final @NotNull JsonBuilder.JsonObject data) {
            try {
                sendData(data);
            } catch (Exception e) {
                if (logErrors) {
                    errorLogger.accept("Could not submit bStats metrics data", e);
                }
            }
        }

        /**
         * Performs the HTTP POST request to the bStats endpoint, logging the outbound payload and.
         * response when configured to do so.
         *
         * @param data the JSON payload to submit
         * @throws IOException if the HTTP request cannot be completed
         */
        private void sendData(final @NotNull JsonBuilder.JsonObject data) throws IOException {
            final String dataString = data.toString();

            if (logSentData) {
                infoLogger.accept("[bStats] Submitting metrics data: " + dataString);
            }

            final String url = String.format(REPORT_URL, platform);
            final HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            final byte[] compressedData = compress(dataString);

            connection.setRequestMethod("POST");
            connection.addRequestProperty("Accept", "application/json");
            connection.addRequestProperty("Connection", "close");
            connection.addRequestProperty("Content-Encoding", "gzip");
            connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Metrics-Service/1");
            connection.setDoOutput(true);

            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(compressedData);
            }

            final StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            if (logResponseStatus) {
                infoLogger.accept("[bStats] Response: " + responseBuilder);
            }
        }

        /**
         * Verifies that the metrics class has been relocated away from the default bStats package,.
         * throwing an {@link IllegalStateException} when relocation is skipped.
         */
        private void checkRelocation() {
            if ("false".equals(System.getProperty("bstats.relocatecheck"))) {
                return;
            }

            final String defaultPackage = new String(
                    new byte[]{'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's'}
            );
            final String currentPackage = getClass().getPackage().getName();

            if (currentPackage.startsWith(defaultPackage)) {
                throw new IllegalStateException(
                        "bStats Metrics class has not been relocated correctly! Current package: " + currentPackage
                );
            }
        }

        /**
         * Compresses the JSON payload before transport.
         *
         * @param str the payload string
         * @return the compressed payload bytes
         * @throws IOException if the compression stream cannot write
         */
        private static byte[] compress(final @Nullable String str) throws IOException {
            if (str == null || str.isEmpty()) {
                return new byte[0];
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
                gzip.write(str.getBytes(StandardCharsets.UTF_8));
                gzip.finish();
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * Base chart abstraction used by the metrics implementation to transform plugin provided data.
     * into JSON structures suitable for submission.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public abstract static class CustomChart {

        protected final String chartId;

        /**
         * Creates a chart with the supplied identifier.
         *
         * @param chartId the non-empty chart identifier registered with bStats
         */
        protected CustomChart(final @NotNull String chartId) {
            if (chartId.isEmpty()) {
                throw new IllegalArgumentException("Chart ID cannot be empty");
            }
            this.chartId = chartId;
        }

        /**
         * Builds the JSON object representing this chart, logging errors when data gathering fails.
         * and error logging is enabled.
         *
         * @param errorLogger the logger used for chart specific failures
         * @param logErrors   whether failures should be logged
         * @return the chart JSON object or {@code null} when no data is available
         */
        protected @Nullable JsonBuilder.JsonObject getRequestJsonObject(
                final @NotNull BiConsumer<String, Throwable> errorLogger,
                final boolean logErrors
        ) {
            JsonBuilder.JsonObject data;
            try {
                data = getChartData();
            } catch (Exception e) {
                if (logErrors) {
                    errorLogger.accept("Failed to get data for custom chart with id '" + chartId + "'", e);
                }
                return null;
            }

            if (data == null) {
                return null;
            }

            final JsonBuilder builder = new JsonBuilder();
            builder.appendField("chartId", chartId);
            builder.appendField("data", data);
            return builder.build();
        }

        /**
         * Collects the chart data.
         *
         * @return the chart JSON structure or {@code null} to skip submission
         * @throws Exception when data collection fails
         */
        protected abstract @Nullable JsonBuilder.JsonObject getChartData() throws Exception;
    }

    /**
     * Simple pie chart implementation that submits a single string value when provided.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class SimplePie extends CustomChart {

        private final Callable<String> callable;

        /**
         * Creates a simple pie chart backed by the provided callable.
         *
         * @param chartId the chart identifier
         * @param callable supplies the chart value during submission
         */
        public SimplePie(final @NotNull String chartId, final @NotNull Callable<String> callable) {
            super(chartId);
            this.callable = Objects.requireNonNull(callable);
        }

        @Override
        protected @Nullable JsonBuilder.JsonObject getChartData() throws Exception {
            final String value = callable.call();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new JsonBuilder().appendField("value", value).build();
        }
    }

    /**
     * Single line chart implementation that forwards integer values to bStats.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class SingleLineChart extends CustomChart {

        private final Callable<Integer> callable;

        /**
         * Creates a single line chart backed by the provided callable.
         *
         * @param chartId the chart identifier
         * @param callable supplies the chart value during submission
         */
        public SingleLineChart(final @NotNull String chartId, final @NotNull Callable<Integer> callable) {
            super(chartId);
            this.callable = Objects.requireNonNull(callable);
        }

        @Override
        protected @Nullable JsonBuilder.JsonObject getChartData() throws Exception {
            final Integer value = callable.call();
            if (value == null || value == 0) {
                return null;
            }
            return new JsonBuilder().appendField("value", value).build();
        }
    }

    /**
     * Advanced pie chart implementation that aggregates multiple labelled values.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class AdvancedPie extends CustomChart {

        private final Callable<Map<String, Integer>> callable;

        /**
         * Creates an advanced pie chart backed by the provided callable.
         *
         * @param chartId the chart identifier
         * @param callable supplies the labelled values during submission
         */
        public AdvancedPie(final @NotNull String chartId, final @NotNull Callable<Map<String, Integer>> callable) {
            super(chartId);
            this.callable = Objects.requireNonNull(callable);
        }

        @Override
        protected @Nullable JsonBuilder.JsonObject getChartData() throws Exception {
            final Map<String, Integer> map = callable.call();
            if (map == null || map.isEmpty()) {
                return null;
            }

            final JsonBuilder valuesBuilder = new JsonBuilder();
            boolean dataFound = false;

            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isEmpty() &&
                        entry.getValue() != null && entry.getValue() != 0) {
                    valuesBuilder.appendField(entry.getKey(), entry.getValue());
                    dataFound = true;
                }
            }

            if (!dataFound) {
                return null;
            }

            return new JsonBuilder().appendField("values", valuesBuilder.build()).build();
        }
    }

    /**
     * Multi-line chart implementation that submits multiple integer time series values.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class MultiLineChart extends CustomChart {

        private final Callable<Map<String, Integer>> callable;

        /**
         * Creates a multi-line chart backed by the provided callable.
         *
         * @param chartId the chart identifier
         * @param callable supplies the labelled series values during submission
         */
        public MultiLineChart(final @NotNull String chartId, final @NotNull Callable<Map<String, Integer>> callable) {
            super(chartId);
            this.callable = Objects.requireNonNull(callable);
        }

        @Override
        protected @Nullable JsonBuilder.JsonObject getChartData() throws Exception {
            final Map<String, Integer> map = callable.call();
            if (map == null || map.isEmpty()) {
                return null;
            }

            final JsonBuilder valuesBuilder = new JsonBuilder();
            boolean dataFound = false;

            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isEmpty() &&
                        entry.getValue() != null && entry.getValue() != 0) {
                    valuesBuilder.appendField(entry.getKey(), entry.getValue());
                    dataFound = true;
                }
            }

            if (!dataFound) {
                return null;
            }

            return new JsonBuilder().appendField("values", valuesBuilder.build()).build();
        }
    }

    /**
     * Drill-down pie chart implementation that emits nested value groups.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class DrillDownPie extends CustomChart {

        private final Callable<Map<String, Map<String, Integer>>> callable;

        /**
         * Creates a drill-down pie chart backed by the provided callable.
         *
         * @param chartId the chart identifier
         * @param callable supplies the nested labelled values during submission
         */
        public DrillDownPie(
                final @NotNull String chartId,
                final @NotNull Callable<Map<String, Map<String, Integer>>> callable
        ) {
            super(chartId);
            this.callable = Objects.requireNonNull(callable);
        }

        @Override
        protected @Nullable JsonBuilder.JsonObject getChartData() throws Exception {
            final Map<String, Map<String, Integer>> map = callable.call();
            if (map == null || map.isEmpty()) {
                return null;
            }

            final JsonBuilder valuesBuilder = new JsonBuilder();
            boolean topLevelDataFound = false;

            for (Map.Entry<String, Map<String, Integer>> topLevelEntry : map.entrySet()) {
                final String topLevelKey = topLevelEntry.getKey();
                final Map<String, Integer> innerMap = topLevelEntry.getValue();

                if (topLevelKey == null || topLevelKey.isEmpty() || innerMap == null || innerMap.isEmpty()) {
                    continue;
                }

                final JsonBuilder innerBuilder = new JsonBuilder();
                boolean innerDataFound = false;

                for (Map.Entry<String, Integer> innerEntry : innerMap.entrySet()) {
                    if (innerEntry.getKey() != null && !innerEntry.getKey().isEmpty() &&
                            innerEntry.getValue() != null && innerEntry.getValue() != 0) {
                        innerBuilder.appendField(innerEntry.getKey(), innerEntry.getValue());
                        innerDataFound = true;
                    }
                }

                if (innerDataFound) {
                    valuesBuilder.appendField(topLevelKey, innerBuilder.build());
                    topLevelDataFound = true;
                }
            }

            if (!topLevelDataFound) {
                return null;
            }

            return new JsonBuilder().appendField("values", valuesBuilder.build()).build();
        }
    }

    /**
     * Lightweight JSON builder used to construct payloads without bringing in an additional.
     * dependency.
     *
     * @author JExcellence
     * @since 1.0.0
     * @version 1.0.1
     */
    public static final class JsonBuilder {

        private StringBuilder builder = new StringBuilder();
        private boolean firstField = true;

        /**
         * Creates a new builder instance.
         */
        public JsonBuilder() {
            builder.append("{");
        }

        /**
         * Appends a string field to the JSON object.
         *
         * @param key   the JSON key
         * @param value the string value to escape and append
         * @return this builder for fluent chaining
         */
        public @NotNull JsonBuilder appendField(final @NotNull String key, final @NotNull String value) {
            Objects.requireNonNull(value);
            appendFieldInternal(key, "\"" + escape(value) + "\"");
            return this;
        }

        /**
         * Appends an integer field to the JSON object.
         *
         * @param key   the JSON key
         * @param value the integer value
         * @return this builder for fluent chaining
         */
        public @NotNull JsonBuilder appendField(final @NotNull String key, final int value) {
            appendFieldInternal(key, String.valueOf(value));
            return this;
        }

        /**
         * Appends a nested JSON object field.
         *
         * @param key    the JSON key
         * @param object the object to include
         * @return this builder for fluent chaining
         */
        public @NotNull JsonBuilder appendField(final @NotNull String key, final @NotNull JsonObject object) {
            Objects.requireNonNull(object);
            appendFieldInternal(key, object.toString());
            return this;
        }

        /**
         * Appends an array field composed of nested JSON objects.
         *
         * @param key    the JSON key
         * @param values the array of objects to include
         * @return this builder for fluent chaining
         */
        public @NotNull JsonBuilder appendField(final @NotNull String key, final @NotNull JsonObject[] values) {
            Objects.requireNonNull(values);
            final String arrayContent = Arrays.stream(values)
                    .map(JsonObject::toString)
                    .collect(Collectors.joining(","));
            appendFieldInternal(key, "[" + arrayContent + "]");
            return this;
        }

        /**
         * Internal helper that manages JSON commas and ensures keys are escaped.
         *
         * @param key          the JSON key
         * @param escapedValue the already escaped value
         */
        private void appendFieldInternal(final @NotNull String key, final @NotNull String escapedValue) {
            if (builder == null) {
                throw new IllegalStateException("JSON has already been built");
            }
            Objects.requireNonNull(key);

            if (!firstField) {
                builder.append(",");
            }

            builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
            firstField = false;
        }

        /**
         * Completes JSON construction and returns the immutable representation.
         *
         * @return the resulting JSON object
         */
        public @NotNull JsonObject build() {
            if (builder == null) {
                throw new IllegalStateException("JSON has already been built");
            }
            final JsonObject result = new JsonObject(builder.append("}").toString());
            builder = null;
            return result;
        }

        /**
         * Escapes a JSON string value.
         *
         * @param value the value to escape
         * @return the escaped string
         */
        private static @NotNull String escape(final @NotNull String value) {
            final StringBuilder sb = new StringBuilder(value.length() + 10);

            for (int i = 0; i < value.length(); i++) {
                final char c = value.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c <= '\u001F') {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }

            return sb.toString();
        }

        /**
         * Immutable wrapper around the raw JSON string produced by the builder.
         *
         * @author JExcellence
         * @since 1.0.0
         * @version 1.0.1
         */
        public static final class JsonObject {

            private final String value;

            /**
             * Creates a new immutable JSON object wrapper.
             *
             * @param value the JSON string backing this object
             */
            private JsonObject(final @NotNull String value) {
                this.value = value;
            }

            /**
             * Executes toString.
             */
            @Override
            public @NotNull String toString() {
                return value;
            }

            /**
             * Executes equals.
             */
            @Override
            public boolean equals(final Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                final JsonObject that = (JsonObject) o;
                return value.equals(that.value);
            }

            /**
             * Returns whether hCode.
             */
            @Override
            public int hashCode() {
                return value.hashCode();
            }
        }
    }
}
