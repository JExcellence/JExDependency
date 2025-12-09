/**
 * Coordinates edition-aware managers that share the staged RDQ lifecycle.
 * <p>
 * The {@link com.raindropcentral.rdq2.manager.RDQManager} base type exposes hooks that mirror the
 * plugin bootstrap order documented in {@code RDQ/AGENTS.md}: platform primitives are prepared first,
 * view frames are wired next, and repositories are attached last. Concrete implementations such as
 * the free and premium editions populate those hooks by composing service-specific managers for
 * bounties, quests, ranks, and perks. Each sub-manager focuses on a domain contract while the
 * edition-level coordinator decides which backing services should be used and when they should be
 * initialized or shut down.
 * </p>
 * <p>
 * Lifecycle-aware coordination keeps repository usage safe. Premium implementations bind to
 * asynchronous repositories (for example {@code RBountyRepository}) only after the platform and
 * views are ready to consume data, while the free edition swaps those dependencies for lightweight
 * in-memory scaffolding. Both editions enter and exit through {@link com.raindropcentral.rdq2.manager.RDQManager#initialize()}
 * and {@link com.raindropcentral.rdq2.manager.RDQManager#shutdown()}, ensuring repository pools,
 * services, and UI event bridges share the same lifecycle boundaries regardless of installed
 * edition.
 * </p>
 */
package com.raindropcentral.rdq2.manager;
