/*
 * RStorage.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.database.entity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raindropcentral.rplatform.database.converter.ItemStackSlotMapConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;

/**
 * Persistent player storage container used for vault and inventory-style item storage.
 *
 * <p>Each storage row belongs to exactly one {@link RDRPlayer}, stores a logical storage key used by
 * gameplay systems to distinguish multiple vaults for the same player, and persists contents as a sparse
 * slot map so empty slots do not consume database space.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(
    name = "rdr_storage",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rdr_storage_player_key",
            columnNames = {"player_id", "storage_key"}
        ),
        @UniqueConstraint(
            name = "uk_rdr_storage_player_hotkey",
            columnNames = {"player_id", "hotkey"}
        )
    }
)
public class RStorage extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RStorage.class);
    private static final TypeReference<Map<UUID, StorageTrustStatus>> TRUSTED_PLAYERS_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RDRPlayer player;

    @Column(name = "storage_key", nullable = false, length = 64)
    private String storageKey;

    @Column(name = "inventory_size", nullable = false)
    private int inventorySize;

    @Column(name = "hotkey")
    private Integer hotkey;

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "inventory", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> inventory = new HashMap<>();

    @Column(name = "trusted_players", nullable = false, columnDefinition = "LONGTEXT")
    private String trustedPlayersJson = "{}";

    @Convert(converter = UUIDConverter.class)
    @Column(name = "lease_server_uuid")
    private UUID leaseServerUuid;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "lease_player_uuid")
    private UUID leasePlayerUuid;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "lease_token")
    private UUID leaseToken;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Transient
    private Map<UUID, StorageTrustStatus> cachedTrustedPlayers;

    /**
     * Creates a new empty storage container for the provided player.
     *
     * @param player owning player entity
     * @param storageKey logical key identifying this storage for the owning player
     * @param inventorySize total slot count supported by this storage
     * @throws NullPointerException if {@code player} or {@code storageKey} is {@code null}
     * @throws IllegalArgumentException if the storage key is blank or the inventory size is not a valid chest size
     */
    public RStorage(
        final @NotNull RDRPlayer player,
        final @NotNull String storageKey,
        final int inventorySize
    ) {
        this.storageKey = normalizeStorageKey(storageKey);
        this.inventorySize = validateInventorySize(inventorySize);
        Objects.requireNonNull(player, "player cannot be null").addStorage(this);
    }

    /**
     * Creates a new storage container with pre-populated contents.
     *
     * @param player owning player entity
     * @param storageKey logical key identifying this storage for the owning player
     * @param inventorySize total slot count supported by this storage
     * @param inventory sparse slot-indexed contents to persist
     * @throws NullPointerException if any parameter except item values is {@code null}
     * @throws IllegalArgumentException if the key or size is invalid, or any slot falls outside the storage size
     */
    public RStorage(
        final @NotNull RDRPlayer player,
        final @NotNull String storageKey,
        final int inventorySize,
        final @NotNull Map<Integer, ItemStack> inventory
    ) {
        this(player, storageKey, inventorySize);
        setInventory(inventory);
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RStorage() {}

    /**
     * Returns the owning player for this storage row.
     *
     * @return owning player entity, or {@code null} only when the relationship has been detached in memory
     */
    public @Nullable RDRPlayer getPlayer() {
        return this.player;
    }

    /**
     * Returns the logical storage key that identifies this vault for its owning player.
     *
     * @return normalized storage key
     */
    public @NotNull String getStorageKey() {
        return this.storageKey;
    }

    /**
     * Updates the logical storage key.
     *
     * @param storageKey replacement key for this storage
     * @throws NullPointerException if {@code storageKey} is {@code null}
     * @throws IllegalArgumentException if the key is blank
     */
    public void setStorageKey(final @NotNull String storageKey) {
        this.storageKey = normalizeStorageKey(storageKey);
    }

    /**
     * Returns the configured slot capacity for this storage.
     *
     * @return total number of supported slots
     */
    public int getInventorySize() {
        return this.inventorySize;
    }

    /**
     * Returns the optional quick-access hotkey currently bound to this storage.
     *
     * @return assigned hotkey number, or {@code null} when no hotkey is bound
     */
    public @Nullable Integer getHotkey() {
        return this.hotkey;
    }

    /**
     * Assigns a quick-access hotkey to this storage.
     *
     * @param hotkey positive hotkey number to bind
     * @throws IllegalArgumentException if {@code hotkey} is less than {@code 1}
     */
    public void setHotkey(final int hotkey) {
        if (hotkey < 1) {
            throw new IllegalArgumentException("hotkey must be greater than zero");
        }
        this.hotkey = hotkey;
    }

    /**
     * Removes any bound quick-access hotkey from this storage.
     */
    public void clearHotkey() {
        this.hotkey = null;
    }

    /**
     * Updates the configured slot capacity.
     *
     * @param inventorySize replacement storage size
     * @throws IllegalArgumentException if the size is not a valid chest size or existing contents would exceed the new size
     */
    public void setInventorySize(final int inventorySize) {
        final int validatedSize = validateInventorySize(inventorySize);
        if (this.inventory.keySet().stream().anyMatch(slot -> slot >= validatedSize)) {
            throw new IllegalArgumentException("Existing inventory contents exceed the requested storage size: " + validatedSize);
        }
        this.inventorySize = validatedSize;
    }

    /**
     * Returns a defensive copy of the stored contents keyed by slot index.
     *
     * @return immutable snapshot of persisted storage contents
     */
    public @NotNull Map<Integer, ItemStack> getInventory() {
        return Map.copyOf(cloneInventory(this.inventory, this.inventorySize));
    }

    /**
     * Replaces the stored contents with the provided sparse slot map.
     *
     * @param inventory replacement contents keyed by slot index
     * @throws NullPointerException if {@code inventory} is {@code null}
     * @throws IllegalArgumentException if any slot falls outside the configured storage size
     */
    public void setInventory(final @NotNull Map<Integer, ItemStack> inventory) {
        Objects.requireNonNull(inventory, "inventory cannot be null");
        this.inventory = cloneInventory(inventory, this.inventorySize);
    }

    /**
     * Returns the number of occupied slots currently persisted in this storage.
     *
     * @return occupied slot count
     */
    public int getStoredSlotCount() {
        return this.inventory.size();
    }

    /**
     * Returns the trusted players configured for this storage.
     *
     * @return defensive copy of trusted player mappings keyed by player UUID
     */
    public @NotNull Map<UUID, StorageTrustStatus> getTrustedPlayers() {
        if (this.cachedTrustedPlayers == null) {
            this.cachedTrustedPlayers = this.parseTrustedPlayers();
        }

        return new HashMap<>(this.cachedTrustedPlayers);
    }

    /**
     * Replaces the trusted player map for this storage.
     *
     * @param trustedPlayers replacement trusted player map
     */
    public void setTrustedPlayers(final @Nullable Map<UUID, StorageTrustStatus> trustedPlayers) {
        final Map<UUID, StorageTrustStatus> safeTrustedPlayers = new HashMap<>();
        if (trustedPlayers != null) {
            for (final Map.Entry<UUID, StorageTrustStatus> entry : trustedPlayers.entrySet()) {
                final UUID playerId = entry.getKey();
                final StorageTrustStatus status = entry.getValue();
                if (playerId == null || status == null || status == StorageTrustStatus.PUBLIC || this.isOwner(playerId)) {
                    continue;
                }

                safeTrustedPlayers.put(playerId, status);
            }
        }

        this.cachedTrustedPlayers = safeTrustedPlayers;

        try {
            this.trustedPlayersJson = OBJECT_MAPPER.writeValueAsString(safeTrustedPlayers);
        } catch (IOException exception) {
            LOGGER.error("Failed to serialize trusted storage players", exception);
            throw new RuntimeException("Failed to serialize trusted storage players", exception);
        }
    }

    /**
     * Returns the effective trust status for the supplied player.
     *
     * @param playerId player UUID to resolve
     * @return effective trust status for the supplied player
     */
    public @NotNull StorageTrustStatus getTrustStatus(final @NotNull UUID playerId) {
        if (this.isOwner(playerId)) {
            return StorageTrustStatus.TRUSTED;
        }

        return this.getTrustedPlayers().getOrDefault(playerId, StorageTrustStatus.PUBLIC);
    }

    /**
     * Updates the trust status for the supplied player.
     *
     * @param playerId player UUID whose trust level should be updated
     * @param status replacement trust status
     */
    public void setTrustStatus(
        final @NotNull UUID playerId,
        final @NotNull StorageTrustStatus status
    ) {
        if (this.isOwner(playerId)) {
            return;
        }

        final Map<UUID, StorageTrustStatus> trustedPlayers = this.getTrustedPlayers();
        if (status == StorageTrustStatus.PUBLIC) {
            trustedPlayers.remove(playerId);
        } else {
            trustedPlayers.put(playerId, status);
        }

        this.setTrustedPlayers(trustedPlayers);
    }

    /**
     * Returns how many trusted players currently have the supplied trust status.
     *
     * @param status trust status to count
     * @return number of trusted players currently assigned to that status
     */
    public int getTrustedPlayerCount(final @NotNull StorageTrustStatus status) {
        int count = 0;
        for (final StorageTrustStatus trustedStatus : this.getTrustedPlayers().values()) {
            if (trustedStatus == status) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns whether the supplied player may access this storage.
     *
     * @param playerId player UUID to evaluate
     * @return {@code true} when the player may deposit into this storage
     */
    public boolean canAccess(final @NotNull UUID playerId) {
        return this.canDeposit(playerId);
    }

    /**
     * Returns whether the supplied player may deposit items into this storage.
     *
     * @param playerId player UUID to evaluate
     * @return {@code true} when the player may deposit items
     */
    public boolean canDeposit(final @NotNull UUID playerId) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId).hasDepositAccess();
    }

    /**
     * Returns whether the supplied player may withdraw items from this storage.
     *
     * @param playerId player UUID to evaluate
     * @return {@code true} when the player may withdraw items
     */
    public boolean canWithdraw(final @NotNull UUID playerId) {
        return this.isOwner(playerId) || this.getTrustStatus(playerId).hasWithdrawAccess();
    }

    /**
     * Returns whether the supplied player UUID matches the storage owner.
     *
     * @param playerId player UUID to evaluate
     * @return {@code true} when the supplied player is the storage owner
     */
    public boolean isOwner(final @NotNull UUID playerId) {
        return this.player != null && Objects.equals(this.player.getIdentifier(), playerId);
    }

    /**
     * Indicates whether this storage currently contains any persisted items.
     *
     * @return {@code true} when no occupied slots are stored
     */
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    /**
     * Returns whether this storage currently has an active lease at the supplied reference time.
     *
     * @param referenceTime point in time used to evaluate lease expiry
     * @return {@code true} when the storage is leased and the lease has not expired
     * @throws NullPointerException if {@code referenceTime} is {@code null}
     */
    public boolean hasActiveLease(final @NotNull LocalDateTime referenceTime) {
        final LocalDateTime validatedReferenceTime = Objects.requireNonNull(referenceTime, "referenceTime cannot be null");
        return this.leaseToken != null
            && this.leaseExpiresAt != null
            && this.leaseExpiresAt.isAfter(validatedReferenceTime);
    }

    /**
     * Returns whether the storage lease is currently held by the supplied session identity.
     *
     * @param serverUuid server UUID that should own the lease
     * @param playerUuid player UUID that should own the lease
     * @param leaseToken unique lease token assigned to the open storage session
     * @return {@code true} when the lease identity matches exactly
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean isLeaseHeldBy(
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken
    ) {
        return Objects.equals(this.leaseServerUuid, Objects.requireNonNull(serverUuid, "serverUuid cannot be null"))
            && Objects.equals(this.leasePlayerUuid, Objects.requireNonNull(playerUuid, "playerUuid cannot be null"))
            && Objects.equals(this.leaseToken, Objects.requireNonNull(leaseToken, "leaseToken cannot be null"));
    }

    /**
     * Acquires or replaces the storage lease with the supplied session identity.
     *
     * @param serverUuid server UUID that will own the lease
     * @param playerUuid player UUID that opened the storage
     * @param leaseToken unique lease token for the open storage session
     * @param leaseExpiresAt time when the lease should expire unless renewed
     * @throws NullPointerException if any argument is {@code null}
     */
    public void acquireLease(
        final @NotNull UUID serverUuid,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken,
        final @NotNull LocalDateTime leaseExpiresAt
    ) {
        this.leaseServerUuid = Objects.requireNonNull(serverUuid, "serverUuid cannot be null");
        this.leasePlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        this.leaseToken = Objects.requireNonNull(leaseToken, "leaseToken cannot be null");
        this.leaseExpiresAt = Objects.requireNonNull(leaseExpiresAt, "leaseExpiresAt cannot be null");
    }

    /**
     * Extends the current lease expiry time.
     *
     * @param leaseExpiresAt replacement expiry timestamp for the active lease
     * @throws NullPointerException if {@code leaseExpiresAt} is {@code null}
     */
    public void renewLease(final @NotNull LocalDateTime leaseExpiresAt) {
        this.leaseExpiresAt = Objects.requireNonNull(leaseExpiresAt, "leaseExpiresAt cannot be null");
    }

    /**
     * Clears the current storage lease metadata.
     */
    public void clearLease() {
        this.leaseServerUuid = null;
        this.leasePlayerUuid = null;
        this.leaseToken = null;
        this.leaseExpiresAt = null;
    }

    void setPlayerInternal(final @Nullable RDRPlayer player) {
        this.player = player;
    }

    private static @NotNull String normalizeStorageKey(final @NotNull String storageKey) {
        final String normalizedKey = Objects.requireNonNull(storageKey, "storageKey cannot be null").trim();
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("storageKey cannot be blank");
        }
        return normalizedKey;
    }

    private static int validateInventorySize(final int inventorySize) {
        if (inventorySize < 9 || inventorySize > 54 || inventorySize % 9 != 0) {
            throw new IllegalArgumentException("inventorySize must be a chest size between 9 and 54");
        }
        return inventorySize;
    }

    private static @NotNull Map<Integer, ItemStack> cloneInventory(
        final @NotNull Map<Integer, ItemStack> inventory,
        final int inventorySize
    ) {
        final Map<Integer, ItemStack> clonedInventory = new HashMap<>();

        for (final Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null) {
                continue;
            }
            if (slot < 0 || slot >= inventorySize) {
                throw new IllegalArgumentException("Inventory slot %d is outside storage size %d".formatted(slot, inventorySize));
            }
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            clonedInventory.put(slot, itemStack.clone());
        }

        return clonedInventory;
    }

    private @NotNull Map<UUID, StorageTrustStatus> parseTrustedPlayers() {
        if (this.trustedPlayersJson == null || this.trustedPlayersJson.isBlank()) {
            return new HashMap<>();
        }

        try {
            final Map<UUID, StorageTrustStatus> parsed = OBJECT_MAPPER.readValue(
                this.trustedPlayersJson,
                TRUSTED_PLAYERS_TYPE
            );
            return parsed == null ? new HashMap<>() : new HashMap<>(parsed);
        } catch (IOException exception) {
            LOGGER.error("Failed to parse trusted storage players JSON", exception);
            throw new RuntimeException("Failed to parse trusted storage players", exception);
        }
    }
}