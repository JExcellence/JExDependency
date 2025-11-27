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

public final class PerkSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkSystemFactory.class.getName());

    private final RDQ rdq;
    private final Executor executor;
    private final PerkConfigurationLoader loader;
    private final PerkValidationService validator;
    private final PerkEntityService entityService;

    private volatile boolean initializing;
    private volatile PerkSystemState state = PerkSystemState.empty();

    public PerkSystemFactory(@NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq");
        this.executor = rdq.getExecutor();
        this.loader = new PerkConfigurationLoader(rdq);
        this.validator = new PerkValidationService();
        this.entityService = new PerkEntityService(rdq);
    }

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

    public boolean isInitialized() {
        return !state.perks().isEmpty();
    }

    public @NotNull Map<String, RPerk> getPerks() {
        return Map.copyOf(state.perks());
    }

    public PerkSystemSection getPerkSystemSection() {
        return state.perkSystemSection();
    }
}