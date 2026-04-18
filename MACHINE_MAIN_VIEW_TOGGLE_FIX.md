# Machine Main View - Toggle Button & Structure Validation Fix

## Problem
The MachineMainView had critical issues:
1. Toggle button didn't work - no response when clicked
2. No structure validation before activating machine
3. No validation for recipe or fuel requirements
4. Missing async/thread safety for callbacks
5. Missing translation keys for error messages

## Solution Applied

### 1. Added Required State Dependencies

```java
private final State<MultiBlockStructure> structure = initialState("structure");
private final State<StructureValidator> validator = initialState("validator");
private final State<Plugin> plugin = initialState("plugin");
```

These states must be passed when opening the view:
```java
Map.of(
    "machineService", machineService,
    "machine", machine,
    "structure", structure,
    "validator", validator,
    "plugin", plugin
)
```

### 2. Implemented Structure Validation

**Before activating machine:**
- Validates multi-block structure is intact
- Checks if recipe is configured
- Checks if fuel is available
- Shows specific error messages for each failure

```java
if (newState) {
    // Validate structure
    final StructureValidator.ValidationResult validationResult = 
        validator.get(click).validate(
            machine.get(click).getLocation(),
            structure.get(click)
        );
    
    if (validationResult.isFailure()) {
        // Show error message
        return;
    }
    
    // Check recipe
    if (machine.get(click).getRecipeData() == null) {
        // Show error message
        return;
    }
    
    // Check fuel
    if (machine.get(click).getFuelLevel() <= 0) {
        // Show error message
        return;
    }
}
```

### 3. Fixed Async Callback Threading

**All callbacks now use Bukkit scheduler:**
```java
machineService.get(click).toggleMachine(machine.get(click).getId(), newState)
    .thenAccept(success -> {
        Bukkit.getScheduler().runTask(
            plugin.get(click),
            () -> {
                // Update UI and send messages on main thread
                if (success) {
                    machine.get(click).setState(newState ? EMachineState.ACTIVE : EMachineState.INACTIVE);
                    i18n("messages.state-" + (newState ? "on" : "off"), player)
                        .build()
                        .sendMessage();
                    click.update();
                }
            }
        );
    })
    .exceptionally(ex -> {
        Bukkit.getScheduler().runTask(
            plugin.get(click),
            () -> i18n("messages.toggle-error", player)
                .withPlaceholder("error", ex.getMessage())
                .build()
                .sendMessage()
        );
        return null;
    });
```

### 4. Added User Feedback

**Success messages:**
- "✅ Machine Activated!" when turned on
- "⏸ Machine Deactivated" when turned off

**Error messages:**
- "❌ Invalid Structure!" with specific error details
- "⚠ No Recipe Set!" when recipe not configured
- "❌ No Fuel!" when fuel level is zero
- "❌ Failed to toggle machine state" for general failures
- "❌ Error: {error}" for exceptions

## Translation Keys Added

Added to `en_US.yml` under `view.machine.main.messages`:

```yaml
messages:
  structure-invalid: "❌ Invalid Structure! %error%"
  no-recipe: "⚠ No Recipe Set! Configure a recipe first"
  no-fuel: "❌ No Fuel! Add fuel to the machine"
  state-on: "✅ Machine Activated!"
  state-off: "⏸ Machine Deactivated"
  toggle-failed: "❌ Failed to toggle machine state"
  toggle-error: "❌ Error: %error%"
```

## Validation Flow

### Turning Machine ON:
1. ✅ Validate structure is intact
2. ✅ Check recipe is configured
3. ✅ Check fuel is available
4. ✅ Call `toggleMachine()` service method
5. ✅ Update machine state
6. ✅ Refresh GUI
7. ✅ Send success message

### Turning Machine OFF:
1. ✅ Call `toggleMachine()` service method
2. ✅ Update machine state
3. ✅ Refresh GUI
4. ✅ Send success message

## Required View Parameters

When opening MachineMainView, you MUST provide these states:

```java
viewFrame.open(MachineMainView.class, player, Map.of(
    "machineService", machineService,
    "machine", machine,
    "structure", structure,          // NEW - Required for validation
    "validator", validator,            // NEW - Required for validation
    "plugin", plugin                   // NEW - Required for scheduler
));
```

## Integration Points

### MachineInteractListener
Should pass structure and validator when opening view:

```java
@EventHandler
public void onMachineInteract(PlayerInteractEvent event) {
    // ... get machine ...
    
    MultiBlockStructure structure = registry.getStructure(machine.getMachineType());
    StructureValidator validator = new StructureValidator();
    
    viewFrame.open(MachineMainView.class, player, Map.of(
        "machineService", machineService,
        "machine", machine,
        "structure", structure,
        "validator", validator,
        "plugin", plugin
    ));
}
```

## Testing Checklist

- [ ] Toggle button responds to clicks
- [ ] Structure validation prevents activation with broken structure
- [ ] Recipe validation prevents activation without recipe
- [ ] Fuel validation prevents activation without fuel
- [ ] Success messages appear when toggling
- [ ] Error messages show specific problems
- [ ] GUI updates after toggle
- [ ] Machine state persists after toggle
- [ ] No console errors from async callbacks
- [ ] Multiple players can toggle simultaneously

## Files Modified

1. `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/view/MachineMainView.java`
   - Added structure, validator, and plugin states
   - Implemented structure validation before activation
   - Added recipe and fuel validation
   - Fixed async callback threading with Bukkit scheduler
   - Added user feedback messages

2. `RDQ/rdq-common/src/main/resources/translations/en_US.yml`
   - Added `view.machine.main.messages` section
   - All error and success message keys

## Related Files

- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/structure/StructureValidator.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/structure/MultiBlockStructure.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/IMachineService.java`
- `RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/machine/listener/MachineInteractListener.java` (needs update)

## Next Steps

1. Update MachineInteractListener to pass structure and validator states
2. Test structure validation with broken structures
3. Test recipe validation without configured recipe
4. Test fuel validation with empty fuel
5. Verify async callbacks don't cause threading issues
