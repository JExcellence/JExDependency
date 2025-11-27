# Design Document

## Overview

The bounty system rebuild modernizes the RDQ bounty feature using the inventory-framework library while maintaining all existing functionality. The system allows players to place bounties on other players with item and currency rewards, automatically claims bounties when targets are eliminated, tracks hunter statistics, and provides comprehensive UI views for bounty management.

The design emphasizes modern Java patterns (records, sealed interfaces, Optional, immutable collections), asynchronous database operations, edition-specific features (free vs premium), and comprehensive testing. All user-facing messages use JExTranslate for internationalization.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ BountyMain   │  │ BountyCreate │  │ BountyList   │      │
│  │ View         │  │ View         │  │ View         │ ...  │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                     Service Layer                            │
│         ┌──────────────────┴──────────────────┐              │
│         │                                     │              │
│  ┌──────▼──────────┐              ┌──────────▼──────────┐   │
│  │ PremiumBounty   │              │ FreeBounty          │   │
│  │ Service         │              │ Service             │   │
│  └─────────────────┘              └─────────────────────┘   │
│         │                                     │              │
│         └──────────────────┬──────────────────┘              │
│                            │                                 │
└────────────────────────────┼─────────────────────────────────┘
                             │
┌────────────────────────────┼─────────────────────────────────┐
│                  Repository Layer                            │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │ RBounty          │  │ BountyHunter     │                 │
│  │ Repository       │  │ StatsRepository  │                 │
│  └──────────────────┘  └──────────────────┘                 │
│         │                       │                            │
└─────────┼───────────────────────┼────────────────────────────┘
          │                       │
┌─────────┼───────────────────────┼────────────────────────────┐
│         │    Database Layer     │                            │
│  ┌──────▼──────────┐  ┌─────────▼──────────┐                │
│  │ RBounty         │  │ BountyHunterStats  │                │
│  │ Entity          │  │ Entity             │                │
│  └─────────────────┘  └────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **View Layer**: Handles user interactions through inventory-framework views
2. **Service Layer**: Provides business logic and edition-specific implementations
3. **Repository Layer**: Manages database access with async operations
4. **Entity Layer**: Represents persistent data models

### Edition Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    BountyService (sealed)                    │
└─────────────────────────────────────────────────────────────┘
                             │
                ┌────────────┴────────────┐
                │                         │
┌───────────────▼──────────┐  ┌──────────▼──────────────────┐
│  PremiumBountyService    │  │  FreeBountyService          │
│  - Full DB persistence   │  │  - In-memory storage        │
│  - Unlimited bounties    │  │  - 1 active bounty limit    │
│  - Dynamic creation      │  │  - Static bounties support  │
└──────────────────────────┘  └─────────────────────────────┘
```

## Components and Interfaces

### Core Service Interface

```java
public sealed interface BountyService permits PremiumBountyService, FreeBountyService {
    
    // Query Operations
    CompletableFuture<List<Bounty>> getAllBounties(int page, int pageSize);
    CompletableFuture<Optional<Bounty>> getBountyByTarget(UUID targetUuid);
    CompletableFuture<List<Bounty>> getBountiesByCommissioner(UUID commissionerUuid);
    CompletableFuture<Integer> getTotalBountyCount();
    
    // Mutation Operations
    CompletableFuture<Bounty> createBounty(BountyCreationRequest request);
    CompletableFuture<Boolean> deleteBounty(Long bountyId);
    CompletableFuture<Bounty> claimBounty(Long bountyId, UUID hunterUuid);
    CompletableFuture<Void> expireBounty(Long bountyId);
    
    // Hunter Statistics
    CompletableFuture<Optional<HunterStats>> getHunterStats(UUID playerUuid);
    CompletableFuture<List<HunterStats>> getTopHunters(int limit, HunterSortOrder sortOrder);
    CompletableFuture<Integer> getHunterRank(UUID playerUuid);
    
    // Edition Capabilities
    boolean isPremium();
    int getMaxBountiesPerPlayer();
    int getMaxRewardItems();
    boolean canCreateBounty(Player player);
}
```

### Data Transfer Objects (Records)

```java
// Immutable bounty data transfer object
public record Bounty(
    Long id,
    UUID targetUuid,
    String targetName,
    UUID commissionerUuid,
    String commissionerName,
    Set<RewardItem> rewardItems,
    Map<String, Double> rewardCurrencies,
    double totalEstimatedValue,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    BountyStatus status,
    Optional<ClaimInfo> claimInfo
) {
    public boolean isActive() {
        return status == BountyStatus.ACTIVE && 
               (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}

// Bounty creation request
public record BountyCreationRequest(
    UUID targetUuid,
    UUID commissionerUuid,
    Set<RewardItem> rewardItems,
    Map<String, Double> rewardCurrencies,
    Optional<LocalDateTime> customExpiration
) {
    public BountyCreationRequest {
        Objects.requireNonNull(targetUuid, "target cannot be null");
        Objects.requireNonNull(commissionerUuid, "commissioner cannot be null");
        rewardItems = Set.copyOf(rewardItems); // Defensive copy
        rewardCurrencies = Map.copyOf(rewardCurrencies); // Defensive copy
    }
}

// Claim information
public record ClaimInfo(
    UUID hunterUuid,
    String hunterName,
    LocalDateTime claimedAt,
    ClaimMode claimMode
) {}

// Hunter statistics
public record HunterStats(
    UUID playerUuid,
    String playerName,
    int bountiesClaimed,
    double totalRewardValue,
    double highestBountyValue,
    Optional<LocalDateTime> lastClaimTime,
    int rank
) {}

// Reward item wrapper
public record RewardItem(
    ItemStack item,
    int amount,
    double estimatedValue
) {
    public RewardItem {
        Objects.requireNonNull(item, "item cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
    }
}
```

### Enumerations

```java
public enum BountyStatus {
    ACTIVE,
    CLAIMED,
    EXPIRED,
    CANCELLED
}

public enum ClaimMode {
    LAST_HIT,      // Killer gets full bounty
    MOST_DAMAGE,   // Player with most damage gets bounty
    DAMAGE_SPLIT   // Bounty split among damage dealers
}

public enum DistributionMode {
    INSTANT,   // Items added to inventory immediately
    VIRTUAL,   // Items stored in virtual storage
    DROP,      // Items dropped at death location
    CHEST      // Items placed in chest at death location
}

public enum HunterSortOrder {
    BOUNTIES_CLAIMED,
    TOTAL_REWARD_VALUE,
    HIGHEST_BOUNTY_VALUE,
    RECENT_CLAIMS
}
```

### View Components

All views extend `BaseView` from RPlatform and use inventory-framework's state management:

```java
public class BountyMainView extends BaseView {
    // State management
    private final State<RDQ> rdq = initialState("plugin");
    
    // View configuration
    @Override
    protected String getKey() { return "bounty_main_ui"; }
    
    @Override
    protected int getSize() { return 3; }
    
    @Override
    protected String[] getLayout() {
        return new String[] {
            "XXXXXXXXX",
            "XXcblsmXX",
            "XXXXXXXXX"
        };
    }
    
    // Rendering
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderNavigationButtons(render, player);
    }
}
```

### Repository Interfaces

```java
public interface RBountyRepository extends JpaRepository<RBounty, Long> {
    
    @Query("SELECT b FROM RBounty b WHERE b.targetUniqueId = :targetUuid AND b.active = true")
    Optional<RBounty> findActiveByTarget(@Param("targetUuid") UUID targetUuid);
    
    @Query("SELECT b FROM RBounty b WHERE b.commissionerUniqueId = :commissionerUuid")
    List<RBounty> findByCommissioner(@Param("commissionerUuid") UUID commissionerUuid);
    
    @Query("SELECT b FROM RBounty b WHERE b.active = true ORDER BY b.createdAt DESC")
    Page<RBounty> findAllActive(Pageable pageable);
    
    @Query("SELECT b FROM RBounty b WHERE b.expiresAt < :now AND b.active = true")
    List<RBounty> findExpired(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(b) FROM RBounty b WHERE b.active = true")
    int countActive();
}

public interface BountyHunterStatsRepository extends JpaRepository<BountyHunterStats, Long> {
    
    Optional<BountyHunterStats> findByPlayerUniqueId(UUID playerUuid);
    
    @Query("SELECT s FROM BountyHunterStats s ORDER BY s.bountiesClaimed DESC")
    List<BountyHunterStats> findTopByBountiesClaimed(Pageable pageable);
    
    @Query("SELECT s FROM BountyHunterStats s ORDER BY s.totalRewardValue DESC")
    List<BountyHunterStats> findTopByRewardValue(Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM BountyHunterStats s WHERE s.bountiesClaimed > " +
           "(SELECT s2.bountiesClaimed FROM BountyHunterStats s2 WHERE s2.playerUniqueId = :playerUuid)")
    int countPlayersAbove(@Param("playerUuid") UUID playerUuid);
}
```

## Data Models

### Entity Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                         RBounty                              │
├─────────────────────────────────────────────────────────────┤
│ - id: Long (PK)                                             │
│ - targetUniqueId: UUID                                      │
│ - commissionerUniqueId: UUID                                │
│ - expiresAt: LocalDateTime                                  │
│ - active: boolean                                           │
│ - claimedBy: UUID                                           │
│ - claimedAt: LocalDateTime                                  │
│ - totalEstimatedValue: double                               │
│ - rewards: List<BountyReward>                               │
│ - rewardItems: Set<RewardItem>                              │
│ - rewardCurrencies: Map<String, Double>                     │
└─────────────────────────────────────────────────────────────┘
                             │
                             │ 1:N
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      BountyReward                            │
├─────────────────────────────────────────────────────────────┤
│ - id: Long (PK)                                             │
│ - bountyId: Long (FK)                                       │
│ - rewardType: String                                        │
│ - rewardData: String                                        │
│ - estimatedValue: double                                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   BountyHunterStats                          │
├─────────────────────────────────────────────────────────────┤
│ - id: Long (PK)                                             │
│ - playerUniqueId: UUID (UNIQUE)                             │
│ - bountiesClaimed: int                                      │
│ - totalRewardValue: double                                  │
│ - highestBountyValue: double                                │
│ - lastClaimTimestamp: Long                                  │
└─────────────────────────────────────────────────────────────┘
```

### Database Indexes

```sql
-- RBounty indexes
CREATE INDEX idx_rbounty_target ON r_bounty(target_unique_id);
CREATE INDEX idx_rbounty_commissioner ON r_bounty(commissioner_unique_id);
CREATE INDEX idx_rbounty_active ON r_bounty(active);
CREATE INDEX idx_rbounty_expires ON r_bounty(expires_at);

-- BountyHunterStats indexes
CREATE UNIQUE INDEX idx_hunter_player ON r_bounty_hunter_stats(player_unique_id);
CREATE INDEX idx_hunter_claimed ON r_bounty_hunter_stats(bounties_claimed DESC);
CREATE INDEX idx_hunter_value ON r_bounty_hunter_stats(total_reward_value DESC);
```

### State Management in Views

Views use inventory-framework's state system for reactive UI updates:

```java
// Immutable state (read-only)
private final State<RDQ> rdq = initialState("plugin");
private final State<Optional<Bounty>> bounty = initialState("bounty");

// Mutable state (can be updated)
private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
private final MutableState<Set<RewardItem>> rewardItems = initialState("reward_items");
private final MutableState<Map<String, Double>> rewardCurrencies = initialState("reward_currencies");

// Computed state (derived from other states)
private final State<ItemStack> targetButton = computedState(ctx -> {
    var player = ctx.getPlayer();
    var target = this.target.get(ctx);
    return createTargetButton(player, target);
});

// Watching state changes
render.slot(12)
    .watch(this.targetButton)
    .renderWith(() -> this.targetButton.get(render))
    .onClick(this::handleTargetSelection);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

