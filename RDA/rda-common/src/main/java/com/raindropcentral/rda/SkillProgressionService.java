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
import com.raindropcentral.rda.database.entity.RDASkillState;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import com.raindropcentral.rda.database.repository.RRDASkillState;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles generic skill XP gain, level progression, soft-cap behavior, and prestige.
 *
 * <p>The service owns the runtime calculations for a skill progression track and exposes
 * view-ready snapshots so the GUI can stay synchronized with persisted player data.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class SkillProgressionService {

    private final RDA rda;
    private final RRDAPlayer playerRepository;
    private final RRDASkillState skillStateRepository;
    private final SkillType skillType;
    private final SkillConfig skillConfig;

    /**
     * Creates a skill progression service.
     *
     * @param rda active plugin runtime
     * @param playerRepository player repository
     * @param skillStateRepository child skill-state repository
     * @param skillType owning skill type
     * @param skillConfig loaded skill configuration
     */
    public SkillProgressionService(
        final @NotNull RDA rda,
        final @NotNull RRDAPlayer playerRepository,
        final @NotNull RRDASkillState skillStateRepository,
        final @NotNull SkillType skillType,
        final @NotNull SkillConfig skillConfig
    ) {
        this.rda = Objects.requireNonNull(rda, "rda");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.skillStateRepository = Objects.requireNonNull(skillStateRepository, "skillStateRepository");
        this.skillType = Objects.requireNonNull(skillType, "skillType");
        this.skillConfig = Objects.requireNonNull(skillConfig, "skillConfig");
    }

    /**
     * Returns the owning skill type.
     *
     * @return owning skill type
     */
    public @NotNull SkillType getSkillType() {
        return this.skillType;
    }

    /**
     * Returns the loaded skill configuration.
     *
     * @return skill configuration
     */
    public @NotNull SkillConfig getSkillConfig() {
        return this.skillConfig;
    }

    /**
     * Returns the ordered rate definitions used by the skill menu.
     *
     * @return ordered rate definitions
     */
    public @NotNull List<SkillConfig.RateDefinition> getRates() {
        return this.skillConfig.getRates();
    }

    /**
     * Awards XP for the supplied rate once.
     *
     * @param player player receiving XP
     * @param rateDefinition matching configured rate
     */
    public void awardXp(
        final @NotNull Player player,
        final @NotNull SkillConfig.RateDefinition rateDefinition
    ) {
        this.awardXp(player, rateDefinition, 1.0D, null);
    }

    /**
     * Awards XP for the supplied rate scaled by the supplied unit count.
     *
     * @param player player receiving XP
     * @param rateDefinition matching configured rate
     * @param units matched unit count
     * @param sourceLabel optional display label used by the gain message
     */
    public void awardXp(
        final @NotNull Player player,
        final @NotNull SkillConfig.RateDefinition rateDefinition,
        final double units,
        final @Nullable String sourceLabel
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rateDefinition, "rateDefinition");

        if (!this.skillConfig.isEnabled()) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        final long baseXp = calculateBaseXp(rateDefinition.xp(), units);
        if (baseXp <= 0L) {
            return;
        }

        final String resolvedSourceLabel = sourceLabel == null ? rateDefinition.label() : sourceLabel;
        final PartyService partyService = this.rda.getPartyService();
        if (partyService != null) {
            partyService.distributeSkillXp(this, player, baseXp, resolvedSourceLabel);
            return;
        }

        this.awardDistributedBaseXp(player, baseXp, resolvedSourceLabel, null);
    }

    /**
     * Attempts to prestige the player's skill.
     *
     * @param player player attempting to prestige
     * @return {@code true} when prestige succeeded
     */
    public boolean prestige(final @NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!this.skillConfig.isEnabled()) {
            new I18n.Builder("ra_skill.message.disabled", player)
                .includePrefix()
                .withPlaceholders(this.skillType.getPlaceholders(player))
                .build()
                .sendMessage();
            return false;
        }

        final RDASkillState skillState = this.ensureState(this.getOrCreatePlayerProfile(player));
        final SkillProfileSnapshot snapshot = createSnapshot(skillState, this.skillType, this.skillConfig);
        if (!snapshot.prestigeAvailable()) {
            new I18n.Builder("ra_skill.message.prestige_unavailable", player)
                .includePrefix()
                .withPlaceholders(this.mergePlaceholders(player, Map.of(
                    "soft_max_level", this.skillConfig.getSoftMaxLevel(),
                    "prestige", snapshot.prestige(),
                    "max_prestiges", snapshot.maxPrestiges()
                )))
                .build()
                .sendMessage();
            return false;
        }

        final PrestigeAdjustmentPreview prestigeAdjustmentPreview = this.rda.previewPrestigeAdjustment(player, this.skillType);
        skillState.setPrestige(skillState.getPrestige() + 1);
        skillState.setLevel(0);
        skillState.setXp(0L);
        this.skillStateRepository.updateAsync(skillState).exceptionally(throwable -> {
            this.rda.getLogger().warning(
                "Failed to persist " + this.skillType.getId() + " prestige for "
                    + player.getUniqueId()
                    + ": "
                    + throwable.getMessage()
            );
            return null;
        });
        if (prestigeAdjustmentPreview != null && this.rda.getPlayerBuildService() != null) {
            this.rda.getPlayerBuildService().applyPrestigeAdjustment(player, prestigeAdjustmentPreview);
        }

        new I18n.Builder("ra_skill.message.prestige_success", player)
            .includePrefix()
            .withPlaceholders(this.mergePlaceholders(player, Map.of(
                "prestige", skillState.getPrestige(),
                "max_prestiges", this.skillConfig.getMaxPrestiges(),
                "xp_bonus_percent", skillState.getPrestige() * this.skillConfig.getPrestigeXpBonusPerPrestigePercent()
            )))
            .build()
            .sendMessage();
        if (prestigeAdjustmentPreview != null) {
            new I18n.Builder("ra_skill.message.prestige_reset", player)
                .includePrefix()
                .withPlaceholders(this.mergePlaceholders(player, Map.of(
                    "reset_points", prestigeAdjustmentPreview.getTotalResetPoints(),
                    "earned_points", prestigeAdjustmentPreview.earnedPointsAfterPrestige(),
                    "unspent_points", prestigeAdjustmentPreview.unspentPointsAfterPrestige()
                )))
                .build()
                .sendMessage();
        }
        return true;
    }

    /**
     * Creates a view-friendly skill snapshot for the supplied player.
     *
     * @param player player whose profile should be resolved
     * @return skill snapshot
     */
    public @NotNull SkillProfileSnapshot getSnapshot(final @NotNull Player player) {
        return createSnapshot(this.ensureState(this.getOrCreatePlayerProfile(player)), this.skillType, this.skillConfig);
    }

    /**
     * Ensures the child skill-state row exists for the supplied player profile.
     *
     * @param playerProfile owning player profile
     * @return resolved or newly created child state
     */
    public synchronized @NotNull RDASkillState ensureState(final @NotNull RDAPlayer playerProfile) {
        Objects.requireNonNull(playerProfile, "playerProfile");

        final RDASkillState existingState = this.skillStateRepository.findByPlayerAndSkill(
            playerProfile.getPlayerUuid(),
            this.skillType
        );
        if (existingState != null) {
            return existingState;
        }

        final RDASkillState seededState = new RDASkillState(playerProfile, this.skillType);
        if (this.skillType.hasLegacyProfileColumns()) {
            seededState.setXp(this.skillType.getLegacyXp(playerProfile));
            seededState.setLevel(this.skillType.getLegacyLevel(playerProfile));
            seededState.setPrestige(this.skillType.getLegacyPrestige(playerProfile));
        }

        try {
            return this.skillStateRepository.create(seededState);
        } catch (final RuntimeException exception) {
            final RDASkillState concurrentState = this.skillStateRepository.findByPlayerAndSkill(
                playerProfile.getPlayerUuid(),
                this.skillType
            );
            if (concurrentState != null) {
                return concurrentState;
            }
            throw exception;
        }
    }

    /**
     * Calculates the XP required to progress from the supplied internal level.
     *
     * @param skillConfig skill configuration
     * @param level current internal level
     * @return XP required to reach the next level
     */
    static long xpToNextLevel(final @NotNull SkillConfig skillConfig, final int level) {
        return skillConfig.getLevelFormula().xpToNextLevel(level);
    }

    /**
     * Resolves the total cumulative progression XP stored in a skill state.
     *
     * @param skillState child skill state to inspect
     * @param skillConfig skill configuration
     * @return cumulative progression XP represented by the current level and carried XP
     */
    static long resolveTotalProgressXp(
        final @NotNull RDASkillState skillState,
        final @NotNull SkillConfig skillConfig
    ) {
        Objects.requireNonNull(skillState, "skillState");
        Objects.requireNonNull(skillConfig, "skillConfig");

        final boolean maxPrestigeReached = skillState.getPrestige() >= skillConfig.getMaxPrestiges();
        final int boundedLevel = maxPrestigeReached
            ? Math.max(0, skillState.getLevel())
            : Math.min(Math.max(0, skillState.getLevel()), skillConfig.getSoftMaxLevel());

        long totalXp = Math.max(0L, skillState.getXp());
        for (int level = 0; level < boundedLevel; level++) {
            totalXp += xpToNextLevel(skillConfig, level);
        }
        return totalXp;
    }

    /**
     * Rebuilds a skill state from one cumulative progression XP total.
     *
     * @param skillState child skill state to mutate
     * @param totalProgressXp cumulative progression XP to apply
     * @param skillConfig skill configuration
     */
    static void applyTotalProgressXp(
        final @NotNull RDASkillState skillState,
        final long totalProgressXp,
        final @NotNull SkillConfig skillConfig
    ) {
        Objects.requireNonNull(skillState, "skillState");
        Objects.requireNonNull(skillConfig, "skillConfig");

        final boolean maxPrestigeReached = skillState.getPrestige() >= skillConfig.getMaxPrestiges();
        int currentLevel = 0;
        long currentLevelXp = Math.max(0L, totalProgressXp);

        if (maxPrestigeReached) {
            while (currentLevelXp >= xpToNextLevel(skillConfig, currentLevel)) {
                currentLevelXp -= xpToNextLevel(skillConfig, currentLevel);
                currentLevel++;
            }
        } else {
            while (currentLevel < skillConfig.getSoftMaxLevel()) {
                final long nextRequirement = xpToNextLevel(skillConfig, currentLevel);
                if (currentLevelXp < nextRequirement) {
                    break;
                }

                currentLevelXp -= nextRequirement;
                currentLevel++;
            }
        }

        skillState.setLevel(currentLevel);
        skillState.setXp(currentLevelXp);
    }

    /**
     * Calculates the skill XP awarded for a configured rate and prestige count.
     *
     * @param xpPerUnit configured XP per unit from configuration
     * @param units matched unit count
     * @param prestige completed prestige count
     * @param skillConfig skill configuration
     * @return awarded XP after applying prestige bonus
     */
    static long calculateAwardedXp(
        final int xpPerUnit,
        final double units,
        final int prestige,
        final @NotNull SkillConfig skillConfig
    ) {
        return applyPrestigeBonus(calculateBaseXp(xpPerUnit, units), prestige, skillConfig);
    }

    /**
     * Calculates the pre-prestige XP awarded for a configured rate.
     *
     * @param xpPerUnit configured XP per unit from configuration
     * @param units matched unit count
     * @return pre-prestige XP before share splitting and prestige bonus
     */
    static long calculateBaseXp(
        final int xpPerUnit,
        final double units
    ) {
        return (long) Math.floor(Math.max(0.0D, units) * Math.max(0, xpPerUnit));
    }

    /**
     * Applies the configured prestige bonus to a pre-prestige XP amount.
     *
     * @param baseXp pre-prestige XP amount
     * @param prestige completed prestige count
     * @param skillConfig skill configuration
     * @return final awarded XP after the prestige bonus
     */
    static long applyPrestigeBonus(
        final long baseXp,
        final int prestige,
        final @NotNull SkillConfig skillConfig
    ) {
        Objects.requireNonNull(skillConfig, "skillConfig");
        if (baseXp <= 0L) {
            return 0L;
        }

        final double multiplier =
            1.0D + prestige * (skillConfig.getPrestigeXpBonusPerPrestigePercent() / 100.0D);
        return (long) Math.floor(baseXp * multiplier);
    }

    /**
     * Applies awarded XP to the supplied child skill state.
     *
     * @param skillState child skill state to mutate
     * @param awardedXp awarded XP amount
     * @param skillConfig skill configuration
     */
    static void applyXpAward(
        final @NotNull RDASkillState skillState,
        final long awardedXp,
        final @NotNull SkillConfig skillConfig
    ) {
        Objects.requireNonNull(skillState, "skillState");
        Objects.requireNonNull(skillConfig, "skillConfig");

        if (awardedXp <= 0L) {
            return;
        }
        applyTotalProgressXp(skillState, resolveTotalProgressXp(skillState, skillConfig) + awardedXp, skillConfig);
    }

    /**
     * Builds a view snapshot from a persisted child skill state.
     *
     * @param skillState persisted child skill state
     * @param skillType owning skill type
     * @param skillConfig skill configuration
     * @return skill profile snapshot
     */
    static @NotNull SkillProfileSnapshot createSnapshot(
        final @NotNull RDASkillState skillState,
        final @NotNull SkillType skillType,
        final @NotNull SkillConfig skillConfig
    ) {
        Objects.requireNonNull(skillState, "skillState");
        Objects.requireNonNull(skillType, "skillType");
        Objects.requireNonNull(skillConfig, "skillConfig");

        final int internalLevel = skillState.getLevel();
        final int prestige = skillState.getPrestige();
        final boolean maxPrestigeReached = prestige >= skillConfig.getMaxPrestiges();
        final int softMaxLevel = skillConfig.getSoftMaxLevel();
        final int displayLevel = Math.min(internalLevel, softMaxLevel);
        final int overlevel = maxPrestigeReached ? Math.max(0, internalLevel - softMaxLevel) : 0;
        final String displayLevelText = overlevel > 0
            ? softMaxLevel + " (" + overlevel + ")"
            : Integer.toString(displayLevel);
        final boolean prestigeAvailable =
            skillConfig.getPrestigeTrigger() == SkillConfig.PrestigeTrigger.MANUAL
                && prestige < skillConfig.getMaxPrestiges()
                && internalLevel >= softMaxLevel;
        final long xpToNextLevel = maxPrestigeReached || internalLevel < softMaxLevel
            ? xpToNextLevel(skillConfig, internalLevel)
            : 0L;

        return new SkillProfileSnapshot(
            skillType,
            skillConfig.isEnabled(),
            internalLevel,
            displayLevel,
            overlevel,
            displayLevelText,
            Math.max(0L, skillState.getXp()),
            xpToNextLevel,
            prestige,
            skillConfig.getMaxPrestiges(),
            prestigeAvailable,
            maxPrestigeReached,
            renderPrestigeSymbols(prestige),
            (long) prestige * softMaxLevel + internalLevel,
            prestige * skillConfig.getPrestigeXpBonusPerPrestigePercent(),
            softMaxLevel
        );
    }

    private static @NotNull String renderPrestigeSymbols(final int prestige) {
        if (prestige <= 0) {
            return "-";
        }

        return "*".repeat(prestige);
    }

    private @NotNull RDAPlayer getOrCreatePlayerProfile(final @NotNull Player player) {
        return this.playerRepository.findOrCreateByPlayer(player.getUniqueId());
    }

    void awardDistributedBaseXp(
        final @NotNull Player player,
        final long baseXp,
        final @NotNull String sourceLabel,
        final @Nullable Player sharedBy
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sourceLabel, "sourceLabel");

        if (!this.skillConfig.isEnabled()) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player);
        final RDASkillState skillState = this.ensureState(playerProfile);
        final int previousLevel = skillState.getLevel();
        final long awardedXp = applyPrestigeBonus(baseXp, skillState.getPrestige(), this.skillConfig);
        if (awardedXp <= 0L) {
            return;
        }

        applyXpAward(skillState, awardedXp, this.skillConfig);
        this.skillStateRepository.updateAsync(skillState).exceptionally(throwable -> {
            this.rda.getLogger().warning(
                "Failed to persist " + this.skillType.getId() + " progression for "
                    + player.getUniqueId()
                    + ": "
                    + throwable.getMessage()
            );
            return null;
        });

        if (sharedBy == null || sharedBy.getUniqueId().equals(player.getUniqueId())) {
            this.sendGainActionBar(player, awardedXp, sourceLabel);
        } else {
            this.sendSharedGainMessage(player, sharedBy, awardedXp, sourceLabel);
        }

        if (skillState.getLevel() > previousLevel) {
            final PlayerBuildService buildService = this.rda.getPlayerBuildService();
            final int grantedPoints = buildService == null
                ? 0
                : buildService.grantLevelThresholdPoints(player, this.skillType, previousLevel, skillState.getLevel());
            final SkillProfileSnapshot snapshot = createSnapshot(skillState, this.skillType, this.skillConfig);
            new I18n.Builder("ra_skill.message.level_up", player)
                .includePrefix()
                .withPlaceholders(this.mergePlaceholders(player, Map.of(
                    "level", snapshot.displayLevelText(),
                    "prestige", snapshot.prestige(),
                    "total_power", snapshot.totalPower()
                )))
                .build()
                .sendMessage();

            if (snapshot.prestigeAvailable()) {
                new I18n.Builder("ra_skill.message.prestige_ready", player)
                    .includePrefix()
                    .withPlaceholders(this.mergePlaceholders(player, Map.of(
                        "soft_max_level", this.skillConfig.getSoftMaxLevel(),
                        "prestige", snapshot.prestige(),
                        "max_prestiges", snapshot.maxPrestiges()
                    )))
                    .build()
                    .sendMessage();
            }

            if (grantedPoints > 0) {
                new I18n.Builder("ra_build.message.points_earned", player)
                    .includePrefix()
                    .withPlaceholders(this.mergePlaceholders(player, Map.of(
                        "points", grantedPoints
                    )))
                    .build()
                    .sendMessage();
            }
        }
    }

    private void sendGainActionBar(
        final @NotNull Player player,
        final long awardedXp,
        final @NotNull String sourceLabel
    ) {
        final Component component = new I18n.Builder("ra_skill.message.gain", player)
            .withPlaceholders(this.mergePlaceholders(player, Map.of(
                "xp", awardedXp,
                "source_name", sourceLabel
            )))
            .build()
            .component();
        player.sendActionBar(component);
    }

    private void sendSharedGainMessage(
        final @NotNull Player player,
        final @NotNull Player sharedBy,
        final long awardedXp,
        final @NotNull String sourceLabel
    ) {
        new I18n.Builder("ra_party.message.shared_gain", player)
            .includePrefix()
            .withPlaceholders(this.mergePlaceholders(player, Map.of(
                "xp", awardedXp,
                "source_name", sourceLabel,
                "earner_name", sharedBy.getName()
            )))
            .build()
            .sendMessage();
    }

    private @NotNull Map<String, Object> mergePlaceholders(
        final @NotNull Player player,
        final @NotNull Map<String, Object> extraPlaceholders
    ) {
        final LinkedHashMap<String, Object> placeholders = new LinkedHashMap<>(this.skillType.getPlaceholders(player));
        placeholders.putAll(extraPlaceholders);
        return placeholders;
    }
}
