/**
 * Central entry point for the shared Raindrop platform runtime.
 *
 * <p>The {@link com.raindropcentral.rplatform.RPlatform#initialize() initialize()} method should be
 * invoked during a plugin's {@code onEnable}. The call spins up an asynchronous task using
 * {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter#runAsync(Runnable)
 * the scheduler adapter}, ensuring Folia servers use their dedicated scheduler implementation while
 * Bukkit/Paper fall back to the bundled adapter. Inside the async block the platform logs the start,
 * prepares database resources through {@code initializeDatabaseResources()}, and bootstraps the
 * {@link com.raindropcentral.rplatform.localization.TranslationManager} alongside the
 * {@link de.jexcellence.evaluable.CommandUpdater}. Once those components are ready the platform marks
 * itself as initialized.</p>
 *
 * <p>Follow-up features such as {@link com.raindropcentral.rplatform.metrics.MetricsManager metrics}
 * and {@link com.raindropcentral.rplatform.placeholder.PlaceholderManager placeholders} must be
 * initialized <em>after</em> {@code initialize()} completes. Use
 * {@link com.raindropcentral.rplatform.RPlatform#initializeMetrics(int)} and
 * {@link com.raindropcentral.rplatform.RPlatform#initializePlaceholders(String)} once the async
 * future finishes so both services can observe the translation manager, command updater, and database
 * infrastructure that the main initialize routine provides.</p>
 *
 * <p><strong>Subpackage index</strong>
 * <ul>
 *     <li>{@link com.raindropcentral.rplatform.api api} – Platform detection and API wrappers.</li>
 *     <li>{@link com.raindropcentral.rplatform.config config} – Configuration adapters and helpers.</li>
 *     <li>{@link com.raindropcentral.rplatform.console console} – Console message formatting.</li>
 *     <li><code>com.raindropcentral.rplatform.database</code> – Database resource descriptors.</li>
 *     <li>{@link com.raindropcentral.rplatform.discord discord} – Discord webhook integration.</li>
 *     <li>{@link com.raindropcentral.rplatform.head head} – Player head utilities.</li>
 *     <li>{@link com.raindropcentral.rplatform.localization localization} – Translation facilities.</li>
 *     <li>{@link com.raindropcentral.rplatform.logging logging} – Logging abstraction and appenders.</li>
 *     <li>{@link com.raindropcentral.rplatform.metrics metrics} – Metrics manager and collectors.</li>
 *     <li>{@link com.raindropcentral.rplatform.placeholder placeholder} – Placeholder API bridges.</li>
 *     <li>{@link com.raindropcentral.rplatform.scheduler scheduler} – Scheduler adapters and
 *     implementations.</li>
 *     <li>{@link com.raindropcentral.rplatform.serializer serializer} – Serialization utilities.</li>
 *     <li>{@link com.raindropcentral.rplatform.service service} – Service registry and lookup.</li>
 *     <li>{@link com.raindropcentral.rplatform.type type} – Platform-specific enums and identifiers.</li>
 *     <li><code>com.raindropcentral.rplatform.utility</code> – Shared helper classes.</li>
 *     <li>{@link com.raindropcentral.rplatform.version version} – Version metadata helpers.</li>
 *     <li>{@link com.raindropcentral.rplatform.view view} – View layer helpers and UI components.</li>
 *     <li>{@link com.raindropcentral.rplatform.workload workload} – Workload scheduling and task
 *     orchestration.</li>
 * </ul>
 */
package com.raindropcentral.rplatform;
