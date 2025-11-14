package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates loading, validation, and persistence of perk configuration data for RDQ.
 * <p>
 *     Instances orchestrate asynchronous tasks that hydrate the in-memory {@link PerkSystemState} and
 *     prepare all perk entities before the gameplay systems access them.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public final class PerkSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkSystemFactory.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull Executor executor;
    private final @NotNull PerkConfigurationLoader loader;
    private final @NotNull PerkValidationService validator;
    private final @NotNull PerkEntityService entityService;

    private volatile boolean initializing;
    private volatile @NotNull PerkSystemState state = PerkSystemState.empty();

    /**
     * Creates a new factory bound to the supplied RDQ plugin instance.
     *
     * @param rdq the RDQ plugin that provides executors and services required for perk initialization
     */
    public PerkSystemFactory(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq");
        this.executor = rdq.getExecutor();
        this.loader = new PerkConfigurationLoader(rdq);
        this.validator = new PerkValidationService();
        this.entityService = new PerkEntityService(rdq);
    }

    /**
     * Initializes the perk system asynchronously by loading configurations, validating them, and
     * creating the corresponding entities.
     *
     * @return a future that completes when the initialization pipeline finishes, successfully or exceptionally
     */
    public @NotNull CompletableFuture<Void> initializeAsync() {
        if (initializing) {
            LOGGER.warning("Perk system initialization already in progress");
            return CompletableFuture.completedFuture(null);
        }
        initializing = true;

        LOGGER.info("Perk system initialization started");

        return loader.loadAllAsync(executor)
                .thenComposeAsync(loaded -> {
                    this.state = loaded;
                    LOGGER.log(Level.INFO, "Loaded {0} perk configurations", loaded.perkSections().size());
                    return validator.validateConfigurationsAsync(loaded, executor);
                }, executor)
                .thenComposeAsync(v -> entityService.createPerksAsync(state, executor), executor)
                .thenComposeAsync(v -> validator.validateSystemAsync(state, executor), executor)
                .whenComplete((v, t) -> {
                    initializing = false;
                    if (t != null) {
                        LOGGER.log(Level.SEVERE, "Perk system initialization failed", t);
                        this.state = PerkSystemState.empty();
                    } else {
                        LOGGER.log(Level.INFO, "Perk system initialization completed successfully ({0} perks)", state.perks().size());
                    }
                });
    }

    /**
     * Indicates whether the perk system has completed its initialization workflow.
     *
     * @return {@code true} when perk data has been loaded
     */
    public boolean isInitialized() {
        return !state.perks().isEmpty();
    }

    /**
     * Provides a snapshot of the registered perks.
     *
     * @return an immutable map containing the known perks keyed by their identifiers
     */
    public @NotNull Map<String, RPerk> getPerks() {
        return Map.copyOf(state.perks());
    }

    /**
     * Retrieves the perk system configuration section.
     *
     * @return the perk system section if one has been configured, otherwise {@code null}
     */
    public PerkSystemSection getPerkSystemSection() {
        return state.perkSystemSection();
    }
}