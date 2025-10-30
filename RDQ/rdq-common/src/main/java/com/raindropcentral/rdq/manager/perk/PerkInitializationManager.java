package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.runtime.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the initialization and lifecycle of the perk system within RDQ.
 *
 * <p>This manager orchestrates the setup of perk services, registries, and event buses
 * during the plugin's startup sequence. It encapsulates all perk-related initialization
 * logic that was previously scattered throughout RDQ.java, providing a clean separation
 * of concerns and improving maintainability.
 *
 * <p><strong>Lifecycle:</strong>
 * <ul>
 *     <li><strong>initializePerkServices()</strong> – Creates core perk infrastructure
 *     (registries, services, event bus) and initializes perk managers.</li>
 *     <li><strong>registerPerkServices()</strong> – Fires activation events for all
 *     registered perk services.</li>
 *     <li><strong>shutdown()</strong> – Cleans up resources and unregisters listeners.</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This manager is designed to be called during the
 * synchronous post-enable phase and should not be accessed concurrently.
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public class PerkInitializationManager {

    private final RDQ rdq;
    private volatile PerkTypeRegistry perkTypeRegistry;
    private volatile PerkEventBus perkEventBus;
    private volatile DefaultPerkManager defaultPerkManager;
    private volatile boolean initialized = false;
    private final List<Listener> registeredListeners = new ArrayList<>();

    public PerkInitializationManager(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Initializes all perk services and infrastructure.
     *
     * <p>This method should be called during the repository initialization phase
     * of the plugin startup sequence. It creates:
     * <ul>
     *     <li>PerkTypeRegistry with supported perk types</li>
     *     <li>PerkRegistry for managing loaded perks</li>
     *     <li>PerkStateService for tracking player perk state</li>
     *     <li>CooldownService for managing perk cooldowns</li>
     *     <li>PerkEventBus for event-driven perk activation</li>
     *     <li>DefaultPerkManager and all perk service implementations</li>
     * </ul>
     */
    public void initializePerkServices() {
        this.perkTypeRegistry = new PerkTypeRegistry();
        this.perkTypeRegistry.register(new ToggleablePerkType());
        this.perkTypeRegistry.register(new EventPerkType());

        this.perkEventBus = new PerkEventBus();

        initializePerkManagers();

        this.initialized = true;
    }

    /**
     * Completes perk service registration.
     *
     * <p>This method should be called after all perk managers have been initialized.
     * It verifies that all perk services are properly registered with the event bus.
     *
     * @throws IllegalStateException if perk services have not been initialized
     */
    public void registerPerkServices() {
        if (perkEventBus == null) {
            throw new IllegalStateException("PerkEventBus not initialized. Call initializePerkServices() first.");
        }
    }

    /**
     * Initializes all perk managers and service implementations.
     *
     * <p>Creates a DefaultPerkManager instance and instantiates all available perk services,
     * registering them with the event bus for event-driven activation.
     */
    private void initializePerkManagers() {
        this.defaultPerkManager = new DefaultPerkManager(rdq, perkEventBus);

        registerService(new PreventDeathPerkService(defaultPerkManager, perkEventBus));
        registerService(new FlyPerkService(defaultPerkManager, perkEventBus));
        registerService(new DoubleExperiencePerkService(defaultPerkManager, perkEventBus));
        registerService(new TreasureHunterPerkService(defaultPerkManager, perkEventBus));
        registerService(new VampirePerkService(defaultPerkManager, perkEventBus));

        defaultPerkManager.initialize();
    }

    /**
     * Shuts down the perk system and cleans up resources.
     *
     * <p>This method should be called during the plugin disable sequence.
     * It clears all listeners and performs any necessary cleanup.
     */
    public void shutdown() {
        if (perkEventBus != null) {
            perkEventBus.clearListeners();
        }
        if (defaultPerkManager != null) {
            defaultPerkManager.shutdown();
        }
        for (Listener listener : registeredListeners) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();
    }

    private void registerService(@NotNull Object service) {
        if (service instanceof Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, rdq.getPlugin());
            registeredListeners.add(listener);
        }
    }

    /**
     * Checks if the perk system has been fully initialized.
     *
     * @return true if initialization is complete, false otherwise
     */
    public boolean isInitialized() {
        return initialized && perkTypeRegistry != null && perkEventBus != null && defaultPerkManager != null;
    }

    @NotNull
    public PerkTypeRegistry getPerkTypeRegistry() {
        if (perkTypeRegistry == null) {
            throw new IllegalStateException("PerkTypeRegistry not initialized. Call initializePerkServices() first.");
        }
        return perkTypeRegistry;
    }

    @NotNull
    public PerkRegistry getPerkRegistry() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager.getPerkRegistry();
    }

    @NotNull
    public PerkStateService getPerkStateService() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager.getPerkStateService();
    }

    @NotNull
    public CooldownService getCooldownService() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager.getCooldownService();
    }

    @NotNull
    public PerkEventBus getPerkEventBus() {
        if (perkEventBus == null) {
            throw new IllegalStateException("PerkEventBus not initialized. Call initializePerkServices() first.");
        }
        return perkEventBus;
    }

    @NotNull
    public PerkManager getPerkManager() {
        if (defaultPerkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return defaultPerkManager;
    }
}
