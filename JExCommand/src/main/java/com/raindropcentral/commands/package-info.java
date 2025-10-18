/**
 * <p>Shared abstractions for Bukkit command entry points and their localization-aware
 * {@link de.jexcellence.evaluable.section.ACommandSection command sections}.</p>
 *
 * <h2>Permission handling workflow</h2>
 * <p>Commands must resolve permissions through the section that bootstrapped them to
 * keep messaging consistent across senders.</p>
 * <ul>
 *         <li><b>PermissionsSection</b> handles player-facing checks. When it reports
 *         that a {@code Player} lacks a node, call
 *         {@link de.jexcellence.evaluable.section.PermissionsSection#sendMissingMessage}
 *         so the localized denial is emitted before returning from the handler.</li>
 *         <li><b>CommandError</b> communicates permission failures for non-player
 *         senders. Console invocations should throw a new
 *         {@link de.jexcellence.evaluable.error.CommandError} with the matching
 *         {@link de.jexcellence.evaluable.error.EErrorType} so the command pipeline
 *         can render the appropriate automation-safe response.</li>
 * </ul>
 *
 * <h2>Failure flow examples</h2>
 * <p>The following snippets illustrate the preferred guard clauses for each sender type.</p>
 * <pre>{@code
 * // Player command guard
 * if (this.hasNoPermission(player, permissionNode)) {
 *         return; // sendMissingMessage already localized the error.
 * }
 *
 * // Console command guard
 * if (permissions != null && !permissions.hasPermission(console, permissionNode)) {
 *         final EErrorType denialErrorType = /* map the node to the console denial error */;
 *         throw new CommandError(
 *                 null,
 *                 denialErrorType
 *         );
 * }
 * }</pre>
 *
 * <h2>Consistency requirements</h2>
 * <p>Always route failures through the section-derived helpers or a thrown
 * <b>CommandError</b>. Doing so guarantees future commands reuse the same
 * localization keys, emit uniform formatting, and keep console automation compatible
 * with the existing error contracts.</p>
 */
package com.raindropcentral.commands;
