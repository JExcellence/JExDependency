package com.raindropcentral.rplatform.metrics;

import com.raindropcentral.rplatform.api.PlatformType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MetricsManager {

    private final JavaPlugin plugin;
    private final int serviceId;
    private final PlatformType platformType;

    public MetricsManager(
            final @NotNull JavaPlugin plugin,
            final int serviceId,
            final @NotNull PlatformType platformType
    ) {
        this.plugin = plugin;
        this.serviceId = serviceId;
        this.platformType = platformType;
        
        initialize();
    }

    private void initialize() {
        try {
            final Class<?> metricsClass = Class.forName("com.raindropcentral.rplatform.metrics.Metrics");
            metricsClass.getConstructor(JavaPlugin.class, int.class, boolean.class)
                    .newInstance(plugin, serviceId, platformType == PlatformType.FOLIA);
        } catch (final Exception e) {
            plugin.getLogger().warning("Failed to initialize metrics: " + e.getMessage());
        }
    }
}
