package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Reward that grants a title to a player.
 * <p>
 * This reward provides a placeholder implementation for title granting.
 * When a title system is available, it can be integrated by setting
 * the title service via {@link #setTitleService(TitleService)}.
 * </p>
 * <p>
 * If no title service is available, the reward will log the title grant
 * and complete successfully, allowing for future integration without
 * breaking existing functionality.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since TBD
 */
@JsonTypeName("TITLE")
public final class TitleReward extends AbstractReward {
    
    private static final Logger LOGGER = Logger.getLogger(TitleReward.class.getName());
    
    @Nullable
    private static TitleService titleService;

    private final String titleId;
    private final String displayName;

    /**
     * Sets the title service for title operations.
     * <p>
     * This should be called during plugin initialization if a title
     * system is available.
     * </p>
     *
     * @param service the title service, or null to disable integration
     */
    public static void setTitleService(@Nullable final TitleService service) {
        titleService = service;
        if (service != null) {
            LOGGER.info("Title service integration enabled");
        }
    }

    /**
     * Creates a new title reward.
     *
     * @param titleId     the unique identifier of the title
     * @param displayName the display name of the title (optional, for logging)
     */
    @JsonCreator
    public TitleReward(
        @JsonProperty("titleId") @NotNull String titleId,
        @JsonProperty("displayName") @Nullable String displayName
    ) {
        this.titleId = titleId;
        this.displayName = displayName != null ? displayName : titleId;
    }

    @Override
    public @NotNull String getTypeId() {
        return "TITLE";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Get any plugin for scheduling (use first available)
        final Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];
        
        // Must run on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (titleService != null) {
                    // Use title service if available
                    boolean success = titleService.grantTitle(player, titleId);
                    if (success) {
                        LOGGER.fine("Granted title '" + displayName + "' to " + player.getName());
                    } else {
                        LOGGER.warning("Failed to grant title '" + displayName + "' to " + player.getName());
                    }
                    future.complete(success);
                } else {
                    // No title service available - log and succeed
                    LOGGER.fine("Title reward granted (no title system): '" + displayName + "' to " + player.getName());
                    future.complete(true);
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to grant title to " + player.getName() + ": " + e.getMessage());
                future.complete(false);
            }
        });
        
        return future;
    }

    @Override
    public double getEstimatedValue() {
        // Titles are cosmetic, assign nominal value
        return 50.0;
    }

    /**
     * Gets the title identifier.
     *
     * @return the title ID
     */
    public String getTitleId() {
        return titleId;
    }

    /**
     * Gets the display name of the title.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void validate() {
        if (titleId == null || titleId.isEmpty()) {
            throw new IllegalArgumentException("Title ID cannot be empty");
        }
    }

    /**
     * Interface for title system integration.
     * <p>
     * Implement this interface to integrate with your title system.
     * </p>
     */
    public interface TitleService {
        /**
         * Grants a title to a player.
         *
         * @param player  the player to grant the title to
         * @param titleId the unique identifier of the title
         * @return true if the title was granted successfully
         */
        boolean grantTitle(@NotNull Player player, @NotNull String titleId);
    }
}
