---
inclusion: always
---

# PaperMC Best Practices - Development Guide

This document provides best practices for developing Minecraft plugins using PaperMC API.

## Modern Paper API Usage

### Use Paper API Instead of Bukkit/Spigot

```java
// ❌ Old Bukkit way
import org.bukkit.ChatColor;
player.sendMessage(ChatColor.GREEN + "Hello!");

// ✅ Modern Paper way with Adventure
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
player.sendMessage(Component.text("Hello!", NamedTextColor.GREEN));

// ✅ Best: Use JExTranslate
r18n.message("welcome.player").send(player);
```

### Component API for All Text

```java
// Item names and lore
ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
ItemMeta meta = item.getItemMeta();

// Use Component API
meta.displayName(Component.text("Epic Sword", NamedTextColor.GOLD));
meta.lore(List.of(
    Component.text("A legendary weapon", NamedTextColor.GRAY),
    Component.text("Damage: 10", NamedTextColor.YELLOW)
));

item.setItemMeta(meta);
```

## Async Operations

### Use Scheduler Correctly

```java
// ❌ Never block main thread
public void onCommand(CommandSender sender) {
    // This blocks the server!
    DatabaseResult result = database.query();
    sender.sendMessage("Result: " + result);
}

// ✅ Use async for I/O operations
public void onCommand(CommandSender sender) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        DatabaseResult result = database.query();
        
        // Switch back to main thread for Bukkit API
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage("Result: " + result);
        });
    });
}

// ✅ Better: Use CompletableFuture
public CompletableFuture<DatabaseResult> queryAsync() {
    return CompletableFuture.supplyAsync(() -> 
        database.query()
    );
}

public void onCommand(CommandSender sender) {
    queryAsync().thenAccept(result -> {
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage("Result: " + result);
        });
    });
}
```

### Paper's Async Chunk Loading

```java
// ❌ Old synchronous way (blocks server)
Chunk chunk = world.getChunkAt(x, z);

// ✅ Paper async chunk loading
world.getChunkAtAsync(x, z).thenAccept(chunk -> {
    // Process chunk on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        // Work with chunk
    });
});
```

## Event Handling

### Use Correct Event Priority

```java
// For monitoring only (don't modify)
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onPlayerJoin(PlayerJoinEvent event) {
    // Log or track statistics
    stats.recordJoin(event.getPlayer());
}

// For normal handling
@EventHandler(priority = EventPriority.NORMAL)
public void onPlayerInteract(PlayerInteractEvent event) {
    // Handle interaction
}

// For overriding other plugins
@EventHandler(priority = EventPriority.HIGH)
public void onPlayerDamage(EntityDamageEvent event) {
    // Override damage
}
```

### Unregister Listeners Properly

```java
public class MyListener implements Listener {
    private final HandlerList handlers = new HandlerList();
    
    public void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void unregister() {
        HandlerList.unregisterAll(this);
    }
}
```

## Inventory Management

### Use Paper's Inventory API

```java
// ❌ Old way with raw slots
inventory.setItem(0, item);

// ✅ Better: Use inventory methods
inventory.addItem(item);
inventory.setStorageContents(items);

// ✅ Best: Use Inventory Framework (see inventory-framework.md)
```

### Handle Inventory Clicks Safely

```java
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    // Always check inventory type
    if (!(event.getInventory().getHolder() instanceof MyHolder)) {
        return;
    }
    
    // Cancel to prevent item movement
    event.setCancelled(true);
    
    // Get clicked item safely
    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR) {
        return;
    }
    
    // Handle click
    Player player = (Player) event.getWhoClicked();
    handleClick(player, clicked);
}
```

## Entity and World Manipulation

### Use Paper's Entity API

```java
// ❌ Old way
Entity entity = world.spawnEntity(location, EntityType.ZOMBIE);
Zombie zombie = (Zombie) entity;

// ✅ Paper way with type safety
Zombie zombie = world.spawn(location, Zombie.class, z -> {
    z.setCustomName("Boss Zombie");
    z.setCustomNameVisible(true);
    z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
    z.setHealth(100);
});
```

### Efficient Block Operations

```java
// ❌ Slow: Multiple block updates
for (Location loc : locations) {
    loc.getBlock().setType(Material.STONE);
}

// ✅ Fast: Batch block updates
world.setBlockData(locations, Material.STONE.createBlockData(), false);

// ✅ For large operations: Use async
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // Prepare block data
    Map<Location, BlockData> changes = prepareChanges();
    
    // Apply on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        changes.forEach((loc, data) -> loc.getBlock().setBlockData(data));
    });
});
```

## Configuration Management

### Use Modern Configuration API

```java
public class PluginConfig {
    private final FileConfiguration config;
    
    public PluginConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }
    
    // Type-safe getters
    public String getDefaultLocale() {
        return config.getString("locale.default", "en_US");
    }
    
    public List<String> getSupportedLocales() {
        return config.getStringList("locale.supported");
    }
    
    public int getCacheSize() {
        return config.getInt("cache.size", 1000);
    }
    
    // Validation
    public void validate() {
        if (getDefaultLocale().isEmpty()) {
            throw new IllegalStateException("Default locale cannot be empty");
        }
    }
}
```

## Performance Optimization

### Cache Expensive Operations

```java
// ❌ Recalculating every time
public Component getDisplayName(Player player) {
    return r18n.message("player.name")
        .placeholder("name", player.getName())
        .placeholder("rank", getRank(player))  // Expensive!
        .toComponent(player);
}

// ✅ Cache results
private final Map<UUID, Component> nameCache = new HashMap<>();

public Component getDisplayName(Player player) {
    return nameCache.computeIfAbsent(player.getUniqueId(), uuid -> 
        r18n.message("player.name")
            .placeholder("name", player.getName())
            .placeholder("rank", getRank(player))
            .toComponent(player)
    );
}

// Clear cache when needed
@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    nameCache.remove(event.getPlayer().getUniqueId());
}
```

### Use Caffeine Cache for Auto-Expiry

```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

private final Cache<UUID, PlayerData> cache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(Duration.ofMinutes(30))
    .build();

public PlayerData getData(UUID playerId) {
    return cache.get(playerId, uuid -> loadFromDatabase(uuid));
}
```

## Resource Management

### Always Clean Up Resources

```java
public class MyPlugin extends JavaPlugin {
    private R18nManager r18n;
    private DatabaseConnection database;
    private ExecutorService executor;
    
    @Override
    public void onEnable() {
        // Initialize resources
        r18n = R18nManager.builder(this)
            .defaultLocale("en_US")
            .build();
        
        database = new DatabaseConnection(config);
        executor = Executors.newFixedThreadPool(4);
        
        r18n.initialize().thenRun(() -> {
            getLogger().info("Plugin enabled!");
        });
    }
    
    @Override
    public void onDisable() {
        // Clean up in reverse order
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        
        if (database != null) {
            database.close();
        }
        
        if (r18n != null) {
            r18n.shutdown();
        }
        
        getLogger().info("Plugin disabled!");
    }
}
```

## Command Handling

### Use Paper's Command API

```java
// Modern command with tab completion
public class MyCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, 
                            String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("myplugin.command")) {
            r18n.message("error.permission")
                .placeholder("permission", "myplugin.command")
                .send(sender);
            return true;
        }
        
        // Player-only check
        if (!(sender instanceof Player player)) {
            r18n.message("error.player-only").send(sender);
            return true;
        }
        
        // Handle command
        handleCommand(player, args);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                     String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "help", "info")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}
```

## Debugging and Logging

### Use Proper Logging Levels

```java
// INFO: Important events
getLogger().info("Plugin initialized with " + playerCount + " players");

// WARNING: Recoverable issues
getLogger().warning("Failed to load config, using defaults");

// SEVERE: Critical errors
getLogger().severe("Database connection failed!");

// FINE/FINER/FINEST: Debug info (only when debug enabled)
if (config.isDebugEnabled()) {
    getLogger().fine("Processing player data for " + player.getName());
}
```

### Use Paper's Debug Features

```java
// Enable debug mode in config
public boolean isDebugMode() {
    return getConfig().getBoolean("debug", false);
}

// Debug logging
public void debug(String message) {
    if (isDebugMode()) {
        getLogger().info("[DEBUG] " + message);
    }
}

// Usage
debug("Cache hit for player " + player.getName());
```

## Best Practices Summary

1. ✅ Use Paper API over Bukkit/Spigot when available
2. ✅ Use Adventure Component API for all text
3. ✅ Run I/O operations asynchronously
4. ✅ Use CompletableFuture for async operations
5. ✅ Handle events with correct priority
6. ✅ Unregister listeners on disable
7. ✅ Use type-safe entity spawning
8. ✅ Batch block updates for performance
9. ✅ Cache expensive operations
10. ✅ Clean up resources in onDisable()
11. ✅ Use proper logging levels
12. ✅ Validate configuration on load

## Common Pitfalls to Avoid

❌ **Never** call Bukkit API from async threads
❌ **Never** block the main thread with I/O
❌ **Never** forget to unregister listeners
❌ **Never** use deprecated methods
❌ **Never** ignore null checks
❌ **Never** use raw strings for messages (use i18n)
❌ **Never** forget to clean up resources
❌ **Never** use `Thread.sleep()` on main thread

## Related Documentation

- JExTranslate i18n: See `jextranslate-i18n.md`
- Inventory Framework: See `inventory-framework.md`
- JEHibernate: See `jehibernate-integration.md`
- Paper API: https://docs.papermc.io/
