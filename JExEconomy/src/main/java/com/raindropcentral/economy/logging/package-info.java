/**
 * Transaction logging utilities supporting the JExEconomy audit trail.
 *
 * <p>The logging layer is expected to emit the following event families:
 * <ul>
 *     <li><strong>Balance mutations</strong> covering deposits, withdrawals, and transfers across all currencies.</li>
 *     <li><strong>Administrative overrides</strong> including manual adjustments, chargebacks, and scripted repairs.</li>
 *     <li><strong>System reconciliation</strong> such as scheduled ledger sweeps, rollbacks, and corrective replays.</li>
 * </ul>
 * Each event must be routed through the shared appenders:
 * <ul>
 *     <li>The synchronous <em>audit trail appender</em> that persists immutable records for compliance queries.</li>
 *     <li>The asynchronous <em>operations appender</em> that feeds monitoring dashboards and alerting integrations.</li>
 * </ul>
 *
 * <p>Utility classes in this package are responsible for wiring appenders into the central
 * {@code AuditTrailBus} published by the core economy module. New event publishers must register via
 * the provided factories so that the audit hooks can enrich payloads with actor metadata, request
 * provenance, and correlation identifiers.</p>
 *
 * <p>To extend the logging surface without bypassing the existing hooks:</p>
 * <ul>
 *     <li>Use the exposed {@code TransactionLoggerBuilder} to obtain logger instances bound to the
 *     audit bus. Avoid instantiating raw logger implementations directly.</li>
 *     <li>Decorate emitted payloads with the {@code AuditContext} helpers shipped in this package to
 *     ensure downstream consumers receive consistent metadata.</li>
 *     <li>When introducing new appenders, chain them through the {@code AuditTrailMultiplexer} so
 *     that legacy listeners remain subscribed.</li>
 * </ul>
 */
package com.raindropcentral.economy.logging;
