package com.raindropcentral.rdq.bounty.announcement;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.config.bounty.BountySection;
import de.jexcellence.jextranslate.api.Placeholder;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Manages bounty-related announcements using JExTranslate for internationalization.
 * Supports both global and radius-based broadcasts with configurable enable/disable settings.
 * 
 * <p>This manager handles:
 * <ul>
 *   <li>Bounty creation announcements</li>
 *   <li>Bounty claim announcements</li>
 *   <li>Global broadcasts (radius = -1)</li>
 *   <li>Radius-based broadcasts (radius > 0)</li>
 *   <li>Respecting announcement enable/disable configuration</li>
 * </ul>
 * 
 * @author RDQ Team
 * @since 1.0.0
 */
public class BountyAnnouncementManager {
    
    private static final Logger LOGGER = Logger.getLogger(BountyAnnouncementManager.class.getName());
    
    private static final TranslationKey BOUNTY_CREATED_KEY = TranslationKey.of("bounty.announcement.created");
    private static final TranslationKey BOUNTY_CLAIMED_KEY = TranslationKey.of("bounty.announcement.claimed");
    
    private final BountySection config;
    private final int broadcastRadius;
    
    /**
     * Creates a new BountyAnnouncementManager with the specified configuration.
     *
     * @param config the bounty configuration section
     * @param broadcastRadius the broadcast radius (-1 for global, positive for radius-based)
     */
    public BountyAnnouncementManager(@NotNull BountySection config, int broadcastRadius) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.broadcastRadius = broadcastRadius;
    }
    
    /**
     * Announces the creation of a bounty.
     * 
     * <p>Broadcasts a message using JExTranslate with placeholders for:
     * <ul>
     *   <li>{commissioner} - The player who created the bounty</li>
     *   <li>{target} - The player who has the bounty placed on them</li>
     *   <li>{value} - The total estimated value of the bounty</li>
     * </ul>
     *
     * @param bounty the bounty that was created
     * @param location the location from which to broadcast (for radius-based broadcasts)
     */
    public void announceBountyCreation(@NotNull Bounty bounty, @NotNull Location location) {
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        
        if (!config.isAnnounceCreation()) {
            return;
        }
        
        broadcastMessage(
            BOUNTY_CREATED_KEY,
            location,
            Placeholder.of("commissioner", bounty.commissionerName()),
            Placeholder.of("target", bounty.targetName()),
            Placeholder.of("value", bounty.totalEstimatedValue())
        );
    }
    
    /**
     * Announces the claiming of a bounty.
     * 
     * <p>Broadcasts a message using JExTranslate with placeholders for:
     * <ul>
     *   <li>{hunter} - The player who claimed the bounty</li>
     *   <li>{target} - The player whose bounty was claimed</li>
     *   <li>{value} - The total estimated value of the bounty</li>
     * </ul>
     *
     * @param bounty the bounty that was claimed
     * @param hunterName the name of the player who claimed the bounty
     * @param location the location from which to broadcast (for radius-based broadcasts)
     */
    public void announceBountyClaim(@NotNull Bounty bounty, @NotNull String hunterName, @NotNull Location location) {
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(hunterName, "hunterName cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        
        if (!config.isAnnounceClaim()) {
            return;
        }
        
        broadcastMessage(
            BOUNTY_CLAIMED_KEY,
            location,
            Placeholder.of("hunter", hunterName),
            Placeholder.of("target", bounty.targetName()),
            Placeholder.of("value", bounty.totalEstimatedValue())
        );
    }
    
    /**
     * Broadcasts a message to players based on the configured broadcast radius.
     * 
     * <p>If radius is -1, broadcasts globally to all online players.
     * If radius is positive, broadcasts only to players within that radius of the location.
     *
     * @param key the translation key for the message
     * @param location the center location for radius-based broadcasts
     * @param placeholders the placeholders to apply to the message
     */
    private void broadcastMessage(
        @NotNull TranslationKey key,
        @NotNull Location location,
        @NotNull Placeholder... placeholders
    ) {
        Collection<? extends Player> recipients = getRecipients(location);
        
        for (Player player : recipients) {
            try {
                TranslationService service = TranslationService.create(key, player);
                
                // Add all placeholders
                for (Placeholder placeholder : placeholders) {
                    service = service.with(placeholder);
                }
                
                // Send the message
                service.send();
            } catch (Exception e) {
                LOGGER.warning("Failed to send bounty announcement to player " + player.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets the collection of players who should receive the broadcast based on the configured radius.
     *
     * @param location the center location for radius-based broadcasts
     * @return the collection of players who should receive the message
     */
    @NotNull
    private Collection<? extends Player> getRecipients(@NotNull Location location) {
        if (broadcastRadius == -1) {
            // Global broadcast
            return Bukkit.getOnlinePlayers();
        } else {
            // Radius-based broadcast
            return location.getWorld().getNearbyPlayers(location, broadcastRadius);
        }
    }
    
    /**
     * Checks if bounty creation announcements are enabled.
     *
     * @return true if creation announcements are enabled
     */
    public boolean isCreationAnnouncementEnabled() {
        return config.isAnnounceCreation();
    }
    
    /**
     * Checks if bounty claim announcements are enabled.
     *
     * @return true if claim announcements are enabled
     */
    public boolean isClaimAnnouncementEnabled() {
        return config.isAnnounceClaim();
    }
    
    /**
     * Gets the configured broadcast radius.
     *
     * @return the broadcast radius (-1 for global, positive for radius-based)
     */
    public int getBroadcastRadius() {
        return broadcastRadius;
    }
}
