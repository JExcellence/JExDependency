# Perk Reward Configuration Documentation

## Overview

The PERK reward type allows you to grant perks to players as part of rank progression. When a player achieves a rank with a PERK reward, the specified perk will be unlocked for that player.

## Configuration Format

### Basic Perk Reward

```yaml
rewards:
  perk_reward_name:
    type: "PERK"
    icon:
      type: "MATERIAL_NAME"  # Icon to display in UI
    perkIdentifier: "perk_id"  # The identifier of the perk to grant
    autoEnable: false  # Optional: Whether to automatically enable the perk after granting
```

### Configuration Fields

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | String | Yes | - | Must be "PERK" |
| `icon` | IconSection | Yes | - | Icon configuration for UI display |
| `perkIdentifier` | String | Yes | - | The unique identifier of the perk to grant (must match a perk defined in the perk system) |
| `autoEnable` | Boolean | No | false | If true, the perk will be automatically enabled after being granted (subject to enabled perk limits) |

## Examples

### Example 1: Basic Perk Reward

Grant a speed boost perk without auto-enabling:

```yaml
rewards:
  speed_boost_reward:
    type: "PERK"
    icon:
      type: "FEATHER"
    perkIdentifier: "speed_boost"
    autoEnable: false
```

### Example 2: Auto-Enabled Perk Reward

Grant a strength perk and automatically enable it:

```yaml
rewards:
  strength_reward:
    type: "PERK"
    icon:
      type: "BLAZE_POWDER"
    perkIdentifier: "strength"
    autoEnable: true
```

### Example 3: Multiple Perk Rewards

Grant multiple perks at once using a composite reward:

```yaml
rewards:
  combat_perks:
    type: "COMPOSITE"
    icon:
      type: "DIAMOND_SWORD"
    rewards:
      - type: "PERK"
        perkIdentifier: "strength"
        autoEnable: true
      - type: "PERK"
        perkIdentifier: "resistance"
        autoEnable: true
      - type: "PERK"
        perkIdentifier: "speed_boost"
        autoEnable: false
```

### Example 4: Perk with Other Rewards

Combine perk rewards with other reward types:

```yaml
rewards:
  veteran_package:
    type: "COMPOSITE"
    icon:
      type: "CHEST"
    rewards:
      - type: "CURRENCY"
        currencyId: "coins"
        amount: 500
      - type: "EXPERIENCE"
        experienceAmount: 1000
        experienceType: "POINTS"
      - type: "PERK"
        perkIdentifier: "night_vision"
        autoEnable: false
      - type: "ITEM"
        item:
          material: "DIAMOND_SWORD"
          amount: 1
```

## Behavior Notes

### Duplicate Perks

If a player already has the perk specified in the reward:
- The system will log an info message
- The reward will be considered successfully granted
- No error will be thrown
- Other rewards in a composite reward will still be granted

### Auto-Enable Behavior

When `autoEnable` is set to `true`:
- The system will attempt to enable the perk immediately after granting
- If the player has reached their maximum enabled perk limit, the perk will be granted but not enabled
- A log message will indicate if auto-enable failed due to limits
- The player can manually enable the perk later from the perk UI

### Perk Availability

- The `perkIdentifier` must match a perk defined in the perk system configuration
- If the perk identifier is not found, the reward will fail and log a warning
- Ensure perks are properly configured before using them in rank rewards

## Integration with Rank System

Perk rewards work seamlessly with the rank system:

1. **Rank Achievement**: When a player completes all requirements for a rank
2. **Reward Granting**: The rank system processes all rewards, including PERK rewards
3. **Perk Unlocking**: The PerkReward handler grants the perk to the player
4. **Notification**: The player receives a notification about the unlocked perk
5. **Auto-Enable** (if configured): The perk is automatically enabled if possible

## Common Use Cases

### Progression-Based Perks

Grant increasingly powerful perks as players progress through ranks:

```yaml
# Tier 1 Rank
rewards:
  basic_speed:
    type: "PERK"
    perkIdentifier: "speed_boost"
    autoEnable: true

# Tier 3 Rank
rewards:
  advanced_combat:
    type: "PERK"
    perkIdentifier: "strength"
    autoEnable: true

# Tier 5 Rank
rewards:
  ultimate_power:
    type: "PERK"
    perkIdentifier: "flight"
    autoEnable: false  # Let player choose when to enable
```

### Specialization Perks

Grant different perks based on rank specialization:

```yaml
# Berserker Path
berserker:
  rewards:
    berserker_perk:
      type: "PERK"
      perkIdentifier: "critical_strike"
      autoEnable: true

# Guardian Path
guardian:
  rewards:
    guardian_perk:
      type: "PERK"
      perkIdentifier: "damage_reduction"
      autoEnable: true

# Gladiator Path
gladiator:
  rewards:
    gladiator_perk:
      type: "PERK"
      perkIdentifier: "combat_heal"
      autoEnable: true
```

## Troubleshooting

### Perk Not Granted

If a perk reward is not being granted:
1. Check that the `perkIdentifier` matches a configured perk exactly
2. Verify the perk system is initialized (check server logs)
3. Ensure the PerkManagementService is available
4. Check for any error messages in the server logs

### Auto-Enable Not Working

If `autoEnable: true` is not enabling the perk:
1. Check if the player has reached their maximum enabled perk limit
2. Verify the perk was successfully granted (check logs)
3. The player can manually enable the perk from the perk UI

### Perk Already Owned

If a player already has a perk:
- This is not an error - the system will skip granting the duplicate
- The reward will be considered successful
- Check logs for info messages about duplicate perks

## See Also

- Perk System Configuration: `/perks/` directory
- Perk System Documentation: See perk system design document
- Other Reward Types: See reward system documentation
