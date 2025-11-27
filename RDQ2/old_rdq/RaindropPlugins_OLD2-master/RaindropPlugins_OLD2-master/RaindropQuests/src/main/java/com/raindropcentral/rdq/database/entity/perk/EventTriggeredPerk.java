package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Abstract base class for perks that are triggered by game events.
 * <p>
 * This class provides common functionality for all event-triggered perks,
 * including event registration, player tracking, cooldown management,
 * and automatic event handling. Concrete implementations define the
 * specific event logic and conditions by implementing their own event handlers.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since TBD
 */
@MappedSuperclass
public abstract class EventTriggeredPerk extends RPerk implements Listener {
    
    @Transient
    private static final Logger LOGGER = Logger.getLogger(EventTriggeredPerk.class.getName());
    
    /**
     * The plugin instance used for event registration.
     */
    @Transient
    private RDQImpl rdq;
    
    /**
     * Tracks which players currently have this perk active.
     * Only players in this set will have the perk effects applied.
     */
    @Transient
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * Tracks cooldowns for each player to prevent spam activation.
     * Maps player UUID to the timestamp when they can use the perk again.
     */
    @Transient
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Whether this perk's event listeners are currently registered.
     */
    @Transient
    private boolean listenersRegistered = false;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected EventTriggeredPerk() {
        super();
    }
    
    /**
     * Constructs a new EventTriggeredPerk.
     *
     * @param identifier the unique identifier for this perk
     * @param perkSection the perk configuration section
     * @param rdq the RDQImpl plugin instance
     */
    protected EventTriggeredPerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull RDQImpl rdq
    ) {
        super(identifier, perkSection, EPerkType.EVENT_TRIGGERED);
        this.rdq = rdq;
    }
    
    /**
     * Sets the RDQImpl plugin instance. This method should be called after JPA instantiation
     * if the no-argument constructor was used.
     *
     * @param rdq the RDQImpl plugin instance
     */
    public void setRdq(final @NotNull RDQImpl rdq) {
        this.rdq = rdq;
    }
    
    /**
     * Gets the RDQImpl plugin instance.
     *
     * @return the RDQImpl plugin instance
     */
    public RDQImpl getRdq() {
        return this.rdq;
    }
    
    @Override
    public boolean performActivation() {
        if (!this.isEnabled()) {
            return false;
        }
        
        if (!this.listenersRegistered) {
            this.registerEventListeners();
        }
        
        return true;
    }
    
    @Override
    public boolean performDeactivation() {
        if (this.listenersRegistered) {
            this.unregisterEventListeners();
        }
        
        this.activePlayers.clear();
        this.playerCooldowns.clear();
        
        return true;
    }
    
    @Override
    public boolean canPerformActivation() {
        return this.isEnabled();
    }
    
    @Override
    public void performTrigger() {
        // Event-triggered perks don't use manual triggering
    }
    
    /**
     * Adds a player to the active players set.
     * This player will now receive the perk effects when events occur.
     *
     * @param player the player to activate the perk for
     * @return true if the player was added successfully
     */
    public boolean activateFoRDQPlayer(final @NotNull RDQPlayer player) {
        if (!this.isEnabled()) {
            return false;
        }
        
        final boolean added = this.activePlayers.add(player.getUniqueId());
        
        if (added) {
            LOGGER.info("Activated " + this.getIdentifier() + " perk for player " + player.getPlayerName());
        }
        
        return added;
    }
    
    /**
     * Removes a player from the active players set.
     * This player will no longer receive the perk effects.
     *
     * @param player the player to deactivate the perk for
     * @return true if the player was removed successfully
     */
    public boolean deactivateFoRDQPlayer(final @NotNull RDQPlayer player) {
        final boolean removed = this.activePlayers.remove(player.getUniqueId());
        this.playerCooldowns.remove(player.getUniqueId());
        
        if (removed) {
            LOGGER.info("Deactivated " + this.getIdentifier() + " perk for player " + player.getPlayerName());
        }
        
        return removed;
    }
    
    /**
     * Checks if a player has this perk active.
     *
     * @param playerId the UUID of the player to check
     * @return true if the player has the perk active
     */
    public boolean isActiveFoRDQPlayer(final @NotNull UUID playerId) {
        return this.activePlayers.contains(playerId);
    }
    
    /**
     * Checks if a player is currently on cooldown for this perk.
     *
     * @param playerId the UUID of the player to check
     * @return true if the player is on cooldown
     */
    public boolean isPlayerOnCooldown(final @NotNull UUID playerId) {
        final Long cooldownEnd = this.playerCooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }
        
        final long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            this.playerCooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Sets a cooldown for a player.
     * This method should be called by concrete implementations after successfully
     * applying a perk effect.
     *
     * @param player the Bukkit player to set cooldown for
     */
    protected void setCooldownFoRDQPlayer(final @NotNull Player player) {
        final long cooldownSeconds = this.getPerkSection()
                                         .getPermissionCooldowns()
                                         .getEffectiveCooldown(player);
        
        if (cooldownSeconds > 0) {
            final long cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000L);
            this.playerCooldowns.put(player.getUniqueId(), cooldownEnd);
        }
    }
    
    /**
     * Utility method for concrete perks to check if they should process an event.
     * This method combines the common checks: is player active, is player on cooldown.
     *
     * @param playerId the UUID of the player
     * @return true if the perk should process the event for this player
     */
    protected boolean shouldProcessEventFoRDQPlayer(final @NotNull UUID playerId) {
        return this.isActiveFoRDQPlayer(playerId) && !this.isPlayerOnCooldown(playerId);
    }
    
    /**
     * Utility method for concrete perks to log perk activation.
     *
     * @param playerName the name of the player
     * @param eventType the type of event that triggered the perk
     */
    protected void logPerkActivation(final @NotNull String playerName, final @NotNull String eventType) {
        LOGGER.info("Triggered " + this.getIdentifier() + " perk for player " +
                    playerName + " on " + eventType);
    }
    
    /**
     * Registers the event listeners for this perk.
     * This should be called when the perk is activated.
     */
    private void registerEventListeners() {
        if (this.rdq != null) {
            Bukkit.getPluginManager().registerEvents(this, this.rdq.getImpl());
            this.listenersRegistered = true;
            LOGGER.info("Registered event listeners for perk: " + this.getIdentifier());
        } else {
            LOGGER.warning("Cannot register event listeners for perk " + this.getIdentifier() +
                           " - RDQImpl plugin instance is null");
        }
    }
    
    /**
     * Unregisters the event listeners for this perk.
     * This should be called when the perk is deactivated.
     */
    private void unregisterEventListeners() {
        this.listenersRegistered = false;
        LOGGER.info("Unregistered event listeners for perk: " + this.getIdentifier());
    }
    
    /**
     * Gets the set of currently active players.
     * This is primarily for monitoring and debugging purposes.
     *
     * @return an unmodifiable view of the active players set
     */
    public Set<UUID> getActivePlayers() {
        return Set.copyOf(this.activePlayers);
    }
}