# Design Document

## Overview

This design document describes the approach for rebuilding the RDQ2 rank system with modern Java practices, reduced verbosity, and improved maintainability. The rebuild will modernize entities, views, services, managers, and supporting classes while maintaining full backward compatibility with existing data and configurations.

## Architecture

### High-Level Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                     Rank System Architecture                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │  View Layer  │─────▶│   Manager    │                    │
│  │  (GUI/UX)    │      │    Layer     │                    │
│  └──────────────┘      └──────┬───────┘                    │
│                               │                              │
│                               ▼                              │
│                        ┌──────────────┐                     │
│                        │   Service    │                     │
│                        │    Layer     │                     │
│                        └──────┬───────┘                     │
│                               │                              │
│                               ▼                              │
│                        ┌──────────────┐                     │
│                        │  Repository  │                     │
│                        │    Layer     │                     │
│                        └──────┬───────┘                     │
│                               │                              │
│                               ▼                              │
│                        ┌──────────────┐                     │
│                        │   Database   │                     │
│                        │   (JPA/H2)   │                     │
│                        └──────────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

### Package Organization

```
com.raindropcentral.rdq2
├── database.entity.rank/          # Entity classes
│   ├── RPlayerRank
│   ├── RPlayerRankPath
│   ├── RPlayerRankUpgradeProgress
│   ├── RRank
│   ├── RRankTree
│   └── RRankUpgradeRequirement
│
├── service.rank/                  # Business logic services
│   ├── RankPathService
│   └── RankUpgradeProgressService
│
├── manager.rank/                  # High-level coordinators
│   └── RankRequirementProgressManager
│
├── view.rank/                     # GUI views
│   ├── view/                      # Main view classes
│   │   ├── RankMainView
│   │   ├── RankTreeOverviewView
│   │   ├── RankPathOverview
│   │   ├── RankPathRankRequirementOverview
│   │   └── RankRequirementDetailView
│   ├── hierarchy/                 # Rank tree structure
│   │   ├── RankNode
│   │   └── RankHierarchyBuilder
│   ├── grid/                      # Grid positioning
│   │   ├── GridPosition
│   │   ├── GridSlotMapper
│   │   └── RankPositionCalculator
│   ├── cache/                     # View caching
│   │   └── RankDataCache
│   └── interaction/               # User interaction handlers
│       ├── RankClickHandler
│       └── RankProgressionManager
│
├── type/                          # Enums and types
│   └── ERankStatus
│
└── utility.rank/                  # Utility classes
    └── RankRequirementContext
```

## Components and Interfaces

### 1. Entity Layer Modernization

#### RPlayerRank Entity

**Current Issues:**
- Verbose getter/setter methods
- Excessive helper methods
- Redundant null checks
- Verbose toString implementation

**Modern Design:**
```java
@Entity
@Table(name = "r_player_rank", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "rank_tree_id"}))
public class RPlayerRank extends AbstractEntity {
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_rank_id", nullable = false)
    private RRank currentRank;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_tree_id")
    private RRankTree rankTree;
    
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    
    protected RPlayerRank() {}
    
    public RPlayerRank(@NotNull RDQPlayer player, @NotNull RRank currentRank, @NotNull RRankTree rankTree) {
        this.player = Objects.requireNonNull(player);
        this.currentRank = Objects.requireNonNull(currentRank);
        this.rankTree = Objects.requireNonNull(rankTree);
    }
    
    // Standard getters/setters with minimal logic
    public RDQPlayer getPlayer() { return player; }
    public RRank getCurrentRank() { return currentRank; }
    public RRankTree getRankTree() { return rankTree; }
    public boolean isActive() { return active; }
    
    public void setCurrentRank(@NotNull RRank rank) { 
        this.currentRank = Objects.requireNonNull(rank); 
    }
    public void setActive(boolean active) { this.active = active; }
    
    // Only essential helper methods
    public boolean belongsToTree(@NotNull RRankTree tree) {
        return rankTree != null && rankTree.equals(tree);
    }
}
```

**Key Improvements:**
- Removed verbose JavaDoc for obvious methods
- Simplified constructors with Objects.requireNonNull
- Removed redundant helper methods (activate/deactivate)
- Kept only essential business logic methods
- Cleaner field declarations

#### RRank Entity

**Modern Design:**
```java
@Entity
@Table(name = "r_rank")
public class RRank extends AbstractEntity {
    
    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;
    
    @Column(name = "display_name_key", nullable = false, unique = true)
    private String displayNameKey;
    
    @Column(name = "description_key", nullable = false)
    private String descriptionKey;
    
    @Column(name = "assigned_luckperms_group", nullable = false)
    private String luckPermsGroup;
    
    @Column(name = "tier", nullable = false)
    private int tier;
    
    @Column(name = "weight", nullable = false)
    private int weight;
    
    @Column(name = "is_initial_rank")
    private boolean initialRank;
    
    @Column(name = "is_enabled")
    private boolean enabled = true;
    
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "r_rank_previous_ranks", joinColumns = @JoinColumn(name = "rank_id"))
    @Column(name = "previous_rank_identifier")
    private List<String> previousRanks = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "r_rank_next_ranks", joinColumns = @JoinColumn(name = "rank_id"))
    @Column(name = "next_rank_identifier")
    private List<String> nextRanks = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "rank_tree_id", nullable = true)
    private RRankTree rankTree;
    
    @OneToMany(mappedBy = "rank", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RRankUpgradeRequirement> upgradeRequirements = new HashSet<>();
    
    @Version
    private int version;
    
    protected RRank() {}
    
    // Constructor with essential fields only
    public RRank(@NotNull String identifier, @NotNull String displayNameKey, 
                 @NotNull String descriptionKey, @NotNull String luckPermsGroup,
                 @NotNull IconSection icon, int tier, int weight, @Nullable RRankTree rankTree) {
        this.identifier = Objects.requireNonNull(identifier);
        this.displayNameKey = Objects.requireNonNull(displayNameKey);
        this.descriptionKey = Objects.requireNonNull(descriptionKey);
        this.luckPermsGroup = Objects.requireNonNull(luckPermsGroup);
        this.icon = Objects.requireNonNull(icon);
        this.tier = tier;
        this.weight = weight;
        this.rankTree = rankTree;
    }
    
    // Standard getters - no setters for immutable fields
    public String getIdentifier() { return identifier; }
    public String getDisplayNameKey() { return displayNameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public String getLuckPermsGroup() { return luckPermsGroup; }
    public int getTier() { return tier; }
    public int getWeight() { return weight; }
    public boolean isInitialRank() { return initialRank; }
    public boolean isEnabled() { return enabled; }
    public IconSection getIcon() { return icon; }
    public List<String> getPreviousRanks() { return List.copyOf(previousRanks); }
    public List<String> getNextRanks() { return List.copyOf(nextRanks); }
    public RRankTree getRankTree() { return rankTree; }
    public Set<RRankUpgradeRequirement> getUpgradeRequirements() { return Set.copyOf(upgradeRequirements); }
    
    // Mutable operations
    public void addUpgradeRequirement(@NotNull RRankUpgradeRequirement requirement) {
        upgradeRequirements.add(Objects.requireNonNull(requirement));
        if (requirement.getRank() != this) {
            requirement.setRank(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof RRank other && identifier.equals(other.identifier));
    }
    
    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}
```

**Key Improvements:**
- Removed verbose prefix/suffix fields (not used in modern design)
- Simplified constructor with only essential fields
- Return immutable copies of collections
- Cleaner relationship management
- Removed excessive logging in entity methods

### 2. View Layer Modernization

#### RankPathOverview View

**Current Issues:**
- Excessive logging (FINE level in hot paths)
- Complex nested methods
- Verbose state management
- Redundant null checks

**Modern Design Patterns:**

```java
public class RankPathOverview extends BaseView {
    
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> player = initialState("player");
    private final State<RRankTree> rankTree = initialState("rankTree");
    private final State<Boolean> previewMode = initialState("previewMode");
    
    private final MutableState<Integer> offsetX = mutableState(0);
    private final MutableState<Integer> offsetY = mutableState(0);
    
    private RankDataCache cache;
    
    @Override
    protected String getKey() {
        return "rank_path_overview_ui";
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext ctx, @NotNull Player player) {
        cache = new RankDataCache();
        cache.initialize(rankTree.get(ctx), rdq.get(ctx), this.player.get(ctx), previewMode.get(ctx));
        
        renderStaticControls(ctx, player);
        renderDynamicGrid(ctx, player);
    }
    
    private void renderStaticControls(RenderContext ctx, Player player) {
        // Navigation arrows
        ctx.slot(SLOT_NAV_LEFT).renderWith(() -> createNavArrow(player, "left"))
           .onClick(c -> offsetX.update(c, x -> x - 1));
        
        ctx.slot(SLOT_NAV_RIGHT).renderWith(() -> createNavArrow(player, "right"))
           .onClick(c -> offsetX.update(c, x -> x + 1));
        
        // Back button
        ctx.slot(SLOT_BACK).renderWith(() -> createBackButton(player))
           .onClick(SlotClickContext::back);
    }
    
    private void renderDynamicGrid(RenderContext ctx, Player player) {
        var hierarchy = cache.getRankHierarchy();
        var positions = cache.getWorldPositions();
        var statuses = cache.getRankStatuses();
        
        for (int slot : GridSlotMapper.getAllRankSlots()) {
            ctx.slot(slot)
               .renderWith(() -> createSlotContent(slot, hierarchy, positions, statuses, player))
               .updateOnStateChange(offsetX, offsetY)
               .onClick(c -> handleSlotClick(c, slot, hierarchy, positions));
        }
    }
    
    private ItemStack createSlotContent(int slot, Map<String, RankNode> hierarchy,
                                       Map<String, GridPosition> positions,
                                       Map<String, ERankStatus> statuses, Player player) {
        var gridPos = GridSlotMapper.getPositionForSlot(slot);
        if (gridPos == null) return createBackground(player);
        
        var worldPos = gridPos.offset(-offsetX.get(), -offsetY.get());
        var rankId = findRankAtPosition(worldPos, positions);
        
        return rankId != null 
            ? createRankItem(hierarchy.get(rankId), statuses.get(rankId), player)
            : createConnectionOrBackground(worldPos, hierarchy, positions, player);
    }
    
    private void handleSlotClick(SlotClickContext ctx, int slot,
                                Map<String, RankNode> hierarchy,
                                Map<String, GridPosition> positions) {
        var gridPos = GridSlotMapper.getPositionForSlot(slot);
        if (gridPos == null) return;
        
        var worldPos = gridPos.offset(-offsetX.get(ctx), -offsetY.get(ctx));
        var rankId = findRankAtPosition(worldPos, positions);
        
        if (rankId != null) {
            var node = hierarchy.get(rankId);
            var status = cache.getRankStatuses().get(rankId);
            handleRankClick(ctx, node, status);
        }
    }
    
    private void handleRankClick(SlotClickContext ctx, RankNode node, ERankStatus status) {
        if (previewMode.get(ctx)) {
            sendMessage(ctx.getPlayer(), "preview_mode_click", node.rank.getIdentifier());
            return;
        }
        
        switch (status) {
            case OWNED -> sendMessage(ctx.getPlayer(), "rank_owned", node.rank.getIdentifier());
            case AVAILABLE -> handleAvailableRank(ctx, node);
            case IN_PROGRESS -> handleInProgressRank(ctx, node);
            case LOCKED -> sendMessage(ctx.getPlayer(), "rank_locked", node.rank.getIdentifier());
        }
    }
}
```

**Key Improvements:**
- Removed excessive FINE logging
- Simplified state management with clear naming
- Used modern switch expressions
- Extracted methods with clear single responsibilities
- Reduced nesting with early returns
- Used method references where appropriate

#### GridPosition Record

**Modern Design:**
```java
public record GridPosition(int x, int y) {
    
    public GridPosition offset(int deltaX, int deltaY) {
        return new GridPosition(x + deltaX, y + deltaY);
    }
    
    public double distanceTo(GridPosition other) {
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
```

**Key Improvements:**
- Used Java record for immutable data class
- Automatic equals/hashCode/toString
- Cleaner syntax with less boilerplate

### 3. Service Layer Modernization

#### RankPathService

**Modern Design:**
```java
public class RankPathService {
    
    private final RDQ rdq;
    private final RPlayerRankPathRepository pathRepo;
    private final RPlayerRankRepository rankRepo;
    
    public RankPathService(@NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq);
        this.pathRepo = rdq.getPlayerRankPathRepository();
        this.rankRepo = rdq.getPlayerRankRepository();
    }
    
    public boolean selectRankPath(@NotNull RDQPlayer player, @NotNull RRankTree tree, @NotNull RRank initialRank) {
        // Validate inputs
        if (!initialRank.isInitialRank()) {
            LOGGER.warning("Attempted to select non-initial rank: " + initialRank.getIdentifier());
            return false;
        }
        
        // Check if already selected
        if (hasSelectedPath(player, tree)) {
            LOGGER.fine("Player already has path selected: " + tree.getIdentifier());
            return false;
        }
        
        // Create path and rank records
        var path = new RPlayerRankPath(player, tree, true);
        var rank = new RPlayerRank(player, initialRank, tree);
        
        pathRepo.create(path);
        rankRepo.create(rank);
        
        LOGGER.info("Player %s selected rank path: %s".formatted(player.getPlayerName(), tree.getIdentifier()));
        return true;
    }
    
    public boolean switchRankPath(@NotNull RDQPlayer player, @NotNull RRankTree tree, @NotNull RRank currentRank) {
        // Deactivate current active path
        pathRepo.findListByAttributes(Map.of("player.id", player.getId(), "active", true))
                .forEach(path -> {
                    path.setActive(false);
                    pathRepo.update(path);
                });
        
        // Activate target path
        return pathRepo.findByAttributes(Map.of("player.id", player.getId(), "selectedRankPath.id", tree.getId()))
                .map(path -> {
                    path.setActive(true);
                    pathRepo.update(path);
                    return true;
                })
                .orElse(false);
    }
    
    public boolean hasSelectedPath(@NotNull RDQPlayer player, @NotNull RRankTree tree) {
        return pathRepo.findByAttributes(Map.of(
            "player.id", player.getId(),
            "selectedRankPath.id", tree.getId()
        )).isPresent();
    }
    
    public Optional<RPlayerRankPath> getActivePath(@NotNull RDQPlayer player) {
        return pathRepo.findByAttributes(Map.of("player.id", player.getId(), "active", true));
    }
}
```

**Key Improvements:**
- Removed verbose JavaDoc for obvious methods
- Used Optional for null safety
- Simplified method logic with early returns
- Used modern string formatting
- Reduced logging verbosity
- Clear method naming

### 4. Manager Layer Modernization

#### RankRequirementProgressManager

**Modern Design:**
```java
public class RankRequirementProgressManager {
    
    private final RDQ rdq;
    private final RPlayerRankUpgradeProgressRepository progressRepo;
    private final Map<UUID, Map<Long, RequirementProgressData>> cache = new ConcurrentHashMap<>();
    
    public RankRequirementProgressManager(@NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq);
        this.progressRepo = rdq.getPlayerRankUpgradeProgressRepository();
    }
    
    public RequirementProgressData getProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer,
                                               @NotNull RRankUpgradeRequirement requirement) {
        return cache.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                   .computeIfAbsent(requirement.getId(), k -> loadProgress(rdqPlayer, requirement));
    }
    
    private RequirementProgressData loadProgress(RDQPlayer player, RRankUpgradeRequirement requirement) {
        var progress = progressRepo.findByAttributes(Map.of(
            "player.id", player.getId(),
            "upgradeRequirement.id", requirement.getId()
        )).orElseGet(() -> createProgress(player, requirement));
        
        var abstractReq = requirement.getRequirement().getRequirement();
        var currentProgress = abstractReq.calculateProgress(Bukkit.getPlayer(player.getUniqueId()));
        
        return new RequirementProgressData(
            requirement,
            currentProgress,
            determineStatus(currentProgress),
            abstractReq.getType().name()
        );
    }
    
    private RequirementStatus determineStatus(double progress) {
        return switch ((int)(progress * 100)) {
            case 100 -> RequirementStatus.COMPLETED;
            case int p when p >= 90 -> RequirementStatus.READY_TO_COMPLETE;
            case int p when p > 0 -> RequirementStatus.IN_PROGRESS;
            default -> RequirementStatus.NOT_STARTED;
        };
    }
    
    public CompletionResult attemptCompletion(@NotNull Player player, @NotNull RDQPlayer rdqPlayer,
                                             @NotNull RRankUpgradeRequirement requirement) {
        var progressData = getProgress(player, rdqPlayer, requirement);
        
        if (progressData.status() != RequirementStatus.READY_TO_COMPLETE) {
            return new CompletionResult(false, "Requirement not ready for completion");
        }
        
        var abstractReq = requirement.getRequirement().getRequirement();
        if (!abstractReq.isMet(player)) {
            return new CompletionResult(false, "Requirement conditions not met");
        }
        
        // Consume resources
        abstractReq.consume(player);
        
        // Update progress
        var progress = progressRepo.findByAttributes(Map.of(
            "player.id", rdqPlayer.getId(),
            "upgradeRequirement.id", requirement.getId()
        )).orElseThrow();
        
        progress.setProgress(1.0);
        progressRepo.update(progress);
        
        // Clear cache
        cache.getOrDefault(player.getUniqueId(), Map.of()).remove(requirement.getId());
        
        LOGGER.info("Player %s completed requirement for rank %s"
            .formatted(player.getName(), requirement.getRank().getIdentifier()));
        
        return new CompletionResult(true, "Requirement completed successfully");
    }
    
    public void clearCache(@NotNull Player player) {
        cache.remove(player.getUniqueId());
    }
    
    // Data records
    public record RequirementProgressData(
        RRankUpgradeRequirement requirement,
        double progress,
        RequirementStatus status,
        String requirementType
    ) {
        public int getProgressPercentage() {
            return (int)(progress * 100);
        }
        
        public boolean isCompleted() {
            return status == RequirementStatus.COMPLETED;
        }
    }
    
    public record CompletionResult(boolean success, String message) {}
}
```

**Key Improvements:**
- Used records for data classes
- Modern switch expressions with pattern matching
- Simplified caching with computeIfAbsent
- Clear method decomposition
- Reduced logging verbosity
- Used Optional for null safety

### 5. Hierarchy and Grid Modernization

#### RankNode

**Modern Design:**
```java
public class RankNode {
    
    public final RRank rank;
    public final List<RankNode> children = new ArrayList<>();
    public final List<RankNode> parents = new ArrayList<>();
    
    public RankNode(@NotNull RRank rank) {
        this.rank = Objects.requireNonNull(rank);
    }
    
    public boolean isRoot() {
        return parents.isEmpty() || rank.isInitialRank();
    }
    
    public boolean isLeaf() {
        return children.isEmpty();
    }
    
    public int getChildCount() {
        return children.size();
    }
}
```

**Key Improvements:**
- Removed verbose getters for simple counts
- Simplified boolean methods
- Public final fields for internal data structure

#### RankHierarchyBuilder

**Modern Design:**
```java
public class RankHierarchyBuilder {
    
    public Map<String, RankNode> buildHierarchy(@NotNull RRankTree tree) {
        var nodes = tree.getRanks().stream()
            .collect(Collectors.toMap(RRank::getIdentifier, RankNode::new));
        
        nodes.values().forEach(node -> establishRelationships(node, nodes));
        
        return Map.copyOf(nodes);
    }
    
    private void establishRelationships(RankNode node, Map<String, RankNode> allNodes) {
        node.rank.getNextRanks().stream()
            .map(allNodes::get)
            .filter(Objects::nonNull)
            .forEach(child -> {
                node.children.add(child);
                child.parents.add(node);
            });
    }
}
```

**Key Improvements:**
- Used streams for collection operations
- Simplified relationship establishment
- Removed excessive logging
- Return immutable map

## Data Models

### ERankStatus Enum

**Modern Design:**
```java
public enum ERankStatus {
    OWNED("Owned", Material.LIME_STAINED_GLASS_PANE),
    AVAILABLE("Available", Material.ORANGE_STAINED_GLASS_PANE),
    IN_PROGRESS("In Progress", Material.YELLOW_STAINED_GLASS_PANE),
    LOCKED("Locked", Material.RED_STAINED_GLASS_PANE);
    
    private final String displayName;
    private final Material material;
    
    ERankStatus(String displayName, Material material) {
        this.displayName = displayName;
        this.material = material;
    }
    
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    
    public boolean isAccessible() {
        return this == OWNED || this == AVAILABLE || this == IN_PROGRESS;
    }
}
```

## Error Handling

### Modern Error Handling Patterns

**Service Layer:**
```java
public Optional<RPlayerRank> getCurrentRank(@NotNull RDQPlayer player, @NotNull RRankTree tree) {
    try {
        return rankRepo.findByAttributes(Map.of(
            "player.id", player.getId(),
            "rankTree.id", tree.getId(),
            "active", true
        ));
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Failed to get current rank for player: " + player.getPlayerName(), e);
        return Optional.empty();
    }
}
```

**View Layer:**
```java
private void handleRankClick(SlotClickContext ctx, RankNode node) {
    try {
        var player = ctx.getPlayer();
        var rdqPlayer = getRDQPlayer(ctx);
        
        if (rdqPlayer == null) {
            sendError(player, "player_not_found");
            return;
        }
        
        // Process click...
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error handling rank click", e);
        sendError(ctx.getPlayer(), "click_failed");
    }
}
```

## Testing Strategy

### Unit Tests

**Entity Tests:**
- Test entity creation and relationships
- Test equals/hashCode contracts
- Test bidirectional relationship management

**Service Tests:**
- Test rank path selection
- Test rank progression
- Test requirement completion
- Mock repository layer

**Manager Tests:**
- Test progress tracking
- Test caching behavior
- Test completion logic
- Mock service layer

### Integration Tests

**Database Tests:**
- Test entity persistence
- Test relationship cascading
- Test query performance

**View Tests:**
- Test rendering logic
- Test click handling
- Test state management

## Performance Considerations

### Caching Strategy

- Cache rank hierarchy per rank tree
- Cache player progress per session
- Clear cache on rank changes
- Use weak references for player data

### Database Optimization

- Use batch operations for bulk updates
- Eager fetch only necessary relationships
- Use indexed columns for queries
- Implement connection pooling

### View Optimization

- Render static elements once
- Update only dynamic elements on state change
- Use efficient slot mapping
- Minimize item stack creation

## Migration Path

### Phase 1: Entity Modernization
1. Update entity classes one by one
2. Test database operations
3. Verify existing data compatibility

### Phase 2: Service Modernization
1. Update service classes
2. Update manager classes
3. Test business logic

### Phase 3: View Modernization
1. Update view classes
2. Update interaction handlers
3. Test GUI functionality

### Phase 4: Testing and Validation
1. Run full test suite
2. Perform manual testing
3. Verify backward compatibility

## Backward Compatibility

### Database Schema
- No schema changes required
- All table names remain the same
- All column names remain the same
- All relationships remain the same

### Configuration
- YAML structure unchanged
- All configuration keys remain the same
- Default values remain the same

### API
- Public method signatures unchanged
- Repository interfaces unchanged
- Service interfaces unchanged
- Event handling unchanged
