# Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following properties have been identified as testable through property-based testing:

## Navigation and UI Properties

### Property 1: Navigation button routing
*For any* navigation button in the main menu, clicking it should open the corresponding view class.
**Validates: Requirements 1.4**

### Property 2: Target selection enables rewards
*For any* bounty creation view, when a target is selected, the item and currency reward buttons should be enabled.
**Validates: Requirements 2.3**

### Property 3: No target disables rewards
*For any* bounty creation view, when no target is selected, the item and currency reward buttons should be disabled.
**Validates: Requirements 2.2**

## State Management Properties

### Property 4: Item insertion updates state
*For any* set of items inserted into reward slots, the creation context state should contain those items.
**Validates: Requirements 2.4, 4.3**

### Property 5: Item removal updates state
*For any* item removed from reward slots, the creation context state should no longer contain that item.
**Validates: Requirements 4.4**

### Property 6: State persistence across navigation
*For any* reward items added before navigating away from the creation view, returning to the view should preserve those items.
**Validates: Requirements 4.5**

### Property 7: Target selection updates state
*For any* player selected as a target, the creation view's target state should be updated to that player.
**Validates: Requirements 3.3**

### Property 8: Target display after selection
*For any* selected target, the creation view should display the target's head and name.
**Validates: Requirements 3.4**

## Bounty Creation Properties

### Property 9: Successful bounty creation
*For any* valid target and non-empty reward set, confirming creation should create a bounty via BountyService.
**Validates: Requirements 2.5**

### Property 10: Creation clears temporary storage
*For any* successful bounty creation, the player's inserted items should be cleared from temporary storage.
**Validates: Requirements 2.6**

### Property 11: Refund on cancellation
*For any* set of inserted items, closing the creation view without confirming should refund all items to the player's inventory.
**Validates: Requirements 2.7**

## Item Merging Properties

### Property 12: Similar items stack
*For any* set of similar items (same type and metadata), merging should combine them by summing their amounts.
**Validates: Requirements 4.6**

## Currency Properties

### Property 13: Currency balance validation
*For any* currency and amount selected, the system should validate the player has sufficient balance before adding to rewards.
**Validates: Requirements 5.3**

### Property 14: Valid currency addition
*For any* currency that passes validation, it should be added to the bounty's reward currencies map.
**Validates: Requirements 5.4**

### Property 15: Currency accumulation
*For any* multiple currencies added, they should accumulate in the rewards map without overwriting.
**Validates: Requirements 5.5**

### Property 16: Currency distribution
*For any* bounty with currency rewards, claiming should credit those currencies to the hunter's economy balance.
**Validates: Requirements 11.5**

## Bounty Listing Properties

### Property 17: Async bounty loading
*For any* bounty list view opening, active bounties should be fetched asynchronously via BountyService.
**Validates: Requirements 6.1**

### Property 18: Paginated display
*For any* loaded bounties, they should be displayed as paginated player heads.
**Validates: Requirements 6.2**

### Property 19: Complete bounty information
*For any* displayed bounty, it should show target head, name, reward summary, and expiration time.
**Validates: Requirements 6.3**

### Property 20: Bounty detail navigation
*For any* bounty entry clicked, the detailed bounty information view should open.
**Validates: Requirements 6.4**

### Property 21: Pagination controls
*For any* bounty list with more entries than fit on one page, pagination controls should be provided.
**Validates: Requirements 6.5**

### Property 22: Page navigation
*For any* page navigation action, the next page of bounties should load and display.
**Validates: Requirements 6.6**

## Bounty Detail Properties

### Property 23: Complete detail display
*For any* bounty detail view, it should display target information, commissioner, and all rewards.
**Validates: Requirements 7.1**

### Property 24: Item detail format
*For any* reward item displayed, it should show quantity and type.
**Validates: Requirements 7.2**

### Property 25: Currency detail format
*For any* reward currency displayed, it should show currency name and amount.
**Validates: Requirements 7.3**

### Property 26: Expiration display
*For any* bounty with an expiration time, the remaining time should be displayed.
**Validates: Requirements 7.4**

### Property 27: Claim information display
*For any* claimed bounty, the claimer's name and claim timestamp should be displayed.
**Validates: Requirements 7.5**

### Property 28: Expired status display
*For any* expired bounty, an expired status indicator should be displayed.
**Validates: Requirements 7.6, 9.5, 12.4**

## Leaderboard Properties

### Property 29: Async hunter loading
*For any* leaderboard view opening, top hunters should be fetched asynchronously via BountyService.
**Validates: Requirements 8.1**

### Property 30: Ranked display
*For any* loaded hunters, they should be displayed ranked by total bounties claimed.
**Validates: Requirements 8.2**

### Property 31: Complete hunter information
*For any* hunter entry, it should show rank, name, bounties claimed, and total reward value.
**Validates: Requirements 8.3**

### Property 32: Leaderboard refresh
*For any* leaderboard update, the display should refresh with current statistics.
**Validates: Requirements 8.4**

### Property 33: Self-highlighting
*For any* player viewing the leaderboard, their own entry should be highlighted.
**Validates: Requirements 8.5**

## My Bounties Properties

### Property 34: Commissioner bounty loading
*For any* my-bounties view opening, bounties created by the player should be fetched via BountyService.
**Validates: Requirements 9.1**

### Property 35: Status indicator display
*For any* player's bounties loaded, they should be displayed with status indicators.
**Validates: Requirements 9.2**

### Property 36: Active status display
*For any* active bounty, it should show an active status indicator.
**Validates: Requirements 9.3**

### Property 37: Claimed status display
*For any* claimed bounty, it should show claimed status and claimer information.
**Validates: Requirements 9.4**

### Property 38: My bounty detail navigation
*For any* player's bounty clicked, the detailed bounty information view should open.
**Validates: Requirements 9.6**

## Claim Mode Properties

### Property 39: Kill attribution
*For any* player with an active bounty killed, the killer should be determined based on the configured claim mode.
**Validates: Requirements 10.1**

### Property 40: Last hit attribution
*For any* kill with LAST_HIT mode, the bounty should be attributed to the player who dealt the final blow.
**Validates: Requirements 10.2**

### Property 41: Most damage attribution
*For any* kill with MOST_DAMAGE mode, the bounty should be attributed to the player who dealt the most damage within the tracking window.
**Validates: Requirements 10.3**

### Property 42: Damage split distribution
*For any* kill with DAMAGE_SPLIT mode, rewards should be distributed proportionally among all damage dealers.
**Validates: Requirements 10.4**

### Property 43: Claim state update
*For any* bounty claimed, it should be marked as claimed with the claimer's UUID and timestamp.
**Validates: Requirements 10.5**

### Property 44: Statistics update on claim
*For any* bounty claimed, the hunter's statistics should be updated via BountyService.
**Validates: Requirements 10.6**

### Property 45: Reward distribution on claim
*For any* bounty claimed, rewards should be distributed according to the configured distribution mode.
**Validates: Requirements 10.7**

## Distribution Mode Properties

### Property 46: Instant distribution
*For any* bounty with INSTANT distribution mode, reward items should be added directly to the hunter's inventory.
**Validates: Requirements 11.1**

### Property 47: Virtual distribution
*For any* bounty with VIRTUAL distribution mode, rewards should be credited to the hunter's virtual storage.
**Validates: Requirements 11.2**

### Property 48: Drop distribution
*For any* bounty with DROP distribution mode, reward items should be dropped at the target's death location.
**Validates: Requirements 11.3**

### Property 49: Chest distribution
*For any* bounty with CHEST distribution mode, rewards should be placed in a chest at the death location.
**Validates: Requirements 11.4**

## Expiration Properties

### Property 50: Expiration time calculation
*For any* bounty created, the expiration time should be set based on the configured expiry-days setting.
**Validates: Requirements 12.1**

### Property 51: Expired bounty detection
*For any* bounty status check, expired bounties should be marked as inactive.
**Validates: Requirements 12.2**

### Property 52: Refund on expiration
*For any* bounty that expires, rewards should be refunded to the commissioner if configured.
**Validates: Requirements 12.3**

## Announcement Properties

### Property 53: Creation announcement
*For any* bounty created when announcements are enabled, a creation message should be broadcast using JExTranslate.
**Validates: Requirements 13.1**

### Property 54: Claim announcement
*For any* bounty claimed when announcements are enabled, a claim message should be broadcast using JExTranslate.
**Validates: Requirements 13.2**

### Property 55: Global broadcast
*For any* announcement with broadcast radius -1, the message should be sent globally to all players.
**Validates: Requirements 13.3**

### Property 56: Radius broadcast
*For any* announcement with positive broadcast radius, the message should be sent only to players within the radius.
**Validates: Requirements 13.4**

### Property 57: Disabled announcements
*For any* bounty event when announcements are disabled, no broadcast should occur.
**Validates: Requirements 13.5**

### Property 58: JExTranslate integration
*For any* user-facing message, JExTranslate should be used with proper placeholder support.
**Validates: Requirements 13.6**

## Visual Indicator Properties

### Property 59: Tab prefix application
*For any* player with an active bounty when visual indicators are enabled, the configured tab prefix should be applied to their tab list name.
**Validates: Requirements 14.1**

### Property 60: Name color application
*For any* player with an active bounty when visual indicators are enabled, the configured name color should be applied to their display name.
**Validates: Requirements 14.2**

### Property 61: Particle spawning
*For any* player with an active bounty when particles are enabled, particles should spawn around them at the configured interval.
**Validates: Requirements 14.3**

### Property 62: Visual indicator cleanup
*For any* bounty that is claimed or expires, all visual indicators should be removed from the target.
**Validates: Requirements 14.4**

### Property 63: Bounty persistence across logout
*For any* player logging out with an active bounty, the bounty should be preserved for when they return.
**Validates: Requirements 14.5**

## Error Handling Properties

### Property 64: Async error handling
*For any* async operation that fails, errors should be logged and the user should be notified appropriately.
**Validates: Requirements 16.3**

### Property 65: Async view updates
*For any* view needing database data, it should fetch asynchronously and update the view when loaded.
**Validates: Requirements 16.4**

## Hunter Statistics Properties

### Property 66: Bounties claimed increment
*For any* bounty claimed, the hunter's bounties claimed counter should be incremented.
**Validates: Requirements 17.1**

### Property 67: Total reward value accumulation
*For any* bounty claimed, the reward value should be added to the hunter's total reward value.
**Validates: Requirements 17.2**

### Property 68: Highest bounty update
*For any* bounty claimed with value exceeding the hunter's current highest, the highest bounty value should be updated.
**Validates: Requirements 17.3**

### Property 69: Timestamp update
*For any* bounty claimed, the hunter's last claim timestamp should be updated.
**Validates: Requirements 17.4**

### Property 70: Async statistics retrieval
*For any* hunter statistics query, they should be fetched asynchronously from the database.
**Validates: Requirements 17.5**

### Property 71: New hunter entity creation
*For any* new hunter claiming their first bounty, a new BountyHunterStats entity should be created.
**Validates: Requirements 17.6**

## Edition Properties

### Property 72: Premium service selection
*For any* premium edition activation, PremiumBountyService should be used with full database persistence and unlimited bounties.
**Validates: Requirements 18.1**

### Property 73: Free service selection
*For any* free edition activation, FreeBountyService should be used with in-memory storage and one active bounty limit.
**Validates: Requirements 18.2**

### Property 74: Static bounty support
*For any* free edition activation, static pre-configured bounties should be loaded from configuration.
**Validates: Requirements 18.3, 19.1**

### Property 75: Edition-specific bounty limits
*For any* edition, the maximum bounties per player should return 1 for free and configurable value for premium.
**Validates: Requirements 18.5**

### Property 76: Edition-specific item limits
*For any* edition, the maximum reward items should return edition-specific limits.
**Validates: Requirements 18.6**

## Free Edition Properties

### Property 77: Static bounty structure
*For any* static bounty configured, it should include target UUID, reward items, and reward currencies.
**Validates: Requirements 19.2**

### Property 78: Free edition bounty limit
*For any* bounty creation attempt in free edition, only one active bounty should be allowed at a time.
**Validates: Requirements 19.3**

### Property 79: Bounty slot release
*For any* free edition bounty claimed, creating a new bounty should be allowed.
**Validates: Requirements 19.4**

### Property 80: Static bounty validation
*For any* static bounty used, the configured target should be validated to exist.
**Validates: Requirements 19.5**

## Edge Cases

The following edge cases should be handled with specific test cases:

1. **Full inventory during refund**: When a player's inventory is full during item refund, excess items should drop at the player's location (Requirements 2.8)

2. **Self-targeting prevention**: When a player attempts to select themselves as a bounty target, the selection should be prevented with an error message (Requirements 3.5)

3. **Full inventory during distribution**: When a hunter's inventory is full during INSTANT distribution, excess items should drop at the hunter's location (Requirements 11.6)

4. **Expired bounty claim prevention**: When a player attempts to claim an expired bounty, the claim should be prevented (Requirements 12.5)
