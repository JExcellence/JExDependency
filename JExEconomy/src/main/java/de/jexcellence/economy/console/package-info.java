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
 * during error scenarios.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
package de.jexcellence.economy.console;
