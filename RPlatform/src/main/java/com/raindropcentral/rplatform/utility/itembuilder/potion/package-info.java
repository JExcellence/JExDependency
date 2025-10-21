/**
 * Potion-specific builder extensions that handle legacy and modern metadata APIs.
 *
 * <p>{@link com.raindropcentral.rplatform.utility.itembuilder.potion.LegacyPotionBuilder}
 * and {@link com.raindropcentral.rplatform.utility.itembuilder.potion.ModernPotionBuilder}
 * expose fluent helpers for colour, effects, and base potion data while accommodating the
 * differences between pre-1.9 and modern server versions.  The shared
 * {@link com.raindropcentral.rplatform.utility.itembuilder.potion.IPotionBuilder} interface
 * keeps the fluent contract uniform for menu composers.</p>
 *
 * <p><strong>Usage patterns.</strong> Obtain the proper builder via
 * {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#potion()} and
 * then apply effect data before calling {@code build()}.  When rendering many variations,
 * reuse {@link org.bukkit.potion.PotionEffect} instances to reduce allocation pressure.</p>
 *
 * <p><strong>Extension points.</strong> Implement {@link com.raindropcentral.rplatform.utility.itembuilder.potion.IPotionBuilder}
 * if you need to target new APIs or server forks.  Ensure additional metadata setters follow
 * the fluent return type contract so they can chain with existing builder stages.</p>
 *
 * <p><strong>Performance.</strong> Potion metadata updates must occur on the main thread.  For
 * bulk operations, construct effect lists asynchronously but defer {@code ItemMeta} mutation
 * until you are back on the scheduler thread that owns the inventory.</p>
 */
package com.raindropcentral.rplatform.utility.itembuilder.potion;
