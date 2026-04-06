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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.database.entity.TownInvite;
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownColorUtil;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardService;
import com.raindropcentral.rplatform.reward.config.RewardFactory;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central synchronous town-domain service for GUI actions, item placement validation, and protection
 * checks.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownRuntimeService {

    private static final Logger LOGGER = Logger.getLogger(TownRuntimeService.class.getName());

    private final RDT plugin;

    /**
     * Creates the town runtime service.
     *
     * @param plugin active plugin runtime
     */
    public TownRuntimeService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns the stored player row for the supplied UUID.
     *
     * @param playerUuid player UUID
     * @return matching player row, or {@code null} when none exists
     */
    public @Nullable RDTPlayer getPlayerData(final @NotNull UUID playerUuid) {
        return this.plugin.getPlayerRepository() == null ? null : this.plugin.getPlayerRepository().findByPlayer(playerUuid);
    }

    /**
     * Returns an existing player row or creates a detached default one when none exists yet.
     *
     * @param playerUuid player UUID
     * @return existing or new player row
     */
    public @NotNull RDTPlayer getOrCreatePlayerData(final @NotNull UUID playerUuid) {
        final RDTPlayer existing = this.getPlayerData(playerUuid);
        return existing == null ? new RDTPlayer(playerUuid) : existing;
    }

    /**
     * Returns a town by UUID.
     *
     * @param townUuid town UUID
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown getTown(final @NotNull UUID townUuid) {
        return this.plugin.getTownRepository() == null ? null : this.plugin.getTownRepository().findByTownUUID(townUuid);
    }

    /**
     * Returns the town currently associated with a player.
     *
     * @param playerUuid player UUID
     * @return matching town, or {@code null} when the player is not in a town
     */
    public @Nullable RTown getTownFor(final @NotNull UUID playerUuid) {
        final RDTPlayer playerData = this.getPlayerData(playerUuid);
        if (playerData == null || playerData.getTownUUID() == null) {
            return null;
        }
        return this.getTown(playerData.getTownUUID());
    }

    static int toChunkCoordinate(final int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, 16);
    }

    /**
     * Returns the claimed town chunk that contains the supplied location.
     *
     * @param location location to inspect
     * @return matching town chunk, or {@code null} when unclaimed
     */
    public @Nullable RTownChunk getChunkAt(final @Nullable Location location) {
        if (location == null || location.getWorld() == null || this.plugin.getTownChunkRepository() == null) {
            return null;
        }
        return this.getChunk(
            location.getWorld().getName(),
            toChunkCoordinate(location.getBlockX()),
            toChunkCoordinate(location.getBlockZ())
        );
    }

    /**
     * Returns the town that owns the supplied location.
     *
     * @param location location to inspect
     * @return owning town, or {@code null} when unclaimed
     */
    public @Nullable RTown getTownAt(final @Nullable Location location) {
        final RTownChunk chunk = this.getChunkAt(location);
        return chunk == null ? null : chunk.getTown();
    }

    /**
     * Resolves one claimed chunk owned by a specific town.
     *
     * @param townUuid owning town UUID
     * @param worldName claimed world name
     * @param chunkX claimed chunk X
     * @param chunkZ claimed chunk Z
     * @return matching claimed chunk, or {@code null} when none exists
     */
    public @Nullable RTownChunk getTownChunk(
        final @NotNull UUID townUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        final RTown town = this.getTown(townUuid);
        return town == null ? null : town.findChunk(worldName, chunkX, chunkZ);
    }

    /**
     * Resolves a claimed chunk by world name and chunk coordinates.
     *
     * @param worldName claimed world name
     * @param chunkX claimed chunk X
     * @param chunkZ claimed chunk Z
     * @return matching claimed chunk, or {@code null} when none exists
     */
    public @Nullable RTownChunk getChunk(
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        return this.plugin.getTownChunkRepository() == null
            ? null
            : this.plugin.getTownChunkRepository().findByChunk(worldName, chunkX, chunkZ);
    }

    /**
     * Returns all persisted towns sorted by name.
     *
     * @return sorted town list
     */
    public @NotNull List<RTown> getTowns() {
        if (this.plugin.getTownRepository() == null) {
            return List.of();
        }
        final List<RTown> towns = new ArrayList<>(this.plugin.getTownRepository().findAll());
        towns.sort(Comparator.comparing(RTown::getTownName, String.CASE_INSENSITIVE_ORDER));
        return towns;
    }

    /**
     * Returns active invites for the supplied player.
     *
     * @param playerUuid invited player UUID
     * @return active invites
     */
    public @NotNull List<TownInvite> getActiveInvites(final @NotNull UUID playerUuid) {
        if (this.plugin.getTownInviteRepository() == null) {
            return List.of();
        }
        return this.plugin.getTownInviteRepository().findActiveInvites(playerUuid);
    }

    /**
     * Creates an invite for a player to join a town.
     *
     * @param town town issuing the invite
     * @param invitedPlayerUuid invited player UUID
     * @param inviterUuid inviter UUID
     * @return {@code true} when the invite was created
     */
    public boolean invitePlayer(
        final @NotNull RTown town,
        final @NotNull UUID invitedPlayerUuid,
        final @NotNull UUID inviterUuid
    ) {
        if (this.getTownFor(invitedPlayerUuid) != null || town.isPlayerInvited(invitedPlayerUuid)) {
            return false;
        }

        final TownInvite invite = new TownInvite(town, invitedPlayerUuid, inviterUuid);
        town.getInvites().forEach(existingInvite -> {
            if (existingInvite.isActive() && Objects.equals(existingInvite.getInvitedPlayerUuid(), invitedPlayerUuid)) {
                existingInvite.expire();
            }
        });
        if (this.plugin.getTownInviteRepository() != null) {
            this.plugin.getTownInviteRepository().create(invite);
        }
        return true;
    }

    /**
     * Accepts an invite and adds the player to the town.
     *
     * @param player player accepting the invite
     * @param invite invite to accept
     * @return {@code true} when the invite was accepted
     */
    public boolean acceptInvite(final @NotNull Player player, final @NotNull TownInvite invite) {
        if (!invite.isActive() || this.getTownFor(player.getUniqueId()) != null) {
            return false;
        }

        final RTown town = invite.getTown();
        final RDTPlayer playerData = this.getOrCreatePlayerData(player.getUniqueId());
        playerData.setTownUUID(town.getTownUUID());
        playerData.setTownRoleId(RTown.MEMBER_ROLE_ID);
        town.addMember(playerData);
        invite.accept();
        this.plugin.getTownRepository().update(town);
        if (this.plugin.getTownInviteRepository() != null) {
            this.plugin.getTownInviteRepository().update(invite);
        }
        return true;
    }

    /**
     * Declines an invite.
     *
     * @param invite invite to decline
     * @return {@code true} when the invite was updated
     */
    public boolean declineInvite(final @NotNull TownInvite invite) {
        if (!invite.isActive()) {
            return false;
        }
        invite.decline();
        if (this.plugin.getTownInviteRepository() != null) {
            this.plugin.getTownInviteRepository().update(invite);
        }
        return true;
    }

    /**
     * Finalizes a new town when the creator places the bound nexus item.
     *
     * @param player creator placing the nexus
     * @param nexusLocation placement location
     * @param townUuid pre-generated town UUID from the item
     * @param townName chosen town name
     * @param townColor raw or canonical town color
     * @return created town, or {@code null} when validation failed
     */
    public @Nullable RTown finalizeTownCreation(
        final @NotNull Player player,
        final @NotNull Location nexusLocation,
        final @NotNull UUID townUuid,
        final @NotNull String townName,
        final @NotNull String townColor
    ) {
        if (nexusLocation.getWorld() == null
            || this.getTownFor(player.getUniqueId()) != null
            || this.getTownAt(nexusLocation) != null
            || this.plugin.getTownRepository() == null) {
            return null;
        }

        final String normalizedName = townName.trim();
        if (normalizedName.isEmpty() || this.plugin.getTownRepository().findByTName(normalizedName) != null) {
            return null;
        }

        final RTown town = new RTown(
            townUuid,
            player.getUniqueId(),
            normalizedName,
            nexusLocation,
            nexusLocation.clone().add(0.5D, 1.0D, 0.5D)
        );
        town.setTownColorHex(TownColorUtil.parseTownColor(townColor));
        final RDTPlayer mayor = this.getOrCreatePlayerData(player.getUniqueId());
        mayor.setTownUUID(townUuid);
        mayor.setTownRoleId(RTown.MAYOR_ROLE_ID);
        town.addMember(mayor);

        final RTownChunk nexusChunk = new RTownChunk(
            town,
            nexusLocation.getWorld().getName(),
            nexusLocation.getChunk().getX(),
            nexusLocation.getChunk().getZ(),
            ChunkType.NEXUS
        );
        nexusChunk.setChunkBlockLocation(nexusLocation);
        town.addChunk(nexusChunk);

        this.plugin.getTownRepository().create(town);
        if (this.plugin.getNexusAccessService() != null) {
            this.plugin.getNexusAccessService().openSession(player, town);
        }
        return town;
    }

    /**
     * Finalizes a GUI-issued chunk claim when the bound chunk block is placed in its target chunk.
     *
     * @param player placer
     * @param blockLocation physical marker block location
     * @param townUuid owning town UUID
     * @param targetWorldName required target world
     * @param targetChunkX required target chunk X
     * @param targetChunkZ required target chunk Z
     * @return created town chunk, or {@code null} when validation failed
     */
    public @Nullable RTownChunk claimChunk(
        final @NotNull Player player,
        final @NotNull Location blockLocation,
        final @NotNull UUID townUuid,
        final @NotNull String targetWorldName,
        final int targetChunkX,
        final int targetChunkZ
    ) {
        if (blockLocation.getWorld() == null) {
            return null;
        }

        final RTown town = this.getTown(townUuid);
        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        if (town == null
            || playerData == null
            || !Objects.equals(playerData.getTownUUID(), townUuid)
            || !playerData.hasTownPermission(TownPermissions.CLAIM_CHUNK)
            || !Objects.equals(blockLocation.getWorld().getName(), targetWorldName)
            || blockLocation.getChunk().getX() != targetChunkX
            || blockLocation.getChunk().getZ() != targetChunkZ
            || town.getChunks().size() >= this.plugin.getDefaultConfig().getGlobalMaxChunkLimit()
            || this.getTownAt(blockLocation) != null
            || !this.isChunkClaimable(town, targetWorldName, targetChunkX, targetChunkZ)) {
            return null;
        }

        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation != null) {
            final int deltaY = blockLocation.getBlockY() - nexusLocation.getBlockY();
            if (deltaY < this.plugin.getDefaultConfig().getChunkBlockMinY()
                || deltaY > this.plugin.getDefaultConfig().getChunkBlockMaxY()) {
                return null;
            }
        }

        final RTownChunk chunk = new RTownChunk(town, targetWorldName, targetChunkX, targetChunkZ, ChunkType.DEFAULT);
        chunk.setChunkBlockLocation(blockLocation);
        town.addChunk(chunk);
        this.plugin.getTownRepository().update(town);
        return chunk;
    }

    /**
     * Returns whether the supplied chunk is adjacent to an existing town claim and therefore
     * claimable.
     *
     * @param town town to inspect
     * @param worldName target world
     * @param chunkX target chunk X
     * @param chunkZ target chunk Z
     * @return {@code true} when the target chunk is claimable
     */
    public boolean isChunkClaimable(
        final @NotNull RTown town,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        if (town.getChunks().size() >= this.plugin.getDefaultConfig().getGlobalMaxChunkLimit()) {
            return false;
        }
        if (town.getChunks().isEmpty()) {
            return true;
        }

        for (final RTownChunk claimedChunk : town.getChunks()) {
            if (!claimedChunk.getWorldName().equalsIgnoreCase(worldName)) {
                continue;
            }
            if (claimedChunk.getX() == chunkX && claimedChunk.getZ() == chunkZ) {
                return false;
            }
            final int deltaX = Math.abs(claimedChunk.getX() - chunkX);
            final int deltaZ = Math.abs(claimedChunk.getZ() - chunkZ);
            if (this.isAdjacentClaim(deltaX, deltaZ)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdjacentClaim(final int deltaX, final int deltaZ) {
        if (deltaX > 1 || deltaZ > 1 || (deltaX == 0 && deltaZ == 0)) {
            return false;
        }

        if (this.plugin.getDefaultConfig().isCornerClaimAdjacencyExcluded()) {
            return deltaX + deltaZ == 1;
        }
        return deltaX <= 1 && deltaZ <= 1;
    }

    /**
     * Renames a town after validating uniqueness.
     *
     * @param town town to rename
     * @param newTownName replacement town name
     * @return {@code true} when the town name was updated
     */
    public boolean renameTown(final @NotNull RTown town, final @NotNull String newTownName) {
        final String normalized = newTownName.trim();
        if (normalized.isEmpty() || this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTown existing = this.plugin.getTownRepository().findByTName(normalized);
        if (existing != null && !Objects.equals(existing.getTownUUID(), town.getTownUUID())) {
            return false;
        }

        town.setTownName(normalized);
        this.plugin.getTownRepository().update(town);
        return true;
    }

    /**
     * Returns the configured archetype change cooldown in milliseconds.
     *
     * @return cooldown in milliseconds
     */
    public long getTownArchetypeChangeCooldownMillis() {
        return Math.max(0L, this.plugin.getDefaultConfig().getTownArchetypeChangeCooldownSeconds()) * 1000L;
    }

    /**
     * Returns the remaining archetype change cooldown for a town.
     *
     * @param town town to inspect
     * @return remaining cooldown in milliseconds
     */
    public long getRemainingTownArchetypeChangeCooldownMillis(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        return this.computeRemainingTownArchetypeChangeCooldownMillis(liveTown == null ? town : liveTown);
    }

    /**
     * Updates a town archetype.
     *
     * @param town town to update
     * @param archetype replacement archetype
     * @return {@code true} when the town was updated
     */
    public boolean setTownArchetype(final @NotNull RTown town, final @Nullable TownArchetype archetype) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return false;
        }

        if (this.computeRemainingTownArchetypeChangeCooldownMillis(liveTown) > 0L) {
            return false;
        }

        liveTown.setArchetype(archetype);
        liveTown.setLastArchetypeChangeAt(System.currentTimeMillis());
        this.plugin.getTownRepository().update(liveTown);
        town.setArchetype(liveTown.getArchetype());
        town.setLastArchetypeChangeAt(liveTown.getLastArchetypeChangeAt());
        return true;
    }

    /**
     * Advances the town to the next configured level when one exists.
     *
     * @param town town to level up
     * @return {@code true} when the level was advanced
     */
    public boolean upgradeTown(final @NotNull RTown town) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final Integer nextLevel = this.plugin.getDefaultConfig().getNextTownLevel(town.getTownLevel());
        if (nextLevel == null) {
            return false;
        }
        town.setTownLevel(nextLevel);
        this.plugin.getTownRepository().update(town);
        return true;
    }

    /**
     * Advances a town to the next configured level after validating config-driven requirements.
     *
     * @param player player performing the upgrade
     * @param town town being upgraded
     * @return {@code true} when the town level advanced
     */
    public boolean upgradeTown(final @NotNull Player player, final @NotNull RTown town) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final Integer nextLevel = this.plugin.getDefaultConfig().getNextTownLevel(town.getTownLevel());
        if (nextLevel == null) {
            return false;
        }

        final ConfigSection.TownLevelSection nextLevelSection = this.plugin.getDefaultConfig().getTownLevelSection(nextLevel);
        if (nextLevelSection == null) {
            return false;
        }

        final List<AbstractRequirement> requirements = this.buildRequirements(nextLevelSection);
        final RequirementService requirementService = RequirementService.getInstance();
        if (!requirementService.areAllMet(player, requirements)) {
            return false;
        }

        town.setTownLevel(nextLevel);
        this.plugin.getTownRepository().update(town);
        requirementService.consumeAll(player, requirements);

        final List<AbstractReward> rewards = this.buildRewards(nextLevelSection);
        if (!rewards.isEmpty()) {
            try {
                RewardService.getInstance().grantAll(player, rewards).join();
            } catch (final Exception exception) {
                LOGGER.log(
                    Level.WARNING,
                    "Failed to grant one or more town-level rewards for town " + town.getTownName() + " at level " + nextLevel,
                    exception
                );
            }
        }
        return true;
    }

    /**
     * Changes a chunk type and resets chunk-local progression when the type changes.
     *
     * @param townChunk chunk to update
     * @param chunkType replacement chunk type
     * @return {@code true} when the chunk type changed
     */
    public boolean setChunkType(final @NotNull RTownChunk townChunk, final @NotNull ChunkType chunkType) {
        if (townChunk.getChunkType() == chunkType || chunkType == ChunkType.NEXUS || this.plugin.getTownRepository() == null) {
            return false;
        }
        townChunk.setChunkType(chunkType);
        this.plugin.getTownRepository().update(townChunk.getTown());
        return true;
    }

    /**
     * Unclaims a non-nexus chunk and removes it from the owning town.
     *
     * @param townChunk chunk to remove
     * @return {@code true} when the chunk was removed
     */
    public boolean unclaimChunk(final @NotNull RTownChunk townChunk) {
        if (townChunk.getChunkType() == ChunkType.NEXUS || this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTown town = townChunk.getTown();
        final boolean removed = town.removeChunk(townChunk);
        if (removed) {
            this.plugin.getTownRepository().update(town);
        }
        return removed;
    }

    /**
     * Returns whether a player has a specific town management permission.
     *
     * @param player player to inspect
     * @param permission management permission to resolve
     * @return {@code true} when the player holds the permission
     */
    public boolean hasTownPermission(final @NotNull Player player, final @NotNull TownPermissions permission) {
        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        return playerData != null && playerData.hasTownPermission(permission);
    }

    /**
     * Returns whether protection editing is unlocked for the supplied town.
     *
     * @param town town to inspect
     * @return {@code true} when the town has at least one security chunk
     */
    public boolean canEditProtections(final @NotNull RTown town) {
        return town.hasSecurityChunk();
    }

    /**
     * Returns the ordered role IDs available when cycling protection thresholds.
     *
     * @param town town whose roles should be listed
     * @return sorted role ID list
     */
    public @NotNull List<String> getProtectionRoleOrder(final @NotNull RTown town) {
        final List<TownRole> roles = new ArrayList<>(town.getRoles());
        roles.sort(Comparator.comparingInt(TownRole::getRolePriority));
        final List<String> orderedIds = new ArrayList<>();
        for (final TownRole role : roles) {
            orderedIds.add(role.getRoleId());
        }
        return orderedIds;
    }

    /**
     * Updates one town-global protection threshold.
     *
     * @param town town to update
     * @param protection protection to update
     * @param requiredRoleId replacement minimum role
     * @return {@code true} when the live town snapshot was updated
     */
    public boolean setTownProtectionRoleId(
        final @NotNull RTown town,
        final @NotNull TownProtections protection,
        final @Nullable String requiredRoleId
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return false;
        }
        liveTown.setProtectionRoleId(protection, requiredRoleId);
        this.plugin.getTownRepository().update(liveTown);
        this.reconcileProtectionEffects(liveTown, null, protection);
        return true;
    }

    /**
     * Updates one chunk-specific protection threshold.
     *
     * @param townChunk chunk to update
     * @param protection protection to update
     * @param requiredRoleId replacement override, or {@code null} to inherit
     * @return {@code true} when the live chunk snapshot was updated
     */
    public boolean setChunkProtectionRoleId(
        final @NotNull RTownChunk townChunk,
        final @NotNull TownProtections protection,
        final @Nullable String requiredRoleId
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }
        liveTownChunk.setProtectionRoleId(protection, requiredRoleId);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        this.reconcileProtectionEffects(liveTownChunk.getTown(), liveTownChunk, protection);
        return true;
    }

    /**
     * Reconciles loaded hostile and passive mobs in one claimed chunk against the active protection
     * thresholds.
     *
     * @param townChunk claimed chunk whose loaded entities should be checked
     */
    public void reconcileLoadedProtectionEntities(final @NotNull RTownChunk townChunk) {
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return;
        }
        this.reconcileLoadedChunkProtectionEntities(
            liveTownChunk.getTown(),
            liveTownChunk,
            TownProtections.TOWN_HOSTILE_ENTITIES
        );
        this.reconcileLoadedChunkProtectionEntities(
            liveTownChunk.getTown(),
            liveTownChunk,
            TownProtections.TOWN_PASSIVE_ENTITIES
        );
    }

    private @Nullable RTown resolveLiveTown(final @NotNull RTown town) {
        return this.getTown(town.getTownUUID());
    }

    private @Nullable RTownChunk resolveLiveTownChunk(final @NotNull RTownChunk townChunk) {
        final RTown liveTown = this.resolveLiveTown(townChunk.getTown());
        return liveTown == null ? null : liveTown.findChunk(townChunk.getWorldName(), townChunk.getX(), townChunk.getZ());
    }

    private void reconcileProtectionEffects(
        final @NotNull RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        if (!this.requiresLoadedEntityReconciliation(protection)) {
            return;
        }
        if (townChunk != null) {
            this.reconcileLoadedChunkProtectionEntities(town, townChunk, protection);
            return;
        }
        for (final RTownChunk claimedChunk : town.getChunks()) {
            this.reconcileLoadedChunkProtectionEntities(town, claimedChunk, protection);
        }
    }

    private void reconcileLoadedChunkProtectionEntities(
        final @NotNull RTown town,
        final @NotNull RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        if (Objects.equals(this.resolveRequiredRoleId(town, townChunk, protection), RTown.PUBLIC_ROLE_ID)) {
            return;
        }

        final World world = this.plugin.getServer().getWorld(townChunk.getWorldName());
        if (world == null || !world.isChunkLoaded(townChunk.getX(), townChunk.getZ())) {
            return;
        }

        final Chunk loadedChunk = world.getChunkAt(townChunk.getX(), townChunk.getZ());
        for (final Entity entity : loadedChunk.getEntities()) {
            if (this.shouldRemoveEntityForProtection(protection, entity)) {
                entity.remove();
            }
        }
    }

    private boolean requiresLoadedEntityReconciliation(final @NotNull TownProtections protection) {
        return protection == TownProtections.TOWN_HOSTILE_ENTITIES
            || protection == TownProtections.TOWN_PASSIVE_ENTITIES;
    }

    private boolean shouldRemoveEntityForProtection(
        final @NotNull TownProtections protection,
        final @NotNull Entity entity
    ) {
        return switch (protection) {
            case TOWN_HOSTILE_ENTITIES -> entity instanceof Monster;
            case TOWN_PASSIVE_ENTITIES -> entity instanceof Animals;
            default -> false;
        };
    }

    private long computeRemainingTownArchetypeChangeCooldownMillis(final @NotNull RTown town) {
        final long cooldownMillis = this.getTownArchetypeChangeCooldownMillis();
        if (cooldownMillis <= 0L) {
            return 0L;
        }

        final long lastChangeAt = town.getLastArchetypeChangeAt();
        if (lastChangeAt <= 0L) {
            return 0L;
        }

        final long elapsedMillis = Math.max(0L, System.currentTimeMillis() - lastChangeAt);
        return Math.max(0L, cooldownMillis - elapsedMillis);
    }

    /**
     * Returns the chunk marker at the supplied exact location when one exists.
     *
     * @param location block location to inspect
     * @return matching town chunk marker, or {@code null} when none exists
     */
    public @Nullable RTownChunk findChunkMarker(final @Nullable Location location) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (location == null || townChunk == null || townChunk.getChunkBlockLocation() == null) {
            return null;
        }
        return sameBlock(location, townChunk.getChunkBlockLocation()) ? townChunk : null;
    }

    /**
     * Returns whether a location matches a town nexus block.
     *
     * @param location location to inspect
     * @return owning town, or {@code null} when no nexus matches
     */
    public @Nullable RTown findNexusTown(final @Nullable Location location) {
        if (location == null || this.plugin.getTownRepository() == null) {
            return null;
        }
        return this.plugin.getTownRepository().findByNexusLocation(location);
    }

    /**
     * Returns whether a player can perform a protected action in the town at the supplied location.
     *
     * @param player acting player
     * @param location target location
     * @param protection protection being checked
     * @return {@code true} when the action is allowed
     */
    public boolean isPlayerAllowed(
        final @NotNull Player player,
        final @NotNull Location location,
        final @NotNull TownProtections protection
    ) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (townChunk == null) {
            return true;
        }

        final RTown town = townChunk.getTown();
        final String requiredRoleId = this.resolveRequiredRoleId(town, townChunk, protection);
        if (Objects.equals(requiredRoleId, RTown.RESTRICTED_ROLE_ID)) {
            return false;
        }
        if (Objects.equals(requiredRoleId, RTown.PUBLIC_ROLE_ID)) {
            return true;
        }

        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        if (playerData == null || !Objects.equals(playerData.getTownUUID(), town.getTownUUID())) {
            return false;
        }
        return this.hasRequiredRole(town, playerData.getTownRoleId(), requiredRoleId);
    }

    /**
     * Returns whether a non-player world action such as fire spread or mob spawning is allowed.
     *
     * @param location affected location
     * @param protection protection being checked
     * @return {@code true} when the world action should be allowed
     */
    public boolean isWorldActionAllowed(
        final @NotNull Location location,
        final @NotNull TownProtections protection
    ) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (townChunk == null) {
            return true;
        }
        return Objects.equals(this.resolveRequiredRoleId(townChunk.getTown(), townChunk, protection), RTown.PUBLIC_ROLE_ID);
    }

    /**
     * Returns whether the supplied location belongs to an outpost chunk.
     *
     * @param location location to inspect
     * @return {@code true} when the location is inside an outpost chunk
     */
    public boolean isOutpostChunk(final @NotNull Location location) {
        final RTownChunk townChunk = this.getChunkAt(location);
        return townChunk != null && townChunk.getChunkType() == ChunkType.OUTPOST;
    }

    private boolean hasRequiredRole(
        final @NotNull RTown town,
        final @Nullable String playerRoleId,
        final @NotNull String requiredRoleId
    ) {
        if (Objects.equals(requiredRoleId, RTown.PUBLIC_ROLE_ID)) {
            return true;
        }
        if (Objects.equals(requiredRoleId, RTown.RESTRICTED_ROLE_ID)) {
            return false;
        }
        if (playerRoleId == null) {
            return false;
        }

        final TownRole playerRole = town.findRoleById(playerRoleId);
        final TownRole requiredRole = town.findRoleById(requiredRoleId);
        final int actualPriority = playerRole == null
            ? RTown.resolveDefaultRolePriority(playerRoleId)
            : playerRole.getRolePriority();
        final int requiredPriority = requiredRole == null
            ? RTown.resolveDefaultRolePriority(requiredRoleId)
            : requiredRole.getRolePriority();
        return actualPriority >= requiredPriority;
    }

    private @NotNull String resolveRequiredRoleId(
        final @NotNull RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        if (townChunk != null && townChunk.overridesProtection(protection)) {
            final String overrideRole = townChunk.getProtectionRoleId(protection);
            if (overrideRole != null) {
                return RTown.normalizeRoleId(overrideRole);
            }
        }
        return RTown.normalizeRoleId(town.getProtectionRoleId(protection));
    }

    private static boolean sameBlock(final @NotNull Location left, final @NotNull Location right) {
        return left.getWorld() != null
            && right.getWorld() != null
            && Objects.equals(left.getWorld().getName(), right.getWorld().getName())
            && left.getBlockX() == right.getBlockX()
            && left.getBlockY() == right.getBlockY()
            && left.getBlockZ() == right.getBlockZ();
    }

    private @NotNull List<AbstractRequirement> buildRequirements(final @NotNull ConfigSection.TownLevelSection levelSection) {
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();
        final List<AbstractRequirement> requirements = new ArrayList<>();
        for (final Map<String, Object> definition : levelSection.getRequirements().values()) {
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(definition);
            requirementFactory.tryFromMap(normalizedDefinition).ifPresent(requirements::add);
        }
        return requirements;
    }

    private @NotNull List<AbstractReward> buildRewards(final @NotNull ConfigSection.TownLevelSection levelSection) {
        final RewardFactory<Map<String, Object>> rewardFactory = RewardFactory.getInstance();
        final List<AbstractReward> rewards = new ArrayList<>();
        for (final Map<String, Object> definition : levelSection.getRewards().values()) {
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(definition);
            rewardFactory.tryFromMap(normalizedDefinition).ifPresent(rewards::add);
        }
        return rewards;
    }

    private @NotNull Map<String, Object> normalizeFactoryDefinition(final @NotNull Map<String, Object> definition) {
        final Map<String, Object> normalized = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : definition.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            normalized.put(this.normalizeFactoryKey(entry.getKey()), this.normalizeFactoryValue(entry.getValue()));
        }
        return normalized;
    }

    private @Nullable Object normalizeFactoryValue(final @Nullable Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            final Map<String, Object> nestedNormalized = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                if (nestedEntry.getKey() == null) {
                    continue;
                }
                nestedNormalized.put(
                    this.normalizeFactoryKey(String.valueOf(nestedEntry.getKey())),
                    this.normalizeFactoryValue(nestedEntry.getValue())
                );
            }
            return nestedNormalized;
        }
        if (value instanceof List<?> list) {
            final List<Object> normalizedList = new ArrayList<>();
            for (final Object entry : list) {
                normalizedList.add(this.normalizeFactoryValue(entry));
            }
            return normalizedList;
        }
        return value;
    }

    private @NotNull String normalizeFactoryKey(final @NotNull String rawKey) {
        final String trimmedKey = rawKey.trim();
        final String flattenedKey = trimmedKey.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return switch (flattenedKey) {
            case "currencyid" -> "currencyId";
            case "experiencetype" -> "experienceType";
            case "requiredpermissions" -> "requiredPermissions";
            case "permissionmode" -> "permissionMode";
            case "requiredworld" -> "requiredWorld";
            case "requiredregion" -> "requiredRegion";
            case "requiredcoordinates" -> "requiredCoordinates";
            case "minimumrequired" -> "minimumRequired";
            case "allowpartialprogress" -> "allowPartialProgress";
            case "timelimitmillis" -> "timeLimitMillis";
            case "autostart" -> "autoStart";
            case "pluginid" -> "pluginId";
            case "integrationid" -> "integrationId";
            case "requiredvalues" -> "requiredValues";
            case "skillplugin" -> "skillPlugin";
            case "jobplugin" -> "jobPlugin";
            case "executeasplayer" -> "executeAsPlayer";
            case "delayticks" -> "delayTicks";
            case "continueonerror" -> "continueOnError";
            case "requiredtownplaytimeticks" -> "requiredTownPlaytimeTicks";
            case "requiredtownplaytimeseconds" -> "requiredTownPlaytimeSeconds";
            default -> trimmedKey;
        };
    }
}
