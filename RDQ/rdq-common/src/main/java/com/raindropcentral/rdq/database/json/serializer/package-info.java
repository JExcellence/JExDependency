/**
 * Custom Jackson serializers and deserializers for Bukkit-specific value objects.
 * <p>
 * {@link com.raindropcentral.rdq.database.json.serializer.ItemStackJSONSerializer} and
 * {@link com.raindropcentral.rdq.database.json.serializer.ItemStackJSONDeserializer} encode
 * {@link org.bukkit.inventory.ItemStack} data into JSON-friendly structures, enabling quest, reward,
 * and requirement entities to persist in-game items via the converters once repositories have been
 * wired during the {@link com.raindropcentral.rdq.RDQ#onEnable()} lifecycle.
 * </p>
 */
package com.raindropcentral.rdq.database.json.serializer;
