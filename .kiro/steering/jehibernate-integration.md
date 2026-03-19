---
inclusion: always
---

# JEHibernate Integration Guide

This document provides guidance for using JEHibernate ORM for database operations in Minecraft plugins.

## Quick Start

### Entity Definition

```java
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "player_data")
public class PlayerData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private UUID playerId;
    
    @Column(nullable = false)
    private String playerName;
    
    @Column(nullable = false)
    private String locale = "en_US";
    
    @Column(nullable = false)
    private int coins = 0;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    
    // Constructors
    public PlayerData() {}
    
    public PlayerData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }
    
    // Getters and setters
    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    // Business methods
    public void addCoins(int amount) {
        this.coins += amount;
    }
    
    public boolean removeCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }
}
```

### Repository Pattern

```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataRepository {
    private final EntityManagerFactory emf;
    
    public PlayerDataRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    /**
     * Find player data by UUID.
     */
    public CompletableFuture<Optional<PlayerData>> findByPlayerId(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                PlayerData data = em.createQuery(
                    "SELECT p FROM PlayerData p WHERE p.playerId = :playerId",
                    PlayerData.class
                )
                .setParameter("playerId", playerId)
                .getResultStream()
                .findFirst()
                .orElse(null);
                
                return Optional.ofNullable(data);
            } finally {
                em.close();
            }
        });
    }
    
    /**
     * Save or update player data.
     */
    public CompletableFuture<PlayerData> save(PlayerData data) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                PlayerData merged = em.merge(data);
                em.getTransaction().commit();
                return merged;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            } finally {
                em.close();
            }
        });
    }
    
    /**
     * Delete player data.
     */
    public CompletableFuture<Void> delete(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                em.createQuery("DELETE FROM PlayerData p WHERE p.playerId = :playerId")
                    .setParameter("playerId", playerId)
                    .executeUpdate();
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            } finally {
                em.close();
            }
        });
    }
}
```

### Service Layer

```java
public class PlayerDataService {
    private final PlayerDataRepository repository;
    private final JavaPlugin plugin;
    
    public PlayerDataService(PlayerDataRepository repository, JavaPlugin plugin) {
        this.repository = repository;
        this.plugin = plugin;
    }
    
    /**
     * Get or create player data.
     */
    public CompletableFuture<PlayerData> getOrCreate(Player player) {
        return repository.findByPlayerId(player.getUniqueId())
            .thenCompose(optional -> {
                if (optional.isPresent()) {
                    return CompletableFuture.completedFuture(optional.get());
                }
                
                // Create new player data
                PlayerData data = new PlayerData(
                    player.getUniqueId(),
                    player.getName()
                );
                return repository.save(data);
            });
    }
    
    /**
     * Add coins to player.
     */
    public CompletableFuture<Void> addCoins(UUID playerId, int amount) {
        return repository.findByPlayerId(playerId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.failedFuture(
                        new IllegalStateException("Player data not found")
                    );
                }
                
                PlayerData data = optional.get();
                data.addCoins(amount);
                return repository.save(data).thenApply(saved -> null);
            });
    }
    
    /**
     * Remove coins from player.
     */
    public CompletableFuture<Boolean> removeCoins(UUID playerId, int amount) {
        return repository.findByPlayerId(playerId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                PlayerData data = optional.get();
                if (!data.removeCoins(amount)) {
                    return CompletableFuture.completedFuture(false);
                }
                
                return repository.save(data).thenApply(saved -> true);
            });
    }
}
```

## Entity Relationships

### One-to-Many Relationship

```java
@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private UUID playerId;
    
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Perk> perks = new ArrayList<>();
    
    // Helper methods
    public void addPerk(Perk perk) {
        perks.add(perk);
        perk.setPlayer(this);
    }
    
    public void removePerk(Perk perk) {
        perks.remove(perk);
        perk.setPlayer(null);
    }
}

@Entity
@Table(name = "perks")
public class Perk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    
    @Column(nullable = false)
    private String perkType;
    
    @Column(nullable = false)
    private int level;
    
    @Column(nullable = false)
    private boolean active;
    
    // Getters and setters
}
```

### Many-to-Many Relationship

```java
@Entity
@Table(name = "players")
public class Player {
    @Id
    private UUID playerId;
    
    @ManyToMany
    @JoinTable(
        name = "player_achievements",
        joinColumns = @JoinColumn(name = "player_id"),
        inverseJoinColumns = @JoinColumn(name = "achievement_id")
    )
    private Set<Achievement> achievements = new HashSet<>();
}

@Entity
@Table(name = "achievements")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String achievementKey;
    
    @ManyToMany(mappedBy = "achievements")
    private Set<Player> players = new HashSet<>();
}
```

## Query Patterns

### JPQL Queries

```java
public class PlayerDataRepository {
    
    /**
     * Find top players by coins.
     */
    public CompletableFuture<List<PlayerData>> findTopByCoins(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT p FROM PlayerData p ORDER BY p.coins DESC",
                    PlayerData.class
                )
                .setMaxResults(limit)
                .getResultList();
            } finally {
                em.close();
            }
        });
    }
    
    /**
     * Find players by locale.
     */
    public CompletableFuture<List<PlayerData>> findByLocale(String locale) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT p FROM PlayerData p WHERE p.locale = :locale",
                    PlayerData.class
                )
                .setParameter("locale", locale)
                .getResultList();
            } finally {
                em.close();
            }
        });
    }
    
    /**
     * Count players with minimum coins.
     */
    public CompletableFuture<Long> countByMinCoins(int minCoins) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT COUNT(p) FROM PlayerData p WHERE p.coins >= :minCoins",
                    Long.class
                )
                .setParameter("minCoins", minCoins)
                .getSingleResult();
            } finally {
                em.close();
            }
        });
    }
}
```

### Criteria API

```java
import jakarta.persistence.criteria.*;

public CompletableFuture<List<PlayerData>> findWithCriteria(
    String locale, 
    int minCoins
) {
    return CompletableFuture.supplyAsync(() -> {
        EntityManager em = emf.createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<PlayerData> query = cb.createQuery(PlayerData.class);
            Root<PlayerData> root = query.from(PlayerData.class);
            
            // Build predicates
            List<Predicate> predicates = new ArrayList<>();
            
            if (locale != null) {
                predicates.add(cb.equal(root.get("locale"), locale));
            }
            
            if (minCoins > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("coins"), minCoins));
            }
            
            query.where(predicates.toArray(new Predicate[0]));
            query.orderBy(cb.desc(root.get("coins")));
            
            return em.createQuery(query).getResultList();
        } finally {
            em.close();
        }
    });
}
```

## Transaction Management

### Manual Transactions

```java
public CompletableFuture<Void> transferCoins(UUID fromId, UUID toId, int amount) {
    return CompletableFuture.runAsync(() -> {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            
            // Load both players
            PlayerData from = em.find(PlayerData.class, fromId);
            PlayerData to = em.find(PlayerData.class, toId);
            
            if (from == null || to == null) {
                throw new IllegalStateException("Player not found");
            }
            
            // Transfer coins
            if (!from.removeCoins(amount)) {
                throw new IllegalStateException("Insufficient funds");
            }
            to.addCoins(amount);
            
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    });
}
```

### Batch Operations

```java
public CompletableFuture<Void> batchUpdate(List<PlayerData> players) {
    return CompletableFuture.runAsync(() -> {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            
            int batchSize = 50;
            for (int i = 0; i < players.size(); i++) {
                em.merge(players.get(i));
                
                if (i % batchSize == 0 && i > 0) {
                    em.flush();
                    em.clear();
                }
            }
            
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    });
}
```

## Caching Strategy

### Entity-Level Caching

```java
@Entity
@Table(name = "player_data")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PlayerData {
    // Entity fields
}
```

### Query Result Caching

```java
public CompletableFuture<List<PlayerData>> findTopByCoins(int limit) {
    return CompletableFuture.supplyAsync(() -> {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT p FROM PlayerData p ORDER BY p.coins DESC",
                PlayerData.class
            )
            .setMaxResults(limit)
            .setHint("org.hibernate.cacheable", true)
            .getResultList();
        } finally {
            em.close();
        }
    });
}
```

## Integration with Plugin

### Plugin Setup

```java
public class MyPlugin extends JavaPlugin {
    private EntityManagerFactory emf;
    private PlayerDataService playerDataService;
    private R18nManager r18n;
    
    @Override
    public void onEnable() {
        // Initialize database
        emf = createEntityManagerFactory();
        
        // Initialize repositories and services
        PlayerDataRepository repository = new PlayerDataRepository(emf);
        playerDataService = new PlayerDataService(repository, this);
        
        // Initialize R18n
        r18n = R18nManager.builder(this)
            .defaultLocale("en_US")
            .build();
        
        r18n.initialize().thenRun(() -> {
            getLogger().info("Plugin initialized!");
        });
        
        // Register listeners
        getServer().getPluginManager().registerEvents(
            new PlayerJoinListener(playerDataService, r18n),
            this
        );
    }
    
    @Override
    public void onDisable() {
        if (r18n != null) {
            r18n.shutdown();
        }
        
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
    
    private EntityManagerFactory createEntityManagerFactory() {
        Map<String, String> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", 
            "jdbc:mysql://localhost:3306/minecraft");
        properties.put("jakarta.persistence.jdbc.user", "root");
        properties.put("jakarta.persistence.jdbc.password", "password");
        properties.put("jakarta.persistence.jdbc.driver", 
            "com.mysql.cj.jdbc.Driver");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        
        return Persistence.createEntityManagerFactory("minecraft-pu", properties);
    }
}
```

### Event Listener Integration

```java
public class PlayerJoinListener implements Listener {
    private final PlayerDataService service;
    private final R18nManager r18n;
    
    public PlayerJoinListener(PlayerDataService service, R18nManager r18n) {
        this.service = service;
        this.r18n = r18n;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data async
        service.getOrCreate(player).thenAccept(data -> {
            // Back to main thread for Bukkit API
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Send welcome message with coins
                r18n.message("welcome.player")
                    .placeholder("player", player.getName())
                    .placeholder("coins", data.getCoins())
                    .send(player);
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load player data: " + ex.getMessage());
            return null;
        });
    }
}
```

## CachedRepository Pattern

### Overview

The CachedRepository pattern provides in-memory caching for frequently accessed entities, reducing database queries and improving performance. It's particularly useful for player data that needs instant access.

### Basic CachedRepository Implementation

```java
public class PlayerDataCache {
    private final PlayerDataRepository repository;
    private final ConcurrentHashMap<UUID, PlayerData> cache;
    private final Set<UUID> dirtyPlayers;
    private final boolean logPerformance;
    
    public PlayerDataCache(PlayerDataRepository repository, boolean logPerformance) {
        this.repository = repository;
        this.cache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
        this.logPerformance = logPerformance;
    }
    
    /**
     * Load player data into cache asynchronously.
     */
    public CompletableFuture<Void> loadPlayerAsync(UUID playerId) {
        return repository.findByPlayerId(playerId)
            .thenAccept(optional -> {
                optional.ifPresent(data -> cache.put(playerId, data));
            });
    }
    
    /**
     * Get player data from cache (instant access).
     */
    public Optional<PlayerData> getPlayer(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }
    
    /**
     * Update player data in cache and mark as dirty.
     */
    public void updatePlayer(UUID playerId, PlayerData data) {
        cache.put(playerId, data);
        markDirty(playerId);
    }
    
    /**
     * Save player data to database and remove from cache.
     */
    public void savePlayer(UUID playerId) {
        if (!dirtyPlayers.contains(playerId)) {
            cache.remove(playerId);
            return;
        }
        
        PlayerData data = cache.get(playerId);
        if (data != null) {
            repository.save(data).thenRun(() -> {
                dirtyPlayers.remove(playerId);
                cache.remove(playerId);
            });
        }
    }
    
    /**
     * Mark player as having unsaved changes.
     */
    public void markDirty(UUID playerId) {
        dirtyPlayers.add(playerId);
    }
    
    /**
     * Auto-save all dirty players (for crash protection).
     */
    public int autoSaveAll() {
        Set<UUID> playersToSave = new HashSet<>(dirtyPlayers);
        int savedCount = 0;
        
        for (UUID playerId : playersToSave) {
            PlayerData data = cache.get(playerId);
            if (data != null) {
                try {
                    repository.save(data).join();
                    dirtyPlayers.remove(playerId);
                    savedCount++;
                } catch (Exception e) {
                    getLogger().warning("Auto-save failed for " + playerId);
                }
            }
        }
        
        return savedCount;
    }
}
```

### Advanced: Collection-Based Cache

For entities with one-to-many relationships (like player perks):

```java
public class PlayerPerkCache {
    private final PlayerPerkRepository repository;
    private final ConcurrentHashMap<UUID, List<PlayerPerk>> cache;
    private final Set<UUID> dirtyPlayers;
    
    public PlayerPerkCache(PlayerPerkRepository repository) {
        this.repository = repository;
        this.cache = new ConcurrentHashMap<>();
        this.dirtyPlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Load all perks for a player.
     */
    public CompletableFuture<Void> loadPlayerAsync(UUID playerId) {
        return repository.findAllByAttributesAsync(
            Map.of("player.uniqueId", playerId)
        ).thenAccept(perks -> {
            cache.put(playerId, new ArrayList<>(perks));
        });
    }
    
    /**
     * Get all perks for a player from cache.
     */
    public List<PlayerPerk> getPerks(UUID playerId) {
        List<PlayerPerk> perks = cache.get(playerId);
        return perks != null ? new ArrayList<>(perks) : Collections.emptyList();
    }
    
    /**
     * Get specific perk from cache.
     */
    public Optional<PlayerPerk> getPerk(UUID playerId, Long perkId) {
        List<PlayerPerk> perks = cache.get(playerId);
        if (perks == null) {
            return Optional.empty();
        }
        
        return perks.stream()
            .filter(p -> p.getId().equals(perkId))
            .findFirst();
    }
    
    /**
     * Update perk in cache.
     */
    public void updatePerk(UUID playerId, PlayerPerk perk) {
        List<PlayerPerk> perks = cache.computeIfAbsent(
            playerId, 
            k -> new ArrayList<>()
        );
        
        // Remove old version
        perks.removeIf(p -> p.getId().equals(perk.getId()));
        
        // Add updated version
        perks.add(perk);
        
        markDirty(playerId);
    }
    
    /**
     * Save all perks for a player.
     */
    public void savePlayer(UUID playerId) {
        if (!dirtyPlayers.contains(playerId)) {
            cache.remove(playerId);
            return;
        }
        
        List<PlayerPerk> perks = cache.get(playerId);
        if (perks == null || perks.isEmpty()) {
            dirtyPlayers.remove(playerId);
            return;
        }
        
        // Batch update
        CompletableFuture<?>[] futures = perks.stream()
            .map(repository::save)
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture.allOf(futures).thenRun(() -> {
            dirtyPlayers.remove(playerId);
            cache.remove(playerId);
        });
    }
    
    /**
     * Check if player data is loaded.
     */
    public boolean isLoaded(UUID playerId) {
        return cache.containsKey(playerId);
    }
    
    /**
     * Check if player has unsaved changes.
     */
    public boolean isDirty(UUID playerId) {
        return dirtyPlayers.contains(playerId);
    }
}
```

### Integration with Event Listeners

```java
public class PlayerDataListener implements Listener {
    private final PlayerDataCache cache;
    private final JavaPlugin plugin;
    
    public PlayerDataListener(PlayerDataCache cache, JavaPlugin plugin) {
        this.cache = cache;
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load data asynchronously
        cache.loadPlayerAsync(player.getUniqueId())
            .exceptionally(ex -> {
                plugin.getLogger().severe("Failed to load data for " + player.getName());
                player.kickPlayer("Failed to load player data");
                return null;
            });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save data synchronously on quit
        cache.savePlayer(player.getUniqueId());
    }
}
```

### Auto-Save Task

```java
public class AutoSaveTask extends BukkitRunnable {
    private final PlayerDataCache cache;
    
    public AutoSaveTask(PlayerDataCache cache) {
        this.cache = cache;
    }
    
    @Override
    public void run() {
        int saved = cache.autoSaveAll();
        if (saved > 0) {
            Bukkit.getLogger().info("Auto-saved " + saved + " players");
        }
    }
}

// In plugin onEnable()
new AutoSaveTask(cache).runTaskTimerAsynchronously(
    plugin,
    20 * 60 * 5,  // Initial delay: 5 minutes
    20 * 60 * 5   // Repeat: every 5 minutes
);
```

### Cache Statistics

```java
public class CacheStatistics {
    private final PlayerDataCache cache;
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_size", cache.getCacheSize());
        stats.put("dirty_count", cache.getDirtyCount());
        stats.put("hit_rate", calculateHitRate());
        return stats;
    }
    
    public void logStatistics() {
        Map<String, Object> stats = getStatistics();
        Bukkit.getLogger().info(String.format(
            "Cache Stats - Size: %d, Dirty: %d, Hit Rate: %.2f%%",
            stats.get("cache_size"),
            stats.get("dirty_count"),
            stats.get("hit_rate")
        ));
    }
}
```

### CachedRepository Best Practices

```java
// ✅ Good: Load on join, save on quit
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    cache.loadPlayerAsync(event.getPlayer().getUniqueId());
}

@EventHandler
public void onQuit(PlayerQuitEvent event) {
    cache.savePlayer(event.getPlayer().getUniqueId());
}

// ✅ Good: Mark dirty after modifications
public void addCoins(UUID playerId, int amount) {
    cache.getPlayer(playerId).ifPresent(data -> {
        data.addCoins(amount);
        cache.markDirty(playerId);
    });
}

// ✅ Good: Auto-save for crash protection
new AutoSaveTask(cache).runTaskTimerAsynchronously(plugin, 6000, 6000);

// ❌ Bad: Saving on every modification
public void addCoins(UUID playerId, int amount) {
    cache.getPlayer(playerId).ifPresent(data -> {
        data.addCoins(amount);
        cache.savePlayer(playerId);  // Too frequent!
    });
}

// ❌ Bad: Not checking if loaded
public void addCoins(UUID playerId, int amount) {
    PlayerData data = cache.getPlayer(playerId).get();  // May throw!
    data.addCoins(amount);
}

// ✅ Good: Check if loaded first
public void addCoins(UUID playerId, int amount) {
    if (!cache.isLoaded(playerId)) {
        getLogger().warning("Player data not loaded for " + playerId);
        return;
    }
    
    cache.getPlayer(playerId).ifPresent(data -> {
        data.addCoins(amount);
        cache.markDirty(playerId);
    });
}
```

### Handling Optimistic Lock Exceptions

```java
public void updatePerk(UUID playerId, PlayerPerk perk) {
    try {
        cache.updatePerk(playerId, perk);
    } catch (OptimisticLockException e) {
        // Entity was modified elsewhere - reload from database
        getLogger().fine("Optimistic lock exception - reloading data");
        cache.loadPlayerAsync(playerId).thenRun(() -> {
            // Retry update
            cache.updatePerk(playerId, perk);
        });
    }
}
```

## Best Practices

### 1. Always Use Async Operations

```java
// ✅ Good: Async database operations
public void handleCommand(Player player) {
    service.getPlayerData(player.getUniqueId())
        .thenAccept(data -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                r18n.message("data.loaded")
                    .placeholder("coins", data.getCoins())
                    .send(player);
            });
        });
}

// ❌ Bad: Blocking main thread
public void handleCommand(Player player) {
    PlayerData data = service.getPlayerDataSync(player.getUniqueId());
    // Server freezes!
}
```

### 2. Close EntityManager Properly

```java
// ✅ Good: Always close in finally
EntityManager em = emf.createEntityManager();
try {
    // Database operations
} finally {
    em.close();
}

// ❌ Bad: May leak resources
EntityManager em = emf.createEntityManager();
// Operations
em.close();  // May not execute if exception occurs
```

### 3. Handle Transactions Correctly

```java
// ✅ Good: Rollback on error
em.getTransaction().begin();
try {
    // Operations
    em.getTransaction().commit();
} catch (Exception e) {
    if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
    }
    throw e;
}

// ❌ Bad: No rollback handling
em.getTransaction().begin();
// Operations
em.getTransaction().commit();
```

### 4. Use DTOs for Data Transfer

```java
// ✅ Good: Use DTO to avoid lazy loading issues
public record PlayerDataDTO(
    UUID playerId,
    String playerName,
    int coins,
    String locale
) {
    public static PlayerDataDTO from(PlayerData entity) {
        return new PlayerDataDTO(
            entity.getPlayerId(),
            entity.getPlayerName(),
            entity.getCoins(),
            entity.getLocale()
        );
    }
}

// ❌ Bad: Passing entity outside transaction
public PlayerData getData() {
    EntityManager em = emf.createEntityManager();
    try {
        return em.find(PlayerData.class, id);
    } finally {
        em.close();  // Entity becomes detached!
    }
}
```

### 5. Optimize Queries

```java
// ✅ Good: Fetch only needed data
em.createQuery(
    "SELECT NEW PlayerDataDTO(p.playerId, p.playerName, p.coins) " +
    "FROM PlayerData p WHERE p.playerId = :id",
    PlayerDataDTO.class
)
.setParameter("id", playerId)
.getSingleResult();

// ❌ Bad: Loading entire entity when only need few fields
PlayerData data = em.find(PlayerData.class, id);
int coins = data.getCoins();  // Loaded entire entity for one field
```

### 6. Choose the Right Pattern

```java
// ✅ Use CachedRepository for frequently accessed data
// - Player data (online players)
// - Player perks/achievements
// - Player settings/preferences
public class PlayerDataService {
    private final PlayerDataCache cache;  // In-memory cache
    
    public void addCoins(UUID playerId, int amount) {
        cache.getPlayer(playerId).ifPresent(data -> {
            data.addCoins(amount);
            cache.markDirty(playerId);
        });
    }
}

// ✅ Use regular Repository for infrequent access
// - Historical data
// - Leaderboards
// - Statistics
public class LeaderboardService {
    private final PlayerDataRepository repository;  // Direct DB access
    
    public CompletableFuture<List<PlayerData>> getTopPlayers(int limit) {
        return repository.findTopByCoins(limit);
    }
}

// ❌ Bad: Caching infrequently accessed data
public class HistoricalDataCache {
    // Wastes memory on data that's rarely accessed
    private final Map<UUID, List<HistoricalRecord>> cache;
}
```

## When to Use CachedRepository

### Use CachedRepository When:

✅ Data is accessed frequently (multiple times per second)
✅ Data belongs to online players only
✅ Instant access is required (no database latency)
✅ Data changes frequently during gameplay
✅ You can afford to lose a few minutes of data on crash

**Examples:**
- Player inventory/equipment
- Active perks/buffs
- Player settings
- Combat statistics (during session)
- Quest progress (active quests)

### Use Regular Repository When:

✅ Data is accessed infrequently
✅ Data belongs to offline players
✅ Database latency is acceptable
✅ Data rarely changes
✅ Data must never be lost

**Examples:**
- Historical records
- Leaderboards
- Achievements (permanent)
- Ban records
- Transaction logs
- Audit trails

### Hybrid Approach

```java
public class PlayerDataService {
    private final PlayerDataCache cache;
    private final PlayerDataRepository repository;
    
    /**
     * Get data for online player (from cache).
     */
    public Optional<PlayerData> getOnlinePlayer(UUID playerId) {
        return cache.getPlayer(playerId);
    }
    
    /**
     * Get data for any player (from database).
     */
    public CompletableFuture<Optional<PlayerData>> getAnyPlayer(UUID playerId) {
        // Check cache first
        Optional<PlayerData> cached = cache.getPlayer(playerId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Fall back to database
        return repository.findByPlayerId(playerId);
    }
    
    /**
     * Update online player (in cache).
     */
    public void updateOnlinePlayer(UUID playerId, PlayerData data) {
        cache.updatePlayer(playerId, data);
    }
    
    /**
     * Update any player (in database).
     */
    public CompletableFuture<Void> updateAnyPlayer(PlayerData data) {
        return repository.save(data).thenApply(saved -> null);
    }
}
```

## Related Documentation

- JExTranslate i18n: See `jextranslate-i18n.md`
- PaperMC Best Practices: See `papermc-best-practices.md`
- Hibernate Documentation: https://hibernate.org/orm/documentation/
