package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.utility.ConfigurationDirectoryLoader;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the perk system configuration files, preparing the in-memory representation used throughout the
 * RDQ plugin.
 *
 * <p>The loader is responsible for wiring system and perk level sections while applying any
 * additional parsing steps required.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public final class PerkConfigurationLoader {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkConfigurationLoader.class.getName());

    private static final String DIR_ROOT = "perks";
    private static final String FILE_SYSTEM = "perk-system.yml";

    private static final List<String> INITIAL_PERK_FILES = List.of(
            "double_experience.yml",
            "fire_resistance.yml",
            "fly.yml",
            "haste.yml",
            "jump_boost.yml",
            "luck.yml",
            "night_vision.yml",
            "prevent_death.yml",
            "regeneration.yml",
            "resistance.yml",
            "speed.yml",
            "strength.yml",
            "treasure_hunter.yml",
            "vampire.yml",
            "water_breathing.yml"
    );

    private final @NotNull RDQ rdq;

    /**
     * Creates a new loader bound to the provided RDQ plugin instance.
     *
     * @param rdq the RDQ plugin that supplies the configuration directory context
     */
    PerkConfigurationLoader(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    /**
     * Asynchronously loads the entire perk system state using the supplied executor.
     *
     * @param executor the executor used to perform the configuration loading off the main thread
     * @return a future that resolves to the fully parsed {@link PerkSystemState}
     */
    public CompletableFuture<PerkSystemState> loadAllAsync(final @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(this::loadAll, executor);
    }

    /**
     * Loads all perk related configurations, including system and perk sections.
     *
     * @return the aggregated {@link PerkSystemState}
     */
    private PerkSystemState loadAll() {
        final PerkSystemSection systemSection = loadSystemSection();
        final Map<String, PerkSection> perkSections = loadPerkSections();
        return PerkSystemState.builder()
                .perkSystemSection(systemSection)
                .perkSections(perkSections)
                .build();
    }

    /**
     * Loads the {@link PerkSystemSection} from the primary configuration file.
     *
     * @return the loaded system section or a fallback instance when parsing fails
     */
    private PerkSystemSection loadSystemSection() {
        try {
            final ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), DIR_ROOT);
            final ConfigKeeper<PerkSystemSection> keeper = new ConfigKeeper<>(cfgManager, FILE_SYSTEM, PerkSystemSection.class);
            LOGGER.info("Loaded perk system configuration");
            return keeper.rootSection;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to load perk system configuration, using fallback", exception);
            return new PerkSystemSection(new EvaluationEnvironmentBuilder());
        }
    }

    /**
     * Loads all perk configuration sections, including the shipped defaults and any additional files supplied by server administrators.
     *
     * @return a mapping of perk identifiers to their parsed {@link PerkSection}
     */
    private Map<String, PerkSection> loadPerkSections() {
        final ConfigurationDirectoryLoader<PerkSection> loader = new ConfigurationDirectoryLoader<>(
                rdq,
                DIR_ROOT,
                PerkSection.class,
                this::normalize,
                (fileName, e) -> LOGGER.log(Level.WARNING, "Failed to load perk configuration: " + fileName, e)
        );

        final Map<String, PerkSection> sections = new HashMap<>();

        for (String fileName : INITIAL_PERK_FILES) {
            try {
                final String id = normalize(fileName);
                final PerkSection section = loadPerk(fileName);
                if (section != null) {
                    sections.put(id, section);
                    LOGGER.log(Level.INFO, "Loaded perk configuration: {0}", id);
                }
            } catch (final Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to load initial perk configuration: " + fileName, exception);
            }
        }

        @SuppressWarnings("unchecked")
        final Map<String, PerkSection> additional = (Map<String, PerkSection>) loader.loadAll(INITIAL_PERK_FILES);
        additional.forEach((id, section) -> sections.put(id, section));

        LOGGER.log(Level.INFO, "Loaded {0} perk configurations", sections.size());
        return sections;
    }

    /**
     * Normalizes a configuration file name into a lowercase identifier suitable for map keys.
     *
     * @param identifier the raw file name
     * @return the normalized identifier without file extension or separators
     */
    private String normalize(final String identifier) {
        return identifier.replace(".yml", "").replace(" ", "").replace("-", "_").toLowerCase(Locale.ROOT);
    }

    /**
     * Loads a single {@link PerkSection} from the given file.
     *
     * @param file the perk configuration file name
     * @return the parsed perk section
     * @throws Exception when the configuration cannot be parsed
     */
    private PerkSection loadPerk(final String file) throws Exception {
        final ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), DIR_ROOT);
        final ConfigKeeper<PerkSection> keeper = new ConfigKeeper<>(cfgManager, file, PerkSection.class);
        return keeper.rootSection;
    }
}
