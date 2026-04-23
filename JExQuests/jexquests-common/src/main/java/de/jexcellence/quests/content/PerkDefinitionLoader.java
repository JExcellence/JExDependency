package de.jexcellence.quests.content;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.database.entity.Perk;
import de.jexcellence.quests.database.entity.PerkKind;
import de.jexcellence.quests.database.repository.PerkRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Loads perk definitions from {@code plugins/JExQuests/perks/*.yml}.
 * One file per perk.
 */
public final class PerkDefinitionLoader {

    private static final String DEFINITIONS_DIR = "perks";

    private final JavaPlugin plugin;
    private final PerkRepository perks;
    private final JExLogger logger;

    public PerkDefinitionLoader(
            @NotNull JavaPlugin plugin,
            @NotNull PerkRepository perks,
            @NotNull JExLogger logger
    ) {
        this.plugin = plugin;
        this.perks = perks;
        this.logger = logger;
    }

    public int load() {
        final File root = new File(this.plugin.getDataFolder(), DEFINITIONS_DIR);
        if (!root.exists()) {
            this.logger.info("No perks directory at {} — skipping load", root.getPath());
            return 0;
        }
        int applied = 0;
        for (final Path path : ContentLoaderSupport.yamlFiles(root)) {
            try {
                if (loadOne(path)) applied++;
            } catch (final RuntimeException ex) {
                this.logger.error("perk definition failed at {}: {}", path, ex.getMessage());
            }
        }
        this.logger.info("Perk definitions loaded: {}", applied);
        return applied;
    }

    private boolean loadOne(@NotNull Path path) {
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(path.toFile());

        final String identifier = cfg.getString("identifier");
        if (identifier == null || identifier.isBlank()) {
            this.logger.warn("perk {} missing identifier — skipped", path);
            return false;
        }

        final String category = cfg.getString("category", "misc");
        final String displayName = cfg.getString("displayName", identifier);
        final PerkKind kind = parseKind(cfg.getString("kind"));

        final Optional<Perk> existing = this.perks.findByIdentifier(identifier);
        final Perk perk = existing.orElseGet(() -> new Perk(identifier, category, displayName, kind));
        perk.setCategory(category);
        perk.setDisplayName(displayName);
        perk.setDescription(cfg.getString("description"));
        perk.setKind(kind);
        perk.setCooldownSeconds(cfg.getLong("cooldownSeconds", 0L));
        perk.setEnabled(cfg.getBoolean("enabled", true));
        perk.setIconData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("icon")));
        perk.setRequirementData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("requirement")));
        perk.setRewardData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("reward")));
        perk.setBehaviourData(ContentLoaderSupport.sectionToJson(cfg.getConfigurationSection("behaviour")));

        if (existing.isPresent()) this.perks.update(perk);
        else this.perks.create(perk);
        return true;
    }

    private static @NotNull PerkKind parseKind(String raw) {
        if (raw == null) return PerkKind.PASSIVE;
        try {
            return PerkKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return PerkKind.PASSIVE;
        }
    }
}
