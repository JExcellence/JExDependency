# Machine System Compilation Fix Progress

## Summary
Fixed approximately 75% of compilation errors in the machine system. Reduced from ~100 errors to 26 errors.

## Completed Fixes

### 1. Repository and Database Layer
- ✅ Fixed `MachineCache.java` - Removed invalid `cache()` and `clear()` method calls
- ✅ Fixed `MachineAutoSaveTask.java` - Added cast for `getCacheSize()` long to int conversion
- ✅ Fixed `MachineService.java` - Changed `repository.delete(machine)` to `repository.delete(machine.getId())`
- ✅ Fixed `MachineFactory.java` - Wrapped `repository.save()` in CompletableFuture instead of using non-existent `thenApply()`

### 2. Configuration Layer
- ✅ Fixed `FabricatorSection.java` - Removed redundant `afterParsing()` call on nested structure section

### 3. Main Plugin Integration
- ✅ Fixed `RDQ.java` - Changed `fabricatorConfig.getStructure().toMultiBlockStructure()` to `new MultiBlockStructure(fabricatorConfig.getStructure())`
- ✅ Fixed `RDQ.java` - Changed `plugin` to `this.plugin` in MachineItemFactory constructor

### 4. View Layer - MachineMainView
- ✅ Added `UnifiedBuilderFactory` import
- ✅ Fixed `renderStateToggle()` - Converted lambda to UnifiedBuilderFactory pattern
- ✅ Fixed `renderMachineType()` - Converted lambda to UnifiedBuilderFactory pattern
- ✅ Fixed `renderFuelDisplay()` - Converted lambda to UnifiedBuilderFactory pattern
- ✅ Fixed `renderRecipePreview()` - Converted lambda to UnifiedBuilderFactory pattern
- ✅ Fixed `renderNavigationButtons()` - Changed from `protected` to `public` and converted all three buttons to UnifiedBuilderFactory pattern
- ✅ Fixed `click.close()` to `click.getPlayer().closeInventory()`

### 5. View Layer - MachineRecipeView
- ✅ Added `UnifiedBuilderFactory` import
- ✅ Fixed `renderRecipeGrid()` - Refactored to build ItemStack first, then pass to `withItem()`
- ✅ Fixed `renderSetButton()` - Converted lambda to UnifiedBuilderFactory pattern

## Remaining Issues (26 errors)

### Pattern: All remaining errors follow the same patterns as those already fixed

### MachineMainView (3 errors)
- Lines 272, 295, 319: `layoutSlot(char, int)` with lambda - needs UnifiedBuilderFactory conversion

### MachineRecipeView (6 errors)
- Line 172: `layoutSlot(char, int)` with lambda
- Lines 228, 235, 276, 287: Symbol not found (likely `i18n` or similar)
- Lines 254, 307: `withItem` lambda issues

### MachineStorageView (4 errors)
- Lines 116, 164, 196, 216: `withItem` lambda issues - needs UnifiedBuilderFactory conversion

### MachineTrustView (6 errors)
- Lines 168, 206, 244, 282, 320, 358: `withItem` lambda issues - needs UnifiedBuilderFactory conversion

### MachineUpgradeView (7 errors)
- Lines 116, 154, 192, 230, 268, 297: `withItem` lambda issues - needs UnifiedBuilderFactory conversion

## Fix Pattern for Remaining Errors

All remaining errors follow one of two patterns:

### Pattern 1: withItem Lambda (Most Common)
```java
// ❌ Wrong
render.slot(0)
    .withItem(itemStack -> {
        itemStack.setType(Material.DIAMOND);
        itemStack.displayName(component);
    });

// ✅ Correct
render.slot(0)
    .withItem(
        UnifiedBuilderFactory.item(Material.DIAMOND)
            .setName(component)
            .build()
    );
```

### Pattern 2: layoutSlot with Index and Lambda
```java
// ❌ Wrong
render.layoutSlot('T', 0)
    .withItem(itemStack -> {
        itemStack.setType(Material.PLAYER_HEAD);
        itemStack.displayName(component);
    });

// ✅ Correct
render.layoutSlot('T', 0)
    .withItem(
        UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
            .setName(component)
            .build()
    );
```

## Next Steps

1. Apply the same UnifiedBuilderFactory pattern to:
   - MachineStorageView (4 locations)
   - MachineTrustView (6 locations)
   - MachineUpgradeView (7 locations)

2. Fix remaining `layoutSlot(char, int)` calls in:
   - MachineMainView (3 locations)
   - MachineRecipeView (1 location)

3. Investigate and fix "Symbol not found" errors in MachineRecipeView (lines 228, 235, 276, 287)

## Estimated Time to Complete
- Approximately 30-45 minutes to fix all remaining view files
- All fixes follow established patterns from completed work
