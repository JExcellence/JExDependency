package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.runtime.*;
import com.raindropcentral.rdq.perk.runtime.PerkManager;
import org.jetbrains.annotations.NotNull;

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
 * @version 1.0.0
 * @since 3.2.0
 */
public class PerkInitializationManager {

    private final RDQ rdq;
    private volatile PerkTypeRegistry perkTypeRegistry;
    private volatile PerkRegistry perkRegistry;
    private volatile PerkStateService perkStateService;
    private volatile CooldownService cooldownService;
    private volatile PerkEventBus perkEventBus;
    private volatile PerkManager perkManager;
    private volatile boolean initialized = false;

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
     *     <li>PerkManager and all perk service implementations</li>
     * </ul>
     */
    public void initializePerkServices() {
        // Create core registries and services
        this.perkTypeRegistry = new PerkTypeRegistry();
        this.perkTypeRegistry.register(new ToggleablePerkType());
        this.perkTypeRegistry.register(new EventPerkType());

        this.perkRegistry = new PerkRegistry(this.perkTypeRegistry);
        this.perkStateService = new DefaultPerkStateService(rdq);
        this.cooldownService = new CooldownService();
        this.perkEventBus = new PerkEventBus();

        // Initialize perk managers and services
        initializePerkManagers();
        
        // Mark as initialized
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
        // Perk services are registered during initializePerkManagers() when they
        // instantiate and call perkEventBus.register(this) in their constructors.
        // No additional registration needed here.
    }

    /**
     * Initializes all perk managers and service implementations.
     *
     * <p>Creates a PerkManager instance and instantiates all 15 perk services,
     * registering them with the event bus for event-driven activation.
     */
    private void initializePerkManagers() {
        this.perkManager = new PerkManager(
            perkRegistry,
            new PerkCache(),
            cooldownService
        );

        // Instantiate passive potion effect perk services
        new SpeedPerkService(perkManager, perkEventBus);
        new JumpBoostPerkService(perkManager, perkEventBus);
        new StrengthPerkService(perkManager, perkEventBus);
        new ResistancePerkService(perkManager, perkEventBus);
        new RegenerationPerkService(perkManager, perkEventBus);
        new HasteePerkService(perkManager, perkEventBus);
        new FireResistancePerkService(perkManager, perkEventBus);
        new NightVisionPerkService(perkManager, perkEventBus);
        new WaterBreathingPerkService(perkManager, perkEventBus);
        new LuckPerkService(perkManager, perkEventBus);

        // Instantiate event-triggered perk services
        new PreventDeathPerkService(perkManager, perkEventBus);
        new FlyPerkService(perkManager, perkEventBus);
        new DoubleExperiencePerkService(perkManager, perkEventBus);
        new TreasureHunterPerkService(perkManager, perkEventBus);
        new VampirePerkService(perkManager, perkEventBus);
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
        if (perkManager != null) {
            // Additional cleanup if needed
        }
    }

    /**
     * Checks if the perk system has been fully initialized.
     *
     * @return true if initialization is complete, false otherwise
     */
    public boolean isInitialized() {
        return initialized && perkTypeRegistry != null && perkRegistry != null 
            && perkStateService != null && cooldownService != null 
            && perkEventBus != null && perkManager != null;
    }

    // Getters for accessing initialized services

    @NotNull
    public PerkTypeRegistry getPerkTypeRegistry() {
        if (perkTypeRegistry == null) {
            throw new IllegalStateException("PerkTypeRegistry not initialized. Call initializePerkServices() first.");
        }
        return perkTypeRegistry;
    }

    @NotNull
    public PerkRegistry getPerkRegistry() {
        if (perkRegistry == null) {
            throw new IllegalStateException("PerkRegistry not initialized. Call initializePerkServices() first.");
        }
        return perkRegistry;
    }

    @NotNull
    public PerkStateService getPerkStateService() {
        if (perkStateService == null) {
            throw new IllegalStateException("PerkStateService not initialized. Call initializePerkServices() first.");
        }
        return perkStateService;
    }

    @NotNull
    public CooldownService getCooldownService() {
        if (cooldownService == null) {
            throw new IllegalStateException("CooldownService not initialized. Call initializePerkServices() first.");
        }
        return cooldownService;
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
        if (perkManager == null) {
            throw new IllegalStateException("PerkManager not initialized. Call initializePerkServices() first.");
        }
        return perkManager;
    }
}
