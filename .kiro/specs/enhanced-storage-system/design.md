# Enhanced Storage System Design

## Architecture Overview

### 1. Core Components

#### StorageManager
- **Purpose**: Central management of all storage operations
- **Responsibilities**:
  - Handle item collection from oneblock breaking
  - Manage storage capacity and tiers
  - Coordinate with GUI and command systems
  - Handle storage transactions

#### StorageRepository
- **Purpose**: Data persistence layer for storage items
- **Responsibilities**:
  - CRUD operations for stored items
  - Query optimization for large datasets
  - Transaction management

#### StorageGUI System
- **Purpose**: User interface for storage management
- **Components**:
  - `StorageMainView`: Overview and navigation
  - `StorageCategoryView`: Category-specific item display
  - `StorageItemDetailView`: Individual item management
  - `StorageSearchView`: Search and filter interface

### 2. Data Model

#### StoredItem Entity
```java
@Entity
public class StoredItem {
    private Long id;
    private UUID islandId;
    private Material material;
    private Long quantity;
    private String itemData; // NBT/meta data
    private StorageCategory category;
    private LocalDateTime lastUpdated;
    private String source; // "ONEBLOCK", "DEPOSIT", etc.
}
```

#### StorageCategory Entity
```java
@Entity
public class StorageCategory {
    private Long id;
    private String name;
    private String displayName;
    private Material icon;
    private List<Material> includedMaterials;
    private Integer sortOrder;
}
```

#### StorageTransaction Entity
```java
@Entity
public class StorageTransaction {
    private Long id;
    private UUID islandId;
    private UUID playerId;
    private TransactionType type; // DEPOSIT, WITHDRAW, AUTO_COLLECT
    private Material material;
    private Long quantity;
    private LocalDateTime timestamp;
    private String source;
}
```

### 3. GUI Design

#### Main Storage View Layout
```
[X][X][X][X][X][X][X][X][X]
[X][B][T][F][M][O][R][S][X]  // Categories
[X][I][I][I][I][I][I][I][X]  // Recent items
[X][I][I][I][I][I][I][I][X]  // Recent items  
[X][I][I][I][I][I][I][I][X]  // Recent items
[X][<][H][C][S][D][>][B][X]  // Navigation & Actions
```

Legend:
- B = Blocks, T = Tools, F = Food, M = Materials, O = Ores, R = Rare Items, S = Search
- I = Item slots showing recent/featured items
- < > = Previous/Next page navigation
- H = Help, C = Categories, S = Statistics, D = Deposit, B = Back

#### Category View Layout
```
[X][X][X][X][X][X][X][X][X]
[X][I][I][I][I][I][I][I][X]  // Items in category
[X][I][I][I][I][I][I][I][X]  // Items in category
[X][I][I][I][I][I][I][I][X]  // Items in category
[X][I][I][I][I][I][I][I][X]  // Items in category
[X][<][F][S][C][W][>][B][X]  // Navigation & Actions
```

Legend:
- I = Item slots for category items
- F = Filter, S = Sort, C = Categories, W = Withdraw All, B = Back

### 4. Integration Points

#### OneBlock Integration
```java
// In OneblockBlockBreakListener
@EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    // ... existing logic ...
    
    // Auto-store items if storage is enabled
    if (storageManager.isAutoStorageEnabled(island)) {
        List<ItemStack> drops = getBlockDrops(block, player);
        storageManager.autoStoreItems(island, drops, "ONEBLOCK");
    }
}
```

#### Infrastructure Integration
```java
// Update InfrastructureStatsView to show storage info
private void renderStorageStats(RenderContext render, Player player, IslandInfrastructure infra) {
    StorageStats stats = storageManager.getStorageStats(infra.getIslandId());
    // ... render storage statistics ...
}
```

### 5. Command System

#### Storage Commands
```java
@Command("island storage")
public class StorageCommand {
    
    @Subcommand("") // /island storage
    public void openStorage(Player player) {
        // Open main storage GUI
    }
    
    @Subcommand("category <category>") // /island storage category blocks
    public void openCategory(Player player, String category) {
        // Open specific category
    }
    
    @Subcommand("search <query>") // /island storage search diamond
    public void searchItems(Player player, String query) {
        // Open search results
    }
    
    @Subcommand("info") // /island storage info
    public void showInfo(Player player) {
        // Show storage statistics
    }
}
```

### 6. Performance Optimizations

#### Caching Strategy
- **L1 Cache**: In-memory cache for frequently accessed items
- **L2 Cache**: Redis cache for cross-server consistency
- **Cache Invalidation**: Smart invalidation on storage changes

#### Database Optimizations
- **Indexes**: Composite indexes on (islandId, category, material)
- **Partitioning**: Partition by island ID for large datasets
- **Batch Operations**: Bulk insert/update for multiple items

#### GUI Optimizations
- **Lazy Loading**: Load items only when GUI is opened
- **Pagination**: Limit items per page to prevent lag
- **Async Rendering**: Render GUI components asynchronously

## Implementation Plan

### Phase 1: Core Infrastructure
1. Create database entities and repositories
2. Implement StorageManager with basic operations
3. Set up caching layer
4. Create basic command structure

### Phase 2: GUI System
1. Implement main storage view
2. Create category views
3. Add search and filter functionality
4. Implement item detail views

### Phase 3: Integration
1. Integrate with oneblock breaking system
2. Update infrastructure views
3. Add notification system
4. Implement auto-storage features

### Phase 4: Enhancement
1. Add advanced search features
2. Implement storage analytics
3. Add export/import functionality
4. Performance optimization and testing