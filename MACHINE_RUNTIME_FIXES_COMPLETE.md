# Machine System Runtime Fixes - Complete

## Summary

Fixed critical runtime errors in the machine GUI system and implemented missing machine commands with proper API usage.

## Issues Fixed

### 1. GUI State Initialization Error

**Problem:**
- NullPointerException when clicking buttons in MachineMainView
- State values (`machineService` and `machine`) were not being passed correctly when opening the view
- MachineInteractListener was passing `"machineManager"` instead of `"machineService"`

**Solution:**
- Fixed `MachineInteractListener.openMachineGUI()` to pass `"machineService"` instead of `"machineManager"`
- This ensures the view's state values are properly initialized when the GUI opens

**Files Modified:**
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/MachineInteractListener.java`

### 2. Missing Machine Commands Implementation

**Problem:**
- Commands `/rq machine list`, `/rq machine teleport`, and `/rq machine reload` showed "not implemented" message
- Players and admins couldn't manage machines via commands

**Solution:**
Implemented the following commands:

#### `/rq machine list [player]`
- Lists all machines owned by a player (defaults to self)
- Shows machine ID, type, location, and state
- Uses `MachineRepository.findByOwnerAsync()` for async database queries
- Requires permission: `rdq.admin.machine.list`

#### `/rq machine teleport <machine_id>`
- Teleports player to a specific machine
- Validates machine ownership/trust permissions via `IMachineService.canInteract()`
- Checks if world exists before teleporting
- Uses proper Machine entity getters: `getWorld()`, `getX()`, `getY()`, `getZ()`
- Requires permission: `rdq.admin.machine.teleport`

#### `/rq machine reload`
- Reloads machine configurations from disk
- Provides success/error feedback
- Requires permission: `rdq.admin.machine.reload`

**Files Modified:**
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/PRQ.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineManager.java`

### 3. API Usage Corrections

**Problem:**
- Initial implementation used incorrect method names that don't exist in the codebase
- `findByAttributesAsync()` doesn't exist in MachineRepository
- Machine entity doesn't have `getWorldName()`, `getCoreX()`, `getCoreY()`, `getCoreZ()` methods

**Solution:**
- Used correct `MachineRepository.findByOwnerAsync(UUID)` method
- Used correct Machine entity getters: `getWorld()`, `getX()`, `getY()`, `getZ()`
- All methods now use the actual API from the codebase

### 4. Missing Translation Keys

**Problem:**
- New commands had no translation keys defined

**Solution:**
Added comprehensive translation keys for all command messages:
- List command: empty, header, entry
- Teleport command: syntax, invalid_id, not_found, no_permission, world_not_found, success
- Reload command: success, error

**Files Modified:**
- `RDQ/rdq-common/src/main/resources/translations/en_US.yml`

## Translation Keys Added

```yaml
machine:
  command:
    list:
      empty: "<gradient:#6b7280:#9ca3af>%player% has no machines.</gradient>"
      header: "<gradient:#FF6B00:#FFD700>Machines owned by %player%</gradient> <gradient:#6b7280:#9ca3af>(%count% total)</gradient>"
      entry: "  <gradient:#3b82f6:#60a5fa>#%id%</gradient> <gradient:#f59e0b:#fbbf24>%type%</gradient> <gradient:#6b7280:#9ca3af>at %location%</gradient> <gradient:#10b981:#34d399>[%state%]</gradient>"
    teleport:
      syntax: "<red>Usage: /rq machine teleport <machine_id></red>"
      invalid_id: "<red>Invalid machine ID: %id%</red>"
      not_found: "<red>Machine #%id% not found</red>"
      no_permission: "<red>You don't have permission to teleport to this machine</red>"
      world_not_found: "<red>Machine world '%world%' not found</red>"
      success: "<green>✓ Teleported to machine #%id% (%type%)</green>"
    reload:
      success: "<green>✓ Machine configurations reloaded successfully</green>"
      error: "<red>Failed to reload machine configurations: %error%</red>"
```

## Commands Still Not Implemented

The following commands remain unimplemented (marked as "not implemented"):
- `/rq machine remove <machine_id>` - Remove a machine by ID
- `/rq machine info <machine_id>` - Display detailed machine information

These can be implemented in a future update if needed.

## Testing Recommendations

1. **GUI Testing:**
   - Right-click a machine to open the GUI
   - Click all buttons (State Toggle, Recipe, Storage, Trust, Upgrades)
   - Verify no NullPointerExceptions occur

2. **Command Testing:**
   - `/rq machine list` - List your own machines
   - `/rq machine list <player>` - List another player's machines
   - `/rq machine teleport <id>` - Teleport to a machine
   - `/rq machine reload` - Reload configurations

3. **Permission Testing:**
   - Test commands without permissions (should show permission error)
   - Test commands with permissions (should work)

## Permissions

All machine commands use the following permission structure:
- `rdq.admin.machine` - Base machine command access
- `rdq.admin.machine.give` - Give machine items
- `rdq.admin.machine.list` - List machines
- `rdq.admin.machine.teleport` - Teleport to machines
- `rdq.admin.machine.reload` - Reload configurations
- `rdq.admin.machine.remove` - Remove machines (not yet implemented)
- `rdq.admin.machine.info` - View machine info (not yet implemented)

## Files Changed

1. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/MachineInteractListener.java`
   - Fixed state initialization by passing correct key name

2. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/PRQ.java`
   - Implemented LIST command using `findByOwnerAsync()`
   - Implemented TELEPORT command with proper Machine entity getters
   - Implemented RELOAD command
   - Fixed all API usage to match actual codebase

3. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/MachineManager.java`
   - Added `reloadConfigurations()` method

4. `RDQ/rdq-common/src/main/resources/translations/en_US.yml`
   - Added translation keys for all new commands

## API Usage Reference

### MachineRepository Methods Used:
- `findByOwnerAsync(UUID ownerUuid)` - Returns `CompletableFuture<List<Machine>>`
- `findByIdAsync(Long id)` - Returns `CompletableFuture<Optional<Machine>>`

### Machine Entity Getters Used:
- `getWorld()` - Returns `String` (world name)
- `getX()` - Returns `int` (X coordinate)
- `getY()` - Returns `int` (Y coordinate)
- `getZ()` - Returns `int` (Z coordinate)
- `getId()` - Returns `Long` (machine ID)
- `getMachineType()` - Returns `EMachineType`
- `getState()` - Returns `EMachineState`

### IMachineService Methods Used:
- `canInteract(Player player, Machine machine)` - Returns `boolean`

## Status

✅ GUI state initialization fixed
✅ Machine list command implemented
✅ Machine teleport command implemented
✅ Machine reload command implemented
✅ Translation keys added
✅ All API usage corrected
✅ All files compile without errors
⏳ Machine remove command (future)
⏳ Machine info command (future)
