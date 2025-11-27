package com.raindropcentral.rdq.config.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration manager for the bounty system.
 * Loads and provides access to bounty-related configuration settings.
 * 
 * Requirements: 17.1, 17.2
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class BountyConfig {
    
    private static final Logger LOGGER = Logger.getLogger(BountyConfig.class.getName());
    private static final String BOUNTY_FOLDER = "bounty";
    private static final String BOUNTY_FILE = "bounty.yml";
    
    private final RDQ rdq;
    private BountySection bountySection;
    
    /**
     * Creates a new BountyConfig instance.
     * 
     * @param rdq the RDQ plugin instance
     */
    public BountyConfig(@NotNull RDQ rdq) {
        this.rdq = rdq;
        loadConfig();
    }
    
    /**
     * Loads the bounty configuration from file.
     * Requirements: 17.1
     */
    private void loadConfig() {
        try {
            var cfgManager = new ConfigManager(rdq.getPlugin(), BOUNTY_FOLDER);
            var cfgKeeper = new ConfigKeeper<>(cfgManager, BOUNTY_FILE, BountySection.class);
            this.bountySection = cfgKeeper.rootSection;
            
            // Validate configuration
            validateConfig();
            
            LOGGER.info("Bounty configuration loaded successfully");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading bounty config, using defaults", e);
            this.bountySection = new BountySection(new EvaluationEnvironmentBuilder());
        }
    }
    
    /**
     * Validates the loaded configuration.
     * Requirements: 17.2
     */
    private void validateConfig() {
        if (bountySection == null) {
            throw new IllegalStateException("Bounty section is null");
        }
        
        // Validate minimum value
        if (bountySection.getMinimumValue() < 0) {
            LOGGER.warning("Minimum bounty value cannot be negative, using 0.0");
        }
        
        // Validate expiry days
        if (bountySection.getExpiryDays() <= 0) {
            LOGGER.warning("Expiry days must be positive, using 7 days");
        }
        
        // Validate max bounties per player
        if (bountySection.getMaxBountiesPerPlayer() <= 0) {
            LOGGER.warning("Max bounties per player must be positive, using 1");
        }
        
        LOGGER.info(String.format("Bounty config validated - ClaimMode: %s, MaxBounties: %d, MinValue: %.2f",
                bountySection.getClaimMode(),
                bountySection.getMaxBountiesPerPlayer(),
                bountySection.getMinimumValue()));
    }
    
    /**
     * Reloads the configuration from file.
     */
    public void reload() {
        loadConfig();
    }
    
    // Configuration getters
    
    /**
     * Gets the claim mode for bounties.
     * 
     * @return the claim mode
     */
    @NotNull
    public ClaimMode getClaimMode() {
        return switch (bountySection.getClaimMode()) {
            case LAST_HIT -> ClaimMode.LAST_HIT;
            case MOST_DAMAGE -> ClaimMode.MOST_DAMAGE;
        };
    }
    
    /**
     * Gets the creation cost for bounties.
     * 
     * @return the creation cost
     */
    public double getCreationCost() {
        return bountySection.getCreationCost();
    }
    
    /**
     * Gets the maximum bounties per player.
     * 
     * @return the maximum bounties per player
     */
    public int getMaxBountiesPerPlayer() {
        return bountySection.getMaxBountiesPerPlayer();
    }
    
    /**
     * Checks if bounty creation should be broadcast.
     * 
     * @return true if creation should be broadcast
     */
    public boolean shouldBroadcastCreation() {
        return bountySection.shouldBroadcastCreation();
    }
    
    /**
     * Checks if bounty completion should be broadcast.
     * 
     * @return true if completion should be broadcast
     */
    public boolean shouldBroadcastCompletion() {
        return bountySection.shouldBroadcastCompletion();
    }
    
    /**
     * Checks if the bounty system is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return bountySection.isEnabled();
    }
    
    /**
     * Gets the minimum bounty value.
     * 
     * @return the minimum value
     */
    public double getMinimumValue() {
        return bountySection.getMinimumValue();
    }
    
    /**
     * Gets the expiry days for bounties.
     * 
     * @return the expiry days
     */
    public int getExpiryDays() {
        return bountySection.getExpiryDays();
    }
    
    /**
     * Gets the kill attribution mode.
     * 
     * @return the kill attribution mode
     */
    @NotNull
    public String getKillAttributionMode() {
        return bountySection.getKillAttributionMode();
    }
    
    /**
     * Gets the reward distribution mode.
     * 
     * @return the reward distribution mode
     */
    @NotNull
    public String getRewardDistributionMode() {
        return bountySection.getRewardDistributionMode();
    }
    
    /**
     * Checks if creation announcements are enabled.
     * 
     * @return true if announcements are enabled
     */
    public boolean isAnnounceCreation() {
        return bountySection.isAnnounceCreation();
    }
    
    /**
     * Checks if claim announcements are enabled.
     * 
     * @return true if announcements are enabled
     */
    public boolean isAnnounceClaim() {
        return bountySection.isAnnounceClaim();
    }
    
    /**
     * Gets the tab prefix for bounty targets.
     * 
     * @return the tab prefix
     */
    @NotNull
    public String getTabPrefix() {
        return bountySection.getTabPrefix();
    }
    
    /**
     * Gets the chat prefix for bounty targets.
     * 
     * @return the chat prefix
     */
    @NotNull
    public String getChatPrefix() {
        return bountySection.getChatPrefix();
    }
    
    /**
     * Gets the name color for bounty targets.
     * 
     * @return the name color
     */
    @NotNull
    public String getNameColor() {
        return bountySection.getNameColor();
    }
    
    /**
     * Gets the damage tracking window in seconds.
     * 
     * @return the tracking window (default 30 seconds)
     */
    public int getDamageTrackingWindow() {
        return 30; // Default value, can be added to BountySection if needed
    }
    
    /**
     * Checks if visual indicators are enabled.
     * 
     * @return true if visual indicators are enabled (default true)
     */
    public boolean isVisualIndicatorsEnabled() {
        return true; // Default value, can be added to BountySection if needed
    }
}
