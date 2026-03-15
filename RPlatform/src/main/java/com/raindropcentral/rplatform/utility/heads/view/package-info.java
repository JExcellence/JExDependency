/**
 * Pre-built {@code RHead} implementations that represent common navigation controls in.
 * multi-step user interfaces.
 *
 * <p>Each class encapsulates a texture, identifier, and translation backing for a specific
 * UI intent (next page, confirm, cancel, return, numeric digits, etc.).  These components are
 * meant to be instantiated once and reused across inventories so the lore and display name
 * resolve consistently via the configured translation service.</p>
 *
 * <p><strong>Usage patterns.</strong> Store singleton instances during GUI bootstrap and feed
 * them to builders produced by {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#head()}.
 * Controllers typically combine these navigation heads with context-specific slot layouts,
 * ensuring pagination controls remain stable even when content inventories change.</p>
 *
 * <p><strong>Extension points.</strong> To create a new navigation affordance, extend one of the
 * base classes and provide a matching translation entry under the {@code head.&lt;id&gt;} namespace.
 * When the action requires player-specific lore (e.g., the number of pending rewards), override
 * {@link com.raindropcentral.rplatform.utility.heads.RHead#getHead(org.bukkit.entity.Player)}
 * and append the additional lines before returning the built {@link org.bukkit.inventory.ItemStack}.</p>
 *
 * <p><strong>Performance.</strong> Keep a per-menu cache when rendering in rapid succession; the
 * head factories rely on translation lookups for each player.  Batched rendering flows should
 * re-use the Adventure {@link net.kyori.adventure.text.Component} instances produced from the
 * translation payload where possible to limit string re-serialization.</p>
 */
package com.raindropcentral.rplatform.utility.heads.view;
