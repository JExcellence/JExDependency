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
 * Core build stats that power passive bonuses and skill-ability scaling in RDA.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public enum CoreStatType {

    /**
     * Survivability-focused stat.
     */
    VIT("vit", Material.GOLDEN_APPLE),

    /**
     * Power-output stat.
     */
    STR("str", Material.NETHERITE_SWORD),

    /**
     * Speed-focused stat.
     */
    AGI("agi", Material.FEATHER),

    /**
     * Precision-focused stat.
     */
    DEX("dex", Material.CROSSBOW),

    /**
     * Systems and crafting stat.
     */
    INT("int", Material.ENCHANTED_BOOK),

    /**
     * Sustain and mana stat.
     */
    SPI("spi", Material.AMETHYST_SHARD),

    /**
     * Luck-based stat.
     */
    LUK("luk", Material.RABBIT_FOOT),

    /**
     * Social and economy stat.
     */
    CHA("cha", Material.EMERALD);

    private final String id;
    private final Material fallbackIcon;

    CoreStatType(
        final @NotNull String id,
        final @NotNull Material fallbackIcon
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.fallbackIcon = Objects.requireNonNull(fallbackIcon, "fallbackIcon");
    }

    /**
     * Returns the canonical lowercase identifier for the stat.
     *
     * @return canonical lowercase stat identifier
     */
    public @NotNull String getId() {
        return this.id;
    }

    /**
     * Returns the fallback icon used before a configured stat icon is available.
     *
     * @return fallback menu icon
     */
    public @NotNull Material getFallbackIcon() {
        return this.fallbackIcon;
    }

    /**
     * Returns the translation key used to resolve the localized stat name.
     *
     * @return localized stat-name key
     */
    public @NotNull String getNameTranslationKey() {
        return "ra_core_stat_names." + this.id;
    }

    /**
     * Resolves the localized display name for the stat.
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
     * Builds a placeholder map shared by the stat menus and translated runtime messages.
     *
     * @param player player whose locale should be used
     * @return stat placeholder map
     */
    public @NotNull Map<String, Object> getPlaceholders(final @NotNull Player player) {
        final String displayName = this.getDisplayName(player);
        final LinkedHashMap<String, Object> placeholders = new LinkedHashMap<>();
        placeholders.put("stat_id", this.id);
        placeholders.put("stat_name", displayName);
        placeholders.put("stat_name_lower", displayName.toLowerCase(Locale.ROOT));
        return placeholders;
    }
}
