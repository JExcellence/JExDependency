# Plugin Integration Bridge System

## Overview
The Plugin Integration Bridge System provides a flexible, configuration-based way to integrate external plugins with the requirement system without hardcoding support for specific plugins.

## Key Benefits
1. **No Code Changes**: Server owners can add new plugin integrations via YAML configuration
2. **Auto-Detection**: Automatically detects available plugins by category
3. **Reflection-Based**: Uses reflection to call plugin APIs dynamically
4. **Extensible**: Easy to add custom bridges for complex integrations
5. **Backward Compatible**: Old requirements still work, new system is opt-in

## Architecture

### Core Components
- **PluginIntegrationBridge**: Interface for all plugin integrations
- **PluginIntegrationRegistry**: Central registry for all bridges
- **ConfigurableBridge**: Reflection-based bridge configured via YAML
- **PluginRequirement**: Generic requirement that works with any bridge
- **PluginIntegrationLoader**: Loads bridges from configuration

### Configuration File
`plugin-integrations.yml` defines how to integrate with external plugins:

```yaml
integrations:
  ecoskills:
    plugin: "EcoSkills"
    category: "SKILLS"
    api-class: "com.willfp.ecoskills.api.EcoSkillsAPI"
    get-instance-method: "getInstance"
    get-value-method: "getSkillLevel"
    get-value-params: ["Player", "String"]
    available-keys:
      - "mining"
      - "combat"
      - "farming"
```

## Usage Examples

### In Rank Configuration
```yaml
ranks:
  rank_2:
    requirements:
      - type: PLUGIN
        plugin: ecoskills
        values:
          mining: 50
          combat: 30
```

### In Code
```java
// Create a plugin requirement
PluginRequirement req = new PluginRequirement("ecoskills", "mining", 50);

// Check if met
boolean met = req.isMet(player);

// Get progress
double progress = req.calculateProgress(player);

// Get current values
Map<String, Double> current = req.getCurrentValues(player);
```

### Registering Custom Bridges
```java
// Option 1: Via configuration (recommended)
// Add to plugin-integrations.yml

// Option 2: Programmatically
PluginIntegrationBridge customBridge = new MyCustomBridge();
PluginIntegrationRegistry.getInstance().register(customBridge);
```

## Adding New Plugin Support

### Method 1: Configuration (Easiest)
Add to `plugin-integrations.yml`:
```yaml
integrations:
  yourplugin:
    plugin: "YourPlugin"
    category: "SKILLS"  # or JOBS, ECONOMY, etc.
    api-class: "com.yourplugin.api.API"
    get-instance-method: "getInstance"
    get-value-method: "getLevel"
    get-value-params: ["Player", "String"]
    available-keys:
      - "skill1"
      - "skill2"
```

### Method 2: Custom Bridge (Advanced)
Create a class implementing `PluginIntegrationBridge`:
```java
public class YourPluginBridge implements PluginIntegrationBridge {
    @Override
    public String getIntegrationId() { return "yourplugin"; }
    
    @Override
    public String getPluginName() { return "YourPlugin"; }
    
    @Override
    public String getCategory() { return "SKILLS"; }
    
    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("YourPlugin") != null;
    }
    
    @Override
    public double getValue(Player player, String key) {
        // Call your plugin's API
        return YourPluginAPI.getSkillLevel(player, key);
    }
    
    // ... implement other methods
}
```

## Categories
Standard categories:
- **SKILLS**: Player skill levels (EcoSkills, mcMMO, AuraSkills)
- **JOBS**: Job levels (EcoJobs, JobsReborn)
- **ECONOMY**: Money/currency (Vault, PlayerPoints)
- **STATS**: Player statistics
- **CUSTOM**: Any custom category

## Migration Guide

### From Old SkillsRequirement
```yaml
# OLD
requirements:
  - type: SKILLS
    skillPlugin: ECOSKILLS
    skill: mining
    level: 50

# NEW
requirements:
  - type: PLUGIN
    plugin: ecoskills
    values:
      mining: 50
```

### From Old JobsRequirement
```yaml
# OLD
requirements:
  - type: JOBS
    jobPlugin: JOBSREBORN
    job: Miner
    level: 10

# NEW
requirements:
  - type: PLUGIN
    plugin: jobsreborn
    values:
      Miner: 10
```

## Auto-Detection
Use `plugin: auto` with a category to automatically detect available plugins:
```yaml
requirements:
  - type: PLUGIN
    plugin: auto
    category: SKILLS
    values:
      mining: 50
```

This will automatically use EcoSkills, mcMMO, or AuraSkills depending on what's installed.

## Advanced Features

### Multiple Values
Check multiple requirements at once:
```yaml
requirements:
  - type: PLUGIN
    plugin: ecoskills
    values:
      mining: 50
      combat: 30
      farming: 25
```

### Consumable Requirements
For economy or item-based requirements:
```yaml
requirements:
  - type: PLUGIN
    plugin: vault
    values:
      balance: 1000
    consumable: true
```

### Custom Display Names
Override the description key:
```yaml
requirements:
  - type: PLUGIN
    plugin: ecoskills
    values:
      mining: 50
    description: "custom.requirement.mining_master"
```

## Performance Considerations
- Bridges cache API instances
- Reflection calls are minimized
- Failed integrations are logged once
- Auto-detection happens on first use

## Troubleshooting

### Integration Not Found
- Check plugin name in `plugin-integrations.yml`
- Verify plugin is installed and enabled
- Check logs for loading errors

### Wrong Values Returned
- Verify API class and method names
- Check parameter types match
- Test with reflection manually

### Performance Issues
- Use specific plugin ID instead of auto-detection
- Implement custom bridge instead of ConfigurableBridge
- Cache values if checking frequently
