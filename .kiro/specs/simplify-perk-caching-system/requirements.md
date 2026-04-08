# Simplify Perk Caching System - Requirements

## Problem Statement

The current perk system is over-engineered with:
- Complex `RetryableOperation` class for handling optimistic lock exceptions
- Complicated caching logic with dirty tracking and async saves
- Slow performance when unlocking/toggling perks (2+ minute delays)
- Multiple layers of abstraction making debugging difficult

## Goals

1. **Simplify caching** - Use simple HashMap to store player perks in memory
2. **Instant operations** - Toggle enabled/disabled instantly without DB calls
3. **Load on join** - Load all player perks into memory when player joins
4. **Save on leave** - Persist all changes to DB when player leaves
5. **Remove complexity** - Eliminate RetryableOperation and complex retry logic
6. **Match rank system** - Follow the same simple pattern used by the rank system

## Current Issues

### Performance Problems
- Unlocking a perk takes 2+ minutes
- Toggling perks has noticeable delay
- Multiple DB queries per operation
- Retry logic adds unnecessary overhead

### Code Complexity
- `RetryableOperation` class adds abstraction
- `PlayerPerkCache` has complex dirty tracking
- `PerkManagementService` has nested CompletableFutures
- Hard to debug and maintain

### User Experience
- Players experience lag when interacting with perks
- UI feels unresponsive
- No immediate feedback on actions

## Success Criteria

1. Perk toggle happens instantly (< 50ms)
2. Perk unlock completes in < 1 second
3. No RetryableOperation class needed
4. Simple HashMap-based caching
5. Code is easy to understand and maintain
6. All perk data saved reliably on player leave

## Non-Goals

- Changing the database schema
- Modifying perk configuration format
- Altering perk types or effects
- Changing the UI/view system

## Constraints

- Must maintain data integrity (no lost perk data)
- Must handle server crashes gracefully (periodic auto-save)
- Must work with existing PlayerPerk entities
- Must be compatible with current perk handlers
