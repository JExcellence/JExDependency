# Requirements Document

## Introduction

This document specifies the requirements for setting up JExHome as a proper multi-module Gradle project following the same structure and patterns as JExEconomy and RDQ. The project will provide a home teleportation system for Minecraft servers with free and premium editions, using modern Java 21+ patterns, the JExcellence ecosystem libraries, and the inventory-framework for GUIs.

## Glossary

- **JExHome**: The home teleportation plugin being developed
- **Home**: A saved location that players can teleport to
- **JExcellence Ecosystem**: The suite of internal libraries (JExCommand, JExTranslate, JEHibernate, RPlatform, etc.)
- **Free Edition**: Basic version with limited features
- **Premium Edition**: Full-featured version with all capabilities
- **Common Module**: Shared code between free and premium editions
- **RPlatform**: The platform abstraction layer for Bukkit/Paper plugins
- **ViewFrame**: The inventory-framework system for creating GUIs
- **CachedRepository**: Repository pattern with caching from JEHibernate

## Requirements

### Requirement 1: Multi-Module Gradle Project Structure

**User Story:** As a developer, I want JExHome to follow the same multi-module Gradle structure as JExEconomy and RDQ, so that I can maintain consistency across projects and support free/premium editions.

#### Acceptance Criteria

1. THE JExHome project SHALL contain three submodules: jexhome-common, jexhome-free, and jexhome-premium.
2. THE JExHome project SHALL use the root project's version catalog (libs.versions.toml) for dependency management.
3. THE jexhome-common module SHALL use the raindrop.library-conventions plugin for shared library configuration.
4. THE jexhome-free module SHALL use the raindrop.shadow-conventions plugin for creating shaded JARs.
5. THE jexhome-premium module SHALL use the raindrop.shadow-conventions plugin for creating shaded JARs.
6. THE root settings.gradle.kts SHALL include JExHome submodules following the pattern of JExEconomy.

### Requirement 2: Dependency Configuration

**User Story:** As a developer, I want JExHome to use the same dependencies as JExEconomy and RDQ, so that I can leverage the existing ecosystem libraries.

#### Acceptance Criteria

1. THE jexhome-common module SHALL include compileOnly dependencies for Paper API, Adventure bundles, and JExcellence bundles.
2. THE jexhome-common module SHALL include compileOnly dependencies for Hibernate platform and JEHibernate.
3. THE jexhome-free module SHALL implement the jexhome-common module and shade JExcellence bundles.
4. THE jexhome-premium module SHALL implement the jexhome-common module and shade JExcellence bundles.
5. THE build configuration SHALL enable Java preview features for modern Java 21+ patterns.

### Requirement 3: Main Plugin Class Structure

**User Story:** As a developer, I want JExHome to follow the same plugin class pattern as RDQ, so that I have proper separation between the JavaPlugin entry point and the implementation delegate.

#### Acceptance Criteria

1. THE JExHome common module SHALL contain an abstract JExHome class following the RDQ pattern.
2. THE JExHome class SHALL use RPlatform for platform initialization and metrics.
3. THE JExHome class SHALL use ViewFrame for inventory GUI management.
4. THE JExHome class SHALL use CommandFactory for command registration.
5. THE jexhome-free module SHALL contain JExHomeFree (JavaPlugin) and JExHomeFreeImpl (delegate) classes.
6. THE jexhome-premium module SHALL contain JExHomePremium (JavaPlugin) and JExHomePremiumImpl (delegate) classes.

### Requirement 4: Command System

**User Story:** As a developer, I want JExHome commands to follow the modern JExCommand patterns from RDQ, so that commands are consistent and maintainable.

#### Acceptance Criteria

1. THE JExHome project SHALL implement a home command using the PlayerCommand base class with @Command annotation.
2. THE JExHome project SHALL implement a sethome command using the PlayerCommand base class with @Command annotation.
3. THE JExHome project SHALL implement a delhome command using the PlayerCommand base class with @Command annotation.
4. EACH command SHALL have a corresponding Section class extending ACommandSection.
5. EACH command SHALL have a corresponding Permission enum implementing IPermissionNode.
6. THE command classes SHALL use var for local variable declarations where type is obvious.
7. THE command classes SHALL use @NotNull and @Nullable annotations instead of Objects.requireNonNull.

### Requirement 5: Database Entity and Repository

**User Story:** As a developer, I want JExHome to use the modern JEHibernate repository pattern from RDQ, so that database operations are consistent and cached.

#### Acceptance Criteria

1. THE Home entity SHALL extend BaseEntity from JEHibernate.
2. THE Home entity SHALL store home name, location, and player UUID.
3. THE HomeRepository SHALL extend CachedRepository for caching and async operations.
4. THE repository SHALL be registered via RepositoryManager following the RDQ pattern.
5. THE entity SHALL use @NotNull and @Nullable annotations appropriately.

### Requirement 6: GUI System

**User Story:** As a developer, I want JExHome to use the inventory-framework ViewFrame system from RDQ, so that GUIs are consistent and maintainable.

#### Acceptance Criteria

1. THE JExHome project SHALL implement a HomeOverviewView for displaying player homes.
2. THE view SHALL be registered with ViewFrame during plugin initialization.
3. THE view SHALL use the modern inventory-framework patterns from RDQ.
4. THE view SHALL support pagination for players with multiple homes.

### Requirement 7: Translation System

**User Story:** As a developer, I want JExHome to use JExTranslate for internationalization, so that messages support multiple languages.

#### Acceptance Criteria

1. THE JExHome project SHALL include translation files in the translations resource directory.
2. THE translation files SHALL support at minimum en_US and de_DE locales.
3. THE translation keys SHALL follow the pattern used in the provided old translation YAML.
4. THE I18n.Builder pattern SHALL be used for sending translated messages.

### Requirement 8: Configuration System

**User Story:** As a developer, I want JExHome to use the JEConfig system for configuration, so that settings are type-safe and validated.

#### Acceptance Criteria

1. THE JExHome project SHALL include command configuration files in the commands resource directory.
2. THE JExHome project SHALL include a home-system.yml configuration for general settings.
3. THE configuration SHALL support permission-based home limits using a map structure.
4. THE configuration sections SHALL implement IConfigSection from bbconfigmapper.

### Requirement 9: Plugin Descriptor Files

**User Story:** As a developer, I want JExHome to have proper plugin.yml and paper-plugin.yml files, so that the plugin loads correctly on Spigot and Paper servers.

#### Acceptance Criteria

1. THE jexhome-free module SHALL include a plugin.yml with version placeholder substitution.
2. THE jexhome-free module SHALL include a paper-plugin.yml with the JEDependency loader.
3. THE jexhome-premium module SHALL include a plugin.yml with version placeholder substitution.
4. THE jexhome-premium module SHALL include a paper-plugin.yml with the JEDependency loader.
5. THE plugin descriptors SHALL declare soft dependencies on Vault, LuckPerms, and PlaceholderAPI.
