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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import com.raindropcentral.rplatform.proxy.NetworkLocation;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Persistent town aggregate for claims, roles, bank balances, and protections.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_towns")
public class RTown extends BaseEntity {

    /** Mayor system role identifier. */
    public static final String MAYOR_ROLE_ID = "MAYOR";
    /** Default member role identifier. */
    public static final String MEMBER_ROLE_ID = "MEMBER";
    /** Public protection role identifier. */
    public static final String PUBLIC_ROLE_ID = "PUBLIC";
    /** Restricted protection role identifier. */
    public static final String RESTRICTED_ROLE_ID = "RESTRICTED";

    private static final String DEFAULT_TOWN_COLOR_HEX = "#55CDFC";

    @Column(name = "town_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID townUuid;

    @Column(name = "mayor_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID mayorUuid;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "town_name", nullable = false, unique = true, length = 64)
    private String townName;

    @Column(name = "town_color_hex", nullable = false, length = 16)
    private String townColorHex;

    @Column(name = "archetype_id", length = 32)
    private String archetypeId;

    @Column(name = "town_level", nullable = false)
    private int townLevel;

    @Column(name = "nexus_level")
    private Integer nexusLevel;

    @Column(name = "buffered_fuel_units", nullable = false)
    private double bufferedFuelUnits;

    @Column(name = "founded", nullable = false)
    private long founded;

    @Column(name = "last_archetype_change_at", nullable = false)
    private long lastArchetypeChangeAt;

    @Column(name = "aggregate_town_playtime_ticks", nullable = false)
    private long aggregateTownPlaytimeTicks;

    @Convert(converter = LocationConverter.class)
    @Column(name = "nexus_location")
    private Location nexusLocation;

    @Convert(converter = LocationConverter.class)
    @Column(name = "town_spawn_location")
    private Location townSpawnLocation;

    @Column(name = "nexus_server_id", length = 64)
    private String nexusServerId;

    @Column(name = "town_spawn_server_id", length = 64)
    private String townSpawnServerId;

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "town_uuid", referencedColumnName = "town_uuid")
    @OrderBy("player_uuid ASC")
    private Set<RDTPlayer> members = new LinkedHashSet<>();

    @OneToMany(mappedBy = "town", fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("rolePriority ASC, roleName ASC")
    private List<TownRole> roles = new ArrayList<>();

    @OneToMany(mappedBy = "town", fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("worldName ASC, xLoc ASC, zLoc ASC")
    private List<RTownChunk> chunks = new ArrayList<>();

    @OneToMany(mappedBy = "town", fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    private List<TownInvite> invites = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_town_protections", joinColumns = @JoinColumn(name = "town_id_fk"))
    @Column(name = "required_role_id", nullable = false, length = 64)
    private Map<String, String> protections = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_town_bank_balances", joinColumns = @JoinColumn(name = "town_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> bankBalances = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_town_level_currency_progress", joinColumns = @JoinColumn(name = "town_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> levelCurrencyProgress = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "level_item_progress", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> levelItemProgress = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "shared_bank_storage", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> sharedBankStorage = new LinkedHashMap<>();

    /** Creates a town. */
    public RTown(
        final @NotNull UUID townUuid,
        final @NotNull UUID mayorUuid,
        final @NotNull String townName,
        final @Nullable Location nexusLocation
    ) {
        this(townUuid, mayorUuid, townName, nexusLocation, null);
    }

    /** Creates a town with nexus and spawn locations. */
    public RTown(
        final @NotNull UUID townUuid,
        final @NotNull UUID mayorUuid,
        final @NotNull String townName,
        final @Nullable Location nexusLocation,
        final @Nullable Location townSpawnLocation
    ) {
        this.townUuid = Objects.requireNonNull(townUuid, "townUuid");
        this.mayorUuid = Objects.requireNonNull(mayorUuid, "mayorUuid");
        this.active = true;
        this.townName = normalizeTownName(townName);
        this.townColorHex = DEFAULT_TOWN_COLOR_HEX;
        this.townLevel = 1;
        this.nexusLevel = 1;
        this.bufferedFuelUnits = 0.0D;
        this.founded = System.currentTimeMillis();
        this.lastArchetypeChangeAt = 0L;
        this.nexusLocation = nexusLocation == null ? null : nexusLocation.clone();
        this.townSpawnLocation = townSpawnLocation == null ? null : townSpawnLocation.clone();
        this.ensureDefaultRoles();
    }

    /** JPA constructor. */
    protected RTown() {
    }

    /** Returns the stable town identifier. */
    public @NotNull UUID getIdentifier() {
        return this.townUuid;
    }

    /** Returns the stable town identifier. */
    public @NotNull UUID getTownUUID() {
        return this.townUuid;
    }

    /** Returns the stable town identifier. */
    public @NotNull UUID getTownUuid() {
        return this.townUuid;
    }

    /** Returns the town name. */
    public @NotNull String getTownName() {
        return this.townName;
    }

    /** Returns the town name. */
    public @NotNull String getName() {
        return this.townName;
    }

    /** Replaces the town name. */
    public void setTownName(final @NotNull String townName) {
        this.townName = normalizeTownName(townName);
    }

    /** Returns the town color hex. */
    public @NotNull String getTownColorHex() {
        return this.townColorHex;
    }

    /** Replaces the town color hex. */
    public void setTownColorHex(final @Nullable String townColorHex) {
        this.townColorHex = normalizeTownColorHex(townColorHex);
    }

    /** Returns the current archetype identifier. */
    public @Nullable String getArchetypeId() {
        return this.archetypeId;
    }

    /** Returns the current archetype. */
    public @Nullable TownArchetype getArchetype() {
        return TownArchetype.fromString(this.archetypeId);
    }

    /** Replaces the current archetype. */
    public void setArchetype(final @Nullable TownArchetype archetype) {
        this.archetypeId = archetype == null ? null : archetype.name();
    }

    /** Replaces the current archetype identifier. */
    public void setArchetypeId(final @Nullable String archetypeId) {
        final TownArchetype archetype = TownArchetype.fromString(archetypeId);
        this.archetypeId = archetype == null ? null : archetype.name();
    }

    /** Returns the mayor UUID. */
    public @NotNull UUID getMayorUUID() {
        return this.mayorUuid;
    }

    /** Returns the mayor UUID. */
    public @NotNull UUID getMayor() {
        return this.mayorUuid;
    }

    /** Returns whether the town is active. */
    public boolean getActive() {
        return this.active;
    }

    /** Replaces the town active state. */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /** Returns the town level. */
    public int getTownLevel() {
        return this.townLevel;
    }

    /** Replaces the town level. */
    public void setTownLevel(final int townLevel) {
        this.townLevel = Math.max(1, townLevel);
    }

    /** Returns whether the persisted nexus level has been backfilled. */
    public boolean hasPersistedNexusLevel() {
        return this.nexusLevel != null;
    }

    /** Backfills legacy towns whose old town level represented the nexus level directly. */
    public boolean backfillLegacyNexusLevelIfNeeded() {
        if (this.nexusLevel != null) {
            return false;
        }
        this.nexusLevel = Math.max(1, this.townLevel);
        return true;
    }

    /** Returns the persisted nexus progression level. */
    public int getNexusLevel() {
        return Math.max(1, this.nexusLevel == null ? this.townLevel : this.nexusLevel);
    }

    /** Replaces the persisted nexus progression level. */
    public void setNexusLevel(final int nexusLevel) {
        this.nexusLevel = Math.max(1, nexusLevel);
        this.recalculateTownLevel();
    }

    /** Returns the currently buffered FE already withdrawn from fuel items. */
    public double getBufferedFuelUnits() {
        return Math.max(0.0D, this.bufferedFuelUnits);
    }

    /** Replaces the currently buffered FE already withdrawn from fuel items. */
    public void setBufferedFuelUnits(final double bufferedFuelUnits) {
        this.bufferedFuelUnits = Math.max(0.0D, bufferedFuelUnits);
    }

    /** Returns the cached composite town level derived from the nexus and non-nexus chunks. */
    public int recalculateTownLevel() {
        this.backfillLegacyNexusLevelIfNeeded();
        this.townLevel = this.calculateCompositeTownLevel();
        return this.townLevel;
    }

    /** Calculates the composite town level without mutating any additional town state. */
    public int calculateCompositeTownLevel() {
        int compositeLevel = this.getNexusLevel();
        for (final RTownChunk chunk : this.chunks) {
            if (chunk.getChunkType() == ChunkType.NEXUS) {
                continue;
            }
            compositeLevel += Math.max(1, chunk.getChunkLevel());
        }
        return Math.max(1, compositeLevel);
    }

    /** Returns the last archetype change timestamp in epoch milliseconds. */
    public long getLastArchetypeChangeAt() {
        return this.lastArchetypeChangeAt;
    }

    /** Replaces the last archetype change timestamp in epoch milliseconds. */
    public void setLastArchetypeChangeAt(final long lastArchetypeChangeAt) {
        this.lastArchetypeChangeAt = Math.max(0L, lastArchetypeChangeAt);
    }

    /** Returns aggregate town playtime in ticks. */
    public long getAggregateTownPlaytimeTicks() {
        return this.aggregateTownPlaytimeTicks;
    }

    /** Adds aggregate town playtime. */
    public void addAggregateTownPlaytimeTicks(final long additionalTicks) {
        if (additionalTicks > 0L) {
            this.aggregateTownPlaytimeTicks += additionalTicks;
        }
    }

    /** Returns the founding timestamp. */
    public long getFounded() {
        return this.founded;
    }

    /** Returns town members. */
    public @NotNull Set<RDTPlayer> getMembers() {
        return Set.copyOf(this.members);
    }

    /** Adds a town member. */
    public void addMember(final @NotNull RDTPlayer player) {
        final RDTPlayer validatedPlayer = Objects.requireNonNull(player, "player");
        if (this.members.stream().anyMatch(existing -> Objects.equals(existing.getIdentifier(), validatedPlayer.getIdentifier()))) {
            return;
        }
        validatedPlayer.setTownUUID(this.townUuid);
        if (Objects.equals(validatedPlayer.getIdentifier(), this.mayorUuid)) {
            validatedPlayer.setTownRoleId(MAYOR_ROLE_ID);
        } else if (validatedPlayer.getTownRoleId() == null) {
            validatedPlayer.setTownRoleId(MEMBER_ROLE_ID);
        }
        this.syncPlayerPermissionsFromRole(validatedPlayer);
        this.members.add(validatedPlayer);
    }

    /** Removes a town member. */
    public void removeMember(final @NotNull RDTPlayer player) {
        final RDTPlayer validatedPlayer = Objects.requireNonNull(player, "player");
        if (this.members.removeIf(existing -> Objects.equals(existing.getIdentifier(), validatedPlayer.getIdentifier()))) {
            validatedPlayer.setTownUUID(null);
        }
    }

    /** Returns town roles. */
    public @NotNull List<TownRole> getRoles() {
        this.ensureDefaultRoles();
        return List.copyOf(this.roles);
    }

    /** Returns claimed chunks. */
    public @NotNull List<RTownChunk> getChunks() {
        return List.copyOf(this.chunks);
    }

    /** Returns invites. */
    public @NotNull List<TownInvite> getInvites() {
        return List.copyOf(this.invites);
    }

    /** Adds a claimed chunk. */
    public void addChunk(final @NotNull RTownChunk chunk) {
        final RTownChunk validatedChunk = Objects.requireNonNull(chunk, "chunk");
        if (this.chunks.stream().anyMatch(existing ->
            Objects.equals(existing.getWorldName(), validatedChunk.getWorldName())
                && existing.getX() == validatedChunk.getX()
                && existing.getZ() == validatedChunk.getZ()
        )) {
            return;
        }
        this.chunks.add(validatedChunk);
        this.recalculateTownLevel();
    }

    /** Removes a claimed chunk. */
    public boolean removeChunk(final @Nullable RTownChunk chunk) {
        if (chunk == null) {
            return false;
        }
        final boolean removed = this.chunks.removeIf(existing ->
            Objects.equals(existing.getWorldName(), chunk.getWorldName())
                && existing.getX() == chunk.getX()
                && existing.getZ() == chunk.getZ()
        );
        if (removed) {
            this.recalculateTownLevel();
        }
        return removed;
    }

    /** Finds a claimed chunk by world and coordinates. */
    public @Nullable RTownChunk findChunk(
        final @Nullable String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        return this.chunks.stream()
            .filter(chunk ->
                chunk.getWorldName().equalsIgnoreCase(worldName)
                    && chunk.getX() == chunkX
                    && chunk.getZ() == chunkZ
            )
            .findFirst()
            .orElse(null);
    }

    /** Finds a role by identifier. */
    public @Nullable TownRole findRoleById(final @Nullable String roleId) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        this.ensureDefaultRoles();
        return this.roles.stream()
            .filter(role -> Objects.equals(role.getRoleId(), normalizedRoleId))
            .findFirst()
            .orElse(null);
    }

    /** Adds a custom role. */
    public boolean addRole(
        final @NotNull String roleId,
        final @NotNull String roleName,
        final @NotNull Set<String> permissions
    ) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        if (isDefaultRoleId(normalizedRoleId) || this.findRoleById(normalizedRoleId) != null) {
            return false;
        }
        this.roles.add(new TownRole(this, normalizedRoleId, roleName, permissions, 50, false));
        return true;
    }

    /** Removes a custom role. */
    public boolean removeRoleById(final @Nullable String roleId) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        if (isDefaultRoleId(normalizedRoleId)) {
            return false;
        }
        return this.roles.removeIf(role -> Objects.equals(role.getRoleId(), normalizedRoleId));
    }

    /** Returns whether a role identifier is built in. */
    public static boolean isDefaultRoleId(final @Nullable String roleId) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        return Objects.equals(normalizedRoleId, MAYOR_ROLE_ID)
            || Objects.equals(normalizedRoleId, MEMBER_ROLE_ID)
            || Objects.equals(normalizedRoleId, PUBLIC_ROLE_ID)
            || Objects.equals(normalizedRoleId, RESTRICTED_ROLE_ID);
    }

    /** Normalizes a role identifier. */
    public static @NotNull String normalizeRoleId(final @Nullable String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return MEMBER_ROLE_ID;
        }
        return roleId.trim().toUpperCase(Locale.ROOT);
    }

    /** Resolves the default priority for a role identifier. */
    public static int resolveDefaultRolePriority(final @Nullable String roleId) {
        return switch (normalizeRoleId(roleId)) {
            case RESTRICTED_ROLE_ID -> 0;
            case PUBLIC_ROLE_ID -> 10;
            case MEMBER_ROLE_ID -> 20;
            case MAYOR_ROLE_ID -> 100;
            default -> 50;
        };
    }

    /** Returns whether a player has a town permission. */
    public boolean hasTownPermission(final @Nullable RDTPlayer player, final @Nullable TownPermissions permission) {
        return player != null && permission != null && player.hasTownPermission(permission);
    }

    /** Returns whether a role grants a town permission. */
    public boolean hasRolePermission(final @Nullable String roleId, final @Nullable TownPermissions permission) {
        final TownRole role = this.findRoleById(roleId);
        return role != null && role.hasPermission(permission);
    }

    /** Returns whether a player has an active invite. */
    public boolean isPlayerInvited(final @Nullable UUID playerUuid) {
        return playerUuid != null && this.invites.stream()
            .anyMatch(invite -> invite.isActive() && Objects.equals(invite.getInvitedPlayerUuid(), playerUuid));
    }

    /** Creates a town invite. */
    public boolean invitePlayer(final @Nullable UUID playerUuid) {
        if (playerUuid == null || this.isPlayerInvited(playerUuid)) {
            return false;
        }
        this.invites.add(new TownInvite(this, playerUuid, this.mayorUuid));
        return true;
    }

    /** Expires a town invite. */
    public boolean uninvitePlayer(final @Nullable UUID playerUuid) {
        boolean updated = false;
        for (final TownInvite invite : this.invites) {
            if (invite.isActive() && Objects.equals(invite.getInvitedPlayerUuid(), playerUuid)) {
                invite.expire();
                updated = true;
            }
        }
        return updated;
    }

    /** Returns a bank balance. */
    public double getBankAmount(final @NotNull String currencyId) {
        return this.bankBalances.getOrDefault(normalizeCurrencyId(currencyId), 0.0D);
    }

    /** Deposits to a bank balance. */
    public double depositBank(final @NotNull String currencyId, final double amount) {
        if (amount <= 0.0D) {
            return this.getBankAmount(currencyId);
        }
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        final double newAmount = this.getBankAmount(normalizedCurrencyId) + amount;
        this.bankBalances.put(normalizedCurrencyId, newAmount);
        return newAmount;
    }

    /** Withdraws from a bank balance. */
    public boolean withdrawBank(final @NotNull String currencyId, final double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        final double currentAmount = this.getBankAmount(normalizedCurrencyId);
        if (currentAmount + 1.0E-6D < amount) {
            return false;
        }
        final double newAmount = Math.max(0.0D, currentAmount - amount);
        if (newAmount <= 1.0E-6D) {
            this.bankBalances.remove(normalizedCurrencyId);
        } else {
            this.bankBalances.put(normalizedCurrencyId, newAmount);
        }
        return true;
    }

    /** Returns the number of tracked bank currencies. */
    public int getBankCurrencyCount() {
        return this.bankBalances.size();
    }

    /** Returns the legacy Vault bank amount. */
    public double getBank() {
        return this.getBankAmount("vault");
    }

    /** Deposits Vault currency. */
    public double deposit(final double amount) {
        return this.depositBank("vault", amount);
    }

    /** Withdraws Vault currency. */
    public boolean withdraw(final double amount) {
        return this.withdrawBank("vault", amount);
    }

    /** Syncs cached permissions from the player's current role. */
    public void syncPlayerPermissionsFromRole(final @NotNull RDTPlayer player) {
        final TownRole role = this.findRoleById(player.getTownRoleId());
        if (role == null) {
            player.replaceTownPermissions(TownPermissions.defaultPermissionKeysForRole(MEMBER_ROLE_ID));
        } else {
            player.syncTownPermissionsFromRole(role);
        }
    }

    /** Returns stored level-item progress. */
    public @NotNull Map<String, ItemStack> getLevelItemProgress() {
        return new LinkedHashMap<>(this.levelItemProgress);
    }

    /** Returns stored level-item progress for a key. */
    public @Nullable ItemStack getLevelItemProgress(final @NotNull String progressKey) {
        return this.levelItemProgress.get(normalizeProgressKey(progressKey));
    }

    /** Replaces stored level-item progress for a key. */
    public void setLevelItemProgress(
        final @NotNull String progressKey,
        final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (itemStack == null || itemStack.isEmpty()) {
            this.levelItemProgress.remove(normalizedProgressKey);
            return;
        }
        this.levelItemProgress.put(normalizedProgressKey, itemStack.clone());
    }

    /** Returns stored level-currency progress for a key. */
    public double getLevelCurrencyProgress(final @NotNull String progressKey) {
        return this.levelCurrencyProgress.getOrDefault(normalizeProgressKey(progressKey), 0.0D);
    }

    /** Returns stored level-currency progress. */
    public @NotNull Map<String, Double> getLevelCurrencyProgress() {
        return new LinkedHashMap<>(this.levelCurrencyProgress);
    }

    /** Replaces stored level-currency progress for a key. */
    public void setLevelCurrencyProgress(final @NotNull String progressKey, final double amount) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (amount <= 0.0D) {
            this.levelCurrencyProgress.remove(normalizedProgressKey);
            return;
        }
        this.levelCurrencyProgress.put(normalizedProgressKey, amount);
    }

    /** Clears level progress entries using a stable prefix. */
    public void clearLevelRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String normalizedPrefix = normalizeProgressKey(progressKeyPrefix);
        this.levelItemProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
        this.levelCurrencyProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
    }

    /** Returns the nexus location. */
    public @Nullable Location getNexusLocation() {
        return this.nexusLocation == null ? null : this.nexusLocation.clone();
    }

    /** Returns the nexus location. */
    public @Nullable Location getNexus_location() {
        return this.getNexusLocation();
    }

    /** Replaces the nexus location. */
    public void setNexusLocation(final @Nullable Location nexusLocation) {
        this.nexusLocation = nexusLocation == null ? null : nexusLocation.clone();
    }

    /** Returns the town spawn location. */
    public @Nullable Location getTownSpawnLocation() {
        return this.townSpawnLocation == null ? null : this.townSpawnLocation.clone();
    }

    /** Replaces the town spawn location. */
    public void setTownSpawnLocation(final @Nullable Location townSpawnLocation) {
        this.townSpawnLocation = townSpawnLocation == null ? null : townSpawnLocation.clone();
    }

    /** Returns the nexus server identifier. */
    public @Nullable String getNexusServerId() {
        return this.nexusServerId;
    }

    /** Replaces the nexus server identifier. */
    public void setNexusServerId(final @Nullable String nexusServerId) {
        this.nexusServerId = normalizeServerId(nexusServerId);
    }

    /** Returns the town-spawn server identifier. */
    public @Nullable String getTownSpawnServerId() {
        return this.townSpawnServerId;
    }

    /** Replaces the town-spawn server identifier. */
    public void setTownSpawnServerId(final @Nullable String townSpawnServerId) {
        this.townSpawnServerId = normalizeServerId(townSpawnServerId);
    }

    /** Returns the nexus network location. */
    public @Nullable NetworkLocation getNexusNetworkLocation(final @Nullable String fallbackServerId) {
        return toNetworkLocation(this.nexusLocation, this.resolveNexusServerId(fallbackServerId));
    }

    /** Returns the town-spawn network location. */
    public @Nullable NetworkLocation getTownSpawnNetworkLocation(final @Nullable String fallbackServerId) {
        return toNetworkLocation(this.townSpawnLocation, this.resolveTownSpawnServerId(fallbackServerId));
    }

    /** Replaces the nexus location from a network payload. */
    public void setNexusNetworkLocation(final @Nullable NetworkLocation networkLocation) {
        this.nexusServerId = networkLocation == null ? null : networkLocation.serverId();
        this.nexusLocation = toBukkitLocation(networkLocation);
    }

    /** Replaces the town spawn location from a network payload. */
    public void setTownSpawnNetworkLocation(final @Nullable NetworkLocation networkLocation) {
        this.townSpawnServerId = networkLocation == null ? null : networkLocation.serverId();
        this.townSpawnLocation = toBukkitLocation(networkLocation);
    }

    /** Backfills missing server ownership fields. */
    public boolean ensureAuthoritativeServerOwnership(final @Nullable String fallbackServerId) {
        boolean changed = false;
        final String normalizedFallback = normalizeServerId(fallbackServerId);
        if (this.nexusLocation != null && this.nexusServerId == null && normalizedFallback != null) {
            this.nexusServerId = normalizedFallback;
            changed = true;
        }
        if (this.townSpawnLocation != null && this.townSpawnServerId == null && normalizedFallback != null) {
            this.townSpawnServerId = normalizedFallback;
            changed = true;
        }
        return changed;
    }

    /** Resolves the nexus server identifier. */
    public @Nullable String resolveNexusServerId(final @Nullable String fallbackServerId) {
        return this.nexusServerId == null ? normalizeServerId(fallbackServerId) : this.nexusServerId;
    }

    /** Resolves the town-spawn server identifier. */
    public @Nullable String resolveTownSpawnServerId(final @Nullable String fallbackServerId) {
        return this.townSpawnServerId == null ? normalizeServerId(fallbackServerId) : this.townSpawnServerId;
    }

    /** Returns configured protection role identifiers. */
    public @NotNull Map<String, String> getProtectionRoleIds() {
        return new LinkedHashMap<>(this.protections);
    }

    /**
     * Returns the explicitly stored role for a protection.
     *
     * @param protection protection to resolve
     * @return stored role identifier, or {@code null} when the protection inherits a legacy parent
     *     or its enum default
     */
    public @Nullable String getConfiguredProtectionRoleId(final @Nullable TownProtections protection) {
        if (protection == null) {
            return null;
        }
        final String configuredRoleId = this.protections.get(protection.getProtectionKey());
        return configuredRoleId == null ? null : protection.normalizeConfiguredRoleId(configuredRoleId);
    }

    /** Returns the required role for a protection. */
    public @NotNull String getProtectionRoleId(final @NotNull TownProtections protection) {
        return this.resolveProtectionRoleId(protection, EnumSet.noneOf(TownProtections.class));
    }

    /** Replaces the required role for a protection. */
    public void setProtectionRoleId(
        final @NotNull TownProtections protection,
        final @Nullable String roleId
    ) {
        Objects.requireNonNull(protection, "protection");
        this.protections.put(protection.getProtectionKey(), protection.normalizeConfiguredRoleId(roleId));
    }

    /** Returns whether the town currently has a placed nexus. */
    public boolean hasNexusPlaced() {
        return this.nexusLocation != null;
    }

    /** Returns whether the town has any security chunk. */
    public boolean hasSecurityChunk() {
        return this.chunks.stream().anyMatch(chunk -> chunk.getChunkType() == ChunkType.SECURITY);
    }

    /** Returns whether remote bank access is unlocked. */
    public boolean supportsRemoteBankAccess() {
        return this.chunks.stream()
            .anyMatch(chunk -> chunk.getChunkType() == ChunkType.BANK && chunk.getChunkLevel() >= 2);
    }

    /** Returns shared bank storage contents. */
    public @NotNull Map<String, ItemStack> getSharedBankStorage() {
        return new LinkedHashMap<>(this.sharedBankStorage);
    }

    /** Replaces shared bank storage contents. */
    public void setSharedBankStorage(final @NotNull Map<String, ItemStack> sharedBankStorage) {
        this.sharedBankStorage = new LinkedHashMap<>(Objects.requireNonNull(sharedBankStorage, "sharedBankStorage"));
    }

    @Override
    public @NotNull String toString() {
        return "RTown{" + this.townUuid + "," + this.townName + "}";
    }

    private @NotNull String resolveProtectionRoleId(
        final @NotNull TownProtections protection,
        final @NotNull EnumSet<TownProtections> visited
    ) {
        if (!visited.add(protection)) {
            return protection.normalizeConfiguredRoleId(protection.getDefaultRoleId());
        }

        final String configuredRoleId = this.protections.get(protection.getProtectionKey());
        if (configuredRoleId != null) {
            return protection.normalizeConfiguredRoleId(configuredRoleId);
        }

        final TownProtections fallbackProtection = protection.getFallbackProtection();
        return fallbackProtection == null
            ? protection.normalizeConfiguredRoleId(protection.getDefaultRoleId())
            : this.resolveProtectionRoleId(fallbackProtection, visited);
    }

    private void ensureDefaultRoles() {
        this.addDefaultRoleIfMissing(RESTRICTED_ROLE_ID, "Restricted", Set.of(), 0);
        this.addDefaultRoleIfMissing(PUBLIC_ROLE_ID, "Public", TownPermissions.defaultPermissionKeysForRole(PUBLIC_ROLE_ID), 10);
        this.addDefaultRoleIfMissing(MEMBER_ROLE_ID, "Member", TownPermissions.defaultPermissionKeysForRole(MEMBER_ROLE_ID), 20);
        this.addDefaultRoleIfMissing(MAYOR_ROLE_ID, "Mayor", TownPermissions.defaultPermissionKeysForRole(MAYOR_ROLE_ID), 100);
    }

    private void addDefaultRoleIfMissing(
        final @NotNull String roleId,
        final @NotNull String roleName,
        final @NotNull Set<String> permissions,
        final int rolePriority
    ) {
        if (this.roles.stream().anyMatch(role -> Objects.equals(role.getRoleId(), roleId))) {
            return;
        }
        this.roles.add(new TownRole(this, roleId, roleName, permissions, rolePriority, true));
    }

    private @NotNull String normalizeCurrencyId(final @NotNull String currencyId) {
        final String normalized = Objects.requireNonNull(currencyId, "currencyId").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("currencyId cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalized = Objects.requireNonNull(progressKey, "progressKey").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeTownName(final @NotNull String townName) {
        final String normalized = Objects.requireNonNull(townName, "townName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("townName cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeTownColorHex(final @Nullable String townColorHex) {
        if (townColorHex == null || townColorHex.isBlank()) {
            return DEFAULT_TOWN_COLOR_HEX;
        }
        final String normalized = townColorHex.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("#") ? normalized : "#" + normalized;
    }

    private static @Nullable String normalizeServerId(final @Nullable String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return null;
        }
        return serverId.trim();
    }

    private static @Nullable NetworkLocation toNetworkLocation(
        final @Nullable Location location,
        final @Nullable String serverId
    ) {
        if (location == null || location.getWorld() == null || serverId == null) {
            return null;
        }
        return new NetworkLocation(
            serverId,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    private static @Nullable Location toBukkitLocation(final @Nullable NetworkLocation networkLocation) {
        if (networkLocation == null) {
            return null;
        }
        final World world = Bukkit.getWorld(networkLocation.worldName());
        if (world == null) {
            return null;
        }
        return new Location(
            world,
            networkLocation.x(),
            networkLocation.y(),
            networkLocation.z(),
            networkLocation.yaw(),
            networkLocation.pitch()
        );
    }
}
