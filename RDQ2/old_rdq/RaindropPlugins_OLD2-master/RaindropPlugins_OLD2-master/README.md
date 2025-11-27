# 🌧️ RaindropPlugins Project Suite

A modular plugin ecosystem built by **[ItsRainingHP](https://github.com/ItsRainingHP)** and *
*[JExcellence](https://github.com/JExcellence)**(JExcellence) for Minecraft servers.  
Includes robust support for multi-language environments, multi-currency economies, player progression systems, and GUI-driven experiences.

---

## 🧱 Steps to Compile

1. Clone [RaindropPlugins_](https://github.com/Antimatter-Zone/RaindropPlugins_)
2. Clean, build, and publishToMavenLocal the following:
3. [JEConfig](https://github.com/Antimatter-Zone/JEConfig)
4. [JEHibernate](https://github.com/Antimatter-Zone/JEHibernate)
5. [RPlatform](https://github.com/Antimatter-Zone/RaindropPlugins_/tree/master/RPlatform)
6. [R18n](https://github.com/Antimatter-Zone/RaindropPlugins_/tree/master/R18n)
7. [RCommands](https://github.com/Antimatter-Zone/RaindropPlugins_/tree/master/RCommands)
8. [RCore](https://github.com/Antimatter-Zone/RaindropPlugins_/tree/master/RCore)
   a. Note: Must also run `shadowFree` task
9. [JECurrency](https://github.com/Antimatter-Zone/RaindropPlugins_/tree/master/JECurrency)
10. [RaindropQuests](https://github.com/Antimatter-Zone/RaindropPlugins_/tree/master/RaindropQuests)
   a. `RDQImpl-Free-6.0.0` and `RDQImpl-Premium-6.0.0` in `RaindropQuests/build/libs`
   b. `RCore-2.0.0` in `RCore/build/libs`

## 📦 Module Overview

### 🔤 R18n – Internationalization - ![Complete](https://img.shields.io/badge/status-complete-brightgreen) 
Provides localization support for all Raindrop plugins.  
Enables dynamic language switching and easy maintenance of translated content.

---

### 💱 JECurrency – Multi-Currency System - ![Complete](https://img.shields.io/badge/status-complete-brightgreen) 
Supports multiple in-game currencies with flexible configuration.  
Ideal for servers using unique economic systems (e.g. Raindrops, Tokens).

---

### 🧠 RCore – Core Configuration & Properties - ![Complete](https://img.shields.io/badge/status-complete-brightgreen) 
Handles plugin-wide configuration properties, including:

- Dependency resolution
- Database access setup
- Logger configuration
- Translation file management

```java
@Override
public void onEnable() {
    this.initializeComponents();
    this.initializeRepositories();
    this.initializePlugins();

    this.rServer = new RServer(UUID.randomUUID(), getServer().getName());
    this.rServerRepository.createAsync(rServer).withCompleteAsync(((server, throwable) -> {
        if (throwable != null) {
            this.getRLogger().logDebug("Error when saving server by uniqueId: " + rServer.getUniqueId());
            return;
        }

        this.rServer = server;
    }));
}
````

---

### 🧱 RPlatform – Shared Platform API - ![Complete](https://img.shields.io/badge/status-complete-brightgreen) 

Foundation for all Raindrop plugins, instantiated in `onLoad()`.

Features include:

* Dependency loader and config manager
* Custom event system and logger
* Metrics tracking
* Entity serializers (items, potions, etc.)
* Placeholder handler
* Scheduler and teleportation factory

```java
@Override
public void onLoad() {
    this.platform = new RPlatform(this);
    this.pluginServiceRegistry = new PluginServiceRegistry(this.platform);
}
```

---

## 🗺️ RaindropQuests (RDQImpl) - ![In Progress](https://img.shields.io/badge/status-in--progress-yellow)  

A feature-rich gameplay plugin introducing quests, ranks, bounties, perks, stats, shops, titles, and more.

---

### 🎮 Player Command Summary
`* Some perks are premium only`

| Command               | Description                              | Status                                                                     | Premium |
|-----------------------|------------------------------------------|----------------------------------------------------------------------------|---------|
| `/rq admin`           | Opens the admin GUI                      | ![In Progress](https://img.shields.io/badge/status-in--progress-yellow)    | False   |
| `/rq advancements`    | Opens the tiered advancements GUI        | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | True    |
| `/rq bounties`        | View, create, and contribute to bounties | ![Under Review](https://img.shields.io/badge/status-under%20review-orange) | True    |
| `/rq main`            | Opens the main RDQImpl menu                  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq options`         | Player customization options             | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq perks`           | Manage and toggle perks                  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False*  |
| `/rq quests`          | Browse and accept available quests       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq ranks`           | View rank tree and progression           | ![In Progress](https://img.shields.io/badge/status-in--progress-yellow)    | True    |
| `/rq recipe <ItemID>` | View custom recipes                      | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq sage`            | Use the Sage perk to search chests       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | True    |
| `/rq store`           | Shop for perks and machines              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq stats`           | View RDQImpl stats                           | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq titles`          | Manage nameplate/chat titles             | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | True    |
| `/rq balance`         | Check raindrop currency                  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq info`            | View player profile summary              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq scoreboard`      | Toggle RDQImpl scoreboard                    | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq toggle <perkID>` | Toggle a perk on/off                     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |

### 🧠 Developer Command Descriptions

<details>
<summary><strong>/rq advancements</strong></summary>

Opens the advancements GUI.

* Roughly 120 advancements across 5 tiers
* Earned through mining, quests, ranking up, etc.
* Buttons represent highest tier; lore displays lower tiers
* Tier progression is linear: e.g., Tier 1 = 10 quests, Tier 2 = 50, etc.

</details>

<details>
<summary><strong>/rq bounties</strong></summary>

Opens the bounty system GUI.

* Players can view, create, or contribute to bounties
* Intended to promote community-based targets and challenges

</details>

<details>
<summary><strong>/rq perks</strong></summary>

GUI displaying perks owned by the player.

* Left-click: open details GUI (cooldown, stats, lore, toggle)
* Right-click: toggle the perk state
* Perk logic can be extended to interact with cooldowns, costs, or statuses

</details>

<details>
<summary><strong>/rq quests</strong></summary>

Shows a menu of all available quests.

* Clicking opens sub-quest GUI:

  * Cooldown status
  * Quest name
  * Total completions
  * Lore book (via ProtocolLib)
  * Requirements GUI
  * Progress & completion buttons
* `/rq quests [ID]`: Open a specific quest directly
* `/rq quests Lore <ID>`: Open lore book or GUI fallback

</details>

<details>
<summary><strong>/rq ranks</strong></summary>

Displays player ranks and progression.

* Click a rank to open sub-GUI:

  * Requirements
  * Lore (book or GUI)
  * Progress bar
  * Rank-up button
* `/rq ranks [ID]` opens a rank directly
* `/rq ranks Lore <ID>` shows lore view

</details>

<details>
<summary><strong>/rq sage</strong></summary>

GUI-driven chest search powered by Sage perk.

* Player can define a search area (x, y, z)
* Results are shown as chest coordinates
* Clicking highlights chests with particles
* Future plan: open chest remotely from GUI after forging perk (not implemented)

</details>

<details>
<summary><strong>/rq store</strong></summary>

Shop GUI with two categories:

* **Perks:** Buy and unlock perks
* **Machines:** Purchase RDQImpl machines with compounding price model
* Future items: skill/job tokens, perk limit increases

</details>

<details>
<summary><strong>/rq titles</strong></summary>

Manages player chat/nameplate titles.

* Titles earned from Ranks and Advancements
* Integrated with LuckPerms for prefix/suffix handling
* Supports automatic switching if better title is earned

</details>

---

### 🛠️ RDQImpl Admin Command Summary

| Command                        | Description                     | Status                                                               | Premium |
| ------------------------------ | ------------------------------- | -------------------------------------------------------------------- |---------|
| `/rq admin create groups`      | Create rank groups in LuckPerms | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin create permissions` | Set default permissions         | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin give`               | Give player a custom item       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin perk`               | Grant/remove perks              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin forge`              | Set forged status for perks     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin quest reset`        | Reset quest cooldown for player | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin rank`               | Add/remove/reset ranks          | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | True    |
| `/rq admin reload`             | Reload plugin                   | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin stats clear`        | Reset player stats              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |
| `/rq admin money`              | Adjust player raindrop currency | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | False   |

### 🧠 Developer Command Descriptions

<details>
<summary><strong>/rq admin forge</strong></summary>

Grants or removes the forged status for a perk.

* Forged perks may unlock additional features
* Used in systems like Sage to enhance GUI capability

</details>

## Perk Summary

| Perk Name          | Description                                                                                                                               | Status                                                                     | Forged                                                                      |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- | ----------------------------------------------------------------------------|
| Amplification      | Chance to strengthen potion effect. While perk is active, consuming potions has a chance to improve the potion effect.                    | ![In Progress](https://img.shields.io/badge/status-in--progress-yellow)    | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Atomic Deformation | Unlock the Atomic Reactor machine. Atomic Reactors will bottle XP for you.                                                                | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)  | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Chrono Crafter     | Chance not to consume material when crafting. Roll percent based on material type.                                                        | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Captis             | Improves fishing catch rate.                                                                                                              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Condense           | Efficiently pack abundant Minecraft materials for improved storage.                                                                       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Crystallize        | Unlock the amethyst recipe.                                                                                                               | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Demonforging       | Unlock demonforging recipes. Demonforged equipment can be enchanted beyond table maximums.                                                | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Expedire           | Craft seeds directly into wheat! No wasting bone meal or waiting on grow time.                                                            | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Externelixiris     | Chance to lengthen potion effect. While perk is active, consuming potions has a 15.00% (45.00% when forged) chance to lengthen the potion's effect. | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |  ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Godsend            | Chance to obtain bonus item.                                                                                                              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Mechanization      | Unlock the Fabricator machine. Fabricators will automatically craft items.                                                                | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Mystic Mender      | Chance to increase amount when mending.                                                                                                   | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Mixologist         | Turn dye back into ingredients.                                                                                                           | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Piscatio           | Earn extra fishing XP.                                                                                                                    | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Quantum Storage    | Unlock the Quantum Storage machine. When Quantum Retrieval is enabled and a storage chest is destroyed. The area becomes unstable as items are transported from the quantum realm on a set interval.                                                                | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |  ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Storage Sage       | Access special sage GUI which points you to the items.                                                                                    | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Scavenger          | Unlock the Scavenger Chest. Scavenger Chest collects items in a radius automatically.                                                     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Superheat          | Unlock superheat recipes. Crafting bars without a furnace or fuel!                                                                        | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Transmogrification | Unlock the transmogrification recipe.                                                                                                     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Transmutation      | Convert golden apples and carrots back into gold.                                                                                         | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Twinbrew Mastery   | Chance to save potion on consume. While perk is active, consuming potions has a chance to not consume the potion.                         | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |
| Spirit Walker      | Neutralize the damage caused by Soul Speed. Note: Taking damage temporarily disables perk protection for 30 ticks.                        | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) | ![oaicite:10](https://img.shields.io/badge/status-not--started-lightgrey)   |

## 📊 Complete V2 Rank System Architecture

---

## 🔧 Factory System

**`RankSystemFactory`**
- Handles loading, parsing, and creating all rank entities from config files

**Key Features:**
- 📁 **Automatic Configuration Loading:**  
  Reads from `config-v2.yml` and `ranks-v2/` directory
- 🧱 **Entity Creation:**  
  Converts configurations into database entities
- 🔗 **Connection Establishment:**  
  Sets up rank progression paths and tree connections
- ✅ **Validation:**  
  Ensures system consistency and reports configuration issues

---

## ⚡ Upgrade Structure

**`RankUpgradeService`**
- Manages rank progression, eligibility checking, and upgrades

**Core Capabilities:**
- 🔄 **Cross-Tree Switching:**  
  Allows switching between trees at the same tier level
- 📈 **Progress Tracking:**  
  Comprehensive overview across all rank trees
- 🔍 **Eligibility Validation:**  
  Checks for requirements, prerequisites, and final rank conditions
- 📦 **Result Objects:**  
  Structured results for upgrades and validation responses

---

## 👁️ View System with I18n

**`RankOverviewView`**
- Responsible for formatted displays of rank progress and data

**Highlights:**
- 🌐 **I18n Integration:**  
  Full internationalization using language keys
- ▓ **Progress Visualization:**  
  ASCII progress bars and styled text output
- 🌳 **Tree Detail Views:**  
  Drill-down into individual rank trees
- 🆚 **Rank Comparison:**  
  View and compare ranks for switching across trees

---

## 🌐 I18n Configuration

**Localization Infrastructure:**
- 🗝️ **Language Keys:**  
  All UI text uses keys instead of hardcoded strings
- ⚙️ **Auto-Generation:**  
  Factory generates missing keys automatically
- 📜 **Translation Files:**  
  Includes complete English translation file
- 🔄 **Flexible Formatting:**  
  Supports placeholders and dynamic content injection

---

## 📋 Supporting Services

- `RankProgressService`: Manages player progression data
- `RankRequirementService`: Validates rank-specific requirements
- `RankRewardService`: Handles player reward distribution

---

## 🎯 Key Features Implemented

### ✅ Unlimited Flexibility
- Unlimited ranks across unlimited trees
- File-based tree management (`filename = tree name`)
- Branching and converging progression paths
- Cross-tree connections and switching support

### ✅ Advanced Final Rank System
- Final ranks that require multiple tree completions
- Flexible completion logic: all, specific, or min-tier trees
- Alternative and "ultimate" rank paths

### ✅ I18n Support
- All display text uses language keys
- Auto-generation of missing keys
- Full translation system with formatting support

### ✅ Comprehensive Views
- Overall progress and achievement tracking
- Detailed tree-level insights
- Rank comparisons for switching decisions
- ASCII progress bars and visual indicators

### ✅ Robust Architecture
- Factory pattern for config and entity management
- Service layer encapsulates business logic
- View layer handles user-facing presentation
- Clean separation of concerns
- Extensive validation and error reporting

---

## 📁 File Structure

entities/rank/v2/  
├── Rank.java # Core rank entity  
├── RankTree.java # Rank tree/group entity  
├── FinalRank.java # Special final rank entity  
├── PlayerRankProgress.java # Player progress tracking  
├── RankRequirement.java # Rank requirements  
└── RankReward.java # Rank rewards  

factory/rank/v2/  
└── RankSystemFactory.java # Configuration loading & entity creation  

service/rank/v2/  
├── RankUpgradeService.java # Upgrade logic & progression  
├── RankProgressService.java # Progress tracking interface  
├── RankRequirementService.java # Requirement validation interface  
└── RankRewardService.java # Reward handling interface  

view/rank/v2/  
└── RankOverviewView.java # Display & formatting with I18n  

config/rank/v2/  
├── RankConfigSectionV2.java # I18n-enabled rank config  
├── RankTreeConfigSection.java # Tree configuration  
└── RankSystemConfigSection.java # System configuration  

---

## 💼 RaindropReserve (RDR) - ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey)

Vault management system with secure access and chest search capabilities.

---

### 🎮 RDR Player Command Summary

| Command                              | Description                              | Status                                                               |
| ------------------------------------ | ---------------------------------------- | -------------------------------------------------------------------- |
| `/rr stats`                          | Show player vault stats                  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr store`                          | Buy more vaults                          | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr main`                           | Access main menu GUI                     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr <type> [number]`                | Open specific vault                      | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr trusted <type> <number>`        | Manage vault trust settings              | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr other <target> <type> <number>` | Open another player’s vault (with trust) | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr search <Material>`              | Search vaults for material               | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |

### 🧠 Developer Command Descriptions

<details>
<summary><strong>/rr search & trusted</strong></summary>

* `/rr search <Material>`: Populates GUI with vaults containing the item
* `/rr trusted <type> <number>`: Add or remove trusted players per vault
* `/rr other ...`: Requires trust granted by target player

</details>

---

### 🛠️ RDR Admin Command Summary

| Command                                     | Description              | Status                                                               |
| ------------------------------------------- | ------------------------ | -------------------------------------------------------------------- |
| `/rr admin reload`                          | Reload the plugin        | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr admin unlock`                          | Force-unlock all vaults  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr admin add <player> <type> <number>`    | Grant vault to a player  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr admin remove <player> <type> <number>` | Remove vault from player | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| `/rr admin clear <player> <type> <number>`  | Clear vault contents     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |

### 🧠 Developer Command Descriptions

<details>
<summary><strong>/rr admin unlock</strong></summary>

* Removes `locked = true` flag from all vault entities in the database.
* Useful for recovery or emergency access.

</details>

---

## 📝 Future Features & Considerations

* 🔧 In-game admin GUI for managing players and vaults
* 🧙 Setup wizard GUI to assist with initial configuration
* 📘 Expanded lore integration using ProtocolLib
* 🛠️ Modular perks and machine shop customization

---

## 🔗 Supported Soft Dependencies

| Plugin Name                                                                       | Description                                                                                 | Status                                                               |
|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| [LuckPerms](https://luckperms.net/)                                               | A powerful permissions plugin for managing player permissions and prefixes.                 | ![In Progress](https://img.shields.io/badge/status-in--progress-yellow)     |
| [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)              | Enhances lore and book GUIs (optional dependency).                                          | ![In Progress](https://img.shields.io/badge/status-in--progress-yellow)     |
| [MythicMobs](http://www.mythiccraft.io/)                                          | Allows creation of custom mobs with advanced abilities and drops.                          | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [Towny](https://github.com/TownyAdvanced/Towny)                                   | Manages land protection and town creation for multiplayer servers.                         | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [TownyChat](https://github.com/TownyAdvanced/TownyChat)                           | Integrates chat channels with Towny, supporting town-based communication.                  | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [HuskTowns](https://william278.net/project/husktowns)                             | A lightweight town plugin with a focus on simplicity and performance.                      | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [JobsReborn](https://www.spigotmc.org/resources/jobs-reborn.4216/)               | Allows players to earn money by performing tasks like mining, fishing, and building.       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [EcoJobs](https://github.com/Auxilor/EcoJobs)                                     | Enables players to earn money by performing jobs and tasks.                                | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [mcMMO](https://github.com/mcMMO-Dev/mcMMO)                                       | Adds RPG-like skill leveling to Minecraft, enhancing gameplay with abilities and stats.    | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/)               | Adds RPG skills and abilities with customizable progression.                               | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [Vault](https://github.com/MilkBowl/Vault)                                        | A permissions, chat, and economy API for Spigot servers.                                   | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [PlaceholderAPI](https://github.com/placeholderapi)                               | Allows integration of placeholders from various plugins into messages and displays.        | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [ChestSort](https://www.spigotmc.org/resources/chestsort-api.59773/)             | Automatically sorts items in chests and inventories.                                       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [CMI](https://github.com/Zrips/CMI)                                               | A comprehensive plugin offering commands, teleportation, and other utility features.       | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [EssentialsX](https://github.com/EssentialsX/Essentials)                          | Core plugin with commands like /home, /spawn, and teleportation tools.                     | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [EssentialsChat](https://github.com/EssentialsX/Essentials)                       | Adds formatting and extra functionality to in-game chat.                                   | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [EssentialsSpawn](https://github.com/EssentialsX/Essentials)                      | Handles spawn points and player respawn behavior.                                          | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |
| [EssentialsDiscord](https://github.com/EssentialsX/Essentials)                    | Bridges Minecraft chat with a Discord server.                                               | ![Not Started](https://img.shields.io/badge/status-not--started-lightgrey) |

