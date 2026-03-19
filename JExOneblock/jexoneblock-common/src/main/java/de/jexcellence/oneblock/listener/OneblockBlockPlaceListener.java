package de.jexcellence.oneblock.listener;

import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.region.RegionPermission;
import de.jexcellence.oneblock.region.IslandRegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Handles block placement events with region protection integration.
 * Prevents players from placing blocks outside their island boundaries.
 */
public class OneblockBlockPlaceListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(OneblockBlockPlaceListener.class.getName());

    private final JExOneblock plugin;
    private final IslandRegionManager regionManager;

    public OneblockBlockPlaceListener(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        
        LOGGER.info("OneBlock block place listener initialized with region protection");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Validate region permissions for block placement
        if (!validateBlockPlacePermissions(player, location)) {
            event.setCancelled(true);
        }
    }

    /**
     * Validates that the player has permission to place blocks at the given location.
     */
    private boolean validateBlockPlacePermissions(@NotNull Player player, @NotNull Location location) {
        try {
            // Admin bypass
            if (player.hasPermission("jexoneblock.admin.bypass.region")) {
                return true;
            }

            // Check if location is within a region where the player has build permission
            if (!regionManager.hasPermission(player, location, RegionPermission.PermissionTypes.BUILD)) {
                // Send appropriate error message
                var region = regionManager.findRegionAt(location);
                if (region == null) {
                    new I18n.Builder("region.boundary_violation", player).build().sendMessage();
                } else {
                    new I18n.Builder("region.permission_denied", player)
                        .withPlaceholder("action", "place blocks")
                        .build().sendMessage();
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            LOGGER.warning("Error validating block place permissions for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
}