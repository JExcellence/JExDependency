/**
 * Premium bounty managers.
 *
 * <p>These classes back the premium edition's bounty subsystem with full database persistence.
 * They are resolved through {@link com.raindropcentral.rdq.manager.RDQManager} during the
 * component stage of {@link com.raindropcentral.rdq.RDQ}'s lifecycle and therefore must remain in
 * sync with the shared initialization contract.</p>
 *
 * <p>Feature gating:</p>
 * <ul>
 *     <li>Expose unlimited bounty creation, deletion, and update workflows, forwarding all writes
 *     to the repositories owned by {@code rdq-common}.</li>
 *     <li>Return premium limits (e.g., negative values for unbounded counts) so UI layers imported
 *     from the common module unlock advanced actions.</li>
 *     <li>Honor asynchronous expectations by delegating to repository futures instead of blocking
 *     the shared executors described in {@code AGENTS.md}.</li>
 * </ul>
 *
 * <p>When the common bounty models or lifecycle signals change, update these classes alongside the
 * free equivalents to keep edition modules aligned.</p>
 */
package com.raindropcentral.rdq.manager.bounty;
