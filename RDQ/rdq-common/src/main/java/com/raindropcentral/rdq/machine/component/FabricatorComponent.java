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

package com.raindropcentral.rdq.machine.component;

import com.raindropcentral.rdq.machine.config.FabricatorSection;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.type.EUpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Component responsible for Fabricator machine crafting logic.
 *
 * <p>This component handles recipe validation, crafting cycle execution,
 * upgrade modifier application, and recipe locking/unlocking. It coordinates
 * with other components to ensure materials, fuel, and upgrades are properly
 * applied during crafting operations.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class FabricatorComponent {

    private final Machine machine;
    private final FabricatorSection config;
    private final Random random;

    /**
     * Constructs a new Fabricator component.
     *
     * @param machine the machine entity this component manages
     * @param config  the Fabricator configuration section
     */
    public FabricatorComponent(
        final @NotNull Machine machine,
        final @NotNull FabricatorSection config
    ) {
        this.machine = machine;
        this.config = config;
        this.random = new Random();
    }

    /**
     * Validates a recipe against the Minecraft crafting system.
     *
     * <p>This method checks if the provided ingredients form a valid crafting recipe
     * by iterating through all registered recipes and attempting to match the pattern.
     *
     * @param ingredients array of ItemStacks representing the crafting grid (3x3)
     * @return the result ItemStack if valid, null otherwise
     */
    @Nullable
    public ItemStack validateRecipe(final @NotNull ItemStack[] ingredients) {
        if (ingredients.length != 9) {
            return null;
        }

        // Try to match against all registered recipes
        final Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            final Recipe recipe = recipeIterator.next();
            
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                final ItemStack result = matchShapedRecipe(shapedRecipe, ingredients);
                if (result != null) {
                    return result;
                }
            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                final ItemStack result = matchShapelessRecipe(shapelessRecipe, ingredients);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Attempts to match ingredients against a shaped recipe.
     *
     * @param recipe      the shaped recipe to match
     * @param ingredients the crafting grid ingredients
     * @return the result ItemStack if matched, null otherwise
     */
    @Nullable
    private ItemStack matchShapedRecipe(
        final @NotNull ShapedRecipe recipe,
        final @NotNull ItemStack[] ingredients
    ) {
        final String[] shape = recipe.getShape();
        final Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();

        // Convert shape to 3x3 grid
        final ItemStack[] recipeGrid = new ItemStack[9];
        int row = 0;
        for (final String line : shape) {
            for (int col = 0; col < 3 && col < line.length(); col++) {
                final char key = line.charAt(col);
                if (key != ' ') {
                    recipeGrid[row * 3 + col] = ingredientMap.get(key);
                }
            }
            row++;
        }

        // Check if ingredients match recipe grid
        if (matchesGrid(recipeGrid, ingredients)) {
            return recipe.getResult().clone();
        }

        return null;
    }

    /**
     * Attempts to match ingredients against a shapeless recipe.
     *
     * @param recipe      the shapeless recipe to match
     * @param ingredients the crafting grid ingredients
     * @return the result ItemStack if matched, null otherwise
     */
    @Nullable
    private ItemStack matchShapelessRecipe(
        final @NotNull ShapelessRecipe recipe,
        final @NotNull ItemStack[] ingredients
    ) {
        final List<ItemStack> recipeIngredients = new ArrayList<>(recipe.getIngredientList());
        final List<ItemStack> providedIngredients = new ArrayList<>();

        // Collect non-null ingredients
        for (final ItemStack item : ingredients) {
            if (item != null && item.getType() != Material.AIR) {
                providedIngredients.add(item);
            }
        }

        // Check if counts match
        if (recipeIngredients.size() != providedIngredients.size()) {
            return null;
        }

        // Try to match all ingredients
        for (final ItemStack provided : providedIngredients) {
            boolean matched = false;
            for (int i = 0; i < recipeIngredients.size(); i++) {
                final ItemStack required = recipeIngredients.get(i);
                if (itemsMatch(required, provided)) {
                    recipeIngredients.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return null;
            }
        }

        return recipe.getResult().clone();
    }

    /**
     * Checks if two grids match.
     *
     * @param recipeGrid     the recipe grid
     * @param ingredientGrid the provided ingredient grid
     * @return true if grids match, false otherwise
     */
    private boolean matchesGrid(
        final @NotNull ItemStack[] recipeGrid,
        final @NotNull ItemStack[] ingredientGrid
    ) {
        for (int i = 0; i < 9; i++) {
            if (!itemsMatch(recipeGrid[i], ingredientGrid[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if two ItemStacks match for recipe purposes.
     *
     * @param required the required item from recipe
     * @param provided the provided item
     * @return true if items match, false otherwise
     */
    private boolean itemsMatch(
        final @Nullable ItemStack required,
        final @Nullable ItemStack provided
    ) {
        if (required == null || required.getType() == Material.AIR) {
            return provided == null || provided.getType() == Material.AIR;
        }
        if (provided == null || provided.getType() == Material.AIR) {
            return false;
        }
        return required.getType() == provided.getType();
    }

    /**
     * Executes a crafting cycle with upgrade modifiers applied.
     *
     * <p>This method calculates the actual output considering bonus output upgrades
     * and returns the result. It does not consume materials or fuel - that should
     * be handled by the caller after this method confirms success.
     *
     * @param baseOutput the base crafting result
     * @return the actual output after applying upgrade modifiers
     */
    @NotNull
    public ItemStack executeCraftingCycle(final @NotNull ItemStack baseOutput) {
        final ItemStack result = baseOutput.clone();

        // Apply bonus output upgrade
        final int bonusLevel = machine.getUpgradeLevel(EUpgradeType.BONUS_OUTPUT);
        if (bonusLevel > 0) {
            final double bonusChance = config.getUpgrades()
                .getUpgrade(EUpgradeType.BONUS_OUTPUT)
                .getEffectPerLevel() * bonusLevel;

            if (random.nextDouble() < bonusChance) {
                // Double the output
                result.setAmount(Math.min(result.getAmount() * 2, config.getCrafting().getMaxOutputStackSize()));
            }
        }

        return result;
    }

    /**
     * Calculates the crafting cooldown with speed upgrade modifiers.
     *
     * @return the cooldown in ticks after applying speed upgrades
     */
    public int calculateCooldown() {
        final int baseCooldown = config.getCrafting().getBaseCooldownTicks();
        final int speedLevel = machine.getUpgradeLevel(EUpgradeType.SPEED);

        if (speedLevel == 0) {
            return baseCooldown;
        }

        final double speedReduction = config.getUpgrades()
            .getUpgrade(EUpgradeType.SPEED)
            .getEffectPerLevel() * speedLevel;

        return (int) (baseCooldown * (1.0 - speedReduction));
    }

    /**
     * Checks if the recipe is currently locked.
     *
     * @return true if recipe is locked, false otherwise
     */
    public boolean isRecipeLocked() {
        return machine.getRecipeData() != null && !machine.getRecipeData().isEmpty();
    }

    /**
     * Locks the recipe with the provided ingredients.
     *
     * <p>This stores the recipe data as JSON in the machine entity.
     * The recipe must be validated before calling this method.
     *
     * @param ingredients the crafting grid ingredients to lock
     */
    public void lockRecipe(final @NotNull ItemStack[] ingredients) {
        final StringBuilder json = new StringBuilder("{\"ingredients\":[");
        
        for (int i = 0; i < ingredients.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            
            final ItemStack item = ingredients[i];
            if (item == null || item.getType() == Material.AIR) {
                json.append("null");
            } else {
                json.append("{\"type\":\"").append(item.getType().name()).append("\",\"amount\":").append(item.getAmount()).append("}");
            }
        }
        
        json.append("]}");
        machine.setRecipeData(json.toString());
    }

    /**
     * Unlocks the current recipe.
     *
     * <p>This clears the recipe data from the machine entity.
     * The machine must be in INACTIVE state to unlock the recipe.
     */
    public void unlockRecipe() {
        machine.setRecipeData(null);
    }

    /**
     * Gets the locked recipe ingredients.
     *
     * @return array of ItemStacks representing the locked recipe, or null if no recipe is locked
     */
    @Nullable
    public ItemStack[] getLockedRecipe() {
        final String recipeData = machine.getRecipeData();
        if (recipeData == null || recipeData.isEmpty()) {
            return null;
        }

        // Parse simple JSON format
        final ItemStack[] ingredients = new ItemStack[9];
        // This is a simplified parser - in production, use a proper JSON library
        final String ingredientsStr = recipeData.substring(
            recipeData.indexOf("[") + 1,
            recipeData.lastIndexOf("]")
        );

        if (ingredientsStr.trim().isEmpty()) {
            return ingredients;
        }

        final String[] items = ingredientsStr.split(",(?=\\{|null)");
        for (int i = 0; i < Math.min(items.length, 9); i++) {
            final String item = items[i].trim();
            if (!item.equals("null")) {
                // Extract type and amount
                final String type = item.substring(
                    item.indexOf("\"type\":\"") + 8,
                    item.indexOf("\",\"amount\"")
                );
                final String amountStr = item.substring(
                    item.indexOf("\"amount\":") + 9,
                    item.indexOf("}")
                );
                
                try {
                    final Material material = Material.valueOf(type);
                    final int amount = Integer.parseInt(amountStr);
                    ingredients[i] = new ItemStack(material, amount);
                } catch (final IllegalArgumentException e) {
                    // Invalid material or amount, skip
                }
            }
        }

        return ingredients;
    }

    /**
     * Gets the expected output for the locked recipe.
     *
     * @return the expected output ItemStack, or null if no recipe is locked or recipe is invalid
     */
    @Nullable
    public ItemStack getExpectedOutput() {
        final ItemStack[] recipe = getLockedRecipe();
        if (recipe == null) {
            return null;
        }
        return validateRecipe(recipe);
    }
}
