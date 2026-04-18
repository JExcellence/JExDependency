# Machine Fabrication System - Administrator Guide

## Overview

The Machine Fabrication System provides an advanced automation framework for RDQ that allows players to construct and operate automated crafting machines. This guide covers installation, configuration, and administration of the system.

## Table of Contents

1. [Installation](#installation)
2. [Configuration](#configuration)
3. [Permissions](#permissions)
4. [Commands](#commands)
5. [Database Management](#database-management)
6. [Troubleshooting](#troubleshooting)
7. [Performance Tuning](#performance-tuning)

---

## Installation

### Prerequisites

- RDQ plugin installed
- MySQL/MariaDB database configured
- JEHibernate ORM enabled
- R18n translation system initialized

### Setup Steps

1. **Database Migration**
   - The machine system automatically creates required tables on first startup
   - Tables created: `rdq_machines`, `rdq_machine_storage`, `rdq_machine_upgrades`, `rdq_machine_trust`
   - See `database/MACHINE_MIGRATION_README.md` for manual migration if needed

2. **Configuration Files**
   - Default configurations are created in `plugins/RDQ/machines/`
   - `machines.yml` - System-wide settings
   - `fabricator.yml` - Fabricator machine configuration

3. **Verify Installation**
   ```
   /rq machine reload
   ```
   Check console for any errors during configuration loading.

---

## Configuration

### System Configuration (`machines.yml`)

```yaml
machine-system:
  # Enable/disable the entire machine system
  enabled: true
  
  cache:
    # Enable in-memory caching for active machines
    enabled: true
    # Auto-save interval in seconds (300 = 5 minutes)
    auto-save-interval: 300
    # Maximum machines per player (0 = unlimited)
    max-machines-per-player: 10
  
  permissions:
    # Require permission to create machines
    require-permission: true
    # Base permission node (rdq.machine.{type})
    base-permission: "rdq.machine"
  
  breaking:
    # Keep items in virtual storage when machine is broken
    drop-items: false
    # Drop machine item when broken
    drop-machine-item: true
    # Allow trusted players to break machines
    require-owner: false
```

#### Configuration Options Explained

**enabled**: Master switch for the entire machine system. Set to `false` to disable all machine functionality.

**cache.enabled**: Enables in-memory caching for machines that are currently loaded. Improves performance by reducing database queries.

**cache.auto-save-interval**: How often (in seconds) to automatically save dirty machine data to the database. Lower values = more frequent saves = less data loss on crash, but more database load.

**cache.max-machines-per-player**: Limits how many machines a single player can own. Set to `0` for unlimited.

**permissions.require-permission**: If `true`, players need `rdq.machine.{type}` permission to create machines of that type.

**breaking.drop-items**: If `true`, stored items drop when machine is broken. If `false`, items remain in virtual storage and are restored when machine is placed again.

**breaking.drop-machine-item**: If `true`, breaking a machine drops the machine item. If `false`, machine is destroyed permanently.

**breaking.require-owner**: If `true`, only the owner can break the machine. If `false`, trusted players can also break it.

### Fabricator Configuration (`fabricator.yml`)

```yaml
fabricator:
  enabled: true
  permission: "rdq.machine.fabricator"
  
  # Multi-block structure definition
  structure:
    core-block: DROPPER
    required-blocks:
      - type: HOPPER
        relative-positions:
          - {x: 0, y: -1, z: 0}  # Below core
          - {x: 1, y: 0, z: 0}   # East
          - {x: -1, y: 0, z: 0}  # West
      - type: CHEST
        relative-positions:
          - {x: 0, y: 0, z: 1}   # South (output)
  
  # Blueprint requirements (cost to create machine)
  blueprint:
    requirements:
      currency:
        type: "currency"
        currency-type: "vault"
        currency-amount: 10000.0
      items:
        type: "item"
        required-items:
          diamond:
            material: DIAMOND
            amount: 16
          redstone:
            material: REDSTONE_BLOCK
            amount: 4
  
  # Crafting configuration
  crafting:
    # Base cooldown between crafts (in ticks, 20 ticks = 1 second)
    base-cooldown-ticks: 100
    # Recipe grid size (3 = 3x3 crafting grid)
    recipe-grid-size: 3
    # Maximum stack size for output
    max-output-stack-size: 64
  
  # Fuel system
  fuel:
    enabled: true
    # Base fuel consumption per craft
    base-consumption: 10
    fuel-types:
      coal:
        material: COAL
        energy-value: 100
      coal-block:
        material: COAL_BLOCK
        energy-value: 900
      lava-bucket:
        material: LAVA_BUCKET
        energy-value: 2000
  
  # Upgrade system
  upgrades:
    speed:
      max-level: 5
      # 10% faster per level
      effect-per-level: 0.10
      requirements:
        level-1:
          type: "composite"
          operator: "AND"
          sub-requirements:
            currency:
              type: "currency"
              currency-type: "vault"
              currency-amount: 5000.0
            items:
              type: "item"
              required-items:
                redstone:
                  material: REDSTONE
                  amount: 32
    
    efficiency:
      max-level: 5
      # 15% chance to not consume fuel per level
      effect-per-level: 0.15
      requirements:
        level-1:
          type: "item"
          required-items:
            diamond:
              material: DIAMOND
              amount: 8
    
    bonus-output:
      max-level: 3
      # 10% chance for double output per level
      effect-per-level: 0.10
      requirements:
        level-1:
          type: "item"
          required-items:
            emerald:
              material: EMERALD
              amount: 16
    
    fuel-reduction:
      max-level: 5
      # 10% less fuel consumption per level
      effect-per-level: 0.10
      requirements:
        level-1:
          type: "item"
          required-items:
            gold-ingot:
              material: GOLD_INGOT
              amount: 16
```

#### Structure Configuration

The `structure` section defines the multi-block pattern required to build the machine:

- **core-block**: The main block that triggers structure detection
- **required-blocks**: List of blocks that must be present at specific positions relative to the core block
- **relative-positions**: Coordinates relative to core block (x, y, z)
  - x: East (+) / West (-)
  - y: Up (+) / Down (-)
  - z: South (+) / North (-)

#### Blueprint Requirements

Uses the existing RDQ requirement system. Supports:
- **currency**: Vault economy integration
- **item**: Required items consumed on creation
- **composite**: Combine multiple requirements with AND/OR operators

#### Crafting Configuration

- **base-cooldown-ticks**: Time between crafting operations (100 ticks = 5 seconds)
- **recipe-grid-size**: Currently only 3x3 supported (future: 6x6 with JExWorkbench)
- **max-output-stack-size**: Maximum items produced per craft

#### Fuel Types

Define which items can be used as fuel and their energy values:
- **material**: Minecraft material type
- **energy-value**: How much energy this fuel provides
- **base-consumption**: How much energy is consumed per craft

#### Upgrade System

Each upgrade type has:
- **max-level**: Maximum upgrade level
- **effect-per-level**: Percentage effect per level (0.10 = 10%)
- **requirements**: Cost for each level (can be different per level)

---

## Permissions

### Machine Type Permissions

```
rdq.machine.fabricator - Allow creating Fabricator machines
```

### Admin Permissions

```
rdq.admin.machine - Access to all machine admin commands
```

### Permission Examples

```yaml
# LuckPerms example
/lp group default permission set rdq.machine.fabricator true
/lp group admin permission set rdq.admin.machine true

# PermissionsEx example
/pex group default add rdq.machine.fabricator
/pex group admin add rdq.admin.machine
```

---

## Commands

### Player Commands

All player commands are accessed through the machine GUI system. No direct commands for players.

### Admin Commands

```
/rq machine give <player> <type>
```
Give a machine item to a player.
- **player**: Target player name
- **type**: Machine type (e.g., "fabricator")
- **Permission**: `rdq.admin.machine`

```
/rq machine list <player>
```
List all machines owned by a player.
- **player**: Target player name
- **Permission**: `rdq.admin.machine`

```
/rq machine remove <machine_id>
```
Delete a machine by ID.
- **machine_id**: Machine database ID (from list command)
- **Permission**: `rdq.admin.machine`

```
/rq machine reload
```
Reload machine configurations from disk.
- **Permission**: `rdq.admin.machine`
- **Note**: Does not affect active machines until they are reloaded

```
/rq machine info <machine_id>
```
Display detailed information about a machine.
- **machine_id**: Machine database ID
- **Permission**: `rdq.admin.machine`

```
/rq machine teleport <machine_id>
```
Teleport to a machine's location.
- **machine_id**: Machine database ID
- **Permission**: `rdq.admin.machine`

---

## Database Management

### Tables

**rdq_machines**
- Stores machine instances
- Columns: id, owner_uuid, machine_type, world, x, y, z, state, fuel_level, recipe_data, created_at, updated_at

**rdq_machine_storage**
- Stores virtual storage items
- Columns: id, machine_id, item_data, quantity, storage_type, updated_at

**rdq_machine_upgrades**
- Stores applied upgrades
- Columns: id, machine_id, upgrade_type, level, applied_at

**rdq_machine_trust**
- Stores trusted players
- Columns: id, machine_id, trusted_uuid, granted_at

### Backup Recommendations

```sql
-- Backup all machine data
mysqldump -u username -p database_name rdq_machines rdq_machine_storage rdq_machine_upgrades rdq_machine_trust > machine_backup.sql

-- Restore from backup
mysql -u username -p database_name < machine_backup.sql
```

### Cleanup Queries

```sql
-- Find machines in unloaded worlds
SELECT * FROM rdq_machines WHERE world NOT IN ('world', 'world_nether', 'world_the_end');

-- Delete machines older than 90 days with no activity
DELETE FROM rdq_machines WHERE updated_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- Find machines with most storage items
SELECT m.id, m.owner_uuid, COUNT(s.id) as item_count
FROM rdq_machines m
LEFT JOIN rdq_machine_storage s ON m.id = s.machine_id
GROUP BY m.id
ORDER BY item_count DESC
LIMIT 10;
```

---

## Troubleshooting

### Machine Not Detecting Structure

**Symptoms**: Placing blocks doesn't create machine

**Solutions**:
1. Verify structure configuration in `fabricator.yml`
2. Check player has permission: `rdq.machine.fabricator`
3. Ensure all required blocks are placed correctly
4. Check console for validation errors
5. Verify player has blueprint requirements

### Machine Not Crafting

**Symptoms**: Machine is ON but not producing items

**Solutions**:
1. Check fuel level (must be > 0)
2. Verify recipe is set and valid
3. Ensure sufficient materials in storage
4. Check machine state (must be ACTIVE)
5. Look for errors in console during crafting cycle

### Performance Issues

**Symptoms**: Server lag when machines are active

**Solutions**:
1. Increase `auto-save-interval` to reduce database writes
2. Limit `max-machines-per-player`
3. Optimize crafting cooldown (increase `base-cooldown-ticks`)
4. Check database connection pool settings
5. Monitor active machine count with `/rq machine list`

### Data Loss After Crash

**Symptoms**: Machine data reverted after server crash

**Solutions**:
1. Decrease `auto-save-interval` for more frequent saves
2. Ensure database has proper write permissions
3. Check database logs for connection issues
4. Verify auto-save task is running (check console on startup)

---

## Performance Tuning

### Recommended Settings for Small Servers (< 50 players)

```yaml
cache:
  auto-save-interval: 300  # 5 minutes
  max-machines-per-player: 10

crafting:
  base-cooldown-ticks: 100  # 5 seconds
```

### Recommended Settings for Large Servers (> 100 players)

```yaml
cache:
  auto-save-interval: 600  # 10 minutes
  max-machines-per-player: 5

crafting:
  base-cooldown-ticks: 200  # 10 seconds
```

### Monitoring

Check machine system health:
```
/rq machine list <player>  # Check player machine count
```

Monitor database performance:
```sql
-- Check machine count
SELECT COUNT(*) FROM rdq_machines;

-- Check active machines
SELECT COUNT(*) FROM rdq_machines WHERE state = 'ACTIVE';

-- Check storage item count
SELECT COUNT(*) FROM rdq_machine_storage;
```

### Optimization Tips

1. **Limit Active Machines**: Use `max-machines-per-player` to prevent abuse
2. **Increase Cooldowns**: Higher `base-cooldown-ticks` reduces server load
3. **Adjust Auto-Save**: Balance between data safety and performance
4. **Database Indexing**: Ensure indexes exist on frequently queried columns
5. **Chunk Loading**: Machines only operate in loaded chunks

---

## Support

For additional support:
- Check console logs for errors
- Review configuration files for syntax errors
- Consult the player guide for usage instructions
- Contact plugin developers for bug reports

---

**Version**: 1.0.0  
**Last Updated**: 2026-04-12
