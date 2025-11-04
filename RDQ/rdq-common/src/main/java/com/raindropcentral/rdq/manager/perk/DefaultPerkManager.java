package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.runtime.*;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of PerkManager.
 *
 * Lifecycle:
 * 1. Constructor: lightweight, stores references only
 * 2. initializeServices(): creates core services (cooldown, state, audit)
 * 3. initializeRegistry(): creates registry with initialized services
 * 4. registerListeners(): wires event listeners after runtime is populated
 *
 * @author JExcellence
 * @version 1.0.6
 * @since 3.2.0
 */
public class DefaultPerkManager implements PerkManager {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkManager.class.getName());

    private final RDQ rdq;
    private final PerkEventBus perkEventBus;

    private CooldownService cooldownService;
    private PerkRuntimeStateService runtimeStateService;
    private PerkAuditService auditService;
    private DefaultPerkRegistry perkRegistry;
    private PerkStateService perkStateService;
    private PerkTriggerService perkTriggerService;

    public DefaultPerkManager(@NotNull RDQ rdq, @NotNull PerkEventBus perkEventBus) {
        this.rdq = rdq;
        this.perkEventBus = perkEventBus;
    }

    @Override
    public PerkRegistry getPerkRegistry() {
        return perkRegistry;
    }

    @Override
    public PerkStateService getPerkStateService() {
        return perkStateService;
    }

    @Override
    public PerkTriggerService getPerkTriggerService() {
        return perkTriggerService;
    }

    @Override
    public CooldownService getCooldownService() {
        return cooldownService;
    }

    @Override
    public @NotNull Optional<PerkRuntime> findRuntime(@NotNull String perkId) {
        return Optional.ofNullable(perkRegistry.getPerkRuntime(perkId));
    }

    @Override
    public boolean activate(@NotNull Player player, @NotNull String perkId) {
        return findRuntime(perkId)
                .filter(runtime -> runtime.canActivate(player))
                .map(runtime -> runtime.activate(player))
                .orElse(false);
    }

    @Override
    public boolean deactivate(@NotNull Player player, @NotNull String perkId) {
        return findRuntime(perkId)
                .map(runtime -> runtime.deactivate(player))
                .orElse(false);
    }

    public void initializeServices() {
        this.cooldownService = new CooldownService();
        this.runtimeStateService = new PerkRuntimeStateService();
        this.auditService = new PerkAuditService();
        LOGGER.log(Level.FINE, "Perk services initialized");
    }

    public void initializeRegistry() {
        if (cooldownService == null || runtimeStateService == null || auditService == null) {
            throw new IllegalStateException("Services must be initialized before registry");
        }
        this.perkRegistry = new DefaultPerkRegistry(
                rdq,
                rdq.getPerkTypeRegistry(),
                cooldownService,
                runtimeStateService,
                auditService,
                perkEventBus
        );
        this.perkStateService = new PlayerPerkStateService(rdq);
        this.perkTriggerService = new EventBasedPerkTrigger(rdq, perkRegistry, auditService);
        LOGGER.log(Level.FINE, "Perk registry and services initialized");
    }

    public void registerListeners() {
        if (perkTriggerService == null) {
            throw new IllegalStateException("Registry must be initialized before registering listeners");
        }
        perkTriggerService.registerListeners();
        LOGGER.log(Level.FINE, "Perk event listeners registered");
    }

    @Override
    public void initialize() {
        LOGGER.log(Level.WARNING, "initialize() is deprecated; use staged lifecycle: initializeServices() → initializeRegistry() → registerListeners()");
    }

    @Override
    public void shutdown() {
        try {
            if (perkTriggerService != null) {
                perkTriggerService.unregisterListeners();
            }
        } finally {
            if (runtimeStateService != null) {
                runtimeStateService.clearAll();
            }
            if (cooldownService != null) {
                cooldownService.clearExpired();
            }
        }
    }

    /**
     * Clears runtime state for the specified player, releasing cooldown and activation tracking.
     *
     * @param playerId the player identifier to purge
     */
    @Override
    public void clearPlayerState(@NotNull UUID playerId) {
        runtimeStateService.clearPlayer(playerId);
        cooldownService.clearAllCooldowns(playerId);
    }

    @Override
    public void clearPlayerState(@NotNull Player player) {
        clearPlayerState(player.getUniqueId());
    }
}