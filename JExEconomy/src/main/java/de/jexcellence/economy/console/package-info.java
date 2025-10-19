/**
 * Console tooling and administrative entry points for managing the economy.
 *
 * <p>The console layer surfaces scripted workflows for economy operators, bridging command-line
 * experiences with the services that coordinate account management. Utilities here normalize
 * localization, permission enforcement, and output formatting so operators receive consistent
 * feedback regardless of locale or server topology.</p>
 *
 * <p>Command handlers are expected to route through these abstractions to ensure privileged
 * operations write to the audit log, respect rate limits, and surface actionable diagnostics
 * during error scenarios. Each console workflow maps directly to a service-layer facade
 * ({@code de.jexcellence.economy.service}) so that parameter validation, currency scale checks, and
 * transaction logging remain centralized.</p>
 *
 * <p>Localization bundles follow the {@code economy.console.*} key pattern. Administrators adding
 * new scripts must register translations for every supported locale alongside default MiniMessage
 * templates and extend the console message registry maintained by this package to reference those
 * keys. Doing so keeps console output synchronized with player-facing prompts and guarantees
 * fallbacks exist for future locales.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
package de.jexcellence.economy.console;
