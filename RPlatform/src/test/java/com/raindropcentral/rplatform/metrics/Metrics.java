package com.raindropcentral.rplatform.metrics;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Test scoped replacement for the relocated metrics entry point referenced by {@link MetricsManager}.
 * The constructor forwards invocations to {@link MetricsTestRegistry} so tests can assert behaviour
 * without relying on the production implementation that is shaded into plugin builds.
 */
public class Metrics {

    public Metrics(final @NotNull JavaPlugin plugin, final int serviceId, final boolean folia) {
        MetricsTestRegistry.recordConstruction(plugin, serviceId, folia);
    }
}
