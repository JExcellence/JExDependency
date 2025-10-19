/**
 * Service layer entry points for interacting with account balances and currencies.
 *
 * <p>Classes within this package coordinate calls between the persistence adapters, engine
 * calculations, and transaction audit logging to present a cohesive API for plugins and
 * administrative tooling. Each service encapsulates validation, concurrency controls, and
 * localization so upstream callers can focus on domain logic.</p>
 *
 * <p>Integrations are expected to rely on these services when issuing balance adjustments,
 * resolving ledger disputes, or broadcasting account updates across clustered servers. Public
 * entry points expose asynchronous variants that marshal work onto the dedicated economy executor
 * while returning {@code CompletionStage}-based handles for plugins that need non-blocking
 * notifications. Implementations surface MiniMessage-compatible localization keys alongside raw
 * numeric data so upstream callers can format responses through {@code JExTranslate} bundles.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
package de.jexcellence.economy.service;
