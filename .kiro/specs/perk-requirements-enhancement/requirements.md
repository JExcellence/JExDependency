# Requirements Document

## Introduction

This spec addresses two critical issues with the perk system's requirement display and currency detection:
1. Currency requirements showing 0% progress despite player having sufficient balance
2. Perk requirement cards lacking detailed information compared to rank requirement cards

## Glossary

- **CurrencyRequirement**: Requirement type that checks if a player has sufficient currency balance
- **PerkDetailView**: UI view showing detailed information about a specific perk
- **RequirementCard**: Visual representation of a requirement in the UI
- **JExEconomy**: Economy plugin providing multi-currency support
- **I18n**: Internationalization system for translating text
- **MiniMessage**: Kyori Adventure's text formatting system

## Requirements

### Requirement 1: Fix Currency Detection

**User Story:** As a player, I want to see my actual currency balance reflected in perk requirements, so that I know if I can unlock a perk

#### Acceptance Criteria

1. WHEN THE CurrencyRequirement SHALL check player balance, THE System SHALL correctly detect and retrieve currency from JExEconomy
2. WHEN THE player has 21000 coins AND THE requirement needs 100 coins, THE System SHALL display progress as 100% (or "21000/100")
3. WHEN THE currency identifier is "coins", THE System SHALL match it against JExEconomy currency identifiers case-insensitively
4. IF THE JExEconomy is not available, THEN THE System SHALL fall back to Vault economy
5. WHEN THE currency cannot be found, THE System SHALL log a warning with the currency identifier and available currencies

### Requirement 2: Add Currency Display Name Support

**User Story:** As a player, I want to see user-friendly currency names in the UI, so that I understand what currency is required

#### Acceptance Criteria

1. THE CurrencyRequirement SHALL provide a method to get the display name of the currency
2. WHEN THE currency is from JExEconomy, THE System SHALL use the currency's display name from JExEconomy
3. WHEN THE currency is from Vault, THE System SHALL use a configurable display name (default: "Money")
4. THE display name SHALL be used in requirement cards and progress displays

### Requirement 3: Enhance Perk Requirement Cards

**User Story:** As a player, I want detailed requirement information in perk cards similar to rank requirements, so that I understand what I need to unlock a perk

#### Acceptance Criteria

1. THE perk requirement card SHALL display task previews showing specific requirements
2. WHEN THE requirement is a currency requirement, THE card SHALL show "Need X coins" or "Have X/Y coins"
3. WHEN THE requirement is met, THE card SHALL show a green checkmark (✓) prefix
4. WHEN THE requirement is not met, THE card SHALL show a gray circle (○) prefix
5. THE card SHALL display a mini progress bar showing completion percentage
6. THE card SHALL use I18n for all translatable text
7. THE card SHALL use MiniMessage for text formatting and colors

### Requirement 4: Add Requirement-Specific Rendering

**User Story:** As a developer, I want type-specific rendering for different requirement types, so that each requirement displays relevant information

#### Acceptance Criteria

1. THE System SHALL provide a method to generate task previews for each requirement type
2. WHEN THE requirement is CURRENCY type, THE System SHALL show currency name and amount
3. WHEN THE requirement is ITEM type, THE System SHALL show item name and quantity
4. WHEN THE requirement is EXPERIENCE_LEVEL type, THE System SHALL show required level
5. WHEN THE requirement is PLAYTIME type, THE System SHALL show required time in human-readable format
6. THE task preview SHALL include current progress and target values

### Requirement 5: Add I18n Translation Keys

**User Story:** As a server administrator, I want translatable requirement descriptions, so that I can provide localized content to players

#### Acceptance Criteria

1. THE System SHALL add translation keys for currency requirement descriptions
2. THE translation key SHALL follow the pattern "requirement.currency.{currency_id}"
3. THE System SHALL provide fallback text if translation key is missing
4. THE translation SHALL support placeholders for amount and currency name
5. THE en_US translation file SHALL include example translations for common currencies

### Requirement 6: Improve Error Logging

**User Story:** As a developer, I want detailed error logging for currency detection failures, so that I can diagnose configuration issues

#### Acceptance Criteria

1. WHEN THE currency cannot be found, THE System SHALL log the requested currency identifier
2. THE System SHALL log all available currency identifiers from JExEconomy
3. THE System SHALL log whether JExEconomy or Vault is being used
4. THE log level SHALL be WARNING for missing currencies
5. THE log message SHALL include actionable information for fixing the issue
