# Quest Reward System - Complete Rewrite Specification

## Executive Summary

This specification outlines a complete rewrite of the quest reward system to properly integrate with RPlatform's reward architecture while providing a modern, extensible, and feature-rich implementation that allows any plugin to extend functionality.

## Current State Analysis

### What Exists
- ✅ Quest entities with reward relationships
- ✅ QuestReward, QuestTaskReward, QuestCategoryReward entities
- ✅ BaseReward entity (wraps RPlatform AbstractReward)
- ✅ RPlatform reward implementations (Currency, Item, Experience, Command, Title)
- ✅ Reward JSON storage in quest entities
- ❌ Reward creation from YAML (disabled)
- ❌ Reward distribution system (incomplete)
- ❌ Manual collection system (not implemented)
- ❌ Extensibility API (not implemented)

### Critical Issues Identified

1. **Constructor Mismatches**
   - ItemReward: Requires `ItemStack`, not material string
   - ExperienceReward: Requires `ExperienceType` enum, not boolean
   - CommandReward: Requires `delayTicks` parameter
   - TitleReward: Uses titleId/displayName, not title/subtitle/timing

2. **IconSection Complexity**
   - Uses config mapper pattern with `EvaluationEnvironmentBuilder`
   - Cannot be instantiated with simple constructor
   - Needs alternative approach or proper config integration

3. **No Extensibility**
   - No API for plugins to register custom rewards
   - No event system for reward processing
   - No hooks for custom distribution logic

## Goals

### Primary Goals
1. **Proper RPlatform Integration** - Correctly use RPlatform reward architecture
2. **Complete Functionality** - All reward types work correctly
3. **Full Extensibility** - Plugins can add custom rewards, handlers, and logic
4. **Modern Architecture** - Event-driven, async, well-documented

### Secondary Goals
1. **Beautiful GUIs** - Rich reward displays with previews
2. **Manual Collection** - Optional reward collection system
3. **Live Updates** - Real-time progress and reward notifications
4. **Performance** - Efficient caching and async operations

## Architecture Design

### Core Components

#### 1. Reward Factory System

**Purpose**: Convert YAML configuration to RPlatform AbstractReward instances

**Components**:
- `RewardFactory` - Main factory interface
- `RewardFactoryRegistry` - Registry for custom factories
- Built-in factories for each reward type
- Plugin API for custom reward factories

**Design**:
```java
public interface RewardFactory<T extends AbstractReward> {
    String getType();
    T create(Map<String, Object> config);
    IconSection createIcon(Map<String, Object> config);
    void validate(Map<String, Object> config);
}

public class RewardFactoryRegistry {
    void register(String type, RewardFactory<?> factory);
    RewardFactory<?> get(String type);
    Set<String> getRegisteredTypes();
}
```

#### 2. Reward Distribution System

**Purpose**: Handle reward granting with extensibility

**Components**:
- `RewardDistributor` - Main distribution service
- `RewardDistributionStrategy` - Strategy interface for custom logic
- `RewardDistributionEvent` - Event for plugins to intercept
- `RewardCollectionService` - Manual collection system

**Design**:
```java
public interface RewardDistributionStrategy {
    CompletableFuture<RewardDistributionResult> distribute(
        Player player, 
        List<QuestReward> rewards,
        DistributionContext context
    );
}

public class RewardDistributionEvent extends Event implements Cancellable {
    Player player;
    List<QuestReward> rewards;
    DistributionContext context;
    // Plugins can cancel or modify
}
```

#### 3. Manual Collection System

**Purpose**: Optional reward collection with expiry

**Components**:
- `PendingReward` entity
- `PendingRewardRepository`
- `RewardCollectionService`
- `RewardCollectionView` GUI
- Auto-expiry task

**Design**:
```java
@Entity
public class PendingReward extends BaseEntity {
    UUID playerId;
    Quest quest;
    QuestReward reward;
    Instant createdAt;
    Instant expiresAt;
    boolean collected;
}
```

#### 4. Extensibility API

**Purpose**: Allow plugins to extend all aspects

**Extension Points**:
1. **Custom Reward Types** - Register new reward implementations
2. **Custom Distribution Logic** - Override how rewards are granted
3. **Custom Collection UI** - Replace or enhance collection GUI
4. **Event Hooks** - Listen to all reward-related events
5. **Custom Validators** - Add validation for custom rewards

## Implementation Phases

### Phase 1: Study & Analysis (30 minutes)

**Objectives**:
- Understand rank system reward implementation
- Document RPlatform reward patterns
- Identify reusable patterns

**Tasks**:
1. Read `RRankReward.java` implementation
2. Read rank config loader reward creation
3. Document IconSection usage in ranks
4. Create pattern documentation

**Deliverables**:
- `RPLATFORM_REWARD_PATTERNS.md` - Documented patterns
- Code examples from rank system

### Phase 2: Core Factory Implementation (1 hour)

**Objectives**:
- Create proper reward factory system
- Support all RPlatform reward types
- Enable extensibility

**Tasks**:
1. Create `RewardFactory` interface
2. Create `RewardFactoryRegistry`
3. Implement built-in factories:
   - `CurrencyRewardFactory`
   - `ItemRewardFactory`
   - `ExperienceRewardFactory`
   - `CommandRewardFactory`
   - `TitleRewardFactory`
4. Handle IconSection properly
5. Add validation

**Deliverables**:
- Working factory system
- All reward types supported
- Unit tests

### Phase 3: Quest Integration (45 minutes)

**Objectives**:
- Integrate factory with quest loading
- Create reward entities from YAML
- Test database persistence

**Tasks**:
1. Update `QuestSystemFactory`
2. Create rewards during quest load
3. Create task rewards during task load
4. Test database persistence
5. Verify eager loading works

**Deliverables**:
- Rewards created from YAML
- Database tables populated
- No lazy loading issues

### Phase 4: Distribution System (1 hour)

**Objectives**:
- Implement reward distribution
- Add extensibility hooks
- Support auto and manual modes

**Tasks**:
1. Create `RewardDistributionStrategy` interface
2. Implement `AutoDistributionStrategy`
3. Implement `ManualCollectionStrategy`
4. Add `RewardDistributionEvent`
5. Update `QuestProgressTrackerImpl` for task rewards
6. Update `QuestRewardDistributor` for quest rewards
7. Add configuration options

**Deliverables**:
- Working distribution system
- Both auto and manual modes
- Event system functional

### Phase 5: Manual Collection System (1 hour)

**Objectives**:
- Implement pending reward system
- Create collection GUI
- Add expiry mechanism

**Tasks**:
1. Create `PendingReward` entity
2. Create `PendingRewardRepository`
3. Create `RewardCollectionService`
4. Create `RewardCollectionView` GUI
5. Implement expiry task
6. Add notifications

**Deliverables**:
- Working collection system
- Beautiful GUI
- Auto-expiry functional

### Phase 6: Enhanced GUIs (45 minutes)

**Objectives**:
- Beautiful reward displays
- Rich information
- Intuitive interactions

**Tasks**:
1. Update `QuestDetailView` with reward display
2. Update `QuestListView` with reward preview
3. Add reward tooltips
4. Add collection status indicators
5. Add animations/effects

**Deliverables**:
- Enhanced quest GUIs
- Beautiful reward displays
- Intuitive UX

### Phase 7: Live Updates (30 minutes)

**Objectives**:
- Real-time progress updates
- Reward notifications
- Action bar integration

**Tasks**:
1. Add action bar progress updates
2. Add reward grant notifications
3. Add collection reminders
4. Integrate with existing sidebar

**Deliverables**:
- Live progress updates
- Rich notifications
- Seamless UX

### Phase 8: Testing & Polish (1 hour)

**Objectives**:
- Comprehensive testing
- Bug fixes
- Documentation

**Tasks**:
1. Test all reward types
2. Test auto distribution
3. Test manual collection
4. Test expiry system
5. Test extensibility API
6. Write developer documentation
7. Write user documentation

**Deliverables**:
- Fully tested system
- Complete documentation
- Example custom reward plugin

## Technical Specifications

### Reward Factory Pattern

```java
/**
 * Factory for creating ItemReward instances from YAML configuration.
 */
public class ItemRewardFactory implements RewardFactory<ItemReward> {
    
    @Override
    public String getType() {
        return "ITEM";
    }
    
    @Override
    public ItemReward create(Map<String, Object> config) {
        // Parse material
        String materialName = (String) config.get("material");
        Material material = Material.valueOf(materialName.toUpperCase());
        
        // Parse amount
        int amount = ((Number) config.getOrDefault("amount", 1)).intValue();
        
        // Create ItemStack
        ItemStack item = new ItemStack(material);
        
        // Apply custom name if present
        if (config.containsKey("name")) {
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text((String) config.get("name")));
            item.setItemMeta(meta);
        }
        
        // Apply lore if present
        if (config.containsKey("lore")) {
            ItemMeta meta = item.getItemMeta();
            List<String> loreStrings = (List<String>) config.get("lore");
            List<Component> lore = loreStrings.stream()
                .map(Component::text)
                .collect(Collectors.toList());
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return new ItemReward(item, amount);
    }
    
    @Override
    public IconSection createIcon(Map<String, Object> config) {
        // Use config mapper to create IconSection
        // OR create a simple wrapper that doesn't require config mapper
        return IconSectionFactory.create(config);
    }
    
    @Override
    public void validate(Map<String, Object> config) {
        if (!config.containsKey("material")) {
            throw new IllegalArgumentException("Item reward requires 'material'");
        }
        // Additional validation
    }
}
```

### Distribution Strategy Pattern

```java
/**
 * Strategy for automatic reward distribution.
 */
public class AutoDistributionStrategy implements RewardDistributionStrategy {
    
    @Override
    public CompletableFuture<RewardDistributionResult> distribute(
        Player player,
        List<QuestReward> rewards,
        DistributionContext context
    ) {
        // Fire pre-distribution event
        RewardDistributionEvent event = new RewardDistributionEvent(player, rewards, context);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(
                RewardDistributionResult.cancelled()
            );
        }
        
        // Distribute each reward
        List<CompletableFuture<Boolean>> futures = rewards.stream()
            .map(reward -> reward.grant(player))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                long successCount = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Boolean::booleanValue)
                    .count();
                
                return new RewardDistributionResult(
                    successCount,
                    rewards.size() - successCount
                );
            });
    }
}
```

### Extensibility API

```java
/**
 * API for plugins to register custom rewards.
 */
public class QuestRewardAPI {
    
    /**
     * Register a custom reward factory.
     *
     * @param factory the reward factory
     */
    public static void registerRewardFactory(RewardFactory<?> factory) {
        RewardFactoryRegistry.getInstance().register(factory.getType(), factory);
    }
    
    /**
     * Register a custom distribution strategy.
     *
     * @param strategy the distribution strategy
     */
    public static void registerDistributionStrategy(RewardDistributionStrategy strategy) {
        RewardDistributor.getInstance().setStrategy(strategy);
    }
    
    /**
     * Create a pending reward for manual collection.
     *
     * @param player the player
     * @param quest the quest
     * @param reward the reward
     * @param expirySeconds seconds until expiry
     * @return the pending reward
     */
    public static CompletableFuture<PendingReward> createPendingReward(
        Player player,
        Quest quest,
        QuestReward reward,
        long expirySeconds
    ) {
        return RewardCollectionService.getInstance()
            .createPending(player.getUniqueId(), quest, reward, expirySeconds);
    }
}
```

### Example Custom Reward

```java
/**
 * Example custom reward that grants a random item from a pool.
 */
public class RandomItemReward extends AbstractReward {
    
    private final List<ItemStack> itemPool;
    
    @JsonCreator
    public RandomItemReward(
        @JsonProperty("items") List<ItemStack> itemPool
    ) {
        this.itemPool = itemPool;
    }
    
    @Override
    public String getTypeId() {
        return "RANDOM_ITEM";
    }
    
    @Override
    public CompletableFuture<Boolean> grant(Player player) {
        ItemStack randomItem = itemPool.get(
            ThreadLocalRandom.current().nextInt(itemPool.size())
        );
        
        return CompletableFuture.supplyAsync(() -> {
            player.getInventory().addItem(randomItem);
            return true;
        });
    }
    
    @Override
    public double getEstimatedValue() {
        return itemPool.stream()
            .mapToDouble(item -> 10.0) // Estimate
            .average()
            .orElse(0.0);
    }
}

/**
 * Factory for RandomItemReward.
 */
public class RandomItemRewardFactory implements RewardFactory<RandomItemReward> {
    
    @Override
    public String getType() {
        return "RANDOM_ITEM";
    }
    
    @Override
    public RandomItemReward create(Map<String, Object> config) {
        List<Map<String, Object>> itemConfigs = 
            (List<Map<String, Object>>) config.get("items");
        
        List<ItemStack> items = itemConfigs.stream()
            .map(this::createItemStack)
            .collect(Collectors.toList());
        
        return new RandomItemReward(items);
    }
    
    private ItemStack createItemStack(Map<String, Object> config) {
        // Parse item configuration
        // ...
    }
}

// Register in plugin
public class MyCustomRewardsPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Register custom reward factory
        QuestRewardAPI.registerRewardFactory(new RandomItemRewardFactory());
        
        getLogger().info("Custom rewards registered!");
    }
}
```

## Configuration

### quest-system.yml

```yaml
rewards:
  # Auto-collect rewards immediately on completion
  auto_collect: true
  
  # Show reward preview before collecting
  show_preview: true
  
  # Time in seconds before uncollected rewards expire
  collection_timeout_seconds: 300
  
  # Notify players about pending rewards
  notify_pending: true
  
  # Notification interval in seconds
  notify_interval_seconds: 60
  
  # Distribution strategy
  # Options: AUTO, MANUAL, CUSTOM
  distribution_strategy: AUTO
  
  # Custom strategy class (if CUSTOM)
  custom_strategy_class: null
```

### Quest YAML (Enhanced)

```yaml
rewards:
  main_currency:
    type: CURRENCY
    currency: coins
    amount: 100.0
    
  bonus_item:
    type: ITEM
    material: DIAMOND
    amount: 5
    name: "<gradient:#00FFFF:#0080FF>Quest Diamond</gradient>"
    lore:
      - "<gray>Earned from completing</gray>"
      - "<yellow>{quest_name}</yellow>"
    
  experience:
    type: EXPERIENCE
    amount: 50
    type: POINTS  # or LEVELS
    
  command:
    type: COMMAND
    command: "give {player} special_item 1"
    as_player: false
    delay_ticks: 0
    
  title:
    type: TITLE
    title_id: "quest_master"
    display_name: "Quest Master"
    
  # Custom reward (if plugin registered)
  random_bonus:
    type: RANDOM_ITEM
    items:
      - material: EMERALD
        amount: 1
      - material: GOLD_INGOT
        amount: 3
      - material: IRON_INGOT
        amount: 5
```

## Translation Keys

```yaml
reward:
  type:
    currency: "<gold>Currency</gold>"
    item: "<aqua>Item</aqua>"
    experience: "<green>Experience</green>"
    command: "<yellow>Command</yellow>"
    title: "<light_purple>Title</light_purple>"
  
  preview:
    title: "<gradient:#FFD700:#FFA500>Quest Rewards</gradient>"
    subtitle: "<gray>You will receive:</gray>"
    
  granted:
    title: "<green>✓ Rewards Granted!</green>"
    subtitle: "<gray>Check your inventory</gray>"
    
  collection:
    title: "<gradient:#FFD700:#FFA500>Collect Rewards</gradient>"
    pending: "<yellow>You have {count} pending rewards</yellow>"
    expires: "<red>⏰ Expires in {time}</red>"
    click: "<green>▶ Click to collect!</green>"
    collected: "<green>✓ Collected {reward}!</green>"
    expired: "<red>✗ Reward expired</red>"
    
  notification:
    pending: "<yellow>⚠ You have {count} uncollected rewards!</yellow>"
    expiring_soon: "<red>⚠ Rewards expiring in {time}!</red>"
```

## Success Criteria

### Must Have
- ✅ All RPlatform reward types work correctly
- ✅ Rewards created from YAML on server start
- ✅ Rewards distributed on quest/task completion
- ✅ Auto-collect mode works
- ✅ Manual collection mode works
- ✅ Extensibility API functional
- ✅ Zero compilation errors
- ✅ Complete documentation

### Should Have
- ✅ Beautiful reward GUIs
- ✅ Live progress updates
- ✅ Reward expiry system
- ✅ Example custom reward plugin
- ✅ Performance optimizations

### Nice to Have
- ⭐ Reward animations
- ⭐ Sound effects
- ⭐ Particle effects
- ⭐ Reward trading system
- ⭐ Reward marketplace

## Risks & Mitigation

### Risk 1: IconSection Complexity
**Mitigation**: Create simple wrapper or use config mapper properly

### Risk 2: RPlatform API Changes
**Mitigation**: Abstract reward creation behind factory interface

### Risk 3: Performance Issues
**Mitigation**: Async operations, caching, batch processing

### Risk 4: Extensibility Complexity
**Mitigation**: Clear API, good documentation, examples

## Timeline

- **Phase 1**: 30 minutes
- **Phase 2**: 1 hour
- **Phase 3**: 45 minutes
- **Phase 4**: 1 hour
- **Phase 5**: 1 hour
- **Phase 6**: 45 minutes
- **Phase 7**: 30 minutes
- **Phase 8**: 1 hour

**Total**: ~6.5 hours

## Next Steps

1. Review and approve this specification
2. Schedule implementation session
3. Begin Phase 1: Study & Analysis
4. Implement phases sequentially
5. Test thoroughly
6. Deploy and monitor

---

**Document Version**: 1.0  
**Created**: Current Session  
**Status**: Ready for Implementation
