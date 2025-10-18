/**
 * Centralized logging infrastructure that unifies console, file, and stdout/stderr capture
 * for Raindrop Platform plugins.
 *
 * <p>{@link com.raindropcentral.rplatform.logging.CentralLogger} must be initialized early in
 * a plugin's {@code onEnable}.  It installs {@link java.util.logging.Handler} implementations
 * ({@link com.raindropcentral.rplatform.logging.PlatformConsoleHandler} and the universal
 * handler installed within
 * {@link com.raindropcentral.rplatform.logging.CentralLogger#initialize(org.bukkit.plugin.java.JavaPlugin)})
 * before redirecting
 * {@link System#out} / {@link System#err} to {@link com.raindropcentral.rplatform.logging.LoggingPrintStream}.
 * This order preserves consistent console output because handlers are attached prior to any
 * stream redirection.</p>
 *
 * <p><strong>Usage patterns.</strong> After calling
 * {@link com.raindropcentral.rplatform.logging.CentralLogger#initialize(org.bukkit.plugin.java.JavaPlugin)},
 * obtain plugin-specific loggers via
 * {@link com.raindropcentral.rplatform.logging.PlatformLogger#create(org.bukkit.plugin.java.JavaPlugin)}.
 * {@link com.raindropcentral.rplatform.logging.LoggerConfig} reads per-package overrides and is
 * referenced whenever new loggers are created or the universal handler processes records, so
 * updates to the configuration file propagate without restarting the JVM.</p>
 *
 * <p><strong>Extension points.</strong> Add new destinations by registering additional handlers
 * inside {@link com.raindropcentral.rplatform.logging.CentralLogger#initialize}.  Custom
 * {@link com.raindropcentral.rplatform.logging.LogLevel} values can be introduced if downstream
 * services require specialized severity mapping, but ensure {@link java.util.logging.Level}
 * conversion remains monotonic.</p>
 *
 * <p><strong>Performance.</strong> Large workloads produce many log records; the universal handler
 * de-duplicates bursts using a sliding time window and writes to rotating files capped at 10MB.
 * When streaming high-volume diagnostics, consider disabling console mirroring via
 * {@link com.raindropcentral.rplatform.logging.PlatformLogger#setConsoleEnabled(boolean)} to keep
 * the main thread responsive.</p>
 */
package com.raindropcentral.rplatform.logging;
