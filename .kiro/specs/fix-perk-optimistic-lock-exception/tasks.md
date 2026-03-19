# Implementation Plan

- [ ] 1. Create retry utility infrastructure
  - Create `RetryableRepositoryOperation` utility class with generic retry logic
  - Implement exponential backoff calculation method
  - Implement exception type checking for retryable exceptions (OptimisticLockException, StaleObjectStateException)
  - Add structured logging for retry attempts and failures
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 3.4_

- [ ] 2. Implement retry context and configuration
  - Create `RetryContext` class for structured logging
  - Add retry configuration to perk system config (max attempts, base delay, backoff multiplier)
  - Implement configuration loading in PerkActivationService
  - _Requirements: 2.1, 2.2, 3.3_

- [ ] 3. Create repository service wrapper
  - Create `PlayerPerkRepositoryService` class to wrap repository with retry logic
  - Implement `updateWithRetry()` method that uses RetryableRepositoryOperation
  - Add entity reload functionality to fetch fresh data between retries
  - Ensure thread-safety for concurrent operations
  - _Requirements: 1.1, 1.2, 3.5, 5.1, 5.2_

- [ ] 4. Update PerkActivationService activate method
  - Modify `activate()` method to use retry-enabled repository operations
  - Implement state reapplication logic for retried operations
  - Add proper error handling and player notification for failures
  - Update cache invalidation to work with retry logic
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.3, 5.3_

- [ ] 5. Update PerkActivationService deactivate method
  - Modify `deactivate()` method to use retry-enabled repository operations
  - Implement state reapplication logic for retried deactivation
  - Add proper error handling and player notification for failures
  - Ensure effects removal is idempotent
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.3, 5.3, 5.4_

- [ ] 6. Enhance logging throughout perk system
  - Update all perk-related logging to use centralized logger
  - Add retry count to success log messages
  - Add complete context (player, perk, version) to error logs
  - Implement different log levels for retry attempts vs final failures
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 7. Add player-facing error messages
  - Create localized error message keys for perk failures
  - Implement player notification method in PerkActivationService
  - Add success confirmation messages for perk toggles
  - Ensure error messages don't expose technical details
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 8. Verify entity state methods are idempotent
  - Review `recordActivation()` and `recordDeactivation()` methods
  - Ensure multiple calls with same state don't cause issues
  - Add guards against duplicate state changes if needed
  - _Requirements: 5.4_

- [ ] 9. Add transaction isolation for retry attempts
  - Ensure each retry attempt uses a fresh transaction
  - Verify previous transaction is properly closed before retry
  - Add transaction boundary logging for debugging
  - _Requirements: 5.1, 5.2, 5.5_

- [ ]* 10. Write unit tests for retry logic
  - Test RetryableRepositoryOperation with successful first attempt
  - Test retry on OptimisticLockException
  - Test retry on StaleObjectStateException  
  - Test exponential backoff calculation
  - Test max retries exhaustion
  - Test non-retryable exception handling
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.4_

- [ ]* 11. Write integration tests for concurrent modifications
  - Simulate concurrent perk toggles by multiple threads
  - Verify both operations eventually succeed
  - Verify final entity state is consistent
  - Test transaction isolation between retries
  - _Requirements: 1.1, 5.1, 5.2, 5.5_

- [ ]* 12. Add monitoring and metrics
  - Add metrics for total retry attempts
  - Add metrics for retry success rate
  - Add metrics for operations failing after max retries
  - Create dashboard or logging for monitoring retry behavior
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
