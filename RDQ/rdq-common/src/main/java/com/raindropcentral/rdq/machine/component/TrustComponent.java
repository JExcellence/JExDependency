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

package com.raindropcentral.rdq.machine.component;

import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.database.entity.machine.MachineTrust;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Component responsible for managing machine trust and permissions.
 *
 * <p>This component handles the trust list for machines, validates player permissions,
 * adds/removes trusted players, and checks if players can interact with the machine.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class TrustComponent {

    private final Machine machine;

    /**
     * Constructs a new Trust component.
     *
     * @param machine the machine entity this component manages
     */
    public TrustComponent(final @NotNull Machine machine) {
        this.machine = machine;
    }

    /**
     * Checks if a player is the owner of the machine.
     *
     * @param playerUuid the UUID of the player to check
     * @return true if the player is the owner, false otherwise
     */
    public boolean isOwner(final @NotNull UUID playerUuid) {
        return machine.getOwnerUuid().equals(playerUuid);
    }

    /**
     * Checks if a player is the owner of the machine.
     *
     * @param player the player to check
     * @return true if the player is the owner, false otherwise
     */
    public boolean isOwner(final @NotNull Player player) {
        return isOwner(player.getUniqueId());
    }

    /**
     * Checks if a player is trusted to interact with the machine.
     *
     * <p>A player is considered trusted if they are either the owner or
     * explicitly added to the trust list.
     *
     * @param playerUuid the UUID of the player to check
     * @return true if the player is trusted, false otherwise
     */
    public boolean isTrusted(final @NotNull UUID playerUuid) {
        return machine.isTrusted(playerUuid);
    }

    /**
     * Checks if a player is trusted to interact with the machine.
     *
     * @param player the player to check
     * @return true if the player is trusted, false otherwise
     */
    public boolean isTrusted(final @NotNull Player player) {
        return isTrusted(player.getUniqueId());
    }

    /**
     * Checks if a player can interact with the machine.
     *
     * <p>This is an alias for {@link #isTrusted(UUID)} and provides
     * a more semantic method name for permission checks.
     *
     * @param playerUuid the UUID of the player to check
     * @return true if the player can interact, false otherwise
     */
    public boolean canInteract(final @NotNull UUID playerUuid) {
        return isTrusted(playerUuid);
    }

    /**
     * Checks if a player can interact with the machine.
     *
     * @param player the player to check
     * @return true if the player can interact, false otherwise
     */
    public boolean canInteract(final @NotNull Player player) {
        return canInteract(player.getUniqueId());
    }

    /**
     * Adds a player to the trust list.
     *
     * <p>If the player is already trusted (owner or in trust list),
     * this method returns false. Otherwise, a new trust entry is created.
     *
     * @param playerUuid the UUID of the player to trust
     * @return true if the player was added, false if already trusted
     */
    public boolean addTrustedPlayer(final @NotNull UUID playerUuid) {
        // Check if already trusted
        if (isTrusted(playerUuid)) {
            return false;
        }

        // Create new trust entry
        final MachineTrust trust = new MachineTrust(machine, playerUuid);
        machine.addTrustedPlayer(trust);
        return true;
    }

    /**
     * Adds a player to the trust list.
     *
     * @param player the player to trust
     * @return true if the player was added, false if already trusted
     */
    public boolean addTrustedPlayer(final @NotNull Player player) {
        return addTrustedPlayer(player.getUniqueId());
    }

    /**
     * Removes a player from the trust list.
     *
     * <p>The owner cannot be removed from the trust list. If the player
     * is not in the trust list, this method returns false.
     *
     * @param playerUuid the UUID of the player to remove
     * @return true if the player was removed, false if not in trust list or is owner
     */
    public boolean removeTrustedPlayer(final @NotNull UUID playerUuid) {
        // Cannot remove owner
        if (isOwner(playerUuid)) {
            return false;
        }

        // Find and remove trust entry
        final Optional<MachineTrust> trust = findTrustEntry(playerUuid);
        if (trust.isEmpty()) {
            return false;
        }

        machine.removeTrustedPlayer(trust.get());
        return true;
    }

    /**
     * Removes a player from the trust list.
     *
     * @param player the player to remove
     * @return true if the player was removed, false if not in trust list or is owner
     */
    public boolean removeTrustedPlayer(final @NotNull Player player) {
        return removeTrustedPlayer(player.getUniqueId());
    }

    /**
     * Gets all trusted player UUIDs.
     *
     * <p>This includes the owner and all explicitly trusted players.
     *
     * @return list of trusted player UUIDs
     */
    @NotNull
    public List<UUID> getTrustedPlayers() {
        final List<UUID> trusted = new ArrayList<>();
        
        // Add owner
        trusted.add(machine.getOwnerUuid());
        
        // Add explicitly trusted players
        for (final MachineTrust trust : machine.getTrustedPlayers()) {
            trusted.add(trust.getTrustedUuid());
        }
        
        return trusted;
    }

    /**
     * Gets explicitly trusted player UUIDs (excluding owner).
     *
     * @return list of explicitly trusted player UUIDs
     */
    @NotNull
    public List<UUID> getExplicitlyTrustedPlayers() {
        final List<UUID> trusted = new ArrayList<>();
        
        for (final MachineTrust trust : machine.getTrustedPlayers()) {
            trusted.add(trust.getTrustedUuid());
        }
        
        return trusted;
    }

    /**
     * Gets the owner UUID.
     *
     * @return the owner's UUID
     */
    @NotNull
    public UUID getOwnerUuid() {
        return machine.getOwnerUuid();
    }

    /**
     * Gets the number of trusted players (including owner).
     *
     * @return the total number of trusted players
     */
    public int getTrustedPlayerCount() {
        return 1 + machine.getTrustedPlayers().size();
    }

    /**
     * Gets the number of explicitly trusted players (excluding owner).
     *
     * @return the number of explicitly trusted players
     */
    public int getExplicitlyTrustedPlayerCount() {
        return machine.getTrustedPlayers().size();
    }

    /**
     * Checks if the trust list is empty (only owner is trusted).
     *
     * @return true if only the owner is trusted, false if others are trusted
     */
    public boolean isTrustListEmpty() {
        return machine.getTrustedPlayers().isEmpty();
    }

    /**
     * Clears all explicitly trusted players.
     *
     * <p>This removes all trust entries but does not affect the owner.
     *
     * @return the number of players removed
     */
    public int clearTrustList() {
        final int count = machine.getTrustedPlayers().size();
        final List<MachineTrust> toRemove = new ArrayList<>(machine.getTrustedPlayers());
        
        for (final MachineTrust trust : toRemove) {
            machine.removeTrustedPlayer(trust);
        }
        
        return count;
    }

    /**
     * Checks if a specific player is explicitly trusted (not owner).
     *
     * @param playerUuid the UUID of the player to check
     * @return true if explicitly trusted, false otherwise
     */
    public boolean isExplicitlyTrusted(final @NotNull UUID playerUuid) {
        if (isOwner(playerUuid)) {
            return false;
        }
        
        return findTrustEntry(playerUuid).isPresent();
    }

    /**
     * Checks if a specific player is explicitly trusted (not owner).
     *
     * @param player the player to check
     * @return true if explicitly trusted, false otherwise
     */
    public boolean isExplicitlyTrusted(final @NotNull Player player) {
        return isExplicitlyTrusted(player.getUniqueId());
    }

    /**
     * Validates if a player has permission to modify the trust list.
     *
     * <p>Only the owner can modify the trust list.
     *
     * @param playerUuid the UUID of the player to check
     * @return true if the player can modify the trust list, false otherwise
     */
    public boolean canModifyTrustList(final @NotNull UUID playerUuid) {
        return isOwner(playerUuid);
    }

    /**
     * Validates if a player has permission to modify the trust list.
     *
     * @param player the player to check
     * @return true if the player can modify the trust list, false otherwise
     */
    public boolean canModifyTrustList(final @NotNull Player player) {
        return canModifyTrustList(player.getUniqueId());
    }

    /**
     * Finds a trust entry for a specific player.
     *
     * @param playerUuid the UUID of the player
     * @return optional containing the trust entry if found
     */
    @NotNull
    private Optional<MachineTrust> findTrustEntry(final @NotNull UUID playerUuid) {
        return machine.getTrustedPlayers().stream()
            .filter(trust -> trust.getTrustedUuid().equals(playerUuid))
            .findFirst();
    }
}
