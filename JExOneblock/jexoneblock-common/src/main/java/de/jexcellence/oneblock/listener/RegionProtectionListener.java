package de.jexcellence.oneblock.listener;

import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import de.jexcellence.oneblock.database.entity.region.RegionPermission;
import de.jexcellence.oneblock.region.IslandRegionManager;
import de.jexcellence.oneblock.region.RegionBoundaryChecker;
import de.jexcellence.oneblock.region.RegionBoundaryChecker.BoundaryCheckResult;
import de.jexcellence.oneblock.region.RegionBoundaryChecker.BoundaryViolationType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Comprehensive region protection listener that prevents unauthorized actions
 * outside island boundaries and enforces permission-based access control.
 */
public class RegionProtectionListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(RegionProtectionListener.class.getName());

    private final JExOneblock plugin;
    private final IslandRegionManager regionManager;
    private final RegionBoundaryChecker boundaryChecker;
    
    // Cache for recent boundary warnings to prevent spam
    private final Map<UUID, Long> lastWarningTime = new ConcurrentHashMap<>();
    private static final long WARNING_COOLDOWN_MS = 3000; // 3 seconds
    
    // Materials that require special interaction permissions
    private static final Set<Material> INTERACTIVE_BLOCKS = Set.of(
        Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
        Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.ANVIL,
        Material.BREWING_STAND, Material.CAULDRON, Material.COMPOSTER,
        Material.BARREL, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.LECTERN, Material.LOOM, Material.STONECUTTER, Material.GRINDSTONE,
        Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE, Material.SMITHING_TABLE
    );
    
    private static final Set<Material> DOOR_BLOCKS = Set.of(
        Material.OAK_DOOR, Material.BIRCH_DOOR, Material.SPRUCE_DOOR,
        Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
        Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR,
        Material.OAK_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.SPRUCE_TRAPDOOR,
        Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
        Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR,
        Material.OAK_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.SPRUCE_FENCE_GATE,
        Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
        Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE
    );
    
    private static final Set<Material> REDSTONE_BLOCKS = Set.of(
        Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON,
        Material.BIRCH_BUTTON, Material.SPRUCE_BUTTON, Material.JUNGLE_BUTTON,
        Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON,
        Material.WARPED_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON,
        Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR,
        Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
        Material.TRIPWIRE_HOOK, Material.DAYLIGHT_DETECTOR
    );

    public RegionProtectionListener(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.boundaryChecker = plugin.getBoundaryChecker();
        
        LOGGER.info("Region protection listener initialized");
    }

    // ========== BLOCK PROTECTION ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.BUILD)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "build");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Check if this is a OneBlock break (handled by OneblockBlockBreakListener)
        if (isOneBlockLocation(player, location)) {
            return; // Let OneblockBlockBreakListener handle this
        }

        if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.BREAK)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "break");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        Location location = clickedBlock.getLocation();
        Material blockType = clickedBlock.getType();

        // Determine required permission based on block type
        String requiredPermission = getRequiredInteractionPermission(blockType);
        
        if (requiredPermission != null && !hasPermissionAtLocation(player, location, requiredPermission)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "interact with " + blockType.name().toLowerCase());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(@NotNull BlockFromToEvent event) {
        // Prevent liquid flow across region boundaries
        Location fromLocation = event.getBlock().getLocation();
        Location toLocation = event.getToBlock().getLocation();

        IslandRegion fromRegion = regionManager.findRegionAt(fromLocation);
        IslandRegion toRegion = regionManager.findRegionAt(toLocation);

        // If flowing from one region to another (or to no region), cancel
        if (fromRegion != null && !fromRegion.equals(toRegion)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPistonExtend(@NotNull BlockPistonExtendEvent event) {
        // Prevent pistons from pushing blocks across region boundaries
        Location pistonLocation = event.getBlock().getLocation();
        IslandRegion pistonRegion = regionManager.findRegionAt(pistonLocation);

        for (Block block : event.getBlocks()) {
            Location blockLocation = block.getLocation();
            IslandRegion blockRegion = regionManager.findRegionAt(blockLocation);

            if (!isRegionMatch(pistonRegion, blockRegion)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPistonRetract(@NotNull BlockPistonRetractEvent event) {
        // Prevent pistons from pulling blocks across region boundaries
        Location pistonLocation = event.getBlock().getLocation();
        IslandRegion pistonRegion = regionManager.findRegionAt(pistonLocation);

        for (Block block : event.getBlocks()) {
            Location blockLocation = block.getLocation();
            IslandRegion blockRegion = regionManager.findRegionAt(blockLocation);

            if (!isRegionMatch(pistonRegion, blockRegion)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ========== ENTITY PROTECTION ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Entity target = event.getEntity();
        Location location = target.getLocation();

        // Check PvP permission for players
        if (target instanceof Player) {
            if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.PVP)) {
                event.setCancelled(true);
                sendBoundaryViolationMessage(player, location, "engage in PvP");
                return;
            }
        }

        // Check animal interaction permission for animals
        if (isAnimal(target)) {
            if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.ANIMAL_INTERACT)) {
                event.setCancelled(true);
                sendBoundaryViolationMessage(player, location, "interact with animals");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getRightClicked();
        Location location = target.getLocation();

        // Check villager trading permission
        if (target instanceof org.bukkit.entity.Villager) {
            if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.VILLAGER_TRADE)) {
                event.setCancelled(true);
                sendBoundaryViolationMessage(player, location, "trade with villagers");
                return;
            }
        }

        // Check animal interaction permission
        if (isAnimal(target)) {
            if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.ANIMAL_INTERACT)) {
                event.setCancelled(true);
                sendBoundaryViolationMessage(player, location, "interact with animals");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingPlace(@NotNull HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        Location location = event.getEntity().getLocation();

        if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.BUILD)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "place hanging entities");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreakByEntity(@NotNull HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) return;

        Location location = event.getEntity().getLocation();

        if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.BREAK)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "break hanging entities");
        }
    }

    // ========== ITEM PROTECTION ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location location = event.getItemDrop().getLocation();

        if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.ITEM_DROP)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "drop items");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(@NotNull PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        Location location = event.getItem().getLocation();

        if (!hasPermissionAtLocation(player, location, RegionPermission.PermissionTypes.ITEM_PICKUP)) {
            event.setCancelled(true);
            sendBoundaryViolationMessage(player, location, "pick up items");
        }
    }

    // ========== TELEPORTATION PROTECTION ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location toLocation = event.getTo();
        
        if (toLocation == null) return;

        // Allow teleportation if player has teleport permission or is island owner
        if (!hasPermissionAtLocation(player, toLocation, RegionPermission.PermissionTypes.TELEPORT)) {
            // Check if this is an admin bypass
            if (player.hasPermission("jexoneblock.admin.bypass.region")) {
                return;
            }

            event.setCancelled(true);
            sendBoundaryViolationMessage(player, toLocation, "teleport");
        }
    }

    // ========== MOVEMENT MONITORING ==========

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        // Only check if player actually moved to a different block
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null || (from.getBlockX() == to.getBlockX() && 
                          from.getBlockZ() == to.getBlockZ() && 
                          from.getWorld().equals(to.getWorld()))) {
            return;
        }

        Player player = event.getPlayer();
        
        // Check boundary warnings
        checkBoundaryWarnings(player, to);
    }

    // ========== UTILITY METHODS ==========

    /**
     * Checks if a player has permission to perform an action at a specific location.
     */
    private boolean hasPermissionAtLocation(@NotNull Player player, @NotNull Location location, @NotNull String permission) {
        // Admin bypass
        if (player.hasPermission("jexoneblock.admin.bypass.region")) {
            return true;
        }

        // Use region manager to check permissions
        return regionManager.hasPermission(player, location, permission);
    }

    /**
     * Checks if a location is a OneBlock location that should be handled by OneblockBlockBreakListener.
     */
    private boolean isOneBlockLocation(@NotNull Player player, @NotNull Location location) {
        try {
            // This is a simplified check - the actual OneBlock validation is done in OneblockBlockBreakListener
            // We just need to identify potential OneBlock locations to avoid double-processing
            var playerRepository = plugin.getOneblockPlayerRepository();
            var islandRepository = plugin.getOneblockIslandRepository();
            
            var oneblockPlayer = playerRepository.findByUuid(player.getUniqueId());
            if (oneblockPlayer == null || !oneblockPlayer.hasIsland()) {
                return false;
            }
            
            var island = islandRepository.findByOwner(oneblockPlayer.getUniqueId());
            if (island == null || island.getOneblock() == null) {
                return false;
            }
            
            return island.getOneblock().isAtLocation(location);
        } catch (Exception e) {
            // If we can't determine, assume it's not a OneBlock location
            return false;
        }
    }

    /**
     * Gets the required permission for interacting with a specific block type.
     */
    @Nullable
    private String getRequiredInteractionPermission(@NotNull Material blockType) {
        if (INTERACTIVE_BLOCKS.contains(blockType)) {
            return RegionPermission.PermissionTypes.CHEST_ACCESS;
        } else if (DOOR_BLOCKS.contains(blockType)) {
            return RegionPermission.PermissionTypes.DOOR_ACCESS;
        } else if (REDSTONE_BLOCKS.contains(blockType)) {
            return RegionPermission.PermissionTypes.REDSTONE_ACCESS;
        } else {
            return RegionPermission.PermissionTypes.INTERACT;
        }
    }

    /**
     * Checks if an entity is considered an animal for permission purposes.
     */
    private boolean isAnimal(@NotNull Entity entity) {
        return entity instanceof org.bukkit.entity.Animals ||
               entity instanceof org.bukkit.entity.Ambient ||
               entity instanceof org.bukkit.entity.WaterMob;
    }

    /**
     * Checks if two regions match (both null or same region).
     */
    private boolean isRegionMatch(@Nullable IslandRegion region1, @Nullable IslandRegion region2) {
        if (region1 == null && region2 == null) return true;
        if (region1 == null || region2 == null) return false;
        return region1.equals(region2);
    }

    /**
     * Sends a boundary violation message to the player.
     */
    private void sendBoundaryViolationMessage(@NotNull Player player, @NotNull Location location, @NotNull String action) {
        IslandRegion region = regionManager.findRegionAt(location);
        
        if (region == null) {
            // Outside any region
            new I18n.Builder("region.no_region", player)
                .build().sendMessage();
        } else {
            // Inside a region but no permission
            new I18n.Builder("region.permission_denied", player)
                .withPlaceholder("action", action)
                .build().sendMessage();
        }
    }

    /**
     * Checks and sends boundary warnings to players approaching region boundaries.
     */
    private void checkBoundaryWarnings(@NotNull Player player, @NotNull Location location) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        Long lastWarning = lastWarningTime.get(playerId);
        if (lastWarning != null && (currentTime - lastWarning) < WARNING_COOLDOWN_MS) {
            return;
        }

        IslandRegion region = regionManager.findRegionAt(location);
        if (region == null) return;

        BoundaryCheckResult result = boundaryChecker.checkBoundaries(location, region);
        
        if (result.isNearBoundary() && result.isWithinBoundaries()) {
            // Send warning message
            new I18n.Builder("region.boundary_warning", player)
                .withPlaceholder("distance", String.format("%.1f", result.getDistanceFromBoundary()))
                .build().sendMessage();
            
            // Update cooldown
            lastWarningTime.put(playerId, currentTime);
        }
    }

    /**
     * Cleans up cached warning times for offline players.
     */
    public void cleanupWarningCache() {
        long currentTime = System.currentTimeMillis();
        lastWarningTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > (WARNING_COOLDOWN_MS * 10)); // Keep for 30 seconds
    }

    /**
     * Gets cache statistics for monitoring.
     */
    @NotNull
    public Map<String, Object> getCacheStatistics() {
        return Map.of(
            "warningCacheSize", lastWarningTime.size(),
            "warningCooldownMs", WARNING_COOLDOWN_MS
        );
    }
}