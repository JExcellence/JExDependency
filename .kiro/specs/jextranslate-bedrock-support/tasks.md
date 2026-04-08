# Implementation Plan

- [x] 1. Create Bedrock support package and configuration enums







  - [x] 1.1 Create `de.jexcellence.jextranslate.bedrock` package with package-info.java

    - Create package documentation describing Bedrock Edition support utilities
    - _Requirements: 1.1, 5.1_

  - [x] 1.2 Create `HexColorFallback` enum in bedrock package

    - Define STRIP, NEAREST_LEGACY, and GRAYSCALE options
    - Add JavaDoc explaining each option's behavior
    - _Requirements: 5.3_

  - [x] 1.3 Create `BedrockFormatMode` enum in bedrock package

    - Define CONSERVATIVE and MODERN options
    - Add JavaDoc explaining compatibility trade-offs
    - _Requirements: 5.1_

- [-] 2. Implement BedrockConverter utility class


  - [x] 2.1 Create `BedrockConverter` class with static conversion methods

    - Implement `toLegacyString(Component)` method using LegacyComponentSerializer
    - Implement `fromMiniMessage(String)` method to parse and convert MiniMessage
    - Implement `stripUnsupportedFormatting(Component)` to remove click/hover events
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 2.2 Implement hex color to legacy color conversion

    - Create `hexToNearestLegacy(String)` method with color distance algorithm
    - Define static mapping of legacy colors with their RGB values
    - Support both `#RRGGBB` and `&#RRGGBB` hex formats
    - _Requirements: 3.4_

  - [x] 2.3 Implement gradient handling for conservative mode

    - Extract dominant color from gradient for single-color fallback
    - Preserve gradient in modern mode by keeping hex colors
    - _Requirements: 2.2_
  - [ ] 2.4 Write unit tests for BedrockConverter
    - Test Component to legacy string conversion
    - Test MiniMessage to legacy conversion
    - Test hex color mapping accuracy
    - Test gradient handling in both modes
    - _Requirements: 2.1, 2.2, 3.4_

- [-] 3. Implement BedrockDetectionCache

  - [x] 3.1 Create `BedrockDetectionCache` class with caching logic


    - Use ConcurrentHashMap for thread-safe UUID to Boolean mapping
    - Implement `isBedrockPlayer(Player)` with cache lookup
    - Implement lazy GeyserService lookup from ServiceRegistry
    - _Requirements: 4.1, 1.2_

  - [x] 3.2 Implement cache invalidation and event handling

    - Implement `invalidate(UUID)` and `invalidateAll()` methods
    - Add PlayerQuitEvent listener to clean up cache entries
    - Register listener with Bukkit event system
    - _Requirements: 4.2, 4.3_
  - [ ] 3.3 Write unit tests for BedrockDetectionCache
    - Test cache hit and miss scenarios
    - Test invalidation behavior
    - Test graceful handling when GeyserService unavailable
    - _Requirements: 4.1, 4.2, 1.3_

- [-] 4. Update R18nConfiguration with Bedrock options

  - [x] 4.1 Add Bedrock configuration fields to R18nConfiguration record


    - Add `bedrockSupportEnabled` boolean field (default: true)
    - Add `hexColorFallback` field (default: NEAREST_LEGACY)
    - Add `bedrockFormatMode` field (default: CONSERVATIVE)
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 4.2 Update R18nConfiguration.Builder with Bedrock methods

    - Add `bedrockSupportEnabled(boolean)` builder method
    - Add `hexColorFallback(HexColorFallback)` builder method
    - Add `bedrockFormatMode(BedrockFormatMode)` builder method
    - _Requirements: 5.1, 5.3_
  - [ ] 4.3 Update R18nConfiguration tests
    - Test new Bedrock configuration options
    - Test default values are applied correctly
    - _Requirements: 5.1, 5.2_

- [-] 5. Enhance MessageBuilder with Bedrock methods


  - [x] 5.1 Add Bedrock-specific string methods to MessageBuilder

    - Implement `toBedrockString(Player)` returning legacy-formatted string
    - Implement `toBedrockStrings(Player)` returning list of legacy strings
    - Implement `isBedrockPlayer(Player)` using BedrockDetectionCache
    - _Requirements: 3.1, 3.2, 3.3_


  - [ ] 5.2 Wire MessageBuilder to BedrockConverter
    - Use BedrockConverter for format conversion in new methods
    - Respect R18nConfiguration settings for format mode
    - Handle null player gracefully (return Java Edition format)
    - _Requirements: 3.1, 3.3, 3.4_

- [-] 6. Update VersionedMessageSender for automatic Bedrock handling


  - [x] 6.1 Integrate BedrockDetectionCache into VersionedMessageSender

    - Add BedrockDetectionCache field with lazy initialization
    - Modify constructor to accept optional cache instance
    - _Requirements: 2.4, 4.1_


  - [ ] 6.2 Update sendMessage methods for automatic Bedrock conversion
    - Check if player is Bedrock before sending
    - Convert Component to legacy string for Bedrock players
    - Preserve existing Java Edition behavior


    - _Requirements: 2.1, 2.4_
  - [x] 6.3 Add configuration check for Bedrock support toggle

    - Skip Bedrock detection if `bedrockSupportEnabled` is false
    - Log info message when Bedrock support is disabled
    - _Requirements: 5.2_

- [-] 7. Initialize Bedrock support in R18nManager


  - [x] 7.1 Create and register BedrockDetectionCache in R18nManager

    - Initialize cache during R18nManager.build()
    - Register PlayerQuitEvent listener with plugin
    - Pass cache to VersionedMessageSender
    - _Requirements: 1.1, 4.2_
  - [x] 7.2 Add shutdown cleanup for Bedrock components


    - Unregister event listeners on shutdown
    - Clear cache on shutdown
    - _Requirements: 4.3_
