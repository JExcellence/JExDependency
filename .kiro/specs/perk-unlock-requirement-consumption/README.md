# Perk Unlock & Enable Fix

## IMMEDIATE ACTION REQUIRED

**Run this SQL command NOW to fix existing perks:**
```sql
UPDATE player_perk SET enabled = true WHERE unlocked = true AND enabled = false;
```

Without this migration, existing perks will remain broken!

## Problem
Perks were being unlocked but not enabled, causing "Cannot activate perk: perk not enabled" errors. Requirements were consumed successfully, but the perk's `enabled` flag remained `false`.

## Root Cause
`PerkManagementService.grantPerk()` only set `unlocked=true` but never set `enabled=true`, creating an inconsistent state where perks were unlocked but unusable.

## Solution Implemented

### Code Changes

1. **PerkManagementService.grantPerk()**
   - Added `autoEnable` boolean parameter
   - Sets `enabled=true` when `autoEnable=true`
   - Created backward-compatible deprecated overload
   - Enhanced logging to show enabled status

2. **PerkRequirementService.attemptUnlock()**
   - Passes `autoEnable=true` when granting perks
   - Validates perk is both unlocked AND enabled after grant
   - Returns GRANT_FAILED if enabled status is incorrect
   - Enhanced logging for unlock process

3. **PerkReward.grant()**
   - Updated to use new `grantPerk(rdqPlayer, perk, autoEnable)` signature
   - Validates perk state after grant
   - Removed redundant `enablePerk()` call

4. **PerkActivationService.activate()**
   - Added unlocked status check before enabled check
   - Upgraded enabled check logging from WARNING to SEVERE
   - Includes both unlocked and enabled status in error logs
   - Better error messages for different failure states

5. **PerkDetailView.handleUnlockAttempt()**
   - Validates perk is enabled after unlock
   - Auto-activates perk after successful unlock
   - Shows appropriate error if perk is unlocked but not enabled
   - Updated success message to "unlocked and enabled"

6. **PerkManagementService.migrateUnlockedPerksToEnabled()**
   - New migration method to fix existing perks
   - Finds all perks with `unlocked=true, enabled=false`
   - Sets `enabled=true` and updates database
   - Returns count of migrated perks

## Migration Required

### Option 1: SQL Migration (Fastest)
```sql
UPDATE player_perk 
SET enabled = true 
WHERE unlocked = true AND enabled = false;
```

### Option 2: In-Game Migration (Recommended)
Add to your plugin startup (after initializing PerkManagementService):

```java
perkManagementService.migrateUnlockedPerksToEnabled().thenAccept(count -> {
    getLogger().info("Migrated " + count + " unlocked perks to enabled status");
});
```

### Option 3: Manual Per-Player
Players can re-unlock perks (requirements won't be consumed again) or manually enable via database.

## Verification Steps

1. Run migration (SQL or in-game)
2. Check logs for "Migration complete: enabled X unlocked perks"
3. Have a player unlock a new perk
4. Verify success message shows "unlocked and enabled"
5. Verify perk can be immediately activated
6. Check logs for no "data inconsistency" SEVERE errors

## Future Enhancements

1. **Configuration Option**: Add `auto-enable-on-unlock` config flag
2. **Admin Command**: Add `/rdq perk migrate` command for manual migration
3. **State Validation**: Add periodic validation to detect and fix inconsistent states
4. **Metrics**: Track unlock success/failure rates
5. **UI Feedback**: Show enabled status more clearly in perk UI
6. **Bulk Operations**: Add admin commands to enable/disable perks for all players

## Testing Checklist

- [x] Perk unlocks with requirements consumed
- [x] Perk is automatically enabled after unlock
- [x] Perk can be immediately activated
- [x] No "perk not enabled" errors in logs
- [x] Existing functionality (enable/disable) still works
- [ ] Migration tested on production database
- [ ] Performance impact measured
- [ ] Edge cases tested (max enabled limit, etc.)

## Notes

- The deprecated `grantPerk(player, perk)` method defaults to `autoEnable=false` for backward compatibility
- All new code should use `grantPerk(player, perk, true)` for unlock operations
- The migration is idempotent and safe to run multiple times
- Logging uses direct variable concatenation instead of `{0}` placeholders for better readability
