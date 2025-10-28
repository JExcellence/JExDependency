package com.raindropcentral.rdq.type;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration defining different categories of perks for organization and filtering.
 * <p>
 * This enum categorizes perks based on their functionality and theme:
 * <ul>
 *     <li>{@link #COMBAT} - Combat-related perks (damage, defense, weapons)</li>
 *     <li>{@link #MOVEMENT} - Movement and mobility perks (speed, flight, teleportation)</li>
 *     <li>{@link #UTILITY} - Utility and convenience perks (tools, automation, quality of life)</li>
 *     <li>{@link #SURVIVAL} - Survival-focused perks (health, hunger, environmental)</li>
 *     <li>{@link #ECONOMY} - Economy and trading perks (money, shops, rewards)</li>
 *     <li>{@link #SOCIAL} - Social and multiplayer perks (chat, friends, guilds)</li>
 *     <li>{@link #COSMETIC} - Cosmetic and visual perks (particles, titles, appearances)</li>
 *     <li>{@link #SPECIAL} - Special or unique perks that don't fit other categories</li>
 * </ul>
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public enum EPerkCategory {

    /**
     * Combat-related perks including damage boosts, defense improvements, and weapon enhancements.
     * Examples: Increased damage, damage reduction, critical hit chance, weapon durability
     */
    COMBAT("combat", "Combat", Material.DIAMOND_SWORD, 0),

    /**
     * Movement and mobility perks including speed boosts, flight, and teleportation abilities.
     * Examples: Speed boost, flight mode, teleportation, jump boost, no fall damage
     */
    MOVEMENT("movement", "Movement", Material.FEATHER, 1),

    /**
     * Utility and convenience perks that improve quality of life and provide helpful tools.
     * Examples: Night vision, water breathing, tool efficiency, inventory management
     */
    UTILITY("utility", "Utility", Material.COMPASS, 2),

    /**
     * Survival-focused perks that help with health, hunger, and environmental challenges.
     * Examples: Health regeneration, hunger satisfaction, fire resistance, poison immunity
     */
    SURVIVAL("survival", "Survival", Material.GOLDEN_APPLE, 3),

    /**
     * Economy and trading perks that affect money, shops, and economic interactions.
     * Examples: Increased money drops, shop discounts, sell multipliers, tax reductions
     */
    ECONOMY("economy", "Economy", Material.EMERALD, 4),

    /**
     * Social and multiplayer perks that enhance interaction with other players.
     * Examples: Chat colors, friend bonuses, guild benefits, party advantages
     */
    SOCIAL("social", "Social", Material.PLAYER_HEAD, 5),

    /**
     * Cosmetic and visual perks that provide aesthetic enhancements without gameplay impact.
     * Examples: Particle effects, custom titles, trail effects, pet companions
     */
    COSMETIC("cosmetic", "Cosmetic", Material.FIREWORK_ROCKET, 6),

    /**
     * Special or unique perks that don't fit into other standard categories.
     * Examples: Custom abilities, event-specific perks, admin tools, experimental features
     */
    SPECIAL("special", "Special", Material.NETHER_STAR, 7);

    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final int sortOrder;

    /**
     * Constructs a perk category with specified properties.
     *
     * @param identifier unique string identifier for the category
     * @param displayName human-readable display name
     * @param iconMaterial material to use as icon in GUIs
     * @param sortOrder order for sorting categories (lower = first)
     */
    EPerkCategory(
            final @NotNull String identifier,
            final @NotNull String displayName,
            final @NotNull Material iconMaterial,
            final int sortOrder
    ) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the unique identifier for this category.
     *
     * @return the category identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Gets the display name for this category.
     *
     * @return the human-readable category name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }

    /**
     * Gets the material to use as an icon for this category.
     *
     * @return the icon material
     */
    public @NotNull Material getIconMaterial() {
        return this.iconMaterial;
    }

    /**
     * Gets the sort order for this category.
     *
     * @return the sort order (lower values appear first)
     */
    public int getSortOrder() {
        return this.sortOrder;
    }

    /**
     * Gets the localization key for this category's display name.
     *
     * @return the i18n key for the category name
     */
    public @NotNull String getDisplayNameKey() {
        return "perk.category." + this.identifier + ".name";
    }

    /**
     * Gets the localization key for this category's description.
     *
     * @return the i18n key for the category description
     */
    public @NotNull String getDescriptionKey() {
        return "perk.category." + this.identifier + ".description";
    }

    /**
     * Finds a perk category by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the matching category, or null if not found
     */
    public static EPerkCategory fromIdentifier(final @NotNull String identifier) {
        for (final EPerkCategory category : values()) {
            if (category.getIdentifier().equalsIgnoreCase(identifier)) {
                return category;
            }
        }
        return null;
    }
}