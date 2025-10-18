/**
 * Public platform-facing APIs consumed by other RaindropCentral plugins and third-party extensions.
 *
 * <h2>Stability guarantees</h2>
 * <p>The contracts in this package are treated as a <em>stable</em> surface. Methods should only change in
 * backwards-compatible ways (for example, adding default methods) and any removal requires a major version
 * increment coordinated across all downstream modules. Binary compatibility must be preserved for the current
 * major release line.</p>
 *
 * <h2>Extension points</h2>
 * <p>Implementations are expected to extend {@code PlatformAPI} and related factories. New hooks should be
 * added as dedicated interfaces or default methods so RDQ, RCore, and external consumers can adopt them
 * incrementally. When introducing optional capabilities, prefer capability-query methods to keep older
 * consumers operating safely.</p>
 *
 * <h2>Coordination for breaking changes</h2>
 * <p>Before shipping any breaking change, open a coordination issue tagging the RDQ, RCore, and integration
 * owners. The change should include migration notes, test plans, and a compatibility window agreed upon by
 * maintainers. Breaking changes must not ship until all dependent plugins have corresponding updates ready.</p>
 *
 * <h2>Versioning and deprecation</h2>
 * <p>This API follows the RDQ release cadence: bump the minor version when adding backwards-compatible
 * features and the major version for breaking changes. Use {@link java.lang.Deprecated @Deprecated} with
 * {@code since} and {@code forRemoval=true} flags to signal upcoming removals, and document replacements in
 * RDQ and consumer changelogs.</p>
 */
package com.raindropcentral.rplatform.api;
