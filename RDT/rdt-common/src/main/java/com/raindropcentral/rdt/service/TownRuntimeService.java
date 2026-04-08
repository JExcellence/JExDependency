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
import com.raindropcentral.rdt.configs.LevelDefinition;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.database.entity.TownInvite;
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.items.FuelTank;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownColorUtil;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.requirement.impl.CurrencyRequirement;
import com.raindropcentral.rplatform.requirement.impl.ItemRequirement;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardService;
import com.raindropcentral.rplatform.reward.config.RewardFactory;
import com.raindropcentral.rplatform.reward.impl.CommandReward;
import com.raindropcentral.rplatform.reward.impl.CurrencyReward;
import com.raindropcentral.rplatform.reward.impl.ExperienceReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import com.raindropcentral.rplatform.reward.impl.PermissionReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
     * Returns the live town-creation progress snapshot for the supplied player.
     *
     * @param player player viewing town creation
     * @return current town-creation progress snapshot
     */
    public @NotNull TownCreationProgressSnapshot getTownCreationProgress(final @NotNull Player player) {
        final LevelContext context = this.resolveTownCreationContext(player);
        if (context == null || !context.available()) {
            return new TownCreationProgressSnapshot(false, this.getTownFor(player.getUniqueId()) != null, false, 0.0D, List.of(), List.of());
        }

        final LevelDefinition levelDefinition = context.levelDefinitions().get(context.targetLevel());
        final LevelEvaluation evaluation = this.evaluateLevel(player, context, context.targetLevel(), levelDefinition, false, false);
        final List<TownLevelRewardSnapshot> rewardSnapshots = this.buildRewardSnapshots(context, 0, context.targetLevel(), levelDefinition);
        final double progress = evaluation.requirements().isEmpty()
            ? 1.0D
            : evaluation.requirements().stream()
                .mapToDouble(TownLevelRequirementSnapshot::progress)
                .average()
                .orElse(0.0D);
        return new TownCreationProgressSnapshot(true, false, evaluation.readyToLevelUp(), progress, evaluation.requirements(), rewardSnapshots);
    }

    /**
     * Returns whether the player has completed the town-creation requirements.
     *
     * @param player player to inspect
     * @return {@code true} when the player can start naming a town
     */
    public boolean canCreateTown(final @NotNull Player player) {
        final TownCreationProgressSnapshot snapshot = this.getTownCreationProgress(player);
        return snapshot.available() && !snapshot.alreadyInTown() && snapshot.readyToCreate();
    }

    /**
     * Saves a partial town-creation currency contribution for the supplied player.
     *
     * @param player contributing player
     * @param entryKey requirement entry key to contribute toward
     * @param requestedAmount requested contribution amount
     * @return structured contribution result
     */
    public @NotNull ContributionResult contributeTownCreationCurrency(
        final @NotNull Player player,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        if (requestedAmount <= 0.0D) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, false);
        }

        final LevelContext context = this.resolveTownCreationContext(player);
        if (context == null || !context.available()) {
            return new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false);
        }

        final TownCreationProgressSnapshot snapshot = this.getTownCreationProgress(player);
        final TownLevelRequirementSnapshot requirement = snapshot.findRequirement(entryKey);
        if (requirement == null || requirement.kind() != RequirementKind.CURRENCY || !requirement.contributable()) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, snapshot.readyToCreate());
        }
        if (requirement.completed()) {
            return new ContributionResult(ContributionStatus.ALREADY_COMPLETE, 0.0D, true, snapshot.readyToCreate());
        }

        final double remainingAmount = Math.max(0.0D, requirement.requiredAmount() - requirement.currentAmount());
        final double availableAmount = Math.max(0.0D, requirement.availableAmount());
        final double contributionAmount = Math.min(requestedAmount, Math.min(remainingAmount, availableAmount));
        if (contributionAmount <= 0.0D || !this.withdrawPlayerCurrency(player, requirement.currencyId(), contributionAmount)) {
            return new ContributionResult(ContributionStatus.NOT_ENOUGH_RESOURCES, 0.0D, false, snapshot.readyToCreate());
        }

        final String progressKey = this.buildProgressKey(LevelScope.NEXUS, context.targetLevel(), requirement.entryKey());
        context.progressAccessor().setCurrencyProgress(progressKey, requirement.currentAmount() + contributionAmount);
        this.persistTownCreationContext(context);

        final TownCreationProgressSnapshot updatedSnapshot = this.getTownCreationProgress(player);
        final TownLevelRequirementSnapshot updatedRequirement = updatedSnapshot.findRequirement(entryKey);
        return new ContributionResult(
            ContributionStatus.SUCCESS,
            contributionAmount,
            updatedRequirement != null && updatedRequirement.completed(),
            updatedSnapshot.readyToCreate()
        );
    }

    /**
     * Saves a town-creation item contribution for the supplied player.
     *
     * @param player contributing player
     * @param entryKey requirement entry key to contribute toward
     * @return structured contribution result
     */
    public @NotNull ContributionResult contributeTownCreationItem(
        final @NotNull Player player,
        final @NotNull String entryKey
    ) {
        final LevelContext context = this.resolveTownCreationContext(player);
        if (context == null || !context.available()) {
            return new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false);
        }

        final TownCreationProgressSnapshot snapshot = this.getTownCreationProgress(player);
        final TownLevelRequirementSnapshot requirement = snapshot.findRequirement(entryKey);
        if (requirement == null
            || requirement.kind() != RequirementKind.ITEM
            || !requirement.contributable()
            || requirement.displayItem() == null) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, snapshot.readyToCreate());
        }
        if (requirement.completed()) {
            return new ContributionResult(ContributionStatus.ALREADY_COMPLETE, 0.0D, true, snapshot.readyToCreate());
        }

        final int remainingAmount = (int) Math.round(Math.max(0.0D, requirement.requiredAmount() - requirement.currentAmount()));
        final int removedAmount = this.removeMatchingInventoryItems(
            player,
            requirement.displayItem(),
            requirement.exactMatch(),
            remainingAmount
        );
        if (removedAmount <= 0) {
            return new ContributionResult(ContributionStatus.NOT_ENOUGH_RESOURCES, 0.0D, false, snapshot.readyToCreate());
        }

        final String progressKey = this.buildProgressKey(LevelScope.NEXUS, context.targetLevel(), requirement.entryKey());
        context.progressAccessor().setItemProgress(
            progressKey,
            this.createStoredProgressItem(requirement.displayItem(), (int) Math.round(requirement.currentAmount()) + removedAmount)
        );
        this.persistTownCreationContext(context);

        final TownCreationProgressSnapshot updatedSnapshot = this.getTownCreationProgress(player);
        final TownLevelRequirementSnapshot updatedRequirement = updatedSnapshot.findRequirement(entryKey);
        return new ContributionResult(
            ContributionStatus.SUCCESS,
            removedAmount,
            updatedRequirement != null && updatedRequirement.completed(),
            updatedSnapshot.readyToCreate()
        );
    }

    /**
     * Clears every stored town-creation progress entry for the supplied player.
     *
     * @param playerUuid player UUID whose progress should be cleared
     */
    public void clearTownCreationProgress(final @NotNull UUID playerUuid) {
        final RDTPlayer playerData = this.getPlayerData(playerUuid);
        if (playerData == null) {
            return;
        }
        playerData.clearTownCreationProgress();
        this.persistPlayerData(playerData);
    }

    /**
     * Returns a town by UUID.
     *
     * @param townUuid town UUID
     * @return matching town, or {@code null} when none exists
     */
    public @Nullable RTown getTown(final @NotNull UUID townUuid) {
        if (this.plugin.getTownRepository() == null) {
            return null;
        }
        final RTown town = this.plugin.getTownRepository().findByTownUUID(townUuid);
        if (town != null) {
            this.ensureCompositeTownState(town);
        }
        return town;
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
        towns.forEach(this::ensureCompositeTownState);
        towns.sort(Comparator.comparing(RTown::getTownName, String.CASE_INSENSITIVE_ORDER));
        return towns;
    }

    /**
     * Backfills the persisted Nexus level when needed and refreshes the cached composite town level.
     *
     * @param town live town entity to refresh
     * @return {@code true} when the town state changed and was persisted
     */
    public boolean ensureCompositeTownState(final @NotNull RTown town) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final boolean backfilled = town.backfillLegacyNexusLevelIfNeeded();
        final int previousTownLevel = town.getTownLevel();
        final int recalculatedTownLevel = town.recalculateTownLevel();
        final boolean changed = backfilled || previousTownLevel != recalculatedTownLevel;
        if (changed) {
            this.plugin.getTownRepository().update(town);
        }
        return changed;
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
     * @return created town, or {@code null} when validation failed
     */
    public @Nullable RTown finalizeTownCreation(
        final @NotNull Player player,
        final @NotNull Location nexusLocation,
        final @NotNull UUID townUuid,
        final @NotNull String townName
    ) {
        if (nexusLocation.getWorld() == null
            || this.getTownFor(player.getUniqueId()) != null
            || this.getTownAt(nexusLocation) != null
            || this.plugin.getTownRepository() == null
            || !this.canCreateTown(player)) {
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
        this.grantTownCreationRewards(player, town);
        mayor.clearTownCreationProgress();
        this.plugin.getTownRepository().update(town);
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
     * Replaces a town's stored color after validating the canonical color value.
     *
     * @param town town to update
     * @param newTownColor replacement town color
     * @return {@code true} when the town color was updated
     */
    public boolean setTownColor(final @NotNull RTown town, final @NotNull String newTownColor) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return false;
        }

        liveTown.setTownColorHex(TownColorUtil.parseTownColor(newTownColor));
        this.plugin.getTownRepository().update(liveTown);
        town.setTownColorHex(liveTown.getTownColorHex());
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
        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return false;
        }
        this.ensureCompositeTownState(liveTown);
        final Integer nextLevel = this.plugin.getNexusConfig().getNextLevel(liveTown.getNexusLevel());
        if (nextLevel == null) {
            return false;
        }
        liveTown.setNexusLevel(nextLevel);
        liveTown.clearLevelRequirementProgress(this.buildProgressKeyPrefix(LevelScope.NEXUS, nextLevel));
        this.plugin.getTownRepository().update(liveTown);
        town.setTownLevel(liveTown.getTownLevel());
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
        return this.levelUpNexus(player, town).status() == LevelUpStatus.SUCCESS;
    }

    /**
     * Returns a live Nexus progression snapshot for the supplied town.
     *
     * @param player viewing player
     * @param town town to inspect
     * @return current Nexus progression snapshot
     */
    public @NotNull LevelProgressSnapshot getNexusLevelProgress(
        final @NotNull Player player,
        final @NotNull RTown town
    ) {
        return this.getLevelProgress(player, LevelScope.NEXUS, town, null, null);
    }

    /**
     * Returns a preview snapshot for one configured Nexus level.
     *
     * @param player viewing player
     * @param town town to inspect
     * @param previewLevel configured level to preview
     * @return preview progression snapshot
     */
    public @NotNull LevelProgressSnapshot getNexusLevelProgress(
        final @NotNull Player player,
        final @NotNull RTown town,
        final int previewLevel
    ) {
        return this.getLevelProgress(player, LevelScope.NEXUS, town, null, previewLevel);
    }

    /**
     * Returns a live Security progression snapshot for the supplied chunk.
     *
     * @param player viewing player
     * @param townChunk chunk to inspect
     * @return current Security progression snapshot
     */
    public @NotNull LevelProgressSnapshot getSecurityLevelProgress(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        return this.getLevelProgress(player, LevelScope.SECURITY, null, townChunk, null);
    }

    /**
     * Returns a preview snapshot for one configured Security level.
     *
     * @param player viewing player
     * @param townChunk chunk to inspect
     * @param previewLevel configured level to preview
     * @return preview progression snapshot
     */
    public @NotNull LevelProgressSnapshot getSecurityLevelProgress(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final int previewLevel
    ) {
        return this.getLevelProgress(player, LevelScope.SECURITY, null, townChunk, previewLevel);
    }

    /**
     * Returns a live progression snapshot for the supplied chunk type.
     *
     * @param player viewing player
     * @param townChunk chunk to inspect
     * @return current chunk progression snapshot
     */
    public @NotNull LevelProgressSnapshot getChunkLevelProgress(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        final LevelScope scope = this.requireChunkLevelScope(townChunk);
        return this.getLevelProgress(player, scope, null, townChunk, null);
    }

    /**
     * Returns a preview snapshot for one configured chunk level.
     *
     * @param player viewing player
     * @param townChunk chunk to inspect
     * @param previewLevel configured level to preview
     * @return preview progression snapshot
     */
    public @NotNull LevelProgressSnapshot getChunkLevelProgress(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final int previewLevel
    ) {
        final LevelScope scope = this.requireChunkLevelScope(townChunk);
        return this.getLevelProgress(player, scope, null, townChunk, previewLevel);
    }

    /**
     * Saves a partial Nexus currency contribution against the town's current target level.
     *
     * @param player contributing player
     * @param town town receiving the contribution
     * @param entryKey requirement entry key to contribute toward
     * @param requestedAmount requested contribution amount
     * @return contribution result
     */
    public @NotNull ContributionResult contributeNexusCurrency(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        return this.contributeCurrency(player, LevelScope.NEXUS, town, null, entryKey, requestedAmount);
    }

    /**
     * Saves a partial Security currency contribution against the chunk's current target level.
     *
     * @param player contributing player
     * @param townChunk Security chunk receiving the contribution
     * @param entryKey requirement entry key to contribute toward
     * @param requestedAmount requested contribution amount
     * @return contribution result
     */
    public @NotNull ContributionResult contributeSecurityCurrency(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        return this.contributeCurrency(player, LevelScope.SECURITY, null, townChunk, entryKey, requestedAmount);
    }

    /**
     * Saves a partial currency contribution against the chunk's current target level.
     *
     * @param player contributing player
     * @param townChunk chunk receiving the contribution
     * @param entryKey requirement entry key to contribute toward
     * @param requestedAmount requested contribution amount
     * @return contribution result
     */
    public @NotNull ContributionResult contributeChunkCurrency(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        final LevelScope scope = this.requireChunkLevelScope(townChunk);
        return this.contributeCurrency(player, scope, null, townChunk, entryKey, requestedAmount);
    }

    /**
     * Saves an item contribution against the town's current target Nexus level.
     *
     * @param player contributing player
     * @param town town receiving the contribution
     * @param entryKey requirement entry key to contribute toward
     * @return contribution result
     */
    public @NotNull ContributionResult contributeNexusItem(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull String entryKey
    ) {
        return this.contributeItem(player, LevelScope.NEXUS, town, null, entryKey);
    }

    /**
     * Saves an item contribution against the chunk's current target Security level.
     *
     * @param player contributing player
     * @param townChunk Security chunk receiving the contribution
     * @param entryKey requirement entry key to contribute toward
     * @return contribution result
     */
    public @NotNull ContributionResult contributeSecurityItem(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final @NotNull String entryKey
    ) {
        return this.contributeItem(player, LevelScope.SECURITY, null, townChunk, entryKey);
    }

    /**
     * Saves an item contribution against the chunk's current target level.
     *
     * @param player contributing player
     * @param townChunk chunk receiving the contribution
     * @param entryKey requirement entry key to contribute toward
     * @return contribution result
     */
    public @NotNull ContributionResult contributeChunkItem(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final @NotNull String entryKey
    ) {
        final LevelScope scope = this.requireChunkLevelScope(townChunk);
        return this.contributeItem(player, scope, null, townChunk, entryKey);
    }

    /**
     * Finalizes the next Nexus level when every stored and live requirement is complete.
     *
     * @param player player performing the final level-up action
     * @param town town being upgraded
     * @return level-up result
     */
    public @NotNull LevelUpResult levelUpNexus(final @NotNull Player player, final @NotNull RTown town) {
        return this.levelUp(player, LevelScope.NEXUS, town, null);
    }

    /**
     * Finalizes the next Security level when every stored and live requirement is complete.
     *
     * @param player player performing the final level-up action
     * @param townChunk chunk being upgraded
     * @return level-up result
     */
    public @NotNull LevelUpResult levelUpSecurity(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        return this.levelUp(player, LevelScope.SECURITY, null, townChunk);
    }

    /**
     * Finalizes the next chunk level when every stored and live requirement is complete.
     *
     * @param player player performing the final level-up action
     * @param townChunk chunk being upgraded
     * @return level-up result
     */
    public @NotNull LevelUpResult levelUpChunk(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        final LevelScope scope = this.requireChunkLevelScope(townChunk);
        return this.levelUp(player, scope, null, townChunk);
    }

    /**
     * Changes a chunk type and optionally resets chunk-local state when the type changes.
     *
     * @param actor acting player, or {@code null} when no player should receive side effects
     * @param townChunk chunk to update
     * @param chunkType replacement chunk type
     * @return structured chunk-type transition result
     */
    public @NotNull ChunkTypeChangeResult setChunkType(
        final @Nullable Player actor,
        final @NotNull RTownChunk townChunk,
        final @NotNull ChunkType chunkType
    ) {
        if (chunkType == ChunkType.NEXUS || this.plugin.getTownRepository() == null) {
            return ChunkTypeChangeResult.failure();
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || liveTownChunk.getChunkType() == chunkType) {
            return ChunkTypeChangeResult.failure();
        }

        final ChunkType previousType = liveTownChunk.getChunkType();
        final boolean enteringSecurity = previousType != ChunkType.SECURITY && chunkType == ChunkType.SECURITY;
        final boolean leavingSecurity = previousType == ChunkType.SECURITY && chunkType != ChunkType.SECURITY;
        final FuelTankRemoval fuelTankRemoval = leavingSecurity
            ? this.removeFuelTankInternal(liveTownChunk, null, false)
            : FuelTankRemoval.none();

        liveTownChunk.setChunkType(chunkType);
        if (this.plugin.getDefaultConfig().isChunkTypeResetOnChange()) {
            liveTownChunk.resetChunkTypeState();
        } else if (leavingSecurity) {
            liveTownChunk.clearFuelTankState();
        }

        final boolean fuelTankGranted = enteringSecurity && actor != null && this.giveFuelTank(actor, liveTownChunk);
        this.ensureCompositeTownState(liveTownChunk.getTown());
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return new ChunkTypeChangeResult(true, fuelTankGranted, fuelTankRemoval.removed(), fuelTankRemoval.droppedFuel());
    }

    /**
     * Changes a chunk type and optionally resets chunk-local state when the type changes.
     *
     * @param townChunk chunk to update
     * @param chunkType replacement chunk type
     * @return {@code true} when the chunk type changed
     */
    public boolean setChunkType(final @NotNull RTownChunk townChunk, final @NotNull ChunkType chunkType) {
        return this.setChunkType(null, townChunk, chunkType).success();
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
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }
        this.removeFuelTankInternal(liveTownChunk, null, false);
        final RTown town = liveTownChunk.getTown();
        final boolean removed = town.removeChunk(liveTownChunk);
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
     * Updates multiple town-global protection thresholds in one repository write.
     *
     * @param town town to update
     * @param protections protections to update
     * @param requiredRoleId replacement minimum role for every supplied protection
     * @return {@code true} when the live town snapshot was updated
     */
    public boolean setTownProtectionRoleIds(
        final @NotNull RTown town,
        final @NotNull Collection<TownProtections> protections,
        final @Nullable String requiredRoleId
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return false;
        }

        final List<TownProtections> distinctProtections = this.normalizeProtectionList(protections);
        if (distinctProtections.isEmpty()) {
            return false;
        }

        for (final TownProtections protection : distinctProtections) {
            liveTown.setProtectionRoleId(protection, requiredRoleId);
        }
        this.plugin.getTownRepository().update(liveTown);
        for (final TownProtections protection : distinctProtections) {
            this.reconcileProtectionEffects(liveTown, null, protection);
        }
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
     * Updates multiple chunk-specific protection thresholds in one repository write.
     *
     * @param townChunk chunk to update
     * @param protections protections to update
     * @param requiredRoleId replacement override, or {@code null} to clear every supplied override
     * @return {@code true} when the live chunk snapshot was updated
     */
    public boolean setChunkProtectionRoleIds(
        final @NotNull RTownChunk townChunk,
        final @NotNull Collection<TownProtections> protections,
        final @Nullable String requiredRoleId
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }

        final List<TownProtections> distinctProtections = this.normalizeProtectionList(protections);
        if (distinctProtections.isEmpty()) {
            return false;
        }

        for (final TownProtections protection : distinctProtections) {
            liveTownChunk.setProtectionRoleId(protection, requiredRoleId);
        }
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        for (final TownProtections protection : distinctProtections) {
            this.reconcileProtectionEffects(liveTownChunk.getTown(), liveTownChunk, protection);
        }
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

    /**
     * Reconciles loaded hostile and passive mobs in every claimed chunk owned by one town.
     *
     * @param town town whose loaded claimed chunks should be checked
     */
    public void reconcileLoadedProtectionEntities(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return;
        }
        for (final RTownChunk claimedChunk : liveTown.getChunks()) {
            this.reconcileLoadedProtectionEntities(claimedChunk);
        }
    }

    private @Nullable RTown resolveLiveTown(final @NotNull RTown town) {
        final RTown liveTown = this.getTown(town.getTownUUID());
        return liveTown == null ? town : liveTown;
    }

    private @NotNull List<TownProtections> normalizeProtectionList(final @NotNull Collection<TownProtections> protections) {
        final LinkedHashSet<TownProtections> distinctProtections = new LinkedHashSet<>();
        for (final TownProtections protection : protections) {
            if (protection != null) {
                distinctProtections.add(protection);
            }
        }
        return List.copyOf(distinctProtections);
    }

    private @Nullable RTownChunk resolveLiveTownChunk(final @NotNull RTownChunk townChunk) {
        final RTown liveTown = this.resolveLiveTown(townChunk.getTown());
        if (liveTown == null) {
            return townChunk;
        }
        final RTownChunk liveTownChunk = liveTown.findChunk(townChunk.getWorldName(), townChunk.getX(), townChunk.getZ());
        return liveTownChunk == null ? townChunk : liveTownChunk;
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
     * Returns the Security chunk whose fuel tank is placed at the supplied exact location.
     *
     * @param location block location to inspect
     * @return owning Security chunk, or {@code null} when the location is not a fuel tank
     */
    public @Nullable RTownChunk findFuelTankChunk(final @Nullable Location location) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (location == null || townChunk == null || townChunk.getFuelTankLocation() == null) {
            return null;
        }
        return sameBlock(location, townChunk.getFuelTankLocation()) ? townChunk : null;
    }

    /**
     * Returns whether one fuel tank placement location is valid for a Security chunk.
     *
     * @param townChunk owning Security chunk
     * @param tankLocation requested tank placement location
     * @return {@code true} when the placement is inside the owning Security chunk and within radius
     */
    public boolean isValidFuelTankPlacement(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location tankLocation
    ) {
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.SECURITY
            || liveTownChunk.hasFuelTank()
            || tankLocation.getWorld() == null) {
            return false;
        }

        if (!Objects.equals(liveTownChunk.getWorldName(), tankLocation.getWorld().getName())
            || toChunkCoordinate(tankLocation.getBlockX()) != liveTownChunk.getX()
            || toChunkCoordinate(tankLocation.getBlockZ()) != liveTownChunk.getZ()) {
            return false;
        }

        final Location chunkMarkerLocation = liveTownChunk.getChunkBlockLocation();
        if (chunkMarkerLocation == null
            || chunkMarkerLocation.getWorld() == null
            || !Objects.equals(chunkMarkerLocation.getWorld().getName(), tankLocation.getWorld().getName())) {
            return false;
        }

        final int radius = this.plugin.getSecurityConfig().getFuel().getTankPlacementRadiusBlocks();
        return chunkMarkerLocation.distanceSquared(tankLocation) <= ((double) radius * (double) radius);
    }

    /**
     * Persists a placed fuel tank for one Security chunk.
     *
     * @param townChunk owning Security chunk
     * @param tankLocation placed chest location
     * @param tankContents initial persisted tank contents
     * @return {@code true} when the tank state was saved
     */
    public boolean registerFuelTank(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location tankLocation,
        final @NotNull Map<String, ItemStack> tankContents
    ) {
        if (this.plugin.getTownRepository() == null || !this.isValidFuelTankPlacement(townChunk, tankLocation)) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }

        liveTownChunk.setFuelTankLocation(tankLocation);
        liveTownChunk.setFuelTankContents(this.filterConfiguredFuelContents(tankContents));
        if (this.plugin.getTownFuelService() != null) {
            this.plugin.getTownFuelService().syncLiveFuelTankInventory(liveTownChunk);
        }
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Persists the current contents of one placed Security fuel tank.
     *
     * @param townChunk owning Security chunk
     * @param tankContents replacement tank contents
     * @return {@code true} when the contents were saved
     */
    public boolean syncFuelTankContents(
        final @NotNull RTownChunk townChunk,
        final @NotNull Map<String, ItemStack> tankContents
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || !liveTownChunk.hasFuelTank()) {
            return false;
        }

        liveTownChunk.setFuelTankContents(this.filterConfiguredFuelContents(tankContents));
        if (this.plugin.getTownFuelService() != null) {
            this.plugin.getTownFuelService().syncLiveFuelTankInventory(liveTownChunk);
        }
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Removes a placed fuel tank through the Security chunk view and returns the bound tank item.
     *
     * @param player acting player
     * @param townChunk owning Security chunk
     * @return structured pickup result
     */
    public @NotNull FuelTankPickupResult pickupFuelTank(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        if (!this.hasTownPermission(player, TownPermissions.CHANGE_CHUNK_TYPE)) {
            return FuelTankPickupResult.noPermission();
        }
        if (this.plugin.getTownRepository() == null) {
            return FuelTankPickupResult.failed();
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || liveTownChunk.getChunkType() != ChunkType.SECURITY) {
            return FuelTankPickupResult.invalidChunk();
        }
        if (!liveTownChunk.hasFuelTank()) {
            return FuelTankPickupResult.noTank();
        }

        final FuelTankRemoval removal = this.removeFuelTankInternal(liveTownChunk, player, true);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return removal.removed()
            ? FuelTankPickupResult.success(removal.droppedFuel())
            : FuelTankPickupResult.failed();
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
        if (this.areProtectionsBypassedForFuel(town)) {
            return true;
        }
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
        if (this.areProtectionsBypassedForFuel(townChunk.getTown())) {
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
        if (townChunk != null && townChunk.getChunkType() == ChunkType.SECURITY) {
            final String overrideRole = this.resolveChunkProtectionRoleId(townChunk, protection);
            if (overrideRole != null) {
                return RTown.normalizeRoleId(overrideRole);
            }
        }
        return RTown.normalizeRoleId(town.getProtectionRoleId(protection));
    }

    private @Nullable String resolveChunkProtectionRoleId(
        final @NotNull RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        TownProtections currentProtection = protection;
        while (currentProtection != null) {
            final String overrideRole = townChunk.getProtectionRoleId(currentProtection);
            if (overrideRole != null) {
                return overrideRole;
            }
            currentProtection = currentProtection.getFallbackProtection();
        }
        return null;
    }

    private boolean areProtectionsBypassedForFuel(final @NotNull RTown town) {
        return this.plugin.getTownFuelService() != null
            && this.plugin.getTownFuelService().isEnabled()
            && !this.plugin.getTownFuelService().isTownPowered(town);
    }

    private boolean giveFuelTank(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        final ItemStack fuelTankItem = FuelTank.getFuelTankItem(
            this.plugin,
            player,
            townChunk.getTown().getTownUUID(),
            townChunk.getWorldName(),
            townChunk.getX(),
            townChunk.getZ()
        );
        player.getInventory().addItem(fuelTankItem)
            .values()
            .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        return true;
    }

    private @NotNull FuelTankRemoval removeFuelTankInternal(
        final @NotNull RTownChunk townChunk,
        final @Nullable Player player,
        final boolean returnTankItem
    ) {
        if (!townChunk.hasFuelTank()) {
            return FuelTankRemoval.none();
        }

        final Location dropLocation = townChunk.getFuelTankLocation();
        final Map<String, ItemStack> storedFuelContents = this.captureFuelTankContents(townChunk);
        final boolean droppedFuel = dropLocation != null && dropLocation.getWorld() != null && !storedFuelContents.isEmpty();
        if (dropLocation != null && dropLocation.getWorld() != null) {
            if (dropLocation.getBlock().getState() instanceof Chest) {
                dropLocation.getBlock().setType(Material.AIR, false);
            }
            for (final ItemStack itemStack : storedFuelContents.values()) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, itemStack.clone());
                }
            }
        }

        townChunk.clearFuelTankState();
        if (returnTankItem && player != null) {
            this.giveFuelTank(player, townChunk);
        }
        return new FuelTankRemoval(true, droppedFuel);
    }

    private @NotNull Map<String, ItemStack> captureFuelTankContents(final @NotNull RTownChunk townChunk) {
        final Location tankLocation = townChunk.getFuelTankLocation();
        if (tankLocation != null
            && tankLocation.getWorld() != null
            && tankLocation.getWorld().isChunkLoaded(townChunk.getX(), townChunk.getZ())
            && tankLocation.getBlock().getState() instanceof Chest chest) {
            final Map<String, ItemStack> contents = new LinkedHashMap<>();
            for (int slot = 0; slot < chest.getBlockInventory().getSize(); slot++) {
                final ItemStack itemStack = chest.getBlockInventory().getItem(slot);
                if (itemStack == null || itemStack.isEmpty()) {
                    continue;
                }
                contents.put(String.valueOf(slot), itemStack.clone());
            }
            return this.filterConfiguredFuelContents(contents);
        }
        return this.filterConfiguredFuelContents(townChunk.getFuelTankContents());
    }

    private @NotNull Map<String, ItemStack> filterConfiguredFuelContents(final @NotNull Map<String, ItemStack> rawContents) {
        final Map<String, ItemStack> filteredContents = new LinkedHashMap<>();
        for (final Map.Entry<String, ItemStack> entry : rawContents.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (!this.plugin.getSecurityConfig().isFuelMaterial(entry.getValue().getType())) {
                continue;
            }
            filteredContents.put(entry.getKey(), entry.getValue().clone());
        }
        return filteredContents;
    }

    private static boolean sameBlock(final @NotNull Location left, final @NotNull Location right) {
        return left.getWorld() != null
            && right.getWorld() != null
            && Objects.equals(left.getWorld().getName(), right.getWorld().getName())
            && left.getBlockX() == right.getBlockX()
            && left.getBlockY() == right.getBlockY()
            && left.getBlockZ() == right.getBlockZ();
    }

    private @NotNull LevelProgressSnapshot getLevelProgress(
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @Nullable RTown town,
        final @Nullable RTownChunk townChunk,
        final @Nullable Integer requestedPreviewLevel
    ) {
        final LevelContext context = this.resolveLevelContext(scope, town, townChunk);
        if (context == null) {
            return this.createUnavailableSnapshot(scope, town, townChunk);
        }

        final Integer previewLevel = requestedPreviewLevel != null && requestedPreviewLevel > 0 ? requestedPreviewLevel : null;
        final int displayLevel = this.resolveDisplayLevel(context, previewLevel);
        final boolean preview = previewLevel != null && !Objects.equals(previewLevel, context.targetLevel());
        final LevelDefinition displayDefinition = context.levelDefinitions().get(displayLevel);
        final int sourceLevel = this.resolveSourceLevel(context.levelDefinitions(), displayLevel, context.currentLevel());
        final boolean completedLevel = displayLevel <= context.currentLevel();
        final LevelEvaluation evaluation = this.evaluateLevel(player, context, displayLevel, displayDefinition, preview, completedLevel);
        final List<TownLevelRewardSnapshot> rewardSnapshots = this.buildRewardSnapshots(context, sourceLevel, displayLevel, displayDefinition);
        final double progress = completedLevel
            ? 1.0D
            : evaluation.requirements().isEmpty()
                ? 1.0D
                : evaluation.requirements().stream()
                    .mapToDouble(TownLevelRequirementSnapshot::progress)
                    .average()
                    .orElse(0.0D);

        return new LevelProgressSnapshot(
            scope,
            context.available(),
            context.town().getTownUUID(),
            context.town().getTownName(),
            context.chunk() == null ? null : context.chunk().getIdentifier(),
            context.chunk() == null ? null : context.chunk().getWorldName(),
            context.chunk() == null ? null : context.chunk().getChunkType().name(),
            context.chunk() == null ? 0 : context.chunk().getX(),
            context.chunk() == null ? 0 : context.chunk().getZ(),
            context.currentLevel(),
            displayLevel,
            sourceLevel,
            context.maxLevel(),
            preview,
            context.targetLevel() == null && previewLevel == null,
            completedLevel,
            !preview && Objects.equals(context.targetLevel(), displayLevel) && evaluation.readyToLevelUp(),
            progress,
            evaluation.requirements(),
            rewardSnapshots
        );
    }

    private @NotNull ContributionResult contributeCurrency(
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @Nullable RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        if (requestedAmount <= 0.0D) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, false);
        }

        final LevelContext context = this.resolveLevelContext(scope, town, townChunk);
        if (context == null || !context.available() || context.targetLevel() == null) {
            return new ContributionResult(
                context != null && context.targetLevel() == null ? ContributionStatus.MAX_LEVEL : ContributionStatus.INVALID_TARGET,
                0.0D,
                false,
                false
            );
        }
        if (!this.hasTownPermission(player, context.town(), TownPermissions.CONTRIBUTE)) {
            return new ContributionResult(ContributionStatus.NO_PERMISSION, 0.0D, false, false);
        }

        final LevelProgressSnapshot snapshot = this.getLevelProgress(player, scope, town, townChunk, null);
        final TownLevelRequirementSnapshot requirement = snapshot.findRequirement(entryKey);
        if (requirement == null || requirement.kind() != RequirementKind.CURRENCY || !requirement.contributable()) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, snapshot.readyToLevelUp());
        }
        if (requirement.completed()) {
            return new ContributionResult(ContributionStatus.ALREADY_COMPLETE, 0.0D, true, snapshot.readyToLevelUp());
        }

        final double remainingAmount = Math.max(0.0D, requirement.requiredAmount() - requirement.currentAmount());
        final double availableAmount = Math.max(0.0D, requirement.availableAmount());
        final double contributionAmount = Math.min(requestedAmount, Math.min(remainingAmount, availableAmount));
        if (contributionAmount <= 0.0D || !this.withdrawPlayerCurrency(player, requirement.currencyId(), contributionAmount)) {
            return new ContributionResult(ContributionStatus.NOT_ENOUGH_RESOURCES, 0.0D, false, snapshot.readyToLevelUp());
        }

        final String progressKey = this.buildProgressKey(context.scope(), context.targetLevel(), requirement.entryKey());
        context.progressAccessor().setCurrencyProgress(progressKey, requirement.currentAmount() + contributionAmount);
        this.persistLevelContext(context);

        final LevelProgressSnapshot updatedSnapshot = this.getLevelProgress(player, scope, context.town(), context.chunk(), null);
        final TownLevelRequirementSnapshot updatedRequirement = updatedSnapshot.findRequirement(entryKey);
        return new ContributionResult(
            ContributionStatus.SUCCESS,
            contributionAmount,
            updatedRequirement != null && updatedRequirement.completed(),
            updatedSnapshot.readyToLevelUp()
        );
    }

    private @NotNull ContributionResult contributeItem(
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @Nullable RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull String entryKey
    ) {
        final LevelContext context = this.resolveLevelContext(scope, town, townChunk);
        if (context == null || !context.available() || context.targetLevel() == null) {
            return new ContributionResult(
                context != null && context.targetLevel() == null ? ContributionStatus.MAX_LEVEL : ContributionStatus.INVALID_TARGET,
                0.0D,
                false,
                false
            );
        }
        if (!this.hasTownPermission(player, context.town(), TownPermissions.CONTRIBUTE)) {
            return new ContributionResult(ContributionStatus.NO_PERMISSION, 0.0D, false, false);
        }

        final LevelProgressSnapshot snapshot = this.getLevelProgress(player, scope, town, townChunk, null);
        final TownLevelRequirementSnapshot requirement = snapshot.findRequirement(entryKey);
        if (requirement == null
            || requirement.kind() != RequirementKind.ITEM
            || !requirement.contributable()
            || requirement.displayItem() == null) {
            return new ContributionResult(ContributionStatus.INVALID_ENTRY, 0.0D, false, snapshot.readyToLevelUp());
        }
        if (requirement.completed()) {
            return new ContributionResult(ContributionStatus.ALREADY_COMPLETE, 0.0D, true, snapshot.readyToLevelUp());
        }

        final int remainingAmount = (int) Math.round(Math.max(0.0D, requirement.requiredAmount() - requirement.currentAmount()));
        final int removedAmount = this.removeMatchingInventoryItems(
            player,
            requirement.displayItem(),
            requirement.exactMatch(),
            remainingAmount
        );
        if (removedAmount <= 0) {
            return new ContributionResult(ContributionStatus.NOT_ENOUGH_RESOURCES, 0.0D, false, snapshot.readyToLevelUp());
        }

        final String progressKey = this.buildProgressKey(context.scope(), context.targetLevel(), requirement.entryKey());
        context.progressAccessor().setItemProgress(
            progressKey,
            this.createStoredProgressItem(requirement.displayItem(), (int) Math.round(requirement.currentAmount()) + removedAmount)
        );
        this.persistLevelContext(context);

        final LevelProgressSnapshot updatedSnapshot = this.getLevelProgress(player, scope, context.town(), context.chunk(), null);
        final TownLevelRequirementSnapshot updatedRequirement = updatedSnapshot.findRequirement(entryKey);
        return new ContributionResult(
            ContributionStatus.SUCCESS,
            removedAmount,
            updatedRequirement != null && updatedRequirement.completed(),
            updatedSnapshot.readyToLevelUp()
        );
    }

    private @NotNull LevelUpResult levelUp(
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @Nullable RTown town,
        final @Nullable RTownChunk townChunk
    ) {
        final LevelContext context = this.resolveLevelContext(scope, town, townChunk);
        if (context == null || !context.available()) {
            return new LevelUpResult(LevelUpStatus.INVALID_TARGET, 1, 1);
        }
        if (context.targetLevel() == null) {
            return new LevelUpResult(LevelUpStatus.MAX_LEVEL, context.currentLevel(), context.currentLevel());
        }

        final TownPermissions requiredPermission = scope == LevelScope.NEXUS
            ? TownPermissions.UPGRADE_TOWN
            : TownPermissions.UPGRADE_CHUNK;
        if (!this.hasTownPermission(player, context.town(), requiredPermission)) {
            return new LevelUpResult(LevelUpStatus.NO_PERMISSION, context.currentLevel(), context.currentLevel());
        }

        final LevelDefinition levelDefinition = context.levelDefinitions().get(context.targetLevel());
        if (levelDefinition == null) {
            return new LevelUpResult(LevelUpStatus.INVALID_TARGET, context.currentLevel(), context.currentLevel());
        }

        final LevelEvaluation evaluation = this.evaluateLevel(
            player,
            context,
            context.targetLevel(),
            levelDefinition,
            false,
            false
        );
        if (!evaluation.readyToLevelUp()) {
            return new LevelUpResult(LevelUpStatus.NOT_READY, context.currentLevel(), context.currentLevel());
        }

        final int previousLevel = context.currentLevel();
        context.progressAccessor().setCurrentLevel(context.targetLevel());
        context.progressAccessor().clearLevelProgress(this.buildProgressKeyPrefix(scope, context.targetLevel()));
        this.persistLevelContext(context);

        if (!evaluation.passiveRequirements().isEmpty()) {
            RequirementService.getInstance().consumeAll(player, evaluation.passiveRequirements());
        }

        final List<AbstractReward> rewards = this.buildRewards(context, previousLevel, context.targetLevel(), levelDefinition);
        if (!rewards.isEmpty()) {
            try {
                RewardService.getInstance().grantAll(player, rewards).join();
            } catch (final Exception exception) {
                LOGGER.log(
                    Level.WARNING,
                    "Failed to grant one or more " + scope.getDisplayName().toLowerCase(Locale.ROOT)
                        + "-level rewards for town " + context.town().getTownName() + " at level " + context.targetLevel(),
                    exception
                );
            }
        }

        if (town != null && town != context.town()) {
            town.setTownLevel(context.town().getTownLevel());
        }
        if (townChunk != null && townChunk != context.chunk()) {
            townChunk.setChunkLevel(context.targetLevel());
        }
        return new LevelUpResult(LevelUpStatus.SUCCESS, previousLevel, context.targetLevel());
    }

    private @Nullable LevelContext resolveLevelContext(
        final @NotNull LevelScope scope,
        final @Nullable RTown town,
        final @Nullable RTownChunk townChunk
    ) {
        return switch (scope) {
            case NEXUS -> {
                final RTown liveTown = town == null ? null : this.resolveLiveTown(town);
                if (liveTown == null) {
                    yield null;
                }
                yield new LevelContext(
                    scope,
                    liveTown,
                    null,
                    true,
                    liveTown.getNexusLevel(),
                    this.plugin.getNexusConfig().getHighestConfiguredLevel(),
                    this.plugin.getNexusConfig().getNextLevel(liveTown.getNexusLevel()),
                    this.plugin.getNexusConfig().getLevels(),
                    new TownProgressAccessor(liveTown)
                );
            }
            case SECURITY, BANK, FARM, OUTPOST -> {
                final RTownChunk liveChunk = townChunk == null ? null : this.resolveLiveTownChunk(townChunk);
                if (liveChunk == null) {
                    yield null;
                }
                final boolean available = LevelScope.fromChunkType(liveChunk.getChunkType()) == scope;
                yield new LevelContext(
                    scope,
                    liveChunk.getTown(),
                    liveChunk,
                    available,
                    liveChunk.getChunkLevel(),
                    this.getHighestConfiguredLevel(scope),
                    available ? this.getNextConfiguredLevel(scope, liveChunk.getChunkLevel()) : null,
                    this.getConfiguredLevels(scope),
                    new ChunkProgressAccessor(liveChunk)
                );
            }
        };
    }

    private @NotNull LevelProgressSnapshot createUnavailableSnapshot(
        final @NotNull LevelScope scope,
        final @Nullable RTown town,
        final @Nullable RTownChunk townChunk
    ) {
        final RTown resolvedTown = townChunk == null ? town : townChunk.getTown();
        final int currentLevel = scope.isChunkScope() && townChunk != null
            ? townChunk.getChunkLevel()
            : resolvedTown == null
                ? 1
                : resolvedTown.getNexusLevel();
        final int maxLevel = this.getHighestConfiguredLevel(scope);
        return new LevelProgressSnapshot(
            scope,
            false,
            resolvedTown == null ? new UUID(0L, 0L) : resolvedTown.getTownUUID(),
            resolvedTown == null ? "" : resolvedTown.getTownName(),
            townChunk == null ? null : townChunk.getIdentifier(),
            townChunk == null ? null : townChunk.getWorldName(),
            townChunk == null ? null : townChunk.getChunkType().name(),
            townChunk == null ? 0 : townChunk.getX(),
            townChunk == null ? 0 : townChunk.getZ(),
            currentLevel,
            currentLevel,
            currentLevel,
            maxLevel,
            false,
            false,
            false,
            false,
            0.0D,
            List.of(),
            List.of()
        );
    }

    private @NotNull Map<Integer, LevelDefinition> getConfiguredLevels(final @NotNull LevelScope scope) {
        return switch (scope) {
            case NEXUS -> this.plugin.getNexusConfig().getLevels();
            case SECURITY -> this.plugin.getSecurityConfig().getLevels();
            case BANK -> this.plugin.getBankConfig().getLevels();
            case FARM -> this.plugin.getFarmConfig().getLevels();
            case OUTPOST -> this.plugin.getOutpostConfig().getLevels();
        };
    }

    private int getHighestConfiguredLevel(final @NotNull LevelScope scope) {
        return switch (scope) {
            case NEXUS -> this.plugin.getNexusConfig().getHighestConfiguredLevel();
            case SECURITY -> this.plugin.getSecurityConfig().getHighestConfiguredLevel();
            case BANK -> this.plugin.getBankConfig().getHighestConfiguredLevel();
            case FARM -> this.plugin.getFarmConfig().getHighestConfiguredLevel();
            case OUTPOST -> this.plugin.getOutpostConfig().getHighestConfiguredLevel();
        };
    }

    private @Nullable Integer getNextConfiguredLevel(final @NotNull LevelScope scope, final int currentLevel) {
        return switch (scope) {
            case NEXUS -> this.plugin.getNexusConfig().getNextLevel(currentLevel);
            case SECURITY -> this.plugin.getSecurityConfig().getNextLevel(currentLevel);
            case BANK -> this.plugin.getBankConfig().getNextLevel(currentLevel);
            case FARM -> this.plugin.getFarmConfig().getNextLevel(currentLevel);
            case OUTPOST -> this.plugin.getOutpostConfig().getNextLevel(currentLevel);
        };
    }

    private @NotNull LevelScope requireChunkLevelScope(final @NotNull RTownChunk townChunk) {
        final LevelScope scope = LevelScope.fromChunkType(Objects.requireNonNull(townChunk, "townChunk").getChunkType());
        if (scope == null) {
            throw new IllegalArgumentException("Chunk type " + townChunk.getChunkType() + " has no progression path");
        }
        return scope;
    }

    private int resolveDisplayLevel(final @NotNull LevelContext context, final @Nullable Integer previewLevel) {
        if (previewLevel != null && context.levelDefinitions().containsKey(previewLevel)) {
            return previewLevel;
        }
        if (context.targetLevel() != null) {
            return context.targetLevel();
        }
        return context.currentLevel();
    }

    private int resolveSourceLevel(
        final @NotNull Map<Integer, LevelDefinition> levelDefinitions,
        final int targetLevel,
        final int fallbackLevel
    ) {
        return levelDefinitions.keySet().stream()
            .filter(level -> level < targetLevel)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(Math.max(1, fallbackLevel));
    }

    private @NotNull List<AbstractRequirement> buildRequirements(final @NotNull LevelDefinition levelDefinition) {
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();
        final List<AbstractRequirement> requirements = new ArrayList<>();
        for (final Map<String, Object> definition : levelDefinition.getRequirements().values()) {
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(definition);
            requirementFactory.tryFromMap(normalizedDefinition).ifPresent(requirements::add);
        }
        return requirements;
    }

    private @NotNull List<AbstractReward> buildRewards(
        final @NotNull LevelContext context,
        final int sourceLevel,
        final int targetLevel,
        final @NotNull LevelDefinition levelDefinition
    ) {
        final RewardFactory<Map<String, Object>> rewardFactory = RewardFactory.getInstance();
        final List<AbstractReward> rewards = new ArrayList<>();
        final Map<String, String> placeholders = this.buildLevelPlaceholders(context, sourceLevel, targetLevel);
        for (final Map<String, Object> definition : this.expandDefinitionPlaceholders(levelDefinition.getRewards(), placeholders).values()) {
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(definition);
            rewardFactory.tryFromMap(normalizedDefinition).ifPresent(rewards::add);
        }
        return rewards;
    }

    private @NotNull LevelEvaluation evaluateLevel(
        final @NotNull Player player,
        final @NotNull LevelContext context,
        final int displayLevel,
        final @Nullable LevelDefinition levelDefinition,
        final boolean preview,
        final boolean completedLevel
    ) {
        if (levelDefinition == null) {
            return new LevelEvaluation(List.of(), List.of(), completedLevel || context.targetLevel() == null);
        }
        if (levelDefinition.getRequirements().isEmpty()) {
            return new LevelEvaluation(List.of(), List.of(), true);
        }

        final RequirementFactory requirementFactory = RequirementFactory.getInstance();
        final RequirementService requirementService = RequirementService.getInstance();
        final List<TownLevelRequirementSnapshot> requirementSnapshots = new ArrayList<>();
        final List<AbstractRequirement> passiveRequirements = new ArrayList<>();
        final boolean activeTargetLevel = !preview && Objects.equals(context.targetLevel(), displayLevel);

        for (final Map.Entry<String, Map<String, Object>> entry : levelDefinition.getRequirements().entrySet()) {
            final String definitionKey = entry.getKey();
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(entry.getValue());
            final AbstractRequirement requirement = requirementFactory.tryFromMap(normalizedDefinition).orElse(null);
            if (requirement == null) {
                continue;
            }

            if (requirement instanceof CurrencyRequirement currencyRequirement) {
                final String entryKey = definitionKey;
                final String progressKey = this.buildProgressKey(context.scope(), displayLevel, entryKey);
                final String currencyId = this.resolveSupportedCurrencyId(currencyRequirement.getCurrencyId());
                if (!this.isSupportedCurrency(currencyId)) {
                    continue;
                }
                final double currentAmount = completedLevel || !activeTargetLevel
                    ? completedLevel ? currencyRequirement.getAmount() : 0.0D
                    : context.progressAccessor().getCurrencyProgress(progressKey);
                final double availableAmount = activeTargetLevel
                    ? this.getPlayerCurrencyBalance(player, currencyId)
                    : 0.0D;
                requirementSnapshots.add(new TownLevelRequirementSnapshot(
                    entryKey,
                    definitionKey,
                    RequirementKind.CURRENCY,
                    this.resolveCurrencyDisplayName(currencyId),
                    this.resolveConfiguredDescription(entry.getValue(), "Currency contribution"),
                    currencyRequirement.getAmount(),
                    currentAmount,
                    availableAmount,
                    completedLevel || currentAmount + 1.0E-6D >= currencyRequirement.getAmount(),
                    activeTargetLevel,
                    false,
                    currencyId,
                    this.createDisplayItem(Material.GOLD_INGOT, this.resolveCurrencyDisplayName(currencyId), currencyRequirement.getAmount())
                ));
                continue;
            }

            if (requirement instanceof ItemRequirement itemRequirement) {
                final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
                for (int index = 0; index < requiredItems.size(); index++) {
                    final ItemStack requiredItem = requiredItems.get(index);
                    final String entryKey = definitionKey + '#' + index;
                    final String progressKey = this.buildProgressKey(context.scope(), displayLevel, entryKey);
                    final ItemStack storedProgress = activeTargetLevel ? context.progressAccessor().getItemProgress(progressKey) : null;
                    final double currentAmount = completedLevel
                        ? requiredItem.getAmount()
                        : storedProgress == null
                            ? 0.0D
                            : storedProgress.getAmount();
                    final double availableAmount = activeTargetLevel
                        ? this.countMatchingInventoryItems(player, requiredItem, itemRequirement.isExactMatch())
                        : 0.0D;
                    requirementSnapshots.add(new TownLevelRequirementSnapshot(
                        entryKey,
                        definitionKey,
                        RequirementKind.ITEM,
                        this.resolveItemDisplayName(requiredItem),
                        this.resolveConfiguredDescription(entry.getValue(), "Item contribution"),
                        requiredItem.getAmount(),
                        currentAmount,
                        availableAmount,
                        completedLevel || currentAmount + 1.0E-6D >= requiredItem.getAmount(),
                        activeTargetLevel,
                        itemRequirement.isExactMatch(),
                        null,
                        requiredItem
                    ));
                }
                continue;
            }

            final double rawProgress = completedLevel ? 1.0D : requirementService.calculateProgress(player, requirement);
            final double clampedProgress = Math.max(0.0D, Math.min(1.0D, rawProgress));
            final boolean met = completedLevel || requirementService.isMet(player, requirement);
            requirementSnapshots.add(new TownLevelRequirementSnapshot(
                definitionKey,
                definitionKey,
                RequirementKind.OTHER,
                this.resolveRequirementTitle(requirement),
                this.resolveConfiguredDescription(entry.getValue(), requirement.getTypeId()),
                1.0D,
                met ? 1.0D : clampedProgress,
                0.0D,
                met,
                false,
                false,
                null,
                this.createDisplayItem(Material.BOOK, this.resolveRequirementTitle(requirement), 1.0D)
            ));
            if (activeTargetLevel) {
                passiveRequirements.add(requirement);
            }
        }

        final boolean readyToLevelUp = requirementSnapshots.stream().allMatch(TownLevelRequirementSnapshot::completed);
        return new LevelEvaluation(List.copyOf(requirementSnapshots), List.copyOf(passiveRequirements), readyToLevelUp);
    }

    private @NotNull List<TownLevelRewardSnapshot> buildRewardSnapshots(
        final @NotNull LevelContext context,
        final int sourceLevel,
        final int targetLevel,
        final @Nullable LevelDefinition levelDefinition
    ) {
        if (levelDefinition == null || levelDefinition.getRewards().isEmpty()) {
            return List.of();
        }

        final RewardFactory<Map<String, Object>> rewardFactory = RewardFactory.getInstance();
        final Map<String, String> placeholders = this.buildLevelPlaceholders(context, sourceLevel, targetLevel);
        final Map<String, Map<String, Object>> expandedRewards = this.expandDefinitionPlaceholders(levelDefinition.getRewards(), placeholders);
        final List<TownLevelRewardSnapshot> rewardSnapshots = new ArrayList<>();
        for (final Map.Entry<String, Map<String, Object>> entry : expandedRewards.entrySet()) {
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(entry.getValue());
            final AbstractReward reward = rewardFactory.tryFromMap(normalizedDefinition).orElse(null);
            rewardSnapshots.add(this.createRewardSnapshot(entry.getKey(), normalizedDefinition, reward));
        }
        return List.copyOf(rewardSnapshots);
    }

    private @NotNull TownLevelRewardSnapshot createRewardSnapshot(
        final @NotNull String definitionKey,
        final @NotNull Map<String, Object> definition,
        final @Nullable AbstractReward reward
    ) {
        if (reward instanceof ItemReward itemReward) {
            final ItemStack displayItem = itemReward.getItem();
            displayItem.setAmount(itemReward.getAmount());
            return new TownLevelRewardSnapshot(
                definitionKey,
                reward.getTypeId(),
                this.resolveItemDisplayName(displayItem),
                this.resolveConfiguredDescription(definition, "Receive item reward"),
                displayItem
            );
        }
        if (reward instanceof CurrencyReward currencyReward) {
            return new TownLevelRewardSnapshot(
                definitionKey,
                reward.getTypeId(),
                this.resolveCurrencyDisplayName(currencyReward.getCurrencyId()),
                this.resolveConfiguredDescription(
                    definition,
                    "Receive %.2f %s".formatted(currencyReward.getAmount(), this.resolveCurrencyDisplayName(currencyReward.getCurrencyId()))
                ),
                this.createDisplayItem(Material.GOLD_BLOCK, this.resolveCurrencyDisplayName(currencyReward.getCurrencyId()), currencyReward.getAmount())
            );
        }
        if (reward instanceof ExperienceReward experienceReward) {
            return new TownLevelRewardSnapshot(
                definitionKey,
                reward.getTypeId(),
                "Experience",
                this.resolveConfiguredDescription(
                    definition,
                    "Receive %d %s".formatted(experienceReward.getAmount(), experienceReward.getExperienceType().name().toLowerCase(Locale.ROOT))
                ),
                this.createDisplayItem(Material.EXPERIENCE_BOTTLE, "Experience", experienceReward.getAmount())
            );
        }
        if (reward instanceof PermissionReward permissionReward) {
            return new TownLevelRewardSnapshot(
                definitionKey,
                reward.getTypeId(),
                "Permissions",
                this.resolveConfiguredDescription(definition, String.join(", ", permissionReward.getPermissions())),
                this.createDisplayItem(Material.NAME_TAG, "Permissions", permissionReward.getPermissions().size())
            );
        }
        if (reward instanceof CommandReward commandReward) {
            return new TownLevelRewardSnapshot(
                definitionKey,
                reward.getTypeId(),
                this.isTownBroadcastCommand(commandReward.getCommand()) ? "Town Broadcast" : "Command Reward",
                this.resolveConfiguredDescription(
                    definition,
                    this.isTownBroadcastCommand(commandReward.getCommand())
                        ? "Broadcast a level-up message to online town members."
                        : commandReward.getCommand()
                ),
                this.createDisplayItem(Material.COMMAND_BLOCK, "Command Reward", 1.0D)
            );
        }

        final String typeId = reward == null
            ? String.valueOf(definition.getOrDefault("type", "UNKNOWN"))
            : reward.getTypeId();
        return new TownLevelRewardSnapshot(
            definitionKey,
            typeId,
            this.toDisplayLabel(typeId),
            this.resolveConfiguredDescription(definition, this.toDisplayLabel(typeId)),
            this.createDisplayItem(Material.CHEST, this.toDisplayLabel(typeId), 1.0D)
        );
    }

    private boolean isTownBroadcastCommand(final @Nullable String command) {
        return command != null && command.toLowerCase(Locale.ROOT).startsWith("rt broadcast ");
    }

    private @NotNull Map<String, String> buildLevelPlaceholders(
        final @NotNull LevelContext context,
        final int sourceLevel,
        final int targetLevel
    ) {
        final Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("town_uuid", context.town().getTownUUID().toString());
        placeholders.put("town_name", context.town().getTownName());
        placeholders.put("current_level", String.valueOf(sourceLevel));
        placeholders.put("target_level", String.valueOf(targetLevel));
        placeholders.put("level_scope", context.scope().name().toLowerCase(Locale.ROOT));
        placeholders.put("level_scope_name", context.scope().getDisplayName());
        placeholders.put("chunk_uuid", context.chunk() == null ? "" : context.chunk().getIdentifier().toString());
        placeholders.put("chunk_type", context.chunk() == null ? "" : context.chunk().getChunkType().name());
        placeholders.put("chunk_x", context.chunk() == null ? "0" : String.valueOf(context.chunk().getX()));
        placeholders.put("chunk_z", context.chunk() == null ? "0" : String.valueOf(context.chunk().getZ()));
        return Map.copyOf(placeholders);
    }

    private @NotNull Map<String, Map<String, Object>> expandDefinitionPlaceholders(
        final @NotNull Map<String, Map<String, Object>> definitions,
        final @NotNull Map<String, String> placeholders
    ) {
        final Map<String, Map<String, Object>> expanded = new LinkedHashMap<>();
        for (final Map.Entry<String, Map<String, Object>> entry : definitions.entrySet()) {
            expanded.put(entry.getKey(), this.expandDefinition(entry.getValue(), placeholders));
        }
        return Map.copyOf(expanded);
    }

    private @NotNull Map<String, Object> expandDefinition(
        final @NotNull Map<String, Object> definition,
        final @NotNull Map<String, String> placeholders
    ) {
        final Map<String, Object> expanded = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : definition.entrySet()) {
            expanded.put(entry.getKey(), this.expandDefinitionValue(entry.getValue(), placeholders));
        }
        return expanded;
    }

    private @Nullable Object expandDefinitionValue(
        final @Nullable Object value,
        final @NotNull Map<String, String> placeholders
    ) {
        if (value instanceof String stringValue) {
            String expanded = stringValue;
            for (final Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                expanded = expanded.replace('{' + placeholder.getKey() + '}', placeholder.getValue());
            }
            return expanded;
        }
        if (value instanceof Map<?, ?> nestedMap) {
            final Map<String, Object> nestedExpanded = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                if (nestedEntry.getKey() == null) {
                    continue;
                }
                nestedExpanded.put(String.valueOf(nestedEntry.getKey()), this.expandDefinitionValue(nestedEntry.getValue(), placeholders));
            }
            return nestedExpanded;
        }
        if (value instanceof List<?> list) {
            final List<Object> expandedList = new ArrayList<>();
            for (final Object entry : list) {
                expandedList.add(this.expandDefinitionValue(entry, placeholders));
            }
            return expandedList;
        }
        return value;
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

    private boolean hasTownPermission(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull TownPermissions permission
    ) {
        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        return playerData != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(permission);
    }

    private @NotNull String buildProgressKeyPrefix(final @NotNull LevelScope scope, final int targetLevel) {
        return scope.name().toLowerCase(Locale.ROOT) + ".level." + Math.max(1, targetLevel) + '.';
    }

    private @NotNull String buildProgressKey(
        final @NotNull LevelScope scope,
        final int targetLevel,
        final @NotNull String entryKey
    ) {
        return this.buildProgressKeyPrefix(scope, targetLevel) + entryKey;
    }

    private void persistLevelContext(final @NotNull LevelContext context) {
        if (this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(context.town());
        }
    }

    private void persistTownCreationContext(final @NotNull LevelContext context) {
        if (context.progressAccessor() instanceof PlayerCreationProgressAccessor creationProgressAccessor) {
            this.persistPlayerData(creationProgressAccessor.playerData());
        }
    }

    private void persistPlayerData(final @NotNull RDTPlayer playerData) {
        if (this.plugin.getPlayerRepository() == null) {
            return;
        }
        if (this.plugin.getPlayerRepository().findByPlayer(playerData.getIdentifier()) == null) {
            this.plugin.getPlayerRepository().create(playerData);
            return;
        }
        this.plugin.getPlayerRepository().update(playerData);
    }

    private @Nullable LevelContext resolveTownCreationContext(final @NotNull Player player) {
        if (this.plugin.getPlayerRepository() == null || this.getTownFor(player.getUniqueId()) != null) {
            return null;
        }

        final LevelDefinition creationDefinition = this.plugin.getNexusConfig().getLevelDefinition(1);
        if (creationDefinition == null) {
            return null;
        }

        final RDTPlayer playerData = this.getOrCreatePlayerData(player.getUniqueId());
        return new LevelContext(
            LevelScope.NEXUS,
            this.createTownCreationPlaceholder(player),
            null,
            true,
            0,
            Math.max(1, this.plugin.getNexusConfig().getHighestConfiguredLevel()),
            1,
            Map.of(1, creationDefinition),
            new PlayerCreationProgressAccessor(playerData)
        );
    }

    private @NotNull RTown createTownCreationPlaceholder(final @NotNull Player player) {
        return new RTown(
            new UUID(0L, 0L),
            player.getUniqueId(),
            "Pending Town",
            null
        );
    }

    private void grantTownCreationRewards(final @NotNull Player player, final @NotNull RTown town) {
        final LevelDefinition creationDefinition = this.plugin.getNexusConfig().getLevelDefinition(1);
        if (creationDefinition == null || creationDefinition.getRewards().isEmpty()) {
            return;
        }

        final LevelContext rewardContext = new LevelContext(
            LevelScope.NEXUS,
            town,
            null,
            true,
            0,
            Math.max(1, this.plugin.getNexusConfig().getHighestConfiguredLevel()),
            1,
            this.plugin.getNexusConfig().getLevels(),
            new TownProgressAccessor(town)
        );
        final List<AbstractReward> rewards = this.buildRewards(rewardContext, 0, 1, creationDefinition);
        if (rewards.isEmpty()) {
            return;
        }

        try {
            RewardService.getInstance().grantAll(player, rewards).join();
        } catch (final Exception exception) {
            LOGGER.log(
                Level.WARNING,
                "Failed to grant one or more town-creation rewards for " + town.getTownName(),
                exception
            );
        }
    }

    private double getPlayerCurrencyBalance(final @NotNull Player player, final @Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return 0.0D;
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            return bridge.getBalance(player, resolvedCurrencyId);
        }

        final var economy = this.plugin.getEco();
        if (economy != null && this.isVaultCurrency(resolvedCurrencyId)) {
            return economy.getBalance(player);
        }
        return 0.0D;
    }

    private boolean withdrawPlayerCurrency(
        final @NotNull Player player,
        final @Nullable String currencyId,
        final double amount
    ) {
        if (amount <= 0.0D || currencyId == null || currencyId.isBlank()) {
            return false;
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            return bridge.withdraw(player, resolvedCurrencyId, amount).join();
        }

        final var economy = this.plugin.getEco();
        if (economy != null && this.isVaultCurrency(resolvedCurrencyId)) {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        }
        return false;
    }

    private boolean isVaultCurrency(final @NotNull String currencyId) {
        final String normalizedCurrencyId = currencyId.trim().toLowerCase(Locale.ROOT);
        return Objects.equals(normalizedCurrencyId, "vault")
            || Objects.equals(normalizedCurrencyId, "money")
            || Objects.equals(normalizedCurrencyId, "dollars");
    }

    private int countMatchingInventoryItems(
        final @NotNull Player player,
        final @NotNull ItemStack requiredItem,
        final boolean exactMatch
    ) {
        if (exactMatch) {
            return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(stack -> stack.isSimilar(requiredItem))
                .mapToInt(ItemStack::getAmount)
                .sum();
        }
        return player.getInventory().all(requiredItem.getType()).values().stream()
            .mapToInt(ItemStack::getAmount)
            .sum();
    }

    private int removeMatchingInventoryItems(
        final @NotNull Player player,
        final @NotNull ItemStack requiredItem,
        final boolean exactMatch,
        final int requiredAmount
    ) {
        int remaining = Math.max(0, requiredAmount);
        final ItemStack[] contents = player.getInventory().getContents();

        for (int index = 0; index < contents.length && remaining > 0; index++) {
            final ItemStack currentStack = contents[index];
            if (currentStack == null) {
                continue;
            }

            final boolean matches = exactMatch
                ? currentStack.isSimilar(requiredItem)
                : currentStack.getType() == requiredItem.getType();
            if (!matches) {
                continue;
            }

            final int removed = Math.min(remaining, currentStack.getAmount());
            currentStack.setAmount(currentStack.getAmount() - removed);
            remaining -= removed;
            if (currentStack.getAmount() <= 0) {
                contents[index] = null;
            }
        }

        player.getInventory().setContents(contents);
        return Math.max(0, requiredAmount - remaining);
    }

    private @NotNull ItemStack createStoredProgressItem(final @NotNull ItemStack template, final int amount) {
        final ItemStack storedItem = template.clone();
        storedItem.setAmount(Math.max(1, amount));
        return storedItem;
    }

    private @Nullable ItemStack createDisplayItem(
        final @NotNull Material material,
        final @NotNull String name,
        final double amount
    ) {
        try {
            final ItemStack displayItem = new ItemStack(material);
            final ItemMeta itemMeta = displayItem.getItemMeta();
            if (itemMeta != null) {
                itemMeta.displayName(Component.text(name));
                displayItem.setItemMeta(itemMeta);
            }
            displayItem.setAmount(Math.max(1, (int) Math.round(amount)));
            return displayItem;
        } catch (final RuntimeException | LinkageError exception) {
            LOGGER.log(
                Level.FINE,
                "Unable to create a level display item for material " + material + "; continuing without an icon.",
                exception
            );
            return null;
        }
    }

    private @NotNull String resolveConfiguredDescription(
        final @NotNull Map<String, Object> definition,
        final @NotNull String fallback
    ) {
        final Object configuredDescription = definition.get("description");
        return configuredDescription instanceof String description && !description.isBlank()
            ? description.trim()
            : fallback;
    }

    private @NotNull String resolveRequirementTitle(final @NotNull AbstractRequirement requirement) {
        return switch (requirement.getTypeId().toUpperCase(Locale.ROOT)) {
            case "PLAYTIME" -> "Playtime";
            case "PLUGIN" -> "Plugin Requirement";
            case "PERMISSION" -> "Permission";
            default -> this.toDisplayLabel(requirement.getTypeId());
        };
    }

    private @NotNull String resolveCurrencyDisplayName(final @NotNull String currencyId) {
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            final String displayName = bridge.getCurrencyDisplayName(resolvedCurrencyId);
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
        }
        if (this.isVaultCurrency(resolvedCurrencyId)) {
            return "Vault";
        }
        return this.toDisplayLabel(resolvedCurrencyId);
    }

    private @NotNull String resolveSupportedCurrencyId(final @Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return "";
        }

        final String trimmedCurrencyId = currencyId.trim();
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null) {
            return trimmedCurrencyId;
        }
        if (bridge.hasCurrency(trimmedCurrencyId)) {
            return trimmedCurrencyId;
        }

        final String normalizedLookupToken = this.normalizeCurrencyLookupToken(trimmedCurrencyId);
        final Map<String, String> availableCurrencies = bridge.getAvailableCurrencies();
        if (availableCurrencies == null || availableCurrencies.isEmpty()) {
            return trimmedCurrencyId;
        }
        for (final Map.Entry<String, String> entry : availableCurrencies.entrySet()) {
            final String candidateCurrencyId = entry.getKey();
            final String candidateDisplayName = entry.getValue();
            if (trimmedCurrencyId.equalsIgnoreCase(candidateCurrencyId)
                || trimmedCurrencyId.equalsIgnoreCase(candidateDisplayName)
                || normalizedLookupToken.equals(this.normalizeCurrencyLookupToken(candidateCurrencyId))
                || normalizedLookupToken.equals(this.normalizeCurrencyLookupToken(candidateDisplayName))) {
                return candidateCurrencyId;
            }
        }
        return trimmedCurrencyId;
    }

    private boolean isSupportedCurrency(final @Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return false;
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            return true;
        }

        return this.plugin.getEco() != null && this.isVaultCurrency(resolvedCurrencyId);
    }

    private @NotNull String normalizeCurrencyLookupToken(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private @NotNull String resolveItemDisplayName(final @NotNull ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(itemMeta.displayName());
        }
        return this.toDisplayLabel(itemStack.getType().name());
    }

    private @NotNull String toDisplayLabel(final @NotNull String rawValue) {
        final String normalized = rawValue.trim().replace('_', ' ');
        if (normalized.isEmpty()) {
            return "";
        }

        final String[] words = normalized.split("\\s+");
        final List<String> formattedWords = new ArrayList<>();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            final String lowerCaseWord = word.toLowerCase(Locale.ROOT);
            formattedWords.add(Character.toUpperCase(lowerCaseWord.charAt(0)) + lowerCaseWord.substring(1));
        }
        return String.join(" ", formattedWords);
    }

    /**
     * Result of a chunk-type transition that may grant or remove a fuel tank.
     *
     * @param success whether the chunk type changed successfully
     * @param fuelTankGranted whether a new bound fuel tank item was granted
     * @param fuelTankRemoved whether an existing placed fuel tank was removed
     * @param droppedFuel whether stored fuel items were dropped at the tank location
     */
    public record ChunkTypeChangeResult(
        boolean success,
        boolean fuelTankGranted,
        boolean fuelTankRemoved,
        boolean droppedFuel
    ) {

        private static @NotNull ChunkTypeChangeResult failure() {
            return new ChunkTypeChangeResult(false, false, false, false);
        }
    }

    /**
     * Status for a Security fuel tank pickup action.
     */
    public enum FuelTankPickupStatus {
        SUCCESS,
        NO_PERMISSION,
        INVALID_CHUNK,
        NO_TANK,
        FAILED
    }

    /**
     * Result of a Security fuel tank pickup action.
     *
     * @param status pickup outcome
     * @param droppedFuel whether stored fuel items were dropped at the former tank location
     */
    public record FuelTankPickupResult(
        @NotNull FuelTankPickupStatus status,
        boolean droppedFuel
    ) {

        private static @NotNull FuelTankPickupResult success(final boolean droppedFuel) {
            return new FuelTankPickupResult(FuelTankPickupStatus.SUCCESS, droppedFuel);
        }

        private static @NotNull FuelTankPickupResult noPermission() {
            return new FuelTankPickupResult(FuelTankPickupStatus.NO_PERMISSION, false);
        }

        private static @NotNull FuelTankPickupResult invalidChunk() {
            return new FuelTankPickupResult(FuelTankPickupStatus.INVALID_CHUNK, false);
        }

        private static @NotNull FuelTankPickupResult noTank() {
            return new FuelTankPickupResult(FuelTankPickupStatus.NO_TANK, false);
        }

        private static @NotNull FuelTankPickupResult failed() {
            return new FuelTankPickupResult(FuelTankPickupStatus.FAILED, false);
        }
    }

    private record LevelContext(
        @NotNull LevelScope scope,
        @NotNull RTown town,
        @Nullable RTownChunk chunk,
        boolean available,
        int currentLevel,
        int maxLevel,
        @Nullable Integer targetLevel,
        @NotNull Map<Integer, LevelDefinition> levelDefinitions,
        @NotNull ProgressAccessor progressAccessor
    ) {
    }

    private record LevelEvaluation(
        @NotNull List<TownLevelRequirementSnapshot> requirements,
        @NotNull List<AbstractRequirement> passiveRequirements,
        boolean readyToLevelUp
    ) {
    }

    private record FuelTankRemoval(boolean removed, boolean droppedFuel) {

        private static @NotNull FuelTankRemoval none() {
            return new FuelTankRemoval(false, false);
        }
    }

    private sealed interface ProgressAccessor permits TownProgressAccessor, ChunkProgressAccessor, PlayerCreationProgressAccessor {

        int currentLevel();

        void setCurrentLevel(int level);

        double getCurrencyProgress(@NotNull String progressKey);

        void setCurrencyProgress(@NotNull String progressKey, double amount);

        @Nullable ItemStack getItemProgress(@NotNull String progressKey);

        void setItemProgress(@NotNull String progressKey, @Nullable ItemStack itemStack);

        void clearLevelProgress(@NotNull String progressKeyPrefix);
    }

    private record TownProgressAccessor(@NotNull RTown town) implements ProgressAccessor {

        @Override
        public int currentLevel() {
            return this.town.getNexusLevel();
        }

        @Override
        public void setCurrentLevel(final int level) {
            this.town.setNexusLevel(level);
        }

        @Override
        public double getCurrencyProgress(final @NotNull String progressKey) {
            return this.town.getLevelCurrencyProgress(progressKey);
        }

        @Override
        public void setCurrencyProgress(final @NotNull String progressKey, final double amount) {
            this.town.setLevelCurrencyProgress(progressKey, amount);
        }

        @Override
        public @Nullable ItemStack getItemProgress(final @NotNull String progressKey) {
            return this.town.getLevelItemProgress(progressKey);
        }

        @Override
        public void setItemProgress(final @NotNull String progressKey, final @Nullable ItemStack itemStack) {
            this.town.setLevelItemProgress(progressKey, itemStack);
        }

        @Override
        public void clearLevelProgress(final @NotNull String progressKeyPrefix) {
            this.town.clearLevelRequirementProgress(progressKeyPrefix);
        }
    }

    private record ChunkProgressAccessor(@NotNull RTownChunk townChunk) implements ProgressAccessor {

        @Override
        public int currentLevel() {
            return this.townChunk.getChunkLevel();
        }

        @Override
        public void setCurrentLevel(final int level) {
            this.townChunk.setChunkLevel(level);
        }

        @Override
        public double getCurrencyProgress(final @NotNull String progressKey) {
            return this.townChunk.getLevelCurrencyProgress(progressKey);
        }

        @Override
        public void setCurrencyProgress(final @NotNull String progressKey, final double amount) {
            this.townChunk.setLevelCurrencyProgress(progressKey, amount);
        }

        @Override
        public @Nullable ItemStack getItemProgress(final @NotNull String progressKey) {
            return this.townChunk.getLevelItemProgress(progressKey);
        }

        @Override
        public void setItemProgress(final @NotNull String progressKey, final @Nullable ItemStack itemStack) {
            this.townChunk.setLevelItemProgress(progressKey, itemStack);
        }

        @Override
        public void clearLevelProgress(final @NotNull String progressKeyPrefix) {
            this.townChunk.clearLevelRequirementProgress(progressKeyPrefix);
        }
    }

    private record PlayerCreationProgressAccessor(@NotNull RDTPlayer playerData) implements ProgressAccessor {

        @Override
        public int currentLevel() {
            return 0;
        }

        @Override
        public void setCurrentLevel(final int level) {
            // Town creation always targets Nexus level 1 before the town exists.
        }

        @Override
        public double getCurrencyProgress(final @NotNull String progressKey) {
            return this.playerData.getTownCreationCurrencyProgress(progressKey);
        }

        @Override
        public void setCurrencyProgress(final @NotNull String progressKey, final double amount) {
            this.playerData.setTownCreationCurrencyProgress(progressKey, amount);
        }

        @Override
        public @Nullable ItemStack getItemProgress(final @NotNull String progressKey) {
            return this.playerData.getTownCreationItemProgress(progressKey);
        }

        @Override
        public void setItemProgress(final @NotNull String progressKey, final @Nullable ItemStack itemStack) {
            this.playerData.setTownCreationItemProgress(progressKey, itemStack);
        }

        @Override
        public void clearLevelProgress(final @NotNull String progressKeyPrefix) {
            this.playerData.clearTownCreationRequirementProgress(progressKeyPrefix);
        }
    }
}
