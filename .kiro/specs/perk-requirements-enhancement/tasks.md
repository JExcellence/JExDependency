# Implementation Plan

- [x] 1. Fix CurrencyRequirement currency detection






  - [x] 1.1 Add case-insensitive currency lookup

    - Modify `findJExCurrency` method to use `equalsIgnoreCase` for identifier matching
    - Add currency caching to avoid repeated reflection calls
    - _Requirements: 1.1, 1.3_
  

  - [x] 1.2 Add detailed logging for currency detection failures

    - Log requested currency identifier when not found
    - Log all available currency identifiers from JExEconomy
    - Log which economy system is being used (JExEconomy vs Vault)
    - _Requirements: 1.5, 6.1, 6.2, 6.3_
  
  - [x] 1.3 Add currency display name support


    - Create `getCurrencyDisplayName()` method
    - Retrieve display name from JExEconomy currency object
    - Provide fallback to identifier if display name not available
    - _Requirements: 2.1, 2.2, 2.3_
  

  - [x] 1.4 Add detailed description method

    - Create `getDetailedDescription(Player)` method
    - Format as "Have X/Y Currency" or "Need Y Currency"
    - Use currency display name instead of identifier
    - _Requirements: 2.4, 4.2_

- [x] 2. Create PerkRequirementCardRenderer





  - [x] 2.1 Create TaskPreview record


    - Define record with name and completed fields
    - _Requirements: 4.1_
  
  - [x] 2.2 Implement task preview generation for CURRENCY type


    - Create method to generate currency task previews
    - Show "Need X coins" when not met
    - Show "Have X/Y coins" when partially or fully met
    - Use currency display name
    - _Requirements: 3.2, 4.2_
  
  - [x] 2.3 Implement task preview generation for other requirement types


    - Add ITEM type: "Need Xx item_name"
    - Add EXPERIENCE_LEVEL type: "Reach level X"
    - Add PLAYTIME type: "Play for X hours"
    - Add PERMISSION type: "Requires permission: X"
    - _Requirements: 4.3, 4.4, 4.5, 4.6_
  
  - [x] 2.4 Create enhanced requirement card rendering method


    - Build card with task previews (max 3 shown)
    - Add "...and X more" if more than 3 tasks
    - Add mini progress bar
    - Use green checkmark (✓) for completed tasks
    - Use gray circle (○) for incomplete tasks
    - _Requirements: 3.1, 3.3, 3.4, 3.5_

- [x] 3. Add I18n translation support






  - [x] 3.1 Add translation keys to en_US.yml

    - Add requirement.currency.{currency_id} keys
    - Add requirement.task.currency.need and have templates
    - Add requirement.task templates for other types
    - _Requirements: 5.1, 5.2, 5.5_
  

  - [x] 3.2 Implement I18n usage in requirement cards

    - Use I18n.Builder for translatable text
    - Support placeholders for amounts and names
    - Provide fallback text if translation missing
    - _Requirements: 3.6, 5.3, 5.4_

- [x] 4. Update PerkDetailView to use enhanced cards






  - [x] 4.1 Replace createRequirementCard method

    - Use PerkRequirementCardRenderer instead of inline rendering
    - Pass requirement service to renderer
    - _Requirements: 3.1, 3.7_
  

  - [x] 4.2 Update requirement card lore building

    - Use task previews from renderer
    - Add progress bar using RequirementProgressRenderer
    - Use MiniMessage for all text formatting
    - _Requirements: 3.5, 3.6, 3.7_

- [ ] 5. Testing and validation
  - [ ] 5.1 Test currency detection with various identifiers
    - Test with "coins", "Coins", "COINS"
    - Verify case-insensitive matching works
    - Verify logging shows available currencies
    - _Requirements: 1.1, 1.3, 1.5_
  
  - [ ] 5.2 Test requirement card rendering
    - Verify task previews display correctly
    - Verify progress bars show accurate percentages
    - Verify checkmarks and circles display properly
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [ ] 5.3 Test with actual perk unlock
    - Verify player with 21000 coins sees correct progress
    - Verify unlock button becomes available when requirements met
    - Verify currency is consumed correctly on unlock
    - _Requirements: 1.2, 3.2_

## Success Criteria
- Currency requirements show accurate balance and progress
- Perk requirement cards display detailed task information
- All text uses I18n for localization
- Logging provides actionable information for debugging
- UI matches rank requirements in detail and polish

## Notes
- Reuse RequirementProgressRenderer from rank system for progress bars
- Follow same card styling as RequirementCardRenderer in rank system
- Ensure all reflection calls are wrapped in try-catch with proper logging
- Cache currency lookups to improve performance
