/*
package com.raindropcentral.rdq2.manager.perk;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.perk.event.PerkEventBus;
import com.raindropcentral.rdq2.perk.runtime.*;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

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

    public PerkRegistry getPerkRegistry() { return perkRegistry; }
    public PerkStateService getPerkStateService() { return perkStateService; }
    public PerkTriggerService getPerkTriggerService() { return perkTriggerService; }
    public CooldownService getCooldownService() { return cooldownService; }

    @Override
    public Optional<PerkRuntime> findRuntime(@NotNull String perkId) {
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

    @Override
    public boolean isActive(@NotNull Player player, @NotNull String perkId) {
        return findRuntime(perkId)
                .map(runtime -> runtime.isActive(player))
                .orElse(false);
    }

    @Override
    public void clearPlayerState(@NotNull UUID playerId) {
        runtimeStateService.clearPlayer(playerId);
        cooldownService.clearAllCooldowns(playerId);
    }

    @Override
    public void clearPlayerState(@NotNull Player player) {
        clearPlayerState(player.getUniqueId());
    }

    public void initializeServices() {
        cooldownService = new CooldownService();
        runtimeStateService = new PerkRuntimeStateService();
        auditService = new PerkAuditService();
    }

    public void initializeRegistry() {
        if (cooldownService == null || runtimeStateService == null || auditService == null) {
            throw new IllegalStateException("Services must be initialized before registry");
        }
        perkRegistry = new DefaultPerkRegistry(rdq, rdq.getPerkTypeRegistry(), cooldownService, runtimeStateService, auditService, perkEventBus);
        perkStateService = new PlayerPerkStateService(rdq);
        perkTriggerService = new EventBasedPerkTrigger(rdq, perkRegistry, auditService);
    }

    public void registerListeners() {
        if (perkTriggerService == null) {
            throw new IllegalStateException("Registry must be initialized before registering listeners");
        }
        perkTriggerService.registerListeners();
    }

    public void shutdown() {
        try {
            if (perkTriggerService != null) perkTriggerService.unregisterListeners();
        } finally {
            if (runtimeStateService != null) runtimeStateService.clearAll();
            if (cooldownService != null) cooldownService.clearExpired();
        }
    }



    @Override
    public void initialize() {
        initializeServices();
        initializeRegistry();
        registerListeners();
    }
}*/
