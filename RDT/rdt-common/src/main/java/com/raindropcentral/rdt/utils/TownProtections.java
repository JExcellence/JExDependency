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

package com.raindropcentral.rdt.utils;

import com.raindropcentral.rdt.database.entity.RTown;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Enumerates grouped town protection rules for world interaction.
 *
 * <p>Each protection stores a minimum allowed role rather than a per-role matrix. Chunk-level
 * settings may inherit the town value or override it for a specific claim.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownProtections {
    TOWN_HOSTILE_ENTITIES(TownProtectionCategory.BINARY_TOGGLE, RTown.RESTRICTED_ROLE_ID, true, "hostile_entities"),
    TOWN_PASSIVE_ENTITIES(TownProtectionCategory.BINARY_TOGGLE, RTown.PUBLIC_ROLE_ID, true, "passive_entities"),
    TOWN_FIRE(TownProtectionCategory.BINARY_TOGGLE, RTown.PUBLIC_ROLE_ID, true, "fire"),
    TOWN_WATER(TownProtectionCategory.BINARY_TOGGLE, RTown.PUBLIC_ROLE_ID, true, "water"),
    TOWN_LAVA(TownProtectionCategory.BINARY_TOGGLE, RTown.PUBLIC_ROLE_ID, true, "lava"),
    BREAK_BLOCK(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "break_block"),
    PLACE_BLOCK(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "place_block"),
    CONTAINER_ACCESS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, false, "container_access"),
    SWITCH_ACCESS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "switch_access"),
    ITEM_USE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "item_use"),
    CHEST(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "chest"),
    SHULKER_BOXES(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "shulker_boxes"),
    TRAPPED_CHEST(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "trapped_chest"),
    FURNACE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "furnace"),
    BLAST_FURNACE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "blast_furnace"),
    DISPENSER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "dispenser"),
    HOPPER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "hopper"),
    DROPPER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "dropper"),
    JUKEBOX(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "jukebox"),
    STONECUTTER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "stonecutter"),
    SMITHING_TABLE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "smithing_table"),
    FLETCHING_TABLE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "fletching_table"),
    SMOKER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "smoker"),
    LOOM(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "loom"),
    GRINDSTONE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "grindstone"),
    COMPOSTER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "composter"),
    CARTOGRAPHY_TABLE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "cartography_table"),
    BELL(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "bell"),
    BARREL(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "barrel"),
    BREWING_STAND(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "brewing_stand"),
    LEVER(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "lever"),
    PRESSURE_PLATES(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "pressure_plates"),
    BUTTONS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "buttons"),
    WOOD_DOORS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "wood_doors"),
    FENCE_GATES(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "fence_gates"),
    TRAPDOORS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "trapdoors"),
    LODESTONE(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "lodestone"),
    RESPAWN_ANCHOR(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "respawn_anchor"),
    TARGET(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "target"),
    MINECARTS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "minecarts"),
    BOATS(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "boats"),
    ENDER_PEARL(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "ender_pearl"),
    FIREBALL(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "fireball"),
    CHORUS_FRUIT(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "chorus_fruit"),
    LEAD(TownProtectionCategory.ROLE_BASED, RTown.MEMBER_ROLE_ID, true, "lead");

    private final TownProtectionCategory category;
    private final String defaultRoleId;
    private final boolean editable;
    private final String translationKey;

    TownProtections(
        final @NotNull TownProtectionCategory category,
        final @NotNull String defaultRoleId,
        final boolean editable,
        final @NotNull String translationKey
    ) {
        this.category = category;
        this.defaultRoleId = normalizeRoleId(defaultRoleId);
        this.editable = editable;
        this.translationKey = translationKey;
    }

    /**
     * Returns the stable persisted protection key.
     *
     * @return stable uppercase protection key
     */
    public @NotNull String getProtectionKey() {
        return this.name();
    }

    /**
     * Returns the editor category for this protection.
     *
     * @return protection category
     */
    public @NotNull TownProtectionCategory getCategory() {
        return this.category;
    }

    /**
     * Returns the default minimum role allowed for this protection.
     *
     * @return normalized default role identifier
     */
    public @NotNull String getDefaultRoleId() {
        return this.defaultRoleId;
    }

    /**
     * Returns whether this protection should appear in editable GUI lists.
     *
     * @return {@code true} when the protection should be shown to players
     */
    public boolean isEditable() {
        return this.editable;
    }

    /**
     * Returns whether this protection uses allowed/restricted states rather than role thresholds.
     *
     * @return {@code true} when the protection is binary
     */
    public boolean isBinaryToggle() {
        return this.category == TownProtectionCategory.BINARY_TOGGLE;
    }

    /**
     * Returns whether this protection appears in the switch-actions submenu.
     *
     * @return {@code true} when this protection is one of the switch-specific entries
     */
    public boolean isSwitchAction() {
        return switch (this) {
            case CHEST, SHULKER_BOXES, TRAPPED_CHEST, FURNACE, BLAST_FURNACE, DISPENSER, HOPPER,
                 DROPPER, JUKEBOX, STONECUTTER, SMITHING_TABLE, FLETCHING_TABLE, SMOKER, LOOM,
                 GRINDSTONE, COMPOSTER, CARTOGRAPHY_TABLE, BELL, BARREL, BREWING_STAND, LEVER,
                 PRESSURE_PLATES, BUTTONS, WOOD_DOORS, FENCE_GATES, TRAPDOORS, LODESTONE,
                 RESPAWN_ANCHOR, TARGET -> true;
            default -> false;
        };
    }

    /**
     * Returns whether this protection appears in the dedicated item-use submenu.
     *
     * @return {@code true} when this protection is one of the item-use-specific entries
     */
    public boolean isItemUseAction() {
        return switch (this) {
            case MINECARTS, BOATS, ENDER_PEARL, FIREBALL, CHORUS_FRUIT, LEAD -> true;
            default -> false;
        };
    }

    /**
     * Returns the legacy parent protection that should be consulted when this interaction does not
     * have its own stored role threshold.
     *
     * @return fallback protection, or {@code null} when this protection has no parent fallback
     */
    public @Nullable TownProtections getFallbackProtection() {
        return switch (this) {
            case ITEM_USE -> SWITCH_ACCESS;
            case CHEST, SHULKER_BOXES, TRAPPED_CHEST, FURNACE, BLAST_FURNACE, DISPENSER, HOPPER,
                 DROPPER, JUKEBOX, SMOKER, BARREL, BREWING_STAND -> CONTAINER_ACCESS;
            case STONECUTTER, SMITHING_TABLE, FLETCHING_TABLE, LOOM, GRINDSTONE, COMPOSTER,
                 CARTOGRAPHY_TABLE, BELL, LEVER, PRESSURE_PLATES, BUTTONS, WOOD_DOORS,
                 FENCE_GATES, TRAPDOORS, LODESTONE, RESPAWN_ANCHOR, TARGET -> SWITCH_ACCESS;
            case MINECARTS, BOATS, ENDER_PEARL, FIREBALL, CHORUS_FRUIT, LEAD -> ITEM_USE;
            default -> null;
        };
    }

    /**
     * Returns the translation key suffix for this protection label.
     *
     * @return translation key suffix
     */
    public @NotNull String getTranslationKey() {
        return this.translationKey;
    }

    /**
     * Normalizes a configured role identifier according to this protection's storage rules.
     *
     * @param roleId raw role identifier
     * @return normalized persisted role identifier
     */
    public @NotNull String normalizeConfiguredRoleId(final @Nullable String roleId) {
        final String normalizedRoleId = normalizeRoleId(roleId);
        return this.isBinaryToggle() ? normalizeBinaryRoleId(normalizedRoleId) : normalizedRoleId;
    }

    /**
     * Normalizes a chunk override role identifier according to this protection's storage rules.
     *
     * @param roleId raw override role identifier
     * @return normalized override role identifier, or {@code null} when the override should clear
     */
    public @Nullable String normalizeOverrideRoleId(final @Nullable String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return null;
        }
        return this.normalizeConfiguredRoleId(roleId);
    }

    /**
     * Normalizes a stored role identifier into the uppercase persisted form.
     *
     * @param roleId raw role identifier
     * @return normalized role identifier, defaulting to {@link RTown#MEMBER_ROLE_ID} when blank
     */
    public static @NotNull String normalizeRoleId(final @Nullable String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return RTown.MEMBER_ROLE_ID;
        }
        return roleId.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes a stored binary protection role into the supported allowed/restricted states.
     *
     * @param roleId raw role identifier
     * @return {@link RTown#PUBLIC_ROLE_ID} when allowed, otherwise {@link RTown#RESTRICTED_ROLE_ID}
     */
    public static @NotNull String normalizeBinaryRoleId(final @Nullable String roleId) {
        return Objects.equals(normalizeRoleId(roleId), RTown.PUBLIC_ROLE_ID)
            ? RTown.PUBLIC_ROLE_ID
            : RTown.RESTRICTED_ROLE_ID;
    }

    /**
     * Returns the editable protections for one category in declaration order.
     *
     * @param category category to filter
     * @return immutable ordered protection list
     */
    public static @NotNull List<TownProtections> editableValues(final @NotNull TownProtectionCategory category) {
        return Arrays.stream(TownProtections.values())
            .filter(TownProtections::isEditable)
            .filter(protection -> protection.getCategory() == category)
            .filter(TownProtections::showsInCategoryEditor)
            .toList();
    }

    /**
     * Returns the switch-action protections shown in the dedicated submenu.
     *
     * @return immutable ordered switch-action list
     */
    public static @NotNull List<TownProtections> switchActionValues() {
        return Arrays.stream(TownProtections.values())
            .filter(TownProtections::isEditable)
            .filter(TownProtections::isSwitchAction)
            .toList();
    }

    /**
     * Returns the protections that should be updated when the bulk switch-action control is used.
     *
     * @return immutable ordered bulk-update target list
     */
    public static @NotNull List<TownProtections> switchBulkValues() {
        return Stream.concat(Stream.of(SWITCH_ACCESS, CONTAINER_ACCESS), switchActionValues().stream())
            .distinct()
            .toList();
    }

    /**
     * Returns the item-use protections shown in the dedicated submenu.
     *
     * @return immutable ordered item-use protection list
     */
    public static @NotNull List<TownProtections> itemUseActionValues() {
        return Arrays.stream(TownProtections.values())
            .filter(TownProtections::isEditable)
            .filter(TownProtections::isItemUseAction)
            .toList();
    }

    /**
     * Returns the protections that should be updated when the bulk item-use control is used.
     *
     * @return immutable ordered bulk-update target list
     */
    public static @NotNull List<TownProtections> itemUseBulkValues() {
        return Stream.concat(Stream.of(ITEM_USE), itemUseActionValues().stream())
            .distinct()
            .toList();
    }

    /**
     * Resolves a protection enum from a raw key.
     *
     * @param protectionKey raw protection key
     * @return matching protection enum, or {@code null} when no protection matches
     */
    public static @Nullable TownProtections fromKey(final @Nullable String protectionKey) {
        if (protectionKey == null || protectionKey.isBlank()) {
            return null;
        }

        try {
            return TownProtections.valueOf(protectionKey.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean showsInCategoryEditor() {
        return this.category != TownProtectionCategory.ROLE_BASED
            || (!this.isSwitchAction() && !this.isItemUseAction());
    }
}
