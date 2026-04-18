# Machine Give Command Implementation - COMPLETE ✅

## Summary

Added the `/rq machine give` command to allow admins to give machine items to players.

## Command Usage

```
/rq machine give <player> <machine_type>
```

### Examples
```
/rq machine give SaltyFeaRz fabricator
/rq machine give @a fabricator
```

## Features

### 1. Give Machine Items
- Gives the specified machine item to a player
- Supports all machine types (currently: FABRICATOR)
- Handles full inventory (drops item at player location)
- Notifies both sender and recipient

### 2. Tab Completion
- First argument: Online player names
- Second argument: Machine types (fabricator, etc.)

### 3. Permission System
- Base permission: `rdq.admin.machine.give`
- Type-specific: `rdq.admin.machine.give.fabricator`
- Wildcard: `rdq.admin.machine.give.*`

### 4. Error Handling
- Player not found
- Invalid machine type
- No permission
- Inventory full (graceful fallback)

## Files Created

### Command Implementation
`RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/command/player/rq/machine/MachineGiveCommand.java`

**Key features:**
- Extends `Command` from JExCommand framework
- Uses R18nManager for translations
- Uses MachineItemFactory to create items
- Full tab completion support
- Permission checks

## Translations

Already exist in `en_US.yml`:

```yaml
command:
  machine:
    give:
      syntax: "<red>Usage: /rq machine give <player> <machine_type></red>"
      success: "<green>✓ Gave %player% a %machine_type% machine item</green>"
      no_permission: "<red>You don't have permission to give %machine_type% machines</red>"
      inventory_full: "<yellow>⚠ %player%'s inventory is full. Machine item dropped at their location.</yellow>"
      received: "<green>✓ You received a %machine_type% machine from %sender%</green>"
```

## Permissions

Already defined in `EMachinePermission.java`:

```java
GIVE("give", "rdq.admin.machine.give")
```

### Permission Hierarchy
```
rdq.admin.machine.give.*          # All machine types
├── rdq.admin.machine.give.fabricator
└── rdq.admin.machine.give.<type>
```

## How It Works

### 1. Command Registration
The `CommandFactory` automatically discovers and registers the command because:
- It's in the correct package: `com.raindropcentral.rdq.command.player.rq.machine`
- It extends `Command`
- It has the proper constructor signature

### 2. Command Execution Flow
```
Player types: /rq machine give SaltyFeaRz fabricator
    ↓
1. Parse arguments (player name, machine type)
    ↓
2. Validate player exists
    ↓
3. Validate machine type
    ↓
4. Check permissions
    ↓
5. Create machine item via MachineItemFactory
    ↓
6. Give to player (or drop if inventory full)
    ↓
7. Send success messages
```

### 3. Tab Completion
```
/rq machine give <TAB>
    → Shows online player names

/rq machine give SaltyFeaRz <TAB>
    → Shows: fabricator
```

## Testing Checklist

- [ ] Command appears in `/rq machine` help
- [ ] Tab completion works for player names
- [ ] Tab completion works for machine types
- [ ] Giving to online player works
- [ ] Giving to offline player shows error
- [ ] Invalid machine type shows error
- [ ] Permission check works
- [ ] Full inventory drops item at location
- [ ] Recipient receives notification
- [ ] Sender receives confirmation

## Usage Instructions

### For Admins

1. **Get a machine item:**
   ```
   /rq machine give <your_name> fabricator
   ```

2. **Place the machine:**
   - Right-click with the machine item
   - Machine will be constructed if structure is valid

3. **Interact with machine:**
   - Right-click the core block to open GUI
   - Configure recipe, manage storage, add upgrades, etc.

### For Players

Players can:
- Receive machines from admins
- Place machines (if they have permission)
- Use machines they own or are trusted on
- Cannot create machines without admin giving them the item

## Next Steps

### Additional Commands to Implement

1. **`/rq machine list [player]`**
   - List all machines owned by a player
   - Show location, type, status

2. **`/rq machine remove <x> <y> <z>`**
   - Remove a machine at coordinates
   - Return item to owner

3. **`/rq machine info`**
   - Show info about machine you're looking at
   - Owner, type, status, upgrades

4. **`/rq machine teleport <machine_id>`**
   - Teleport to a specific machine
   - Useful for admin management

5. **`/rq machine reload`**
   - Reload machine configurations
   - Refresh recipes and settings

## Integration

The command integrates with:
- **MachineItemFactory** - Creates machine items
- **R18nManager** - Translations
- **EMachinePermission** - Permission system
- **CommandFactory** - Auto-registration
- **MachineCommandSection** - Configuration

## Notes

- Command is automatically registered on plugin enable
- No manual registration needed
- Follows existing RDQ command patterns
- Uses same translation and permission systems
- Compatible with existing machine system
