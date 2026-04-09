# Quest Task Handlers and Reward Distribution - Design

## Overview

This design document outlines the architecture for implementing quest task handlers and reward distribution in the RDQ quest system. The system uses an event-driven architecture where task handlers listen to Bukkit events and update quest progress, while reward distributors grant rewards upon quest completion.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Bukkit Event System                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Task Handler Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ KillMobs     │  │ CollectItems │  │ CraftItems   │ ... │
│  │ Handler      │  │ Handler      │  │ Handler      │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────┐
│              Quest Progress Tracker Service                  │
│  - Validates quest is active                                 │
│  - Validates task criteria                                   │
│  - Updates task progress                                     │
│  - Checks for task/quest completion                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Quest Event System                          │
│  - TaskCompleteEvent                                         │
│  - QuestCompleteEvent                                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Reward Distribution Service                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Currency     │  │ Experience   │  │ Item         │ ... │
│  │ Distributor  │  │ Distributor  │  │ Distributor  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Base Task Handler

```java
/**
 * Base class for all task handlers.
 * Provides common functionality for event handling and progress tracking.
 */
public abstract class BaseTaskHandler implements Listener {
    protected final QuestProgressTracker progressTracker;
    protected final QuestCacheManager cacheManager;
    protected final Logger logger;
    
    /**
     * Get the task type this handler processes.
     */
    protected abstract String getTaskType();
    
    /**
     * Check if the event should be processed.
     */
    protected abstract boolean shouldProcess(Event event, Player player);
    
    /**
     * Extract task criteria from the event.
     */
    protected abstract Map<String, Object> extractCriteria(Event event);
    
    /**
     * Validate player is eligible for progress.
     */
    protected boolean isEligible(Player player) {
        // Check creative mode, disabled worlds, etc.
    }
    
    /**
     * Update progress for all matching active quests.
     */
    protected void updateProgress(Player player, Map<String, Object> criteria, int amount) {
        // Get active quests from cache
        // Filter by task type and criteria
        // Update progress via tracker
    }
}
```

### 2. Specific Task Handlers

#### KillMobsTaskHandler

```java
public class KillMobsTaskHandler extends BaseTaskHandler {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !isEligible(killer)) {
            return;
        }
        
        if (!shouldProcess(event, killer)) {
            return;
        }
        
        Map<String, Object> criteria = extractCriteria(event);
        updateProgress(killer, criteria, 1);
    }
    
    @Override
    protected String getTaskType() {
        return "KILL_MOBS";
    }
    
    @Override
    protected boolean shouldProcess(Event event, Player player) {
        EntityDeathEvent deathEvent = (EntityDeathEvent) event;
        // Check if entity is a mob (not player, not armor stand, etc.)
        return deathEvent.getEntity() instanceof Monster || 
               deathEvent.getEntity() instanceof Animals;
    }
    
    @Override
    protected Map<String, Object> extractCriteria(Event event) {
        EntityDeathEvent deathEvent = (EntityDeathEvent) event;
        return Map.of(
            "entity_type", deathEvent.getEntityType().name(),
            "world", deathEvent.getEntity().getWorld().getName()
        );
    }
}
```

#### CollectItemsTaskHandler

```java
public class CollectItemsTaskHandler extends BaseTaskHandler {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (!isEligible(player)) {
            return;
        }
        
        ItemStack item = event.getItem().getItemStack();
        Map<String, Object> criteria = Map.of(
            "material", item.getType().name(),
            "amount", item.getAmount()
        );
        
        updateProgress(player, criteria, item.getAmount());
    }
    
    @Override
    protected String getTaskType() {
        return "COLLECT_ITEMS";
    }
}
```

#### CraftItemsTaskHandler

```java
public class CraftItemsTaskHandler extends BaseTaskHandler {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!isEligible(player)) {
            return;
        }
        
        ItemStack result = event.getRecipe().getResult();
        int amount = result.getAmount();
        
        // Handle shift-click crafting
        if (event.isShiftClick()) {
            amount = calculateShiftClickAmount(event);
        }
        
        Map<String, Object> criteria = Map.of(
            "material", result.getType().name()
        );
        
        updateProgress(player, criteria, amount);
    }
    
    @Override
    protected String getTaskType() {
        return "CRAFT_ITEMS";
    }
    
    private int calculateShiftClickAmount(CraftItemEvent event) {
        // Calculate max craftable amount based on inventory
        // This is complex due to Minecraft's crafting mechanics
    }
}
```

### 3. Quest Progress Tracker Enhancement

```java
public interface QuestProgressTracker {
    
    /**
     * Update task progress for a player.
     * 
     * @param playerId The player's UUID
     * @param taskType The type of task (e.g., "KILL_MOBS")
     * @param criteria The criteria to match (e.g., entity_type, material)
     * @param amount The amount to increment
     * @return CompletableFuture with list of completed task IDs
     */
    CompletableFuture<List<String>> updateTaskProgress(
        UUID playerId,
        String taskType,
        Map<String, Object> criteria,
        int amount
    );
    
    /**
     * Check if a task matches the given criteria.
     */
    boolean matchesCriteria(QuestTask task, Map<String, Object> criteria);
    
    /**
     * Get all active quests for a player with tasks of a specific type.
     */
    List<ActiveQuest> getActiveQuestsWithTaskType(UUID playerId, String taskType);
}
```

### 4. Reward Distribution Service

```java
public interface RewardDistributor {
    
    /**
     * Distribute all rewards for a completed quest.
     * 
     * @param player The player to reward
     * @param quest The completed quest
     * @return CompletableFuture with distribution results
     */
    CompletableFuture<RewardDistributionResult> distributeRewards(
        Player player,
        Quest quest
    );
    
    /**
     * Distribute a single reward.
     */
    CompletableFuture<Boolean> distributeReward(
        Player player,
        QuestReward reward
    );
}

public class RewardDistributorImpl implements RewardDistributor {
    
    private final JExEconomyBridge economy;
    private final PerkManagementService perkService;
    private final Logger logger;
    
    @Override
    public CompletableFuture<RewardDistributionResult> distributeRewards(
        Player player,
        Quest quest
    ) {
        List<QuestReward> rewards = quest.getRewards();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (QuestReward reward : rewards) {
            futures.add(distributeReward(player, reward));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<Boolean> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                
                return new RewardDistributionResult(
                    results.stream().allMatch(Boolean::booleanValue),
                    rewards,
                    results
                );
            });
    }
    
    @Override
    public CompletableFuture<Boolean> distributeReward(
        Player player,
        QuestReward reward
    ) {
        return switch (reward.getType()) {
            case CURRENCY -> distributeCurrency(player, reward);
            case EXPERIENCE -> distributeExperience(player, reward);
            case ITEM -> distributeItem(player, reward);
            case PERK -> distributePerk(player, reward);
            case COMMAND -> distributeCommand(player, reward);
            case TITLE -> distributeTitle(player, reward);
            default -> CompletableFuture.completedFuture(false);
        };
    }
    
    private CompletableFuture<Boolean> distributeCurrency(Player player, QuestReward reward) {
        JsonObject data = JsonParser.parseString(reward.getData()).getAsJsonObject();
        double amount = data.get("amount").getAsDouble();
        String currency = data.has("currency") ? 
            data.get("currency").getAsString() : "default";
        
        return economy.deposit(player.getUniqueId(), currency, amount)
            .thenApply(success -> {
                if (success) {
                    logger.info("Gave " + amount + " " + currency + " to " + player.getName());
                }
                return success;
            })
            .exceptionally(ex -> {
                logger.severe("Failed to give currency reward: " + ex.getMessage());
                return false;
            });
    }
    
    private CompletableFuture<Boolean> distributeExperience(Player player, QuestReward reward) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject data = JsonParser.parseString(reward.getData()).getAsJsonObject();
            int amount = data.get("amount").getAsInt();
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.giveExp(amount);
            });
            
            return true;
        });
    }
    
    private CompletableFuture<Boolean> distributeItem(Player player, QuestReward reward) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject data = JsonParser.parseString(reward.getData()).getAsJsonObject();
            String material = data.get("material").getAsString();
            int amount = data.get("amount").getAsInt();
            
            ItemStack item = new ItemStack(Material.valueOf(material), amount);
            
            // Apply NBT data if present
            if (data.has("nbt")) {
                // Apply NBT using UnifiedBuilderFactory
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                
                // Drop items that don't fit
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), drop);
                    }
                }
            });
            
            return true;
        });
    }
    
    private CompletableFuture<Boolean> distributePerk(Player player, QuestReward reward) {
        JsonObject data = JsonParser.parseString(reward.getData()).getAsJsonObject();
        String perkId = data.get("perk").getAsString();
        
        return perkService.activatePerk(player.getUniqueId(), perkId)
            .thenApply(success -> {
                if (success) {
                    logger.info("Activated perk " + perkId + " for " + player.getName());
                }
                return success;
            });
    }
    
    private CompletableFuture<Boolean> distributeCommand(Player player, QuestReward reward) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject data = JsonParser.parseString(reward.getData()).getAsJsonObject();
            String command = data.get("command").getAsString();
            
            // Replace placeholders
            command = command.replace("{player}", player.getName())
                           .replace("{uuid}", player.getUniqueId().toString());
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
            
            return true;
        });
    }
    
    private CompletableFuture<Boolean> distributeTitle(Player player, QuestReward reward) {
        JsonObject data = JsonParser.parseString(reward.getData()).getAsJsonObject();
        String titleId = data.get("title").getAsString();
        
        // Integrate with title system (if available)
        return CompletableFuture.completedFuture(true);
    }
}
```

### 5. Task Handler Manager

```java
public class TaskHandlerManager {
    
    private final Map<String, BaseTaskHandler> handlers = new HashMap<>();
    private final JavaPlugin plugin;
    private final QuestProgressTracker progressTracker;
    private final QuestCacheManager cacheManager;
    
    public void registerHandlers() {
        // Register all task handlers
        registerHandler(new KillMobsTaskHandler(progressTracker, cacheManager));
        registerHandler(new CollectItemsTaskHandler(progressTracker, cacheManager));
        registerHandler(new CraftItemsTaskHandler(progressTracker, cacheManager));
        registerHandler(new BreakBlocksTaskHandler(progressTracker, cacheManager));
        registerHandler(new PlaceBlocksTaskHandler(progressTracker, cacheManager));
        registerHandler(new ReachLocationTaskHandler(progressTracker, cacheManager));
        registerHandler(new TradeWithVillagerTaskHandler(progressTracker, cacheManager));
        registerHandler(new EnchantItemTaskHandler(progressTracker, cacheManager));
        registerHandler(new BreedAnimalsTaskHandler(progressTracker, cacheManager));
        registerHandler(new GainExperienceTaskHandler(progressTracker, cacheManager));
        registerHandler(new FishItemsTaskHandler(progressTracker, cacheManager));
    }
    
    private void registerHandler(BaseTaskHandler handler) {
        String taskType = handler.getTaskType();
        
        // Check if enabled in config
        if (!isHandlerEnabled(taskType)) {
            logger.info("Task handler " + taskType + " is disabled in configuration");
            return;
        }
        
        handlers.put(taskType, handler);
        plugin.getServer().getPluginManager().registerEvents(handler, plugin);
        logger.info("Registered task handler: " + taskType);
    }
    
    public void unregisterHandlers() {
        for (BaseTaskHandler handler : handlers.values()) {
            HandlerList.unregisterAll(handler);
        }
        handlers.clear();
        logger.info("Unregistered all task handlers");
    }
    
    private boolean isHandlerEnabled(String taskType) {
        return plugin.getConfig().getBoolean("task-handlers." + taskType + ".enabled", true);
    }
}
```

## Data Models

### RewardDistributionResult

```java
public record RewardDistributionResult(
    boolean allSuccessful,
    List<QuestReward> rewards,
    List<Boolean> results
) {
    public List<QuestReward> getSuccessfulRewards() {
        List<QuestReward> successful = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            if (results.get(i)) {
                successful.add(rewards.get(i));
            }
        }
        return successful;
    }
    
    public List<QuestReward> getFailedRewards() {
        List<QuestReward> failed = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            if (!results.get(i)) {
                failed.add(rewards.get(i));
            }
        }
        return failed;
    }
}
```

## Error Handling

### Task Handler Errors

1. **Event Processing Errors**: Catch and log all exceptions in event handlers to prevent crashes
2. **Progress Update Errors**: Retry once on failure, then log error
3. **Cache Miss Errors**: Fall back to database query if cache is unavailable

### Reward Distribution Errors

1. **Currency Errors**: Retry once, then log error and continue with other rewards
2. **Inventory Full**: Drop items on ground instead of failing
3. **Perk Activation Errors**: Log error and continue with other rewards
4. **Command Execution Errors**: Log error and continue with other rewards

## Testing Strategy

### Unit Tests

1. Test each task handler with mock events
2. Test reward distributors with mock services
3. Test criteria matching logic
4. Test progress calculation logic

### Integration Tests

1. Test full quest lifecycle (start → progress → complete → reward)
2. Test multiple concurrent players
3. Test edge cases (inventory full, insufficient permissions, etc.)
4. Test performance with many active quests

### Performance Tests

1. Benchmark task handler processing time
2. Benchmark reward distribution time
3. Test with 100+ concurrent players
4. Test with 1000+ active quests

## Configuration

### quest-system.yml

```yaml
task-handlers:
  KILL_MOBS:
    enabled: true
  COLLECT_ITEMS:
    enabled: true
  CRAFT_ITEMS:
    enabled: true
  BREAK_BLOCKS:
    enabled: true
  PLACE_BLOCKS:
    enabled: true
  REACH_LOCATION:
    enabled: true
  TRADE_WITH_VILLAGER:
    enabled: true
  ENCHANT_ITEM:
    enabled: true
  BREED_ANIMALS:
    enabled: true
  GAIN_EXPERIENCE:
    enabled: true
  FISH_ITEMS:
    enabled: true

rewards:
  # Retry failed currency rewards
  retry-currency: true
  
  # Drop items on ground if inventory full
  drop-on-full: true
  
  # Continue distributing rewards even if one fails
  continue-on-error: true

performance:
  # Skip task handler processing for players with no active quests
  skip-inactive-players: true
  
  # Use cached quest data instead of database queries
  use-cache: true
  
  # Maximum time (ms) to spend processing a single event
  max-processing-time: 5
```

## Migration Strategy

1. Implement base task handler class
2. Implement 2-3 core task handlers (KILL_MOBS, COLLECT_ITEMS, CRAFT_ITEMS)
3. Test with existing quests
4. Implement remaining task handlers
5. Implement reward distribution service
6. Test full quest lifecycle
7. Deploy to production

## Performance Considerations

1. **Cache Active Quests**: Keep active quests in memory to avoid database queries
2. **Async Processing**: Process events asynchronously to avoid blocking main thread
3. **Batch Updates**: Batch progress updates to reduce database writes
4. **Early Exit**: Skip processing if player has no active quests
5. **Efficient Criteria Matching**: Use indexed lookups instead of linear scans

