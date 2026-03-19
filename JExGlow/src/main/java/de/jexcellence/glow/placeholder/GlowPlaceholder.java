package de.jexcellence.glow.placeholder;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.glow.factory.GlowFactory;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PlaceholderAPI expansion for JExGlow.
 * <p>
 * Provides the %jexglow_status% placeholder that returns "true" or "false"
 * based on whether a player has the glow effect enabled.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class GlowPlaceholder extends PlaceholderExpansion {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("GlowPlaceholder");

    @Override
    public @NotNull String getIdentifier() {
        return "jexglow";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JExcellence";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion registered across reloads
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return null;
        }

        // Handle %jexglow_status% placeholder
        if (identifier.equalsIgnoreCase("glow_status")) {
            try {
                var glowService = GlowFactory.getService();
                
                // Use join() to get the result synchronously (cached lookup should be fast)
                Boolean isEnabled = glowService.isGlowEnabled(player.getUniqueId())
                    .exceptionally(throwable -> {
                        LOGGER.log(Level.WARNING, 
                            "Failed to check glow status for placeholder for " + player.getName(), 
                            throwable);
                        return false;
                    })
                    .join();
                
                return isEnabled ? "true" : "false";
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, 
                    "Error processing glow placeholder for " + player.getName(), 
                    e);
                return "false";
            }
        }

        return null; // Unknown placeholder
    }
}
