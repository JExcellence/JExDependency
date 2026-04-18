# Machine Service Auto-Load Fix

## Issue

Machine functionality (toggle, upgrades, trust, recipe, storage) was not working because machines were not being loaded into the active registry when accessed through the GUI.

## Root Cause

The `MachineService` methods all check `manager.getActiveMachine(machineId)` which returns empty if the machine isn't in the active registry. When a player opens a machine GUI, the machine entity exists in the database but hasn't been loaded into the active registry yet, causing all operations to fail silently.

## Solution

Added an `ensureMachineLoaded()` helper method that automatically loads machines from cache or database if they're not in the active registry. This method:

1. Checks if machine is already in active registry (fast)
2. If not, tries to load from cache (fast)
3. If not in cache, loads from database (slower but necessary)
4. Registers the machine in the active registry
5. Returns the loaded machine

## Implementation

### Helper Method Added

```java
private Optional<Machine> ensureMachineLoaded(@NotNull final Long machineId) {
    // Check if already in active registry
    Optional<Machine> machineOpt = manager.getActiveMachine(machineId);
    if (machineOpt.isPresent()) {
        return machineOpt;
    }

    // Try cache
    Machine machine = cache.getCachedByKey().get(machineId);
    if (machine != null) {
        manager.registerMachine(machine);
        return Optional.of(machine);
    }

    // Try database
    machineOpt = repository.findByIdWithRelationships(machineId);
    if (machineOpt.isPresent()) {
        manager.registerMachine(machineOpt.get());
        cache.getCachedByKey().put(machineId, machineOpt.get());
    }

    return machineOpt;
}
```

### Methods Updated

All service methods now use `ensureMachineLoaded()` instead of `manager.getActiveMachine()`:

- `toggleMachine()` - Machine state toggling
- `setRecipe()` - Recipe configuration
- `addFuel()` - Fuel management
- `depositItems()` - Storage deposits
- `withdrawItems()` - Storage withdrawals
- `getStorageContents()` - Storage queries
- `addTrustedPlayer()` - Trust management
- `removeTrustedPlayer()` - Trust management
- `getTrustedPlayers()` - Trust queries
- `applyUpgrade()` - Upgrade application
- `getUpgrades()` - Upgrade queries

## Benefits

1. **Automatic Loading**: Machines are loaded on-demand when accessed
2. **Performance**: Uses fast cache lookup before database
3. **Reliability**: Ensures machines are always available when needed
4. **Transparency**: No changes needed to calling code
5. **Consistency**: All operations use the same loading logic

## Testing

Test the following scenarios:

1. **Toggle Machine**: Click the ON/OFF button in main view
   - Should toggle machine state
   - Should update button appearance

2. **Apply Upgrades**: Click upgrade buttons in upgrade view
   - Should increment upgrade level
   - Should update display

3. **Manage Trust**: Add/remove players in trust view
   - Should add players to trust list
   - Should remove players when clicked

4. **Configure Recipe**: Set recipe in recipe view
   - Should lock recipe configuration
   - Should display recipe items

5. **Storage Operations**: Deposit/withdraw in storage view
   - Should add items to storage
   - Should remove items from storage

## Files Modified

- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineService.java`
  - Added `ensureMachineLoaded()` helper method
  - Updated all operation methods to use the helper

## Status

✅ Auto-load mechanism implemented
✅ All service methods updated
✅ Compilation successful
✅ Ready for testing

## Next Steps

1. Test all machine operations in-game
2. Verify machines load correctly from database
3. Check performance with multiple machines
4. Monitor for any lazy-loading issues
