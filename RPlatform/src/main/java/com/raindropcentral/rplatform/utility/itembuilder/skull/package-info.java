/**
 * Skull-specific builder extensions that wrap UUID and texture handling across server
 * versions.
 *
 * <p>The classes in this package encapsulate differences between the legacy profile API and
 * modern Paper components.  Builders such as
 * {@link com.raindropcentral.rplatform.utility.itembuilder.skull.LegacyHeadBuilder} and
 * {@link com.raindropcentral.rplatform.utility.itembuilder.skull.ModernHeadBuilder} expose
 * fluent helpers for owner assignment, texture encoding, and safe fallbacks when textures
 * cannot be resolved.</p>
 *
 * <p><strong>Usage patterns.</strong> Access these builders through
 * {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#head()} and then
 * call the skull-specific setters ({@code setCustomTexture}, {@code fromPlayer}) before
 * finishing with {@code build()}.  Menu controllers often keep a cache of the final
 * {@link org.bukkit.inventory.ItemStack} objects per player to avoid refetching textures.</p>
 *
 * <p><strong>Extension points.</strong> New formats can extend
 * {@link com.raindropcentral.rplatform.utility.itembuilder.skull.AHeadBuilder} or compose the
 * existing builders while providing additional validation logic.  When integrating with remote
 * profile APIs, ensure background downloads marshal texture assignment back onto the main
 * thread.</p>
 *
 * <p><strong>Performance.</strong> Texture application can trigger expensive Base64 decoding.
 * Reuse encoded texture strings and avoid recomputing safe fallbacks when the requested head is
 * unchanged between renders.</p>
 */
package com.raindropcentral.rplatform.utility.itembuilder.skull;
