# Machine Fabrication System - Player Guide

## Overview

The Machine Fabrication System allows you to build automated crafting machines that produce items while you're away. This guide will teach you how to build, configure, and operate your machines.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Building Your First Machine](#building-your-first-machine)
3. [Machine Interface](#machine-interface)
4. [Configuring Recipes](#configuring-recipes)
5. [Managing Storage](#managing-storage)
6. [Fuel System](#fuel-system)
7. [Upgrades](#upgrades)
8. [Trust System](#trust-system)
9. [Tips & Tricks](#tips--tricks)
10. [FAQ](#faq)

---

## Getting Started

### What is a Fabricator?

A Fabricator is an automated crafting machine that:
- Crafts items automatically when enabled
- Stores materials in virtual storage
- Consumes fuel to operate
- Can be upgraded for better performance
- Allows trusted friends to access it

### Requirements

To build a Fabricator, you need:
- Permission from server admins
- Blueprint materials (check with `/rq machine` or ask admins)
- Sufficient space to build the structure
- Currency (if required by server)

---

## Building Your First Machine

### Step 1: Obtain a Machine Item

Machines can be obtained through:
- Admin commands (`/rq machine give`)
- Crafting (if enabled)
- Shop purchases (if configured)
- Rewards or events

### Step 2: Build the Structure

The Fabricator requires a specific block pattern:

```
Top View:
  [Hopper]
[Hopper][Dropper][Hopper]
  [Chest]

Side View:
[Dropper] ← Core block
[Hopper] ← Below core
```

**Building Instructions**:
1. Place a **Hopper** on the ground
2. Place a **Dropper** on top of the hopper (this is the core block)
3. Place **Hoppers** to the East, West, and below the dropper
4. Place a **Chest** to the South of the dropper

### Step 3: Activate the Machine

1. Right-click the **Dropper** (core block) with the machine item
2. If the structure is valid, the machine will be created
3. Blueprint materials will be consumed
4. You'll receive a confirmation message

**Common Issues**:
- "Invalid structure" - Check block placement
- "Insufficient materials" - You need more blueprint items
- "No permission" - Contact server admins

---

## Machine Interface

Right-click the core block (Dropper) to open the machine GUI.

### Main Menu

The main menu shows:
- **Machine State**: ON/OFF toggle
- **Fuel Level**: Current fuel and maximum capacity
- **Recipe Preview**: Currently configured recipe
- **Storage**: Access stored items
- **Trust**: Manage who can access your machine
- **Upgrades**: View and apply upgrades

### Navigation

- Click buttons to navigate between menus
- Use the **Back** button to return to previous menu
- Close inventory to exit the GUI

---

## Configuring Recipes

### Setting a Recipe

1. Open the machine GUI
2. Click **Recipe Configuration**
3. Place items in the 3x3 crafting grid
4. The output will be shown if the recipe is valid
5. Click **Set Recipe** to lock it in
6. The machine will now craft this item automatically

### Recipe Requirements

- Recipe must be a valid Minecraft crafting recipe
- Shaped recipes maintain their pattern
- Shapeless recipes work in any arrangement
- You need sufficient fuel to activate the recipe

### Changing Recipes

1. Turn the machine **OFF**
2. Open **Recipe Configuration**
3. Click **Clear Recipe**
4. Set a new recipe following the steps above

**Note**: You cannot change recipes while the machine is running.

---

## Managing Storage

### Virtual Storage

Machines have unlimited virtual storage for:
- **Input Materials**: Items used for crafting
- **Output Items**: Crafted items
- **Fuel**: Energy source for the machine

### Adding Items

1. Open the machine GUI
2. Click **Storage Management**
3. Click **Deposit Items**
4. Place items from your inventory
5. Items are added to virtual storage

### Removing Items

1. Open **Storage Management**
2. Find the item you want to withdraw
3. Click the item
4. Choose quantity to withdraw
5. Items are added to your inventory

### Physical Storage

You can also use the attached blocks:
- **Hoppers**: Automatically feed items into the machine
- **Chest**: Automatically receive crafted items

**Note**: Physical storage is limited by block capacity. Virtual storage is unlimited.

---

## Fuel System

### Why Fuel?

Machines consume fuel to operate. Without fuel, the machine will automatically turn OFF.

### Fuel Types

Common fuel types (check with admins for server-specific values):
- **Coal**: 100 energy
- **Coal Block**: 900 energy
- **Lava Bucket**: 2000 energy

### Adding Fuel

1. Open the machine GUI
2. Click the **Fuel** indicator
3. Place fuel items from your inventory
4. Fuel is converted to energy automatically

### Fuel Consumption

- Each craft consumes a fixed amount of fuel
- Upgrades can reduce fuel consumption
- Efficiency upgrades can prevent fuel consumption
- Monitor fuel level to avoid machine shutdown

---

## Upgrades

### Upgrade Types

**Speed Upgrade**
- Reduces crafting cooldown
- Each level: 10% faster
- Max level: 5
- Effect: Level 5 = 50% faster crafting

**Efficiency Upgrade**
- Chance to not consume fuel
- Each level: 15% chance
- Max level: 5
- Effect: Level 5 = 75% chance to save fuel

**Bonus Output Upgrade**
- Chance to produce double output
- Each level: 10% chance
- Max level: 3
- Effect: Level 3 = 30% chance for double items

**Fuel Reduction Upgrade**
- Reduces fuel consumption
- Each level: 10% reduction
- Max level: 5
- Effect: Level 5 = 50% less fuel used

### Applying Upgrades

1. Open the machine GUI
2. Click **Upgrades**
3. Select the upgrade you want
4. Check the requirements
5. Click **Apply Upgrade** if you have the materials
6. Upgrade is applied immediately

### Upgrade Strategy

**For Maximum Speed**:
- Prioritize Speed upgrades
- Add Fuel Reduction to offset increased fuel usage

**For Efficiency**:
- Max out Efficiency upgrades first
- Add Fuel Reduction for minimal fuel costs

**For Profit**:
- Focus on Bonus Output upgrades
- Combine with Speed for maximum production

---

## Trust System

### Why Trust Players?

The trust system allows you to:
- Let friends access your machine
- Share resources with team members
- Collaborate on automation projects

### Adding Trusted Players

1. Open the machine GUI
2. Click **Trust Management**
3. Click **Add Player**
4. Type the player's username
5. Player is added to trust list

### Removing Trusted Players

1. Open **Trust Management**
2. Find the player in the list
3. Click **Remove**
4. Player can no longer access the machine

### Trust Permissions

Trusted players can:
- Open the machine GUI
- Add/remove items from storage
- Add fuel
- Turn the machine ON/OFF
- View the recipe

Trusted players **cannot**:
- Change the recipe
- Apply upgrades
- Add/remove other trusted players
- Break the machine (unless server allows)

---

## Tips & Tricks

### Efficient Fuel Management

- Use Coal Blocks instead of Coal (9x more efficient)
- Apply Efficiency upgrades early
- Keep fuel stocked to avoid downtime
- Use Lava Buckets for long-term operation

### Maximizing Output

- Upgrade Speed first for more crafts per hour
- Add Bonus Output for extra items
- Keep materials stocked in storage
- Use hoppers to auto-feed materials

### Resource Management

- Store excess materials in the machine
- Use the machine as a secure storage
- Withdraw items regularly to avoid clutter
- Monitor storage through the GUI

### Automation Setup

1. Place hoppers above the machine to auto-feed materials
2. Place a chest below to auto-collect output
3. Connect hoppers to your storage system
4. Machine runs automatically when materials are available

### Security

- Only trust players you know well
- Remove trust when players leave your team
- Keep valuable items in separate storage
- Monitor machine activity regularly

---

## FAQ

### Q: Why won't my machine turn ON?

**A**: Check these requirements:
- Recipe is set and valid
- Fuel level is above 0
- Materials are available in storage
- Machine is not broken or disabled

### Q: Can I move my machine?

**A**: Yes! Break the core block (Dropper) and the machine item will drop. Place it elsewhere to rebuild. Your fuel, upgrades, recipe, and storage are preserved.

### Q: What happens if the server crashes?

**A**: Machines auto-save periodically. You may lose a few minutes of progress, but most data is preserved.

### Q: How many machines can I have?

**A**: Check with server admins. There may be a limit per player.

### Q: Can I use the machine while offline?

**A**: No. Machines only operate when the chunk is loaded (usually when you or another player is nearby).

### Q: What happens to my items if I break the machine?

**A**: Depends on server settings:
- Items may drop on the ground
- Items may stay in virtual storage and restore when you place the machine again

### Q: Can I upgrade a machine multiple times?

**A**: Yes! Each upgrade type has multiple levels. Apply them one at a time.

### Q: Do upgrades stack?

**A**: Yes! All upgrades work together. Speed + Efficiency + Bonus Output = Fast, efficient, profitable machine.

### Q: Can I see what recipe a machine is using?

**A**: Yes. Open the machine GUI and check the Recipe Preview in the main menu.

### Q: How do I know when my machine runs out of fuel?

**A**: The machine will automatically turn OFF and you'll see the fuel level at 0 in the GUI.

---

## Advanced Techniques

### Multi-Machine Setup

Build multiple machines for different recipes:
- One for tools
- One for building blocks
- One for food items
- Specialize each machine with different upgrades

### Hopper Networks

Connect machines with hopper chains:
```
[Chest] → [Hopper] → [Machine] → [Hopper] → [Chest]
```
- Input chest feeds materials
- Output chest collects products
- Fully automated production line

### Upgrade Priorities

**Early Game**: Focus on Speed
- Get items faster
- Build up resources quickly

**Mid Game**: Add Efficiency
- Reduce fuel costs
- Sustain longer operation

**Late Game**: Max Everything
- Speed + Efficiency + Bonus Output + Fuel Reduction
- Ultimate automation machine

---

## Getting Help

If you encounter issues:
1. Check this guide for solutions
2. Ask server admins for help
3. Report bugs to server staff
4. Share tips with other players

---

**Happy Automating!**

*This guide is for the Machine Fabrication System v1.0.0*
