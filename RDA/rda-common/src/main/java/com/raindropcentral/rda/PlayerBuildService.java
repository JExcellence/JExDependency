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

package com.raindropcentral.rda;

import com.raindropcentral.rda.database.entity.RDAPlayer;
import com.raindropcentral.rda.database.entity.RDAPlayerBuild;
import com.raindropcentral.rda.database.entity.RDASkillPreference;
import com.raindropcentral.rda.database.entity.RDASkillState;
import com.raindropcentral.rda.database.entity.RDAStatAllocation;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import com.raindropcentral.rda.database.repository.RRDAPlayerBuild;
import com.raindropcentral.rda.database.repository.RRDASkillPreference;
import com.raindropcentral.rda.database.repository.RRDASkillState;
import com.raindropcentral.rda.database.repository.RRDAStatAllocation;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns persisted RDA build state, shared mana, stat allocation, and derived ability tiers.
 *
 * <p>The service provides the single source of truth for ability points, core-stat passives,
 * player trigger preferences, and active-ability cooldowns so the rest of RDA can query one
 * coherent build model.</p>
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class PlayerBuildService {

    private static final long HUD_UPDATE_PERIOD_TICKS = 20L;
    private static final int MANA_FLUSH_INTERVAL_SECONDS = 5;

    private final JavaPlugin plugin;
    private final RDA rda;
    private final ISchedulerAdapter scheduler;
    private final RRDAPlayer playerRepository;
    private final RRDASkillState skillStateRepository;
    private final RRDAPlayerBuild playerBuildRepository;
    private final RRDAStatAllocation statAllocationRepository;
    private final RRDASkillPreference skillPreferenceRepository;
    private final StatsConfig statsConfig;
    private final NamespacedKey vitalityHealthModifierKey;
    private final NamespacedKey agilitySpeedModifierKey;
    private final Map<UUID, Map<SkillType, Long>> cooldownExpiries = new ConcurrentHashMap<>();
    private final Map<UUID, Map<SkillType, Long>> activeExpiries = new ConcurrentHashMap<>();
    private final Map<String, Double> pityMeters = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> manaBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Object> buildCreationLocks = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyBuilds = ConcurrentHashMap.newKeySet();
    private @Nullable CancellableTaskHandle manaTask;

    /**
     * Creates the shared build service.
     *
     * @param plugin owning plugin
     * @param rda active RDA runtime
     * @param playerRepository player repository
     * @param skillStateRepository skill-state repository
     * @param playerBuildRepository build repository
     * @param statAllocationRepository stat-allocation repository
     * @param skillPreferenceRepository skill-preference repository
     * @param statsConfig loaded stats configuration
     */
    public PlayerBuildService(
        final @NotNull JavaPlugin plugin,
        final @NotNull RDA rda,
        final @NotNull RRDAPlayer playerRepository,
        final @NotNull RRDASkillState skillStateRepository,
        final @NotNull RRDAPlayerBuild playerBuildRepository,
        final @NotNull RRDAStatAllocation statAllocationRepository,
        final @NotNull RRDASkillPreference skillPreferenceRepository,
        final @NotNull StatsConfig statsConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.rda = Objects.requireNonNull(rda, "rda");
        this.scheduler = Objects.requireNonNull(
            Objects.requireNonNull(this.rda.getPlatform(), "platform").getScheduler(),
            "scheduler"
        );
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.skillStateRepository = Objects.requireNonNull(skillStateRepository, "skillStateRepository");
        this.playerBuildRepository = Objects.requireNonNull(playerBuildRepository, "playerBuildRepository");
        this.statAllocationRepository = Objects.requireNonNull(statAllocationRepository, "statAllocationRepository");
        this.skillPreferenceRepository = Objects.requireNonNull(skillPreferenceRepository, "skillPreferenceRepository");
        this.statsConfig = Objects.requireNonNull(statsConfig, "statsConfig");
        this.vitalityHealthModifierKey = new NamespacedKey(this.plugin, "rda_vitality_health");
        this.agilitySpeedModifierKey = new NamespacedKey(this.plugin, "rda_agility_speed");
    }

    /**
     * Starts shared mana regeneration and HUD updates.
     */
    public void initialize() {
        if (this.manaTask != null) {
            return;
        }

        this.manaTask = this.scheduler.runRepeating(
            new Runnable() {
                private int flushCounter;

                @Override
                public void run() {
                    PlayerBuildService.this.tickOnlinePlayers();
                    this.flushCounter++;
                    if (this.flushCounter >= MANA_FLUSH_INTERVAL_SECONDS) {
                        this.flushCounter = 0;
                        PlayerBuildService.this.flushDirtyBuilds();
                    }
                }
            },
            HUD_UPDATE_PERIOD_TICKS,
            HUD_UPDATE_PERIOD_TICKS
        );
    }

    /**
     * Stops scheduled HUD work and flushes any dirty build state.
     */
    public void shutdown() {
        if (this.manaTask != null) {
            this.manaTask.cancel();
            this.manaTask = null;
        }

        this.flushDirtyBuilds();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.clearBossBar(player);
        }
    }

    /**
     * Ensures the supplied player's build rows exist and passive attributes are refreshed.
     *
     * @param player player whose build state should be provisioned
     */
    public void ensurePlayerState(final @NotNull Player player) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        this.ensureBuildRows(playerProfile);
        this.refreshPassiveAttributes(player);
    }

    /**
     * Ensures the supplied player's persisted build rows exist without touching live Bukkit state.
     *
     * @param playerUuid player whose persisted build rows should be provisioned
     */
    public void ensureStoredState(final @NotNull UUID playerUuid) {
        this.ensureBuildRows(this.getOrCreatePlayerProfile(playerUuid));
    }

    /**
     * Returns the loaded stats configuration.
     *
     * @return loaded stats configuration
     */
    public @NotNull StatsConfig getStatsConfig() {
        return this.statsConfig;
    }

    /**
     * Returns a current build snapshot for the supplied player.
     *
     * @param player player whose build should be resolved
     * @return current build snapshot
     */
    public @NotNull PlayerBuildSnapshot getBuildSnapshot(final @NotNull Player player) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        final EnumMap<CoreStatType, Integer> allocatedPoints = this.loadAllocatedPoints(playerProfile.getPlayerUuid());
        final LinkedHashMap<CoreStatType, CoreStatSnapshot> statSnapshots = new LinkedHashMap<>();
        int spentPoints = 0;
        for (final CoreStatType coreStatType : CoreStatType.values()) {
            final int points = allocatedPoints.getOrDefault(coreStatType, 0);
            spentPoints += points;
            statSnapshots.put(coreStatType, this.createCoreStatSnapshot(coreStatType, points));
        }

        final int spiPoints = allocatedPoints.getOrDefault(CoreStatType.SPI, 0);
        final double maxMana = this.statsConfig.getManaSettings().resolveMaxMana(spiPoints);
        final double regenPerSecond = this.statsConfig.getManaSettings().resolveManaRegenPerSecond(spiPoints);
        final double currentMana = Math.min(playerBuild.getCurrentMana(), maxMana);
        if (currentMana != playerBuild.getCurrentMana()) {
            playerBuild.setCurrentMana(currentMana);
            this.playerBuildRepository.update(playerBuild);
        }

        return new PlayerBuildSnapshot(
            this.calculateEarnedPoints(playerProfile.getPlayerUuid()),
            spentPoints,
            playerBuild.getUnspentPoints(),
            currentMana,
            maxMana,
            regenPerSecond,
            this.resolveManaDisplayMode(player.getUniqueId(), playerBuild),
            Map.copyOf(statSnapshots)
        );
    }

    /**
     * Returns every derived ability snapshot for one skill.
     *
     * @param player player whose ability tiers should be resolved
     * @param skillType skill whose abilities should be resolved
     * @return derived ability snapshots in configured order
     */
    public @NotNull List<AbilitySnapshot> getAbilitySnapshots(
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
        if (skillConfig == null) {
            return List.of();
        }

        final PlayerBuildSnapshot buildSnapshot = this.getBuildSnapshot(player);
        final SkillProfileSnapshot skillSnapshot = this.rda.getSkillSnapshot(skillType, player);
        final ArrayList<AbilitySnapshot> abilitySnapshots = new ArrayList<>();
        for (final SkillConfig.AbilityDefinition abilityDefinition : skillConfig.getAllAbilities()) {
            abilitySnapshots.add(this.createAbilitySnapshot(skillType, skillSnapshot, buildSnapshot, abilityDefinition));
        }
        return List.copyOf(abilitySnapshots);
    }

    /**
     * Returns every derived ability snapshot associated with the supplied stat.
     *
     * @param player player whose build should be resolved
     * @param coreStatType stat whose linked abilities should be resolved
     * @return stat-linked ability snapshots, primary-scaling abilities first
     */
    public @NotNull List<AbilitySnapshot> getAbilitySnapshotsForStat(
        final @NotNull Player player,
        final @NotNull CoreStatType coreStatType
    ) {
        final ArrayList<AbilitySnapshot> abilitySnapshots = new ArrayList<>();
        for (final SkillType skillType : this.rda.getEnabledSkills()) {
            for (final AbilitySnapshot abilitySnapshot : this.getAbilitySnapshots(player, skillType)) {
                final SkillConfig.AbilityDefinition abilityDefinition = abilitySnapshot.abilityDefinition();
                if (abilityDefinition.primaryStat() != coreStatType
                    && abilityDefinition.secondaryStat() != coreStatType) {
                    continue;
                }
                abilitySnapshots.add(abilitySnapshot);
            }
        }

        abilitySnapshots.sort(Comparator
            .comparing((AbilitySnapshot abilitySnapshot) ->
                abilitySnapshot.abilityDefinition().primaryStat() == coreStatType ? 0 : 1)
            .thenComparing(AbilitySnapshot::skillType)
            .thenComparing(abilitySnapshot -> abilitySnapshot.abilityDefinition().active() ? 1 : 0)
            .thenComparing(abilitySnapshot -> abilitySnapshot.abilityDefinition().name()));
        return List.copyOf(abilitySnapshots);
    }

    /**
     * Attempts to spend one unspent point into the supplied stat.
     *
     * @param player player spending a point
     * @param coreStatType target stat
     * @return {@code true} when the point was spent
     */
    public boolean spendPoint(final @NotNull Player player, final @NotNull CoreStatType coreStatType) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        if (playerBuild.getUnspentPoints() <= 0) {
            return false;
        }

        final RDAStatAllocation allocation = this.getOrCreateAllocation(playerProfile, coreStatType);
        allocation.setAllocatedPoints(allocation.getAllocatedPoints() + 1);
        playerBuild.setUnspentPoints(playerBuild.getUnspentPoints() - 1);
        this.statAllocationRepository.update(allocation);
        this.playerBuildRepository.update(playerBuild);
        this.refreshPassiveAttributes(player);
        this.markDirty(playerProfile.getPlayerUuid());
        return true;
    }

    /**
     * Resets every allocated point through the configured respec policy.
     *
     * <p>The manual respec tax is applied to total skill progression first, then the player's
     * global earned-point pool is recalculated from the taxed skills before all points become
     * available for reallocation again.</p>
     *
     * @param player player requesting a manual respec
     * @return {@code true} when at least one spent point was reset
     */
    public boolean respecAll(final @NotNull Player player) {
        final long now = System.currentTimeMillis();
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        if (playerBuild.getRespecAvailableAtEpochMillis() > now) {
            return false;
        }

        final List<RDAStatAllocation> allocations = this.statAllocationRepository.findAllByPlayer(player.getUniqueId());
        int resetPoints = 0;
        for (final RDAStatAllocation allocation : allocations) {
            resetPoints += allocation.getAllocatedPoints();
        }
        if (resetPoints <= 0) {
            return false;
        }

        this.applyManualRespecXpTax(player.getUniqueId());
        for (final RDAStatAllocation allocation : allocations) {
            allocation.setAllocatedPoints(0);
            this.statAllocationRepository.update(allocation);
        }

        playerBuild.setUnspentPoints(this.calculateEarnedPoints(player.getUniqueId()));
        playerBuild.setRespecAvailableAtEpochMillis(
            now + this.statsConfig.getRespecSettings().cooldownSeconds() * 1000L
        );
        this.playerBuildRepository.update(playerBuild);
        this.refreshPassiveAttributes(player);
        this.markDirty(player.getUniqueId());
        return true;
    }

    /**
     * Rotates the player's selected mana HUD mode.
     *
     * @param player player whose HUD mode should be updated
     * @return newly selected HUD mode
     */
    public @NotNull ManaDisplayMode cycleManaDisplayMode(final @NotNull Player player) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        final ManaDisplayMode currentMode = this.resolveManaDisplayMode(player.getUniqueId(), playerBuild);
        if (this.rda.getManaBossBarIntegration() != null) {
            return this.rda.getManaBossBarIntegration().cycleDisplayMode(player, currentMode);
        }

        final ManaDisplayMode[] values = ManaDisplayMode.values();
        final ManaDisplayMode nextMode = values[(currentMode.ordinal() + 1) % values.length];
        playerBuild.setManaDisplayMode(nextMode.name());
        this.playerBuildRepository.update(playerBuild);
        this.updateHud(player, this.getBuildSnapshot(player));
        return nextMode;
    }

    /**
     * Rotates the player's selected activation mode for the supplied skill.
     *
     * @param player player whose preference should be updated
     * @param skillType skill whose activation mode should be rotated
     * @return newly selected activation mode
     */
    public @NotNull ActivationMode cycleActivationMode(
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        this.ensureBuildRows(playerProfile);
        final RDASkillPreference skillPreference = this.getOrCreateSkillPreference(playerProfile, skillType);
        final List<ActivationMode> allowedModes = this.getAllowedActivationModes(skillType);
        if (allowedModes.isEmpty()) {
            skillPreference.setActivationMode(ActivationMode.COMMAND.name());
            this.skillPreferenceRepository.update(skillPreference);
            return ActivationMode.COMMAND;
        }

        final ActivationMode currentMode = this.resolveActivationMode(skillPreference);
        final int currentIndex = allowedModes.indexOf(currentMode);
        final ActivationMode nextMode = allowedModes.get((Math.max(currentIndex, 0) + 1) % allowedModes.size());
        skillPreference.setActivationMode(nextMode.name());
        this.skillPreferenceRepository.update(skillPreference);
        return nextMode;
    }

    /**
     * Returns the player's selected activation mode for the supplied skill.
     *
     * @param player player whose preference should be resolved
     * @param skillType skill whose preference should be resolved
     * @return selected activation mode
     */
    public @NotNull ActivationMode getActivationMode(
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        this.ensureBuildRows(playerProfile);
        return this.resolveActivationMode(this.getOrCreateSkillPreference(playerProfile, skillType));
    }

    /**
     * Returns the allowed activation modes for the supplied skill after server and skill filtering.
     *
     * @param skillType skill whose allowed modes should be resolved
     * @return allowed activation modes
     */
    public @NotNull List<ActivationMode> getAllowedActivationModesForSkill(final @NotNull SkillType skillType) {
        return this.getAllowedActivationModes(skillType);
    }

    /**
     * Grants newly earned ability points after a skill crosses one or more configured thresholds.
     *
     * @param player player who gained the skill levels
     * @param skillType progressed skill
     * @param previousLevel previous internal skill level
     * @param newLevel new internal skill level
     * @return number of newly granted points
     */
    public int grantLevelThresholdPoints(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final int previousLevel,
        final int newLevel
    ) {
        final int interval = this.resolveAbilityPointInterval(skillType);
        final int previousPoints = Math.max(0, previousLevel) / interval;
        final int currentPoints = Math.max(0, newLevel) / interval;
        final int grantedPoints = Math.max(0, currentPoints - previousPoints);
        if (grantedPoints <= 0) {
            return 0;
        }

        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        playerBuild.setUnspentPoints(playerBuild.getUnspentPoints() + grantedPoints);
        this.playerBuildRepository.update(playerBuild);
        this.markDirty(player.getUniqueId());
        return grantedPoints;
    }

    /**
     * Previews the full-build reset and point recalculation caused by a skill prestige.
     *
     * @param player player previewing the prestige
     * @param skillType skill being prestiged
     * @return derived prestige adjustment preview
     */
    public @NotNull PrestigeAdjustmentPreview previewPrestigeAdjustment(
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        this.ensureBuildRows(playerProfile);
        final int currentContribution = this.resolveCurrentSkillContribution(player.getUniqueId(), skillType);
        final int earnedPointsAfterPrestige = Math.max(0, this.calculateEarnedPoints(player.getUniqueId()) - currentContribution);
        final int resetPoints = this.loadAllocatedPoints(player.getUniqueId()).values().stream().mapToInt(Integer::intValue).sum();

        return new PrestigeAdjustmentPreview(
            skillType,
            resetPoints,
            earnedPointsAfterPrestige,
            earnedPointsAfterPrestige
        );
    }

    /**
     * Applies the supplied prestige adjustment after the owning skill has been reset.
     *
     * @param player player whose build should be updated
     * @param preview precomputed prestige adjustment
     */
    public void applyPrestigeAdjustment(
        final @NotNull Player player,
        final @NotNull PrestigeAdjustmentPreview preview
    ) {
        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        for (final RDAStatAllocation allocation : this.statAllocationRepository.findAllByPlayer(player.getUniqueId())) {
            allocation.setAllocatedPoints(0);
            this.statAllocationRepository.update(allocation);
        }

        playerBuild.setUnspentPoints(preview.unspentPointsAfterPrestige());
        this.playerBuildRepository.update(playerBuild);
        this.refreshPassiveAttributes(player);
        this.markDirty(player.getUniqueId());
    }

    /**
     * Attempts to cast the supplied skill's active ability using the requested trigger source.
     *
     * @param player player attempting to cast
     * @param skillType skill whose active ability should be cast
     * @param activationMode trigger source used for the cast
     * @return {@code true} when the cast succeeded
     */
    public boolean cast(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final @NotNull ActivationMode activationMode
    ) {
        final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
        if (skillConfig == null || !skillConfig.isEnabled()) {
            return false;
        }

        final SkillConfig.AbilityDefinition activeAbility = skillConfig.getActiveAbility();
        if (activeAbility == null || activeAbility.activeConfig() == null) {
            return false;
        }

        final ActivationMode preferredMode = this.getActivationMode(player, skillType);
        if (preferredMode != activationMode) {
            return false;
        }

        final List<ActivationMode> allowedModes = this.getAllowedActivationModes(skillType);
        if (!allowedModes.contains(activationMode)) {
            return false;
        }

        final AbilitySnapshot abilitySnapshot = this.getAbilitySnapshots(player, skillType)
            .stream()
            .filter(snapshot -> snapshot.abilityDefinition().active())
            .findFirst()
            .orElse(null);
        if (abilitySnapshot == null || !abilitySnapshot.unlocked()) {
            return false;
        }

        final long remainingCooldownSeconds = this.getRemainingCooldownSeconds(player, skillType);
        if (remainingCooldownSeconds > 0L) {
            new I18n.Builder("ra_build.message.cast_cooldown", player)
                .includePrefix()
                .withPlaceholders(this.mergeSkillPlaceholders(player, skillType, Map.of(
                    "cooldown_seconds", remainingCooldownSeconds
                )))
                .build()
                .sendMessage();
            return false;
        }

        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(playerProfile);
        if (playerBuild.getCurrentMana() < activeAbility.activeConfig().manaCost()) {
            new I18n.Builder("ra_build.message.cast_no_mana", player)
                .includePrefix()
                .withPlaceholders(this.mergeSkillPlaceholders(player, skillType, Map.of(
                    "mana_cost", activeAbility.activeConfig().manaCost()
                )))
                .build()
                .sendMessage();
            return false;
        }

        playerBuild.setCurrentMana(playerBuild.getCurrentMana() - activeAbility.activeConfig().manaCost());
        this.playerBuildRepository.update(playerBuild);
        this.markDirty(player.getUniqueId());

        final long now = System.currentTimeMillis();
        this.cooldownExpiries
            .computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(SkillType.class))
            .put(skillType, now + activeAbility.activeConfig().cooldownSeconds() * 1000L);
        if (activeAbility.activeConfig().durationSeconds() > 0) {
            this.activeExpiries
                .computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(SkillType.class))
                .put(skillType, now + activeAbility.activeConfig().durationSeconds() * 1000L);
        }

        new I18n.Builder("ra_build.message.cast_success", player)
            .includePrefix()
            .withPlaceholders(this.mergeSkillPlaceholders(player, skillType, Map.of(
                "mana_cost", activeAbility.activeConfig().manaCost(),
                "duration_seconds", activeAbility.activeConfig().durationSeconds(),
                "cooldown_seconds", activeAbility.activeConfig().cooldownSeconds()
            )))
            .build()
            .sendMessage();
        this.updateHud(player, this.getBuildSnapshot(player));
        return true;
    }

    /**
     * Returns the remaining cooldown for the supplied skill's active ability.
     *
     * @param player player whose cooldown should be resolved
     * @param skillType active skill
     * @return remaining cooldown seconds, or {@code 0} when ready
     */
    public long getRemainingCooldownSeconds(final @NotNull Player player, final @NotNull SkillType skillType) {
        final Map<SkillType, Long> skillCooldowns = this.cooldownExpiries.get(player.getUniqueId());
        if (skillCooldowns == null) {
            return 0L;
        }

        final long remainingMillis = skillCooldowns.getOrDefault(skillType, 0L) - System.currentTimeMillis();
        return remainingMillis <= 0L ? 0L : (long) Math.ceil(remainingMillis / 1000.0D);
    }

    /**
     * Reports whether the supplied skill's active ability is currently running.
     *
     * @param player player whose active state should be resolved
     * @param skillType skill to probe
     * @return {@code true} when the active ability is currently running
     */
    public boolean isSkillActive(final @NotNull Player player, final @NotNull SkillType skillType) {
        final Map<SkillType, Long> skillExpiries = this.activeExpiries.get(player.getUniqueId());
        if (skillExpiries == null) {
            return false;
        }

        final long expiry = skillExpiries.getOrDefault(skillType, 0L);
        return expiry > System.currentTimeMillis();
    }

    /**
     * Returns the resolved potency for the supplied ability key.
     *
     * @param player player whose build should be resolved
     * @param skillType owning skill
     * @param abilityKey stable ability key
     * @return resolved potency, or {@code 0} when the ability is missing or locked
     */
    public double getAbilityPotency(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final @NotNull String abilityKey
    ) {
        for (final AbilitySnapshot abilitySnapshot : this.getAbilitySnapshots(player, skillType)) {
            if (!abilitySnapshot.abilityDefinition().key().equalsIgnoreCase(abilityKey)) {
                continue;
            }
            return abilitySnapshot.unlocked() ? abilitySnapshot.potency() : 0.0D;
        }
        return 0.0D;
    }

    /**
     * Increments the named pity meter and reports whether it triggered this time.
     *
     * @param player player owning the pity meter
     * @param meterKey stable pity meter key
     * @param incrementPercent pity progress to add in percent units
     * @return {@code true} when the pity meter triggered
     */
    public boolean triggerPityMeter(
        final @NotNull Player player,
        final @NotNull String meterKey,
        final double incrementPercent
    ) {
        final String compositeKey = player.getUniqueId() + ":" + meterKey.toLowerCase(Locale.ROOT);
        final double updated = this.pityMeters.getOrDefault(compositeKey, 0.0D) + Math.max(0.0D, incrementPercent);
        if (updated < 100.0D) {
            this.pityMeters.put(compositeKey, updated);
            return false;
        }

        this.pityMeters.put(compositeKey, updated - 100.0D);
        return true;
    }

    /**
     * Returns the passive value currently resolved for the supplied core stat.
     *
     * @param player player whose build should be resolved
     * @param coreStatType stat to resolve
     * @return resolved passive value
     */
    public double getPassiveValue(final @NotNull Player player, final @NotNull CoreStatType coreStatType) {
        return this.getBuildSnapshot(player).statSnapshots().get(coreStatType).passiveValue();
    }

    /**
     * Clears transient HUD state for a departing player.
     *
     * @param player departing player
     */
    public void handlePlayerQuit(final @NotNull Player player) {
        this.clearHud(player);
        this.flushDirtyBuild(player.getUniqueId());
    }

    /**
     * Refreshes the live mana HUD for the supplied player using the current centralized
     * preferences.
     *
     * @param player player whose HUD should be refreshed
     */
    public void refreshHud(final @NotNull Player player) {
        this.updateHud(player, this.getBuildSnapshot(player));
    }

    /**
     * Clears all live mana HUD surfaces for the supplied player.
     *
     * @param player player whose HUD should be cleared
     */
    public void clearHud(final @NotNull Player player) {
        this.clearBossBar(player);
        player.sendActionBar(Component.empty());
    }

    /**
     * Refreshes attribute-backed stat passives for the supplied player.
     *
     * @param player player whose passive attributes should be refreshed
     */
    public void refreshPassiveAttributes(final @NotNull Player player) {
        final PlayerBuildSnapshot buildSnapshot = this.getBuildSnapshot(player);
        this.refreshMaxHealthModifier(player, buildSnapshot.statSnapshots().get(CoreStatType.VIT).passiveValue());
        this.refreshMovementSpeedModifier(player, buildSnapshot.statSnapshots().get(CoreStatType.AGI).passiveValue());
    }

    private void tickOnlinePlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.scheduler.runAtEntity(player, () -> this.tickPlayer(player));
        }
    }

    private void tickPlayer(final @NotNull Player player) {
        if (!player.isOnline()) {
            this.clearBossBar(player);
            return;
        }

        final PlayerBuildSnapshot buildSnapshot = this.getBuildSnapshot(player);
        final RDAPlayerBuild playerBuild = this.ensureBuildRows(this.getOrCreatePlayerProfile(player.getUniqueId()));
        final double regeneratedMana = Math.min(
            buildSnapshot.maxMana(),
            playerBuild.getCurrentMana() + buildSnapshot.manaRegenPerSecond()
        );
        if (regeneratedMana != playerBuild.getCurrentMana()) {
            playerBuild.setCurrentMana(regeneratedMana);
            this.markDirty(player.getUniqueId());
        }

        this.expireEndedActives(player);
        this.refreshTimedBuffs(player);
        this.updateHud(player, new PlayerBuildSnapshot(
            buildSnapshot.earnedPoints(),
            buildSnapshot.spentPoints(),
            buildSnapshot.unspentPoints(),
            playerBuild.getCurrentMana(),
            buildSnapshot.maxMana(),
            buildSnapshot.manaRegenPerSecond(),
            buildSnapshot.manaDisplayMode(),
            buildSnapshot.statSnapshots()
        ));
    }

    private void expireEndedActives(final @NotNull Player player) {
        final Map<SkillType, Long> skillExpiries = this.activeExpiries.get(player.getUniqueId());
        if (skillExpiries == null) {
            return;
        }

        final long now = System.currentTimeMillis();
        skillExpiries.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (skillExpiries.isEmpty()) {
            this.activeExpiries.remove(player.getUniqueId());
        }
    }

    private void refreshTimedBuffs(final @NotNull Player player) {
        if (this.isSkillActive(player, SkillType.DEFENSE)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.RESISTANCE,
                40,
                0,
                true,
                false,
                false
            ));
        }
        if (this.isSkillActive(player, SkillType.AGILITY)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                40,
                1,
                true,
                false,
                false
            ));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                40,
                0,
                true,
                false,
                false
            ));
        }
        if (this.isSkillActive(player, SkillType.FIGHTING)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH,
                40,
                0,
                true,
                false,
                false
            ));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                40,
                0,
                true,
                false,
                false
            ));
        }
        if (this.isSkillActive(player, SkillType.MINING) || this.isSkillActive(player, SkillType.EXCAVATION)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.HASTE,
                40,
                1,
                true,
                false,
                false
            ));
        }
    }

    private void updateHud(final @NotNull Player player, final @NotNull PlayerBuildSnapshot buildSnapshot) {
        if (this.rda.getManaBossBarIntegration() != null
            && !this.rda.getManaBossBarIntegration().isEnabled(player.getUniqueId())) {
            this.clearBossBar(player);
            return;
        }

        final Component hudComponent = new I18n.Builder("ra_build.hud.mana", player)
            .withPlaceholders(Map.of(
                "current_mana", String.format(Locale.ROOT, "%.1f", buildSnapshot.currentMana()),
                "max_mana", String.format(Locale.ROOT, "%.1f", buildSnapshot.maxMana())
            ))
            .build()
            .component();
        switch (buildSnapshot.manaDisplayMode()) {
            case ACTION_BAR -> {
                this.clearBossBar(player);
                player.sendActionBar(hudComponent);
            }
            case BOSS_BAR -> this.showBossBar(player, hudComponent, buildSnapshot);
            case ALWAYS_VISIBLE -> {
                this.showBossBar(player, hudComponent, buildSnapshot);
                player.sendActionBar(hudComponent);
            }
            case MENUS_ONLY -> this.clearBossBar(player);
        }
    }

    private void showBossBar(
        final @NotNull Player player,
        final @NotNull Component hudComponent,
        final @NotNull PlayerBuildSnapshot buildSnapshot
    ) {
        final float progress = buildSnapshot.maxMana() <= 0.0D
            ? 0.0F
            : (float) Math.max(0.0D, Math.min(1.0D, buildSnapshot.currentMana() / buildSnapshot.maxMana()));
        final BossBar bossBar = this.manaBossBars.computeIfAbsent(
            player.getUniqueId(),
            ignored -> BossBar.bossBar(
                hudComponent,
                progress,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
            )
        );
        bossBar.name(hudComponent);
        bossBar.progress(progress);
        player.showBossBar(bossBar);
    }

    private void clearBossBar(final @NotNull Player player) {
        final BossBar bossBar = this.manaBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private void flushDirtyBuilds() {
        for (final UUID playerUuid : new HashSet<>(this.dirtyBuilds)) {
            this.flushDirtyBuild(playerUuid);
        }
    }

    private void flushDirtyBuild(final @NotNull UUID playerUuid) {
        final RDAPlayerBuild playerBuild = this.playerBuildRepository.findByPlayer(playerUuid);
        if (playerBuild != null) {
            this.playerBuildRepository.updateAsync(playerBuild);
        }
        this.dirtyBuilds.remove(playerUuid);
    }

    private void markDirty(final @NotNull UUID playerUuid) {
        this.dirtyBuilds.add(playerUuid);
    }

    private int calculateEarnedPoints(final @NotNull UUID playerUuid) {
        int earnedPoints = 0;
        for (final SkillType skillType : SkillType.values()) {
            earnedPoints += this.resolveCurrentSkillContribution(playerUuid, skillType);
        }
        return earnedPoints;
    }

    private int resolveCurrentSkillContribution(final @NotNull UUID playerUuid, final @NotNull SkillType skillType) {
        final RDASkillState skillState = this.skillStateRepository.findByPlayerAndSkill(playerUuid, skillType);
        if (skillState == null) {
            return 0;
        }
        return Math.max(0, skillState.getLevel()) / this.resolveAbilityPointInterval(skillType);
    }

    private int resolveAbilityPointInterval(final @NotNull SkillType skillType) {
        final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
        if (skillConfig != null && skillConfig.getAbilityPointIntervalOverride() > 0) {
            return skillConfig.getAbilityPointIntervalOverride();
        }
        return this.statsConfig.getAbilityPointInterval(skillType);
    }

    private @NotNull RDAPlayer getOrCreatePlayerProfile(final @NotNull UUID playerUuid) {
        return this.playerRepository.findOrCreateByPlayer(playerUuid);
    }

    private @NotNull RDAPlayerBuild ensureBuildRows(final @NotNull RDAPlayer playerProfile) {
        final RDAPlayerBuild playerBuild = this.getOrCreatePlayerBuild(playerProfile);

        for (final CoreStatType coreStatType : CoreStatType.values()) {
            this.getOrCreateAllocation(playerProfile, coreStatType);
        }
        for (final SkillType skillType : SkillType.values()) {
            this.getOrCreateSkillPreference(playerProfile, skillType);
        }

        final int spiPoints = this.loadAllocatedPoints(playerProfile.getPlayerUuid()).getOrDefault(CoreStatType.SPI, 0);
        final double maxMana = this.statsConfig.getManaSettings().resolveMaxMana(spiPoints);
        if (playerBuild.getCurrentMana() <= 0.0D) {
            playerBuild.setCurrentMana(maxMana);
        } else {
            playerBuild.setCurrentMana(Math.min(playerBuild.getCurrentMana(), maxMana));
        }

        return this.playerBuildRepository.update(playerBuild);
    }

    private @NotNull RDAPlayerBuild getOrCreatePlayerBuild(final @NotNull RDAPlayer playerProfile) {
        final UUID playerUuid = playerProfile.getPlayerUuid();
        final RDAPlayerBuild existingBuild = this.playerBuildRepository.findByPlayer(playerUuid);
        if (existingBuild != null) {
            return existingBuild;
        }

        final Object creationLock = this.buildCreationLocks.computeIfAbsent(playerUuid, ignored -> new Object());
        synchronized (creationLock) {
            try {
                final RDAPlayerBuild concurrentBuild = this.playerBuildRepository.findByPlayer(playerUuid);
                if (concurrentBuild != null) {
                    return concurrentBuild;
                }

                final RDAPlayerBuild createdBuild = new RDAPlayerBuild(playerProfile);
                createdBuild.setUnspentPoints(this.calculateEarnedPoints(playerUuid));
                createdBuild.setManaDisplayMode(this.statsConfig.getManaSettings().defaultDisplayMode().name());
                try {
                    return this.playerBuildRepository.create(createdBuild);
                } catch (final RuntimeException exception) {
                    final RDAPlayerBuild persistedBuild = this.playerBuildRepository.findByPlayer(playerUuid);
                    if (persistedBuild != null) {
                        return persistedBuild;
                    }
                    throw exception;
                }
            } finally {
                this.buildCreationLocks.remove(playerUuid, creationLock);
            }
        }
    }

    private @NotNull RDAStatAllocation getOrCreateAllocation(
        final @NotNull RDAPlayer playerProfile,
        final @NotNull CoreStatType coreStatType
    ) {
        final RDAStatAllocation existingAllocation = this.statAllocationRepository.findByPlayerAndStat(
            playerProfile.getPlayerUuid(),
            coreStatType
        );
        if (existingAllocation != null) {
            return existingAllocation;
        }

        try {
            return this.statAllocationRepository.create(new RDAStatAllocation(playerProfile, coreStatType));
        } catch (final RuntimeException exception) {
            final RDAStatAllocation concurrentAllocation = this.statAllocationRepository.findByPlayerAndStat(
                playerProfile.getPlayerUuid(),
                coreStatType
            );
            if (concurrentAllocation != null) {
                return concurrentAllocation;
            }
            throw exception;
        }
    }

    private @NotNull RDASkillPreference getOrCreateSkillPreference(
        final @NotNull RDAPlayer playerProfile,
        final @NotNull SkillType skillType
    ) {
        final RDASkillPreference existingPreference = this.skillPreferenceRepository.findByPlayerAndSkill(
            playerProfile.getPlayerUuid(),
            skillType
        );
        if (existingPreference != null) {
            final ActivationMode resolvedMode = this.resolveActivationMode(existingPreference);
            if (!this.getAllowedActivationModes(skillType).contains(resolvedMode)) {
                final List<ActivationMode> allowedModes = this.getAllowedActivationModes(skillType);
                existingPreference.setActivationMode(
                    allowedModes.isEmpty() ? ActivationMode.COMMAND.name() : allowedModes.getFirst().name()
                );
                return this.skillPreferenceRepository.update(existingPreference);
            }
            return existingPreference;
        }

        final RDASkillPreference createdPreference = new RDASkillPreference(playerProfile, skillType);
        final List<ActivationMode> allowedModes = this.getAllowedActivationModes(skillType);
        if (!allowedModes.isEmpty()) {
            createdPreference.setActivationMode(allowedModes.getFirst().name());
        }
        try {
            return this.skillPreferenceRepository.create(createdPreference);
        } catch (final RuntimeException exception) {
            final RDASkillPreference concurrentPreference = this.skillPreferenceRepository.findByPlayerAndSkill(
                playerProfile.getPlayerUuid(),
                skillType
            );
            if (concurrentPreference != null) {
                return concurrentPreference;
            }
            throw exception;
        }
    }

    private @NotNull EnumMap<CoreStatType, Integer> loadAllocatedPoints(final @NotNull UUID playerUuid) {
        final EnumMap<CoreStatType, Integer> allocatedPoints = new EnumMap<>(CoreStatType.class);
        for (final RDAStatAllocation allocation : this.statAllocationRepository.findAllByPlayer(playerUuid)) {
            for (final CoreStatType coreStatType : CoreStatType.values()) {
                if (!coreStatType.getId().equalsIgnoreCase(allocation.getStatId())) {
                    continue;
                }
                allocatedPoints.put(coreStatType, allocation.getAllocatedPoints());
                break;
            }
        }
        return allocatedPoints;
    }

    private void applyManualRespecXpTax(final @NotNull UUID playerUuid) {
        final int pointTaxPercent = this.statsConfig.getRespecSettings().pointTaxPercent();
        if (pointTaxPercent <= 0) {
            return;
        }

        for (final SkillType skillType : SkillType.values()) {
            final RDASkillState skillState = this.skillStateRepository.findByPlayerAndSkill(playerUuid, skillType);
            if (skillState == null) {
                continue;
            }

            final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
            if (skillConfig == null) {
                continue;
            }

            final long totalProgressXp = SkillProgressionService.resolveTotalProgressXp(skillState, skillConfig);
            final long taxedProgressXp = (long) Math.floor(totalProgressXp * (1.0D - pointTaxPercent / 100.0D));
            if (taxedProgressXp == totalProgressXp) {
                continue;
            }

            SkillProgressionService.applyTotalProgressXp(skillState, taxedProgressXp, skillConfig);
            this.skillStateRepository.update(skillState);
        }
    }

    private @NotNull CoreStatSnapshot createCoreStatSnapshot(
        final @NotNull CoreStatType coreStatType,
        final int allocatedPoints
    ) {
        final StatsConfig.StatDefinition statDefinition = this.statsConfig.getStatDefinition(coreStatType);
        return new CoreStatSnapshot(
            coreStatType,
            allocatedPoints,
            statDefinition.resolvePassiveValue(allocatedPoints),
            statDefinition.passiveLabel(),
            statDefinition.passiveUnit(),
            statDefinition.loreDescription()
        );
    }

    private @NotNull AbilitySnapshot createAbilitySnapshot(
        final @NotNull SkillType skillType,
        final @NotNull SkillProfileSnapshot skillSnapshot,
        final @NotNull PlayerBuildSnapshot buildSnapshot,
        final @NotNull SkillConfig.AbilityDefinition abilityDefinition
    ) {
        final int primaryPoints = buildSnapshot.statSnapshots()
            .getOrDefault(abilityDefinition.primaryStat(), this.createCoreStatSnapshot(abilityDefinition.primaryStat(), 0))
            .allocatedPoints();
        final int secondaryPoints = abilityDefinition.secondaryStat() == null
            ? 0
            : buildSnapshot.statSnapshots()
                .getOrDefault(abilityDefinition.secondaryStat(), this.createCoreStatSnapshot(abilityDefinition.secondaryStat(), 0))
                .allocatedPoints();
        final SkillConfig.AbilityTierDefinition currentTier =
            abilityDefinition.resolveTier(skillSnapshot.internalLevel(), primaryPoints);
        int currentTierIndex = -1;
        double potency = 0.0D;
        if (currentTier != null) {
            currentTierIndex = abilityDefinition.tiers().indexOf(currentTier);
            potency = currentTier.resolvePotency(primaryPoints, secondaryPoints);
        }

        int nextRequiredSkillLevel = 0;
        int nextRequiredStatPoints = 0;
        for (final SkillConfig.AbilityTierDefinition tierDefinition : abilityDefinition.tiers()) {
            if (tierDefinition == currentTier) {
                continue;
            }
            if (currentTier != null && abilityDefinition.tiers().indexOf(tierDefinition) <= currentTierIndex) {
                continue;
            }
            nextRequiredSkillLevel = tierDefinition.requiredSkillLevel();
            nextRequiredStatPoints = tierDefinition.requiredStatPoints();
            break;
        }

        return new AbilitySnapshot(
            skillType,
            abilityDefinition,
            currentTier != null,
            currentTierIndex,
            currentTier,
            potency,
            primaryPoints,
            secondaryPoints,
            nextRequiredSkillLevel,
            nextRequiredStatPoints
        );
    }

    private @NotNull List<ActivationMode> getAllowedActivationModes(final @NotNull SkillType skillType) {
        final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
        if (skillConfig == null || skillConfig.getActiveAbility() == null || skillConfig.getActiveAbility().activeConfig() == null) {
            return List.of(ActivationMode.COMMAND);
        }

        final EnumSet<ActivationMode> allowedModes = EnumSet.copyOf(this.statsConfig.getAllowedActivationModes());
        allowedModes.retainAll(skillConfig.getActiveAbility().activeConfig().allowedActivationModes());
        if (allowedModes.isEmpty()) {
            allowedModes.add(ActivationMode.COMMAND);
        }
        return List.copyOf(allowedModes);
    }

    private @NotNull ActivationMode resolveActivationMode(final @NotNull RDASkillPreference skillPreference) {
        try {
            return ActivationMode.valueOf(skillPreference.getActivationMode().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return ActivationMode.COMMAND;
        }
    }

    private @NotNull ManaDisplayMode resolveManaDisplayMode(
        final @NotNull UUID playerUuid,
        final @NotNull RDAPlayerBuild playerBuild
    ) {
        if (this.rda.getManaBossBarIntegration() != null) {
            return this.rda.getManaBossBarIntegration().resolveDisplayMode(playerUuid, playerBuild);
        }

        try {
            return ManaDisplayMode.valueOf(playerBuild.getManaDisplayMode().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException exception) {
            return this.statsConfig.getManaSettings().defaultDisplayMode();
        }
    }

    private @NotNull Map<String, Object> mergeSkillPlaceholders(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final @NotNull Map<String, Object> extraPlaceholders
    ) {
        final LinkedHashMap<String, Object> placeholders = new LinkedHashMap<>(skillType.getPlaceholders(player));
        placeholders.putAll(extraPlaceholders);
        return placeholders;
    }

    private void refreshMaxHealthModifier(final @NotNull Player player, final double bonusHealth) {
        final AttributeInstance attribute = this.resolveAttribute(player, "MAX_HEALTH", "GENERIC_MAX_HEALTH");
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(this.vitalityHealthModifierKey);
        if (bonusHealth > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(
                this.vitalityHealthModifierKey,
                bonusHealth,
                AttributeModifier.Operation.ADD_NUMBER
            ));
        }

        final double currentMaxHealth = attribute.getValue();
        if (player.getHealth() > currentMaxHealth) {
            player.setHealth(currentMaxHealth);
        }
    }

    private void refreshMovementSpeedModifier(final @NotNull Player player, final double bonusScalar) {
        final AttributeInstance attribute = this.resolveAttribute(player, "MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(this.agilitySpeedModifierKey);
        if (bonusScalar > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(
                this.agilitySpeedModifierKey,
                bonusScalar,
                AttributeModifier.Operation.ADD_SCALAR
            ));
        }
    }

    private @Nullable AttributeInstance resolveAttribute(
        final @NotNull Player player,
        final @NotNull String... fieldNames
    ) {
        for (final String fieldName : fieldNames) {
            try {
                return player.getAttribute((Attribute) Attribute.class.getField(fieldName).get(null));
            } catch (final ReflectiveOperationException | ExceptionInInitializerError ignored) {
                // Try the next compatible field name.
            }
        }
        return null;
    }
}
