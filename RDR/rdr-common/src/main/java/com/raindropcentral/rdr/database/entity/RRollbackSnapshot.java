/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdr.database.entity;

import com.raindropcentral.rplatform.database.converter.ItemStackConverter;
import com.raindropcentral.rplatform.database.converter.ItemStackSlotMapConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted full-state rollback snapshot for one player.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(
    name = "rdr_rollback_snapshot",
    indexes = {
        @Index(name = "idx_rdr_rollback_snapshot_target", columnList = "target_player_uuid"),
        @Index(name = "idx_rdr_rollback_snapshot_created", columnList = "created_at")
    }
)
public class RRollbackSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RDRPlayer player;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "target_player_uuid", nullable = false)
    private UUID targetPlayerUuid;

    @Column(name = "last_known_player_name", length = 64)
    private String lastKnownPlayerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private RollbackTriggerType triggerType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "actor_uuid")
    private UUID actorUuid;

    @Column(name = "actor_name", length = 64)
    private String actorName;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "server_uuid", nullable = false)
    private UUID serverUuid;

    @Column(name = "from_world_name", length = 128)
    private String fromWorldName;

    @Column(name = "to_world_name", length = 128)
    private String toWorldName;

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "main_inventory", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> mainInventory = new HashMap<>();

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "armor_inventory", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> armorInventory = new HashMap<>();

    @Convert(converter = ItemStackConverter.class)
    @Column(name = "offhand_item", columnDefinition = "LONGTEXT")
    private ItemStack offhandItem;

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "ender_chest_inventory", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> enderChestInventory = new HashMap<>();

    @OneToMany(
        mappedBy = "snapshot",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("storageKey ASC")
    private List<RRollbackStorageSnapshot> storageSnapshots = new ArrayList<>();

    /**
     * Creates one rollback snapshot for the supplied player record.
     *
     * @param player owning player record
     * @param targetPlayerUuid target player UUID
     * @param lastKnownPlayerName saved last-known player name, or {@code null} when unavailable
     * @param triggerType trigger that produced the snapshot
     * @param serverUuid server UUID that saved the snapshot
     * @param fromWorldName previous world name, or {@code null} when not applicable
     * @param toWorldName current world name, or {@code null} when not applicable
     */
    public RRollbackSnapshot(
        final @NotNull RDRPlayer player,
        final @NotNull UUID targetPlayerUuid,
        final @Nullable String lastKnownPlayerName,
        final @NotNull RollbackTriggerType triggerType,
        final @NotNull UUID serverUuid,
        final @Nullable String fromWorldName,
        final @Nullable String toWorldName
    ) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.targetPlayerUuid = Objects.requireNonNull(targetPlayerUuid, "targetPlayerUuid cannot be null");
        this.lastKnownPlayerName = normalizeOptionalName(lastKnownPlayerName);
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType cannot be null");
        this.serverUuid = Objects.requireNonNull(serverUuid, "serverUuid cannot be null");
        this.fromWorldName = normalizeOptionalName(fromWorldName);
        this.toWorldName = normalizeOptionalName(toWorldName);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RRollbackSnapshot() {}

    /**
     * Returns the owning player record.
     *
     * @return owning player record
     */
    public @NotNull RDRPlayer getPlayer() {
        return this.player;
    }

    /**
     * Returns the target player UUID.
     *
     * @return target player UUID
     */
    public @NotNull UUID getTargetPlayerUuid() {
        return this.targetPlayerUuid;
    }

    /**
     * Returns the saved last-known player name.
     *
     * @return saved last-known player name, or {@code null} when unavailable
     */
    public @Nullable String getLastKnownPlayerName() {
        return this.lastKnownPlayerName;
    }

    /**
     * Returns the trigger that produced this snapshot.
     *
     * @return snapshot trigger type
     */
    public @NotNull RollbackTriggerType getTriggerType() {
        return this.triggerType;
    }

    /**
     * Returns when the snapshot was created.
     *
     * @return snapshot creation timestamp
     */
    public @NotNull LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Returns the actor UUID associated with this snapshot.
     *
     * @return actor UUID, or {@code null} when the snapshot was automatic
     */
    public @Nullable UUID getActorUuid() {
        return this.actorUuid;
    }

    /**
     * Returns the actor name associated with this snapshot.
     *
     * @return actor name, or {@code null} when the snapshot was automatic
     */
    public @Nullable String getActorName() {
        return this.actorName;
    }

    /**
     * Returns the server UUID that saved this snapshot.
     *
     * @return server UUID
     */
    public @NotNull UUID getServerUuid() {
        return this.serverUuid;
    }

    /**
     * Returns the saved previous world name.
     *
     * @return previous world name, or {@code null} when not applicable
     */
    public @Nullable String getFromWorldName() {
        return this.fromWorldName;
    }

    /**
     * Returns the saved destination/current world name.
     *
     * @return destination/current world name, or {@code null} when not applicable
     */
    public @Nullable String getToWorldName() {
        return this.toWorldName;
    }

    /**
     * Returns a defensive copy of the saved main inventory contents.
     *
     * @return sparse slot-indexed main inventory contents
     */
    public @NotNull Map<Integer, ItemStack> getMainInventory() {
        return Map.copyOf(cloneInventory(this.mainInventory, 36));
    }

    /**
     * Replaces the saved main inventory contents.
     *
     * @param mainInventory replacement sparse slot-indexed main inventory contents
     */
    public void setMainInventory(final @NotNull Map<Integer, ItemStack> mainInventory) {
        this.mainInventory = cloneInventory(Objects.requireNonNull(mainInventory, "mainInventory cannot be null"), 36);
    }

    /**
     * Returns a defensive copy of the saved armor contents.
     *
     * @return sparse slot-indexed armor contents
     */
    public @NotNull Map<Integer, ItemStack> getArmorInventory() {
        return Map.copyOf(cloneInventory(this.armorInventory, 4));
    }

    /**
     * Replaces the saved armor contents.
     *
     * @param armorInventory replacement sparse slot-indexed armor contents
     */
    public void setArmorInventory(final @NotNull Map<Integer, ItemStack> armorInventory) {
        this.armorInventory = cloneInventory(Objects.requireNonNull(armorInventory, "armorInventory cannot be null"), 4);
    }

    /**
     * Returns the saved offhand item.
     *
     * @return cloned saved offhand item, or {@code null} when empty
     */
    public @Nullable ItemStack getOffhandItem() {
        return this.offhandItem == null ? null : this.offhandItem.clone();
    }

    /**
     * Replaces the saved offhand item.
     *
     * @param offhandItem replacement offhand item, or {@code null} when empty
     */
    public void setOffhandItem(final @Nullable ItemStack offhandItem) {
        this.offhandItem = offhandItem == null || offhandItem.isEmpty() ? null : offhandItem.clone();
    }

    /**
     * Returns a defensive copy of the saved ender chest contents.
     *
     * @return sparse slot-indexed ender chest contents
     */
    public @NotNull Map<Integer, ItemStack> getEnderChestInventory() {
        return Map.copyOf(cloneInventory(this.enderChestInventory, 27));
    }

    /**
     * Replaces the saved ender chest contents.
     *
     * @param enderChestInventory replacement sparse slot-indexed ender chest contents
     */
    public void setEnderChestInventory(final @NotNull Map<Integer, ItemStack> enderChestInventory) {
        this.enderChestInventory = cloneInventory(
            Objects.requireNonNull(enderChestInventory, "enderChestInventory cannot be null"),
            27
        );
    }

    /**
     * Returns the saved storage snapshots.
     *
     * @return immutable view of saved storage snapshots
     */
    public @NotNull List<RRollbackStorageSnapshot> getStorageSnapshots() {
        return List.copyOf(this.storageSnapshots);
    }

    /**
     * Adds one child storage snapshot.
     *
     * @param storageSnapshot child snapshot to add
     */
    public void addStorageSnapshot(final @NotNull RRollbackStorageSnapshot storageSnapshot) {
        final RRollbackStorageSnapshot validatedSnapshot = Objects.requireNonNull(
            storageSnapshot,
            "storageSnapshot cannot be null"
        );
        final RRollbackSnapshot currentOwner = validatedSnapshot.getSnapshot();
        if (currentOwner == this && this.storageSnapshots.contains(validatedSnapshot)) {
            return;
        }
        if (currentOwner != null && currentOwner != this) {
            currentOwner.removeStorageSnapshot(validatedSnapshot);
        }
        if (!this.storageSnapshots.contains(validatedSnapshot)) {
            this.storageSnapshots.add(validatedSnapshot);
        }
        validatedSnapshot.setSnapshotInternal(this);
    }

    /**
     * Removes one child storage snapshot.
     *
     * @param storageSnapshot child snapshot to remove
     */
    public void removeStorageSnapshot(final @NotNull RRollbackStorageSnapshot storageSnapshot) {
        final RRollbackStorageSnapshot validatedSnapshot = Objects.requireNonNull(
            storageSnapshot,
            "storageSnapshot cannot be null"
        );
        if (this.storageSnapshots.remove(validatedSnapshot)) {
            validatedSnapshot.setSnapshotInternal(null);
        }
    }

    /**
     * Replaces actor metadata on the snapshot.
     *
     * @param actorUuid actor UUID, or {@code null} for automatic snapshots
     * @param actorName actor name, or {@code null} for automatic snapshots
     */
    public void setActorInfo(final @Nullable UUID actorUuid, final @Nullable String actorName) {
        this.actorUuid = actorUuid;
        this.actorName = normalizeOptionalName(actorName);
    }

    private static @NotNull Map<Integer, ItemStack> cloneInventory(
        final @NotNull Map<Integer, ItemStack> inventory,
        final int maxSlots
    ) {
        final Map<Integer, ItemStack> copiedInventory = new HashMap<>();
        for (final Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null || slot < 0 || slot >= maxSlots || itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            copiedInventory.put(slot, itemStack.clone());
        }
        return copiedInventory;
    }

    private static @Nullable String normalizeOptionalName(final @Nullable String value) {
        if (value == null) {
            return null;
        }
        final String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
