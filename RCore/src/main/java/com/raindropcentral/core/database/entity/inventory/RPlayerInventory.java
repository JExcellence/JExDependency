package com.raindropcentral.core.database.entity.inventory;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Captures a serialized snapshot of a player's inventory state for a given server
 * in the {@code r_player_inventory} table. Each row references both the owning
 * {@link RPlayer} and the {@link RCentralServer} that produced the snapshot, enabling
 * server-scoped inventory restores and auditing. Item stacks are converted via
 * {@link ItemStackMapConverter} to support Hibernate persistence.
 *
 * <p>Construction should occur on synchronous threads interacting with Bukkit
 * APIs, while persistence and retrieval are delegated to repository executors to
 * avoid blocking scheduler threads.</p>
 * <p>
 * Snapshot creation, replay, and deletion should generate audit trails through
 * {@link CentralLogger CentralLogger}. Emit debug logs for
 * cache misses that require reloading inventory blobs, info logs when capturing or restoring a
 * snapshot for a player/server pair, and warnings when snapshot data is skipped (for example,
 * creative-mode captures). Any serialization failure or persistence exception must be escalated to
 * an error log containing the player UUID and server identifier so shard synchronization issues can
 * be diagnosed quickly.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_inventory")
public class RPlayerInventory extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Foreign-key column {@code player_id} linking this snapshot to the owning player.
     * The association is required and lazily loaded to minimize memory footprint when
     * inventories are fetched without immediately needing profile details.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RPlayer rPlayer;

    /**
     * Foreign-key column {@code server_id} partitioning snapshots by originating server.
     * Ensures restores only occur for matching environments and prevents cross-server
     * contamination of inventories.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private RCentralServer rCentralServer;

    /**
     * Serialized hotbar and main inventory contents stored as a slot-indexed map in
     * {@code inventory}. The {@link ItemStackMapConverter} handles byte-array encoding
     * for database storage.
     */
    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "inventory", columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> inventory = new HashMap<>();

    /**
     * Serialized armor slots persisted to the {@code armor_contents} column. Empty maps
     * are stored instead of {@code null} values to simplify merge semantics.
     */
    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "armor_contents", columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> armor = new HashMap<>();

    /**
     * Serialized ender chest contents mapped to the {@code enderchest} column. Values are
     * lazily copied to prevent shared mutable state when applied back to Bukkit players.
     */
    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "enderchest", columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> enderchest = new HashMap<>();

    /**
     * Framework constructor for JPA. Use the snapshot constructor for new instances.
     */
    protected RPlayerInventory() {}

    /**
     * Builds a snapshot from a live Bukkit {@link Player}. Item stacks are cloned to avoid
     * concurrent modification when persisted asynchronously.
     *
     * @param rCentralServer owning server context used to scope the snapshot
     * @param rPlayer player metadata owning the inventory
     * @param player  live Bukkit player providing inventory contents
     */
    public RPlayerInventory(
            final @NotNull RCentralServer rCentralServer,
            final @NotNull RPlayer rPlayer,
            final @NotNull Player player
    ) {
        this.rCentralServer = Objects.requireNonNull(rCentralServer, "rCentralServer cannot be null");
        this.rPlayer = Objects.requireNonNull(rPlayer, "rPlayer cannot be null");
        Objects.requireNonNull(player, "player cannot be null");

        // Do not create a half-initialized entity when in creative. Persist empty maps instead.
        if (player.getGameMode() == GameMode.CREATIVE) {
            handleCreativeMode(player);
            this.inventory = new HashMap<>();
            this.armor = new HashMap<>();
            this.enderchest = new HashMap<>();
            return;
        }

        this.inventory = extractInventoryMap(player.getInventory().getContents());
        this.armor = extractInventoryMap(player.getInventory().getArmorContents());
        this.enderchest = extractInventoryMap(player.getEnderChest().getContents());
    }

    /**
     * Provides the player association for repository hydration logic.
     *
     * @return linked {@link RPlayer} reference
     */
    public @NotNull RPlayer getRPlayer() {
        return this.rPlayer;
    }

    /**
     * Reassigns the owning player, useful when merging detached entities. The foreign key
     * will be updated on the next flush.
     *
     * @param rPlayer new owning player reference
     */
    public void setRPlayer(final @NotNull RPlayer rPlayer) {
        this.rPlayer = Objects.requireNonNull(rPlayer, "rPlayer cannot be null");
    }

    /**
     * Returns the server scope associated with this inventory snapshot.
     *
     * @return owning {@link RCentralServer}
     */
    public @NotNull RCentralServer getRCentralServer() {
        return this.rCentralServer;
    }

    /**
     * Updates the server association when transferring inventories between shards.
     *
     * @param rCentralServer new server reference
     */
    public void setRCentralServer(final @NotNull RCentralServer rCentralServer) {
        this.rCentralServer = Objects.requireNonNull(rCentralServer, "rCentralServer cannot be null");
    }

    /**
     * Exposes the stored hotbar and inventory contents as an immutable copy to prevent
     * direct mutation of the persistence-managed map.
     *
     * @return copy of slot-indexed inventory contents
     */
    public @NotNull Map<Integer, ItemStack> getInventory() {
        return Map.copyOf(this.inventory);
    }

    /**
     * Replaces the stored inventory contents. Callers should ensure items were cloned
     * prior to invocation when called from asynchronous contexts.
     *
     * @param inventory new inventory map to persist
     */
    public void setInventory(final @NotNull Map<Integer, ItemStack> inventory) {
        Objects.requireNonNull(inventory, "inventory cannot be null");
        this.inventory = new HashMap<>(inventory);
    }

    /**
     * Retrieves the stored armor contents as an immutable snapshot.
     *
     * @return armor slot mapping
     */
    public @NotNull Map<Integer, ItemStack> getArmor() {
        return Map.copyOf(this.armor);
    }

    /**
     * Updates the armor slot mapping.
     *
     * @param armor replacement armor map
     */
    public void setArmor(final @NotNull Map<Integer, ItemStack> armor) {
        Objects.requireNonNull(armor, "armor cannot be null");
        this.armor = new HashMap<>(armor);
    }

    /**
     * Returns the ender chest snapshot as an immutable copy.
     *
     * @return ender chest slot mapping
     */
    public @NotNull Map<Integer, ItemStack> getEnderchest() {
        return Map.copyOf(this.enderchest);
    }

    /**
     * Replaces the stored ender chest contents with the provided mapping.
     *
     * @param enderchest new ender chest map
     */
    public void setEnderchest(final @NotNull Map<Integer, ItemStack> enderchest) {
        Objects.requireNonNull(enderchest, "enderchest cannot be null");
        this.enderchest = new HashMap<>(enderchest);
    }

    /**
     * Applies the stored snapshot to a live player. Should be invoked on the main server
     * thread to comply with Bukkit inventory threading rules.
     *
     * @param player target Bukkit player receiving the snapshot
     */
    public void applyToPlayer(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        player.getInventory().setContents(
                mapToArray(this.inventory, player.getInventory().getSize())
        );
        player.getInventory().setArmorContents(
                mapToArray(this.armor, player.getInventory().getArmorContents().length)
        );
        player.getEnderChest().setContents(
                mapToArray(this.enderchest, player.getEnderChest().getSize())
        );
    }

    /**
     * Refreshes the stored snapshot from a live player. Invoke on synchronous threads to
     * avoid concurrency issues with Bukkit inventory APIs.
     *
     * @param player player to read state from
     */
    public void updateFromPlayer(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");

        this.inventory = extractInventoryMap(player.getInventory().getContents());
        this.armor = extractInventoryMap(player.getInventory().getArmorContents());
        this.enderchest = extractInventoryMap(player.getEnderChest().getContents());
    }

    /**
     * Calculates the total number of persisted slot entries across inventory, armor,
     * and ender chest collections.
     *
     * @return combined slot count
     */
    public int getTotalItemCount() {
        return this.inventory.size() + this.armor.size() + this.enderchest.size();
    }

    /**
     * Indicates whether all stored collections are empty. Useful to skip unnecessary
     * persistence for players without items.
     *
     * @return {@code true} when no items are stored
     */
    public boolean isEmpty() {
        return this.inventory.isEmpty() && this.armor.isEmpty() && this.enderchest.isEmpty();
    }

    /**
     * Logs when a player is in creative mode and inventory persistence is skipped. Invoke from
     * the primary server thread where inventory snapshots are generated to preserve Bukkit's
     * threading guarantees; the method itself performs only synchronous logging.
     *
     * @param player creative-mode player whose inventory is intentionally ignored
     */
    private static void handleCreativeMode(final @NotNull Player player) {
        CentralLogger.getLogger(RPlayerInventory.class.getName())
                .info("Player %s is in creative mode, inventory storage is disabled".formatted(player.getName()));
    }

    /**
     * Converts an array of Bukkit {@link ItemStack} objects to a sparse slot-index map. Callers must
     * supply arrays obtained on synchronous threads to avoid violating Bukkit's inventory access
     * rules. Each {@link ItemStack} is cloned to decouple the persisted state from live objects
     * that may be mutated on other threads.
     *
     * @param items inventory array pulled from a player or container
     * @return map keyed by slot index containing cloned item stacks
     */
    private static Map<Integer, ItemStack> extractInventoryMap(final @Nullable ItemStack[] items) {
        final Map<Integer, ItemStack> map = new HashMap<>();

        for (int slot = 0; slot < items.length; slot++) {
            final ItemStack item = items[slot];
            if (item != null && item.getType() != Material.AIR) {
                map.put(slot, item.clone());
            }
        }

        return map;
    }

    /**
     * Reconstructs an {@link ItemStack} array from a sparse slot-index map. The provided map should
     * contain cloned entries if cross-thread usage is expected; this helper does not perform
     * defensive copying and returns direct references for performance reasons.
     *
     * @param map  sparse slot map typically obtained from persistence
     * @param size expected size of the target inventory array
     * @return populated array ready for Bukkit inventory setters
     */
    private static ItemStack[] mapToArray(
            final @NotNull Map<Integer, ItemStack> map,
            final int size
    ) {
        final ItemStack[] array = new ItemStack[size];
        map.forEach((slot, item) -> {
            if (slot >= 0 && slot < size) {
                array[slot] = item;
            }
        });
        return array;
    }

    /**
     * Produces a concise textual representation of the snapshot for logging or debugging. Safe to
     * call from any thread as it only accesses immutable identifiers and cached counts, avoiding
     * interaction with live Bukkit state.
     *
     * @return formatted description containing key identifiers and item counts
     */
    @Override
    public String toString() {
        return "RPlayerInventory[id=%d, player=%s, server=%s, items=%d]"
                .formatted(
                        getId(),
                        rPlayer != null ? rPlayer.getPlayerName() : "null",
                        rCentralServer != null ? rCentralServer.getServerUuid() : "null",
                        getTotalItemCount()
                );
    }
}