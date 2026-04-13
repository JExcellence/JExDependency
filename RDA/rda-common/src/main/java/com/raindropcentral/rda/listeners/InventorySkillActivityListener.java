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

package com.raindropcentral.rda.listeners;

import com.raindropcentral.rda.PlayerBuildService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillTriggerType;
import com.raindropcentral.rda.SkillType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Listener that awards crafting, furnace, brewing, enchanting, and honey-collection skill XP.
 *
 * <p>The crafting path intentionally uses cheap result-material matching so it does not add heavy
 * ingredient analysis to craft events.</p>
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@SuppressWarnings("unused")
public final class InventorySkillActivityListener implements Listener {

    private final RDA rda;

    /**
     * Creates an inventory skill listener bound to the active runtime.
     *
     * @param rda active RDA runtime
     */
    public InventorySkillActivityListener(final @NotNull RDA rda) {
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Awards crafting XP from configured result materials.
     *
     * @param event craft event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCraftItem(final @NotNull CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final ItemStack result = event.getRecipe() == null ? null : event.getRecipe().getResult();
        if (!this.isValidItem(result)) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.CRAFTING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.CRAFTING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final SkillConfig.RateDefinition rate = this.findMaterialRate(
            skillConfig.getRatesByTrigger(SkillTriggerType.CRAFT_RESULT),
            result.getType()
        );
        if (rate == null) {
            return;
        }

        final int producedAmount = this.resolveCraftAmount(event, result);
        if (producedAmount > 0) {
            progressionService.awardXp(player, rate, producedAmount, rate.label());
            this.tryGrantActiveBonus(player, SkillType.CRAFTING, result.clone());
        }
    }

    /**
     * Awards brewing, furnace, smoker, blast furnace, and anvil enchanting XP from manual output
     * collection.
     *
     * @param event inventory click event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(final @NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!this.isOutputExtractionAction(event.getAction())) {
            return;
        }

        final Inventory topInventory = event.getView().getTopInventory();
        final ItemStack currentItem = event.getCurrentItem();
        if (!this.isValidItem(currentItem) || event.getClickedInventory() != topInventory) {
            return;
        }

        final int amount = this.resolveCollectedAmount(event, currentItem);
        if (amount <= 0) {
            return;
        }

        switch (topInventory.getType()) {
            case BREWING -> this.handleBrewingCollect(player, event.getRawSlot(), currentItem, amount);
            case FURNACE, BLAST_FURNACE, SMOKER -> this.handleFurnaceCollect(player, event.getRawSlot(), currentItem, amount);
            case ANVIL -> this.handleAnvilCollect(player, topInventory, event.getRawSlot(), currentItem);
            default -> {
            }
        }
    }

    /**
     * Awards enchanting XP from enchanting table usage.
     *
     * @param event enchanting table event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEnchantItem(final @NotNull EnchantItemEvent event) {
        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.ENCHANTING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.ENCHANTING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final Material resultMaterial = event.getItem().getType() == Material.BOOK
            ? Material.ENCHANTED_BOOK
            : event.getItem().getType();
        final SkillConfig.RateDefinition rate = this.findMaterialRate(
            skillConfig.getRatesByTrigger(SkillTriggerType.ENCHANT_TABLE),
            resultMaterial
        );
        if (rate != null) {
            progressionService.awardXp(event.getEnchanter(), rate, 1.0D, rate.label());
            this.tryGrantEnchantingBonus(event.getEnchanter());
        }
    }

    /**
     * Awards foraging XP from honey bottle and honeycomb collection.
     *
     * @param event interact event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        final ItemStack usedItem = event.getItem();
        if (clickedBlock == null || !this.isValidItem(usedItem)) {
            return;
        }

        if (clickedBlock.getType() != Material.BEEHIVE && clickedBlock.getType() != Material.BEE_NEST) {
            return;
        }

        final BlockData blockData = clickedBlock.getBlockData();
        if (!(blockData instanceof Beehive beehive) || beehive.getHoneyLevel() < beehive.getMaximumHoneyLevel()) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.FORAGING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.FORAGING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        for (final SkillConfig.RateDefinition rate : skillConfig.getRatesByTrigger(SkillTriggerType.HONEY_COLLECT)) {
            if (!rate.matchesRequiredItem(usedItem.getType())) {
                continue;
            }

            progressionService.awardXp(event.getPlayer(), rate, 1.0D, rate.label());
            return;
        }
    }

    private void handleBrewingCollect(
        final @NotNull Player player,
        final int rawSlot,
        final @NotNull ItemStack currentItem,
        final int amount
    ) {
        if (rawSlot < 0 || rawSlot > 2) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.ALCHEMY);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.ALCHEMY);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final SkillConfig.RateDefinition rate = this.findMaterialRate(
            skillConfig.getRatesByTrigger(SkillTriggerType.BREW_COLLECT),
            currentItem.getType()
        );
        if (rate != null) {
            progressionService.awardXp(player, rate, amount, rate.label());
            this.tryGrantActiveBonus(player, SkillType.ALCHEMY, currentItem.clone());
        }
    }

    private void handleFurnaceCollect(
        final @NotNull Player player,
        final int rawSlot,
        final @NotNull ItemStack currentItem,
        final int amount
    ) {
        if (rawSlot != 2) {
            return;
        }

        this.awardFurnaceCollect(player, SkillType.COOKING, currentItem, amount);
        this.awardFurnaceCollect(player, SkillType.SMITHING, currentItem, amount);
    }

    private void awardFurnaceCollect(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final @NotNull ItemStack currentItem,
        final int amount
    ) {
        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(skillType);
        final SkillConfig skillConfig = this.rda.getSkillConfig(skillType);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final SkillConfig.RateDefinition rate = this.findMaterialRate(
            skillConfig.getRatesByTrigger(SkillTriggerType.FURNACE_COLLECT),
            currentItem.getType()
        );
        if (rate != null) {
            progressionService.awardXp(player, rate, amount, rate.label());
            this.tryGrantActiveBonus(player, skillType, currentItem.clone());
        }
    }

    private void handleAnvilCollect(
        final @NotNull Player player,
        final @NotNull Inventory inventory,
        final int rawSlot,
        final @NotNull ItemStack resultItem
    ) {
        if (rawSlot != 2 || !this.isTrueEnchantApplication(inventory.getItem(0), inventory.getItem(1), resultItem)) {
            return;
        }

        final SkillProgressionService progressionService = this.rda.getSkillProgressionService(SkillType.ENCHANTING);
        final SkillConfig skillConfig = this.rda.getSkillConfig(SkillType.ENCHANTING);
        if (progressionService == null || skillConfig == null || !skillConfig.isEnabled()) {
            return;
        }

        final SkillConfig.RateDefinition rate = this.findMaterialRate(
            skillConfig.getRatesByTrigger(SkillTriggerType.ANVIL_ENCHANT),
            resultItem.getType()
        );
        if (rate != null) {
            progressionService.awardXp(player, rate, 1.0D, rate.label());
            this.tryGrantEnchantingBonus(player);
        }
    }

    private void tryGrantActiveBonus(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final @NotNull ItemStack bonusItem
    ) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null || !buildService.isSkillActive(player, skillType)) {
            return;
        }

        player.getInventory().addItem(bonusItem);
        if (skillType == SkillType.COOKING) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SATURATION,
                100,
                0,
                true,
                false,
                false
            ));
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.REGENERATION,
                100,
                0,
                true,
                false,
                false
            ));
        }
    }

    private void tryGrantEnchantingBonus(final @NotNull Player player) {
        final PlayerBuildService buildService = this.rda.getPlayerBuildService();
        if (buildService == null || !buildService.isSkillActive(player, SkillType.ENCHANTING)) {
            return;
        }

        player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
    }

    private boolean isTrueEnchantApplication(
        final @Nullable ItemStack leftItem,
        final @Nullable ItemStack rightItem,
        final @NotNull ItemStack resultItem
    ) {
        if (!this.isValidItem(resultItem)) {
            return false;
        }

        final ItemMeta resultMeta = resultItem.getItemMeta();
        if (resultMeta == null || resultMeta.getEnchants().isEmpty()) {
            return false;
        }

        if (this.isValidItem(rightItem) && rightItem.getType() == Material.ENCHANTED_BOOK) {
            return true;
        }

        final Map<org.bukkit.enchantments.Enchantment, Integer> leftEnchants =
            leftItem != null && leftItem.hasItemMeta() ? leftItem.getItemMeta().getEnchants() : Map.of();
        for (final Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : resultMeta.getEnchants().entrySet()) {
            if (entry.getValue() > leftEnchants.getOrDefault(entry.getKey(), 0)) {
                return true;
            }
        }

        return false;
    }

    private @Nullable SkillConfig.RateDefinition findMaterialRate(
        final @NotNull List<SkillConfig.RateDefinition> rates,
        final @NotNull Material material
    ) {
        SkillConfig.RateDefinition fallback = null;
        for (final SkillConfig.RateDefinition rate : rates) {
            if (rate.materials().isEmpty() && fallback == null) {
                fallback = rate;
                continue;
            }

            if (rate.matchesMaterial(material)) {
                return rate;
            }
        }
        return fallback;
    }

    private boolean isOutputExtractionAction(final @NotNull InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL,
                PICKUP_HALF,
                PICKUP_ONE,
                PICKUP_SOME,
                MOVE_TO_OTHER_INVENTORY,
                HOTBAR_SWAP,
                HOTBAR_MOVE_AND_READD,
                SWAP_WITH_CURSOR,
                COLLECT_TO_CURSOR -> true;
            default -> false;
        };
    }

    private int resolveCraftAmount(final @NotNull CraftItemEvent event, final @NotNull ItemStack result) {
        final int resultAmount = Math.max(1, result.getAmount());
        if (!event.isShiftClick()) {
            return resultAmount;
        }

        int craftableSets = Integer.MAX_VALUE;
        for (final ItemStack ingredient : event.getInventory().getMatrix()) {
            if (!this.isValidItem(ingredient)) {
                continue;
            }
            craftableSets = Math.min(craftableSets, ingredient.getAmount());
        }
        if (craftableSets == Integer.MAX_VALUE) {
            craftableSets = 1;
        }

        final int totalProduced = craftableSets * resultAmount;
        return Math.min(totalProduced, this.calculateInventoryFit((Player) event.getWhoClicked(), result));
    }

    private int resolveCollectedAmount(final @NotNull InventoryClickEvent event, final @NotNull ItemStack currentItem) {
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return Math.min(currentItem.getAmount(), this.calculateInventoryFit((Player) event.getWhoClicked(), currentItem));
        }
        if (event.isRightClick()) {
            return Math.max(1, (int) Math.ceil(currentItem.getAmount() / 2.0D));
        }
        return currentItem.getAmount();
    }

    private int calculateInventoryFit(final @NotNull Player player, final @NotNull ItemStack stack) {
        int fit = 0;
        for (final ItemStack contents : player.getInventory().getStorageContents()) {
            if (contents == null || contents.getType() == Material.AIR) {
                fit += stack.getMaxStackSize();
                continue;
            }

            if (contents.isSimilar(stack)) {
                fit += Math.max(0, contents.getMaxStackSize() - contents.getAmount());
            }
        }
        return fit;
    }

    private boolean isValidItem(final @Nullable ItemStack itemStack) {
        return itemStack != null && itemStack.getType() != Material.AIR;
    }
}
