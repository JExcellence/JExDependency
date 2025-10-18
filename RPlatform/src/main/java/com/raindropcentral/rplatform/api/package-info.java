/**
 * Public platform-facing APIs consumed by other RaindropCentral plugins and third-party extensions.
 *
 * <h2>Stability guarantees</h2>
 * <p>The contracts in this package are treated as a <em>stable</em> surface. Methods should only change in
 * backwards-compatible ways (for example, adding default methods) and any removal requires a major version
 * increment coordinated across all downstream modules. Binary compatibility must be preserved for the current
 * major release line. When adding optional behaviours, prefer additive interfaces or capability toggles so that
 * compiled consumers continue to run without recompilation.</p>
 *
 * <h2>Adapter strategies</h2>
 * <p>Scheduler, metrics, and translation adapters exposed here must delegate through the platform factories
 * shared by RCore and RDQ. New adapters should provide no-op fallbacks for features unavailable on legacy
 * servers, log capability detection at startup, and preserve the threading guarantees described in
 * {@code AGENTS.md}. Keep adapter selection logic isolated to the factory layer so downstream consumers only
 * depend on the stable abstractions.</p>
 *
 * <h2>Environment-specific bindings</h2>
 * <p>{@link com.raindropcentral.rplatform.api.PlatformAPIFactory PlatformAPIFactory} detects supported server
 * targets and reflectively wires the corresponding {@code api.impl} implementation. Detection first checks for
 * Folia classes, then modern Paper entry points, and finally falls back to Spigot, ensuring every
 * {@link com.raindropcentral.rplatform.api.PlatformType platform type} has a matching adapter ready for
 * instantiation. When adding another environment, introduce a new {@code PlatformType} entry, extend the
 * switch inside {@link com.raindropcentral.rplatform.api.PlatformAPIFactory#create}, and implement the
 * reflective constructor lookup with an ordered fallback so production servers pick the best match available.</p>
 *
 * <h2>Extension alignment</h2>
 * <p>Whenever you extend {@link com.raindropcentral.rplatform.api.PlatformAPI PlatformAPI} or related entry
 * points, update every implementation under {@code api.impl} to keep capabilities aligned across Paper,
 * Folia, and Spigot. Treat new default methods as compatibility shims and gate breaking additions behind
 * coordinated release planning. Validate the update by constructing an {@link com.raindropcentral.rplatform.api.PlatformAPI}
 * through the factory for each runtime so test suites and downstream plugins exercise consistent behaviour.</p>
 *
 * <h2>Coordination for breaking changes</h2>
 * <p>Before shipping any breaking change, open a coordination issue tagging the RDQ, RCore, and integration
 * owners. The change should include migration notes, test plans, and a compatibility window agreed upon by
 * maintainers. Breaking changes must not ship until all dependent plugins have corresponding updates ready.
 * Communicate the planned merge date during weekly RDQ syncs, surface status updates in the shared #rdq-platform
 * channel, and secure approvals from RDQ release managers before merging.</p>
 *
 * <h2>Versioning and deprecation</h2>
 * <p>This API follows the RDQ release cadence: bump the minor version when adding backwards-compatible
 * features and the major version for breaking changes. Use {@link java.lang.Deprecated @Deprecated} with
 * {@code since} and {@code forRemoval=true} flags to signal upcoming removals, and document replacements in
 * RDQ and consumer changelogs.</p>
 *
 * <h2>Deprecation &amp; schema rollout steps</h2>
 * <ol>
 *     <li>Introduce new fields, methods, or tables alongside the legacy surface. Provide adapter shims that
 *     bridge old invocations onto the new implementations so existing consumers continue to function.</li>
 *     <li>Annotate deprecated APIs with {@code @Deprecated} and document migration snippets. For schema
 *     changes, add background migrations or lazy translation layers so RDQ and third-party plugins can read
 *     both formats during the transition.</li>
 *     <li>Publish release notes and sample upgrade paths, then monitor RDQ telemetry to verify consumers
 *     adopt the new surface before scheduling removal.</li>
 *     <li>After the agreed upon support window, remove the shims and legacy schema, incrementing the major
 *     version if binary compatibility breaks.</li>
 * </ol>
 */
package com.raindropcentral.rplatform.api;
