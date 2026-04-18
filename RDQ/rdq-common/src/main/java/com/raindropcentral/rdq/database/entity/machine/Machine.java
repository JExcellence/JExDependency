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

package com.raindropcentral.rdq.database.entity.machine;

import com.raindropcentral.rdq.machine.type.EMachineState;
import com.raindropcentral.rdq.machine.type.EMachineType;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a machine in the fabrication system.
 *
 * <p>This entity tracks the machine's owner, type, location, state, fuel level,
 * recipe configuration, and relationships to storage, upgrades, and trusted players.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(
    name = "rdq_machines",
    indexes = {
        @Index(name = "idx_machine_owner", columnList = "owner_uuid"),
        @Index(name = "idx_machine_location", columnList = "world, x, y, z"),
        @Index(name = "idx_machine_type", columnList = "machine_type"),
        @Index(name = "idx_machine_state", columnList = "state")
    }
)
@Getter
@Setter
public class Machine extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The UUID of the player who owns this machine.
     */
    @Column(name = "owner_uuid", nullable = false)
    private UUID ownerUuid;

    /**
     * The type of machine (e.g., FABRICATOR).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", nullable = false, length = 50)
    private EMachineType machineType;

    /**
     * The world name where the machine is located.
     */
    @Column(name = "world", nullable = false, length = 50)
    private String world;

    /**
     * The X coordinate of the machine's core block.
     */
    @Column(name = "x", nullable = false)
    private int x;

    /**
     * The Y coordinate of the machine's core block.
     */
    @Column(name = "y", nullable = false)
    private int y;

    /**
     * The Z coordinate of the machine's core block.
     */
    @Column(name = "z", nullable = false)
    private int z;

    /**
     * The current operational state of the machine.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private EMachineState state = EMachineState.INACTIVE;

    /**
     * The current fuel level of the machine.
     */
    @Column(name = "fuel_level", nullable = false)
    private int fuelLevel = 0;

    /**
     * JSON-serialized recipe data for the machine.
     * Stores the crafting recipe configuration.
     */
    @Column(name = "recipe_data", columnDefinition = "TEXT")
    private String recipeData;

    /**
     * Storage entries associated with this machine.
     */
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MachineStorage> storage = new ArrayList<>();

    /**
     * Upgrades applied to this machine.
     */
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MachineUpgrade> upgrades = new HashSet<>();

    /**
     * Players trusted to interact with this machine.
     */
    @OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MachineTrust> trustedPlayers = new HashSet<>();

    /**
     * Timestamp when this machine was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this machine was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private int version;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected Machine() {
    }

    /**
     * Constructs a new {@code Machine}.
     *
     * @param ownerUuid   the UUID of the player who owns this machine
     * @param machineType the type of machine
     * @param location    the location of the machine's core block
     */
    public Machine(
        @NotNull final UUID ownerUuid,
        @NotNull final EMachineType machineType,
        @NotNull final Location location
    ) {
        this.ownerUuid = ownerUuid;
        this.machineType = machineType;
        this.world = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    /**
     * Checks if this machine is currently active (ACTIVE state).
     *
     * @return true if the machine is in the ACTIVE state, false otherwise
     */
    public boolean isActive() {
        return state == EMachineState.ACTIVE;
    }

    /**
     * Gets the location of this machine's core block.
     *
     * @return the location, or null if the world is not loaded
     */
    @Nullable
    public Location getLocation() {
        var world = Bukkit.getWorld(this.world);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    /**
     * Checks if a player is trusted to interact with this machine.
     *
     * @param playerUuid the UUID of the player to check
     * @return true if the player is the owner or is in the trust list, false otherwise
     */
    public boolean isTrusted(@NotNull final UUID playerUuid) {
        if (ownerUuid.equals(playerUuid)) {
            return true;
        }
        return trustedPlayers.stream()
            .anyMatch(trust -> trust.getTrustedUuid().equals(playerUuid));
    }

    /**
     * Gets the level of a specific upgrade type.
     *
     * @param upgradeType the type of upgrade to check
     * @return the upgrade level, or 0 if the upgrade is not applied
     */
    public int getUpgradeLevel(@NotNull final com.raindropcentral.rdq.machine.type.EUpgradeType upgradeType) {
        return upgrades.stream()
            .filter(upgrade -> upgrade.getUpgradeType() == upgradeType)
            .findFirst()
            .map(MachineUpgrade::getLevel)
            .orElse(0);
    }

    /**
     * Adds a storage entry to this machine.
     *
     * @param storageEntry the storage entry to add
     */
    public void addStorage(@NotNull final MachineStorage storageEntry) {
        storage.add(storageEntry);
        storageEntry.setMachine(this);
    }

    /**
     * Removes a storage entry from this machine.
     *
     * @param storageEntry the storage entry to remove
     */
    public void removeStorage(@NotNull final MachineStorage storageEntry) {
        storage.remove(storageEntry);
        storageEntry.setMachine(null);
    }

    /**
     * Adds an upgrade to this machine.
     *
     * @param upgrade the upgrade to add
     */
    public void addUpgrade(@NotNull final MachineUpgrade upgrade) {
        upgrades.add(upgrade);
        upgrade.setMachine(this);
    }

    /**
     * Removes an upgrade from this machine.
     *
     * @param upgrade the upgrade to remove
     */
    public void removeUpgrade(@NotNull final MachineUpgrade upgrade) {
        upgrades.remove(upgrade);
        upgrade.setMachine(null);
    }

    /**
     * Adds a trusted player to this machine.
     *
     * @param trust the trust entry to add
     */
    public void addTrustedPlayer(@NotNull final MachineTrust trust) {
        trustedPlayers.add(trust);
        trust.setMachine(this);
    }

    /**
     * Removes a trusted player from this machine.
     *
     * @param trust the trust entry to remove
     */
    public void removeTrustedPlayer(@NotNull final MachineTrust trust) {
        trustedPlayers.remove(trust);
        trust.setMachine(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Machine machine)) return false;

        if (this.getId() != null && machine.getId() != null) {
            return this.getId().equals(machine.getId());
        }

        return ownerUuid.equals(machine.ownerUuid) &&
            machineType == machine.machineType &&
            world.equals(machine.world) &&
            x == machine.x &&
            y == machine.y &&
            z == machine.z;
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(ownerUuid, machineType, world, x, y, z);
    }

    @Override
    public String toString() {
        return "Machine{" +
            "id=" + getId() +
            ", ownerUuid=" + ownerUuid +
            ", machineType=" + machineType +
            ", location=" + world + "(" + x + "," + y + "," + z + ")" +
            ", state=" + state +
            ", fuelLevel=" + fuelLevel +
            '}';
    }
}
