/**
 * Primary package for the JExEconomy multi-currency services.
 *
 * <p>The service layout separates currency balance engines, transaction audit capture, and
 * administrative tooling into cooperating but independent modules so Paper integrations
 * can compose only the components they require.</p>
 *
 * <p>Audit logging must flow through the shared appenders to capture both player-facing
 * operations and the back-office reconciliation routines. Extenders should layer on the
 * platform appenders rather than bypassing established audit trails.</p>
 *
 * <p>Console tooling stays aligned with the internationalization facilities supplied by
 * {@code JExTranslate}, keeping localized prompts consistent with their in-game
 * counterparts and eliminating hard-coded strings.</p>
 *
 * <p>Supporting dependencies include:</p>
 * <ul>
 *     <li>{@code JExTranslate} for locale resources and MiniMessage formatting.</li>
 *     <li>Shared audit appenders provided by the platform logging infrastructure for
 *     consistent transaction trails.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
package de.jexcellence.economy;

