# Machine Recipe View - Complete Fix

## Issues to Fix

### 1. Recipe Clearing Not Returning Items
**Problem:** When clearing recipe, items from the locked recipe are not returned to player.

**Root Cause:** The clear button only clears the database but doesn't extract items from the serialized recipe data.

**Solution:**
```java
// In renderClearButton onClick handler:
if (!hasRecipe) {
    i18n("messages.no-recipe-to-clear", player).build().sendMessage();
    return;
}

if (machine.get(click).isActive()) {
    i18n("messages.recipe-clear-active", player).build().sendMessage();
    return;
}

// Extract items from recipe before clearing
final ItemStack[] recipeItems = new ItemStack[9];
for (int i = 0; i < 9; i++) {
    recipeItems[i] = getRecipeSlotItem(machine.get(click).getRecipeData(), i);
}

// Clear recipe in database
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
            
            i18n("messages.recipe-cleared", player).build().sendMessage();
            click.update();
        }
    });
```

### 2. Recipe Output Preview Missing

**Problem:** No visual preview of what the recipe will craft.

**Solution:** Add slot 24 (center of row 3) for output preview.

**Layout Change:**
```java
@Override
protected String[] getLayout() {
    return new String[]{
        "XXXXXXXXX",
        "XXRRRXXXX",
        "XXRRRXXOX",  // O = output preview at slot 24
        "XXRRRXXXX",
        "XXXXXXXXX",
        "   scv   "
    };
}
```

**Add Output Preview Rendering:**
```java
@Override
public void onFirstRender(@NotNull final RenderContext render, @NotNull final Player player) {
    render.layoutSlot('X', createFillItem(player));
    renderCraftingGrid(render, player);
    renderOutputPreview(render, player);  // NEW
    renderSetButton(render, player);
    renderClearButton(render, player);
    renderValidationStatus(render, player);
}

/**
 * Renders the recipe output preview.
 */
private void renderOutputPreview(@NotNull final RenderContext render, @NotNull final Player player) {
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
            .build());
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
            .build());
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
        for (int i = 0; i < 9 && i < recipeItems.length; i++) {
            if (recipeItems[i] != null) {
                craftingInv.setMatrix(recipeItems);
            }
        }
        
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
    }
    return calculateRecipeOutput(recipeArray);
}
```

### 3. Dynamic Output Updates

**Problem:** Output preview doesn't update when items are placed/removed.

**Solution:** Use State and updateOnStateChange:

```java
// Add state for recipe changes
private final State<Integer> recipeVersion = initialState("recipeVersion");

// In handleRecipeSlotClick, after modifying playerItems:
if (click.isLeftClick()) {
    // ... place item logic ...
    recipeVersion.set(click, recipeVersion.get(click) + 1);  // Trigger update
} else if (click.isRightClick()) {
    // ... remove item logic ...
    recipeVersion.set(click, recipeVersion.get(click) + 1);  // Trigger update
}

// In renderOutputPreview:
render.layoutSlot('O', ...)
    .updateOnStateChange(recipeVersion);  // Auto-update when state changes
```

### 4. Translation Keys to Add

```yaml
view:
  machine:
    recipe:
      items:
        output:
          name: "<gradient:#10b981:#34d399>➜ Output: %item% x%amount%</gradient>"
          lore:
          - "<gray>This is what the recipe will craft</gray>"
          none:
            name: "<gradient:#6b7280:#9ca3af>➜ No Output</gradient>"
            lore:
            - "<gray>Place items in the grid to see output</gray>"
```

## Implementation Priority

1. ✅ Fix recipe clearing to return items (CRITICAL)
2. ✅ Add output preview at slot 24 (HIGH)
3. ✅ Add dynamic updates (MEDIUM)
4. ✅ Add translation keys (LOW)

## Testing Checklist

- [ ] Clear recipe returns all items to inventory
- [ ] Clear recipe drops items if inventory full
- [ ] Output preview shows correct item
- [ ] Output preview updates when placing items
- [ ] Output preview updates when removing items
- [ ] Output preview shows "No Output" for invalid recipes
- [ ] Output preview works with locked recipes
- [ ] Translation keys display correctly

## Files to Modify

1. `MachineRecipeView.java`
   - Fix renderClearButton() to return items
   - Add renderOutputPreview() method
   - Add calculateRecipeOutput() methods
   - Update layout to include 'O' slot
   - Add recipeVersion state for dynamic updates
   - Update handleRecipeSlotClick() to trigger updates

2. `en_US.yml`
   - Add view.machine.recipe.items.output section

## Related Issues

- Recipe clearing was broken due to not extracting items from serialized data
- No visual feedback for what recipe will craft
- Players couldn't see if their recipe configuration was valid

## Benefits

1. Players get their items back when clearing recipes
2. Visual feedback shows what will be crafted
3. Real-time preview helps validate recipe configuration
4. Better UX with immediate visual feedback
