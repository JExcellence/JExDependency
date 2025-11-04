package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPerkUnlockRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import com.raindropcentral.rdq.utility.requirement.RequirementFactory;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

final class PerkEntityService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkEntityService.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull RequirementFactory requirementFactory;

    PerkEntityService(final @NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementFactory = new RequirementFactory(rdq);
    }

    CompletableFuture<Void> createPerksAsync(
            final @NotNull PerkSystemState state,
            final @NotNull Executor executor
    ) {
        return CompletableFuture.runAsync(() -> {
            if (state.perkSections().isEmpty()) return;

            final Map<String, RPerk> persisted = new HashMap<>();

            for (Map.Entry<String, PerkSection> e : state.perkSections().entrySet()) {
                final String id = e.getKey();
                final PerkSection cfg = e.getValue();

                try {
                    final RPerk existing = rdq.getPerkRepository().findByAttributes(Map.of("identifier", id));
                    if (existing != null) {
                        updatePerkFromConfiguration(existing, cfg);
                        rdq.getPerkRepository().update(existing);
                        updatePerkRequirements(existing, cfg);
                        persisted.put(id, existing);
                        LOGGER.log(Level.INFO, "Updated perk: ", id);
                        continue;
                    }

                    final RPerk created = createPerkFromConfiguration(id, cfg);
                    rdq.getPerkRepository().create(created);
                    updatePerkRequirements(created, cfg);
                    persisted.put(id, created);
                    LOGGER.log(Level.INFO, "Created perk: ", id);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to create/update perk: " + id, ex);
                }
            }

            state.perks().putAll(persisted);
            LOGGER.log(Level.INFO, "Created/updated {0} perks", persisted.size());
        }, executor);
    }

    private RPerk createPerkFromConfiguration(
            final @NotNull String identifier,
            final @NotNull PerkSection cfg
    ) {
        final EPerkIdentifier enumId = EPerkIdentifier.fromIdentifier(identifier);
        if (enumId == null) {
            throw new IllegalStateException("Unknown EPerkIdentifier for id " + identifier);
        }
        final EPerkType type = resolvePerkType(cfg);

        final RPerk perk = instantiate(enumId.getClazz(), identifier, cfg, type);
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
            var ctor = clazz.getDeclaredConstructor(String.class, PerkSection.class, RDQ.class);
            ctor.setAccessible(true);
            return ctor.newInstance(identifier, section, this.rdq);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate RPerk subclass " + clazz.getName(), e);
        }
    }
}