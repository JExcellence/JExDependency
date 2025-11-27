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
 * <p>The {@code de.jexcellence} namespace defines the public APIs and orchestration layers that
 * downstream plugins consume, while {@code com.raindropcentral.economy} houses the engine and
 * logging implementations. Integrators should depend on the API-facing packages and allow the
 * service layer to broker calls into the implementation namespace, preserving the architecture's
 * clear separation between contracts and execution details.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
package de.jexcellence.economy;

