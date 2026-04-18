# Machine System Configuration Examples

This directory contains example configurations for different server types and use cases.

## Available Examples

### Starter Server (`machines-starter.yml`, `fabricator-starter.yml`)
**Best for**: New servers, learning the system, limited resources

**Features**:
- Low blueprint costs (8 iron, 16 redstone)
- Simple 1-block structure
- Slower crafting (10 seconds per craft)
- Limited upgrades (max level 3)
- Basic fuel types (coal, charcoal)
- Max 5 machines per player

**Use this if**:
- Your server is new
- Players are learning the system
- You want affordable automation
- Resources are limited

### Advanced Server (`machines-advanced.yml`, `fabricator-advanced.yml`)
**Best for**: Established servers, end-game content, economy integration

**Features**:
- High blueprint costs (50,000 coins + 32 diamonds + 2 netherite)
- Complex 6-block structure
- Fast crafting (3 seconds per craft)
- Extensive upgrades (max level 10)
- Advanced fuel types (lava, blaze rods)
- Max 20 machines per player

**Use this if**:
- Your server has an established economy
- You want end-game automation
- Players have abundant resources
- You need progression depth

### Creative Server (`fabricator-creative.yml`)
**Best for**: Creative servers, testing, sandbox environments

**Features**:
- No blueprint costs
- Minimal structure
- Very fast crafting (1 second per craft)
- Free upgrades (no requirements)
- No fuel required
- Unlimited machines

**Use this if**:
- Running a creative server
- Testing machine functionality
- Building showcases
- No survival restrictions needed

### Skyblock Server (`fabricator-skyblock.yml`)
**Best for**: Skyblock, resource-limited, renewable focus

**Features**:
- Moderate blueprint costs (16 iron, 32 redstone, 4 diamonds)
- Standard 4-block structure
- Balanced crafting (6 seconds per craft)
- Renewable fuel focus (wood, kelp, charcoal)
- Moderate upgrades (max level 5)
- Limited machines (3-5 per player)

**Use this if**:
- Running a skyblock server
- Resources are limited
- Renewable resources are important
- Balanced progression is key

## How to Use These Examples

### Option 1: Direct Copy
1. Choose the example that matches your server type
2. Copy the files to `plugins/RDQ/machines/`
3. Rename them to remove the prefix:
   - `machines-starter.yml` → `machines.yml`
   - `fabricator-starter.yml` → `fabricator.yml`
4. Restart the server or run `/rq machine reload`

### Option 2: Merge with Existing
1. Open your current configuration files
2. Compare with the example files
3. Copy specific sections you want to use
4. Adjust values to match your server's needs
5. Save and reload

### Option 3: Customize
1. Start with the example closest to your needs
2. Modify values to match your vision:
   - Adjust costs in `blueprint.requirements`
   - Change crafting speed in `crafting.base-cooldown-ticks`
   - Modify upgrade costs and effects
   - Add/remove fuel types
3. Test on a development server first
4. Deploy to production

## Configuration Comparison

| Feature | Starter | Advanced | Creative | Skyblock |
|---------|---------|----------|----------|----------|
| Blueprint Cost | Low | Very High | None | Moderate |
| Crafting Speed | Slow (10s) | Fast (3s) | Very Fast (1s) | Balanced (6s) |
| Max Upgrades | Level 3 | Level 10 | Level 5 | Level 5 |
| Fuel Required | Yes | Yes | No | Yes |
| Max Machines | 5 | 20 | Unlimited | 3-5 |
| Structure Size | 1 block | 6 blocks | 1 block | 4 blocks |
| Economy Integration | No | Yes | No | No |

## Customization Tips

### Adjusting Difficulty

**Make it easier**:
- Reduce `blueprint.requirements` costs
- Lower `crafting.base-cooldown-ticks` (faster crafting)
- Reduce upgrade costs
- Increase `fuel-types.energy-value`
- Increase `cache.max-machines-per-player`

**Make it harder**:
- Increase `blueprint.requirements` costs
- Raise `crafting.base-cooldown-ticks` (slower crafting)
- Increase upgrade costs
- Decrease `fuel-types.energy-value`
- Decrease `cache.max-machines-per-player`

### Balancing for Your Economy

1. **Calculate player income**: How much currency do players earn per hour?
2. **Set blueprint cost**: 1-2 hours of income for starter machines
3. **Set upgrade costs**: Progressive scaling (level 1 = 30 min, level 5 = 2 hours)
4. **Test with players**: Adjust based on feedback

### Server Type Recommendations

**Survival**: Use Starter or Skyblock examples
**Economy**: Use Advanced example
**Creative**: Use Creative example
**Hardcore**: Use Skyblock example with higher costs
**Modded**: Use Advanced example with custom materials

## Testing Your Configuration

1. **Syntax Check**: Reload and check console for errors
   ```
   /rq machine reload
   ```

2. **Blueprint Test**: Try creating a machine
   - Verify costs are correct
   - Check structure detection works
   - Confirm permissions are required

3. **Crafting Test**: Set a recipe and enable the machine
   - Verify crafting speed
   - Check fuel consumption
   - Test output generation

4. **Upgrade Test**: Apply upgrades
   - Verify costs are correct
   - Check effects are applied
   - Test max level limits

5. **Performance Test**: Run multiple machines
   - Monitor server TPS
   - Check database load
   - Verify auto-save works

## Common Modifications

### Change Blueprint Cost
```yaml
blueprint:
  requirements:
    items:
      type: "item"
      required-items:
        your-item:
          material: YOUR_MATERIAL
          amount: YOUR_AMOUNT
```

### Add New Fuel Type
```yaml
fuel-types:
  your-fuel:
    material: YOUR_MATERIAL
    energy-value: YOUR_VALUE
```

### Modify Upgrade Cost
```yaml
upgrades:
  speed:
    requirements:
      level-1:
        type: "item"
        required-items:
          your-item:
            material: YOUR_MATERIAL
            amount: YOUR_AMOUNT
```

### Change Structure
```yaml
structure:
  core-block: YOUR_CORE_BLOCK
  required-blocks:
    - type: YOUR_BLOCK
      relative-positions:
        - {x: X, y: Y, z: Z}
```

## Support

For more information:
- See `MACHINE_SYSTEM_ADMIN_GUIDE.md` for detailed configuration reference
- See `MACHINE_SYSTEM_CONFIG_REFERENCE.md` for all options
- See `MACHINE_SYSTEM_PLAYER_GUIDE.md` for player instructions

## Version

These examples are for Machine Fabrication System v1.0.0
