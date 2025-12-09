/*
package com.raindropcentral.rdq2.perk.config;

import com.raindropcentral.rdq2.perk.*;
import com.raindropcentral.rdq2.perk.repository.PerkRepository;
import com.raindropcentral.rdq2.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq2.perk.runtime.PerkTypeRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PerkConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(PerkConfigLoader.class.getName());

    private final File perksDirectory;
    private final File configFile;
    private final PerkRepository perkRepository;
    private final PerkRegistry perkRegistry;
    private PerkSystemConfig config;

    public PerkConfigLoader(
        @NotNull File perksDirectory,
        @NotNull File configFile,
        @NotNull PerkRepository perkRepository,
        @NotNull PerkRegistry perkRegistry
    ) {
        this.perksDirectory = perksDirectory;
        this.configFile = configFile;
        this.perkRepository = perkRepository;
        this.perkRegistry = perkRegistry;
        this.config = PerkSystemConfig.defaults();
    }

    @NotNull
    public PerkSystemConfig loadConfig() {
        if (!configFile.exists()) {
            LOGGER.info("Perk config not found, using defaults: " + configFile.getAbsolutePath());
            return config;
        }

        try {
            var yaml = YamlConfiguration.loadConfiguration(configFile);

            var notificationSection = yaml.getConfigurationSection("notifications");
            var notifications = notificationSection != null
                ? new PerkSystemConfig.NotificationConfig(
                    notificationSection.getBoolean("activationEnabled", true),
                    notificationSection.getBoolean("deactivationEnabled", true),
                    notificationSection.getBoolean("unlockEnabled", true),
                    notificationSection.getBoolean("cooldownWarningEnabled", true),
                    notificationSection.getBoolean("soundEnabled", true),
                    notificationSection.getString("activationSound", "BLOCK_BEACON_ACTIVATE"),
                    notificationSection.getString("deactivationSound", "BLOCK_BEACON_DEACTIVATE"),
                    notificationSection.getString("unlockSound", "ENTITY_PLAYER_LEVELUP")
                )
                : PerkSystemConfig.NotificationConfig.defaults();

            config = new PerkSystemConfig(
                yaml.getBoolean("enabled", true),
                yaml.getInt("maxActivePerks", 1),
                yaml.getBoolean("allowMultipleSameCategory", false),
                yaml.getInt("defaultCooldownSeconds", 300),
                yaml.getInt("defaultDurationSeconds", 60),
                yaml.getBoolean("requireUnlockBeforeActivation", true),
                notifications
            );

            LOGGER.info("Loaded perk system configuration");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load perk config, using defaults", e);
            config = PerkSystemConfig.defaults();
        }

        return config;
    }

    @NotNull
    public List<Perk> loadPerks() {
        var perks = new ArrayList<Perk>();

        if (!perksDirectory.exists() || !perksDirectory.isDirectory()) {
            LOGGER.warning("Perks directory not found: " + perksDirectory.getAbsolutePath());
            return perks;
        }

        var files = perksDirectory.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null || files.length == 0) {
            LOGGER.info("No perk configuration files found");
            return perks;
        }

        for (var file : files) {
            try {
                var perk = loadPerkFromFile(file);
                if (perk != null) {
                    perks.add(perk);
                    LOGGER.fine(() -> "Loaded perk: " + perk.id());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load perk from file: " + file.getName(), e);
            }
        }

        LOGGER.info("Loaded " + perks.size() + " perks");
        return perks;
    }

    @Nullable
    private Perk loadPerkFromFile(@NotNull File file) {
        var yaml = YamlConfiguration.loadConfiguration(file);

        var id = yaml.getString("id");
        if (id == null || id.isBlank()) {
            LOGGER.warning("Perk file missing id: " + file.getName());
            return null;
        }

        var displayNameKey = yaml.getString("displayNameKey", "perk." + id + ".name");
        var descriptionKey = yaml.getString("descriptionKey", "perk." + id + ".description");
        var category = yaml.getString("category");
        var iconMaterial = yaml.getString("iconMaterial", "NETHER_STAR");
        var enabled = yaml.getBoolean("enabled", true);
        var cooldownSeconds = yaml.getInt("cooldownSeconds", config.defaultCooldownSeconds());
        var durationSeconds = yaml.getInt("durationSeconds", config.defaultDurationSeconds());

        var type = parseType(yaml);
        var effect = parseEffect(yaml.getConfigurationSection("effect"));
        var requirements = parseRequirements(yaml.getConfigurationSection("requirements"));

        if (effect == null) {
            LOGGER.warning("Perk file missing or invalid effect: " + file.getName());
            return null;
        }

        return new Perk(
            id,
            displayNameKey,
            descriptionKey,
            type,
            category,
            cooldownSeconds,
            durationSeconds,
            enabled,
            effect,
            iconMaterial,
            requirements
        );
    }

    @NotNull
    private PerkType parseType(@NotNull ConfigurationSection yaml) {
        var typeString = yaml.getString("type", "TOGGLEABLE");
        var typeConfig = yaml.getConfigurationSection("typeConfig");
        var configMap = typeConfig != null ? toMap(typeConfig) : Map.<String, Object>of();

        var parsed = PerkTypeRegistry.parse(typeString, configMap);
        if (parsed == null) {
            throw new IllegalArgumentException("Unknown perk type: " + typeString);
        }
        // TODO: Convert runtime.PerkType to perk.PerkType when type system is unified
        // For now, return a stub to allow compilation
        return new PerkType.Toggleable();
    }

    @Nullable
    private PerkEffect parseEffect(@Nullable ConfigurationSection section) {
        if (section == null) return null;

        var type = section.getString("type", "").toUpperCase();

        return switch (type) {
            case "POTION_EFFECT", "POTION" -> new PerkEffect.PotionEffect(
                section.getString("potionType", "SPEED"),
                section.getInt("amplifier", 0)
            );
            case "ATTRIBUTE_MODIFIER", "ATTRIBUTE" -> new PerkEffect.AttributeModifier(
                section.getString("attribute", "GENERIC_MOVEMENT_SPEED"),
                section.getDouble("value", 0.1),
                section.getString("operation", "ADD")
            );
            case "FLIGHT", "FLY" -> new PerkEffect.Flight(
                section.getBoolean("allowInCombat", false)
            );
            case "EXPERIENCE_MULTIPLIER", "XP_MULTIPLIER" -> new PerkEffect.ExperienceMultiplier(
                section.getDouble("multiplier", 2.0)
            );
            case "DEATH_PREVENTION", "PREVENT_DEATH" -> new PerkEffect.DeathPrevention(
                section.getInt("healthOnSave", 4)
            );
            case "CUSTOM" -> new PerkEffect.Custom(
                section.getString("handler", ""),
                toMap(section.getConfigurationSection("config"))
            );
            default -> {
                LOGGER.warning("Unknown perk effect type: " + type);
                yield null;
            }
        };
    }

    @NotNull
    private List<PerkRequirement> parseRequirements(@Nullable ConfigurationSection section) {
        if (section == null) return List.of();

        var requirements = new ArrayList<PerkRequirement>();

        for (var key : section.getKeys(false)) {
            var reqSection = section.getConfigurationSection(key);
            if (reqSection == null) continue;

            var type = reqSection.getString("type", "").toUpperCase();
            var requirement = switch (type) {
                case "RANK", "RANK_REQUIRED" -> new PerkRequirement.RankRequired(
                    reqSection.getString("value", reqSection.getString("rankId", ""))
                );
                case "PERMISSION", "PERMISSION_REQUIRED" -> new PerkRequirement.PermissionRequired(
                    reqSection.getString("value", reqSection.getString("permission", ""))
                );
                case "CURRENCY", "CURRENCY_REQUIRED" -> new PerkRequirement.CurrencyRequired(
                    reqSection.getString("currency", "coins"),
                    BigDecimal.valueOf(reqSection.getDouble("amount", 0))
                );
                case "LEVEL", "LEVEL_REQUIRED" -> new PerkRequirement.LevelRequired(
                    reqSection.getInt("value", reqSection.getInt("level", 0))
                );
                default -> {
                    LOGGER.warning("Unknown requirement type: " + type);
                    yield null;
                }
            };

            if (requirement != null) {
                requirements.add(requirement);
            }
        }

        return requirements;
    }

    @NotNull
    private Map<String, Object> toMap(@Nullable ConfigurationSection section) {
        if (section == null) return Map.of();
        var map = new HashMap<String, Object>();
        for (var key : section.getKeys(false)) {
            map.put(key, section.get(key));
        }
        return map;
    }

    public void reload() {
        loadConfig();
        var perks = loadPerks();
        perkRepository.reload(perks);
        perkRegistry.reload(perks);
        LOGGER.info("Reloaded perk configuration");
    }

    @NotNull
    public PerkSystemConfig getConfig() {
        return config;
    }

    @NotNull
    public List<String> validate() {
        var errors = new ArrayList<String>();

        if (!perksDirectory.exists()) {
            errors.add("Perks directory does not exist: " + perksDirectory.getAbsolutePath());
        }

        var perks = perkRepository.findAll();
        for (var perk : perks) {
            if (perk.id() == null || perk.id().isBlank()) {
                errors.add("Perk has empty id");
            }
            if (perk.effect() == null) {
                errors.add("Perk " + perk.id() + " has no effect defined");
            }
            if (perk.cooldownSeconds() < 0) {
                errors.add("Perk " + perk.id() + " has negative cooldown");
            }
            if (perk.durationSeconds() < 0) {
                errors.add("Perk " + perk.id() + " has negative duration");
            }
        }

        return errors;
    }
}
*/
