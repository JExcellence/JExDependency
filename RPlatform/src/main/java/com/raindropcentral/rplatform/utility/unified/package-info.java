/**
 * Centralized factory and contracts that select the correct item builder implementation for
 * the active server environment.
 *
 * <p>{@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory} inspects the
 * {@link com.raindropcentral.rplatform.version.ServerEnvironment} to decide whether to return
 * legacy or modern builders for items, potions, and heads.  The
 * {@link com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder} interface aligns the
 * fluent API surface across those implementations.</p>
 *
 * <p><strong>Usage patterns.</strong> Call the static factory methods ({@code item()},
 * {@code head()}, {@code potion()}) during menu construction.  The returned builder uses the
 * appropriate subclass, so downstream code rarely needs version checks.</p>
 *
 * <p><strong>Extension points.</strong> To register new builder types, add a factory method to
 * {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory} that constructs a
 * custom implementation of {@link com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder}.
 * Keep the selection logic centralized so callers continue to treat the API as version-agnostic.</p>
 *
 * <p><strong>Performance.</strong> The factory performs minimal work beyond environment checks;
 * builders themselves should be reused within a render loop but discarded after the
 * {@code ItemStack} is built to avoid leaking mutable state.</p>
 */
package com.raindropcentral.rplatform.utility.unified;
