package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.perk.runtime.*;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of PerkManager.
 *
 * @author qodo
 * @version 1.0.1
 * @since TBD
 */
public class DefaultPerkManager implements PerkManager {

    private final RDQ rdq;
    private final DefaultPerkRegistry perkRegistry;
    private final PerkStateService perkStateService;
    private final PerkTriggerService perkTriggerService;

    public DefaultPerkManager(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.perkRegistry = new DefaultPerkRegistry(rdq, rdq.getPerkTypeRegistry());
        this.perkStateService = new DefaultPerkStateService(rdq);
        this.perkTriggerService = new DefaultPerkTriggerService(rdq, perkRegistry);
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
        // Load perk configurations and build runtimes
        perkRegistry.reloadAllPerkRuntimes();
        // Register event listeners
        perkTriggerService.registerListeners();
    }

    @Override
    public void shutdown() {
        // Unregister listeners
        perkTriggerService.unregisterListeners();
        // Additional cleanup if needed
    }
}