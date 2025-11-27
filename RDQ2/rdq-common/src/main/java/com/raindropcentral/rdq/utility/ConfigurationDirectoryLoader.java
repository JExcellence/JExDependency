package com.raindropcentral.rdq.utility;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigurationDirectoryLoader<T extends AConfigSection> {

    private static final Logger LOGGER = CentralLogger.getLogger(ConfigurationDirectoryLoader.class.getName());

    private final RDQ rdq;
    private final String directory;
    private final Class<T> sectionClass;
    private final Function<String, String> normalizer;
    private final BiConsumer<String, Exception> errorHandler;

    public ConfigurationDirectoryLoader(@NotNull RDQ rdq, @NotNull String directory, @NotNull Class<T> sectionClass,
                                       @NotNull Function<String, String> normalizer, @NotNull BiConsumer<String, Exception> errorHandler) {
        this.rdq = rdq;
        this.directory = directory;
        this.sectionClass = sectionClass;
        this.normalizer = normalizer;
        this.errorHandler = errorHandler;
    }

    public @NotNull Map<String, ? extends AConfigSection> loadAll(@NotNull List<String> initialFiles) {
        var sections = new HashMap<String, AConfigSection>();
        var folder = new File(rdq.getPlugin().getDataFolder(), directory);

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

        initialFiles.forEach(fileName -> loadFile(sections, fileName));

        var files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            Arrays.stream(files)
                  .map(File::getName)
                  .filter(name -> !initialFiles.contains(name.toLowerCase()))
                  .forEach(name -> loadFile(sections, name));
        }

        LOGGER.log(Level.INFO, "Loaded {0} configurations from {1}", new Object[]{sections.size(), directory});
        return sections;
    }

    public AConfigSection loadSingle(@NotNull String fileName) {
        try {
            var cfgManager = new ConfigManager(rdq.getPlugin(), directory);
            var keeper = new ConfigKeeper<>(cfgManager, fileName, sectionClass);
            return keeper.rootSection;
        } catch (Exception e) {
            errorHandler.accept(fileName, e);
            return null;
        }
    }

    private void loadFile(@NotNull Map<String, AConfigSection> sections, @NotNull String fileName) {
        try {
            var section = loadSingle(fileName);
            if (section != null) {
                sections.put(normalizer.apply(fileName), section);
            }
        } catch (Exception exception) {
            errorHandler.accept(fileName, exception);
        }
    }
}