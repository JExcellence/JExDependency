# Implementation Plan

- [ ] 1. Create @FlatKeys annotation
  - Create new annotation class in `de.jexcellence.configmapper.annotations`
  - Add RetentionPolicy.RUNTIME and Target ElementType.FIELD
  - Add comprehensive Javadoc with usage examples
  - _Requirements: 2.1, 2.3_

- [ ] 2. Fix MapSerializer to respect @FlatKeys
  - [ ] 2.1 Add annotation detection in MapSerializer
    - Check for @FlatKeys presence on the field being serialized
    - Pass flat key mode flag to serialization logic
    - _Requirements: 1.1, 2.1_
  
  - [ ] 2.2 Implement literal key serialization
    - Quote keys containing dots or special YAML characters
    - Preserve exact key format without path interpretation
    - _Requirements: 1.1, 1.4_
  
  - [ ] 2.3 Ensure backward compatibility for non-annotated fields
    - Maintain existing nested path behavior as default
    - Only apply flat key logic when annotation is present
    - _Requirements: 2.2_

- [ ] 3. Fix value replacement on save
  - [ ] 3.1 Clear existing section before writing
    - Remove all existing keys in the section before serialization
    - Prevent accumulation of duplicate entries
    - _Requirements: 1.2, 3.1_
  
  - [ ] 3.2 Implement complete map replacement
    - Replace entire map contents, not merge/append
    - Handle nested structures correctly during replacement
    - _Requirements: 1.2, 3.2_

- [ ] 4. Fix default value handling
  - [ ] 4.1 Read field values directly, not through getters
    - Use reflection to get actual field value
    - Bypass getter methods that return defaults for null fields
    - _Requirements: 1.3, 4.1_
  
  - [ ] 4.2 Skip serialization of null fields
    - Do not write entries for fields that are null
    - Let getters provide runtime defaults without persisting them
    - _Requirements: 4.1, 4.2_
  
  - [ ] 4.3 Add @DefaultValue annotation support (optional)
    - Allow explicit default value specification
    - Skip serialization if value equals annotated default
    - _Requirements: 4.2, 4.3_

- [ ] 5. Add configuration migration utility
  - [ ] 5.1 Create ConfigMigrator class
    - Utility to detect and fix corrupted nested structures
    - Flatten incorrectly nested maps back to flat keys
    - _Requirements: 3.2_
  
  - [ ] 5.2 Add automatic migration on load
    - Detect corrupted structure patterns
    - Log warning and attempt automatic recovery
    - _Requirements: 3.2_

- [ ]* 6. Write unit tests
  - [ ]* 6.1 Test @FlatKeys serialization
    - Verify dotted keys are preserved as literals
    - Verify no nested structure is created
    - _Requirements: 1.1, 1.4_
  
  - [ ]* 6.2 Test idempotency
    - Load and save 100 times
    - Verify file size remains constant
    - _Requirements: 3.1, 3.2_
  
  - [ ]* 6.3 Test default value handling
    - Verify null fields are not serialized
    - Verify getter defaults work at runtime
    - _Requirements: 4.1, 4.2_
