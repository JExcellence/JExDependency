/**
 * Documentation for the shared RDQ module contract.
 *
 * <h2>Lifecycle pipeline</h2>
 * <p>
 * RDQ boots in three distinct stages so platform abstractions, UI components, and
 * storage layers are prepared in a predictable order.
 * </p>
 * <ol>
 *     <li>
 *         <strong>Platform initialization</strong> resolves the active platform edition,
 *         binds shared services, and exposes cross-module singletons that downstream
 *         features depend on.
 *     </li>
 *     <li>
 *         <strong>Component &amp; view setup</strong> builds GUI frames, registers listeners,
 *         and connects feature controllers. Heavy work is deferred to background
 *         tasks and only the wiring needed to create views should run synchronously.
 *     </li>
 *     <li>
 *         <strong>Repository wiring</strong> constructs repositories and data gateways
 *         after the UI is ready so persistence layers can safely publish updates back
 *         through the prepared views.
 *     </li>
 * </ol>
 *
 * <h2>Threading contract</h2>
 * <p>
 * The module guards synchronous Bukkit/Paper access behind a <em>runSync</em> boundary.
 * Blocking IO and expensive calculations must execute off-thread, re-entering
 * the sync boundary only when a result needs to touch the main server thread.
 * Violating this rule risks deadlocks and TPS degradation.
 * </p>
 *
 * <h2>Executor selection</h2>
 * <p>
 * Asynchronous tasks prefer the virtual-thread executor for lightweight fan-out.
 * When virtual threads are unavailable (older runtimes or security managers),
 * fall back to the fixed-size executor exposed by the module. Code submitted to
 * either executor must remain responsive and handle interruption during plugin
 * disable.
 * </p>
 *
 * <h2>Integration points</h2>
 * <p>
 * The {@link com.raindropcentral.rdq2.RDQ} base class coordinates with shared modules during
 * enablement. {@code RPlatform} initialises persistence and metrics services, while the
 * command layer flows through the JExCommand framework. InventoryFramework powers GUI wiring,
 * and repository implementations lean on the rdq-database subpackages for ORM access.
 * </p>
 *
 * <h2>Subpackage index</h2>
 * <dl>
 *     <dt>{@link com.raindropcentral.rdq2.api}</dt>
 *     <dd>Public-facing APIs, domain DTOs, and shared service contracts.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.command}</dt>
 *     <dd>Command factories and command tree wiring for free and premium editions.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.config}</dt>
 *     <dd>Configuration bootstrap helpers, serialization, and validation routines.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.database}</dt>
 *     <dd>Repository abstractions, entity mappers, and persistence integration.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.listener}</dt>
 *     <dd>Event listeners bridging Bukkit events into RDQ services.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.manager}</dt>
 *     <dd>High-level coordinators orchestrating quests, perks, and runtime state.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.requirement}</dt>
 *     <dd>Requirement definitions and evaluators gating quest progression.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.reward}</dt>
 *     <dd>Reward builders and payout pipelines used by quest completion flows.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.service}</dt>
 *     <dd>Core service implementations that power the public API.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.type}</dt>
 *     <dd>Enums and type descriptors shared across requirements and rewards.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.utility}</dt>
 *     <dd>Utility helpers for threading, formatting, and platform bridges.</dd>
 *     <dt>{@link com.raindropcentral.rdq2.view}</dt>
 *     <dd>GUI frames, navigation flows, and view-specific controllers.</dd>
 * </dl>
 */
package com.raindropcentral.rdq2;
