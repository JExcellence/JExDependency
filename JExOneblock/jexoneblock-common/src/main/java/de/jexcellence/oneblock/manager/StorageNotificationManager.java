package de.jexcellence.oneblock.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smart Storage Notification Manager
 * Reduces chat spam by showing floating holograms and summary notifications
 */
public class StorageNotificationManager {
    
    private final Map<UUID, StorageSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSummaryTime = new ConcurrentHashMap<>();
    private final long SUMMARY_INTERVAL = 15 * 60 * 1000; // 15 minutes
    
    /**
     * Handles a storage event - either accumulates for summary or shows floating text
     */
    public void handleStorageEvent(Player player, Material material, int amount, boolean wasStored, Location blockLocation) {
        UUID playerId = player.getUniqueId();
        
        // Get or create session
        StorageSession session = activeSessions.computeIfAbsent(playerId, 
            id -> new StorageSession(player));
        
        // Update core location if this is near the oneblock
        if (blockLocation != null) {
            session.setCoreLocation(blockLocation);
        }
        
        if (wasStored) {
            // Add to accumulated items
            session.addStoredItem(material, amount);
            
            // Show floating +amount with material name at block location
            showFloatingText(player, "§a+" + amount + " " + formatMaterialName(material), blockLocation, true);
        } else {
            // Show floating overflow text with material name at block location
            showFloatingText(player, "§c+" + amount + " " + formatMaterialName(material) + " §7(overflow)", blockLocation, false);
        }
        
        // Check if we should show summary
        checkAndShowSummary(player, session);
    }
    
    /**
     * Overload for backward compatibility
     */
    public void handleStorageEvent(Player player, Material material, int amount, boolean wasStored) {
        handleStorageEvent(player, material, amount, wasStored, player.getLocation());
    }
    
    /**
     * Shows floating text above the broken block that moves upward slowly
     */
    private void showFloatingText(Player player, String text, Location blockLocation, boolean isStored) {
        // Position text slightly above the block location (where the player is looking)
        Location loc = blockLocation.clone().add(0.5, 1.2, 0.5);
        
        // Create invisible armor stand for text
        ArmorStand hologram = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setCustomNameVisible(true);
        hologram.customName(net.kyori.adventure.text.Component.text(text));
        hologram.setMarker(true);
        hologram.setSmall(true);
        
        // Animate upward movement
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 40; // 2 seconds
            
            @Override
            public void run() {
                if (ticks >= maxTicks || hologram.isDead()) {
                    hologram.remove();
                    cancel();
                    return;
                }
                
                // Move upward slowly
                Location newLoc = hologram.getLocation().add(0, 0.04, 0);
                hologram.teleport(newLoc);
                
                // Fade out effect by changing text transparency
                if (ticks > maxTicks * 0.6) {
                    double fadePercent = (ticks - maxTicks * 0.6) / (maxTicks * 0.4);
                    String fadeColor = isStored ? "§7" : "§8";
                    hologram.customName(net.kyori.adventure.text.Component.text(fadeColor + text.substring(2)));
                }
                
                ticks++;
            }
        }.runTaskTimer(getPlugin(), 0L, 1L);
    }
    
    /**
     * Checks if summary should be shown and displays it
     */
    private void checkAndShowSummary(Player player, StorageSession session) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastSummary = lastSummaryTime.getOrDefault(playerId, 0L);
        
        // Show summary every 15 minutes or when session has many items
        boolean timeForSummary = (currentTime - lastSummary) >= SUMMARY_INTERVAL;
        boolean manyItems = session.getTotalItemsStored() >= 100;
        
        if (timeForSummary || manyItems) {
            showStorageSummary(player, session);
            session.reset();
            lastSummaryTime.put(playerId, currentTime);
        }
    }
    
    /**
     * Shows a comprehensive storage summary as a floating hologram above the oneblock core
     */
    private void showStorageSummary(Player player, StorageSession session) {
        if (session.getStoredItems().isEmpty()) return;
        
        // Try to get the oneblock core location, fallback to player location
        Location coreLocation = session.getCoreLocation();
        if (coreLocation == null) {
            coreLocation = player.getLocation();
        }
        
        // Position summary above the oneblock core
        Location loc = coreLocation.clone().add(0.5, 3.5, 0.5);
        
        // Create main summary hologram
        ArmorStand mainHologram = createHologram(loc, 
            "§6§l[STORAGE SUMMARY]");
        
        // Show top stored items
        List<Map.Entry<Material, Integer>> topItems = session.getTopStoredItems(5);
        List<ArmorStand> itemHolograms = new ArrayList<>();
        
        for (int i = 0; i < topItems.size(); i++) {
            Map.Entry<Material, Integer> entry = topItems.get(i);
            String itemText = "§a+" + formatNumber(entry.getValue()) + " §f" + 
                formatMaterialName(entry.getKey());
            
            ArmorStand itemHologram = createHologram(
                loc.clone().add(0, -0.3 * (i + 1), 0), 
                itemText
            );
            itemHolograms.add(itemHologram);
        }
        
        // Show total summary
        int totalItems = session.getTotalItemsStored();
        int uniqueTypes = session.getStoredItems().size();
        
        ArmorStand totalHologram = createHologram(
            loc.clone().add(0, -0.3 * (topItems.size() + 2), 0),
            "§7Total: §e" + formatNumber(totalItems) + " items §7(§b" + uniqueTypes + " types§7)"
        );
        
        // Remove all holograms after 8 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                mainHologram.remove();
                itemHolograms.forEach(ArmorStand::remove);
                totalHologram.remove();
            }
        }.runTaskLater(getPlugin(), 160L); // 8 seconds
        
        // Send chat message as backup
        player.sendMessage("§6[Storage] §7Stored §e" + formatNumber(totalItems) + 
            " items §7of §b" + uniqueTypes + " types §7in the last period!");
    }
    
    /**
     * Creates a hologram armor stand
     */
    private ArmorStand createHologram(Location location, String text) {
        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setCustomNameVisible(true);
        hologram.customName(net.kyori.adventure.text.Component.text(text));
        hologram.setMarker(true);
        hologram.setSmall(true);
        return hologram;
    }
    
    /**
     * Formats material name for display
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) formatted.append(word.substring(1));
            }
        }
        return formatted.toString();
    }
    
    /**
     * Formats numbers with K/M/B suffixes
     */
    private String formatNumber(int number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
    
    /**
     * Cleans up session when player leaves
     */
    public void cleanupPlayer(UUID playerId) {
        activeSessions.remove(playerId);
        lastSummaryTime.remove(playerId);
    }
    
    /**
     * Gets the plugin instance - implement based on your plugin structure
     */
    private org.bukkit.plugin.Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("JExOneblock");
    }
    
    /**
     * Storage session for tracking accumulated items
     */
    private static class StorageSession {
        private final Player player;
        private final Map<Material, Integer> storedItems = new HashMap<>();
        private final long startTime;
        private Location coreLocation;
        
        public StorageSession(Player player) {
            this.player = player;
            this.startTime = System.currentTimeMillis();
        }
        
        public void addStoredItem(Material material, int amount) {
            storedItems.merge(material, amount, Integer::sum);
        }
        
        public void setCoreLocation(Location location) {
            this.coreLocation = location;
        }
        
        public Location getCoreLocation() {
            return coreLocation;
        }
        
        public Map<Material, Integer> getStoredItems() {
            return storedItems;
        }
        
        public int getTotalItemsStored() {
            return storedItems.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public List<Map.Entry<Material, Integer>> getTopStoredItems(int limit) {
            return storedItems.entrySet().stream()
                .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                .limit(limit)
                .toList();
        }
        
        public void reset() {
            storedItems.clear();
        }
        
        public long getSessionDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }
}