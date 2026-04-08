# Design Document

## Overview

This design addresses the critical bug where only 3 out of 17 configured perks are being loaded into the database. The root cause analysis reveals that the `loadSinglePerkConfig` method in `PerkSystemFactory` is skipping perks that don't have an identifier set in the configuration, but all perk files DO have identifiers. The issue appears to be in how the system validates and processes perk configurations during initialization.

Additionally, the translation file is missing keys for many perks. The design will ensure all perks have complete translation support following the established pattern used in other RDQ plugins.

## Architecture

### Current System Flow

```
PerkSystemFactory.initialize()
  ├─> loadConfigurations()
  │     ├─> loadSystemConfig()
  │     └─> loadPerkConfigs()
  │           └─> loadSinglePerkConfig() [for each .yml file]
  │                 ├─> Parse YAML to PerkSection
  │                 ├─> Validate identifier
  │                 └─> Store in perkSections map
  ├─> validateConfigurations()
  └─> createPerks()
        ├─> createOrUpdatePerk() [for each perkSection]
        ├─> updatePerkRequirements()
        └─> updatePerkUnlockRewards()
```

### Problem Areas Identified

1. **Identifier Validation Issue**: The `loadSinglePerkConfig` method checks if the identifier is null or empty and skips the perk if so, but logs suggest this shouldn't be happening for all perks
2. **Silent Failures**: Perks may be failing to load due to exceptions that are caught and logged but don't prevent the system from continuing
3. **Missing Translation Keys**: The translation file only has keys for 3 perks (speed_boost, night_vision, jump_boost) but uses inconsistent naming (speed_boost vs speed)

## Components and Interfaces

### 1. PerkSystemFactory Enhancement

**Responsibility**: Load all perk configurations reliably with detailed diagnostics

**Changes Required**:
- Add more detailed logging in `loadSinglePerkConfig` to track each step
- Add validation logging before skipping perks
- Add a summary report of loaded vs skipped perks
- Ensure exceptions don't silently fail perk loading

**Key Methods**:
```java
private void loadSinglePerkConfig(String fileName) {
    // Enhanced logging at each step
    // Better error handling
    // Validation reporting
}

private void logLoadingSummary() {
    // Report total files found
    // Report successful loads
    // Report skipped/failed loads with reasons
}
```

### 2. Translation File Structure

**Current Pattern Analysis**:
- RDQ ranks use: `rank.{tree}.{rank}.name` (e.g., `rank.warrior.recruit.name`)
- RDQ perks should use: `perk.{identifier}.name`, `perk.{identifier}.description`, `perk.{identifier}.effect`
- Some perks use different identifiers in translations vs config (e.g., `speed_boost` in translations but `speed` in config)
- Translation file already has correct structure but is missing many perk entries

**Standardized Pattern** (following rank system example):
```yaml
perk:
  {identifier}:
    name: "<gradient>Display Name</gradient>"
    description: "<gradient>Description text</gradient>"
    effect: "<gradient>Effect description</gradient>"
```

**All Perks from Configuration Files**:
1. combat_heal - MISSING
2. critical_strike - MISSING  
3. double_experience - MISSING
4. fire_resistance - EXISTS
5. fly - EXISTS (as "flight")
6. glow - EXISTS
7. haste - EXISTS
8. jump_boost - EXISTS
9. keep_experience - MISSING
10. keep_inventory - MISSING
11. night_vision - EXISTS
12. no_fall_damage - EXISTS
13. resistance - EXISTS
14. saturation - EXISTS
15. speed - EXISTS (as "speed_boost")
16. strength - EXISTS

**Issues to Fix**:
- Rename `speed_boost` to `speed` in translations
- Rename `flight` to `fly` in translations
- Add missing perks: combat_heal, critical_strike, double_experience, keep_experience, keep_inventory

## Data Models

### PerkSection Configuration

The YAML configuration structure that must be properly parsed:

```yaml
identifier: "perk_id"
perkType: "PASSIVE" | "EVENT_TRIGGERED" | "COOLDOWN_BASED"
category: "COMBAT" | "MOVEMENT" | "UTILITY" | "SURVIVAL" | "ECONOMY" | "SOCIAL" | "COSMETIC" | "SPECIAL"
enabled: true
displayOrder: 1

icon:
  type: "MATERIAL_NAME"
  displayNameKey: "perk.{identifier}.name"
  descriptionKey: "perk.{identifier}.description"
  enchanted: true

requirements:
  {requirement_id}:
    type: "REQUIREMENT_TYPE"
    # ... requirement config

unlockRewards:
  {reward_id}:
    type: "REWARD_TYPE"
    # ... reward config

effect:
  # Potion effect config OR special type config
```

### Database Entity

The `Perk` entity that should be created for each configuration:

```java
@Entity
class Perk {
    String identifier;
    PerkType perkType;
    PerkCategory category;
    boolean enabled;
    int displayOrder;
    IconSection icon;
    String configJson;
    List<PerkRequirement> requirements;
    List<PerkUnlockReward> unlockRewards;
}
```

## Error Handling

### Loading Errors

1. **File Not Found**: Log warning, continue with other perks
2. **Invalid YAML**: Log error with file name and parsing error, skip perk
3. **Missing Identifier**: Log error with file name, skip perk
4. **Invalid Enum Values**: Log error with details, skip perk
5. **Database Errors**: Log error, attempt to continue with other perks

### Translation Errors

1. **Missing Translation Key**: Fall back to identifier or default text
2. **Invalid Key Format**: Log warning, use identifier as fallback

## Testing Strategy

### Unit Testing Focus

1. **PerkSystemFactory Loading**:
   - Test loading all 17 perk configuration files
   - Test handling of invalid configurations
   - Test identifier extraction from filenames
   - Test perkSections map population

2. **Translation Key Resolution**:
   - Test all perk identifiers resolve to translation keys
   - Test fallback behavior for missing keys
   - Test consistency between config identifiers and translation keys

### Integration Testing Focus

1. **End-to-End Perk Loading**:
   - Start with empty database
   - Load all perk configurations
   - Verify 17 perks in database
   - Verify all perks have correct properties

2. **Translation Integration**:
   - Load perks with translation system active
   - Verify all perk names display correctly
   - Verify all perk descriptions display correctly

### Manual Testing Checklist

1. Delete all perks from database
2. Restart server
3. Check logs for perk loading messages
4. Query database to count perks (should be 17)
5. Open perk UI in-game
6. Verify all perks display with correct names and descriptions
7. Check for any "missing translation" messages

## Implementation Plan

### Phase 1: Diagnostic Enhancement

1. Add detailed logging to `loadSinglePerkConfig`
2. Add logging summary after loading completes
3. Run server and analyze logs to identify exact failure points

### Phase 2: Fix Loading Issues

1. Fix any identified issues in perk loading logic
2. Ensure all 17 perks load successfully
3. Verify database contains all perks

### Phase 3: Translation Completion

1. Identify all missing translation keys
2. Add missing keys following the established pattern
3. Ensure identifier consistency between configs and translations
4. Test translation resolution for all perks

### Phase 4: Verification

1. Run full test suite
2. Perform manual testing
3. Verify logs show successful loading of all perks
4. Verify UI displays all perks correctly

## Risk Analysis

### High Risk

- **Database Corruption**: If perk loading fails mid-process, could leave database in inconsistent state
  - Mitigation: Use transactions, implement rollback on failure

### Medium Risk

- **Translation Key Mismatch**: If identifiers don't match between config and translations, perks won't display correctly
  - Mitigation: Implement validation to check translation key existence during loading

### Low Risk

- **Performance Impact**: Loading 17 perks instead of 3 could impact startup time
  - Mitigation: Perk loading is already async, minimal impact expected

## Dependencies

- **JExTranslate**: Translation system for resolving perk names and descriptions
- **RPlatform**: Requirement and reward factories for parsing perk requirements and rewards
- **Database**: JPA repository for persisting perk entities

## Configuration Changes

### Translation File Updates Required

Add missing perk translation keys to `RDQ/rdq-premium/src/main/resources/translations/en_US.yml`:

```yaml
perk:
  # Fix naming inconsistency
  speed:  # Currently named speed_boost
    name: "<gradient:#3b82f6:#60a5fa>⚡ Speed</gradient>"
    description: "<gradient:#6b7280:#9ca3af>Increases your movement speed</gradient>"
    effect: "<gradient:#6b7280:#9ca3af>Speed II effect while active</gradient>"
  
  # Add missing perks
  fly:  # Currently named flight
    name: "<gradient:#a855f7:#c084fc>🕊 Flight</gradient>"
    description: "<gradient:#6b7280:#9ca3af>Fly like in creative mode</gradient>"
    effect: "<gradient:#6b7280:#9ca3af>Grants flight ability while active</gradient>"
  
  combat_heal:
    name: "<gradient:#ef4444:#f87171>💉 Combat Heal</gradient>"
    description: "<gradient:#6b7280:#9ca3af>Chance to heal when taking damage</gradient>"
    effect: "<gradient:#6b7280:#9ca3af>25% chance to heal 2 hearts when damaged</gradient>"
  
  # ... (continue for all missing perks)
```

## Monitoring and Logging

### Key Metrics to Log

1. Total perk configuration files found
2. Successfully loaded perks
3. Skipped perks with reasons
4. Failed perks with error details
5. Database persistence success/failure
6. Translation key resolution success/failure

### Log Levels

- **INFO**: Successful operations, summary statistics
- **WARNING**: Skipped perks, missing translations (with fallback)
- **ERROR**: Failed to load perk, database errors, critical failures
- **FINE/DEBUG**: Detailed step-by-step loading progress

## Success Criteria

1. All 17 perk configuration files successfully load into database
2. All perks have complete translation keys (name, description, effect where applicable)
3. Perk UI displays all perks with correct names and descriptions
4. Server logs show clear summary of perk loading with no errors
5. No regression in existing perk functionality
