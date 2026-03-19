# Infrastructure System Fix - Design

## Overview

This design document outlines the technical approach for fixing the JExOneblock infrastructure system. The fix focuses on completing missing translations, improving error handling, ensuring proper service integration, and connecting the storage system.

## Architecture

The infrastructure system consists of several layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Command Layer                                 │
│  PInfrastructure → EInfrastructureAction → Views                │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│                    Service Layer                                 │
│  IInfrastructureService → InfrastructureServiceImpl             │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│                    Manager Layer                                 │
│  InfrastructureManager ← → IslandStorageManager                 │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│                    Data Layer                                    │
│  IslandInfrastructure ← → IslandStorage                         │
└─────────────────────────────────────────────────────────────────┘
```

## Components to Fix

### 1. Translation System

**Problem**: Missing translation keys cause command failures and poor user experience.

**Solution**: Add comprehensive translation keys for all infrastructure operations.

**Files to Update**:
- `JExOneblock/jexoneblock-common/src/main/resources/translations/en_US.yml`
- `JExOneblock/jexoneblock-common/src/main/resources/translations/de_DE.yml`

**Translation Keys to Add**:
```yaml
infrastructure:
  no_island: '<red>You need an island to use infrastructure commands!</red>'
  service_unavailable: '<red>Infrastructure system is currently unavailable. Please try again later.</red>'
  data_not_found: '<red>Infrastructure data not found. Creating new infrastructure...</red>'
  
  help:
    header: '<gold>═══ Infrastructure Commands ═══</gold>'
    main: '<yellow>/island infrastructure main</yellow> <gray>- Open infrastructure dashboard</gray>'
    stats: '<yellow>/island infrastructure stats</yellow> <gray>- View infrastructure statistics</gray>'
    energy: '<yellow>/island infrastructure energy</yellow> <gray>- Show energy information</gray>'
    storage: '<yellow>/island infrastructure storage</yellow> <gray>- Manage island storage</gray>'
    automation: '<yellow>/island infrastructure automation</yellow> <gray>- Configure automation</gray>'
    processors: '<yellow>/island infrastructure processors</yellow> <gray>- Manage processors</gray>'
    generators: '<yellow>/island infrastructure generators</yellow> <gray>- Manage generators</gray>'
    crafting: '<yellow>/island infrastructure crafting</yellow> <gray>- View crafting queue</gray>'
  
  energy:
    header: '<gold>═══ Energy Information ═══</gold>'
    current: '<gray>Current Energy: <yellow>%current%</yellow>/<green>%max%</green></gray>'
    generation: '<gray>Generation: <green>+%value%/tick</green></gray>'
    consumption: '<gray>Consumption: <red>-%value%/tick</red></gray>'
    net: '<gray>Net Change: %value%/tick</gray>'
```

### 2. Enhanced Error Handling

**Problem**: Commands fail silently or with unclear error messages.

**Solution**: Add comprehensive error handling with clear user feedback.

**Implementation**:
```java
// In PInfrastructure.java
private boolean validateInfrastructureAccess(Player player) {
    // Check if service is available
    if (plugin.getInfrastructureService() == null) {
        new I18n.Builder("infrastructure.service_unavailable", player)
            .includePrefix().build().sendMessage();
        return false;
    }
    
    // Check if player has island
    var islandId = plugin.getOneblockService().getPlayerIslandId(player);
    if (islandId == null) {
        new I18n.Builder("infrastructure.no_island", player)
            .includePrefix().build().sendMessage();
        return false;
    }
    
    return true;
}
```

### 3. Service Integration Improvements

**Problem**: Views may not properly integrate with the infrastructure service.

**Solution**: Ensure all views use the service layer consistently.

**Files to Update**:
- All infrastructure views in `view/infrastructure/` package
- Add service validation in view initialization

**Implementation Pattern**:
```java
// In infrastructure views
@Override
protected void onRender(@NotNull RenderContext render, @NotNull Player player) {
    var plugin = this.plugin.get(render);
    var infrastructureService = plugin.getInfrastructureService();
    
    if (infrastructureService == null) {
        // Show error and close view
        new I18n.Builder("infrastructure.service_unavailable", player)
            .includePrefix().build().sendMessage();
        render.close();
        return;
    }
    
    // Continue with normal rendering
    super.onRender(render, player);
}
```

### 4. Storage System Integration

**Problem**: Storage system is disconnected from infrastructure views.

**Solution**: Ensure storage manager syncs with infrastructure data.

**Implementation**:
```java
// Enhanced sync method in IslandStorageManager
public void syncWithInfrastructure(Long islandId, IslandInfrastructure infrastructure) {
    IslandStorage storage = storageCache.get(islandId);
    if (storage != null && infrastructure != null) {
        // Bidirectional sync
        infrastructure.getStoredItems().clear();
        infrastructure.getStoredItems().putAll(storage.getStoredItems());
        
        // Update infrastructure storage tier from storage manager
        infrastructure.setStorageTier(storage.getCurrentTier());
        
        // Update capacity information
        infrastructure.updateStorageCapacities(storage.getRarityCapacities());
    }
}
```

### 5. View Error Handling

**Problem**: Views may crash or show empty data when infrastructure is unavailable.

**Solution**: Add error handling to all infrastructure views.

**Implementation Pattern**:
```java
// In base infrastructure view class or each view
protected boolean validateInfrastructure(Context context) {
    var plugin = this.plugin.get(context);
    var infrastructure = this.infrastructure.get(context);
    
    if (plugin.getInfrastructureService() == null) {
        context.getPlayer().sendMessage("§cInfrastructure service unavailable");
        context.close();
        return false;
    }
    
    if (infrastructure == null) {
        context.getPlayer().sendMessage("§cInfrastructure data not found");
        context.close();
        return false;
    }
    
    return true;
}
```

## Implementation Plan

### Phase 1: Translation Keys
1. Add all missing translation keys to English and German files
2. Test command help and error messages
3. Verify all infrastructure subcommands show proper messages

### Phase 2: Error Handling
1. Add validation methods to PInfrastructure command
2. Update all infrastructure views with error handling
3. Add logging for debugging infrastructure issues

### Phase 3: Service Integration
1. Ensure all views properly use infrastructure service
2. Add service availability checks
3. Test view opening and data display

### Phase 4: Storage Integration
1. Enhance storage sync methods
2. Test storage view with actual stored items
3. Verify storage operations work end-to-end

### Phase 5: Testing and Polish
1. Test all infrastructure subcommands
2. Test all infrastructure views
3. Test error conditions
4. Performance testing
5. Final bug fixes

## Testing Strategy

### Unit Tests
- Test translation key existence
- Test error handling methods
- Test service integration methods

### Integration Tests
- Test command execution with various scenarios
- Test view opening and data display
- Test storage system integration

### Manual Testing
- Test all infrastructure subcommands
- Test all infrastructure views
- Test error conditions (no island, service unavailable)
- Test storage operations
- Test with multiple players

## Success Metrics

1. **Command Success Rate**: 100% of infrastructure subcommands work without errors
2. **View Success Rate**: 100% of infrastructure views open and display data
3. **Error Handling**: All error conditions show appropriate messages
4. **Storage Integration**: Storage operations work seamlessly with infrastructure
5. **Performance**: Infrastructure operations complete within specified time limits

## Risk Mitigation

1. **Translation Conflicts**: Use unique key names to avoid conflicts
2. **Service Unavailability**: Always check service availability before operations
3. **Data Corruption**: Add data validation and recovery mechanisms
4. **Performance Impact**: Use caching and async operations where appropriate
5. **Backward Compatibility**: Ensure changes don't break existing functionality