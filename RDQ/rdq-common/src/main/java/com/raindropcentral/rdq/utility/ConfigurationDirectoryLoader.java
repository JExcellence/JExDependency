package com.raindropcentral.rdq.utility;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for loading configuration sections from a directory of YAML files.
 *
 * <p>This loader standardizes the pattern used across RDQ for loading multiple configuration files
 * from a directory, handling exceptions and logging consistently.</p>
 *
 * @param <T> the type of configuration section to load
 * @author JExcellence
 * @since 1.0.0
 */
public final class ConfigurationDirectoryLoader<T extends AConfigSection> {

    private static final Logger LOGGER = CentralLogger.getLogger(ConfigurationDirectoryLoader.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull String directory;
    private final @NotNull Class<T> sectionClass;
    private final @NotNull Function<String, String> normalizer;
    private final @NotNull BiConsumer<String, Exception> errorHandler;

    /**
     * Creates a new directory loader.
     *
     * @param rdq the RDQ plugin instance
     * @param directory the subdirectory relative to plugin data folder
     * @param sectionClass the class of the configuration section
     * @param normalizer function to normalize file names to identifiers
     * @param errorHandler consumer for handling load errors (identifier, exception)
     */
    public ConfigurationDirectoryLoader(
            @NotNull RDQ rdq,
            @NotNull String directory,
            @NotNull Class<T> sectionClass,
            @NotNull Function<String, String> normalizer,
            @NotNull BiConsumer<String, Exception> errorHandler
    ) {
        this.rdq = rdq;
        this.directory = directory;
        this.sectionClass = sectionClass;
        this.normalizer = normalizer;
        this.errorHandler = errorHandler;
    }

    /**
     * Loads all YAML files from the directory into a map.
     *
     * @param initialFiles list of initial file names to load first
     * @return a map of normalized identifiers to loaded sections
     */
    public @NotNull Map<String, ? extends AConfigSection> loadAll(@NotNull List<String> initialFiles) {
        final Map<String, AConfigSection> sections = new HashMap<>();
        final File folder = new File(rdq.getPlugin().getDataFolder(), directory);

        if (!folder.exists()) {
            if (folder.mkdirs()) {
                LOGGER.log(Level.INFO, "Created configuration directory: {0}", folder.getAbsolutePath());
            } else {
                LOGGER.log(Level.WARNING, "Failed to create configuration directory: {0}", folder.getAbsolutePath());
            }
            return sections;
        }

        if (!folder.isDirectory()) {
            LOGGER.log(Level.WARNING, "Configuration path is not a directory: {0}", folder.getAbsolutePath());
            return sections;
        }

        for (String fileName : initialFiles) {
            loadFile(sections, fileName);
        }

        final File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                final String name = file.getName();
                if (initialFiles.contains(name.toLowerCase())) continue;
                loadFile(sections, name);
            }
        }

        LOGGER.log(Level.INFO, "Loaded {0} configurations from {1}", new Object[]{sections.size(), directory});
        return sections;
    }

    /**
     * Loads a single configuration file.
     *
     * @param fileName the file name
     * @return the loaded section or null if loading failed
     */
    public AConfigSection loadSingle(@NotNull String fileName) {
        try {
            final ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), directory);
            final ConfigKeeper<?> keeper = new ConfigKeeper<>(cfgManager, fileName, sectionClass);
            return keeper.rootSection;
        } catch (Exception e) {
            errorHandler.accept(fileName, e);
            return null;
        }
    }

    private void loadFile(@NotNull Map<String, AConfigSection> sections, @NotNull String fileName) {
        try {
            final String id = normalizer.apply(fileName);
            final AConfigSection section = loadSingle(fileName);
            if (section != null) {
                sections.put(id, section);
                LOGGER.log(Level.INFO, "Loaded configuration: {0}", id);
            }
        } catch (Exception e) {
            errorHandler.accept(fileName, e);
        }
    }
}