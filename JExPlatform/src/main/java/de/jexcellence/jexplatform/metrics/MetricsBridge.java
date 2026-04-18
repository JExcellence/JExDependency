package de.jexcellence.jexplatform.metrics;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.server.ServerType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Lightweight bStats metrics wrapper using reflection to avoid a hard dependency.
 *
 * <p>Consumer plugins shade bStats themselves — this bridge discovers it at
 * runtime and auto-registers a {@code server_platform} chart showing the
 * detected server type (Folia, Paper, Spigot):
 *
 * <pre>{@code
 * var metrics = MetricsBridge.create(plugin, 12345, serverType, log);
 * metrics.ifPresent(m -> m.addSimplePie("feature", () -> "enabled"));
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class MetricsBridge {

    private static final String METRICS_CLASS = "org.bstats.bukkit.Metrics";
    private static final String SIMPLE_PIE_CLASS = "org.bstats.charts.SimplePie";

    private final Object metricsInstance;
    private final JExLogger log;

    private MetricsBridge(@NotNull Object metricsInstance, @NotNull JExLogger log) {
        this.metricsInstance = metricsInstance;
        this.log = log;
    }

    /**
     * Attempts to create a metrics bridge by reflectively loading bStats.
     *
     * <p>Returns empty if bStats classes are not on the classpath. When
     * successful, a {@code server_platform} chart is automatically registered.
     *
     * @param plugin     the owning plugin
     * @param serviceId  bStats service ID
     * @param serverType detected server type for the platform chart
     * @param log        logger for diagnostics
     * @return the bridge, or empty if bStats is unavailable
     */
    public static @NotNull Optional<MetricsBridge> create(
            @NotNull JavaPlugin plugin,
            int serviceId,
            @NotNull ServerType serverType,
            @NotNull JExLogger log
    ) {
        try {
            var metricsClass = Class.forName(METRICS_CLASS);
            var instance = metricsClass
                    .getConstructor(JavaPlugin.class, int.class)
                    .newInstance(plugin, serviceId);

            var bridge = new MetricsBridge(instance, log);
            bridge.addSimplePie("server_platform", serverType::name);
            log.info("bStats metrics enabled (service {})", serviceId);
            return Optional.of(bridge);
        } catch (ClassNotFoundException e) {
            log.debug("bStats not available on classpath");
            return Optional.empty();
        } catch (ReflectiveOperationException e) {
            log.warn("Failed to initialize bStats: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Adds a simple pie chart that supplies a single string value.
     *
     * @param chartId  bStats chart identifier
     * @param callable supplier returning the chart value
     */
    public void addSimplePie(@NotNull String chartId, @NotNull java.util.concurrent.Callable<String> callable) {
        try {
            var pieClass = Class.forName(SIMPLE_PIE_CLASS);
            var chart = pieClass
                    .getConstructor(String.class, java.util.concurrent.Callable.class)
                    .newInstance(chartId, callable);
            addChart(chart);
        } catch (ReflectiveOperationException e) {
            log.debug("Failed to add SimplePie chart {}: {}", chartId, e.getMessage());
        }
    }

    /**
     * Adds a custom chart object to the metrics instance.
     *
     * <p>The chart must be an instance of a bStats {@code CustomChart} subclass
     * (e.g. {@code SimplePie}, {@code SingleLineChart}).
     *
     * @param chart the bStats chart object
     */
    public void addChart(@NotNull Object chart) {
        try {
            var addMethod = metricsInstance.getClass().getMethod("addCustomChart",
                    Class.forName("org.bstats.charts.CustomChart"));
            addMethod.invoke(metricsInstance, chart);
        } catch (ReflectiveOperationException e) {
            log.debug("Failed to add chart: {}", e.getMessage());
        }
    }
}
