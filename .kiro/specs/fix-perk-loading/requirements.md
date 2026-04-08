# Requirements Document

## Introduction

The perk system currently has a critical bug where only 3 out of 17 configured perks are being loaded and saved to the database. Additionally, many perks are missing their translation keys in the translation files. This feature aims to fix the perk loading mechanism and ensure all perks are properly saved with complete translation support.

## Glossary

- **Perk System**: The RDQ system that manages player perks (special abilities and bonuses)
- **PerkSystemFactory**: The factory class responsible for loading perk configurations from YAML files and creating database entities
- **Translation Keys**: Localization keys used to display perk names and descriptions in different languages
- **Perk Configuration**: YAML files in the `perks/` directory that define individual perks
- **Database Entity**: JPA entity objects that represent perks in the database

## Requirements

### Requirement 1: Complete Perk Loading

**User Story:** As a server administrator, I want all configured perks to be loaded into the database, so that players can access all available perks.

#### Acceptance Criteria

1. WHEN THE Perk System initializes, THE Perk System SHALL load all YAML configuration files from the perks directory
2. WHEN A perk configuration file has a valid identifier, THE Perk System SHALL create or update the corresponding database entity
3. WHEN THE perk loading process completes, THE Perk System SHALL log the total number of perks loaded
4. IF A perk configuration file is invalid or missing required fields, THEN THE Perk System SHALL log a warning and continue loading other perks
5. THE Perk System SHALL verify that all 17 perk configuration files result in 17 database entities

### Requirement 2: Complete Translation Keys

**User Story:** As a player, I want to see properly translated names and descriptions for all perks, so that I understand what each perk does.

#### Acceptance Criteria

1. THE Translation File SHALL contain name keys for all configured perks
2. THE Translation File SHALL contain description keys for all configured perks
3. WHEN A perk references a translation key, THE Translation System SHALL resolve the key to the appropriate translated text
4. THE Translation File SHALL follow the naming pattern `perk.{identifier}.name` for perk names
5. THE Translation File SHALL follow the naming pattern `perk.{identifier}.description` for perk descriptions
6. THE Translation File SHALL include effect descriptions where applicable using the pattern `perk.{identifier}.effect`

### Requirement 3: Consistent Translation Pattern

**User Story:** As a developer, I want translation keys to follow a consistent pattern across all plugins, so that the codebase is maintainable.

#### Acceptance Criteria

1. THE Translation Keys SHALL follow the same pattern used in other RDQ plugins
2. WHEN EXAMINING other plugin translation files, THE System SHALL identify the standard translation key pattern
3. THE Perk translation keys SHALL use the same formatting and structure as rank and reward translation keys
4. THE Translation File SHALL organize perk keys in a logical hierarchy matching the perk category structure

### Requirement 4: Perk Loading Diagnostics

**User Story:** As a server administrator, I want detailed logging during perk initialization, so that I can diagnose any loading issues.

#### Acceptance Criteria

1. WHEN THE Perk System starts loading, THE System SHALL log the start of the initialization process
2. WHEN EACH perk configuration is loaded, THE System SHALL log the perk identifier
3. IF A perk fails to load, THEN THE System SHALL log the specific error with the perk identifier and reason
4. WHEN THE loading process completes, THE System SHALL log a summary including total perks loaded and any failures
5. THE System SHALL log at INFO level for successful operations and WARNING level for failures
