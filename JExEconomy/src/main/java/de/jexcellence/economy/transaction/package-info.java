/**
 * Transaction domain objects and audit logging utilities for the economy module.
 *
 * <p>This package defines value objects that describe the lifecycle of deposits, withdrawals,
 * transfers, and administrative adjustments alongside the helpers that serialize those events
 * into the shared audit infrastructure. Builders and factories found here centralize currency
 * rounding rules and metadata enrichment so that downstream appenders emit consistent payloads.</p>
 *
 * <p>Consumers should use the provided transaction abstractions when composing ledger operations,
 * ensuring that every mutation is accompanied by immutable records suitable for compliance
 * reviews and reconciliation workflows.</p>
 *
 * <p>Transactions must inherit their scale, rounding mode, and currency compatibility checks from
 * the economy service layer before being submitted to the engine. The resulting payloads should
 * embed localization keys so that console and UI integrations can translate descriptors without
 * recomputing financial context. Adhering to these contracts keeps account balances, audit logs,
 * and localized operator messaging synchronized.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
package de.jexcellence.economy.transaction;
