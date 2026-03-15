/**
 * Serialization helpers for common Bukkit primitives consumed by configuration and database.
 * layers.
 *
 * <p>The classes provide deterministic string and byte array encodings for
 * {@link org.bukkit.Location}, {@link org.bukkit.util.BoundingBox}, and
 * {@link org.bukkit.inventory.ItemStack}.  They are primarily used when persisting menu state
 * or complex quest data to YAML/JSON stores shared across plugins.</p>
 *
 * <p><strong>Usage patterns.</strong> Feed serialized strings into JEConfig-backed documents or
 * plugin-specific storage layers.  The {@link com.raindropcentral.rplatform.serializer.ItemStackSerializer}
 * exposes both binary and Base64 conversions: prefer the binary form when writing to databases
 * that accept {@code BLOB} columns, and switch to Base64 when working with text-only configs.</p>
 *
 * <p><strong>Extension points.</strong> To support additional primitives, follow the established
 * pattern: accept {@code null} inputs, guard against malformed data, and surface informative
 * {@link IllegalArgumentException} messages.  Consumers can extend the serializers to include
 * plugin-specific metadata while retaining compatibility with the core helpers.</p>
 *
 * <p><strong>Performance.</strong> Serializing large inventories can allocate sizable buffers;
 * stream results directly to files when possible to avoid retaining the entire payload in
 * memory.  {@link org.bukkit.util.io.BukkitObjectOutputStream} usage is synchronized on the main
 * thread, so bulk operations should be scheduled off-peak or split across ticks to prevent UI
 * stalls.</p>
 */
package com.raindropcentral.rplatform.serializer;
