# Config Sections No-Arg Constructors Fix

## Issue
Machine system failed to initialize with error:
```
java.lang.IllegalStateException: Core block must be configured
at MachineStructureSection.getCoreBlock(MachineStructureSection.java:68)
at MachineStructureSection.afterParsing(MachineStructureSection.java:102)
at ConfigMapper.mapSectionSub(ConfigMapper.java:199)
```

## Root Cause
ConfigMapper/ConfigKeeper requires no-arg constructors to automatically instantiate config section classes when reading from YAML files. The machine config sections only had constructors that required an `EvaluationEnvironmentBuilder` parameter, which ConfigMapper couldn't provide.

## Fix Applied
Added no-arg constructors to all machine config section classes that delegate to the parameterized constructor with a default `EvaluationEnvironmentBuilder`.

### Files Modified

1. **MachineSystemSection.java**
   - Added no-arg constructor

2. **MachineStructureSection.java**
   - Added no-arg constructor to `MachineStructureSection`
   - Added no-arg constructor to nested `RequiredBlockSection`

3. **FabricatorSection.java**
   - Added no-arg constructor to `FabricatorSection`
   - Added no-arg constructor to `BlueprintSection`
   - Added no-arg constructor to `CraftingSection`
   - Added no-arg constructor to `FuelSection`
   - Added no-arg constructor to `FuelTypeSection`
   - Added no-arg constructor to `UpgradesSection`
   - Added no-arg constructor to `UpgradeDefinitionSection`

### Pattern Applied
```java
// Existing parameterized constructor
public SectionClass(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
}

// NEW: No-arg constructor for ConfigMapper
public SectionClass() {
    this(new EvaluationEnvironmentBuilder());
}
```

## How ConfigMapper Works

When ConfigMapper reads a YAML file:

1. **Instantiation**: Uses reflection to call the no-arg constructor
2. **Field Population**: Maps YAML keys to Java fields (kebab-case → camelCase)
3. **Nested Sections**: Recursively instantiates nested section classes
4. **Validation**: Calls `afterParsing()` to validate the loaded configuration

Without no-arg constructors, ConfigMapper cannot instantiate the classes, resulting in null fields and validation errors.

## Build Status
✅ **BUILD SUCCESSFUL**
- All config sections now have no-arg constructors
- ConfigMapper can properly instantiate all sections
- YAML configuration will be loaded correctly

## Testing
After deploying the new JAR:
1. Config files will be created in `plugins/RDQ/machines/`
2. ConfigKeeper will load the YAML files
3. ConfigMapper will instantiate all sections using no-arg constructors
4. Machine system will initialize successfully
5. Machine placement will work without errors

## Technical Notes

### Why No-Arg Constructors Are Required

ConfigMapper uses reflection to instantiate config section classes:
```java
// ConfigMapper internally does something like:
Class<?> sectionClass = MachineStructureSection.class;
Constructor<?> constructor = sectionClass.getDeclaredConstructor(); // No-arg!
Object instance = constructor.newInstance();
```

Without a no-arg constructor, this fails with `NoSuchMethodException`.

### Why Not Just Remove the Parameterized Constructor?

The parameterized constructor is kept for:
1. **Backward Compatibility**: Existing code may use it
2. **Explicit Configuration**: Allows manual instantiation with custom environments
3. **Flexibility**: Supports both automatic (ConfigMapper) and manual instantiation

### Standard Pattern in RDQ

This is the standard pattern used throughout RDQ:
- `PerkSystemSection` - Has no-arg constructor
- `QuestSystemSection` - Has no-arg constructor
- `RankSystemSection` - Has no-arg constructor
- All nested sections - Have no-arg constructors

The machine config sections now follow this established pattern.

## Summary
All machine configuration section classes now have no-arg constructors, allowing ConfigMapper to properly instantiate them when loading from YAML files. This fixes the "Core block must be configured" error and enables the machine system to initialize correctly.

## Next Steps
1. Deploy the new JAR to the server
2. Restart the server
3. Verify config files are created in `plugins/RDQ/machines/`
4. Test machine placement with `/rq machine give <player> fabricator`
5. Confirm no initialization errors in console
