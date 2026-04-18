# Machine View Runtime Fixes

## Issues Fixed

### 1. NullPointerException on Toggle Button Click

**Problem:**
```
java.lang.NullPointerException: Cannot invoke "com.raindropcentral.rdq.machine.structure.StructureValidator.validate(...)" 
because the return value of "...State.get(...)" is null
```

**Root Cause:**
- The `validator` and `plugin` states were null when the view was opened from a previous session
- States were not being passed correctly or had no default values

**Solution:**
- Added null checks before using `validator` and `structure` states
- Added fallback logic for `plugin` state
- Messages now sent directly if plugin is null (not ideal but prevents crash)
- Extracted toggle result handling to separate method for cleaner code

**Code Changes:**
```java
// Before
validator.get(click).validate(...)  // NPE if null

// After
if (validator.get(click) != null && structure.get(click) != null) {
    validator.get(click).validate(...)
}
```

### 2. Missing Translation Keys

**Problem:**
All `view.machine.main` translation keys were missing, causing blank text in GUI.

**Solution:**
Added complete translation section to `en_US.yml`:

```yaml
view:
  machine:
    main:
      title: "⚙ Machine Control"
      items:
        state:
          on: # Active state
          off: # Inactive state
        type: # Machine type display
        fuel: # Fuel level display
        recipe:
          set: # Recipe configured
          not-set: # No recipe
        navigation:
          storage: # Storage button
          trust: # Trust button
          upgrades: # Upgrades button
```

## Remaining Issues to Fix

### 3. Recipe Clearing Not Returning Items

**Problem:**
When clearing a recipe in MachineRecipeView, items are not returned to player inventory properly.

**Status:** Not yet fixed - needs investigation in MachineRecipeView

### 4. Recipe Output Preview Missing

**Problem:**
No visual preview of what the recipe will craft (should show at slot 24).

**Status:** Not yet implemented - needs to be added to MachineRecipeView

## Files Modified

1. **MachineMainView.java**
   - Added null checks for validator and structure states
   - Added fallback logic for plugin state
   - Extracted `handleToggleResult()` method
   - Added SlotClickContext import

2. **en_US.yml**
   - Added complete `view.machine.main` section
   - All item names and lore
   - All navigation button text

## Testing Checklist

- [x] Toggle button doesn't crash with NPE
- [x] Translation keys display correctly
- [x] Toggle button shows proper messages
- [ ] Recipe clearing returns items correctly
- [ ] Recipe output preview shows at slot 24
- [ ] Structure validation works when states are provided
- [ ] Fuel validation works
- [ ] Recipe validation works

## Next Steps

1. **Fix Recipe Clearing in MachineRecipeView**
   - Investigate why items aren't returned
   - Ensure insertedItems map is properly cleared
   - Test with multiple items

2. **Add Recipe Output Preview**
   - Add slot 24 to MachineRecipeView layout
   - Calculate recipe output using Minecraft crafting system
   - Update preview dynamically when recipe changes
   - Use Context7 inventory-framework documentation for implementation

3. **Test Structure Validation**
   - Ensure validator and structure states are passed from MachineInteractListener
   - Test with broken structures
   - Verify error messages display correctly

## Related Documentation

- `MACHINE_MAIN_VIEW_TOGGLE_FIX.md` - Original toggle implementation
- `MACHINE_RECIPE_VIEW_FIX.md` - Recipe view item placement
- `MACHINE_LISTENER_VIEW_INTEGRATION_COMPLETE.md` - State passing integration
