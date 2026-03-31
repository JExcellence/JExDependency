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

package com.raindropcentral.rdq.view.ranks.util;

import com.raindropcentral.rdq.database.entity.rank.RRankReward;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for rendering reward cards in rank UIs.
 * Similar to RequirementCardRenderer but for rewards.
 */
public class RewardCardRenderer {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Mapping of reward types to their display icons.
     */
    private static final Map<String, Material> TYPE_ICONS = Map.of(
            "CURRENCY", Material.GOLD_INGOT,
            "ITEM", Material.CHEST,
            "COMMAND", Material.COMMAND_BLOCK,
            "PERMISSION", Material.PAPER,
            "EXPERIENCE", Material.EXPERIENCE_BOTTLE,
            "TELEPORT", Material.ENDER_PEARL,
            "PARTICLE", Material.BLAZE_POWDER,
            "VANISHING_CHEST", Material.ENDER_CHEST
    );

    /**
     * Creates an ItemStack representing a rank reward.
     *
     * @param rankReward the rank reward to render
     * @param player     the player viewing the reward
     * @return the rendered ItemStack
     */
    public static @NotNull ItemStack createRewardCard(
            @NotNull RRankReward rankReward,
            @NotNull Player player
    ) {
        AbstractReward reward = rankReward.getReward().getReward();

        Material iconMaterial = getRewardIcon(reward);

        Component displayName = new I18n.Builder("reward.card.name", player)
                .withPlaceholder("type", formatTypeName(reward.getTypeId()))
                .build()
                .component();

        List<Component> lore = buildCardLore(player, rankReward);
        
        return UnifiedBuilderFactory.item(iconMaterial)
                .setName(displayName)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    /**
     * Creates a compact reward card for grid display.
     *
     * @param rankReward the rank reward to render
     * @param player     the player viewing the reward
     * @return the rendered ItemStack
     */
    public static @NotNull ItemStack createCompactRewardCard(
            @NotNull RRankReward rankReward,
            @NotNull Player player
    ) {
        AbstractReward reward = rankReward.getReward().getReward();
        Material iconMaterial = getRewardIcon(reward);

        Component displayName = new I18n.Builder("reward.card.compact_name", player)
                .withPlaceholder("type", formatTypeName(reward.getTypeId()))
                .build()
                .component();

        List<Component> lore = new ArrayList<>();

        Component description = new I18n.Builder(reward.getDescriptionKey(), player)
                .build()
                .component();
        lore.add(description);

        if (rankReward.isAutoGrant()) {
            lore.add(Component.empty());
            lore.add(MINI_MESSAGE.deserialize("<green>✓ Auto-granted</green>"));
        }
        
        return UnifiedBuilderFactory.item(iconMaterial)
                .setName(displayName)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    /**
     * Creates a placeholder reward card when no rewards are configured.
     *
     * @param player the player viewing the card
     * @return the placeholder ItemStack
     */
    public static @NotNull ItemStack createNoRewardsCard(@NotNull Player player) {
        Component displayName = new I18n.Builder("reward.card.no_rewards", player)
                .build()
                .component();
        
        List<Component> lore = new I18n.Builder("reward.card.no_rewards_lore", player)
                .build()
                .children();
        
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(displayName)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Builds the lore for a reward card.
     */
    private static @NotNull List<Component> buildCardLore(
            @NotNull Player player,
            @NotNull RRankReward rankReward
    ) {
        List<Component> lore = new ArrayList<>();
        AbstractReward reward = rankReward.getReward().getReward();
        
        lore.add(Component.empty());

        Component description = new I18n.Builder(reward.getDescriptionKey(), player)
                .build()
                .component();
        lore.add(description);
        
        lore.add(Component.empty());

        Component typeLabel = new I18n.Builder("reward.card.type_label", player)
                .withPlaceholder("type", reward.getTypeId())
                .build()
                .component();
        lore.add(typeLabel);

        double value = reward.getEstimatedValue();
        if (value > 0) {
            Component valueLabel = new I18n.Builder("reward.card.value_label", player)
                    .withPlaceholder("value", String.format("%.2f", value))
                    .build()
                    .component();
            lore.add(valueLabel);
        }

        if (rankReward.isAutoGrant()) {
            lore.add(Component.empty());
            lore.add(MINI_MESSAGE.deserialize("<green>✓ Granted automatically</green>"));
        }
        
        return lore;
    }

    /**
     * Gets the appropriate icon material for a reward.
     *
     * @param reward the reward
     * @return the icon material
     */
    private static @NotNull Material getRewardIcon(@NotNull AbstractReward reward) {
        String typeId = reward.getTypeId().toUpperCase();
        return TYPE_ICONS.getOrDefault(typeId, Material.DIAMOND);
    }

    /**
     * Formats a reward type name for display.
     */
    private static @NotNull String formatTypeName(@NotNull String typeId) {
        return switch (typeId.toUpperCase()) {
            case "CURRENCY" -> "💰 Currency";
            case "ITEM" -> "📦 Item";
            case "COMMAND" -> "⚙️ Command";
            case "PERMISSION" -> "📄 Permission";
            case "EXPERIENCE" -> "⭐ Experience";
            case "TELEPORT" -> "🌀 Teleport";
            case "PARTICLE" -> "✨ Particle";
            case "VANISHING_CHEST" -> "📦 Vanishing Chest";
            default -> typeId.replace("_", " ");
        };
    }
}
