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
import com.raindropcentral.rdt.configs.NexusCombatStats;
import com.raindropcentral.rdt.database.entity.*;
import com.raindropcentral.rdt.items.FuelTank;
import com.raindropcentral.rdt.items.RepairBlock;
import com.raindropcentral.rdt.items.SalvageBlock;
import com.raindropcentral.rdt.items.SeedBox;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownColorUtil;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rdt.utils.TownRelationshipState;
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
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
     * Returns the configured Nexus level required before nation creation unlocks.
     *
     * @return required Nexus level for nation creation
     */
    public int getTownNationUnlockLevel() {
        return this.plugin.getDefaultConfig().getTownNationUnlockLevel();
    }

    /**
     * Returns the configured minimum number of towns required to form a nation, including the
     * initiating capital town.
     *
     * @return minimum required town count
     */
    public int getTownNationMinTowns() {
        return this.plugin.getDefaultConfig().getTownNationMinTowns();
    }

    /**
     * Returns the configured nation invite timeout in milliseconds.
     *
     * @return nation invite timeout in milliseconds
     */
    public long getTownNationInviteTimeoutMillis() {
        return Math.max(0L, this.plugin.getDefaultConfig().getTownNationInviteTimeoutSeconds()) * 1000L;
    }

    /**
     * Returns whether one town has unlocked nation creation.
     *
     * @param town town to inspect
     * @return {@code true} when the town can begin the nation creation flow
     */
    public boolean areNationsUnlocked(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        final RTown resolvedTown = liveTown == null ? town : liveTown;
        return resolvedTown.getNexusLevel() >= this.getTownNationUnlockLevel()
            && this.getAlliedTowns(resolvedTown).stream().findAny().isPresent();
    }

    /**
     * Returns the current nation creation progress snapshot for one town member.
     *
     * @param player viewing player
     * @return nation creation progress snapshot
     */
    public @NotNull NationCreationProgressSnapshot getNationCreationProgress(final @NotNull Player player) {
        this.processPendingNationTimeouts();

        final RTown town = this.getTownFor(player.getUniqueId());
        if (town == null) {
            return new NationCreationProgressSnapshot(false, false, false, false, 0.0D, List.of(), List.of());
        }

        final boolean alreadyInNation = this.getNationForTown(town) != null;
        final boolean pendingNation = this.getPendingNationCreatedBy(town) != null || this.getPendingNationInviteFor(town) != null;
        final LevelProgressSnapshot snapshot = this.getLevelProgress(player, LevelScope.NATION_FORMATION, town, null, null);
        return new NationCreationProgressSnapshot(
            snapshot.available(),
            alreadyInNation,
            pendingNation,
            snapshot.readyToLevelUp(),
            snapshot.progress(),
            snapshot.requirements(),
            snapshot.rewards()
        );
    }

    /**
     * Returns whether the player can finalize nation creation for their current town.
     *
     * @param player player to inspect
     * @return {@code true} when nation creation can proceed to the naming step
     */
    public boolean canCreateNation(final @NotNull Player player) {
        final RTown town = this.getTownFor(player.getUniqueId());
        return town != null
            && this.hasTownPermission(player, town, TownPermissions.MANAGE_NATIONS)
            && this.getNationCreationProgress(player).readyToCreate();
    }

    /**
     * Saves a shared nation creation currency contribution for the player's town.
     *
     * @param player contributing player
     * @param entryKey requirement entry key
     * @param requestedAmount requested contribution amount
     * @return structured contribution result
     */
    public @NotNull ContributionResult contributeNationCreationCurrency(
        final @NotNull Player player,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        final RTown town = this.getTownFor(player.getUniqueId());
        return town == null
            ? new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false)
            : this.contributeCurrency(player, LevelScope.NATION_FORMATION, town, null, entryKey, requestedAmount);
    }

    /**
     * Saves a shared nation creation item contribution for the player's town.
     *
     * @param player contributing player
     * @param entryKey requirement entry key
     * @return structured contribution result
     */
    public @NotNull ContributionResult contributeNationCreationItem(
        final @NotNull Player player,
        final @NotNull String entryKey
    ) {
        final RTown town = this.getTownFor(player.getUniqueId());
        return town == null
            ? new ContributionResult(ContributionStatus.INVALID_TARGET, 0.0D, false, false)
            : this.contributeItem(player, LevelScope.NATION_FORMATION, town, null, entryKey);
    }

    /**
     * Returns one persisted nation by UUID.
     *
     * @param nationUuid nation UUID
     * @return matching nation, or {@code null} when none exists
     */
    public @Nullable RNation getNation(final @NotNull UUID nationUuid) {
        if (this.plugin.getNationRepository() == null) {
            return null;
        }

        final RNation nation = this.plugin.getNationRepository().findByNationUuid(nationUuid);
        if (nation != null) {
            this.ensureNationProgressState(nation);
        }
        return nation;
    }

    /**
     * Returns the active nation for one town, if present.
     *
     * @param town town to inspect
     * @return active nation, or {@code null} when none exists
     */
    public @Nullable RNation getNationForTown(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        final RTown resolvedTown = liveTown == null ? town : liveTown;
        return resolvedTown.getNationUuid() == null ? null : this.getNation(resolvedTown.getNationUuid());
    }

    /**
     * Returns the pending nation proposal created by one town.
     *
     * @param town town to inspect
     * @return pending nation proposal, or {@code null} when none exists
     */
    public @Nullable RNation getPendingNationCreatedBy(final @NotNull RTown town) {
        this.processPendingNationTimeouts();
        if (this.plugin.getNationRepository() == null) {
            return null;
        }
        return this.plugin.getNationRepository().findPendingByInitiatingTownUuid(town.getTownUUID()).stream()
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the pending nation invite addressed to one town.
     *
     * @param town town to inspect
     * @return pending nation invite, or {@code null} when none exists
     */
    public @Nullable NationInvite getPendingNationInviteFor(final @NotNull RTown town) {
        this.processPendingNationTimeouts();
        if (this.plugin.getNationInviteRepository() == null) {
            return null;
        }
        return this.plugin.getNationInviteRepository().findPendingByTargetTownUuid(town.getTownUUID()).stream()
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the towns that currently belong to one active nation.
     *
     * @param nation nation to inspect
     * @return active member towns
     */
    public @NotNull List<RTown> getNationMemberTowns(final @NotNull RNation nation) {
        return this.getTowns().stream()
            .filter(town -> Objects.equals(town.getNationUuid(), nation.getNationUuid()))
            .sorted(Comparator.comparing(RTown::getTownName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /**
     * Returns the persisted invites for one nation.
     *
     * @param nation nation to inspect
     * @return nation invites sorted by creation time
     */
    public @NotNull List<NationInvite> getNationInvites(final @NotNull RNation nation) {
        if (this.plugin.getNationInviteRepository() == null) {
            return List.of();
        }
        return this.plugin.getNationInviteRepository().findByNationUuid(nation.getNationUuid()).stream()
            .sorted(Comparator.comparingLong(NationInvite::getCreatedAtMillis))
            .toList();
    }

    /**
     * Returns allied towns that can be selected during nation formation.
     *
     * @param town initiating town
     * @return eligible allied towns
     */
    public @NotNull List<RTown> getEligibleNationFormationTowns(final @NotNull RTown town) {
        this.processPendingNationTimeouts();
        return this.getAlliedTowns(town).stream()
            .map(this::resolveLiveTown)
            .filter(Objects::nonNull)
            .filter(alliedTown -> !Objects.equals(alliedTown.getTownUUID(), town.getTownUUID()))
            .filter(alliedTown -> this.getNationForTown(alliedTown) == null)
            .filter(alliedTown -> this.getPendingNationCreatedBy(alliedTown) == null)
            .filter(alliedTown -> this.getPendingNationInviteFor(alliedTown) == null)
            .sorted(Comparator.comparing(RTown::getTownName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    /**
     * Returns allied towns that can be invited into an active nation expansion.
     *
     * @param nation active nation
     * @return eligible allied towns
     */
    public @NotNull List<RTown> getEligibleNationExpansionTowns(final @NotNull RNation nation) {
        final RTown capitalTown = this.getTown(nation.getCapitalTownUuid());
        if (capitalTown == null) {
            return List.of();
        }
        return this.getEligibleNationFormationTowns(capitalTown);
    }

    /**
     * Creates a pending nation proposal from the initiating capital town.
     *
     * @param player initiating player
     * @param nationName requested nation name
     * @param selectedTownUuids selected allied towns to invite
     * @return nation action result
     */
    public @NotNull NationActionResult createNation(
        final @NotNull Player player,
        final @NotNull String nationName,
        final @NotNull Collection<UUID> selectedTownUuids
    ) {
        this.processPendingNationTimeouts();

        final RTown town = this.getTownFor(player.getUniqueId());
        if (town == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, null);
        }
        if (!this.hasTownPermission(player, town, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, null);
        }
        if (this.getNationForTown(town) != null) {
            return new NationActionResult(NationActionStatus.ALREADY_IN_NATION, null);
        }
        if (this.getPendingNationCreatedBy(town) != null || this.getPendingNationInviteFor(town) != null) {
            return new NationActionResult(NationActionStatus.ALREADY_PENDING, null);
        }
        if (!this.canCreateNation(player)) {
            return new NationActionResult(NationActionStatus.NOT_READY, null);
        }

        final String normalizedNationName = nationName.trim();
        if (normalizedNationName.isEmpty() || normalizedNationName.length() > 32) {
            return new NationActionResult(NationActionStatus.INVALID_NAME, null);
        }
        if (this.plugin.getNationRepository() != null
            && this.plugin.getNationRepository().findByNationName(normalizedNationName) != null) {
            return new NationActionResult(NationActionStatus.NAME_TAKEN, null);
        }

        final Set<UUID> normalizedSelections = selectedTownUuids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        final List<RTown> eligibleTowns = this.getEligibleNationFormationTowns(town);
        final Map<UUID, RTown> eligibleById = eligibleTowns.stream()
            .collect(Collectors.toMap(RTown::getTownUUID, alliedTown -> alliedTown));
        final List<RTown> selectedTowns = normalizedSelections.stream()
            .map(eligibleById::get)
            .filter(Objects::nonNull)
            .toList();
        if (selectedTowns.size() != normalizedSelections.size()) {
            return new NationActionResult(NationActionStatus.INVALID_SELECTION, null);
        }
        if (selectedTowns.size() + 1 < this.getTownNationMinTowns()) {
            return new NationActionResult(NationActionStatus.NOT_ENOUGH_TOWNS, null);
        }
        if (this.plugin.getNationRepository() == null || this.plugin.getNationInviteRepository() == null) {
            return new NationActionResult(NationActionStatus.FAILED, null);
        }

        final long expiresAt = System.currentTimeMillis() + this.getTownNationInviteTimeoutMillis();
        final RNation nation = new RNation(
            UUID.randomUUID(),
            normalizedNationName,
            town.getTownUUID(),
            town.getTownUUID(),
            player.getUniqueId(),
            this.getTownNationMinTowns(),
            expiresAt
        );
        this.plugin.getNationRepository().create(nation);
        for (final RTown selectedTown : selectedTowns) {
            this.plugin.getNationInviteRepository().create(
                new NationInvite(nation.getNationUuid(), selectedTown.getTownUUID(), NationInviteType.FORMATION, expiresAt)
            );
        }
        return new NationActionResult(NationActionStatus.SUCCESS, nation);
    }

    /**
     * Accepts a pending nation invite on behalf of the player's current town.
     *
     * @param player responding player
     * @param invite invite to accept
     * @return invite response result
     */
    public @NotNull NationInviteResponseResult acceptNationInvite(
        final @NotNull Player player,
        final @NotNull NationInvite invite
    ) {
        return this.respondToNationInvite(player, invite, true);
    }

    /**
     * Declines a pending nation invite on behalf of the player's current town.
     *
     * @param player responding player
     * @param invite invite to decline
     * @return invite response result
     */
    public @NotNull NationInviteResponseResult declineNationInvite(
        final @NotNull Player player,
        final @NotNull NationInvite invite
    ) {
        return this.respondToNationInvite(player, invite, false);
    }

    /**
     * Renames an active nation.
     *
     * @param player acting player
     * @param nation nation to rename
     * @param nationName requested replacement name
     * @return nation action result
     */
    public @NotNull NationActionResult renameNation(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull String nationName
    ) {
        this.processPendingNationTimeouts();
        final RNation liveNation = this.getNation(nation.getNationUuid());
        if (liveNation == null || !liveNation.isActive() || this.plugin.getNationRepository() == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, liveNation);
        }
        final RTown capitalTown = this.getTown(liveNation.getCapitalTownUuid());
        if (capitalTown == null || !this.hasTownPermission(player, capitalTown, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, liveNation);
        }
        final String normalizedNationName = nationName.trim();
        if (normalizedNationName.isEmpty() || normalizedNationName.length() > 32) {
            return new NationActionResult(NationActionStatus.INVALID_NAME, liveNation);
        }
        final RNation existingNation = this.plugin.getNationRepository().findByNationName(normalizedNationName);
        if (existingNation != null && !Objects.equals(existingNation.getNationUuid(), liveNation.getNationUuid())) {
            return new NationActionResult(NationActionStatus.NAME_TAKEN, liveNation);
        }
        liveNation.setNationName(normalizedNationName);
        this.plugin.getNationRepository().update(liveNation);
        return new NationActionResult(NationActionStatus.SUCCESS, liveNation);
    }

    /**
     * Invites one additional allied town into an active nation.
     *
     * @param player acting player
     * @param nation active nation
     * @param targetTownUuid invited town UUID
     * @return nation action result
     */
    public @NotNull NationActionResult inviteTownToNation(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull UUID targetTownUuid
    ) {
        this.processPendingNationTimeouts();
        final RNation liveNation = this.getNation(nation.getNationUuid());
        if (liveNation == null || !liveNation.isActive()) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, liveNation);
        }
        final RTown capitalTown = this.getTown(liveNation.getCapitalTownUuid());
        final RTown targetTown = this.getTown(targetTownUuid);
        if (capitalTown == null || targetTown == null || this.plugin.getNationInviteRepository() == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, liveNation);
        }
        if (!this.hasTownPermission(player, capitalTown, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, liveNation);
        }
        if (!this.getNationInvites(liveNation).stream().noneMatch(invite ->
            invite.isPending() && invite.getInviteType() == NationInviteType.EXPANSION)) {
            return new NationActionResult(NationActionStatus.ALREADY_PENDING, liveNation);
        }
        if (Objects.equals(targetTown.getTownUUID(), capitalTown.getTownUUID())
            || this.getNationForTown(targetTown) != null
            || this.getPendingNationCreatedBy(targetTown) != null
            || this.getPendingNationInviteFor(targetTown) != null
            || this.getEffectiveRelationshipState(capitalTown, targetTown) != TownRelationshipState.ALLIED) {
            return new NationActionResult(NationActionStatus.INVALID_SELECTION, liveNation);
        }

        this.plugin.getNationInviteRepository().create(
            new NationInvite(
                liveNation.getNationUuid(),
                targetTown.getTownUUID(),
                NationInviteType.EXPANSION,
                System.currentTimeMillis() + this.getTownNationInviteTimeoutMillis()
            )
        );
        return new NationActionResult(NationActionStatus.SUCCESS, liveNation);
    }

    /**
     * Promotes another member town to capital immediately.
     *
     * @param player acting player
     * @param nation active nation
     * @param targetTownUuid member town UUID to promote
     * @return nation action result
     */
    public @NotNull NationActionResult promoteNationCapital(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull UUID targetTownUuid
    ) {
        this.processPendingNationTimeouts();
        final RNation liveNation = this.getNation(nation.getNationUuid());
        final RTown capitalTown = liveNation == null ? null : this.getTown(liveNation.getCapitalTownUuid());
        final RTown targetTown = this.getTown(targetTownUuid);
        if (liveNation == null || !liveNation.isActive() || capitalTown == null || targetTown == null || this.plugin.getNationRepository() == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, liveNation);
        }
        if (!this.hasTownPermission(player, capitalTown, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, liveNation);
        }
        if (!Objects.equals(targetTown.getNationUuid(), liveNation.getNationUuid())
            || Objects.equals(targetTown.getTownUUID(), capitalTown.getTownUUID())) {
            return new NationActionResult(NationActionStatus.INVALID_SELECTION, liveNation);
        }
        liveNation.setCapitalTownUuid(targetTown.getTownUUID());
        this.plugin.getNationRepository().update(liveNation);
        return new NationActionResult(NationActionStatus.SUCCESS, liveNation);
    }

    /**
     * Removes the player's town from its active nation.
     *
     * @param player acting player
     * @return nation action result
     */
    public @NotNull NationActionResult leaveNation(final @NotNull Player player) {
        this.processPendingNationTimeouts();
        final RTown town = this.getTownFor(player.getUniqueId());
        final RNation nation = town == null ? null : this.getNationForTown(town);
        if (town == null || nation == null || this.plugin.getNationRepository() == null || this.plugin.getTownRepository() == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, nation);
        }
        if (!this.hasTownPermission(player, town, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, nation);
        }
        if (Objects.equals(nation.getCapitalTownUuid(), town.getTownUUID())) {
            return new NationActionResult(NationActionStatus.CAPITAL_REQUIRED, nation);
        }

        town.setNationUuid(null);
        this.plugin.getTownRepository().update(town);
        this.enforceNationMinimumOrDisband(nation);
        return new NationActionResult(NationActionStatus.SUCCESS, this.getNation(nation.getNationUuid()));
    }

    /**
     * Kicks one member town from an active nation.
     *
     * @param player acting player
     * @param nation active nation
     * @param targetTownUuid member town UUID to remove
     * @return nation action result
     */
    public @NotNull NationActionResult kickTownFromNation(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull UUID targetTownUuid
    ) {
        this.processPendingNationTimeouts();
        final RNation liveNation = this.getNation(nation.getNationUuid());
        final RTown capitalTown = liveNation == null ? null : this.getTown(liveNation.getCapitalTownUuid());
        final RTown targetTown = this.getTown(targetTownUuid);
        if (liveNation == null || !liveNation.isActive() || capitalTown == null || targetTown == null || this.plugin.getTownRepository() == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, liveNation);
        }
        if (!this.hasTownPermission(player, capitalTown, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, liveNation);
        }
        if (!Objects.equals(targetTown.getNationUuid(), liveNation.getNationUuid())) {
            return new NationActionResult(NationActionStatus.INVALID_SELECTION, liveNation);
        }
        if (Objects.equals(targetTown.getTownUUID(), liveNation.getCapitalTownUuid())) {
            return new NationActionResult(NationActionStatus.CAPITAL_REQUIRED, liveNation);
        }

        targetTown.setNationUuid(null);
        this.plugin.getTownRepository().update(targetTown);
        this.enforceNationMinimumOrDisband(liveNation);
        return new NationActionResult(NationActionStatus.SUCCESS, this.getNation(liveNation.getNationUuid()));
    }

    /**
     * Disbands one active nation from the capital town.
     *
     * @param player acting player
     * @param nation active nation
     * @return nation action result
     */
    public @NotNull NationActionResult disbandNation(
        final @NotNull Player player,
        final @NotNull RNation nation
    ) {
        this.processPendingNationTimeouts();
        final RNation liveNation = this.getNation(nation.getNationUuid());
        final RTown capitalTown = liveNation == null ? null : this.getTown(liveNation.getCapitalTownUuid());
        if (liveNation == null || capitalTown == null || this.plugin.getNationRepository() == null || this.plugin.getTownRepository() == null) {
            return new NationActionResult(NationActionStatus.INVALID_TARGET, liveNation);
        }
        if (!this.hasTownPermission(player, capitalTown, TownPermissions.MANAGE_NATIONS)) {
            return new NationActionResult(NationActionStatus.NO_PERMISSION, liveNation);
        }

        this.disbandNationInternal(liveNation);
        return new NationActionResult(NationActionStatus.SUCCESS, this.getNation(liveNation.getNationUuid()));
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
     * Resolves the live FOB chunk for one town.
     *
     * @param town town to inspect
     * @return FOB chunk, or {@code null} when none exists
     */
    public @Nullable RTownChunk getFobChunk(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        final RTown resolvedTown = liveTown == null ? town : liveTown;
        return resolvedTown.findFobChunk();
    }

    /**
     * Returns whether one town currently owns a FOB chunk.
     *
     * @param town town to inspect
     * @return {@code true} when the town has a FOB chunk
     */
    public boolean hasFobChunk(final @NotNull RTown town) {
        return this.getFobChunk(town) != null;
    }

    /**
     * Resolves the local teleport target for one town's FOB marker.
     *
     * @param town town to inspect
     * @return FOB teleport target, or {@code null} when the marker is unavailable
     */
    public @Nullable Location resolveFobTeleportLocation(final @NotNull RTown town) {
        final RTownChunk fobChunk = this.getFobChunk(town);
        final Location markerLocation = fobChunk == null ? null : fobChunk.getChunkBlockLocation();
        return markerLocation == null || markerLocation.getWorld() == null
            ? null
            : markerLocation.clone().add(0.5D, 1.0D, 0.5D);
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
     * Returns the configured Nexus level required before diplomacy unlocks.
     *
     * @return required Nexus level for town relationships
     */
    public int getTownRelationshipUnlockLevel() {
        return this.plugin.getDefaultConfig().getTownRelationshipUnlockLevel();
    }

    /**
     * Returns the configured cooldown in milliseconds between confirmed relationship changes for
     * one town pair.
     *
     * @return relationship-change cooldown in milliseconds
     */
    public long getTownRelationshipChangeCooldownMillis() {
        return Math.max(0L, this.plugin.getDefaultConfig().getTownRelationshipChangeCooldownSeconds()) * 1000L;
    }

    /**
     * Returns whether one town has unlocked diplomacy based on its current Nexus level.
     *
     * @param town town to inspect
     * @return {@code true} when diplomacy is unlocked for the town
     */
    public boolean areTownRelationshipsUnlocked(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        return (liveTown == null ? town : liveTown).getNexusLevel() >= this.getTownRelationshipUnlockLevel();
    }

    /**
     * Returns one derived diplomacy snapshot for a source town and target town.
     *
     * @param sourceTown viewing source town
     * @param targetTown target town
     * @return immutable relationship snapshot for the pair
     */
    public @NotNull TownRelationshipViewEntry getTownRelationshipViewEntry(
        final @NotNull RTown sourceTown,
        final @NotNull RTown targetTown
    ) {
        final RTown liveSourceTown = this.resolveLiveTown(sourceTown);
        final RTown liveTargetTown = this.resolveLiveTown(targetTown);
        final RTown resolvedSourceTown = liveSourceTown == null ? sourceTown : liveSourceTown;
        final RTown resolvedTargetTown = liveTargetTown == null ? targetTown : liveTargetTown;
        return this.buildTownRelationshipViewEntry(
            resolvedSourceTown,
            resolvedTargetTown,
            this.getStoredTownRelationship(resolvedSourceTown, resolvedTargetTown)
        );
    }

    /**
     * Returns diplomacy snapshots for every town other than the supplied source town.
     *
     * @param sourceTown viewing source town
     * @return immutable sorted list of relationship snapshots
     */
    public @NotNull List<TownRelationshipViewEntry> getTownRelationshipViewEntries(final @NotNull RTown sourceTown) {
        final RTown liveSourceTown = this.resolveLiveTown(sourceTown);
        final RTown resolvedSourceTown = liveSourceTown == null ? sourceTown : liveSourceTown;
        final Map<String, RTownRelationship> relationshipsByPair = new HashMap<>();
        if (this.plugin.getTownRelationshipRepository() != null) {
            for (final RTownRelationship relationship : this.plugin.getTownRelationshipRepository().findByTownUuid(
                resolvedSourceTown.getTownUUID()
            )) {
                relationshipsByPair.put(relationship.getPairKey(), relationship);
            }
        }

        final List<TownRelationshipViewEntry> entries = new ArrayList<>();
        for (final RTown targetTown : this.getTowns()) {
            if (Objects.equals(targetTown.getTownUUID(), resolvedSourceTown.getTownUUID())) {
                continue;
            }
            entries.add(this.buildTownRelationshipViewEntry(
                resolvedSourceTown,
                targetTown,
                relationshipsByPair.get(RTownRelationship.buildPairKey(
                    resolvedSourceTown.getTownUUID(),
                    targetTown.getTownUUID()
                ))
            ));
        }
        return List.copyOf(entries);
    }

    /**
     * Attempts to change the diplomacy state between two towns.
     *
     * @param sourceTown town requesting the change
     * @param targetTown target town
     * @param requestedState requested diplomacy state
     * @return structured change result describing the updated relationship snapshot
     */
    public @NotNull TownRelationshipChangeResult changeTownRelationship(
        final @NotNull RTown sourceTown,
        final @NotNull RTown targetTown,
        final @NotNull TownRelationshipState requestedState
    ) {
        if (this.plugin.getTownRelationshipRepository() == null) {
            return new TownRelationshipChangeResult(
                TownRelationshipChangeStatus.FAILED,
                this.getTownRelationshipViewEntry(sourceTown, targetTown),
                requestedState
            );
        }

        final RTown liveSourceTown = this.resolveLiveTown(sourceTown);
        final RTown liveTargetTown = this.resolveLiveTown(targetTown);
        final RTown resolvedSourceTown = liveSourceTown == null ? sourceTown : liveSourceTown;
        final RTown resolvedTargetTown = liveTargetTown == null ? targetTown : liveTargetTown;
        if (Objects.equals(resolvedSourceTown.getTownUUID(), resolvedTargetTown.getTownUUID())) {
            return new TownRelationshipChangeResult(
                TownRelationshipChangeStatus.FAILED,
                this.buildTownRelationshipViewEntry(resolvedSourceTown, resolvedTargetTown, null),
                requestedState
            );
        }
        if (resolvedSourceTown.getNationUuid() != null
            && Objects.equals(resolvedSourceTown.getNationUuid(), resolvedTargetTown.getNationUuid())
            && this.getNation(resolvedSourceTown.getNationUuid()) != null
            && this.getNation(resolvedSourceTown.getNationUuid()).isActive()
            && requestedState != TownRelationshipState.ALLIED) {
            return new TownRelationshipChangeResult(
                TownRelationshipChangeStatus.LOCKED,
                this.buildTownRelationshipViewEntry(
                    resolvedSourceTown,
                    resolvedTargetTown,
                    this.getStoredTownRelationship(resolvedSourceTown, resolvedTargetTown)
                ),
                requestedState
            );
        }

        RTownRelationship relationship = this.getStoredTownRelationship(resolvedSourceTown, resolvedTargetTown);
        final TownRelationshipViewEntry currentEntry = this.buildTownRelationshipViewEntry(
            resolvedSourceTown,
            resolvedTargetTown,
            relationship
        );
        if (currentEntry.lockedByLevel()) {
            return new TownRelationshipChangeResult(TownRelationshipChangeStatus.LOCKED, currentEntry, requestedState);
        }
        if (currentEntry.cooldownRemainingMillis() > 0L) {
            return new TownRelationshipChangeResult(TownRelationshipChangeStatus.COOLDOWN, currentEntry, requestedState);
        }

        final TownRelationshipState confirmedState = relationship == null
            ? TownRelationshipState.NEUTRAL
            : relationship.getConfirmedState();
        final TownRelationshipState pendingState = relationship == null ? null : relationship.getPendingState();
        final UUID sourceTownUuid = resolvedSourceTown.getTownUUID();
        final UUID pendingRequesterTownUuid = relationship == null ? null : relationship.getPendingRequesterTownUuid();

        if (requestedState == TownRelationshipState.HOSTILE) {
            if (confirmedState == TownRelationshipState.HOSTILE) {
                if (relationship != null && relationship.getPendingState() != null) {
                    relationship.clearPendingState();
                    this.persistTownRelationship(relationship);
                }
                return new TownRelationshipChangeResult(
                    TownRelationshipChangeStatus.UNCHANGED,
                    this.buildTownRelationshipViewEntry(resolvedSourceTown, resolvedTargetTown, relationship),
                    requestedState
                );
            }

            relationship = relationship == null
                ? new RTownRelationship(resolvedSourceTown.getTownUUID(), resolvedTargetTown.getTownUUID())
                : relationship;
            relationship.setConfirmedState(TownRelationshipState.HOSTILE);
            relationship.clearPendingState();
            relationship.setCooldownUntilMillis(System.currentTimeMillis() + this.getTownRelationshipChangeCooldownMillis());
            this.persistTownRelationship(relationship);
            this.broadcastConfirmedTownRelationshipChange(
                resolvedSourceTown,
                resolvedTargetTown,
                TownRelationshipState.HOSTILE
            );
            return new TownRelationshipChangeResult(
                TownRelationshipChangeStatus.CONFIRMED,
                this.buildTownRelationshipViewEntry(resolvedSourceTown, resolvedTargetTown, relationship),
                requestedState
            );
        }

        if (confirmedState == requestedState) {
            return new TownRelationshipChangeResult(TownRelationshipChangeStatus.UNCHANGED, currentEntry, requestedState);
        }

        if (pendingState == requestedState) {
            if (!Objects.equals(pendingRequesterTownUuid, sourceTownUuid) && relationship != null) {
                relationship.setConfirmedState(requestedState);
                relationship.clearPendingState();
                relationship.setCooldownUntilMillis(System.currentTimeMillis() + this.getTownRelationshipChangeCooldownMillis());
                this.persistTownRelationship(relationship);
                this.broadcastConfirmedTownRelationshipChange(resolvedSourceTown, resolvedTargetTown, requestedState);
                return new TownRelationshipChangeResult(
                    TownRelationshipChangeStatus.CONFIRMED,
                    this.buildTownRelationshipViewEntry(resolvedSourceTown, resolvedTargetTown, relationship),
                    requestedState
                );
            }
            return new TownRelationshipChangeResult(TownRelationshipChangeStatus.UNCHANGED, currentEntry, requestedState);
        }

        relationship = relationship == null
            ? new RTownRelationship(resolvedSourceTown.getTownUUID(), resolvedTargetTown.getTownUUID())
            : relationship;
        relationship.setPendingState(requestedState);
        relationship.setPendingRequesterTownUuid(sourceTownUuid);
        this.persistTownRelationship(relationship);
        this.broadcastPendingTownRelationshipRequest(resolvedSourceTown, resolvedTargetTown, requestedState);
        return new TownRelationshipChangeResult(
            TownRelationshipChangeStatus.PENDING,
            this.buildTownRelationshipViewEntry(resolvedSourceTown, resolvedTargetTown, relationship),
            requestedState
        );
    }

    /**
     * Backfills the persisted Nexus level when needed and refreshes the cached composite town level.
     *
     * @param town live town entity to refresh
     * @return {@code true} when the town state changed and was persisted
     */
    public boolean ensureCompositeTownState(final @NotNull RTown town) {
        final boolean backfilled = town.backfillLegacyNexusLevelIfNeeded();
        final int previousTownLevel = town.getTownLevel();
        final int recalculatedTownLevel = town.recalculateTownLevel();
        final boolean combatStateChanged = this.ensureNexusCombatState(town);
        final boolean changed = backfilled || previousTownLevel != recalculatedTownLevel || combatStateChanged;
        if (changed && this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(town);
        }
        return changed;
    }

    private void ensureNationProgressState(final @NotNull RNation nation) {
        final boolean backfilled = nation.backfillLegacyNationLevelIfNeeded();
        if (backfilled && this.plugin.getNationRepository() != null) {
            this.plugin.getNationRepository().update(nation);
        }
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
        this.healTownNexusToFull(town);

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
        return this.claimChunk(
            player,
            blockLocation,
            townUuid,
            targetWorldName,
            targetChunkX,
            targetChunkZ,
            ChunkType.DEFAULT
        );
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
     * @param initialChunkType initial chunk type requested by the claim item
     * @return created town chunk, or {@code null} when validation failed
     */
    public @Nullable RTownChunk claimChunk(
        final @NotNull Player player,
        final @NotNull Location blockLocation,
        final @NotNull UUID townUuid,
        final @NotNull String targetWorldName,
        final int targetChunkX,
        final int targetChunkZ,
        final @Nullable ChunkType initialChunkType
    ) {
        if (blockLocation.getWorld() == null) {
            return null;
        }

        final ChunkType resolvedInitialChunkType = initialChunkType == ChunkType.FOB ? ChunkType.FOB : ChunkType.DEFAULT;
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
            || this.getChunk(targetWorldName, targetChunkX, targetChunkZ) != null
            || !this.isChunkClaimable(town, targetWorldName, targetChunkX, targetChunkZ, resolvedInitialChunkType)) {
            return null;
        }

        final Location nexusLocation = town.getNexusLocation();
        if (resolvedInitialChunkType != ChunkType.FOB && nexusLocation != null) {
            final int deltaY = blockLocation.getBlockY() - nexusLocation.getBlockY();
            if (deltaY < this.plugin.getDefaultConfig().getChunkBlockMinY()
                || deltaY > this.plugin.getDefaultConfig().getChunkBlockMaxY()) {
                return null;
            }
        }

        final RTownChunk chunk = new RTownChunk(town, targetWorldName, targetChunkX, targetChunkZ, resolvedInitialChunkType);
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
        return this.isChunkClaimable(town, worldName, chunkX, chunkZ, ChunkType.DEFAULT);
    }

    /**
     * Returns whether the supplied chunk is claimable for the requested initial type.
     *
     * @param town town to inspect
     * @param worldName target world
     * @param chunkX target chunk X
     * @param chunkZ target chunk Z
     * @param initialChunkType initial chunk type being requested
     * @return {@code true} when the target chunk is claimable
     */
    public boolean isChunkClaimable(
        final @NotNull RTown town,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final @Nullable ChunkType initialChunkType
    ) {
        final ChunkType resolvedInitialChunkType = initialChunkType == ChunkType.FOB ? ChunkType.FOB : ChunkType.DEFAULT;
        if (town.getChunks().size() >= this.plugin.getDefaultConfig().getGlobalMaxChunkLimit()) {
            return false;
        }
        if (town.findChunk(worldName, chunkX, chunkZ) != null || this.getChunk(worldName, chunkX, chunkZ) != null) {
            return false;
        }
        if (resolvedInitialChunkType == ChunkType.FOB) {
            return !town.hasFobChunk();
        }
        if (town.getChunks().isEmpty()) {
            return true;
        }

        for (final RTownChunk claimedChunk : town.getChunks()) {
            if (!claimedChunk.getWorldName().equalsIgnoreCase(worldName)) {
                continue;
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
        this.healTownNexusToFull(liveTown);
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
     * Returns the live active nation progression snapshot for the supplied town.
     *
     * @param player viewing player
     * @param town town to inspect
     * @return current active nation progression snapshot
     */
    public @NotNull LevelProgressSnapshot getNationLevelProgress(
        final @NotNull Player player,
        final @NotNull RTown town
    ) {
        return this.getLevelProgress(player, LevelScope.NATION, town, null, null);
    }

    /**
     * Returns a preview snapshot for one configured active nation level.
     *
     * @param player viewing player
     * @param town town to inspect
     * @param previewLevel configured level to preview
     * @return preview progression snapshot
     */
    public @NotNull LevelProgressSnapshot getNationLevelProgress(
        final @NotNull Player player,
        final @NotNull RTown town,
        final int previewLevel
    ) {
        return this.getLevelProgress(player, LevelScope.NATION, town, null, previewLevel);
    }

    /**
     * Saves a shared currency contribution against the active nation's current target level.
     *
     * @param player contributing player
     * @param town player's current nation-member town
     * @param entryKey requirement entry key to contribute toward
     * @param requestedAmount requested contribution amount
     * @return contribution result
     */
    public @NotNull ContributionResult contributeNationCurrency(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull String entryKey,
        final double requestedAmount
    ) {
        return this.contributeCurrency(player, LevelScope.NATION, town, null, entryKey, requestedAmount);
    }

    /**
     * Saves a shared item contribution against the active nation's current target level.
     *
     * @param player contributing player
     * @param town player's current nation-member town
     * @param entryKey requirement entry key to contribute toward
     * @return contribution result
     */
    public @NotNull ContributionResult contributeNationItem(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull String entryKey
    ) {
        return this.contributeItem(player, LevelScope.NATION, town, null, entryKey);
    }

    /**
     * Finalizes the next active nation level when every stored and live requirement is complete.
     *
     * @param player player performing the final level-up action
     * @param town player's current nation-member town
     * @return level-up result
     */
    public @NotNull LevelUpResult levelUpNation(final @NotNull Player player, final @NotNull RTown town) {
        return this.levelUp(player, LevelScope.NATION, town, null);
    }

    /**
     * Returns the current Nexus combat state for one town.
     *
     * <p>This accessor normalizes legacy persisted health before returning the snapshot.</p>
     *
     * @param town town whose Nexus combat state should be resolved
     * @return current Nexus combat snapshot
     */
    public @NotNull NexusCombatSnapshot getNexusCombatSnapshot(final @NotNull RTown town) {
        final RTown liveTown = this.resolveLiveTown(town);
        this.ensureCompositeTownState(liveTown);
        final NexusCombatStats combatStats = this.resolveNexusCombatStats(liveTown.getNexusLevel());
        return new NexusCombatSnapshot(
            liveTown.getNexusLevel(),
            liveTown.getCurrentNexusHealth(combatStats.maxHealth()),
            combatStats.maxHealth(),
            combatStats.defense()
        );
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
        if (chunkType == ChunkType.NEXUS || chunkType == ChunkType.FOB || this.plugin.getTownRepository() == null) {
            return ChunkTypeChangeResult.failure();
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || liveTownChunk.getChunkType() == ChunkType.FOB || liveTownChunk.getChunkType() == chunkType) {
            return ChunkTypeChangeResult.failure();
        }

        final ChunkType previousType = liveTownChunk.getChunkType();
        final boolean enteringSecurity = previousType != ChunkType.SECURITY && chunkType == ChunkType.SECURITY;
        final boolean leavingSecurity = previousType == ChunkType.SECURITY && chunkType != ChunkType.SECURITY;
        final boolean enteringBank = previousType != ChunkType.BANK && chunkType == ChunkType.BANK;
        final boolean leavingBank = previousType == ChunkType.BANK && chunkType != ChunkType.BANK;
        final boolean enteringFarm = previousType != ChunkType.FARM && chunkType == ChunkType.FARM;
        final boolean leavingFarm = previousType == ChunkType.FARM && chunkType != ChunkType.FARM;
        final boolean enteringArmory = previousType != ChunkType.ARMORY && chunkType == ChunkType.ARMORY;
        final boolean leavingArmory = previousType == ChunkType.ARMORY && chunkType != ChunkType.ARMORY;
        final FuelTankRemoval fuelTankRemoval = leavingSecurity
            ? this.removeFuelTankInternal(liveTownChunk, null, false)
            : FuelTankRemoval.none();
        final SeedBoxRemoval seedBoxRemoval = leavingFarm
            ? this.removeSeedBoxInternal(liveTownChunk, null, false)
            : SeedBoxRemoval.none();
        final SalvageBlockRemoval salvageBlockRemoval = leavingArmory
            ? this.removeSalvageBlockInternal(liveTownChunk, null, false)
            : SalvageBlockRemoval.none();
        final RepairBlockRemoval repairBlockRemoval = leavingArmory
            ? this.removeRepairBlockInternal(liveTownChunk, null, false)
            : RepairBlockRemoval.none();
        if (leavingBank && this.plugin.getTownBankService() != null) {
            this.plugin.getTownBankService().clearCacheForHostLoss(liveTownChunk);
        }

        liveTownChunk.setChunkType(chunkType);
        if (this.plugin.getDefaultConfig().isChunkTypeResetOnChange()) {
            liveTownChunk.resetChunkTypeState();
        } else if (leavingSecurity) {
            liveTownChunk.clearFuelTankState();
        } else if (leavingFarm) {
            liveTownChunk.clearFarmState();
        } else if (leavingArmory) {
            liveTownChunk.clearArmoryState();
        }
        this.initializeFarmEnhancementDefaults(liveTownChunk);
        this.initializeArmoryEnhancementDefaults(liveTownChunk);
        this.syncChunkMarkerMaterial(liveTownChunk);

        final boolean fuelTankGranted = enteringSecurity && actor != null && this.giveFuelTank(actor, liveTownChunk);
        final boolean seedBoxGranted = enteringFarm
            && actor != null
            && this.shouldGrantSeedBoxAtLevel(liveTownChunk.getChunkLevel())
            && this.giveSeedBox(actor, liveTownChunk);
        final boolean salvageBlockGranted = enteringArmory
            && actor != null
            && this.shouldGrantSalvageBlockAtLevel(liveTownChunk.getChunkLevel())
            && this.giveSalvageBlock(actor, liveTownChunk);
        final boolean repairBlockGranted = enteringArmory
            && actor != null
            && this.shouldGrantRepairBlockAtLevel(liveTownChunk.getChunkLevel())
            && this.giveRepairBlock(actor, liveTownChunk);
        if (enteringBank
            && actor != null
            && this.plugin.getTownBankService() != null
            && this.plugin.getTownBankService().shouldGrantCacheChestOnBankEntry(liveTownChunk.getTown(), liveTownChunk.getChunkLevel())) {
            this.plugin.getTownBankService().giveCacheChest(actor, liveTownChunk.getTown());
        }
        this.ensureCompositeTownState(liveTownChunk.getTown());
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        if (chunkType == ChunkType.OUTPOST) {
            this.syncRdsTownOutpost(liveTownChunk);
        } else if (previousType == ChunkType.OUTPOST) {
            this.removeRdsTownOutpost(liveTownChunk.getIdentifier());
        }
        return new ChunkTypeChangeResult(
            true,
            fuelTankGranted,
            fuelTankRemoval.removed(),
            fuelTankRemoval.droppedFuel(),
            seedBoxGranted,
            seedBoxRemoval.removed(),
            seedBoxRemoval.droppedSeeds(),
            salvageBlockGranted,
            salvageBlockRemoval.removed(),
            repairBlockGranted,
            repairBlockRemoval.removed()
        );
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
        this.removeSeedBoxInternal(liveTownChunk, null, false);
        this.removeSalvageBlockInternal(liveTownChunk, null, false);
        this.removeRepairBlockInternal(liveTownChunk, null, false);
        if (liveTownChunk.getChunkType() == ChunkType.BANK && this.plugin.getTownBankService() != null) {
            this.plugin.getTownBankService().clearCacheForHostLoss(liveTownChunk);
        }
        final boolean removingOutpost = liveTownChunk.getChunkType() == ChunkType.OUTPOST;
        final UUID removedChunkUuid = liveTownChunk.getIdentifier();
        final RTown town = liveTownChunk.getTown();
        final boolean removed = town.removeChunk(liveTownChunk);
        if (removed) {
            this.plugin.getTownRepository().update(town);
            if (removingOutpost) {
                this.removeRdsTownOutpost(removedChunkUuid);
            }
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
     * Returns whether a player has one town management permission for a specific town.
     *
     * @param player player to inspect
     * @param town town to validate ownership against
     * @param permission management permission to resolve
     * @return {@code true} when the player belongs to the supplied town and holds the permission
     */
    public boolean hasTownPermission(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull TownPermissions permission
    ) {
        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        return playerData != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(permission);
    }

    /**
     * Returns whether a player has a specific town management permission key.
     *
     * @param player player to inspect
     * @param permissionKey management permission key to resolve
     * @return {@code true} when the player holds the permission
     */
    public boolean hasTownPermission(final @NotNull Player player, final @NotNull String permissionKey) {
        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        return playerData != null && playerData.hasTownPermission(permissionKey);
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
     * Updates one town-global allied-access rule.
     *
     * @param town town to update
     * @param protection protection to update
     * @param allowed replacement allied-access state
     * @return {@code true} when the live town snapshot was updated
     */
    public boolean setTownAlliedProtectionAllowed(
        final @NotNull RTown town,
        final @NotNull TownProtections protection,
        final boolean allowed
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTown liveTown = this.resolveLiveTown(town);
        if (liveTown == null) {
            return false;
        }
        liveTown.setAlliedProtectionAllowed(protection, allowed);
        this.plugin.getTownRepository().update(liveTown);
        return true;
    }

    /**
     * Updates multiple town-global allied-access rules in one repository write.
     *
     * @param town town to update
     * @param protections protections to update
     * @param allowed replacement allied-access state for every supplied protection
     * @return {@code true} when the live town snapshot was updated
     */
    public boolean setTownAlliedProtectionAllowed(
        final @NotNull RTown town,
        final @NotNull Collection<TownProtections> protections,
        final boolean allowed
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
            liveTown.setAlliedProtectionAllowed(protection, allowed);
        }
        this.plugin.getTownRepository().update(liveTown);
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
     * Updates one chunk-specific allied-access override.
     *
     * @param townChunk chunk to update
     * @param protection protection to update
     * @param allowed replacement allied-access override, or {@code null} to inherit
     * @return {@code true} when the live chunk snapshot was updated
     */
    public boolean setChunkAlliedProtectionAllowed(
        final @NotNull RTownChunk townChunk,
        final @NotNull TownProtections protection,
        final @Nullable Boolean allowed
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }
        liveTownChunk.setAlliedProtectionAllowed(protection, allowed);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Updates multiple chunk-specific allied-access overrides in one repository write.
     *
     * @param townChunk chunk to update
     * @param protections protections to update
     * @param allowed replacement allied-access override, or {@code null} to clear every supplied
     *     override
     * @return {@code true} when the live chunk snapshot was updated
     */
    public boolean setChunkAlliedProtectionAllowed(
        final @NotNull RTownChunk townChunk,
        final @NotNull Collection<TownProtections> protections,
        final @Nullable Boolean allowed
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
            liveTownChunk.setAlliedProtectionAllowed(protection, allowed);
        }
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
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

    private boolean ensureNexusCombatState(final @NotNull RTown town) {
        if (town.getId() == null && !town.hasPersistedCurrentNexusHealth()) {
            return false;
        }
        final NexusCombatStats combatStats = this.resolveNexusCombatStats(town.getNexusLevel());
        final boolean backfilled = town.getId() != null
            && town.backfillLegacyCurrentNexusHealthIfNeeded(combatStats.maxHealth());
        final boolean clamped = town.clampCurrentNexusHealth(combatStats.maxHealth());
        return backfilled || clamped;
    }

    private @NotNull NexusCombatStats resolveNexusCombatStats(final int nexusLevel) {
        return this.plugin.getNexusConfig().getCombatStats(nexusLevel);
    }

    private void healTownNexusToFull(final @NotNull RTown town) {
        final NexusCombatStats combatStats = this.resolveNexusCombatStats(town.getNexusLevel());
        town.healNexusToFull(combatStats.maxHealth());
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

    private void syncChunkMarkerMaterial(final @NotNull RTownChunk townChunk) {
        final Location markerLocation = townChunk.getChunkBlockLocation();
        if (markerLocation == null || markerLocation.getWorld() == null) {
            return;
        }
        final World markerWorld = markerLocation.getWorld();
        if (!markerWorld.isChunkLoaded(
            toChunkCoordinate(markerLocation.getBlockX()),
            toChunkCoordinate(markerLocation.getBlockZ())
        )) {
            return;
        }
        markerLocation.getBlock().setType(this.plugin.getChunkTypeDisplayMaterial(townChunk.getChunkType()), false);
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
     * Returns the Farm chunk whose seed box is placed at the supplied exact location.
     *
     * @param location block location to inspect
     * @return owning Farm chunk, or {@code null} when the location is not a seed box
     */
    public @Nullable RTownChunk findSeedBoxChunk(final @Nullable Location location) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (location == null || townChunk == null || townChunk.getSeedBoxLocation() == null) {
            return null;
        }
        return sameBlock(location, townChunk.getSeedBoxLocation()) ? townChunk : null;
    }

    /**
     * Returns the Armory chunk whose salvage block is placed at the supplied exact location.
     *
     * @param location block location to inspect
     * @return owning Armory chunk, or {@code null} when the location is not a salvage block
     */
    public @Nullable RTownChunk findSalvageBlockChunk(final @Nullable Location location) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (location == null || townChunk == null || townChunk.getSalvageBlockLocation() == null) {
            return null;
        }
        return sameBlock(location, townChunk.getSalvageBlockLocation()) ? townChunk : null;
    }

    /**
     * Returns the Armory chunk whose repair block is placed at the supplied exact location.
     *
     * @param location block location to inspect
     * @return owning Armory chunk, or {@code null} when the location is not a repair block
     */
    public @Nullable RTownChunk findRepairBlockChunk(final @Nullable Location location) {
        final RTownChunk townChunk = this.getChunkAt(location);
        if (location == null || townChunk == null || townChunk.getRepairBlockLocation() == null) {
            return null;
        }
        return sameBlock(location, townChunk.getRepairBlockLocation()) ? townChunk : null;
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
     * Returns whether one seed-box placement location is valid for a Farm chunk.
     *
     * @param townChunk owning Farm chunk
     * @param seedBoxLocation requested seed-box placement location
     * @return {@code true} when the placement is inside the owning Farm chunk and within radius
     */
    public boolean isValidSeedBoxPlacement(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location seedBoxLocation
    ) {
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.FARM
            || liveTownChunk.hasSeedBox()
            || seedBoxLocation.getWorld() == null) {
            return false;
        }

        if (!Objects.equals(liveTownChunk.getWorldName(), seedBoxLocation.getWorld().getName())
            || toChunkCoordinate(seedBoxLocation.getBlockX()) != liveTownChunk.getX()
            || toChunkCoordinate(seedBoxLocation.getBlockZ()) != liveTownChunk.getZ()) {
            return false;
        }

        final Location chunkMarkerLocation = liveTownChunk.getChunkBlockLocation();
        if (chunkMarkerLocation == null
            || chunkMarkerLocation.getWorld() == null
            || !Objects.equals(chunkMarkerLocation.getWorld().getName(), seedBoxLocation.getWorld().getName())) {
            return false;
        }

        final int radius = this.plugin.getFarmConfig().getSeedBox().placementRadiusBlocks();
        return chunkMarkerLocation.distanceSquared(seedBoxLocation) <= ((double) radius * (double) radius);
    }

    /**
     * Returns whether one salvage-block placement location is valid for an Armory chunk.
     *
     * @param townChunk owning Armory chunk
     * @param salvageBlockLocation requested salvage-block placement location
     * @return {@code true} when the placement is inside the owning Armory chunk and within radius
     */
    public boolean isValidSalvageBlockPlacement(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location salvageBlockLocation
    ) {
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.ARMORY
            || liveTownChunk.hasSalvageBlock()
            || salvageBlockLocation.getWorld() == null) {
            return false;
        }

        if (!Objects.equals(liveTownChunk.getWorldName(), salvageBlockLocation.getWorld().getName())
            || toChunkCoordinate(salvageBlockLocation.getBlockX()) != liveTownChunk.getX()
            || toChunkCoordinate(salvageBlockLocation.getBlockZ()) != liveTownChunk.getZ()) {
            return false;
        }

        final Location repairBlockLocation = liveTownChunk.getRepairBlockLocation();
        if (repairBlockLocation != null && sameBlock(salvageBlockLocation, repairBlockLocation)) {
            return false;
        }

        final Location chunkMarkerLocation = liveTownChunk.getChunkBlockLocation();
        if (chunkMarkerLocation == null
            || chunkMarkerLocation.getWorld() == null
            || !Objects.equals(chunkMarkerLocation.getWorld().getName(), salvageBlockLocation.getWorld().getName())) {
            return false;
        }

        final int radius = this.plugin.getArmoryConfig().getSalvageBlock().placementRadiusBlocks();
        return chunkMarkerLocation.distanceSquared(salvageBlockLocation) <= ((double) radius * (double) radius);
    }

    /**
     * Returns whether one repair-block placement location is valid for an Armory chunk.
     *
     * @param townChunk owning Armory chunk
     * @param repairBlockLocation requested repair-block placement location
     * @return {@code true} when the placement is inside the owning Armory chunk and within radius
     */
    public boolean isValidRepairBlockPlacement(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location repairBlockLocation
    ) {
        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.ARMORY
            || liveTownChunk.hasRepairBlock()
            || repairBlockLocation.getWorld() == null) {
            return false;
        }

        if (!Objects.equals(liveTownChunk.getWorldName(), repairBlockLocation.getWorld().getName())
            || toChunkCoordinate(repairBlockLocation.getBlockX()) != liveTownChunk.getX()
            || toChunkCoordinate(repairBlockLocation.getBlockZ()) != liveTownChunk.getZ()) {
            return false;
        }

        final Location salvageBlockLocation = liveTownChunk.getSalvageBlockLocation();
        if (salvageBlockLocation != null && sameBlock(repairBlockLocation, salvageBlockLocation)) {
            return false;
        }

        final Location chunkMarkerLocation = liveTownChunk.getChunkBlockLocation();
        if (chunkMarkerLocation == null
            || chunkMarkerLocation.getWorld() == null
            || !Objects.equals(chunkMarkerLocation.getWorld().getName(), repairBlockLocation.getWorld().getName())) {
            return false;
        }

        final int radius = this.plugin.getArmoryConfig().getRepairBlock().placementRadiusBlocks();
        return chunkMarkerLocation.distanceSquared(repairBlockLocation) <= ((double) radius * (double) radius);
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
     * Persists a placed seed box for one Farm chunk.
     *
     * @param townChunk owning Farm chunk
     * @param seedBoxLocation placed chest location
     * @param seedBoxContents initial persisted seed-box contents
     * @return {@code true} when the seed-box state was saved
     */
    public boolean registerSeedBox(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location seedBoxLocation,
        final @NotNull Map<String, ItemStack> seedBoxContents
    ) {
        if (this.plugin.getTownRepository() == null || !this.isValidSeedBoxPlacement(townChunk, seedBoxLocation)) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }

        liveTownChunk.setSeedBoxLocation(seedBoxLocation);
        liveTownChunk.setSeedBoxContents(this.filterConfiguredSeedContents(seedBoxContents));
        if (this.plugin.getTownFarmService() != null) {
            this.plugin.getTownFarmService().syncLiveSeedBoxInventory(liveTownChunk);
        }
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Persists a placed salvage block for one Armory chunk.
     *
     * @param townChunk owning Armory chunk
     * @param salvageBlockLocation placed block location
     * @return {@code true} when the salvage-block state was saved
     */
    public boolean registerSalvageBlock(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location salvageBlockLocation
    ) {
        if (this.plugin.getTownRepository() == null || !this.isValidSalvageBlockPlacement(townChunk, salvageBlockLocation)) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }

        liveTownChunk.setSalvageBlockLocation(salvageBlockLocation);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Persists a placed repair block for one Armory chunk.
     *
     * @param townChunk owning Armory chunk
     * @param repairBlockLocation placed block location
     * @return {@code true} when the repair-block state was saved
     */
    public boolean registerRepairBlock(
        final @NotNull RTownChunk townChunk,
        final @NotNull Location repairBlockLocation
    ) {
        if (this.plugin.getTownRepository() == null || !this.isValidRepairBlockPlacement(townChunk, repairBlockLocation)) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null) {
            return false;
        }

        liveTownChunk.setRepairBlockLocation(repairBlockLocation);
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
     * Persists the current contents of one placed Farm seed box.
     *
     * @param townChunk owning Farm chunk
     * @param seedBoxContents replacement seed-box contents
     * @return {@code true} when the contents were saved
     */
    public boolean syncSeedBoxContents(
        final @NotNull RTownChunk townChunk,
        final @NotNull Map<String, ItemStack> seedBoxContents
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || !liveTownChunk.hasSeedBox()) {
            return false;
        }

        liveTownChunk.setSeedBoxContents(this.filterConfiguredSeedContents(seedBoxContents));
        if (this.plugin.getTownFarmService() != null) {
            this.plugin.getTownFarmService().syncLiveSeedBoxInventory(liveTownChunk);
        }
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Replaces the Armory double-smelt toggle state for one Armory chunk.
     *
     * @param townChunk target Armory chunk
     * @param enabled replacement double-smelt state
     * @return {@code true} when the state was saved
     */
    public boolean setArmoryDoubleSmeltEnabled(final @NotNull RTownChunk townChunk, final boolean enabled) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.ARMORY
            || !this.plugin.getArmoryConfig().getDoubleSmelt().isUnlocked(liveTownChunk.getChunkLevel())) {
            return false;
        }

        liveTownChunk.setArmoryDoubleSmeltEnabled(enabled);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Removes a placed salvage block through direct breaking and returns the bound salvage block item.
     *
     * @param player acting player
     * @param townChunk owning Armory chunk
     * @return structured break result
     */
    public @NotNull ArmoryBlockBreakResult breakSalvageBlock(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        if (this.plugin.getTownRepository() == null) {
            return ArmoryBlockBreakResult.failed();
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || liveTownChunk.getChunkType() != ChunkType.ARMORY) {
            return ArmoryBlockBreakResult.invalidChunk();
        }
        if (!this.isPlayerAllowed(player, liveTownChunk, TownProtections.ARMORY_BREAK)) {
            return ArmoryBlockBreakResult.noPermission();
        }
        if (!liveTownChunk.hasSalvageBlock()) {
            return ArmoryBlockBreakResult.noBlock();
        }

        final SalvageBlockRemoval removal = this.removeSalvageBlockInternal(liveTownChunk, player, true);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return removal.removed() ? ArmoryBlockBreakResult.success() : ArmoryBlockBreakResult.failed();
    }

    /**
     * Removes a placed repair block through direct breaking and returns the bound repair block item.
     *
     * @param player acting player
     * @param townChunk owning Armory chunk
     * @return structured break result
     */
    public @NotNull ArmoryBlockBreakResult breakRepairBlock(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        if (this.plugin.getTownRepository() == null) {
            return ArmoryBlockBreakResult.failed();
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || liveTownChunk.getChunkType() != ChunkType.ARMORY) {
            return ArmoryBlockBreakResult.invalidChunk();
        }
        if (!this.isPlayerAllowed(player, liveTownChunk, TownProtections.ARMORY_BREAK)) {
            return ArmoryBlockBreakResult.noPermission();
        }
        if (!liveTownChunk.hasRepairBlock()) {
            return ArmoryBlockBreakResult.noBlock();
        }

        final RepairBlockRemoval removal = this.removeRepairBlockInternal(liveTownChunk, player, true);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return removal.removed() ? ArmoryBlockBreakResult.success() : ArmoryBlockBreakResult.failed();
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
     * Removes a placed seed box through the Farm chunk view and returns the bound seed-box item.
     *
     * @param player acting player
     * @param townChunk owning Farm chunk
     * @return structured pickup result
     */
    public @NotNull SeedBoxPickupResult pickupSeedBox(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        if (!this.hasTownPermission(player, TownPermissions.CHANGE_CHUNK_TYPE)) {
            return SeedBoxPickupResult.noPermission();
        }
        if (this.plugin.getTownRepository() == null) {
            return SeedBoxPickupResult.failed();
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null || liveTownChunk.getChunkType() != ChunkType.FARM) {
            return SeedBoxPickupResult.invalidChunk();
        }
        if (!liveTownChunk.hasSeedBox()) {
            return SeedBoxPickupResult.noSeedBox();
        }

        final SeedBoxRemoval removal = this.removeSeedBoxInternal(liveTownChunk, player, true);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return removal.removed()
            ? SeedBoxPickupResult.success(removal.droppedSeeds())
            : SeedBoxPickupResult.failed();
    }

    /**
     * Replaces the Farm growth-toggle state for one Farm chunk.
     *
     * @param townChunk target Farm chunk
     * @param enabled replacement growth-toggle state
     * @return {@code true} when the state was saved
     */
    public boolean setFarmGrowthEnabled(final @NotNull RTownChunk townChunk, final boolean enabled) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.FARM
            || !this.plugin.getFarmConfig().getGrowth().isUnlocked(liveTownChunk.getChunkLevel())) {
            return false;
        }

        liveTownChunk.setFarmGrowthEnabled(enabled);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Replaces the Farm auto-replant state for one Farm chunk.
     *
     * @param townChunk target Farm chunk
     * @param enabled replacement auto-replant state
     * @return {@code true} when the state was saved
     */
    public boolean setFarmAutoReplantEnabled(final @NotNull RTownChunk townChunk, final boolean enabled) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.FARM
            || !this.plugin.getFarmConfig().getReplant().isUnlocked(liveTownChunk.getChunkLevel())) {
            return false;
        }

        liveTownChunk.setFarmAutoReplantEnabled(enabled);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
    }

    /**
     * Replaces the Farm seed-consumption priority for one Farm chunk.
     *
     * @param townChunk target Farm chunk
     * @param priority replacement seed-consumption priority
     * @return {@code true} when the state was saved
     */
    public boolean setFarmReplantPriority(
        final @NotNull RTownChunk townChunk,
        final @NotNull FarmReplantPriority priority
    ) {
        if (this.plugin.getTownRepository() == null) {
            return false;
        }

        final RTownChunk liveTownChunk = this.resolveLiveTownChunk(townChunk);
        if (liveTownChunk == null
            || liveTownChunk.getChunkType() != ChunkType.FARM
            || !this.plugin.getFarmConfig().getReplant().isUnlocked(liveTownChunk.getChunkLevel())) {
            return false;
        }

        liveTownChunk.setFarmReplantPriority(priority);
        this.plugin.getTownRepository().update(liveTownChunk.getTown());
        return true;
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

        return this.isPlayerAllowed(player, townChunk, protection);
    }

    /**
     * Returns whether a player can perform a protected action in one claimed town chunk.
     *
     * @param player acting player
     * @param townChunk claimed chunk being checked
     * @param protection protection being checked
     * @return {@code true} when the action is allowed
     */
    public boolean isPlayerAllowed(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(townChunk, "townChunk");
        Objects.requireNonNull(protection, "protection");

        final RTown town = townChunk.getTown();
        if (this.areProtectionsBypassedForFuel(town)) {
            return true;
        }
        final String requiredRoleId = this.resolveRequiredRoleId(town, townChunk, protection);
        if (Objects.equals(requiredRoleId, RTown.PUBLIC_ROLE_ID)) {
            return true;
        }

        final RDTPlayer playerData = this.getPlayerData(player.getUniqueId());
        if (playerData != null && Objects.equals(playerData.getTownUUID(), town.getTownUUID())) {
            if (Objects.equals(requiredRoleId, RTown.RESTRICTED_ROLE_ID)) {
                return false;
            }
            return this.hasRequiredRole(town, playerData.getTownRoleId(), requiredRoleId);
        }
        return this.isAlliedTownMemberAllowed(town, townChunk, protection, playerData);
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
     * Returns whether allied towns may perform one protected action in the supplied scope.
     *
     * @param town owner town
     * @param townChunk optional Security chunk scope
     * @param protection protection to resolve
     * @return {@code true} when allied towns are allowed to perform the protected action
     */
    public boolean isAlliedProtectionAllowed(
        final @NotNull RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        return this.resolveAlliedProtectionAllowed(town, townChunk, protection);
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

    private boolean isAlliedTownMemberAllowed(
        final @NotNull RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull TownProtections protection,
        final @Nullable RDTPlayer playerData
    ) {
        if (playerData == null || playerData.getTownUUID() == null) {
            return false;
        }
        final RTown playerTown = this.getTown(playerData.getTownUUID());
        if (playerTown == null
            || Objects.equals(playerTown.getTownUUID(), town.getTownUUID())
            || this.getEffectiveRelationshipState(town, playerTown) != TownRelationshipState.ALLIED) {
            return false;
        }
        return this.resolveAlliedProtectionAllowed(town, townChunk, protection);
    }

    private @NotNull String resolveRequiredRoleId(
        final @NotNull RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        if (townChunk != null && this.supportsChunkProtectionOverrides(townChunk)) {
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

    private boolean giveSeedBox(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        final ItemStack seedBoxItem = SeedBox.getSeedBoxItem(
            this.plugin,
            player,
            townChunk.getTown().getTownUUID(),
            townChunk.getWorldName(),
            townChunk.getX(),
            townChunk.getZ()
        );
        player.getInventory().addItem(seedBoxItem)
            .values()
            .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        return true;
    }

    private boolean giveSalvageBlock(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        final TownArmoryService townArmoryService = this.plugin.getTownArmoryService();
        final ItemStack salvageBlockItem = townArmoryService == null
            ? SalvageBlock.getSalvageBlockItem(
                this.plugin,
                player,
                townChunk.getTown().getTownUUID(),
                townChunk.getWorldName(),
                townChunk.getX(),
                townChunk.getZ()
            )
            : townArmoryService.createSalvageBlockItem(player, townChunk);
        player.getInventory().addItem(salvageBlockItem)
            .values()
            .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        return true;
    }

    private boolean giveRepairBlock(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        final TownArmoryService townArmoryService = this.plugin.getTownArmoryService();
        final ItemStack repairBlockItem = townArmoryService == null
            ? RepairBlock.getRepairBlockItem(
                this.plugin,
                player,
                townChunk.getTown().getTownUUID(),
                townChunk.getWorldName(),
                townChunk.getX(),
                townChunk.getZ()
            )
            : townArmoryService.createRepairBlockItem(player, townChunk);
        player.getInventory().addItem(repairBlockItem)
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

    private @NotNull SeedBoxRemoval removeSeedBoxInternal(
        final @NotNull RTownChunk townChunk,
        final @Nullable Player player,
        final boolean returnSeedBoxItem
    ) {
        if (!townChunk.hasSeedBox()) {
            return SeedBoxRemoval.none();
        }

        final Location dropLocation = townChunk.getSeedBoxLocation();
        final Map<String, ItemStack> storedSeedContents = this.captureSeedBoxContents(townChunk);
        final boolean droppedSeeds = dropLocation != null && dropLocation.getWorld() != null && !storedSeedContents.isEmpty();
        if (dropLocation != null && dropLocation.getWorld() != null) {
            if (dropLocation.getBlock().getState() instanceof Chest) {
                dropLocation.getBlock().setType(Material.AIR, false);
            }
            for (final ItemStack itemStack : storedSeedContents.values()) {
                if (itemStack != null && !itemStack.isEmpty()) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, itemStack.clone());
                }
            }
        }

        townChunk.clearSeedBoxState();
        if (returnSeedBoxItem && player != null) {
            this.giveSeedBox(player, townChunk);
        }
        return new SeedBoxRemoval(true, droppedSeeds);
    }

    private @NotNull SalvageBlockRemoval removeSalvageBlockInternal(
        final @NotNull RTownChunk townChunk,
        final @Nullable Player player,
        final boolean returnSalvageBlockItem
    ) {
        if (!townChunk.hasSalvageBlock()) {
            return SalvageBlockRemoval.none();
        }

        final Location blockLocation = townChunk.getSalvageBlockLocation();
        if (blockLocation != null && blockLocation.getWorld() != null) {
            blockLocation.getBlock().setType(Material.AIR, false);
        }

        townChunk.setSalvageBlockLocation(null);
        if (returnSalvageBlockItem && player != null) {
            this.giveSalvageBlock(player, townChunk);
        }
        return new SalvageBlockRemoval(true);
    }

    private @NotNull RepairBlockRemoval removeRepairBlockInternal(
        final @NotNull RTownChunk townChunk,
        final @Nullable Player player,
        final boolean returnRepairBlockItem
    ) {
        if (!townChunk.hasRepairBlock()) {
            return RepairBlockRemoval.none();
        }

        final Location blockLocation = townChunk.getRepairBlockLocation();
        if (blockLocation != null && blockLocation.getWorld() != null) {
            blockLocation.getBlock().setType(Material.AIR, false);
        }

        townChunk.setRepairBlockLocation(null);
        if (returnRepairBlockItem && player != null) {
            this.giveRepairBlock(player, townChunk);
        }
        return new RepairBlockRemoval(true);
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

    private @NotNull Map<String, ItemStack> captureSeedBoxContents(final @NotNull RTownChunk townChunk) {
        final Location seedBoxLocation = townChunk.getSeedBoxLocation();
        if (seedBoxLocation != null
            && seedBoxLocation.getWorld() != null
            && seedBoxLocation.getWorld().isChunkLoaded(townChunk.getX(), townChunk.getZ())
            && seedBoxLocation.getBlock().getState() instanceof Chest chest) {
            final Map<String, ItemStack> contents = new LinkedHashMap<>();
            for (int slot = 0; slot < chest.getBlockInventory().getSize(); slot++) {
                final ItemStack itemStack = chest.getBlockInventory().getItem(slot);
                if (itemStack == null || itemStack.isEmpty()) {
                    continue;
                }
                contents.put(String.valueOf(slot), itemStack.clone());
            }
            return this.filterConfiguredSeedContents(contents);
        }
        return this.filterConfiguredSeedContents(townChunk.getSeedBoxContents());
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

    private @NotNull Map<String, ItemStack> filterConfiguredSeedContents(final @NotNull Map<String, ItemStack> rawContents) {
        final Map<String, ItemStack> filteredContents = new LinkedHashMap<>();
        for (final Map.Entry<String, ItemStack> entry : rawContents.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (!this.plugin.getFarmConfig().isAllowedSeedMaterial(entry.getValue().getType())) {
                continue;
            }
            filteredContents.put(entry.getKey(), entry.getValue().clone());
        }
        return filteredContents;
    }

    private void initializeFarmEnhancementDefaults(final @NotNull RTownChunk townChunk) {
        if (townChunk.getChunkType() != ChunkType.FARM) {
            return;
        }

        final var growthSettings = this.plugin.getFarmConfig().getGrowth();
        final var replantSettings = this.plugin.getFarmConfig().getReplant();
        if (growthSettings.isUnlocked(townChunk.getChunkLevel()) && townChunk.getFarmGrowthEnabledValue() == null) {
            townChunk.setFarmGrowthEnabled(growthSettings.enabledByDefault());
        }
        if (replantSettings.isUnlocked(townChunk.getChunkLevel())) {
            if (townChunk.getFarmAutoReplantEnabledValue() == null) {
                townChunk.setFarmAutoReplantEnabled(replantSettings.enabledByDefault());
            }
            if (townChunk.getFarmReplantPriorityValue() == null) {
                townChunk.setFarmReplantPriority(replantSettings.defaultSourcePriority());
            }
        }
    }

    private boolean shouldGrantSeedBoxAtLevel(final int farmLevel) {
        return this.plugin.getFarmConfig().getSeedBox().isUnlocked(farmLevel);
    }

    private void initializeArmoryEnhancementDefaults(final @NotNull RTownChunk townChunk) {
        if (townChunk.getChunkType() != ChunkType.ARMORY) {
            return;
        }

        final var doubleSmeltSettings = this.plugin.getArmoryConfig().getDoubleSmelt();
        if (doubleSmeltSettings.isUnlocked(townChunk.getChunkLevel())
            && townChunk.getArmoryDoubleSmeltEnabledValue() == null) {
            townChunk.setArmoryDoubleSmeltEnabled(doubleSmeltSettings.enabledByDefault());
        }
    }

    private boolean shouldGrantSalvageBlockAtLevel(final int armoryLevel) {
        return this.plugin.getArmoryConfig().getSalvageBlock().isUnlocked(armoryLevel);
    }

    private boolean shouldGrantRepairBlockAtLevel(final int armoryLevel) {
        return this.plugin.getArmoryConfig().getRepairBlock().isUnlocked(armoryLevel);
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

        if (scope == LevelScope.NATION_FORMATION) {
            return new LevelUpResult(LevelUpStatus.INVALID_TARGET, context.currentLevel(), context.currentLevel());
        }
        if (scope == LevelScope.NATION) {
            final RNation nation = context.nation();
            final RTown capitalTown = nation == null ? null : this.getTown(nation.getCapitalTownUuid());
            if (capitalTown == null
                || !Objects.equals(capitalTown.getTownUUID(), context.town().getTownUUID())
                || !this.hasTownPermission(player, capitalTown, TownPermissions.MANAGE_NATIONS)) {
                return new LevelUpResult(LevelUpStatus.NO_PERMISSION, context.currentLevel(), context.currentLevel());
            }
        } else {
            final TownPermissions requiredPermission = scope == LevelScope.NEXUS
                ? TownPermissions.UPGRADE_TOWN
                : TownPermissions.UPGRADE_CHUNK;
            if (!this.hasTownPermission(player, context.town(), requiredPermission)) {
                return new LevelUpResult(LevelUpStatus.NO_PERMISSION, context.currentLevel(), context.currentLevel());
            }
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
        if (scope == LevelScope.NEXUS) {
            this.healTownNexusToFull(context.town());
        }
        context.progressAccessor().clearLevelProgress(this.buildProgressKeyPrefix(scope, context.targetLevel()));
        if (scope == LevelScope.FARM && context.chunk() != null) {
            this.initializeFarmEnhancementDefaults(context.chunk());
        }
        if (scope == LevelScope.ARMORY && context.chunk() != null) {
            this.initializeArmoryEnhancementDefaults(context.chunk());
        }
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
        if (scope == LevelScope.FARM
            && context.chunk() != null
            && previousLevel < this.plugin.getFarmConfig().getSeedBox().unlockLevel()
            && this.shouldGrantSeedBoxAtLevel(context.targetLevel())) {
            this.giveSeedBox(player, context.chunk());
        }
        if (scope == LevelScope.ARMORY
            && context.chunk() != null
            && previousLevel < this.plugin.getArmoryConfig().getSalvageBlock().unlockLevel()
            && this.shouldGrantSalvageBlockAtLevel(context.targetLevel())) {
            this.giveSalvageBlock(player, context.chunk());
        }
        if (scope == LevelScope.ARMORY
            && context.chunk() != null
            && previousLevel < this.plugin.getArmoryConfig().getRepairBlock().unlockLevel()
            && this.shouldGrantRepairBlockAtLevel(context.targetLevel())) {
            this.giveRepairBlock(player, context.chunk());
        }
        if (scope == LevelScope.BANK
            && context.chunk() != null
            && this.plugin.getTownBankService() != null
            && this.plugin.getTownBankService().shouldGrantCacheChestOnLevelGain(
            context.town(),
            previousLevel,
            context.targetLevel()
        )) {
            this.plugin.getTownBankService().giveCacheChest(player, context.town());
        }
        if (scope == LevelScope.OUTPOST && context.chunk() != null) {
            this.syncRdsTownOutpost(context.chunk());
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
                    null,
                    true,
                    liveTown.getNexusLevel(),
                    this.plugin.getNexusConfig().getHighestConfiguredLevel(),
                    this.plugin.getNexusConfig().getNextLevel(liveTown.getNexusLevel()),
                    this.plugin.getNexusConfig().getLevels(),
                    new TownProgressAccessor(liveTown)
                );
            }
            case NATION_FORMATION -> {
                final RTown liveTown = town == null ? null : this.resolveLiveTown(town);
                if (liveTown == null) {
                    yield null;
                }
                final Map<Integer, LevelDefinition> configuredLevels = this.plugin.getNationConfig().getFormationLevels();
                final boolean available = this.isTownEligibleForNationCreation(liveTown);
                final Integer targetLevel = this.plugin.getNationConfig().getNextFormationLevel(0);
                yield new LevelContext(
                    scope,
                    liveTown,
                    null,
                    null,
                    available,
                    0,
                    this.plugin.getNationConfig().getHighestConfiguredFormationLevel(),
                    available ? targetLevel : null,
                    configuredLevels,
                    new NationCreationProgressAccessor(liveTown)
                );
            }
            case NATION -> {
                final RTown liveTown = town == null ? null : this.resolveLiveTown(town);
                if (liveTown == null) {
                    yield null;
                }
                final RNation nation = this.getNationForTown(liveTown);
                if (nation == null || !nation.isActive()) {
                    yield null;
                }
                final int currentLevel = nation.getNationLevel();
                yield new LevelContext(
                    scope,
                    liveTown,
                    null,
                    nation,
                    true,
                    currentLevel,
                    this.plugin.getNationConfig().getHighestConfiguredProgressionLevel(),
                    this.plugin.getNationConfig().getNextProgressionLevel(currentLevel),
                    this.plugin.getNationConfig().getProgressionLevels(),
                    new NationProgressAccessor(nation)
                );
            }
            case SECURITY, BANK, FARM, FOB, OUTPOST, MEDIC, ARMORY -> {
                final RTownChunk liveChunk = townChunk == null ? null : this.resolveLiveTownChunk(townChunk);
                if (liveChunk == null) {
                    yield null;
                }
                final boolean available = LevelScope.fromChunkType(liveChunk.getChunkType()) == scope;
                yield new LevelContext(
                    scope,
                    liveChunk.getTown(),
                    liveChunk,
                    null,
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
        final int currentLevel = scope == LevelScope.NATION_FORMATION
            ? 0
            : scope == LevelScope.NATION
                ? 1
            : scope.isChunkScope() && townChunk != null
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
            case NATION_FORMATION -> this.plugin.getNationConfig().getFormationLevels();
            case NATION -> this.plugin.getNationConfig().getProgressionLevels();
            case SECURITY -> this.plugin.getSecurityConfig().getLevels();
            case BANK -> this.plugin.getBankConfig().getLevels();
            case FARM -> this.plugin.getFarmConfig().getLevels();
            case FOB -> this.plugin.getFobConfig().getLevels();
            case OUTPOST -> this.plugin.getOutpostConfig().getLevels();
            case MEDIC -> this.plugin.getMedicConfig().getLevels();
            case ARMORY -> this.plugin.getArmoryConfig().getLevels();
        };
    }

    private int getHighestConfiguredLevel(final @NotNull LevelScope scope) {
        return switch (scope) {
            case NEXUS -> this.plugin.getNexusConfig().getHighestConfiguredLevel();
            case NATION_FORMATION -> this.plugin.getNationConfig().getHighestConfiguredFormationLevel();
            case NATION -> this.plugin.getNationConfig().getHighestConfiguredProgressionLevel();
            case SECURITY -> this.plugin.getSecurityConfig().getHighestConfiguredLevel();
            case BANK -> this.plugin.getBankConfig().getHighestConfiguredLevel();
            case FARM -> this.plugin.getFarmConfig().getHighestConfiguredLevel();
            case FOB -> this.plugin.getFobConfig().getHighestConfiguredLevel();
            case OUTPOST -> this.plugin.getOutpostConfig().getHighestConfiguredLevel();
            case MEDIC -> this.plugin.getMedicConfig().getHighestConfiguredLevel();
            case ARMORY -> this.plugin.getArmoryConfig().getHighestConfiguredLevel();
        };
    }

    private @Nullable Integer getNextConfiguredLevel(final @NotNull LevelScope scope, final int currentLevel) {
        return switch (scope) {
            case NEXUS -> this.plugin.getNexusConfig().getNextLevel(currentLevel);
            case NATION_FORMATION -> this.plugin.getNationConfig().getNextFormationLevel(currentLevel);
            case NATION -> this.plugin.getNationConfig().getNextProgressionLevel(currentLevel);
            case SECURITY -> this.plugin.getSecurityConfig().getNextLevel(currentLevel);
            case BANK -> this.plugin.getBankConfig().getNextLevel(currentLevel);
            case FARM -> this.plugin.getFarmConfig().getNextLevel(currentLevel);
            case FOB -> this.plugin.getFobConfig().getNextLevel(currentLevel);
            case OUTPOST -> this.plugin.getOutpostConfig().getNextLevel(currentLevel);
            case MEDIC -> this.plugin.getMedicConfig().getNextLevel(currentLevel);
            case ARMORY -> this.plugin.getArmoryConfig().getNextLevel(currentLevel);
        };
    }

    private @NotNull LevelScope requireChunkLevelScope(final @NotNull RTownChunk townChunk) {
        final LevelScope scope = LevelScope.fromChunkType(Objects.requireNonNull(townChunk, "townChunk").getChunkType());
        if (scope == null) {
            throw new IllegalArgumentException("Chunk type " + townChunk.getChunkType() + " has no progression path");
        }
        return scope;
    }

    private boolean supportsChunkProtectionOverrides(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.SECURITY || townChunk.getChunkType() == ChunkType.FOB;
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
        final List<TownLevelRewardSnapshot> rewardSnapshots = new ArrayList<>();
        if (levelDefinition != null && !levelDefinition.getRewards().isEmpty()) {
            final RewardFactory<Map<String, Object>> rewardFactory = RewardFactory.getInstance();
            final Map<String, String> placeholders = this.buildLevelPlaceholders(context, sourceLevel, targetLevel);
            final Map<String, Map<String, Object>> expandedRewards = this.expandDefinitionPlaceholders(
                levelDefinition.getRewards(),
                placeholders
            );
            for (final Map.Entry<String, Map<String, Object>> entry : expandedRewards.entrySet()) {
                final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(entry.getValue());
                final AbstractReward reward = rewardFactory.tryFromMap(normalizedDefinition).orElse(null);
                final TownLevelRewardSnapshot rewardSnapshot = this.createRewardSnapshot(entry.getKey(), normalizedDefinition, reward);
                if (rewardSnapshot != null) {
                    rewardSnapshots.add(rewardSnapshot);
                }
            }
        }
        rewardSnapshots.addAll(this.buildSyntheticRewardSnapshots(context, targetLevel));
        return List.copyOf(rewardSnapshots);
    }

    private @NotNull List<TownLevelRewardSnapshot> buildSyntheticRewardSnapshots(
        final @NotNull LevelContext context,
        final int targetLevel
    ) {
        return switch (context.scope()) {
            case FARM -> this.buildFarmSyntheticRewardSnapshots(targetLevel);
            case ARMORY -> this.buildArmorySyntheticRewardSnapshots(targetLevel);
            case OUTPOST -> this.buildOutpostSyntheticRewardSnapshots(context, targetLevel);
            default -> List.of();
        };
    }

    private @NotNull List<TownLevelRewardSnapshot> buildFarmSyntheticRewardSnapshots(final int targetLevel) {
        final List<TownLevelRewardSnapshot> syntheticRewards = new ArrayList<>();
        final var growthSettings = this.plugin.getFarmConfig().getGrowth();
        final var seedBoxSettings = this.plugin.getFarmConfig().getSeedBox();
        final var replantSettings = this.plugin.getFarmConfig().getReplant();
        final var doubleHarvestSettings = this.plugin.getFarmConfig().getDoubleHarvest();

        if (targetLevel == growthSettings.tierOneUnlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "farm_growth_tier_1",
                "FARM_UNLOCK",
                "Enhanced Growth I",
                "Unlocks the Farm growth toggle and increases natural crop growth speed to %sx while enabled."
                    .formatted(TownFarmService.formatGrowthSpeedMultiplier(growthSettings.tierOneGrowthSpeedMultiplier())),
                this.createDisplayItem(Material.WHEAT, "Enhanced Growth I", 1.0D)
            ));
        }
        if (targetLevel == seedBoxSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "farm_seed_box",
                "FARM_UNLOCK",
                "Seed Box Storage",
                "Unlocks a Farm seed box within %d blocks of the chunk marker for configured seed storage."
                    .formatted(seedBoxSettings.placementRadiusBlocks()),
                this.createDisplayItem(Material.CHEST, "Seed Box Storage", 1.0D)
            ));
        }
        if (targetLevel == replantSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "farm_auto_replant",
                "FARM_UNLOCK",
                "Auto-Replant",
                "Unlocks Farm auto-replanting with the default priority set to %s."
                    .formatted(this.toDisplayLabel(replantSettings.defaultSourcePriority().name())),
                this.createDisplayItem(Material.WHEAT_SEEDS, "Auto-Replant", 1.0D)
            ));
        }
        if (targetLevel == growthSettings.tierTwoUnlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "farm_growth_tier_2",
                "FARM_UNLOCK",
                "Enhanced Growth II",
                "Upgrades the Farm growth toggle to %sx natural crop growth speed while enabled."
                    .formatted(TownFarmService.formatGrowthSpeedMultiplier(growthSettings.tierTwoGrowthSpeedMultiplier())),
                this.createDisplayItem(Material.GOLDEN_HOE, "Enhanced Growth II", 1.0D)
            ));
        }
        if (targetLevel == doubleHarvestSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "farm_double_harvest",
                "FARM_UNLOCK",
                "Double Harvest",
                "Harvested crop drops are multiplied by %d while this Farm chunk is active."
                    .formatted(doubleHarvestSettings.multiplier()),
                this.createDisplayItem(Material.HAY_BLOCK, "Double Harvest", doubleHarvestSettings.multiplier())
            ));
        }
        return List.copyOf(syntheticRewards);
    }

    private @NotNull List<TownLevelRewardSnapshot> buildArmorySyntheticRewardSnapshots(final int targetLevel) {
        final List<TownLevelRewardSnapshot> syntheticRewards = new ArrayList<>();
        final var freeRepairSettings = this.plugin.getArmoryConfig().getFreeRepair();
        final var salvageBlockSettings = this.plugin.getArmoryConfig().getSalvageBlock();
        final var repairBlockSettings = this.plugin.getArmoryConfig().getRepairBlock();
        final var doubleSmeltSettings = this.plugin.getArmoryConfig().getDoubleSmelt();

        if (targetLevel == freeRepairSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "armory_free_repair",
                "ARMORY_UNLOCK",
                "Free Repair",
                "Unlocks chunk-view free repair for equipped gear with a %s cooldown per player."
                    .formatted(this.formatDurationMillis(freeRepairSettings.cooldownSeconds() * 1000L)),
                this.createDisplayItem(Material.ANVIL, "Free Repair", 1.0D)
            ));
        }
        if (targetLevel == salvageBlockSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "armory_salvage_block",
                "ARMORY_UNLOCK",
                "Salvage Block",
                "Unlocks a salvage block within %d blocks of the chunk marker for salvaging supported gear."
                    .formatted(salvageBlockSettings.placementRadiusBlocks()),
                this.createDisplayItem(salvageBlockSettings.blockMaterial(), "Salvage Block", 1.0D)
            ));
        }
        if (targetLevel == repairBlockSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "armory_repair_block",
                "ARMORY_UNLOCK",
                "Repair Block",
                "Unlocks a repair block within %d blocks of the chunk marker for repairing iron, gold, diamond, and netherite gear."
                    .formatted(repairBlockSettings.placementRadiusBlocks()),
                this.createDisplayItem(repairBlockSettings.blockMaterial(), "Repair Block", 1.0D)
            ));
        }
        if (targetLevel == doubleSmeltSettings.unlockLevel()) {
            syntheticRewards.add(new TownLevelRewardSnapshot(
                "armory_double_smelt",
                "ARMORY_UNLOCK",
                "Double Smelt",
                "Unlocks furnace and blast-furnace doubling with a %sx burn multiplier and %d extra fuel per smelt."
                    .formatted(
                        this.formatDecimal(doubleSmeltSettings.burnFasterMultiplier()),
                        doubleSmeltSettings.extraFuelPerSmeltUnits()
                    ),
                this.createDisplayItem(Material.BLAST_FURNACE, "Double Smelt", 2.0D)
            ));
        }
        return List.copyOf(syntheticRewards);
    }

    private @NotNull List<TownLevelRewardSnapshot> buildOutpostSyntheticRewardSnapshots(
        final @NotNull LevelContext context,
        final int targetLevel
    ) {
        if (context.chunk() == null || targetLevel < 3) {
            return List.of();
        }

        final boolean rdsAvailable = this.isRdsTownShopFeatureAvailable();
        final int totalShops = switch (targetLevel) {
            case 3 -> 1;
            case 4 -> 3;
            default -> 5;
        };
        final int additionalShops = switch (targetLevel) {
            case 3 -> 1;
            case 4, 5 -> 2;
            default -> 0;
        };
        final String title = additionalShops == 1
            ? "Town Shop"
            : additionalShops + " Town Shops";
        final String description = rdsAvailable
            ? switch (targetLevel) {
                case 3 -> "Unlocks 1 town shop for this Outpost. Eligible town members can claim a bound Town Shop token and place it inside this chunk.";
                case 4 -> "Adds 2 more town shops for this Outpost, bringing the total to 3. Eligible town members can claim bound Town Shop tokens when capacity is available.";
                default -> "Adds 2 more town shops for this Outpost, bringing the total to 5. Eligible town members can claim bound Town Shop tokens when capacity is available.";
            }
            : switch (targetLevel) {
                case 3 -> "Town shop reward preview is unavailable because RDS is not installed. The Outpost still levels successfully.";
                case 4 -> "Additional town shops are unavailable because RDS is not installed. The Outpost still levels successfully.";
                default -> "Additional town shops are unavailable because RDS is not installed. The Outpost still levels successfully.";
            };

        final Material material = rdsAvailable ? Material.CHEST : Material.BARRIER;
        final ItemStack displayItem = this.createDisplayItem(
            material,
            rdsAvailable ? title : title + " Unavailable",
            rdsAvailable ? additionalShops : 1.0D
        );
        return List.of(new TownLevelRewardSnapshot(
            "outpost_town_shops_level_" + targetLevel,
            rdsAvailable ? "TOWN_SHOP" : "TOWN_SHOP_UNAVAILABLE",
            title,
            description + " Total unlocked at this level: " + totalShops + '.',
            displayItem
        ));
    }

    private @Nullable TownLevelRewardSnapshot createRewardSnapshot(
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
            if (this.isHiddenOutpostTownShopRewardCommand(commandReward.getCommand())) {
                return null;
            }
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

    /**
     * Returns whether the optional RDS town-shop integration is available.
     *
     * @return {@code true} when RDS town-shop services can be reached
     */
    public boolean isRdsTownShopFeatureAvailable() {
        return this.resolveRdsTownShopService() != null;
    }

    /**
     * Attempts to reissue one bound town-shop token for an unlocked Outpost chunk.
     *
     * @param player requesting player
     * @param townChunk outpost chunk to claim against
     * @return {@code true} when a token was reissued
     */
    public boolean claimOutpostTownShopToken(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        if (townChunk.getChunkType() != ChunkType.OUTPOST
            || townChunk.getChunkLevel() < 3
            || !this.hasTownPermission(player, townChunk.getTown(), TownPermissions.MANAGE_TOWN_SHOPS)) {
            return false;
        }

        final Object townShopService = this.resolveRdsTownShopService();
        if (townShopService == null) {
            return false;
        }

        try {
            final Method method = townShopService.getClass().getMethod("claimTownShopToken", Player.class, UUID.class);
            final Object result = method.invoke(townShopService, player, townChunk.getIdentifier());
            return result instanceof Boolean claimed && claimed;
        } catch (final ReflectiveOperationException ignored) {
            return false;
        }
    }

    private boolean isHiddenOutpostTownShopRewardCommand(final @Nullable String command) {
        if (command == null || command.isBlank()) {
            return false;
        }

        String normalizedCommand = command.trim().toLowerCase(Locale.ROOT);
        if (normalizedCommand.startsWith("/")) {
            normalizedCommand = normalizedCommand.substring(1);
        }
        return normalizedCommand.startsWith("rs internal reward-town-shop ")
            || normalizedCommand.startsWith("prs internal reward-town-shop ");
    }

    private void syncRdsTownOutpost(final @Nullable RTownChunk townChunk) {
        if (townChunk == null || townChunk.getChunkType() != ChunkType.OUTPOST) {
            return;
        }

        final Object townShopService = this.resolveRdsTownShopService();
        if (townShopService == null) {
            return;
        }

        try {
            final Method method = townShopService.getClass().getMethod(
                "syncOutpost",
                String.class,
                String.class,
                String.class,
                UUID.class,
                String.class,
                int.class,
                int.class,
                int.class
            );
            method.invoke(
                townShopService,
                "RDT",
                townChunk.getTown().getTownUUID().toString(),
                townChunk.getTown().getTownName(),
                townChunk.getIdentifier(),
                townChunk.getWorldName(),
                townChunk.getX(),
                townChunk.getZ(),
                townChunk.getChunkLevel()
            );
        } catch (final ReflectiveOperationException exception) {
            LOGGER.log(Level.FINE, "Failed to sync RDS outpost " + townChunk.getIdentifier(), exception);
        }
    }

    private void removeRdsTownOutpost(final @Nullable UUID chunkUuid) {
        if (chunkUuid == null) {
            return;
        }

        final Object townShopService = this.resolveRdsTownShopService();
        if (townShopService == null) {
            return;
        }

        try {
            final Method method = townShopService.getClass().getMethod("removeOutpost", UUID.class);
            method.invoke(townShopService, chunkUuid);
        } catch (final ReflectiveOperationException exception) {
            LOGGER.log(Level.FINE, "Failed to remove RDS outpost " + chunkUuid, exception);
        }
    }

    private @Nullable Object resolveRdsTownShopService() {
        if (this.plugin.getServer() == null || this.plugin.getServer().getPluginManager() == null) {
            return null;
        }
        final Plugin rdsPlugin = this.plugin.getServer().getPluginManager().getPlugin("RDS");
        if (rdsPlugin == null || !rdsPlugin.isEnabled()) {
            return null;
        }

        try {
            final Object runtime = this.resolveRdsRuntime(rdsPlugin);
            return runtime == null ? null : this.invokeOptional(runtime, "getTownShopService");
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @Nullable Object resolveRdsRuntime(final @NotNull Plugin plugin) throws ReflectiveOperationException {
        if (this.hasZeroArgMethod(plugin.getClass(), "getTownShopService")) {
            return plugin;
        }

        final Object directRuntime = this.firstNonNull(
            this.invokeOptional(plugin, "getRds"),
            this.readFieldOptional(plugin, "rds")
        );
        if (directRuntime != null && this.hasZeroArgMethod(directRuntime.getClass(), "getTownShopService")) {
            return directRuntime;
        }

        final Object delegate = this.firstNonNull(
            this.readFieldOptional(plugin, "impl"),
            this.readFieldOptional(plugin, "delegate")
        );
        if (delegate == null) {
            return null;
        }

        final Object delegatedRuntime = this.firstNonNull(
            this.invokeOptional(delegate, "getRds"),
            this.readFieldOptional(delegate, "rds")
        );
        return delegatedRuntime != null && this.hasZeroArgMethod(delegatedRuntime.getClass(), "getTownShopService")
            ? delegatedRuntime
            : null;
    }

    private boolean hasZeroArgMethod(final @NotNull Class<?> type, final @NotNull String methodName) {
        try {
            type.getMethod(methodName);
            return true;
        } catch (final NoSuchMethodException ignored) {
            return false;
        }
    }

    private @Nullable Object invokeOptional(
        final @NotNull Object target,
        final @NotNull String methodName,
        final Object... arguments
    ) throws ReflectiveOperationException {
        final Method method = this.findMethod(target.getClass(), methodName, arguments.length);
        if (method == null) {
            return null;
        }
        return method.invoke(target, arguments);
    }

    private @Nullable Method findMethod(
        final @NotNull Class<?> type,
        final @NotNull String methodName,
        final int parameterCount
    ) {
        for (final Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private boolean resolveAlliedProtectionAllowed(
        final @NotNull RTown town,
        final @Nullable RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        if (protection.isBinaryToggle()) {
            return false;
        }
        if (townChunk != null && this.supportsChunkProtectionOverrides(townChunk)) {
            final Boolean chunkOverride = this.resolveChunkAlliedProtectionAllowed(townChunk, protection);
            if (chunkOverride != null) {
                return chunkOverride;
            }
        }
        return town.isAlliedProtectionAllowed(protection);
    }

    private @Nullable Boolean resolveChunkAlliedProtectionAllowed(
        final @NotNull RTownChunk townChunk,
        final @NotNull TownProtections protection
    ) {
        TownProtections currentProtection = protection;
        while (currentProtection != null) {
            final Boolean override = townChunk.getConfiguredAlliedProtectionAllowed(currentProtection);
            if (override != null) {
                return override;
            }
            currentProtection = currentProtection.getFallbackProtection();
        }
        return null;
    }

    private @Nullable Object readFieldOptional(
        final @NotNull Object target,
        final @NotNull String fieldName
    ) throws ReflectiveOperationException {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                final Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private @Nullable Object firstNonNull(final @Nullable Object... values) {
        if (values == null) {
            return null;
        }
        for (final Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private @NotNull Map<String, String> buildLevelPlaceholders(
        final @NotNull LevelContext context,
        final int sourceLevel,
        final int targetLevel
    ) {
        final Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("town_uuid", context.town().getTownUUID().toString());
        placeholders.put("town_name", context.town().getTownName());
        placeholders.put(
            "town_name_base64",
            Base64.getUrlEncoder().withoutPadding().encodeToString(context.town().getTownName().getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        placeholders.put("current_level", String.valueOf(sourceLevel));
        placeholders.put("target_level", String.valueOf(targetLevel));
        placeholders.put("level_scope", context.scope().name().toLowerCase(Locale.ROOT));
        placeholders.put("level_scope_name", context.scope().getDisplayName());
        placeholders.put("chunk_uuid", context.chunk() == null ? "" : context.chunk().getIdentifier().toString());
        placeholders.put("chunk_type", context.chunk() == null ? "" : context.chunk().getChunkType().name());
        placeholders.put("world_name", context.chunk() == null ? "" : context.chunk().getWorldName());
        placeholders.put("chunk_x", context.chunk() == null ? "0" : String.valueOf(context.chunk().getX()));
        placeholders.put("chunk_z", context.chunk() == null ? "0" : String.valueOf(context.chunk().getZ()));
        if (context.nation() != null) {
            placeholders.putAll(this.buildNationPlaceholders(context.nation()));
        }
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

    private void processPendingNationTimeouts() {
        if (this.plugin.getNationRepository() == null || this.plugin.getNationInviteRepository() == null) {
            return;
        }

        final long now = System.currentTimeMillis();
        for (final RNation nation : this.plugin.getNationRepository().findAll()) {
            for (final NationInvite invite : this.plugin.getNationInviteRepository().findByNationUuid(nation.getNationUuid())) {
                if (invite.isPending() && invite.getExpiresAt() > 0L && now >= invite.getExpiresAt()) {
                    invite.timeout();
                    this.plugin.getNationInviteRepository().update(invite);
                }
            }
            if (nation.getStatus() == NationStatus.PENDING) {
                this.resolvePendingNationFormation(nation);
            } else if (nation.getStatus() == NationStatus.ACTIVE) {
                this.resolvePendingNationExpansion(nation);
            }
        }
    }

    private void resolvePendingNationFormation(final @NotNull RNation nation) {
        if (this.plugin.getNationRepository() == null || this.plugin.getNationInviteRepository() == null) {
            return;
        }

        final List<NationInvite> formationInvites = this.plugin.getNationInviteRepository().findByNationUuid(nation.getNationUuid()).stream()
            .filter(invite -> invite.getInviteType() == NationInviteType.FORMATION)
            .toList();
        if (formationInvites.isEmpty() || formationInvites.stream().anyMatch(NationInvite::isPending)) {
            return;
        }

        final RTown capitalTown = this.getTown(nation.getCapitalTownUuid());
        final RTown initiatingTown = this.getTown(nation.getInitiatingTownUuid());
        if (capitalTown == null || initiatingTown == null) {
            nation.setStatus(NationStatus.FAILED);
            nation.setExpiresAt(0L);
            this.plugin.getNationRepository().update(nation);
            return;
        }

        final List<RTown> acceptedTowns = formationInvites.stream()
            .filter(invite -> invite.getStatus() == NationInviteStatus.ACCEPTED)
            .map(invite -> this.getTown(invite.getTargetTownUuid()))
            .filter(Objects::nonNull)
            .toList();
        if (acceptedTowns.size() + 1 >= nation.getMinimumTownThreshold()) {
            nation.setStatus(NationStatus.ACTIVE);
            nation.setNationLevel(1);
            nation.setExpiresAt(0L);
            this.plugin.getNationRepository().update(nation);

            capitalTown.setNationUuid(nation.getNationUuid());
            this.updateTown(capitalTown);
            for (final RTown acceptedTown : acceptedTowns) {
                acceptedTown.setNationUuid(nation.getNationUuid());
                this.updateTown(acceptedTown);
            }
            this.syncNationAlliances(this.getNationMemberTowns(nation));
            this.clearNationCreationProgress(initiatingTown);
            this.updateTown(initiatingTown);
            this.grantNationCreationRewards(nation, capitalTown, initiatingTown);
            return;
        }

        nation.setStatus(NationStatus.FAILED);
        nation.setExpiresAt(0L);
        this.plugin.getNationRepository().update(nation);
        this.refundNationCreationProgress(initiatingTown);
        this.clearNationCreationProgress(initiatingTown);
        this.updateTown(initiatingTown);
    }

    private void resolvePendingNationExpansion(final @NotNull RNation nation) {
        if (this.plugin.getNationInviteRepository() == null) {
            return;
        }

        final List<NationInvite> expansionInvites = this.plugin.getNationInviteRepository().findByNationUuid(nation.getNationUuid()).stream()
            .filter(invite -> invite.getInviteType() == NationInviteType.EXPANSION)
            .toList();
        if (expansionInvites.isEmpty() || expansionInvites.stream().anyMatch(NationInvite::isPending)) {
            return;
        }

        boolean membershipChanged = false;
        for (final NationInvite invite : expansionInvites) {
            if (invite.getStatus() != NationInviteStatus.ACCEPTED) {
                continue;
            }

            final RTown invitedTown = this.getTown(invite.getTargetTownUuid());
            if (invitedTown == null || Objects.equals(invitedTown.getNationUuid(), nation.getNationUuid())) {
                continue;
            }
            invitedTown.setNationUuid(nation.getNationUuid());
            this.updateTown(invitedTown);
            membershipChanged = true;
        }

        if (membershipChanged) {
            this.syncNationAlliances(this.getNationMemberTowns(nation));
        }
    }

    private @Nullable NationInvite resolveNationInvite(final @NotNull NationInvite invite) {
        if (this.plugin.getNationInviteRepository() == null || invite.getId() == null) {
            return null;
        }
        return this.plugin.getNationInviteRepository().findAll().stream()
            .filter(existingInvite -> Objects.equals(existingInvite.getId(), invite.getId()))
            .findFirst()
            .orElse(null);
    }

    private @NotNull NationInviteResponseResult respondToNationInvite(
        final @NotNull Player player,
        final @NotNull NationInvite invite,
        final boolean accept
    ) {
        this.processPendingNationTimeouts();

        final NationInvite liveInvite = this.resolveNationInvite(invite);
        final RTown town = this.getTownFor(player.getUniqueId());
        if (liveInvite == null || town == null || !liveInvite.isPending()) {
            return new NationInviteResponseResult(NationInviteResponseStatus.INVALID_TARGET, null, liveInvite);
        }
        if (!Objects.equals(town.getTownUUID(), liveInvite.getTargetTownUuid())
            || !this.hasTownPermission(player, town, TownPermissions.MANAGE_NATIONS)
            || this.plugin.getNationInviteRepository() == null) {
            return new NationInviteResponseResult(NationInviteResponseStatus.NO_PERMISSION, null, liveInvite);
        }

        final RNation nation = this.getNation(liveInvite.getNationUuid());
        if (nation == null) {
            return new NationInviteResponseResult(NationInviteResponseStatus.INVALID_TARGET, null, liveInvite);
        }

        if (accept) {
            liveInvite.accept();
        } else {
            liveInvite.decline();
        }
        this.plugin.getNationInviteRepository().update(liveInvite);
        if (nation.getStatus() == NationStatus.PENDING) {
            this.resolvePendingNationFormation(nation);
        } else if (nation.getStatus() == NationStatus.ACTIVE) {
            this.resolvePendingNationExpansion(nation);
        }
        return new NationInviteResponseResult(
            accept ? NationInviteResponseStatus.ACCEPTED : NationInviteResponseStatus.DECLINED,
            this.getNation(nation.getNationUuid()),
            liveInvite
        );
    }

    private boolean isTownEligibleForNationCreation(final @NotNull RTown town) {
        return town.getNexusLevel() >= this.getTownNationUnlockLevel()
            && this.getNationForTown(town) == null
            && this.getPendingNationCreatedBy(town) == null
            && this.getPendingNationInviteFor(town) == null
            && !this.getAlliedTowns(town).isEmpty();
    }

    private @NotNull List<RTown> getAlliedTowns(final @NotNull RTown town) {
        return this.getTowns().stream()
            .filter(otherTown -> !Objects.equals(otherTown.getTownUUID(), town.getTownUUID()))
            .filter(otherTown -> this.getEffectiveRelationshipState(town, otherTown) == TownRelationshipState.ALLIED)
            .toList();
    }

    private void syncNationAlliances(final @NotNull List<RTown> memberTowns) {
        if (this.plugin.getTownRelationshipRepository() == null) {
            return;
        }

        for (int leftIndex = 0; leftIndex < memberTowns.size(); leftIndex++) {
            final RTown leftTown = memberTowns.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < memberTowns.size(); rightIndex++) {
                final RTown rightTown = memberTowns.get(rightIndex);
                RTownRelationship relationship = this.getStoredTownRelationship(leftTown, rightTown);
                if (relationship == null) {
                    relationship = new RTownRelationship(leftTown.getTownUUID(), rightTown.getTownUUID());
                }
                relationship.setConfirmedState(TownRelationshipState.ALLIED);
                relationship.clearPendingState();
                relationship.setCooldownUntilMillis(0L);
                this.persistTownRelationship(relationship);
            }
        }
    }

    private void clearNationCreationProgress(final @NotNull RTown town) {
        for (final Integer level : this.plugin.getNationConfig().getFormationLevels().keySet()) {
            town.clearLevelRequirementProgress(this.buildProgressKeyPrefix(LevelScope.NATION_FORMATION, level));
        }
    }

    private void refundNationCreationProgress(final @NotNull RTown town) {
        final RequirementFactory requirementFactory = RequirementFactory.getInstance();
        for (final Map.Entry<Integer, LevelDefinition> levelEntry : this.plugin.getNationConfig().getFormationLevels().entrySet()) {
            final int targetLevel = levelEntry.getKey();
            for (final Map.Entry<String, Map<String, Object>> entry : levelEntry.getValue().getRequirements().entrySet()) {
                final AbstractRequirement requirement = requirementFactory.tryFromMap(
                    this.normalizeFactoryDefinition(entry.getValue())
                ).orElse(null);
                if (requirement instanceof CurrencyRequirement currencyRequirement) {
                    final String progressKey = this.buildProgressKey(LevelScope.NATION_FORMATION, targetLevel, entry.getKey());
                    final double storedAmount = town.getLevelCurrencyProgress(progressKey);
                    if (storedAmount > 0.0D) {
                        town.depositBank(this.resolveSupportedCurrencyId(currencyRequirement.getCurrencyId()), storedAmount);
                    }
                    continue;
                }
                if (!(requirement instanceof ItemRequirement itemRequirement)) {
                    continue;
                }
                final List<ItemStack> requiredItems = itemRequirement.getRequiredItems();
                for (int index = 0; index < requiredItems.size(); index++) {
                    final String progressKey = this.buildProgressKey(
                        LevelScope.NATION_FORMATION,
                        targetLevel,
                        entry.getKey() + '#' + index
                    );
                    final ItemStack storedItem = town.getLevelItemProgress(progressKey);
                    if (storedItem != null && !storedItem.isEmpty()) {
                        this.dropTownItemAtNexus(town, storedItem);
                    }
                }
            }
        }
    }

    private void grantNationCreationRewards(
        final @NotNull RNation nation,
        final @NotNull RTown capitalTown,
        final @NotNull RTown initiatingTown
    ) {
        final LevelDefinition creationDefinition = this.plugin.getNationConfig().getFormationLevelDefinition(1);
        if (creationDefinition == null || creationDefinition.getRewards().isEmpty()) {
            return;
        }

        final Map<String, String> placeholders = this.buildNationPlaceholders(nation, capitalTown, initiatingTown);
        final Map<String, Map<String, Object>> expandedRewards = this.expandDefinitionPlaceholders(
            creationDefinition.getRewards(),
            placeholders
        );
        final Player rewardPlayer = nation.getInitiatingPlayerUuid() == null || this.plugin.getServer() == null
            ? null
            : this.plugin.getServer().getPlayer(nation.getInitiatingPlayerUuid());
        if (rewardPlayer != null && rewardPlayer.isOnline()) {
            final RewardFactory<Map<String, Object>> rewardFactory = RewardFactory.getInstance();
            final List<AbstractReward> rewards = new ArrayList<>();
            for (final Map<String, Object> definition : expandedRewards.values()) {
                rewardFactory.tryFromMap(this.normalizeFactoryDefinition(definition)).ifPresent(rewards::add);
            }
            if (rewards.isEmpty()) {
                return;
            }
            try {
                RewardService.getInstance().grantAll(rewardPlayer, rewards).join();
            } catch (final Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to grant one or more nation creation rewards for " + nation.getNationName(), exception);
            }
            return;
        }

        if (this.plugin.getServer() == null) {
            return;
        }
        for (final Map<String, Object> definition : expandedRewards.values()) {
            final Map<String, Object> normalizedDefinition = this.normalizeFactoryDefinition(definition);
            final String typeId = String.valueOf(normalizedDefinition.getOrDefault("type", "")).trim().toUpperCase(Locale.ROOT);
            if (!Objects.equals(typeId, "COMMAND")) {
                LOGGER.warning(
                    "Skipping offline nation reward of type " + typeId + " for nation " + nation.getNationName()
                        + " because no initiating player is online."
                );
                continue;
            }
            final String command = String.valueOf(normalizedDefinition.getOrDefault("command", "")).trim();
            final boolean executeAsPlayer = Boolean.TRUE.equals(normalizedDefinition.get("executeAsPlayer"));
            if (command.isBlank() || executeAsPlayer) {
                continue;
            }
            this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), command.startsWith("/") ? command.substring(1) : command);
        }
    }

    private @NotNull Map<String, String> buildNationPlaceholders(final @NotNull RNation nation) {
        final RTown capitalTown = this.getTown(nation.getCapitalTownUuid());
        final Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("nation_uuid", nation.getNationUuid().toString());
        placeholders.put("nation_name", nation.getNationName());
        placeholders.put("capital_town_uuid", nation.getCapitalTownUuid().toString());
        placeholders.put("capital_town_name", capitalTown == null ? nation.getCapitalTownUuid().toString() : capitalTown.getTownName());
        placeholders.put("minimum_town_threshold", String.valueOf(nation.getMinimumTownThreshold()));
        placeholders.put("member_count", String.valueOf(this.getNationMemberTowns(nation).size()));
        return Map.copyOf(placeholders);
    }

    private @NotNull Map<String, String> buildNationPlaceholders(
        final @NotNull RNation nation,
        final @NotNull RTown capitalTown,
        final @NotNull RTown initiatingTown
    ) {
        final Map<String, String> placeholders = new LinkedHashMap<>(this.buildNationPlaceholders(nation));
        placeholders.put("capital_town_uuid", capitalTown.getTownUUID().toString());
        placeholders.put("capital_town_name", capitalTown.getTownName());
        placeholders.put("initiating_town_uuid", initiatingTown.getTownUUID().toString());
        placeholders.put("initiating_town_name", initiatingTown.getTownName());
        return Map.copyOf(placeholders);
    }

    private void dropTownItemAtNexus(final @NotNull RTown town, final @NotNull ItemStack itemStack) {
        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null || itemStack.isEmpty()) {
            return;
        }
        nexusLocation.getWorld().dropItemNaturally(nexusLocation, itemStack.clone());
    }

    private void enforceNationMinimumOrDisband(final @NotNull RNation nation) {
        final RNation liveNation = this.getNation(nation.getNationUuid());
        if (liveNation == null || !liveNation.isActive()) {
            return;
        }
        if (this.getNationMemberTowns(liveNation).size() < liveNation.getMinimumTownThreshold()) {
            this.disbandNationInternal(liveNation);
        }
    }

    private void disbandNationInternal(final @NotNull RNation nation) {
        if (this.plugin.getNationRepository() == null || this.plugin.getTownRepository() == null) {
            return;
        }

        for (final RTown town : this.getNationMemberTowns(nation)) {
            town.setNationUuid(null);
            this.plugin.getTownRepository().update(town);
        }
        if (this.plugin.getNationInviteRepository() != null) {
            for (final NationInvite invite : this.plugin.getNationInviteRepository().findByNationUuid(nation.getNationUuid())) {
                if (invite.isPending()) {
                    invite.decline();
                    this.plugin.getNationInviteRepository().update(invite);
                }
            }
        }
        nation.setStatus(NationStatus.DISBANDED);
        nation.setExpiresAt(0L);
        this.plugin.getNationRepository().update(nation);
    }

    private void updateTown(final @NotNull RTown town) {
        if (this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(town);
        }
    }

    private @NotNull TownRelationshipState getEffectiveRelationshipState(
        final @NotNull RTown leftTown,
        final @NotNull RTown rightTown
    ) {
        if (leftTown.getNationUuid() != null && Objects.equals(leftTown.getNationUuid(), rightTown.getNationUuid())) {
            final RNation nation = this.getNation(leftTown.getNationUuid());
            if (nation != null && nation.isActive()) {
                return TownRelationshipState.ALLIED;
            }
        }
        if (!this.areTownRelationshipsUnlocked(leftTown) || !this.areTownRelationshipsUnlocked(rightTown)) {
            return TownRelationshipState.NEUTRAL;
        }
        final RTownRelationship storedRelationship = this.getStoredTownRelationship(leftTown, rightTown);
        return storedRelationship == null ? TownRelationshipState.NEUTRAL : storedRelationship.getConfirmedState();
    }

    private @Nullable RTownRelationship getStoredTownRelationship(
        final @NotNull RTown leftTown,
        final @NotNull RTown rightTown
    ) {
        return this.plugin.getTownRelationshipRepository() == null
            ? null
            : this.plugin.getTownRelationshipRepository().findByTownPair(leftTown.getTownUUID(), rightTown.getTownUUID());
    }

    private @NotNull TownRelationshipViewEntry buildTownRelationshipViewEntry(
        final @NotNull RTown sourceTown,
        final @NotNull RTown targetTown,
        final @Nullable RTownRelationship relationship
    ) {
        final TownRelationshipState confirmedState = relationship == null
            ? TownRelationshipState.NEUTRAL
            : relationship.getConfirmedState();
        final boolean sourceUnlocked = this.areTownRelationshipsUnlocked(sourceTown);
        final boolean targetUnlocked = this.areTownRelationshipsUnlocked(targetTown);
        final long cooldownRemainingMillis = relationship == null
            ? 0L
            : Math.max(0L, relationship.getCooldownUntilMillis() - System.currentTimeMillis());
        return new TownRelationshipViewEntry(
            sourceTown,
            targetTown,
            confirmedState,
            this.getEffectiveRelationshipState(sourceTown, targetTown),
            relationship == null ? null : relationship.getPendingState(),
            relationship == null ? null : relationship.getPendingRequesterTownUuid(),
            cooldownRemainingMillis,
            sourceUnlocked,
            targetUnlocked
        );
    }

    private void persistTownRelationship(final @NotNull RTownRelationship relationship) {
        if (this.plugin.getTownRelationshipRepository() == null) {
            return;
        }
        if (relationship.getId() == null) {
            this.plugin.getTownRelationshipRepository().create(relationship);
            return;
        }
        this.plugin.getTownRelationshipRepository().update(relationship);
    }

    private void broadcastPendingTownRelationshipRequest(
        final @NotNull RTown sourceTown,
        final @NotNull RTown targetTown,
        final @NotNull TownRelationshipState requestedState
    ) {
        final Server server = this.plugin.getServer();
        if (server == null) {
            return;
        }
        for (final RDTPlayer member : targetTown.getMembers()) {
            final Player onlinePlayer = server.getPlayer(member.getIdentifier());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                continue;
            }
            new I18n.Builder("town_relationship_shared.broadcasts.request_received", onlinePlayer)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "source_town", sourceTown.getTownName(),
                    "target_town", targetTown.getTownName(),
                    "relationship", this.resolveRelationshipStateText(requestedState, onlinePlayer)
                ))
                .build()
                .sendMessage();
        }
    }

    private void broadcastConfirmedTownRelationshipChange(
        final @NotNull RTown firstTown,
        final @NotNull RTown secondTown,
        final @NotNull TownRelationshipState confirmedState
    ) {
        final Server server = this.plugin.getServer();
        if (server == null) {
            return;
        }
        final String translationKey = "town_relationship_shared.broadcasts.confirmed." + confirmedState.getTranslationKey();
        this.broadcastConfirmedTownRelationshipChange(server, firstTown, secondTown, confirmedState, translationKey);
        this.broadcastConfirmedTownRelationshipChange(server, secondTown, firstTown, confirmedState, translationKey);
    }

    private void broadcastConfirmedTownRelationshipChange(
        final @NotNull Server server,
        final @NotNull RTown town,
        final @NotNull RTown otherTown,
        final @NotNull TownRelationshipState confirmedState,
        final @NotNull String translationKey
    ) {
        for (final RDTPlayer member : town.getMembers()) {
            final Player onlinePlayer = server.getPlayer(member.getIdentifier());
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                continue;
            }
            new I18n.Builder(translationKey, onlinePlayer)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "other_town", otherTown.getTownName(),
                    "relationship", this.resolveRelationshipStateText(confirmedState, onlinePlayer)
                ))
                .build()
                .sendMessage();
        }
    }

    private @NotNull String resolveRelationshipStateText(
        final @NotNull TownRelationshipState relationshipState,
        final @NotNull Player player
    ) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder("town_relationship_shared.states." + relationshipState.getTranslationKey(), player)
                .build()
                .component()
        );
    }

    private @NotNull String buildProgressKeyPrefix(final @NotNull LevelScope scope, final int targetLevel) {
        final String scopeKey = switch (scope) {
            case NATION_FORMATION, NATION -> "nation";
            default -> scope.name().toLowerCase(Locale.ROOT);
        };
        return scopeKey + ".level." + Math.max(1, targetLevel) + '.';
    }

    private @NotNull String buildProgressKey(
        final @NotNull LevelScope scope,
        final int targetLevel,
        final @NotNull String entryKey
    ) {
        return this.buildProgressKeyPrefix(scope, targetLevel) + entryKey;
    }

    private void persistLevelContext(final @NotNull LevelContext context) {
        if (context.nation() != null && this.plugin.getNationRepository() != null) {
            this.plugin.getNationRepository().update(context.nation());
        }
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

    private @NotNull String formatDecimal(final double value) {
        final String formatted = String.format(Locale.ROOT, "%.2f", value);
        if (formatted.endsWith("00")) {
            return formatted.substring(0, formatted.length() - 3);
        }
        if (formatted.endsWith("0")) {
            return formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private @NotNull String formatDurationMillis(final long durationMillis) {
        final long clampedMillis = Math.max(0L, durationMillis);
        final long totalSeconds = Math.floorDiv(clampedMillis, 1000L);
        final long days = Math.floorDiv(totalSeconds, 86_400L);
        final long hours = Math.floorDiv(totalSeconds % 86_400L, 3_600L);
        final long minutes = Math.floorDiv(totalSeconds % 3_600L, 60L);
        final long seconds = totalSeconds % 60L;
        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    /**
     * Result of a chunk-type transition that may grant or remove a fuel tank.
     *
     * @param success whether the chunk type changed successfully
     * @param fuelTankGranted whether a new bound fuel tank item was granted
     * @param fuelTankRemoved whether an existing placed fuel tank was removed
     * @param droppedFuel whether stored fuel items were dropped at the tank location
     * @param seedBoxGranted whether a new bound seed-box item was granted
     * @param seedBoxRemoved whether an existing placed seed box was removed
     * @param droppedSeeds whether stored seed items were dropped at the seed-box location
     * @param salvageBlockGranted whether a new bound salvage block item was granted
     * @param salvageBlockRemoved whether an existing placed salvage block was removed
     * @param repairBlockGranted whether a new bound repair block item was granted
     * @param repairBlockRemoved whether an existing placed repair block was removed
     */
    public record ChunkTypeChangeResult(
        boolean success,
        boolean fuelTankGranted,
        boolean fuelTankRemoved,
        boolean droppedFuel,
        boolean seedBoxGranted,
        boolean seedBoxRemoved,
        boolean droppedSeeds,
        boolean salvageBlockGranted,
        boolean salvageBlockRemoved,
        boolean repairBlockGranted,
        boolean repairBlockRemoved
    ) {

        private static @NotNull ChunkTypeChangeResult failure() {
            return new ChunkTypeChangeResult(false, false, false, false, false, false, false, false, false, false, false);
        }
    }

    /**
     * Status for one Armory block direct-break recovery action.
     */
    public enum ArmoryBlockBreakStatus {
        SUCCESS,
        NO_PERMISSION,
        INVALID_CHUNK,
        NO_BLOCK,
        FAILED
    }

    /**
     * Result of one Armory block direct-break recovery action.
     *
     * @param status break outcome
     */
    public record ArmoryBlockBreakResult(@NotNull ArmoryBlockBreakStatus status) {

        private static @NotNull ArmoryBlockBreakResult success() {
            return new ArmoryBlockBreakResult(ArmoryBlockBreakStatus.SUCCESS);
        }

        private static @NotNull ArmoryBlockBreakResult noPermission() {
            return new ArmoryBlockBreakResult(ArmoryBlockBreakStatus.NO_PERMISSION);
        }

        private static @NotNull ArmoryBlockBreakResult invalidChunk() {
            return new ArmoryBlockBreakResult(ArmoryBlockBreakStatus.INVALID_CHUNK);
        }

        private static @NotNull ArmoryBlockBreakResult noBlock() {
            return new ArmoryBlockBreakResult(ArmoryBlockBreakStatus.NO_BLOCK);
        }

        private static @NotNull ArmoryBlockBreakResult failed() {
            return new ArmoryBlockBreakResult(ArmoryBlockBreakStatus.FAILED);
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

    /**
     * Status for a Farm seed-box pickup action.
     */
    public enum SeedBoxPickupStatus {
        SUCCESS,
        NO_PERMISSION,
        INVALID_CHUNK,
        NO_SEED_BOX,
        FAILED
    }

    /**
     * Result of a Farm seed-box pickup action.
     *
     * @param status pickup outcome
     * @param droppedSeeds whether stored seed items were dropped at the former seed-box location
     */
    public record SeedBoxPickupResult(
        @NotNull SeedBoxPickupStatus status,
        boolean droppedSeeds
    ) {

        private static @NotNull SeedBoxPickupResult success(final boolean droppedSeeds) {
            return new SeedBoxPickupResult(SeedBoxPickupStatus.SUCCESS, droppedSeeds);
        }

        private static @NotNull SeedBoxPickupResult noPermission() {
            return new SeedBoxPickupResult(SeedBoxPickupStatus.NO_PERMISSION, false);
        }

        private static @NotNull SeedBoxPickupResult invalidChunk() {
            return new SeedBoxPickupResult(SeedBoxPickupStatus.INVALID_CHUNK, false);
        }

        private static @NotNull SeedBoxPickupResult noSeedBox() {
            return new SeedBoxPickupResult(SeedBoxPickupStatus.NO_SEED_BOX, false);
        }

        private static @NotNull SeedBoxPickupResult failed() {
            return new SeedBoxPickupResult(SeedBoxPickupStatus.FAILED, false);
        }
    }

    private record LevelContext(
        @NotNull LevelScope scope,
        @NotNull RTown town,
        @Nullable RTownChunk chunk,
        @Nullable RNation nation,
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

    private record SeedBoxRemoval(boolean removed, boolean droppedSeeds) {

        private static @NotNull SeedBoxRemoval none() {
            return new SeedBoxRemoval(false, false);
        }
    }

    private record SalvageBlockRemoval(boolean removed) {

        private static @NotNull SalvageBlockRemoval none() {
            return new SalvageBlockRemoval(false);
        }
    }

    private record RepairBlockRemoval(boolean removed) {

        private static @NotNull RepairBlockRemoval none() {
            return new RepairBlockRemoval(false);
        }
    }

    private sealed interface ProgressAccessor permits TownProgressAccessor,
        ChunkProgressAccessor,
        PlayerCreationProgressAccessor,
        NationCreationProgressAccessor,
        NationProgressAccessor {

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

    private record NationCreationProgressAccessor(@NotNull RTown town) implements ProgressAccessor {

        @Override
        public int currentLevel() {
            return 0;
        }

        @Override
        public void setCurrentLevel(final int level) {
            // Nation creation is a one-time shared requirement flow and does not persist a level.
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

    private record NationProgressAccessor(@NotNull RNation nation) implements ProgressAccessor {

        @Override
        public int currentLevel() {
            return this.nation.getNationLevel();
        }

        @Override
        public void setCurrentLevel(final int level) {
            this.nation.setNationLevel(level);
        }

        @Override
        public double getCurrencyProgress(final @NotNull String progressKey) {
            return this.nation.getLevelCurrencyProgress(progressKey);
        }

        @Override
        public void setCurrencyProgress(final @NotNull String progressKey, final double amount) {
            this.nation.setLevelCurrencyProgress(progressKey, amount);
        }

        @Override
        public @Nullable ItemStack getItemProgress(final @NotNull String progressKey) {
            return this.nation.getLevelItemProgress(progressKey);
        }

        @Override
        public void setItemProgress(final @NotNull String progressKey, final @Nullable ItemStack itemStack) {
            this.nation.setLevelItemProgress(progressKey, itemStack);
        }

        @Override
        public void clearLevelProgress(final @NotNull String progressKeyPrefix) {
            this.nation.clearLevelRequirementProgress(progressKeyPrefix);
        }
    }
}
