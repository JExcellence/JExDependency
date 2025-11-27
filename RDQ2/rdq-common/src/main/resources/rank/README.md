# Rank Definitions

Rank YAML files load during stage 3 when
[`RDQ#initializeRepositories()`](../../java/com/raindropcentral/rdq/RDQ.java) wires the
`RRankRepository`, `RRankTreeRepository`, and related progress repositories. Hydration occurs on the
stage-1 executor (virtual threads with fixed-pool fallback) before managers touch the data inside the
[`runSync`](../../java/com/raindropcentral/rdq/RDQ.java) stage-2 block.

Both editions consume these definitions through their respective managers, so coordinate changes with
the package documentation under `com.raindropcentral.rdq.config.ranks` and the lifecycle Javadocs for
`RDQFreeManager` and `RDQPremiumManager` to keep promotion behaviour aligned.

# MMORPG Rank System Example (42 Ranks, 6 Paths)

This example demonstrates a flexible rank system for RaindropQuests, suitable for MMORPG-style progression.
It features **6 distinct rank paths** (Cleric, Mage, Merchant, Ranger, Rogue, and Warrior); each with 7 ranks.

---

## **Rank Paths Overview**

- **Cleric Path:** Acolyte → Devotee → Priest → Healer → Bishop → Oracle → High Priest
- **Mage Path:** Apprentice → Scholar → Elementalist → Enchanter → Sorcerer → Wizard → Archmage
- **Merchant Path:** Trader → Vendor → Artificer → Alchemist → Master Crafter → Merchant Prince → Guild Master
- **Ranger Path:** Hunter → Archer → Tracker → Marksman → Pathfinder → Sniper → Forest Lord
- **Rogue Path:** Thief → Scout → Assassin → Infiltrator → Shadowblade → Spymaster → Shadowmaster
- **Warrior Path:** Recruit → Fighter → Guardian → Berserker → Paladin → Warlord → Champion

---

# Defining and Handling Requirements

## Overview

Requirements in RaindropQuests are conditions that must be met by a player to unlock ranks, complete quests, or access
special features.
All requirements are based on the `AbstractRequirement` contract, which ensures a consistent structure for checking
fulfillment, calculating progress, and consuming resources.

## Requirement Types

The following types are supported out-of-the-box (see `AbstractRequirement.Type`):

- **CURRENCY**: Based on in-game currency (e.g., Vault money).
- **ITEM**: Possession of specific items.
- **PLAYTIME**: Accumulated playtime.
- **ACHIEVEMENT**: Completion of a specific achievement.
- **PREVIOUS_LEVEL**: Reaching a previous upgrade, level, or rank.
- **COMPOSITE**: Combination of multiple sub-requirements.
- **CHOICE**: Player can choose among alternatives.
- **TIME_BASED**: Time constraints or cooldowns.
- **EXPERIENCE_LEVEL**: Player's experience level.

## Requirement Structure in YAML

Requirements are defined in rank, quest, or perk YAML files under a `Requirements` section.
Each requirement has a unique key (e.g., `itemRequirement`) and varying fields available.

**Examples:**

```yaml
itemRequirement:
  consumeOnComplete: true
  allowPartialProgress: false
  requiredItems:
    item1:
      type: "IRON_SWORD"
      amount: 1
      patchFlags: [ ]
    item2:
      type: "DIAMOND"
      amount: 1
      patchFlags: [ ]

special_challenge:
  type: "CUSTOM"
  displayOrder: 14
  descriptionKey: "requirement.warrior.special"
  consumeOnComplete: false
  customScript: "getKillCount('ZOMBIE') >= getCustomData('requiredKills', 50)"
  progressScript: "Math.min(1.0, getKillCount('ZOMBIE') / getCustomData('requiredKills', 50))"
  customData:
    mobType: "ZOMBIE"
    requiredKills: 50
    trackingEnabled: true

# Custom plugin integration
custom_plugin_check:
  type: "CUSTOM"
  customScript: |
    var plugin = Bukkit.getPluginManager().getPlugin('MyCustomPlugin');
    if (plugin != null) {
      return plugin.getAPI().hasCompletedQuest(player, 'epic_quest');
    }
    return false;
  customData:
    pluginName: "MyCustomPlugin"
    questName: "epic_quest"

# Database-driven requirement
database_check:
  type: "CUSTOM"
  customScript: |
    // This would require your database service to be available in script context
    var dbService = getCustomData('databaseService', null);
    if (dbService != null) {
      return dbService.hasPlayerAchievement(player.getUniqueId(), 'special_achievement');
    }
    return false;
  customData:
    achievementId: "special_achievement"
    tableName: "player_achievements"

# Custom script with progress calculation
advanced_challenge:
  type: "CUSTOM"
  customScript: "player.getKillCount('SKELETON') >= getCustomData('requiredKills', 25)"
  progressScript: "Math.min(1.0, player.getKillCount('SKELETON') / getCustomData('requiredKills', 25))"
  customData:
    requiredKills: 25
    mobType: "SKELETON"
    description: "Skeleton Hunter Challenge"

# Time-based custom requirement
daily_login:
  type: "CUSTOM"
  customScript: |
    var lastLogin = player.getMetadata('lastLogin');
    var today = new Date().toDateString();
    return lastLogin && lastLogin.equals(today);
  customData:
    type: "daily_check"
    resetTime: "00:00"

# Complex multi-condition requirement
master_challenge:
  type: "CUSTOM"
  customScript: |
    var kills = player.getKillCount('ZOMBIE');
    var level = player.getLevel();
    var hasPermission = player.hasPermission('warrior.master');
    return kills >= 100 && level >= 30 && hasPermission;
  progressScript: |
    var killProgress = Math.min(1.0, player.getKillCount('ZOMBIE') / 100.0);
    var levelProgress = Math.min(1.0, player.getLevel() / 30.0);
    var permProgress = player.hasPermission('warrior.master') ? 1.0 : 0.0;
    return (killProgress + levelProgress + permProgress) / 3.0;
  customData:
    requiredKills: 100
    requiredLevel: 30
    requiredPermission: "warrior.master"

# Single permission
basic_access:
  type: "PERMISSION"
  requiredPermissions:
    - "ranks.member"

# Any permission (useful for multiple rank paths)
rank_access:
  type: "PERMISSION"
  permissionMode: "ANY"
  requiredPermissions:
    - "ranks.premium"
    - "ranks.vip"
    - "ranks.admin"
  description: "Must have any premium rank"

# Minimum permissions (e.g., at least 2 out of 4 permissions)
advanced_access:
  type: "PERMISSION"
  permissionMode: "MINIMUM"
  minimumRequired: 2
  requiredPermissions:
    - "skills.combat"
    - "skills.mining"
    - "skills.building"
    - "skills.magic"
  description: "Must master at least 2 skill categories"

# Negated permissions (player should NOT have these)
restricted_access:
  type: "PERMISSION"
  checkNegated: true
  requiredPermissions:
    - "ranks.banned"
    - "restrictions.combat"
  description: "Must not be banned or restricted"

training_fee:
  type: "CURRENCY"
  requiredCurrencies:
    VAULT: 500.0
    TOKENS: 10.0

# With custom plugin (future extensibility)
custom_currency:
  type: "CURRENCY"
  currencyPlugin: "vault"
  requiredCurrencies:
    MONEY: 1000.0

# AND Logic (original behavior)
basic_training:
  type: "COMPOSITE"
  operator: "AND"
  requirements:
    - type: "CURRENCY"
      currency: "VAULT"
      amount: 500.0
    - type: "EXPERIENCE_LEVEL"
      level: 5

# OR Logic (choice between requirements)
alternative_training:
  type: "COMPOSITE"
  operator: "OR"
  requirements:
    - type: "CURRENCY"
      currency: "VAULT"
      amount: 1000.0
    - type: "PLAYTIME"
      playtimeHours: 10

# MINIMUM Logic (complete N out of M requirements)
specialization_training:
  type: "COMPOSITE"
  operator: "MINIMUM"
  minimumRequired: 2
  allowPartialProgress: true
  description: "Complete at least 2 of these training modules"
  requirements:
    - type: "CURRENCY"
      currency: "VAULT"
      amount: 500.0
    - type: "ITEM"
      requiredItem:
        material: "DIAMOND"
        amount: 5
    - type: "EXPERIENCE_LEVEL"
      level: 15
    - type: "PLAYTIME"
      playtimeHours: 5

# choice example
weapon_choice:
  type: "CHOICE"
  choices:
    - type: "ITEM"
      requiredItem:
        material: "IRON_SWORD"
        amount: 1
    - type: "ITEM"
      requiredItem:
        material: "IRON_AXE"
        amount: 1

# Advanced choice with multiple requirements
specialization_choice:
  type: "CHOICE"
  minimumChoicesRequired: 2
  allowPartialProgress: true
  choices:
    - type: "CURRENCY"
      currency: "VAULT"
      amount: 1000.0
    - type: "EXPERIENCE_LEVEL"
      level: 15
    - type: "ITEM"
      requiredItem:
        material: "DIAMOND"
        amount: 10
    - type: "PLAYTIME"
      playtimeHours: 5
```

---

## Reward Structure in YAML

Rewards are defined in your rank or quest YAML files under a `Rewards` section.
Each reward has a unique key (e.g., `reward1`) and the following fields:

| Field    | Description                                                     | Example Value                             |
|----------|-----------------------------------------------------------------|-------------------------------------------|
| `Type`   | The reward type (see below)                                     | `"ITEM"`                                  |
| `Value`  | The value for the reward (item name, command, etc.)             | `"DIAMOND"` or `"give %player% bread 16"` |
| `Amount` | (Optional) The amount for item, currency, or experience rewards | `5`                                       |

**Reward Types:**

- `COMMAND`: Executes a server command (e.g., give items, broadcast messages).
- `ITEM`: Gives a specific item to the player.
- `CURRENCY`: Awards in-game currency (requires economy integration).
- `EXPERIENCE`: Grants experience points or levels.
- `COMPOSITE`: Combines multiple sub-rewards into one.

**Example:**

```yaml
Rewards:
  reward1:
    Type: "ITEM"
    Value: "DIAMOND"
    Amount: 5
  reward2:
    Type: "COMMAND"
    Value: "broadcast %player% has ranked up!"
  reward3:
    Type: "CURRENCY"
    Amount: 1000
  reward4:
    Type: "EXPERIENCE"
    Amount: 3
```

---
