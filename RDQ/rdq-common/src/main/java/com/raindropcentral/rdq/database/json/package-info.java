/**
 * JSON parsing utilities used by converters and repositories.
 * <p>
 * The subpackages provide Jackson configuration for translating polymorphic requirement and reward
 * hierarchies, as well as Bukkit-specific value objects such as {@link org.bukkit.inventory.ItemStack},
 * into the string blobs stored by the RDQ entities. Converters in
 * {@link com.raindropcentral.rdq.database.converter} call these parsers during persistence operations
 * that run once repositories are wired via the enable sequence described in
 * {@link com.raindropcentral.rdq.RDQ#onEnable()}.
 * </p>
 */
package com.raindropcentral.rdq.database.json;
