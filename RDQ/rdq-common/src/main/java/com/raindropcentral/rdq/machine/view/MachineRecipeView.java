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

package com.raindropcentral.rdq.machine.view;

import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.machine.IMachineService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * View for configuring machine crafting recipes.
 *
 * <p>This view provides:
 * <ul>
 *   <li>3x3 crafting grid for recipe configuration</li>
 *   <li>Set Recipe button to lock in the recipe</li>
 *   <li>Recipe validation status display</li>
 *   <li>Recipe preview when locked</li>
 *   <li>Clear Recipe button</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MachineRecipeView extends BaseView {

    private final State<IMachineService> machineService = initialState("machineService");
    private final State<Machine> machine = initialState("machine");
    private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");
    private final State<Integer> recipeVersion = initialState("recipeVersion");

    /**
     * Constructs a new {@code MachineRecipeView}.
     */
    public MachineRecipeView() {
        super(MachineMainView.class);
    }

    @Override
    protected String getKey() {
        return "view.machine.recipe";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "XXRRRXXXX",
            "XXRRROXXX",
            "XXRRRXXXX",
            "XXXXXXXXX",
            "   scv   "
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false; // Don't auto-fill to allow recipe grid interaction
    }

    @Override
    public void onFirstRender(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        // Render decoration
        render.layoutSlot('X', createFillItem(player));

        // Render crafting grid
        renderCraftingGrid(render, player);

        // Render output preview
        renderOutputPreview(render, player);

        // Render control buttons
        renderSetButton(render, player);
        renderClearButton(render, player);
        renderValidationStatus(render, player);
    }

    @Override
    public void onClick(@NotNull final SlotClickContext click) {
        // Handle shift-click from player inventory
        if (click.isShiftClick() && click.getClickedContainer().isEntityContainer()) {
            handleShiftClick(click);
            return;
        }

        // Allow normal clicks in player inventory
        if (!click.isShiftClick() && click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(false);
        }
    }

    /**
     * Renders the 3x3 crafting grid.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderCraftingGrid(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean hasRecipe = machine.get(render).getRecipeData() != null && !machine.get(render).getRecipeData().isEmpty();

        // Render all 9 recipe slots (3x3 grid) as interactive panes
        render.layoutSlot('R', buildRecipePane(player, hasRecipe ? "locked" : "configure"))
            .onClick(this::handleRecipeSlotClick);
        
        // Render any existing inserted items on top
        if (!hasRecipe && insertedItems.get(render).containsKey(player.getUniqueId())) {
            Map<Integer, ItemStack> playerItems = insertedItems.get(render).get(player.getUniqueId());
            if (!playerItems.isEmpty()) {
                playerItems.forEach((slot, item) -> 
                    render.slot(slot, item.clone()).onClick(this::handleRecipeSlotClick)
                );
            }
        } else if (hasRecipe) {
            // Show locked recipe preview
            for (int i = 0; i < 9; i++) {
                final ItemStack recipeItem = getRecipeSlotItem(machine.get(render).getRecipeData(), i);
                if (recipeItem != null && recipeItem.getType() != Material.AIR) {
                    final int slotIndex = getRecipeSlotPosition(i);
                    render.slot(slotIndex, UnifiedBuilderFactory.item(recipeItem.getType())
                        .setAmount(recipeItem.getAmount())
                        .setName(
                            i18n("items.recipe-slot.locked.name", player)
                                .withPlaceholder("item", recipeItem.getType().name())
                                .build()
                                .component()
                        )
                        .setLore(
                            i18n("items.recipe-slot.locked.lore", player)
                                .build()
                                .children()
                        )
                        .build());
                }
            }
        }
    }

    /**
     * Handles clicks on recipe slots.
     */
    private void handleRecipeSlotClick(@NotNull final SlotClickContext click) {
        final Player player = click.getPlayer();
        final ItemStack cursorItem = click.getClickOrigin().getCursor();
        final int clickedSlot = click.getClickedSlot();
        final ItemStack currentSlotItem = click.getClickOrigin().getCurrentItem();
        
        // Don't allow interaction if recipe is locked
        final boolean hasRecipe = machine.get(click).getRecipeData() != null && !machine.get(click).getRecipeData().isEmpty();
        if (hasRecipe) {
            click.setCancelled(true);
            return;
        }
        
        Map<Integer, ItemStack> playerItems = insertedItems.get(click).computeIfAbsent(
            player.getUniqueId(),
            k -> new HashMap<>()
        );
        
        // Prevent shift-click in entity container
        if (click.getClickedContainer().isEntityContainer() && click.isShiftClick()) {
            click.setCancelled(true);
            return;
        }
        
        boolean isSlotEmptyOrPane = currentSlotItem == null 
            || currentSlotItem.getType() == Material.AIR 
            || currentSlotItem.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE;
        
        if (click.isLeftClick()) {
            // Place item from cursor
            if (isSlotEmptyOrPane && cursorItem != null && cursorItem.getType() != Material.AIR) {
                ItemStack placedItem = cursorItem.clone();
                placedItem.setAmount(1); // Only use 1 item for recipe
                
                click.getClickOrigin().setCursor(null);
                playerItems.put(clickedSlot, placedItem);
                
                click.getClickedContainer().renderItem(clickedSlot, placedItem);
                
                // Remove 1 from cursor if it had more
                if (cursorItem.getAmount() > 1) {
                    ItemStack remaining = cursorItem.clone();
                    remaining.setAmount(cursorItem.getAmount() - 1);
                    click.getClickOrigin().setCursor(remaining);
                }
                
                // Trigger output preview update
                recipeVersion.set(click, recipeVersion.get(click) + 1);
            }
        } else if (click.isRightClick()) {
            // Remove item from slot
            if (!isSlotEmptyOrPane && currentSlotItem.getType() != Material.AIR) {
                ItemStack removed = playerItems.remove(clickedSlot);
                if (removed != null) {
                    // Give item back to player
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(removed);
                    } else {
                        player.getWorld().dropItem(player.getLocation(), removed);
                    }
                }
                
                click.getClickedContainer().renderItem(
                    clickedSlot,
                    buildRecipePane(player, "configure")
                );
                
                // Trigger output preview update
                recipeVersion.set(click, recipeVersion.get(click) + 1);
            }
        }
    }

    /**
     * Handles shift-click from player inventory to recipe grid.
     */
    private void handleShiftClick(@NotNull final SlotClickContext click) {
        final Player player = click.getPlayer();
        final ItemStack clickedItem = click.getClickOrigin().getCurrentItem();
        
        // Don't allow if recipe is locked
        final boolean hasRecipe = machine.get(click).getRecipeData() != null && !machine.get(click).getRecipeData().isEmpty();
        if (hasRecipe) {
            click.setCancelled(true);
            return;
        }
        
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            Inventory guiInv = player.getOpenInventory().getTopInventory();
            int targetSlot = findFirstPaneSlot(guiInv, Set.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
            
            if (targetSlot != -1) {
                ItemStack placedItem = clickedItem.clone();
                placedItem.setAmount(1);
                
                player.getInventory().removeItem(clickedItem);
                guiInv.setItem(targetSlot, placedItem);
                
                insertedItems.get(click)
                    .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(targetSlot, placedItem);
                
                click.setCancelled(true);
                return;
            }
        }
        
        click.setCancelled(true);
    }

    /**
     * Finds the first pane slot in the inventory.
     */
    private int findFirstPaneSlot(@NotNull final Inventory inv, @NotNull final Set<Material> paneTypes) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slotItem = inv.getItem(i);
            if (slotItem != null && paneTypes.contains(slotItem.getType())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Renders the Set Recipe button.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderSetButton(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean hasRecipe = machine.get(render).getRecipeData() != null && !machine.get(render).getRecipeData().isEmpty();
        
        Map<Integer, ItemStack> playerItems = insertedItems.get(render).computeIfAbsent(
            player.getUniqueId(),
            k -> new HashMap<>()
        );

        render.layoutSlot('s')
            .withItem(
                UnifiedBuilderFactory.item(hasRecipe ? Material.GRAY_WOOL : Material.GREEN_WOOL)
                    .setName(
                        i18n("items.set-recipe.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.set-recipe.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                if (hasRecipe) {
                    i18n("messages.recipe-already-set", player)
                        .build()
                        .sendMessage();
                    return;
                }

                // Validate recipe has at least one item
                if (playerItems.isEmpty()) {
                    i18n("messages.recipe-empty", player)
                        .build()
                        .sendMessage();
                    return;
                }

                // Convert map to array (map keys are GUI slots, need to convert to recipe indices)
                final ItemStack[] recipeArray = new ItemStack[9];
                playerItems.forEach((guiSlot, item) -> {
                    int recipeIndex = getRecipeIndexFromSlot(guiSlot);
                    if (recipeIndex >= 0 && recipeIndex < 9) {
                        recipeArray[recipeIndex] = item.clone();
                    }
                });

                // Set recipe
                machineService.get(click).setRecipe(machine.get(click).getId(), recipeArray)
                    .thenAccept(success -> {
                        if (success) {
                            // Update machine recipe data
                            machine.get(click).setRecipeData(serializeRecipe(recipeArray));
                            
                            // Clear temporary recipe items
                            playerItems.clear();
                            
                            // Send success message on main thread
                            /*Bukkit.getScheduler().runTask(
                                machineService.get(click).getPlugin(),
                                () -> {
                                    i18n("messages.recipe-set", player)
                                        .build()
                                        .sendMessage();
                                    click.update();
                                }
                            );*/
                        } else {
                            /*Bukkit.getScheduler().runTask(
                                machineService.get(click).getPlugin(),
                                () -> i18n("messages.recipe-invalid", player)
                                    .build()
                                    .sendMessage()
                            );*/
                        }
                    });
            });
    }

    /**
     * Renders the Clear Recipe button.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderClearButton(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean hasRecipe = machine.get(render).getRecipeData() != null && !machine.get(render).getRecipeData().isEmpty();

        render.layoutSlot('c')
            .withItem(
                UnifiedBuilderFactory.item(Material.RED_WOOL)
                    .setName(
                        i18n("items.clear-recipe.name", player)
                            .build()
                            .component()
                    )
                    .setLore(
                        i18n("items.clear-recipe.lore", player)
                            .build()
                            .children()
                    )
                    .build()
            )
            .onClick(click -> {
                if (!hasRecipe) {
                    i18n("messages.no-recipe-to-clear", player)
                        .build()
                        .sendMessage();
                    return;
                }

                // Only allow clearing when machine is OFF
                if (machine.get(click).isActive()) {
                    i18n("messages.recipe-clear-active", player)
                        .build()
                        .sendMessage();
                    return;
                }

                // Extract items from recipe before clearing
                final ItemStack[] recipeItems = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    recipeItems[i] = getRecipeSlotItem(machine.get(click).getRecipeData(), i);
                }

                // Clear recipe
                machine.get(click).setRecipeData(null);
                machineService.get(click).setRecipe(machine.get(click).getId(), new ItemStack[9])
                    .thenAccept(success -> {
                        if (success) {
                            // Return items to player
                            for (ItemStack item : recipeItems) {
                                if (item != null && item.getType() != Material.AIR) {
                                    player.getInventory().addItem(item).forEach((index, leftover) -> {
                                        player.getWorld().dropItem(player.getLocation(), leftover);
                                    });
                                }
                            }
                            
                            i18n("messages.recipe-cleared", player)
                                .build()
                                .sendMessage();
                            click.update();
                        }
                    });
            });
    }

    /**
     * Renders the recipe validation status indicator.
     *
     * @param render the render context
     * @param player the viewing player
     */
    private void renderValidationStatus(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean hasRecipe = machine.get(render).getRecipeData() != null && !machine.get(render).getRecipeData().isEmpty();
        
        Map<Integer, ItemStack> playerItems = insertedItems.get(render).computeIfAbsent(
            player.getUniqueId(),
            k -> new HashMap<>()
        );

        final ItemStack validationItem;
        if (hasRecipe) {
            validationItem = UnifiedBuilderFactory.item(Material.GREEN_STAINED_GLASS_PANE)
                .setName(
                    i18n("items.validation.valid.name", player)
                        .build()
                        .component()
                )
                .setLore(
                    i18n("items.validation.valid.lore", player)
                        .build()
                        .children()
                )
                .build();
        } else if (!playerItems.isEmpty()) {
            validationItem = UnifiedBuilderFactory.item(Material.YELLOW_STAINED_GLASS_PANE)
                .setName(
                    i18n("items.validation.ready.name", player)
                        .build()
                        .component()
                )
                .setLore(
                    i18n("items.validation.ready.lore", player)
                        .build()
                        .children()
                )
                .build();
        } else {
            validationItem = UnifiedBuilderFactory.item(Material.RED_STAINED_GLASS_PANE)
                .setName(
                    i18n("items.validation.pending.name", player)
                        .build()
                        .component()
                )
                .setLore(
                    i18n("items.validation.pending.lore", player)
                        .build()
                        .children()
                )
                .build();
        }

        render.layoutSlot('v')
            .withItem(validationItem);
    }

    /**
     * Renders the recipe output preview.
     */
    private void renderOutputPreview(
        @NotNull final RenderContext render,
        @NotNull final Player player
    ) {
        final boolean hasRecipe = machine.get(render).getRecipeData() != null && 
            !machine.get(render).getRecipeData().isEmpty();
        
        Map<Integer, ItemStack> playerItems = insertedItems.get(render)
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        
        ItemStack outputItem = null;
        
        if (hasRecipe) {
            // Get output from locked recipe
            outputItem = calculateRecipeOutput(machine.get(render).getRecipeData());
        } else if (!playerItems.isEmpty()) {
            // Get output from current configuration
            final ItemStack[] recipeArray = new ItemStack[9];
            playerItems.forEach((guiSlot, item) -> {
                int recipeIndex = getRecipeIndexFromSlot(guiSlot);
                if (recipeIndex >= 0 && recipeIndex < 9) {
                    recipeArray[recipeIndex] = item.clone();
                }
            });
            outputItem = calculateRecipeOutput(recipeArray);
        }
        
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            render.layoutSlot('O', UnifiedBuilderFactory.item(outputItem.getType())
                .setAmount(outputItem.getAmount())
                .setName(
                    i18n("items.output.name", player)
                        .withPlaceholder("item", outputItem.getType().name())
                        .withPlaceholder("amount", outputItem.getAmount())
                        .build()
                        .component()
                )
                .setLore(
                    i18n("items.output.lore", player)
                        .build()
                        .children()
                )
                .build())
                .updateOnStateChange(recipeVersion);
        } else {
            render.layoutSlot('O', UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(
                    i18n("items.output.none.name", player)
                        .build()
                        .component()
                )
                .setLore(
                    i18n("items.output.none.lore", player)
                        .build()
                        .children()
                )
                .build())
                .updateOnStateChange(recipeVersion);
        }
    }

    /**
     * Calculates the output of a recipe using Minecraft's crafting system.
     */
    @Nullable
    private ItemStack calculateRecipeOutput(@NotNull final ItemStack[] recipeItems) {
        try {
            // Create a crafting inventory
            org.bukkit.inventory.CraftingInventory craftingInv = 
                org.bukkit.Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.WORKBENCH);
            
            // Set the recipe items
            craftingInv.setMatrix(recipeItems);
            
            // Get the result
            return craftingInv.getResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculates output from serialized recipe data.
     */
    @Nullable
    private ItemStack calculateRecipeOutput(@NotNull final String recipeData) {
        final ItemStack[] recipeArray = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            recipeArray[i] = getRecipeSlotItem(recipeData, i);
            if (recipeArray[i] == null) {
                recipeArray[i] = new ItemStack(Material.AIR);
            }
        }
        return calculateRecipeOutput(recipeArray);
    }

    /**
     * Builds a recipe slot pane.
     */
    private ItemStack buildRecipePane(
        @NotNull final Player player,
        @NotNull final String type
    ) {
        return UnifiedBuilderFactory.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .setName(
                i18n("items.recipe-slot." + type + ".name", player)
                    .build()
                    .component()
            )
            .setLore(
                i18n("items.recipe-slot." + type + ".lore", player)
                    .build()
                    .children()
            )
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Gets the GUI slot position for a recipe index (0-8).
     */
    private int getRecipeSlotPosition(int recipeIndex) {
        // Layout is 9x6, recipe grid starts at row 1, col 2
        // R positions: 10,11,12,19,20,21,28,29,30
        int row = recipeIndex / 3;
        int col = recipeIndex % 3;
        return (row + 1) * 9 + (col + 2);
    }

    /**
     * Gets the recipe index (0-8) from a GUI slot position.
     */
    private int getRecipeIndexFromSlot(int slot) {
        // Reverse of getRecipeSlotPosition
        int[] recipeSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int i = 0; i < recipeSlots.length; i++) {
            if (recipeSlots[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the item at a specific recipe slot index from serialized data.
     *
     * @param recipeData the serialized recipe data
     * @param slotIndex the slot index (0-8)
     * @return the ItemStack at that slot, or null
     */
    @Nullable
    private ItemStack getRecipeSlotItem(@NotNull final String recipeData, final int slotIndex) {
        if (recipeData == null || recipeData.isEmpty() || recipeData.equals("{}")) {
            return null;
        }
        
        try {
            // Simple JSON parsing for recipe data
            // Format: {"0":"MATERIAL:AMOUNT","1":"MATERIAL:AMOUNT",...}
            final String[] entries = recipeData.replace("{", "").replace("}", "").replace("\"", "").split(",");
            
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                
                final String[] parts = entry.split(":");
                if (parts.length >= 2) {
                    final int index = Integer.parseInt(parts[0].trim());
                    if (index == slotIndex) {
                        final String materialName = parts[1].trim();
                        final int amount = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 1;
                        
                        try {
                            final Material material = Material.valueOf(materialName);
                            return new ItemStack(material, amount);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }

    /**
     * Serializes recipe items to JSON.
     *
     * @param items the recipe items
     * @return JSON string representation
     */
    @NotNull
    private String serializeRecipe(@NotNull final ItemStack[] items) {
        final StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (int i = 0; i < items.length; i++) {
            final ItemStack item = items[i];
            if (item != null && item.getType() != Material.AIR) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(i).append("\":\"")
                    .append(item.getType().name()).append(":")
                    .append(item.getAmount()).append("\"");
                first = false;
            }
        }
        
        json.append("}");
        return json.toString();
    }
}
