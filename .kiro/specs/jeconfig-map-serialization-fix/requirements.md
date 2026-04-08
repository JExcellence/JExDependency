# Requirements Document

## Introduction

This document specifies requirements for fixing a critical bug in JEConfig (ConfigMapper) where Map fields with dotted keys are incorrectly serialized, causing configuration file corruption through duplicate entries on every server restart.

## Glossary

- **ConfigMapper**: The JEConfig library component responsible for serializing/deserializing Java objects to/from YAML configuration files
- **AConfigSection**: Abstract base class for configuration sections that ConfigMapper processes
- **Dotted Key**: A map key containing period characters (e.g., "jexhome.limit.basic")
- **Nested Path**: YAML structure where dots create hierarchical nesting (e.g., `jexhome: limit: basic:`)
- **Flat Key**: A map key treated as a literal string without path interpretation

## Requirements

### Requirement 1

**User Story:** As a plugin developer, I want Map fields with dotted keys to be serialized correctly, so that my configuration files don't grow infinitely on each restart.

#### Acceptance Criteria

1. WHEN ConfigMapper serializes a Map<String, Integer> field with keys containing dots, THE ConfigMapper SHALL treat the keys as literal strings and NOT interpret dots as nested path separators.
2. WHEN ConfigMapper saves a configuration file, THE ConfigMapper SHALL replace existing map values completely instead of appending to them.
3. WHEN a Map field getter returns a default value because the field is null, THE ConfigMapper SHALL NOT serialize the default value unless explicitly configured to do so.
4. THE ConfigMapper SHALL preserve the exact key format when reading and writing Map entries (e.g., "jexhome.limit.basic" remains "jexhome.limit.basic").

### Requirement 2

**User Story:** As a plugin developer, I want to control whether dotted keys are treated as paths or literals, so that I can use either behavior based on my needs.

#### Acceptance Criteria

1. WHERE a @FlatKeys annotation is present on a Map field, THE ConfigMapper SHALL treat all keys in that map as literal strings regardless of dot characters.
2. WHERE no @FlatKeys annotation is present, THE ConfigMapper SHALL maintain backward compatibility with existing path-based behavior.
3. THE ConfigMapper SHALL provide clear documentation for the @FlatKeys annotation behavior.

### Requirement 3

**User Story:** As a plugin developer, I want configuration saves to be idempotent, so that saving the same configuration multiple times produces identical files.

#### Acceptance Criteria

1. WHEN ConfigMapper saves a configuration that has not changed, THE ConfigMapper SHALL produce an identical file to the previous save.
2. WHEN ConfigMapper loads and immediately saves a configuration without modifications, THE ConfigMapper SHALL NOT add, remove, or duplicate any entries.
3. THE ConfigMapper SHALL handle null fields consistently without creating phantom entries.

### Requirement 4

**User Story:** As a plugin developer, I want default values in getters to not pollute my configuration files, so that only explicitly set values are persisted.

#### Acceptance Criteria

1. WHEN a getter method returns a default value because the backing field is null, THE ConfigMapper SHALL NOT serialize that default value to the configuration file.
2. WHERE a @DefaultValue annotation specifies a default, THE ConfigMapper SHALL only write the value if it differs from the default.
3. THE ConfigMapper SHALL distinguish between "field is null" and "field was explicitly set to a value equal to the default".
