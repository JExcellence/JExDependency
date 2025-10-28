package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.perk.runtime.CooldownService;
import com.raindropcentral.rdq.perk.runtime.DefaultPerkRegistry;
import com.raindropcentral.rdq.perk.runtime.DefaultPerkStateService;
import com.raindropcentral.rdq.perk.runtime.DefaultPerkTriggerService;
import com.raindropcentral.rdq.perk.runtime.PerkAuditService;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkRuntimeStateService;
import com.raindropcentral.rdq.perk.runtime.PerkStateService;
import com.raindropcentral.rdq.perk.runtime.PerkTriggerService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of PerkManager.
 *
 * @author JExcellence
 * @version 1.0.3
 * @since TBD
 */
public class DefaultPerkManager implements PerkManager {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkManager.class.getName());

    private final RDQ rdq;
    private final CooldownService cooldownService;
    private final PerkRuntimeStateService runtimeStateService;
    private final PerkAuditService auditService;
    private final DefaultPerkRegistry perkRegistry;
    private final PerkStateService perkStateService;
    private final PerkTriggerService perkTriggerService;

    public DefaultPerkManager(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.cooldownService = new CooldownService();
        this.runtimeStateService = new PerkRuntimeStateService();
        this.auditService = new PerkAuditService();
        this.perkRegistry = new DefaultPerkRegistry(rdq, rdq.getPerkTypeRegistry(), cooldownService, runtimeStateService, auditService);
        this.perkStateService = new DefaultPerkStateService(rdq);
        this.perkTriggerService = new DefaultPerkTriggerService(rdq, perkRegistry, auditService);
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
    public void initialize() {
        try {
            perkRegistry.reloadAllPerkRuntimes();
            perkTriggerService.registerListeners();
            LOGGER.log(Level.INFO, "Perk manager initialised with {0} runtimes", perkRegistry.getAllPerkRuntimes().size());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to initialise perk manager", exception);
            runtimeStateService.clearAll();
        }
    }

    @Override
    public void shutdown() {
        try {
            perkTriggerService.unregisterListeners();
        } finally {
            runtimeStateService.clearAll();
            cooldownService.clearExpired();
        }
    }

    /**
     * Clears runtime state for the specified player, releasing cooldown and activation tracking.
     *
     * @param playerId the player identifier to purge
     */
    public void clearRuntimeState(@NotNull UUID playerId) {
        runtimeStateService.clearPlayer(playerId);
        cooldownService.clearAllCooldowns(playerId);
    }
}