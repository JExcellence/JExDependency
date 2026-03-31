/**
 * Configuration section mappers that bridge YAML files into runtime toggles across the platform.
 *
 * <p><strong>Runtime behaviour mapping</strong>
 * <p><code>LoggerConfig#load(JavaPlugin)</code> hydrates {@link LoggerSection} data to align
 * logging level routing with package-specific thresholds and console/debug switches at startup.
 * {@link DurationSection} standardises human readable length formats so permission-aware sections can
 * calculate milliseconds, seconds, and derived units without re-parsing user input.</p>
 * <p>The permission-focused views in {@link com.raindropcentral.rplatform.config.permission} depend on these
 * sections to derive effective cooldowns and perk durations for a player at runtime, ensuring YAML edits
 * immediately influence scheduler windows and effect expiry inside quest, perk, and cooldown processors
 * that call into the shared configuration surface.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/config/permission/PermissionCooldownSection.java†L21-L115】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/config/permission/PermissionDurationSection.java†L19-L139】</p>
 *
 * <p><strong>Maintaining sections</strong>
 * <p>All classes extend the ConfigMapper base so expression placeholders resolve through the
 * {@link de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder} supplied by the consuming module.
 * When introducing new keys, keep YAML naming consistent with the camelCase fields exposed here and update
 * any bootstrap loaders that persist defaults (for example the logger bootstrapper) to prevent mismatches.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/config/LoggerSection.java†L12-L42】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/logging/LoggerConfig.java†L17-L125】</p>
 *
 * <p><strong>Update coordination</strong>
 * <p>When refactoring section layouts, ship migration notes with the RDQ and RCore teams so downstream
 * repositories can rebind their permission and duration lookups during the staged lifecycle described in
 * the module guidance. Tests that depend on timing or logging thresholds should be rerun after modifying
 * defaults to confirm runtime consumers still respect the updated ranges.</p>
 */
package com.raindropcentral.rplatform.config;
