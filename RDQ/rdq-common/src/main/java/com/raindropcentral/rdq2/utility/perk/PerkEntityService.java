/*
package com.raindropcentral.rdq2.utility.perk;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.config.perk.PerkSection;
import com.raindropcentral.rdq2.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq2.database.entity.perk.RPerk;
import com.raindropcentral.rdq2.database.entity.perk.RPerkUnlockRequirement;
import com.raindropcentral.rdq2.database.entity.perk.YamlLoadedPerk;
import com.raindropcentral.rdq2.database.entity.rank.RRequirement;
import com.raindropcentral.rdq2.type.EPerkIdentifier;
import com.raindropcentral.rdq2.type.EPerkType;
import com.raindropcentral.rdq2.utility.requirement.RequirementFactory;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Coordinates the creation and synchronization of perk entities and their relationships
 * against configuration-driven definitions.
 *
 * <p>This service is invoked during perk system initialization and ensures that perks
 * are present in persistent storage. It also maintains requirements and resolves any
 * prerequisite dependencies before the entities become available to gameplay systems.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 *//*

final class PerkEntityService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkEntityService.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull RequirementFactory requirementFactory;

    */
/**
     * Creates a new service using the provided RDQ plugin instance.
     *
     * @param rdq the RDQ plugin dependency that exposes repositories and configuration
     *//*

    PerkEntityService(final @NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementFactory = new RequirementFactory(rdq);
    }

    */
/**
     * Synchronizes perk definitions from configuration into the backing repository.
     *
     * @param state    the aggregated perk system state containing configured perks
     * @param executor the executor used for asynchronous persistence work
     * @return a future that resolves after perks have been created or updated
     *//*

    CompletableFuture<Void> createPerksAsync(
            final @NotNull PerkSystemState state,
            final @NotNull Executor executor
    ) {
        return CompletableFuture.runAsync(() -> {
            if (state.perkSections().isEmpty()) {
                LOGGER.log(Level.WARNING, "No perk sections loaded from configuration - cannot create perks");
                return;
            }

            LOGGER.log(Level.INFO, "Processing {0} perk configurations", state.perkSections().size());
            final Map<String, RPerk> persisted = new HashMap<>();

            for (Map.Entry<String, PerkSection> e : state.perkSections().entrySet()) {
                final String id = e.getKey();
                final PerkSection cfg = e.getValue();

                try {
                    final RPerk existing = rdq.getPerkRepository().findByAttributes(Map.of("identifier", id));
                    if (existing != null) {
                        updatePerkFromConfiguration(existing, cfg);
                        rdq.getPerkRepository().update(existing);
                        // Refresh entity to get updated version
                        final RPerk refreshed = rdq.getPerkRepository().findByAttributes(Map.of("identifier", id));
                        if (refreshed != null) {
                            updatePerkRequirements(refreshed, cfg);
                            persisted.put(id, refreshed);
                        } else {
                            persisted.put(id, existing);
                        }
                        LOGGER.log(Level.INFO, "Updated perk: ", id);
                        continue;
                    }

                    final RPerk created = createPerkFromConfiguration(id, cfg);
                    rdq.getPerkRepository().create(created);
                    // Refresh entity to get updated version and ID
                    final RPerk fresh = rdq.getPerkRepository().findByAttributes(Map.of("identifier", id));
                    if (fresh != null) {
                        updatePerkRequirements(fresh, cfg);
                        persisted.put(id, fresh);
                    } else {
                        persisted.put(id, created);
                    }
                    LOGGER.log(Level.INFO, "Created perk: ", id);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to create/update perk: " + id, ex);
                }
            }

            state.perks().putAll(persisted);
            LOGGER.log(Level.INFO, "Created/updated {0} perks", persisted.size());
        }, executor);
    }

    */
/**
     * Creates a new perk entity from configuration.
     *
     * @param identifier the perk identifier
     * @param cfg        the perk configuration section
     * @return the created perk entity
     *//*

    private RPerk createPerkFromConfiguration(
            final @NotNull String identifier,
            final @NotNull PerkSection cfg
    ) {
        final EPerkIdentifier enumId = EPerkIdentifier.fromIdentifier(identifier);
        final EPerkType type = resolvePerkType(cfg);

        final RPerk perk;
        if (enumId != null) {
            // Use specific perk class if defined in enum
            perk = instantiate(enumId.getClazz(), identifier, cfg, type);
        } else {
            // Fall back to YamlLoadedPerk for perks without specific implementations
            LOGGER.log(Level.INFO, "Using YamlLoadedPerk for perk: {0}", identifier);
            perk = instantiateYamlPerk(identifier, cfg, type);
        }
        
        applyCommonSettings(perk, cfg);
        return perk;
    }

    private void updatePerkFromConfiguration(
            final @NotNull RPerk perk,
            final @NotNull PerkSection cfg
    ) {
        perk.setPerkSection(cfg);
        applyCommonSettings(perk, cfg);
    }

    private void applyCommonSettings(
            final @NotNull RPerk perk, final @NotNull PerkSection cfg
    ) {
        perk.setDisplayNameKey(cfg.getPerkSettings().getDisplayNameKey());
        perk.setDescriptionKey(cfg.getPerkSettings().getDescriptionKey());
        perk.setEnabled(cfg.getPerkSettings().getEnabled());
        perk.setPriority(cfg.getPerkSettings().getPriority());
        perk.setMaxConcurrentUsers(cfg.getPerkSettings().getMaxConcurrentUsers());
        perk.setRequiredPermission(cfg.getPerkSettings().getMetadata().getOrDefault("requiredPermission", "raindropcentral.perks." + perk.getIdentifier()).toString());
    }

    private void updatePerkRequirements(
            final @NotNull RPerk perk,
            final @NotNull PerkSection cfg
    ) {
        try {
            final Map<String, ? extends BaseRequirementSection> reqCfg = cfg.getRequirements();
            if (reqCfg == null || reqCfg.isEmpty()) {
                perk.replaceUnlockRequirements(Collections.emptyList());
                rdq.getPerkRepository().update(perk);
                return;
            }

            final List<RPerkUnlockRequirement> parsed = requirementFactory.parse(
                    reqCfg,
                    base -> "perk '" + perk.getIdentifier() + "'",
                    (req, icon) -> new RPerkUnlockRequirement(perk, req, icon)
            );

            if (parsed.isEmpty()) {
                perk.replaceUnlockRequirements(Collections.emptyList());
                rdq.getPerkRepository().update(perk);
                return;
            }

            final List<RPerkUnlockRequirement> processed = new ArrayList<>(parsed.size());
            for (RPerkUnlockRequirement ur : parsed) {
                RRequirement req = ur.getRequirement();
                if (req.getId() == null) {
                    req = rdq.getRequirementRepository().create(req);
                }
                final RPerkUnlockRequirement persisted = new RPerkUnlockRequirement(perk, req, ur.getIcon());
                persisted.setDisplayOrder(ur.getDisplayOrder());
                processed.add(persisted);
            }

            perk.replaceUnlockRequirements(processed);

            try {
                rdq.getPerkRepository().update(perk);
            } catch (Exception fallback) {
                final RPerk fresh = rdq.getPerkRepository().findByAttributes(Map.of("identifier", perk.getIdentifier()));
                if (fresh == null) {
                    throw fallback;
                }
                fresh.replaceUnlockRequirements(processed);
                rdq.getPerkRepository().update(fresh);
            }

            LOGGER.log(Level.INFO, "Updated {0} unlock requirements for perk: {1}",
                    new Object[]{processed.size(), perk.getIdentifier()});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update requirements for perk: " + perk.getIdentifier(), e);
            throw new RuntimeException("Failed to update perk requirements", e);
        }
    }

    private EPerkType resolvePerkType(final @NotNull PerkSection cfg) {
        final Object typeRaw = cfg.getPerkSettings().getMetadata().get("perkType");
        if (typeRaw instanceof String s && !s.isBlank()) {
            try {
                return EPerkType.valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                LOGGER.log(Level.WARNING, "Unknown perk type {0}, defaulting to TOGGLEABLE_PASSIVE", s);
            }
        }
        return EPerkType.TOGGLEABLE_PASSIVE;
    }

    */
/**
     * Instantiates a specific perk class using reflection.
     *
     * @param clazz      the perk class to instantiate
     * @param identifier the perk identifier
     * @param section    the perk configuration section
     * @param type       the perk type
     * @return the instantiated perk
     *//*

    private RPerk instantiate(
            final Class<? extends RPerk> clazz,
            final String identifier,
            final PerkSection section,
            final EPerkType type
    ) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Perk identifier is null/blank for class " + type.name());
        }

        if (section == null) {
            throw new IllegalArgumentException("PerkSection is null for '" + identifier + "' (" + type.name() + ")");
        }

        try {
            // Try (String, PerkSection, RDQ) constructor first (for event-triggered perks)
            try {
                var ctor = clazz.getDeclaredConstructor(String.class, PerkSection.class, RDQ.class);
                ctor.setAccessible(true);
                return ctor.newInstance(identifier, section, this.rdq);
            } catch (NoSuchMethodException e) {
                // Try (PerkSection) constructor for potion perks
                var ctor = clazz.getDeclaredConstructor(PerkSection.class);
                ctor.setAccessible(true);
                return ctor.newInstance(section);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate RPerk subclass " + clazz.getName(), e);
        }
    }

    */
/**
     * Instantiates a YamlLoadedPerk for perks without specific Java implementations.
     *
     * @param identifier the perk identifier
     * @param section    the perk configuration section
     * @param type       the perk type
     * @return the instantiated YamlLoadedPerk
     *//*

    private RPerk instantiateYamlPerk(
            final String identifier,
            final PerkSection section,
            final EPerkType type
    ) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Perk identifier is null/blank");
        }

        if (section == null) {
            throw new IllegalArgumentException("PerkSection is null for '" + identifier + "'");
        }

        return new YamlLoadedPerk(identifier, section, type);
    }
}*/
