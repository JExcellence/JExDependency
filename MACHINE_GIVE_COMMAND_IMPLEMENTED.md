# Machine Give Command Implementation

## Summary
Implemented the `/rq machine give` command to allow admins to give machine items to players.

## Changes Made

### 1. Updated PRQ Command Handler
**File:** `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/PRQ.java`

- Added machine subcommand handling
- Implemented `handleMachineSubcommand()` method
- Added support for `/rq machine give <player> <machine_type>`
- Added tab completion for machine subcommands
- Added tab completion for player names and machine types

### 2. Updated Translations
**File:** `RDQ/rdq-common/src/main/resources/translations/en_US.yml`

Added translation keys:
```yaml
machine:
  command:
    give:
      syntax: "<red>Usage: /rq machine give <player> <machine_type></red>"
      success: "<green>✓ Gave {player} a {machine} machine item</green>"
      invalid_type: "<red>Invalid machine type: {type}</red>"
      received: "<green>✓ You received a {machine} machine</green>"
    help: "..."

error:
  not_implemented: "<gradient:#ef4444:#f87171>❌ Not Implemented!</gradient> ..."
  player_not_found: "... {player} ..."
```

## Usage

### Give Command
```
/rq machine give <player> <machine_type>
```

**Examples:**
- `/rq machine give SaltyFeaRz fabricator` - Gives a fabricator to SaltyFeaRz
- `/rq machine give @p fabricator` - Gives a fabricator to nearest player

### Tab Completion
- `/rq machine <TAB>` - Shows: give, list, remove, reload, info, teleport, help
- `/rq machine give <TAB>` - Shows online player names
- `/rq machine give SaltyFeaRz <TAB>` - Shows: fabricator

### Permissions
- `rdq.machine` - Access to `/rq machine` command
- `rdq.machine.give` - Permission to use `/rq machine give`

## Features

### Implemented
✅ `/rq machine give <player> <machine_type>` - Give machine items
✅ Tab completion for subcommands
✅ Tab completion for player names
✅ Tab completion for machine types
✅ Permission checks
✅ Player validation
✅ Machine type validation
✅ Success/error messages
✅ Recipient notification

### Placeholder (Not Yet Implemented)
⏳ `/rq machine list [player]` - List machines
⏳ `/rq machine remove <id>` - Remove a machine
⏳ `/rq machine reload` - Reload configurations
⏳ `/rq machine info <id>` - View machine info
⏳ `/rq machine teleport <id>` - Teleport to machine

## Technical Details

### Command Flow
1. Player executes `/rq machine give <player> <type>`
2. PRQ command checks `EPRQPermission.MACHINE`
3. Calls `handleMachineSubcommand()` with args
4. Parses subcommand as `EMachineAction.GIVE`
5. Checks `EMachinePermission.GIVE`
6. Validates target player exists
7. Validates machine type is valid
8. Creates machine item via `MachineItemFactory`
9. Adds item to player inventory
10. Sends success messages to both players

### Error Handling
- Invalid player name → "Player not found" error
- Invalid machine type → "Invalid machine type" error
- No permission → Permission denied message
- Machine system disabled → "Machine system disabled" error

## Testing
To test the command:
1. Rebuild: `./gradlew clean :RDQ:rdq-premium:build`
2. Copy JAR to server plugins folder
3. Restart server
4. Run: `/rq machine give <player> fabricator`
5. Check player inventory for machine item

## Files Modified
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/PRQ.java`
- `RDQ/rdq-common/src/main/resources/translations/en_US.yml`

## Related Files
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/machine/EMachineAction.java` (enum)
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/machine/EMachinePermission.java` (permissions)
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/item/MachineItemFactory.java` (item creation)
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/type/EMachineType.java` (machine types)
