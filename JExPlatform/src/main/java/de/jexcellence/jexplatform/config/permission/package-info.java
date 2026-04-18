/**
 * Permission-gated configuration sections that resolve values based on
 * a player's effective permissions.
 *
 * <p>The abstract {@link de.jexcellence.jexplatform.config.permission.PermissionBasedSection}
 * provides wildcard matching, best-value selection, and bounds application.
 * Concrete sections cover amplifiers, cooldowns, and durations.
 *
 * @since 1.0.0
 */
package de.jexcellence.jexplatform.config.permission;
