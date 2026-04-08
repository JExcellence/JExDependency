# Implementation Plan

- [x] 1. Update TranslationLoader extraction filtering




  - [ ] 1.1 Modify extractUsingBukkitSaveResource to filter by supportedLocales
    - Check if supportedLocales is non-empty before filtering
    - Skip extraction for locales not in the supported list


    - Add debug logging for skipped locales
    - _Requirements: 1.1, 1.3, 3.1, 3.4_
  - [x] 1.2 Modify extractUsingJarScanning to filter by supportedLocales




    - Extract locale from filename before extraction
    - Skip extraction for locales not in the supported list


    - Add debug logging for skipped locales
    - _Requirements: 1.1, 1.3, 3.2, 3.4_

- [ ] 2. Update TranslationLoader loading filtering
  - [ ] 2.1 Update loadYamlFile to respect empty supportedLocales as auto-detect
    - Only filter when supportedLocales is non-empty
    - Always load default locale regardless of configuration
    - _Requirements: 1.2, 4.1, 4.2, 4.3_




  - [ ] 2.2 Update loadJsonFile to respect empty supportedLocales as auto-detect
    - Only filter when supportedLocales is non-empty
    - Always load default locale regardless of configuration
    - _Requirements: 1.2, 4.1, 4.2, 4.3_

- [ ] 3. Add helper method for locale extraction
  - [ ] 3.1 Create extractLocaleFromFilename utility method
    - Extract locale code from filename (e.g., "en_US.yml" -> "en_US")
    - Handle both .yml and .json extensions
    - _Requirements: 3.4_
