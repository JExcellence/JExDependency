package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.perk.runtime.DefaultCooldownStore;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Abstract base class for perks that are triggered by game events.
 * <p>
 * Provides listener lifecycle, active player tracking, and centralized cooldown handling via CooldownStore.
 * Concrete implementations attach Bukkit @EventHandler methods. This base ensures proper registration
 * and safe teardown.
 */
@MappedSuperclass
public abstract class EventTriggeredPerk extends RPerk implements Listener {

    @Transient
    private static final Logger LOGGER = Logger.getLogger(EventTriggeredPerk.class.getName());

    /** Plugin entry used for listener registration. */
    @Transient
    private RDQ rdq;

    /** Players for which this perk is currently active. */
    @Transient
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();

    /** Whether listeners are currently registered. */
    @Transient
    private volatile boolean listenersRegistered = false;

    /** Protected no-arg constructor for JPA. */
    protected EventTriggeredPerk() {
        super();
    }

    /**
     * Constructs a new EventTriggeredPerk definition.
     */
    protected EventTriggeredPerk(
            final @NotNull String identifier,
            final @NotNull PerkSection perkSection,
            final @NotNull RDQ rdq
    ) {
        super(identifier, perkSection, EPerkType.EVENT_TRIGGERED);
        this.rdq = rdq;
    }

    /** Assigns the plugin after JPA instantiation if needed. */
    public void setRdq(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    public RDQ getRdq() {
        return this.rdq;
    }

    @Override
    public boolean performActivation() {
        if (!this.isEnabled()) {
            return false;
        }
        if (!this.listenersRegistered) {
            registerEventListeners();
        }
        return true;
    }

    @Override
    public boolean performDeactivation() {
        if (this.listenersRegistered) {
            unregisterEventListeners();
        }
        this.activePlayers.clear();
        return true;
    }

    @Override
    public boolean canPerformActivation() {
        return this.isEnabled();
    }

    @Override
    public void performTrigger() {
        // Event-triggered perks execute via event listeners only
    }

    /** Adds a player to the active set. */
    public boolean activateForPlayer(final @NotNull com.raindropcentral.rdq.database.entity.player.RDQPlayer player) {
        if (!this.isEnabled()) return false;
        final boolean added = this.activePlayers.add(player.getUniqueId());
        if (added) {
            LOGGER.info("Activated perk " + this.getIdentifier() + " for player " + player.getPlayerName());
        }
        return added;
    }

    /** Removes a player from the active set and clears cooldown entry if any. */
    public boolean deactivateForPlayer(final @NotNull com.raindropcentral.rdq.database.entity.player.RDQPlayer player) {
        final boolean removed = this.activePlayers.remove(player.getUniqueId());
        final Player bukkitPlayer = Bukkit.getPlayer(player.getUniqueId());
        if (bukkitPlayer != null) {
            DefaultCooldownStore.getInstance().clearCooldown(bukkitPlayer, this.getIdentifier());
        }
        if (removed) {
            LOGGER.info("Deactivated perk " + this.getIdentifier() + " for player " + player.getPlayerName());
        }
        return removed;
    }

    /** Returns whether the player has this perk active. */
    public boolean isActiveForPlayer(final @NotNull UUID playerId) {
        return this.activePlayers.contains(playerId);
    }

    /** Returns whether the player is on cooldown for this perk. */
    public boolean isPlayerOnCooldown(final @NotNull UUID playerId) {
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        return DefaultCooldownStore.getInstance().isOnCooldown(player, this.getIdentifier());
    }

    /** Sets cooldown for the player using permission-based cooldown seconds. */
    protected void setCooldownForPlayer(final @NotNull Player player) {
        final long cooldownSeconds = this.getPerkSection()
                .getPermissionCooldowns()
                .getEffectiveCooldown(player);
        if (cooldownSeconds <= 0) return;
        DefaultCooldownStore.getInstance().setCooldown(player, this.getIdentifier(), cooldownSeconds);
    }

    /** Combines active and cooldown checks for event processing. */
    protected boolean shouldProcessEventForPlayer(final @NotNull UUID playerId) {
        return isActiveForPlayer(playerId) && !isPlayerOnCooldown(playerId);
    }

    /** Helper log for concrete perks. */
    protected void logPerkActivation(final @NotNull String playerName, final @NotNull String eventType) {
        LOGGER.info("Triggered perk " + this.getIdentifier() + " for player " + playerName + " on " + eventType);
    }

    /** Registers Bukkit listeners for this perk. */
    private void registerEventListeners() {
        if (this.rdq == null) {
            LOGGER.warning("Cannot register listeners for perk " + this.getIdentifier() + ": RDQ plugin instance is null");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this.rdq.getPlugin());
        this.listenersRegistered = true;
        LOGGER.info("Registered event listeners for perk: " + this.getIdentifier());
    }

    /** Unregisters listeners and resets the flag. */
    private void unregisterEventListeners() {
        HandlerList.unregisterAll(this);
        this.listenersRegistered = false;
        LOGGER.info("Unregistered event listeners for perk: " + this.getIdentifier());
    }

    /** Exposes a snapshot of active players for diagnostics. */
    public Set<UUID> getActivePlayers() {
        return Set.copyOf(this.activePlayers);
    }
}
