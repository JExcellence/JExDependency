package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.perk.runtime.CooldownService;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkStateService;
import com.raindropcentral.rdq.perk.runtime.PerkTriggerService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Coordinates the lifecycle of player perks across the RDQ platform, providing a
 * single integration point for loading perk definitions, wiring runtime
 * listeners, and resolving dependencies between perks and other gameplay
 * systems.
 *
 * <p>Implementations should focus on three core responsibilities:
 * <ul>
 *     <li><strong>Bootstrap</strong> – hydrate perk metadata from configured
 *     sources and register any supporting services that perks rely on.</li>
 *     <li><strong>Activation</strong> – expose hooks to grant, revoke, and query
 *     perk state for a player in synchronization with quest and rank updates.</li>
 *     <li><strong>Lifecycle Safety</strong> – ensure all registrations are
 *     reversible so that server shutdowns or hot reloads clean up listeners and
 *     scheduled tasks deterministically.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> Implementations are expected to guard their
 * state so that asynchronous perk updates defer to the platform's synchronization
 * utilities before mutating shared game state.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 * @see com.raindropcentral.rdq.manager.RDQManager
 */
public interface PerkManager {

    /**
     * Gets the perk registry for accessing runtime perk implementations.
     *
     * @return the perk registry
     */
    PerkRegistry getPerkRegistry();

    /**
     * Gets the perk state service for managing player perk ownership and activation.
     *
     * @return the perk state service
     */
    PerkStateService getPerkStateService();

    /**
     * Gets the perk trigger service for handling event-based perk activation.
     *
     * @return the perk trigger service
     */
    PerkTriggerService getPerkTriggerService();

    /**
     * Initializes the perk system, loading configurations and registering runtimes.
     */
    void initialize();

    /**
     * Shuts down the perk system, cleaning up resources and unregistering listeners.
     */
    void shutdown();

    /**
     * Provides access to the cooldown service backing the perk runtime.
     *
     * @return the cooldown service instance
     */
    default CooldownService getCooldownService() {
        throw new UnsupportedOperationException("Cooldown service is not available in this implementation");
    }

    /**
     * Clears runtime state for the supplied player identifier.
     *
     * @param playerId the unique identifier of the player whose state should be purged
     */
    default void clearPlayerState(@NotNull UUID playerId) {
        // Optional
    }

    /**
     * Clears runtime state for the supplied player.
     *
     * @param player the player whose state should be purged
     */
    default void clearPlayerState(@NotNull Player player) {
        clearPlayerState(player.getUniqueId());
    }
}