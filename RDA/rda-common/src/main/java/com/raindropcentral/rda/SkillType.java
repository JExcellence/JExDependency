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
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Built-in RDA skills backed by the shared generic progression framework.
 *
 * <p>Each enum constant defines the configuration resource path, fallback icon, and optional legacy
 * migration accessors used to seed the new per-skill state table from historic mining and
 * woodcutting columns.</p>
 *
 * @author Codex
 * @since 1.0.0
 * @version 1.1.0
 */
public enum SkillType {

    /**
     * Mining progression backed by tracked stone, ore, and mineral blocks.
     */
    MINING("mining", Material.DIAMOND_PICKAXE),

    /**
     * Woodcutting progression backed by tracked log, stem, hyphae, and wood blocks.
     */
    WOODCUTTING("woodcutting", Material.DIAMOND_AXE),

    /**
     * Defense progression earned by surviving damage and blocking with shields.
     */
    DEFENSE("defense", Material.SHIELD),

    /**
     * Farming progression earned by harvesting mature crops.
     */
    FARMING("farming", Material.WHEAT),

    /**
     * Agility progression earned by movement and discovery.
     */
    AGILITY("agility", Material.FEATHER),

    /**
     * Archery progression earned through arrow damage.
     */
    ARCHERY("archery", Material.BOW),

    /**
     * Alchemy progression earned by collecting brewed potions.
     */
    ALCHEMY("alchemy", Material.BREWING_STAND),

    /**
     * Enchanting progression earned from enchanting books and gear.
     */
    ENCHANTING("enchanting", Material.ENCHANTING_TABLE),

    /**
     * Excavation progression earned by shovel digging.
     */
    EXCAVATION("excavation", Material.DIAMOND_SHOVEL),

    /**
     * Crafting progression earned by producing configured recipes.
     */
    CRAFTING("crafting", Material.CRAFTING_TABLE),

    /**
     * Fishing progression earned from successful catches.
     */
    FISHING("fishing", Material.FISHING_ROD),

    /**
     * Foraging progression earned by harvesting flowers and bee products.
     */
    FORAGING("foraging", Material.HONEYCOMB),

    /**
     * Taming progression earned by taming creatures and tame final blows.
     */
    TAMING("taming", Material.BONE),

    /**
     * Cooking progression earned by collecting cooked food from furnaces and smokers.
     */
    COOKING("cooking", Material.SMOKER),

    /**
     * Fighting progression earned by direct combat damage.
     */
    FIGHTING("fighting", Material.DIAMOND_SWORD),

    /**
     * Smithing progression earned by collecting smelted bars from furnaces and blast furnaces.
     */
    SMITHING("smithing", Material.BLAST_FURNACE);

    private static final String RESOURCE_DIRECTORY = "skills/";
    private static final String LEGACY_RESOURCE_DIRECTORY = RESOURCE_DIRECTORY + "abilities/";

    private final String id;
    private final Material fallbackIcon;

    SkillType(
        final @NotNull String id,
        final @NotNull Material fallbackIcon
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.fallbackIcon = Objects.requireNonNull(fallbackIcon, "fallbackIcon");
    }

    /**
     * Returns the canonical skill identifier.
     *
     * @return canonical lowercase skill identifier
     */
    public @NotNull String getId() {
        return this.id;
    }

    /**
     * Returns the bundled configuration resource path for the skill.
     *
     * @return bundled configuration resource path
     */
    public @NotNull String getResourcePath() {
        return RESOURCE_DIRECTORY + this.id + ".yml";
    }

    @NotNull String getLegacyResourcePath() {
        return LEGACY_RESOURCE_DIRECTORY + this.id + ".yml";
    }

    /**
     * Returns the fallback icon used before a skill configuration is available.
     *
     * @return fallback menu icon
     */
    public @NotNull Material getFallbackIcon() {
        return this.fallbackIcon;
    }

    /**
     * Returns the translation key used to resolve the localized skill name.
     *
     * @return localized skill name translation key
     */
    public @NotNull String getNameTranslationKey() {
        return "ra_skill_names." + this.id;
    }

    /**
     * Reports whether the skill can seed its new child state row from legacy columns on
     * {@link RDAPlayer}.
     *
     * @return {@code true} when the skill has legacy root-column storage
     */
    public boolean hasLegacyProfileColumns() {
        return this == MINING || this == WOODCUTTING;
    }

    /**
     * Resolves the legacy XP stored on the root player profile.
     *
     * @param playerProfile player profile to inspect
     * @return legacy XP value, or {@code 0} when none exists
     */
    public long getLegacyXp(final @NotNull RDAPlayer playerProfile) {
        return switch (this) {
            case MINING -> playerProfile.getMiningXp();
            case WOODCUTTING -> playerProfile.getWoodcuttingXp();
            default -> 0L;
        };
    }

    /**
     * Resolves the legacy level stored on the root player profile.
     *
     * @param playerProfile player profile to inspect
     * @return legacy level value, or {@code 0} when none exists
     */
    public int getLegacyLevel(final @NotNull RDAPlayer playerProfile) {
        return switch (this) {
            case MINING -> playerProfile.getMiningLevel();
            case WOODCUTTING -> playerProfile.getWoodcuttingLevel();
            default -> 0;
        };
    }

    /**
     * Resolves the legacy prestige count stored on the root player profile.
     *
     * @param playerProfile player profile to inspect
     * @return legacy prestige count, or {@code 0} when none exists
     */
    public int getLegacyPrestige(final @NotNull RDAPlayer playerProfile) {
        return switch (this) {
            case MINING -> playerProfile.getMiningPrestige();
            case WOODCUTTING -> playerProfile.getWoodcuttingPrestige();
            default -> 0;
        };
    }

    /**
     * Resolves the localized display name for the skill.
     *
     * @param player player whose locale should be used
     * @return plain-text localized display name
     */
    public @NotNull String getDisplayName(final @NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder(this.getNameTranslationKey(), player).build().component()
        );
    }

    /**
     * Builds a common placeholder map for translated skill UI and messages.
     *
     * @param player player whose locale should be used
     * @return skill placeholder map
     */
    public @NotNull Map<String, Object> getPlaceholders(final @NotNull Player player) {
        final String displayName = this.getDisplayName(player);
        final LinkedHashMap<String, Object> placeholders = new LinkedHashMap<>();
        placeholders.put("skill_id", this.id);
        placeholders.put("skill_name", displayName);
        placeholders.put("skill_name_lower", displayName.toLowerCase(Locale.ROOT));
        return placeholders;
    }
}
