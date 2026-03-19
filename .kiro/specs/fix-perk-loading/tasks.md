# Implementation Plan

- [x] 1. Add diagnostic logging to perk loading system





  - Add detailed logging at each step of perk configuration loading
  - Log file discovery, YAML parsing, identifier validation, and entity creation
  - Add summary logging showing total files found, loaded, and skipped
  - _Requirements: 1.3, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 2. Investigate and fix perk loading failures




  - [x] 2.1 Run server with enhanced logging to identify failure points


    - Start server and capture full perk loading logs
    - Identify which perks are failing and why
    - Document specific error messages and stack traces
    - _Requirements: 1.1, 1.2, 4.1, 4.2_
  
  - [x] 2.2 Fix identifier validation and configuration parsing issues


    - Review and fix the identifier validation logic in `loadSinglePerkConfig`
    - Ensure YAML parsing correctly extracts all perk properties
    - Fix any enum conversion issues (PerkType, PerkCategory)
    - Handle edge cases in configuration parsing
    - _Requirements: 1.1, 1.2, 1.4_
  
  - [x] 2.3 Verify all 17 perks load successfully


    - Restart server with fixes applied
    - Verify logs show 17 perks loaded
    - Query database to confirm 17 perk entities exist
    - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [x] 3. Add missing perk translation keys





  - [x] 3.1 Identify all missing and misnamed translation keys


    - Compare perk configuration identifiers with translation file keys
    - Identify naming inconsistencies: `speed_boost` should be `speed`, `flight` should be `fly`
    - Create list of 5 missing perks: combat_heal, critical_strike, double_experience, keep_experience, keep_inventory
    - _Requirements: 2.1, 2.2, 2.4, 3.1, 3.2_
  
  - [x] 3.2 Fix misnamed translation keys


    - Rename `perk.speed_boost` to `perk.speed` in translation file
    - Rename `perk.flight` to `perk.fly` in translation file
    - Ensure all references use the correct identifier
    - _Requirements: 2.1, 2.2, 2.4, 3.1, 3.2, 3.4_
  
  - [x] 3.3 Add translation keys for missing perks


    - Add `perk.combat_heal` with name, description, and effect keys
    - Add `perk.critical_strike` with name, description, and effect keys
    - Add `perk.double_experience` with name, description, and effect keys
    - Add `perk.keep_experience` with name, description, and effect keys
    - Add `perk.keep_inventory` with name, description, and effect keys
    - Follow the established pattern with gradients and emojis
    - Use appropriate colors matching perk categories (combat=red, utility=green, etc.)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4_

- [ ] 4. Verify translation resolution
  - Test that all perk names display correctly in UI
  - Test that all perk descriptions display correctly
  - Verify no "missing translation" warnings in logs
  - Check that translation keys resolve properly for all 17 perks
  - _Requirements: 2.3, 2.4, 2.5, 2.6_

- [ ] 5. Final verification and testing
  - [ ] 5.1 Perform end-to-end testing
    - Delete all perks from database
    - Restart server
    - Verify logs show successful loading of all 17 perks
    - Verify database contains 17 perk entities with correct properties
    - _Requirements: 1.1, 1.2, 1.3, 1.5_
  
  - [ ] 5.2 Test perk UI display
    - Open perk overview UI in-game
    - Verify all 17 perks are visible
    - Verify all perks display correct names and descriptions
    - Verify perk icons display correctly
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  
  - [ ] 5.3 Verify no regressions
    - Test existing perk functionality (activation, deactivation)
    - Test perk requirements checking
    - Test perk unlock rewards
    - Ensure no existing functionality is broken
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
