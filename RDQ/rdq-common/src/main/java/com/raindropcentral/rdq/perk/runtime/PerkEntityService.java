package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.type.EPerkType;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates the creation and synchronization of perk entities against configuration-driven definitions.
 *
 * <p>This service is invoked during perk system initialization and ensures that all perk entities
 * are present in persistent storage. It also maintains consistency between configuration and
 * database representations before the entities become available to gameplay systems.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
final class PerkEntityService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkEntityService.class.getName());

    private final @NotNull RDQ rdq;

    /**
     * Creates a new service using the provided RDQ plugin instance.
     *
     * @param rdq the RDQ plugin dependency that exposes repositories and configuration
     */
    PerkEntityService(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Creates or updates perk entities for the provided configuration payload.
     *
     * @param state    the aggregated perk system state containing configured perks
     * @param executor the executor used for asynchronous persistence work
     * @return a future that resolves when all perk entities have been synchronized
     */
    CompletableFuture<Void> createPerksAsync(
            final @NotNull PerkSystemState state,
            final @NotNull Executor executor
    ) {
        return CompletableFuture.runAsync(() -> {
            if (state.perkSections().isEmpty()) return;

            final Map<String, RPerk> perks = new HashMap<>();

            for (Map.Entry<String, PerkSection> entry : state.perkSections().entrySet()) {
                final String perkId = entry.getKey();
                final PerkSection cfg = entry.getValue();

                try {
                    final RPerk existing = rdq.getPerkRepository().findByAttributes(Map.of("identifier", perkId));
                    if (existing != null) {
                        updatePerkFromConfiguration(existing, cfg);
                        rdq.getPerkRepository().update(existing);
                        perks.put(perkId, existing);
                        LOGGER.log(Level.INFO, "Updated perk: {0}", perkId);
                        continue;
                    }

                    final RPerk created = createPerkFromConfiguration(perkId, cfg);
                    rdq.getPerkRepository().create(created);
                    perks.put(perkId, created);
                    LOGGER.log(Level.INFO, "Created perk: {0}", perkId);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to create/update perk: " + perkId, e);
                }
            }

            state.perks().putAll(perks);
            LOGGER.log(Level.INFO, "Created/updated {0} perks", perks.size());
        }, executor);
    }

    /**
     * Creates a new perk entity from the provided configuration section.
     *
     * @param perkId the unique identifier for the perk
     * @param cfg    the configuration section describing the perk
     * @return a new perk entity ready for persistence
     */
    private RPerk createPerkFromConfiguration(final @NotNull String perkId,
                                             final @NotNull PerkSection cfg) {
        final var settings = cfg.getPerkSettings();
        final EPerkType perkType = resolvePerkType(settings.getMetadata());

        final RPerk perk = instantiatePerk(perkId, cfg, perkType);
        perk.setDisplayNameKey(settings.getDisplayNameKey());
        perk.setDescriptionKey(settings.getDescriptionKey());
        perk.setEnabled(settings.getEnabled());
        perk.setPriority(settings.getPriority());
        perk.setMaxConcurrentUsers(settings.getMaxConcurrentUsers());

        final Object requiredPermission = settings.getMetadata().get("requiredPermission");
        if (requiredPermission instanceof String permission && !permission.isBlank()) {
            perk.setRequiredPermission(permission);
        }

        return perk;
    }

    /**
     * Updates an existing perk entity with values from the provided configuration section.
     *
     * @param perk the perk entity to update
     * @param cfg  the configuration section describing the updated values
     */
    private void updatePerkFromConfiguration(final @NotNull RPerk perk,
                                            final @NotNull PerkSection cfg) {
        final var settings = cfg.getPerkSettings();

        perk.setDisplayNameKey(settings.getDisplayNameKey());
        perk.setDescriptionKey(settings.getDescriptionKey());
        perk.setEnabled(settings.getEnabled());
        perk.setPriority(settings.getPriority());
        perk.setMaxConcurrentUsers(settings.getMaxConcurrentUsers());
        perk.setPerkSection(cfg);

        final Object requiredPermission = settings.getMetadata().get("requiredPermission");
        if (requiredPermission instanceof String permission && !permission.isBlank()) {
            perk.setRequiredPermission(permission);
        } else {
            perk.setRequiredPermission(null);
        }
    }

    /**
     * Resolves the perk type from the configuration metadata.
     *
     * @param metadata the metadata map containing perk type information
     * @return the resolved perk type, or TOGGLEABLE_PASSIVE as default
     */
    private EPerkType resolvePerkType(final @NotNull Map<String, Object> metadata) {
        final String perkTypeStr = Optional.ofNullable(metadata.get("perkType"))
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .orElse("TOGGLEABLE_PASSIVE");

        try {
            return EPerkType.valueOf(perkTypeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Unknown perk type {0}, defaulting to TOGGLEABLE_PASSIVE", perkTypeStr);
            return EPerkType.TOGGLEABLE_PASSIVE;
        }
    }

    /**
     * Instantiates a perk entity by identifier. Searches for a matching class in known packages.
     *
     * @param perkId   the perk identifier matching the class name
     * @param cfg      the configuration section
     * @param perkType the resolved perk type
     * @return the instantiated perk entity
     */
    @SuppressWarnings("unchecked")
    private RPerk instantiatePerk(final @NotNull String perkId,
                                  final @NotNull PerkSection cfg,
                                  final @NotNull EPerkType perkType
    ) {
        try {
            return ((Class<? extends RPerk>) Class.forName(perkId)).getDeclaredConstructor(PerkSection.class, EPerkType.class).newInstance(cfg, perkType);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
