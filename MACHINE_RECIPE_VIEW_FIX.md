# Machine Recipe View - Interactive Fix Complete

## Problem
The MachineRecipeView had multiple issues:
- Could not place items in the recipe grid
- Could not toggle buttons
- Could not save recipes
- Used instance field instead of State for multi-player support
- Missing proper click handlers

## Solution Applied

### 1. Fixed Item Insertion Pattern (Based on BountyRewardView)

**Added proper State management:**
```java
private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");
```

**Implemented onClick() override:**
```java
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
```

### 2. Proper Recipe Grid Rendering

**Changed from lambda-based to slot-based rendering:**
- Render panes first with `layoutSlot('R', ...)`
- Then overlay inserted items with `render.slot(slot, item)`
- Attach click handlers to enable interaction

### 3. Click Handlers

**handleRecipeSlotClick():**
- Left-click: Place item from cursor (only 1 item)
- Right-click: Remove item and return to player
- Prevents interaction when recipe is locked

**handleShiftClick():**
- Finds first empty pane slot
- Places item from player inventory
- Removes item from player inventory

### 4. Helper Methods

**Added utility methods:**
- `findFirstPaneSlot()` - Finds empty slots for shift-click
- `getRecipeSlotPosition()` - Converts recipe index (0-8) to GUI slot
- `getRecipeIndexFromSlot()` - Converts GUI slot to recipe index (0-8)
- `buildRecipePane()` - Creates interactive pane items

### 5. Fixed Button Interactions

**Set Recipe Button:**
- Now properly reads from `insertedItems` State
- Converts GUI slots to recipe indices before saving
- Uses Bukkit scheduler for async callbacks

**Clear Recipe Button:**
- Checks if machine is active
- Uses Bukkit scheduler for async callbacks

**Validation Status:**
- Shows 3 states: Valid (green), Ready (yellow), Pending (red)
- Updates based on inserted items

## Translation Keys Added

Added complete i18n support to `en_US.yml`:

```yaml
view:
  machine:
    recipe:
      title: "📋 Machine Recipe"
      items:
        recipe-slot:
          configure: # Empty slot
          locked: # Recipe locked
          empty: # Empty in locked state
        set-recipe: # Set button
        clear-recipe: # Clear button
        validation:
          valid: # Recipe is set
          ready: # Items placed, ready to set
          pending: # No items placed
      messages:
        recipe-already-set
        recipe-empty
        recipe-set
        recipe-invalid
        no-recipe-to-clear
        recipe-cleared
        recipe-clear-active
```

## Key Improvements

1. **Multi-player Support**: Uses State instead of instance field
2. **Proper Click Handling**: Follows BountyRewardView pattern
3. **Visual Feedback**: Three-state validation indicator
4. **Shift-Click Support**: Can shift-click items from inventory
5. **Thread Safety**: Uses Bukkit scheduler for async operations
6. **Recipe Locking**: Prevents modification when machine is active

## Testing Checklist

- [ ] Can place items in recipe grid by clicking with item
- [ ] Can remove items by right-clicking
- [ ] Can shift-click items from player inventory
- [ ] Set Recipe button works and locks recipe
- [ ] Clear Recipe button works (only when machine is OFF)
- [ ] Validation indicator shows correct state
- [ ] Recipe persists after setting
- [ ] Multiple players can use view simultaneously
- [ ] Locked recipe shows preview correctly

## Files Modified

1. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/view/MachineRecipeView.java`
   - Complete rewrite following BountyRewardView pattern
   - Added State management
   - Added proper click handlers
   - Added helper methods

2. `RDQ/rdq-common/src/main/resources/translations/en_US.yml`
   - Added complete `view.machine.recipe` section
   - All translation keys for items and messages

## Related Files

- Reference: `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/view/bounty/BountyRewardView.java`
- Steering: `.kiro/steering/inventory-framework.md`
- Steering: `.kiro/steering/jextranslate.md`
