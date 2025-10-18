/**
 * Head catalogue metadata and filtering utilities shared across menu renderers.
 *
 * <p>{@link com.raindropcentral.rplatform.utility.heads.RHead} implementations wrap a
 * skull texture identifier, {@link java.util.UUID}, and translation keys that are expanded
 * just before rendering to a player.  The {@link com.raindropcentral.rplatform.utility.heads.EHeadFilter}
 * enum classifies heads for search and filtering; menu controllers typically preselect a
 * filter, render the head collection, and only translate lore for the visible subset.</p>
 *
 * <p><strong>Usage patterns.</strong> Instantiate concrete {@code RHead} subclasses during
 * plugin bootstrap and retain them in registries keyed by
 * {@link com.raindropcentral.rplatform.utility.heads.RHead#getIdentifier()}.
 * The registry can then be reused by both pagination views and search results to avoid
 * rebuilding builders on each refresh.  When bundling new textures, ensure a translation key
 * in the {@code head.&lt;identifier&gt;} namespace exists so display names and lore resolve.</p>
 *
 * <p><strong>Extension points.</strong> Override {@link com.raindropcentral.rplatform.utility.heads.RHead#getHead(org.bukkit.entity.Player)}
 * if you need to append dynamic lore or conditional enchantments.  Custom filters can extend
 * {@link com.raindropcentral.rplatform.utility.heads.EHeadFilter} or wrap existing filters
 * with higher level registries to expose contextual groupings (e.g., seasonal event heads).</p>
 *
 * <p><strong>Performance.</strong> Large collections should be cached after translation when
 * the same player revisits a menu in quick succession.  Expensive translation requests may
 * be offloaded to async tasks via {@link de.jexcellence.jextranslate.api.TranslationService#buildAsync()},
 * but ensure the {@code ItemStack} assembly executes on the main thread.</p>
 */
package com.raindropcentral.rplatform.utility.heads;
