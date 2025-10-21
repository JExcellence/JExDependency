/**
 * Inventory aggregates responsible for capturing player loadouts on a per-server basis.
 * <p>
 * {@link com.raindropcentral.core.database.entity.inventory.RPlayerInventory} binds
 * {@link com.raindropcentral.core.database.entity.player.RPlayer} snapshots to
 * {@link com.raindropcentral.core.database.entity.server.RServer} contexts. Inventories are optional
 * members of the broader player aggregate and should only be attached when synchronous capture has
 * completed.
 * </p>
 * <p>
 * Persistence uses {@link com.raindropcentral.rplatform.database.converter.ItemStackMapConverter} to
 * serialize {@code Map<Integer, ItemStack>} payloads into long-text columns. Ensure maps are copied
 * defensively before hand-off to maintain Hibernate dirty-tracking and to avoid mutating Bukkit
 * state outside of the sync thread. When a player is in creative mode the constructor intentionally
 * persists empty maps and relies on
 * {@link com.raindropcentral.rplatform.logging.CentralLogger} to record the bypass event.
 * </p>
 */
package com.raindropcentral.core.database.entity.inventory;
