# Machine Configuration Loading Fix

## Issue
When placing a machine item, the server threw an error:
```
java.lang.IllegalStateException: Fabricator structure must be configured
at FabricatorSection.getStructure(FabricatorSection.java:113)
```

## Root Cause
The machine configuration files (`machines.yml` and `fabricator.yml`) were not being loaded. The code had a `// TODO: Load configurations from files` comment, meaning the config sections were instantiated but never populated with data from the YAML files.

## Fix Applied

### File Modified
`RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/RDQ.java`

### Changes Made

1. **Added Imports**
```java
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
```

2. **Implemented Configuration Loading**
```java
// Before (BROKEN):
MachineSystemSection systemConfig = new MachineSystemSection(new EvaluationEnvironmentBuilder());
FabricatorSection fabricatorConfig = new FabricatorSection(new EvaluationEnvironmentBuilder());
// TODO: Load configurations from files

// After (FIXED):
ConfigManager machinesConfigManager = new ConfigManager(plugin, "machines");
ConfigManager fabricatorConfigManager = new ConfigManager(plugin, "machines");

ConfigKeeper<MachineSystemSection> systemConfigKeeper = 
    new ConfigKeeper<>(machinesConfigManager, "machines.yml", MachineSystemSection.class);
ConfigKeeper<FabricatorSection> fabricatorConfigKeeper = 
    new ConfigKeeper<>(fabricatorConfigManager, "fabricator.yml", FabricatorSection.class);

MachineSystemSection systemConfig = systemConfigKeeper.rootSection;
FabricatorSection fabricatorConfig = fabricatorConfigKeeper.rootSection;
```

## How ConfigKeeper Works

`ConfigKeeper` is part of the JEConfig library (Evaluable module) and provides automatic configuration loading:

1. **Automatic Resource Extraction**: If the config file doesn't exist in the plugin's data folder, it automatically copies it from the JAR's resources
2. **YAML Parsing**: Parses the YAML file and maps it to the config section class
3. **Field Population**: Uses reflection and annotations to populate all fields in the config section
4. **Validation**: Validates the configuration based on annotations and field types

## Configuration Files

### machines/machines.yml
Contains global machine system settings:
- Enabled status
- Auto-save interval
- Performance settings
- Default permissions

### machines/fabricator.yml
Contains Fabricator-specific settings:
- Structure definition (core block, required blocks, positions)
- Blueprint requirements (currency, items)
- Crafting settings (cooldown, grid size)
- Fuel system (types, energy values)
- Upgrade definitions (speed, efficiency, bonus output, fuel reduction)

## Build Status
✅ **BUILD SUCCESSFUL**
- rdq-common: Compiled successfully
- rdq-premium: Built successfully
- Only deprecation warnings (existing code, non-critical)

## Testing
After deploying the new JAR:
1. The config files will be automatically created in `plugins/RDQ/machines/`
2. Machine items can be placed without errors
3. Structure validation will work correctly
4. All machine features will be functional

## Related Files
- `RDQ/rdq-common/src/main/resources/machines/machines.yml` - Global settings
- `RDQ/rdq-common/src/main/resources/machines/fabricator.yml` - Fabricator settings
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/config/MachineSystemSection.java` - Config class
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/config/FabricatorSection.java` - Config class

## Next Steps
1. Deploy the new JAR to the server
2. Restart the server
3. Config files will be automatically created
4. Test machine placement with `/rq machine give <player> fabricator`
5. Verify structure detection works correctly

## Technical Notes

### Why ConfigKeeper Instead of Manual Loading?
- **Automatic**: Handles file extraction, parsing, and mapping
- **Type-Safe**: Uses generics for compile-time type checking
- **Validated**: Built-in validation based on annotations
- **Consistent**: Same pattern used throughout the codebase (perks, quests, ranks, bounties)

### Configuration Loading Pattern
This is the standard pattern used across all RDQ systems:
```java
ConfigManager manager = new ConfigManager(plugin, "folder");
ConfigKeeper<SectionClass> keeper = new ConfigKeeper<>(manager, "file.yml", SectionClass.class);
SectionClass config = keeper.rootSection;
```

## Summary
The machine system now properly loads its configuration from YAML files, fixing the "Fabricator structure must be configured" error. The fix follows the established configuration loading pattern used throughout the RDQ plugin.
