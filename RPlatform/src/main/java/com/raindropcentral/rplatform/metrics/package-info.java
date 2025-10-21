/**
 * Provides the bStats metrics bridge that {@link com.raindropcentral.rplatform.RPlatform} exposes to plugins.
 *
 * <p>{@link com.raindropcentral.rplatform.metrics.MetricsManager} is constructed via
 * {@link com.raindropcentral.rplatform.RPlatform#initializeMetrics(int)} once the asynchronous
 * {@link com.raindropcentral.rplatform.RPlatform#initialize()} sequence has completed. This ordering ensures the
 * translation manager, command updater, and database resources created during service initialization are ready before
 * usage statistics are reported.</p>
 *
 * <p>The manager reflects the shaded {@code Metrics} bootstrapper which instantiates {@link BStatsMetrics}. The bStats
 * implementation collects environment data such as online player count, online-mode flag, Bukkit name/version, Java and
 * operating-system details, logical CPU count, and the running plugin's version. Folia servers also emit a dedicated
 * single-line chart so dashboards can segment metrics by scheduler implementation.</p>
 *
 * <p>Invoke {@link com.raindropcentral.rplatform.RPlatform#detectPremiumVersion(Class, String)} before initializing
 * metrics when you need to differentiate premium behaviour: the premium flag can then be surfaced through
 * {@link BStatsMetrics#addCustomChart(BStatsMetrics.CustomChart)} or other charts without changing the shared
 * {@code serviceId}. Keeping the service identifier stable allows existing dashboards to continue tracking both free and
 * premium distributions while still enabling opt-in premium-specific visualisations.</p>
 *
 * <p>When extending the metrics pipeline prefer additive changes. Reuse the existing {@code serviceId}, keep previously
 * published chart keys intact, and register new charts through {@link BStatsMetrics#addCustomChart(BStatsMetrics.CustomChart)}.
 * Update consuming dashboards to read new fields instead of renaming or removing old ones; this avoids breaking
 * historical visualisations while still exposing richer analytics.</p>
 */
package com.raindropcentral.rplatform.metrics;
