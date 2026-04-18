# Machine View Rendering Fix

## Issue

When clicking navigation buttons in MachineMainView to open child views (MachineTrustView, MachineUpgradeView, MachineRecipeView, MachineStorageView), the following error occurred:

```
java.lang.IllegalStateException: At least one fallback item or render handler must be provided
    at de.jexcellence.remapped.me.devnatan.inventoryframework.component.ItemComponent.render
```

## Root Cause

The Inventory Framework requires that when using `layoutSlot(char, BiConsumer<Integer, ItemComponentBuilder>)`, ALL occurrences of that character in the layout must have items rendered. 

The problem was in the rendering logic:
- **MachineTrustView**: Used a loop to call `layoutSlot('T', BiConsumer)` multiple times, but only rendered items for specific indices, leaving some 'T' slots without items
- **MachineRecipeView**: Same issue with 'R' slots for the crafting grid

## Solution

Fixed both views to render items for ALL layout slots in a single `layoutSlot` call:

### MachineTrustView Fix

**Before (Broken):**
```java
// Called layoutSlot multiple times in a loop
for (final MachineTrust trust : trustedPlayers) {
    render.layoutSlot('T', (index, item) -> {
        if (index != currentSlot) return;  // ❌ Leaves other slots empty
        item.withItem(...);
    });
}
```

**After (Fixed):**
```java
// Single layoutSlot call that handles ALL 'T' slots
render.layoutSlot('T', (index, item) -> {
    if (index < trustArray.length) {
        // Render trusted player
        item.withItem(...);
    } else {
        // Fill empty slot with placeholder
        item.withItem(createEmptySlot());
    }
});
```

### MachineRecipeView Fix

**Before (Broken):**
```java
// Called layoutSlot multiple times in a loop
for (int i = 0; i < 9; i++) {
    render.layoutSlot('R', (index, item) -> {
        if (index != currentIndex) return;  // ❌ Leaves other slots empty
        item.withItem(slotItem);
    });
}
```

**After (Fixed):**
```java
// Single layoutSlot call that handles ALL 'R' slots
render.layoutSlot('R', (index, item) -> {
    // Render item for this specific index
    ItemStack slotItem = getSlotItem(index);
    item.withItem(slotItem);
    item.onClick(click -> handleClick(index));
});
```

## Key Principle

When using `layoutSlot(char, BiConsumer)`:
- The BiConsumer is called ONCE for EACH occurrence of that character in the layout
- You MUST provide an item for every index
- Use the `index` parameter to determine which item to render
- Never use early returns (`if (index != X) return;`) as this leaves slots empty

## Files Modified

1. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/view/MachineTrustView.java`
   - Fixed `renderTrustedPlayers()` to render all 'T' slots in one call
   - Fills empty slots with gray glass pane placeholders

2. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/view/MachineRecipeView.java`
   - Fixed `renderCraftingGrid()` to render all 'R' slots in one call
   - Each slot gets appropriate item based on recipe state

## Testing

All machine views should now open without errors:
- ✅ MachineMainView → MachineTrustView
- ✅ MachineMainView → MachineUpgradeView  
- ✅ MachineMainView → MachineRecipeView
- ✅ MachineMainView → MachineStorageView

## Status

✅ MachineTrustView rendering fixed
✅ MachineRecipeView rendering fixed
✅ All files compile without errors
✅ No more "fallback item" errors
