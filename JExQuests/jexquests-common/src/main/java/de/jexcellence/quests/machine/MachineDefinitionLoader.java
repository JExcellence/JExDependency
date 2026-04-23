package de.jexcellence.quests.machine;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.content.ContentLoaderSupport;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code plugins/JExQuests/machines/*.yml} into the
 * {@link MachineRegistry}. Each file defines one machine type — the
 * file stem defaults as identifier when the YAML omits it.
 */
public final class MachineDefinitionLoader {

    private static final String DEFINITIONS_DIR = "machines";

    private final JavaPlugin plugin;
    private final MachineRegistry registry;
    private final JExLogger logger;

    public MachineDefinitionLoader(
            @NotNull JavaPlugin plugin,
            @NotNull MachineRegistry registry,
            @NotNull JExLogger logger
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.logger = logger;
    }

    public int load() {
        final File root = new File(this.plugin.getDataFolder(), DEFINITIONS_DIR);
        if (!root.exists()) {
            this.logger.info("No machines directory at {} — skipping load", root.getPath());
            this.registry.replace(Map.of());
            return 0;
        }
        final Map<String, MachineType> loaded = new HashMap<>();
        for (final Path path : ContentLoaderSupport.yamlFiles(root)) {
            try {
                final MachineType type = parse(path);
                if (type != null) loaded.put(type.identifier(), type);
            } catch (final RuntimeException ex) {
                this.logger.error("machine definition failed at {}: {}", path, ex.getMessage());
            }
        }
        this.registry.replace(loaded);
        return loaded.size();
    }

    private MachineType parse(@NotNull Path path) {
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(path.toFile());
        final String stem = stemOf(path);
        final String identifier = cfg.getString("identifier", stem);
        if (identifier == null || identifier.isBlank()) {
            this.logger.warn("machine definition {} missing identifier — skipped", path);
            return null;
        }
        final ConfigurationSection properties = cfg.getConfigurationSection("properties");
        final Map<String, Object> propMap = properties != null ? properties.getValues(true) : Map.of();
        return new MachineType(
                identifier,
                cfg.getString("displayName", identifier),
                cfg.getString("description", ""),
                cfg.getString("category", "misc"),
                cfg.getString("icon", "BEACON"),
                cfg.getInt("width", 1),
                cfg.getInt("height", 1),
                cfg.getInt("depth", 1),
                propMap
        );
    }

    private static @NotNull String stemOf(@NotNull Path path) {
        final String name = path.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
