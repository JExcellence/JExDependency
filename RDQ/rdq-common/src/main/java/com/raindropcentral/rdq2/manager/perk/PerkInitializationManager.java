/*
package com.raindropcentral.rdq2.manager.perk;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.perk.event.PerkEventBus;
import com.raindropcentral.rdq2.perk.runtime.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class PerkInitializationManager {

    private final RDQ rdq;
    private PerkTypeRegistry perkTypeRegistry;
    private PerkEventBus perkEventBus;
    private DefaultPerkManager defaultPerkManager;
    private boolean initialized = false;
    private final List<Listener> registeredListeners = new ArrayList<>();

    public PerkInitializationManager(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    public void initializePerkServices() {
        perkTypeRegistry = new PerkTypeRegistry();
        perkTypeRegistry.register(new ToggleablePerkType());
        perkTypeRegistry.register(new EventPerkType());
        perkEventBus = new PerkEventBus();
        initializePerkManagers();
        initialized = true;
    }

    public void registerPerkServices() {
        if (perkEventBus == null) {
            throw new IllegalStateException("PerkEventBus not initialized. Call initializePerkServices() first.");
        }
    }

    private void initializePerkManagers() {
        defaultPerkManager = new DefaultPerkManager(rdq, perkEventBus);
        defaultPerkManager.initializeServices();
        defaultPerkManager.initializeRegistry();
        defaultPerkManager.registerListeners();
    }

    public void shutdown() {
        if (perkEventBus != null) {
            perkEventBus.clearListeners();
        }
        if (defaultPerkManager != null) {
            defaultPerkManager.shutdown();
        }
        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();
    }

    private void registerService(@NotNull Object service) {
        if (service instanceof Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, rdq.getPlugin());
            registeredListeners.add(listener);
        }
    }

    public boolean isInitialized() {
        return initialized && perkTypeRegistry != null && perkEventBus != null && defaultPerkManager != null;
    }

    public @NotNull PerkTypeRegistry getPerkTypeRegistry() {
        if (perkTypeRegistry == null) {
            throw new IllegalStateException("PerkTypeRegistry not initialized. Call initializePerkServices() first.");
        }
        return perkTypeRegistry;
    }

    public @NotNull PerkRegistry getPerkRegistry() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager.getPerkRegistry();
    }

    public @NotNull PerkStateService getPerkStateService() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager.getPerkStateService();
    }

    public @NotNull CooldownService getCooldownService() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager.getCooldownService();
    }

    public @NotNull PerkEventBus getPerkEventBus() {
        if (perkEventBus == null) {
            throw new IllegalStateException("PerkEventBus not initialized. Call initializePerkServices() first.");
        }
        return perkEventBus;
    }

    public @NotNull PerkManager getPerkManager() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager;
    }
}
*/
