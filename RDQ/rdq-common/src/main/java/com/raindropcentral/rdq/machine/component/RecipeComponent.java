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
import com.raindropcentral.rdq.machine.type.EMachineState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Component responsible for recipe management and validation.
 *
 * <p>This component handles recipe validation against the Minecraft crafting system,
 * recipe data storage as JSON, recipe locking/unlocking, and ingredient matching.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class RecipeComponent {

    private final Machine machine;
    private final FabricatorSection config;

    /**
     * Constructs a new Recipe component.
     *
     * @param machine the machine entity this component manages
     * @param config  the Fabricator configuration section
     */
    public RecipeComponent(
        final @NotNull Machine machine,
        final @NotNull FabricatorSection config
    ) {
        this.machine = machine;
        this.config = config;
    }

    /**
     * Validates a recipe against the Minecraft crafting system.
     *
     * <p>This method checks if the provided ingredients form a valid crafting recipe
     * by iterating through all registered recipes and attempting to match the pattern.
     *
     * @param ingredients array of ItemStacks representing the crafting grid
     * @return the result ItemStack if valid, null otherwise
     */
    @Nullable
    public ItemStack validateRecipe(final @NotNull ItemStack[] ingredients) {
        final int gridSize = config.getCrafting().getRecipeGridSize();
        final int expectedSize = gridSize * gridSize;

        if (ingredients.length != expectedSize) {
            return null;
        }

        // Try to match against all registered recipes
        final Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            final Recipe recipe = recipeIterator.next();

            if (recipe instanceof ShapedRecipe shapedRecipe) {
                final ItemStack result = matchShapedRecipe(shapedRecipe, ingredients, gridSize);
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
     * @param gridSize    the size of the crafting grid (3 for 3x3)
     * @return the result ItemStack if matched, null otherwise
     */
    @Nullable
    private ItemStack matchShapedRecipe(
        final @NotNull ShapedRecipe recipe,
        final @NotNull ItemStack[] ingredients,
        final int gridSize
    ) {
        final String[] shape = recipe.getShape();
        final Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();

        // Convert shape to grid
        final ItemStack[] recipeGrid = new ItemStack[gridSize * gridSize];
        int row = 0;
        for (final String line : shape) {
            for (int col = 0; col < gridSize && col < line.length(); col++) {
                final char key = line.charAt(col);
                if (key != ' ') {
                    recipeGrid[row * gridSize + col] = ingredientMap.get(key);
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
        for (int i = 0; i < recipeGrid.length; i++) {
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
     * Locks the recipe with the provided ingredients.
     *
     * <p>This stores the recipe data as JSON in the machine entity.
     * The recipe must be validated before calling this method.
     *
     * @param ingredients the crafting grid ingredients to lock
     * @return true if recipe was locked successfully, false if machine is active
     */
    public boolean lockRecipe(final @NotNull ItemStack[] ingredients) {
        if (machine.isActive()) {
            return false;
        }

        final String json = serializeRecipe(ingredients);
        machine.setRecipeData(json);
        return true;
    }

    /**
     * Unlocks the current recipe.
     *
     * <p>This clears the recipe data from the machine entity.
     * The machine must be in INACTIVE state to unlock the recipe.
     *
     * @return true if recipe was unlocked successfully, false if machine is active
     */
    public boolean unlockRecipe() {
        if (machine.isActive()) {
            return false;
        }

        machine.setRecipeData(null);
        return true;
    }

    /**
     * Checks if a recipe is currently locked.
     *
     * @return true if recipe is locked, false otherwise
     */
    public boolean isRecipeLocked() {
        return machine.getRecipeData() != null && !machine.getRecipeData().isEmpty();
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

        return deserializeRecipe(recipeData);
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

    /**
     * Checks if the provided ingredients match the locked recipe.
     *
     * @param ingredients the ingredients to check
     * @return true if ingredients match the locked recipe, false otherwise
     */
    public boolean matchesLockedRecipe(final @NotNull ItemStack[] ingredients) {
        final ItemStack[] lockedRecipe = getLockedRecipe();
        if (lockedRecipe == null) {
            return false;
        }

        if (ingredients.length != lockedRecipe.length) {
            return false;
        }

        for (int i = 0; i < ingredients.length; i++) {
            if (!itemsMatch(lockedRecipe[i], ingredients[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Serializes a recipe to JSON format.
     *
     * <p>This is a simplified JSON serialization. In production, use a proper
     * JSON library for more robust serialization.
     *
     * @param ingredients the recipe ingredients
     * @return the JSON string
     */
    @NotNull
    private String serializeRecipe(final @NotNull ItemStack[] ingredients) {
        final StringBuilder json = new StringBuilder("{\"ingredients\":[");

        for (int i = 0; i < ingredients.length; i++) {
            if (i > 0) {
                json.append(",");
            }

            final ItemStack item = ingredients[i];
            if (item == null || item.getType() == Material.AIR) {
                json.append("null");
            } else {
                json.append("{\"type\":\"")
                    .append(item.getType().name())
                    .append("\",\"amount\":")
                    .append(item.getAmount())
                    .append("}");
            }
        }

        json.append("]}");
        return json.toString();
    }

    /**
     * Deserializes a recipe from JSON format.
     *
     * <p>This is a simplified JSON parser. In production, use a proper
     * JSON library for more robust parsing.
     *
     * @param json the JSON string
     * @return array of ItemStacks representing the recipe
     */
    @NotNull
    private ItemStack[] deserializeRecipe(final @NotNull String json) {
        final int gridSize = config.getCrafting().getRecipeGridSize();
        final ItemStack[] ingredients = new ItemStack[gridSize * gridSize];

        // Parse simple JSON format
        final String ingredientsStr = json.substring(
            json.indexOf("[") + 1,
            json.lastIndexOf("]")
        );

        if (ingredientsStr.trim().isEmpty()) {
            return ingredients;
        }

        final String[] items = ingredientsStr.split(",(?=\\{|null)");
        for (int i = 0; i < Math.min(items.length, ingredients.length); i++) {
            final String item = items[i].trim();
            if (!item.equals("null")) {
                try {
                    // Extract type and amount
                    final String type = item.substring(
                        item.indexOf("\"type\":\"") + 8,
                        item.indexOf("\",\"amount\"")
                    );
                    final String amountStr = item.substring(
                        item.indexOf("\"amount\":") + 9,
                        item.indexOf("}")
                    );

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
     * Gets the recipe grid size.
     *
     * @return the grid size (e.g., 3 for 3x3)
     */
    public int getRecipeGridSize() {
        return config.getCrafting().getRecipeGridSize();
    }

    /**
     * Clears the recipe if the machine is inactive.
     *
     * @return true if recipe was cleared, false if machine is active
     */
    public boolean clearRecipe() {
        return unlockRecipe();
    }
}
