# Machine Fabrication System - Configuration Reference

## Overview

This document provides a complete reference for all configuration options in the Machine Fabrication System.

## Table of Contents

1. [System Configuration](#system-configuration)
2. [Machine Type Configuration](#machine-type-configuration)
3. [Structure Definitions](#structure-definitions)
4. [Blueprint Requirements](#blueprint-requirements)
5. [Fuel Configuration](#fuel-configuration)
6. [Upgrade Configuration](#upgrade-configuration)
7. [Configuration Examples](#configuration-examples)

---

## System Configuration

**File**: `plugins/RDQ/machines/machines.yml`

### Complete Configuration

```yaml
machine-system:
  # Master switch for the entire machine system
  # Type: boolean
  # Default: true
  enabled: true
  
  cache:
    # Enable in-memory caching for active machines
    # Type: boolean
    # Default: true
    # Impact: Improves performance, reduces database queries
    enabled: true
    
    # Auto-save interval in seconds
    # Type: integer
    # Default: 300 (5 minutes)
    # Range: 60-3600
    # Impact: Lower = more frequent saves, higher database load
    auto-save-interval: 300
    
    # Maximum machines per player
    # Type: integer
    # Default: 10
    # Range: 0-unlimited (0 = no limit)
    # Impact: Prevents abuse, controls server load
    max-machines-per-player: 10
  
  permissions:
    # Require permission to create machines
    # Type: boolean
    # Default: true
    # Permission format: rdq.machine.{type}
    require-permission: true
    
    # Base permission node
    # Type: string
    # Default: "rdq.machine"
    # Usage: {base-permission}.{machine-type}
    base-permission: "rdq.machine"
  
  breaking:
    # Drop items when machine is broken
    # Type: boolean
    # Default: false
    # true = items drop on ground
    # false = items stay in virtual storage
    drop-items: false
    
    # Drop machine item when broken
    # Type: boolean
    # Default: true
    # true = machine item drops
    # false = machine is destroyed
    drop-machine-item: true
    
    # Require owner to break machine
    # Type: boolean
    # Default: false
    # true = only owner can break
    # false = trusted players can break
    require-owner: false
```

### Configuration Option Details

#### enabled
- **Type**: Boolean
- **Default**: `true`
- **Description**: Master switch for the entire machine system
- **Impact**: When `false`, all machine functionality is disabled
- **Use Case**: Temporarily disable machines for maintenance

#### cache.enabled
- **Type**: Boolean
- **Default**: `true`
- **Description**: Enable in-memory caching for loaded machines
- **Impact**: Significantly improves performance by reducing database queries
- **Recommendation**: Keep enabled unless debugging database issues

#### cache.auto-save-interval
- **Type**: Integer (seconds)
- **Default**: `300` (5 minutes)
- **Range**: `60` - `3600`
- **Description**: How often to save dirty machine data to database
- **Impact**: 
  - Lower values = less data loss on crash, higher database load
  - Higher values = more data loss on crash, lower database load
- **Recommendations**:
  - Small servers: `300` (5 minutes)
  - Large servers: `600` (10 minutes)
  - High-traffic servers: `900` (15 minutes)

#### cache.max-machines-per-player
- **Type**: Integer
- **Default**: `10`
- **Range**: `0` - unlimited
- **Description**: Maximum machines a single player can own
- **Impact**: Prevents abuse and controls server load
- **Special**: `0` = unlimited machines
- **Recommendations**:
  - Survival servers: `5-10`
  - Creative servers: `20-50`
  - Skyblock servers: `3-5`

#### permissions.require-permission
- **Type**: Boolean
- **Default**: `true`
- **Description**: Require permission to create machines
- **Permission Format**: `{base-permission}.{machine-type}`
- **Example**: `rdq.machine.fabricator`
- **Use Case**: Set to `false` to allow all players to create machines

#### permissions.base-permission
- **Type**: String
- **Default**: `"rdq.machine"`
- **Description**: Base permission node for machine creation
- **Usage**: Combined with machine type to form full permission
- **Example**: `rdq.machine` + `fabricator` = `rdq.machine.fabricator`

#### breaking.drop-items
- **Type**: Boolean
- **Default**: `false`
- **Description**: Drop stored items when machine is broken
- **Options**:
  - `true`: Items drop on ground (can be lost)
  - `false`: Items stay in virtual storage (restored on placement)
- **Recommendation**: `false` for better player experience

#### breaking.drop-machine-item
- **Type**: Boolean
- **Default**: `true`
- **Description**: Drop machine item when broken
- **Options**:
  - `true`: Machine item drops (can be moved)
  - `false`: Machine is destroyed permanently
- **Recommendation**: `true` to allow machine relocation

#### breaking.require-owner
- **Type**: Boolean
- **Default**: `false`
- **Description**: Require owner to break machine
- **Options**:
  - `true`: Only owner can break
  - `false`: Trusted players can also break
- **Use Case**: Set to `true` for stricter security

---

## Machine Type Configuration

**File**: `plugins/RDQ/machines/fabricator.yml`

### Complete Fabricator Configuration

```yaml
fabricator:
  # Enable this machine type
  # Type: boolean
  # Default: true
  enabled: true
  
  # Permission required to create this machine
  # Type: string
  # Default: "rdq.machine.fabricator"
  permission: "rdq.machine.fabricator"
  
  # Multi-block structure definition
  structure:
    # Core block type (triggers structure detection)
    # Type: Minecraft Material
    # Default: DROPPER
    core-block: DROPPER
    
    # Required blocks and their positions
    # Type: list of block definitions
    required-blocks:
      - type: HOPPER
        relative-positions:
          - {x: 0, y: -1, z: 0}  # Below core
          - {x: 1, y: 0, z: 0}   # East
          - {x: -1, y: 0, z: 0}  # West
      - type: CHEST
        relative-positions:
          - {x: 0, y: 0, z: 1}   # South
  
  # Blueprint requirements (cost to create)
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
    # Base cooldown between crafts (ticks)
    # Type: integer
    # Default: 100 (5 seconds)
    # Range: 20-1200 (1-60 seconds)
    base-cooldown-ticks: 100
    
    # Recipe grid size
    # Type: integer
    # Default: 3 (3x3 grid)
    # Options: 3 (currently only 3x3 supported)
    recipe-grid-size: 3
    
    # Maximum output stack size
    # Type: integer
    # Default: 64
    # Range: 1-64
    max-output-stack-size: 64
  
  # Fuel system
  fuel:
    # Enable fuel requirement
    # Type: boolean
    # Default: true
    enabled: true
    
    # Base fuel consumption per craft
    # Type: integer
    # Default: 10
    # Range: 1-1000
    base-consumption: 10
    
    # Fuel types and their energy values
    # Type: map of fuel definitions
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
      # Maximum upgrade level
      # Type: integer
      # Default: 5
      # Range: 1-10
      max-level: 5
      
      # Effect per level (percentage)
      # Type: decimal
      # Default: 0.10 (10%)
      # Range: 0.01-1.00
      effect-per-level: 0.10
      
      # Requirements for each level
      # Type: requirement definition
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
        level-2:
          type: "composite"
          operator: "AND"
          sub-requirements:
            currency:
              type: "currency"
              currency-type: "vault"
              currency-amount: 10000.0
            items:
              type: "item"
              required-items:
                redstone:
                  material: REDSTONE
                  amount: 64
    
    efficiency:
      max-level: 5
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
      effect-per-level: 0.10
      requirements:
        level-1:
          type: "item"
          required-items:
            gold-ingot:
              material: GOLD_INGOT
              amount: 16
```

---

## Structure Definitions

### Structure Format

```yaml
structure:
  core-block: <MATERIAL>
  required-blocks:
    - type: <MATERIAL>
      relative-positions:
        - {x: <int>, y: <int>, z: <int>}
```

### Coordinate System

- **x**: East (+) / West (-)
- **y**: Up (+) / Down (-)
- **z**: South (+) / North (-)

### Example Structures

#### Compact Structure (Minimal)
```yaml
structure:
  core-block: DROPPER
  required-blocks:
    - type: HOPPER
      relative-positions:
        - {x: 0, y: -1, z: 0}  # Below
```

#### Standard Structure (Default)
```yaml
structure:
  core-block: DROPPER
  required-blocks:
    - type: HOPPER
      relative-positions:
        - {x: 0, y: -1, z: 0}   # Below
        - {x: 1, y: 0, z: 0}    # East
        - {x: -1, y: 0, z: 0}   # West
    - type: CHEST
      relative-positions:
        - {x: 0, y: 0, z: 1}    # South
```

#### Large Structure (Complex)
```yaml
structure:
  core-block: BEACON
  required-blocks:
    - type: IRON_BLOCK
      relative-positions:
        - {x: 0, y: -1, z: 0}   # Below
        - {x: 1, y: -1, z: 0}   # Below East
        - {x: -1, y: -1, z: 0}  # Below West
        - {x: 0, y: -1, z: 1}   # Below South
        - {x: 0, y: -1, z: -1}  # Below North
    - type: HOPPER
      relative-positions:
        - {x: 1, y: 0, z: 0}    # East
        - {x: -1, y: 0, z: 0}   # West
        - {x: 0, y: 0, z: 1}    # South
        - {x: 0, y: 0, z: -1}   # North
```

---

## Blueprint Requirements

### Requirement Types

#### Currency Requirement
```yaml
currency:
  type: "currency"
  currency-type: "vault"  # or "playerpoints", "tokens", etc.
  currency-amount: 10000.0
```

#### Item Requirement
```yaml
items:
  type: "item"
  required-items:
    diamond:
      material: DIAMOND
      amount: 16
    redstone:
      material: REDSTONE_BLOCK
      amount: 4
```

#### Composite Requirement (AND)
```yaml
composite:
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
        diamond:
          material: DIAMOND
          amount: 8
```

#### Composite Requirement (OR)
```yaml
composite:
  type: "composite"
  operator: "OR"
  sub-requirements:
    option1:
      type: "currency"
      currency-type: "vault"
      currency-amount: 10000.0
    option2:
      type: "item"
      required-items:
        diamond:
          material: DIAMOND
          amount: 32
```

---

## Fuel Configuration

### Fuel Type Format

```yaml
fuel-types:
  <fuel-id>:
    material: <MATERIAL>
    energy-value: <integer>
```

### Common Fuel Types

```yaml
fuel-types:
  # Basic fuels
  coal:
    material: COAL
    energy-value: 100
  charcoal:
    material: CHARCOAL
    energy-value: 100
  
  # Block fuels
  coal-block:
    material: COAL_BLOCK
    energy-value: 900
  
  # Liquid fuels
  lava-bucket:
    material: LAVA_BUCKET
    energy-value: 2000
  
  # Wood fuels
  oak-log:
    material: OAK_LOG
    energy-value: 50
  oak-planks:
    material: OAK_PLANKS
    energy-value: 15
  stick:
    material: STICK
    energy-value: 5
  
  # Blaze fuels
  blaze-rod:
    material: BLAZE_ROD
    energy-value: 120
  blaze-powder:
    material: BLAZE_POWDER
    energy-value: 60
```

### Fuel Calculation

```
Crafts per fuel = energy-value / base-consumption
```

**Example**:
- Coal: 100 energy / 10 consumption = 10 crafts
- Coal Block: 900 energy / 10 consumption = 90 crafts
- Lava Bucket: 2000 energy / 10 consumption = 200 crafts

---

## Upgrade Configuration

### Upgrade Type Format

```yaml
upgrades:
  <upgrade-type>:
    max-level: <integer>
    effect-per-level: <decimal>
    requirements:
      level-<n>:
        <requirement-definition>
```

### Upgrade Types

#### Speed Upgrade
```yaml
speed:
  max-level: 5
  effect-per-level: 0.10  # 10% faster per level
  requirements:
    level-1:
      type: "item"
      required-items:
        redstone:
          material: REDSTONE
          amount: 32
```

**Effect**: Reduces crafting cooldown
- Level 1: 10% faster (90 ticks instead of 100)
- Level 5: 50% faster (50 ticks instead of 100)

#### Efficiency Upgrade
```yaml
efficiency:
  max-level: 5
  effect-per-level: 0.15  # 15% chance per level
  requirements:
    level-1:
      type: "item"
      required-items:
        diamond:
          material: DIAMOND
          amount: 8
```

**Effect**: Chance to not consume fuel
- Level 1: 15% chance to save fuel
- Level 5: 75% chance to save fuel

#### Bonus Output Upgrade
```yaml
bonus-output:
  max-level: 3
  effect-per-level: 0.10  # 10% chance per level
  requirements:
    level-1:
      type: "item"
      required-items:
        emerald:
          material: EMERALD
          amount: 16
```

**Effect**: Chance to produce double output
- Level 1: 10% chance for 2x output
- Level 3: 30% chance for 2x output

#### Fuel Reduction Upgrade
```yaml
fuel-reduction:
  max-level: 5
  effect-per-level: 0.10  # 10% reduction per level
  requirements:
    level-1:
      type: "item"
      required-items:
        gold-ingot:
          material: GOLD_INGOT
          amount: 16
```

**Effect**: Reduces fuel consumption
- Level 1: 10% less fuel (9 instead of 10)
- Level 5: 50% less fuel (5 instead of 10)

---

## Configuration Examples

### Example 1: Cheap Starter Machine

```yaml
fabricator:
  enabled: true
  permission: "rdq.machine.fabricator"
  
  blueprint:
    requirements:
      items:
        type: "item"
        required-items:
          iron:
            material: IRON_INGOT
            amount: 8
          redstone:
            material: REDSTONE
            amount: 16
  
  crafting:
    base-cooldown-ticks: 200  # Slower (10 seconds)
  
  fuel:
    enabled: true
    base-consumption: 20  # Higher consumption
    fuel-types:
      coal:
        material: COAL
        energy-value: 100
  
  upgrades:
    speed:
      max-level: 3  # Limited upgrades
      effect-per-level: 0.10
```

### Example 2: Expensive Advanced Machine

```yaml
fabricator:
  enabled: true
  permission: "rdq.machine.fabricator"
  
  blueprint:
    requirements:
      composite:
        type: "composite"
        operator: "AND"
        sub-requirements:
          currency:
            type: "currency"
            currency-type: "vault"
            currency-amount: 50000.0
          items:
            type: "item"
            required-items:
              diamond:
                material: DIAMOND
                amount: 32
              netherite:
                material: NETHERITE_INGOT
                amount: 4
  
  crafting:
    base-cooldown-ticks: 50  # Fast (2.5 seconds)
  
  fuel:
    enabled: true
    base-consumption: 5  # Low consumption
    fuel-types:
      lava-bucket:
        material: LAVA_BUCKET
        energy-value: 5000
  
  upgrades:
    speed:
      max-level: 10  # More upgrades
      effect-per-level: 0.05
```

### Example 3: Free Creative Machine

```yaml
fabricator:
  enabled: true
  permission: "rdq.machine.fabricator"
  
  blueprint:
    requirements: {}  # No requirements
  
  crafting:
    base-cooldown-ticks: 20  # Very fast (1 second)
  
  fuel:
    enabled: false  # No fuel required
  
  upgrades:
    speed:
      max-level: 1
      effect-per-level: 0.50
      requirements:
        level-1: {}  # Free upgrade
```

---

**Version**: 1.0.0  
**Last Updated**: 2026-04-12
