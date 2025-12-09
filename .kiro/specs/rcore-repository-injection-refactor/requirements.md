# Requirements Document

## Introduction

This document specifies the requirements for refactoring RCore to leverage JEHibernate's dependency injection system for repositories and services. The goal is to reduce code verbosity, improve maintainability, and establish a consistent pattern for repository access across the RCore codebase. Currently, RCore manually manages repository instances and passes them through method calls, leading to verbose code and tight coupling. By adopting the `@InjectRepository` annotation pattern with dedicated service classes, we can achieve cleaner, more maintainable code that follows modern dependency injection principles.

## Glossary

- **RCore**: The core plugin module that provides player and statistic management functionality
- **JEHibernate**: The persistence framework providing repository and dependency injection capabilities
- **RepositoryManager**: The singleton managing repository registration and injection in JEHibernate
- **Service Class**: A class containing business logic that uses repositories through dependency injection
- **Repository**: A data access object extending GenericCachedRepository or AbstractCRUDRepository
- **@InjectRepository**: An annotation marking fields for automatic repository injection
- **RCoreImpl**: The main plugin implementation class managing lifecycle and initialization
- **RCoreService**: The public API interface exposing RCore functionality to other plugins

## Requirements

### Requirement 1: Service Layer Architecture

**User Story:** As a developer, I want a clear service layer architecture so that business logic is separated from repository management and the codebase is easier to maintain.

#### Acceptance Criteria

1. WHERE service classes are created, THE System SHALL use the `@InjectRepository` annotation to declare repository dependencies
2. WHEN a service class is instantiated, THE RepositoryManager SHALL automatically inject all annotated repository fields
3. THE System SHALL organize service classes in the `com.raindropcentral.core.service` package
4. THE System SHALL ensure service classes contain only business logic without direct EntityManager access
5. WHERE multiple repositories are needed, THE System SHALL inject all required repositories into a single service class

### Requirement 2: Repository Registration

**User Story:** As a developer, I want repositories to be registered once during plugin initialization so that they are available for injection throughout the application lifecycle.

#### Acceptance Criteria

1. WHEN RCore initializes, THE System SHALL register all repository classes with RepositoryManager before creating service instances
2. THE System SHALL register RPlayerRepository with UUID as the cache key using `RPlayer::getUniqueId`
3. THE System SHALL register RPlayerStatisticRepository with Long as the cache key using `RPlayerStatistic::getId`
4. THE System SHALL register RStatisticRepository with Long as the cache key using `RAbstractStatistic::getId`
5. THE System SHALL register RPlayerInventoryRepository with appropriate cache key
6. THE System SHALL register RCentralServerRepository with appropriate cache key

### Requirement 3: Service Class Implementation

**User Story:** As a developer, I want dedicated service classes for different functional areas so that code is organized by domain and easier to navigate.

#### Acceptance Criteria

1. THE System SHALL provide a PlayerService class handling all player-related operations
2. THE System SHALL provide a StatisticService class handling all statistic-related operations
3. THE System SHALL provide an InventoryService class handling all inventory-related operations
4. WHERE a service needs multiple repositories, THE System SHALL inject all required repositories using `@InjectRepository` annotations
5. THE System SHALL ensure each service method returns CompletableFuture for asynchronous operations

### Requirement 4: RCoreService Adapter Refactoring

**User Story:** As a developer, I want the RCoreService implementation to delegate to service classes so that the adapter layer is thin and maintainable.

#### Acceptance Criteria

1. THE System SHALL refactor RCoreService implementation to use injected service classes instead of direct repository access
2. WHEN RCoreService methods are called, THE System SHALL delegate to the appropriate service class method
3. THE System SHALL inject PlayerService into the RCoreService implementation
4. THE System SHALL inject StatisticService into the RCoreService implementation
5. THE System SHALL ensure the RCoreService adapter contains no direct repository access

### Requirement 5: Initialization Sequence

**User Story:** As a developer, I want a clear initialization sequence so that dependencies are available when needed and the plugin starts reliably.

#### Acceptance Criteria

1. WHEN RCore enables, THE System SHALL initialize RepositoryManager before creating service instances
2. THE System SHALL register all repositories with RepositoryManager before injecting services
3. WHEN service instances are created, THE System SHALL call `RepositoryManager.getInstance().injectInto()` for each service
4. THE System SHALL initialize services in the correct dependency order
5. IF repository initialization fails, THEN THE System SHALL log an error and disable the plugin gracefully

### Requirement 6: Backward Compatibility

**User Story:** As a developer, I want the refactoring to maintain backward compatibility so that existing functionality continues to work without breaking changes.

#### Acceptance Criteria

1. THE System SHALL maintain all existing public API methods in RCoreService
2. THE System SHALL preserve all existing method signatures and return types
3. THE System SHALL ensure all existing asynchronous operations continue to execute on the configured executor
4. THE System SHALL maintain the same error handling behavior for exceptional completions
5. THE System SHALL preserve null-safety guarantees with `@NotNull` annotations

### Requirement 7: Code Reduction and Simplification

**User Story:** As a developer, I want reduced code verbosity so that I can write features faster and maintain code more easily.

#### Acceptance Criteria

1. THE System SHALL eliminate manual repository instantiation from RCoreImpl
2. THE System SHALL remove repository getter methods from RCoreImpl that expose repositories directly
3. THE System SHALL reduce the number of lines in RCoreImpl by at least 30%
4. WHERE repository access is needed, THE System SHALL use service classes instead of direct repository references
5. THE System SHALL eliminate repository null checks by ensuring injection happens during initialization

### Requirement 8: Documentation and Examples

**User Story:** As a developer, I want clear documentation and examples so that I can understand how to use the new service-based architecture.

#### Acceptance Criteria

1. THE System SHALL provide a usage documentation file showing how to create new service classes
2. THE System SHALL include examples of single-repository and multi-repository service patterns
3. THE System SHALL document the initialization sequence and dependency order
4. THE System SHALL provide examples of how to inject services into commands and listeners
5. THE System SHALL include troubleshooting guidance for common injection issues
