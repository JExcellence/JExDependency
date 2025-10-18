/**
 * Core services shared across the RaindropCentral platform.
 *
 * <p>This module expects contributors to leverage asynchronous execution primitives consistently.
 * Use {@link java.util.concurrent.CompletableFuture} for non-blocking workflows and always supply
 * the module-provided executors (for example, {@code ServiceExecutionContext#getExecutor()}) when
 * scheduling callbacks to preserve deterministic threading.</p>
 *
 * <p>All public entry points must defensively guard external inputs using
 * {@link java.util.Objects#requireNonNull(Object, String)} or equivalent checks. These guards
 * prevent latent {@link java.lang.NullPointerException} issues while documenting required
 * invariants.</p>
 *
 * <p>The package is organized into the following sub-packages to help locate components quickly:</p>
 * <ul>
 *     <li>{@code api} – Public-facing interfaces, DTOs, and lifecycle hooks consumed by other
 *     modules.</li>
 *     <li>{@code database} – Persistence adapters, entity mappings, and repository abstractions.</li>
 *     <li>{@code service} – Internal service implementations orchestrating API calls and
 *     persistence workflows.</li>
 * </ul>
 */
package com.raindropcentral.core;
