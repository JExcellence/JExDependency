/*
package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.utility.ConfigurationDirectoryLoader;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Consolidated loader for perk system configuration files.
 * 
 * Combines functionality from:
 * - PerkConfigurationLoader (YAML section loading)
 * - PerkConfigLoader (file/resource loading)
 * - PerkConfigValidator (configuration validation)
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 *//*

public final class PerkConfigurationLoader {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkConfigurationLoader.class.getName());
    private static final String DIR_ROOT = "perks";

    private final @NotNull RDQ rdq;

    public PerkConfigurationLoader(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    public CompletableFuture<PerkSystemState> loadAllAsync(final @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(this::loadAll, executor);
    }

    private PerkSystemState loadAll() {
        final Map<String, PerkSection> perkSections = loadPerkSections();
        return PerkSystemState.builder()
                .perkSections(perkSections)
                .build();
    }

    private Map<String, PerkSection> loadPerkSections() {
        final ConfigurationDirectoryLoader<PerkSection> loader = new ConfigurationDirectoryLoader<>(
                rdq,
                DIR_ROOT,
                PerkSection.class,
                this::normalize,
                (fileName, e) -> LOGGER.log(Level.WARNING, "Failed to load perk configuration: " + fileName, e)
        );

        final Map<String, PerkSection> sections = new HashMap<>();
        final Map<String, PerkSection> loadedSections = (Map<String, PerkSection>) loader.loadAll(Collections.emptyList());

        loadedSections.forEach((id, section) -> {
            try {
                section.getPerkSettings().setPerkId(id);
                sections.put(section.getPerkSettings().getMetadata().get("class").toString(), section);
                section.afterParsing(new ArrayList<>());
                LOGGER.log(Level.INFO, "Loaded perk configuration: {0}", id);
            } catch (final Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to parse perk " + id, exception);
            }
        });

        LOGGER.log(Level.INFO, "Loaded {0} perk configurations", sections.size());
        return sections;
    }

    private String normalize(final String identifier) {
        return identifier.replace(".yml", "").replace(" ", "").replace("-", "_").toLowerCase(Locale.ROOT);
    }
}
*/
