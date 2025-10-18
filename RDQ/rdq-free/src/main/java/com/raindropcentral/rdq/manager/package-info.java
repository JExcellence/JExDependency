/**
 * Free edition manager implementations.
 * <p>
 * {@link com.raindropcentral.rdq.manager.RDQFreeManager} wires in-memory managers that satisfy the
 * shared contracts defined in {@code rdq-common}. The class honors the staged lifecycle by emitting
 * initialization diagnostics while deliberately avoiding repository wiring. Services and views rely
 * on the same interfaces as the premium build, ensuring feature access gracefully degrades without
 * breaking navigation or UI flows.
 * </p>
 */
package com.raindropcentral.rdq.manager;
