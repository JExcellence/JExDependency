/**
 * Fluent item builder implementations that bridge Adventure components with legacy Bukkit
 * metadata APIs.
 *
 * <p>The {@link com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder} hierarchy
 * adapts per-version differences via {@link com.raindropcentral.rplatform.version.ServerEnvironment}
 * checks, enabling Paper-specific methods while falling back to legacy setters when required.
 * Builders are typically instantiated through
 * {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#item()} and then
 * refined using fluent operations.</p>
 *
 * <p><strong>Usage patterns.</strong> Start with the base builder, set common metadata (name,
 * lore, enchantments), and then specialize using nested packages for potion or skull data.
 * The fluent API returns {@code this} so method chains read naturally inside menu assembly
 * code.</p>
 *
 * <p><strong>Extension points.</strong> Custom builders should extend
 * {@link com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder} and implement the
 * generic contract defined by
 * {@link com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder}.  Override helper
 * methods like {@code setNameLegacy} when additional fallbacks are required for niche server
 * versions.</p>
 *
 * <p><strong>Performance.</strong> Because the builders capture a mutable
 * {@link org.bukkit.inventory.meta.ItemMeta} reference, avoid sharing instances across
 * threads.  For large UI refreshes, create new builders per item but reuse expensive inputs
 * (translations, lore lists) across iterations.</p>
 */
package com.raindropcentral.rplatform.utility.itembuilder;
