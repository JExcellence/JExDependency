# Bounty System Improvements - Requirements Document

## Introduction

This document outlines the requirements for improving the RDQ bounty system by migrating missing features from the old RDQ2 implementation. The current implementation lacks critical components like ClaimHandler, RewardDistributors, ClaimResult, and proper bounty expiration handling.

## Glossary

- **BountySystem**: The complete bounty management system in RDQ
- **ClaimHandler**: Component that determines bounty winners based on damage and claim modes
- **RewardDistributor**: Component that distributes bounty rewards to hunters
- **ClaimResult**: Data structure containing claim outcome and reward distribution
- **DamageTracker**: Component that tracks damage dealt to bounty targets
- **BountyFactory**: High-level API for bounty operations
- **ClaimMode**: Strategy for determining bounty winners (LAST_HIT, MOST_DAMAGE, DAMAGE_SPLIT)
- **DistributionMode**: Strategy for distributing rewards (INSTANT, DROP, CHEST, VIRTUAL)
- **Hunter**: Player who claims a bounty by killing the target
- **Commissioner**: Player who creates a bounty
- **Target**: Player who has a bounty on their head

## Requirements

### Requirement 1: Claim Attribution System

**User Story:** As a server administrator, I want a flexible claim attribution system so that bounties can be awarded based on different combat contribution strategies.

#### Acceptance Criteria

1. WHEN a bounty target dies, THE BountySystem SHALL determine the winner(s) using the configured ClaimMode
2. WHILE ClaimMode is LAST_HIT, THE BountySystem SHALL award the full bounty to the player who dealt the final blow
3. WHILE ClaimMode is MOST_DAMAGE, THE BountySystem SHALL award the full bounty to the player who dealt the most damage within the tracking window
4. WHILE ClaimMode is DAMAGE_SPLIT, THE BountySystem SHALL split the bounty proportionally among all damage dealers
5. IF the target dies from environmental damage AND players dealt damage within the tracking window, THEN THE BountySystem SHALL award the bounty to the highest damage dealer

### Requirement 2: Reward Distribution System

**User Story:** As a server administrator, I want multiple reward distribution modes so that I can customize how bounty rewards are delivered to hunters.

#### Acceptance Criteria

1. THE BountySystem SHALL support INSTANT distribution mode that adds rewards directly to the hunter's inventory
2. THE BountySystem SHALL support DROP distribution mode that drops rewards at the death location
3. THE BountySystem SHALL support CHEST distribution mode that places rewards in a chest at the death location
4. THE BountySystem SHALL support VIRTUAL distribution mode that stores rewards in a virtual storage system
5. WHEN the hunter's inventory is full during INSTANT distribution, THE BountySystem SHALL drop excess items at the hunter's location

### Requirement 3: Claim Result Tracking

**User Story:** As a developer, I want a structured ClaimResult object so that claim outcomes can be properly tracked and announced.

#### Acceptance Criteria

1. THE BountySystem SHALL create a ClaimResult containing the bounty, total reward value, and distribution mode
2. THE ClaimResult SHALL include a map of winners to their reward proportions for split claims
3. THE BountySystem SHALL use ClaimResult data for announcements and statistics
4. THE ClaimResult SHALL be immutable after creation

### Requirement 4: Enhanced BountyFactory

**User Story:** As a developer, I want an enhanced BountyFactory so that all bounty operations are accessible through a single, clean API.

#### Acceptance Criteria

1. THE BountyFactory SHALL provide a claimBounty method that handles the complete claim workflow
2. THE BountyFactory SHALL provide access to the ClaimHandler for determining winners
3. THE BountyFactory SHALL provide access to RewardDistributors for distributing rewards
4. THE BountyFactory SHALL handle bounty expiration checks and cleanup
5. THE BountyFactory SHALL maintain cache consistency after claim operations

### Requirement 5: Bounty Expiration System

**User Story:** As a server administrator, I want automatic bounty expiration so that old bounties don't accumulate indefinitely.

#### Acceptance Criteria

1. THE BountySystem SHALL run a periodic task to check for expired bounties
2. WHEN a bounty expires, THE BountySystem SHALL mark it as expired in the database
3. WHEN a bounty expires, THE BountySystem SHALL remove it from the active bounty cache
4. THE BountySystem SHALL optionally refund a percentage of the bounty value to the commissioner
5. THE BountySystem SHALL announce bounty expirations based on configuration

### Requirement 6: Enhanced Damage Tracking

**User Story:** As a developer, I want enhanced damage tracking so that claim attribution is accurate and performant.

#### Acceptance Criteria

1. THE DamageTracker SHALL integrate with Bukkit's EntityDamageByEntityEvent
2. THE DamageTracker SHALL track projectile damage with proper attacker attribution
3. THE DamageTracker SHALL track damage from tamed entities (wolves, etc.) to their owners
4. THE DamageTracker SHALL automatically clean up expired damage records
5. THE DamageTracker SHALL provide methods to get the last attacker for environmental death handling

### Requirement 7: Listener Integration

**User Story:** As a developer, I want properly integrated event listeners so that the bounty system responds correctly to game events.

#### Acceptance Criteria

1. THE BountyPlayerDeathListener SHALL use ClaimHandler to determine winners
2. THE BountyPlayerDeathListener SHALL use RewardDistributor to distribute rewards
3. THE BountyPlayerDeathListener SHALL create ClaimResult objects for announcements
4. THE BountyPlayerDeathListener SHALL handle environmental deaths using last attacker data
5. THE BountyPlayerDeathListener SHALL clear damage tracking after processing claims

### Requirement 8: Hunter Statistics

**User Story:** As a player, I want my bounty hunting statistics tracked so that I can see my performance and compete on leaderboards.

#### Acceptance Criteria

1. WHEN a hunter claims a bounty, THE BountySystem SHALL update their total bounties claimed count
2. WHEN a hunter claims a bounty, THE BountySystem SHALL update their total earnings
3. THE BountySystem SHALL calculate and store the hunter's kill/death ratio
4. THE BountySystem SHALL provide leaderboard queries sorted by bounties claimed or total earnings
5. THE BountySystem SHALL calculate net profit (earnings minus bounties placed on the hunter)
