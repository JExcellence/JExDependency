# Requirements Document

## Introduction

This feature adds configuration options to JExTranslate that control which translation files are extracted and generated in the translations folder. Currently, the system extracts all available translation files regardless of which locales are actually supported by the plugin. This enhancement allows developers to specify which locales should be extracted, reducing clutter and improving clarity about which languages are actually supported.

## Glossary

- **JExTranslate**: The translation management library that handles internationalization (i18n) for Minecraft plugins
- **TranslationLoader**: The component responsible for loading, extracting, and managing translation files
- **R18nConfiguration**: The configuration class that holds all translation system settings
- **Locale**: A language/region identifier (e.g., `en_US`, `de_DE`) representing a specific translation
- **Supported Locales**: The set of locales that a plugin explicitly supports and wants to extract
- **Translation Directory**: The folder where translation files are stored (typically `translations/`)

## Requirements

### Requirement 1: Configurable Locale Extraction

**User Story:** As a plugin developer, I want to specify which translation files should be extracted to the translations folder, so that only the locales I actually support are present in the plugin's data folder.

#### Acceptance Criteria

1. WHEN supported locales are configured via `R18nConfiguration`, THE TranslationLoader SHALL extract only translation files matching those locales.
2. WHEN no supported locales are explicitly configured, THE TranslationLoader SHALL extract all available translation files (backward compatible behavior).
3. WHEN a locale file exists in the JAR but is not in the supported locales list, THE TranslationLoader SHALL skip extraction of that file.
4. THE TranslationLoader SHALL log which locale files are being extracted when debug mode is enabled.

### Requirement 2: Builder API for Locale Configuration

**User Story:** As a plugin developer, I want to configure supported locales through the R18nManager builder API, so that I can easily specify which languages my plugin supports during initialization.

#### Acceptance Criteria

1. THE R18nManager.Builder SHALL provide a `supportedLocales(String... locales)` method to specify which locales to extract and support.
2. THE R18nManager.Builder SHALL provide a `supportedLocales(Set<String> locales)` method to specify locales using a Set.
3. WHEN `supportedLocales` is called, THE R18nConfiguration SHALL store the specified locales for use during extraction.
4. THE default locale SHALL automatically be included in the supported locales set if not explicitly specified.

### Requirement 3: Extraction Filtering Logic

**User Story:** As a plugin developer, I want the extraction process to respect my locale configuration, so that unnecessary translation files are not created in my plugin's data folder.

#### Acceptance Criteria

1. WHEN extracting via Bukkit saveResource, THE TranslationLoader SHALL check if each locale is in the supported locales list before extraction.
2. WHEN extracting via JAR scanning, THE TranslationLoader SHALL check if each locale is in the supported locales list before extraction.
3. WHEN a translation file already exists in the data folder, THE TranslationLoader SHALL not overwrite it regardless of locale configuration.
4. THE TranslationLoader SHALL support both `.yml` and `.json` file extensions when filtering by locale.

### Requirement 4: Loading Behavior Alignment

**User Story:** As a plugin developer, I want the loading behavior to match the extraction behavior, so that only configured locales are loaded into memory.

#### Acceptance Criteria

1. WHEN loading translation files, THE TranslationLoader SHALL skip files for locales not in the supported locales list (if configured).
2. WHEN supported locales are empty or not configured, THE TranslationLoader SHALL load all available translation files.
3. THE TranslationLoader SHALL always load the default locale regardless of the supported locales configuration.
