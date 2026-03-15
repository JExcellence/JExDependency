package com.raindropcentral.rdq.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkEffectSection;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSectionAdapter;
import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkCategory;
import com.raindropcentral.rdq.database.entity.perk.PerkRequirement;
import com.raindropcentral.rdq.database.entity.perk.PerkType;
import com.raindropcentral.rdq.database.entity.perk.PerkUnlockReward;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.config.RewardFactory;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory responsible for loading, constructing, and validating the perk system from configuration files.
 *
 * <p>This factory follows the same pattern as RankSystemFactory with proper entity lifecycle management
 * to avoid OptimisticLockException. Key features:
 * - Single-pass entity creation and updates
 * - Fresh entity fetches before each modification
 * - Proper transaction boundaries
 * - Graceful error handling
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PerkSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private static final String FILE_PATH = "perks";
    private static final String SYSTEM_FILE_NAME = "perk-system.yml";

    private final RDQ rdq;
    private final RequirementFactory requirementFactory;

    private volatile boolean isInitializing = false;
    
    @Getter
    private PerkSystemSection perkSystemSection;

    private final Map<String, PerkSection> perkSections = new HashMap<>();
    private final Map<String, Perk> perks = new HashMap<>();

    /**
     * Executes PerkSystemFactory.
     */
    public PerkSystemFactory(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementFactory = RequirementFactory.getInstance();

        this.requirementFactory.registerSectionAdapter(
            BaseRequirementSection.class,
            BaseRequirementSectionAdapter.getInstance()
        );
    }

    /**
     * Initializes the perk system.
     */
    public void initialize() {
        if (isInitializing) {
            return;
        }

        isInitializing = true;
        try {
            LOGGER.info("╔════════════════════════════════════════════════════════════╗");
            LOGGER.info("║          PERK SYSTEM INITIALIZATION STARTED                ║");
            LOGGER.info("╚════════════════════════════════════════════════════════════╝");

            loadConfigurations();

            validateConfigurations();

            createPerks();

            LOGGER.info("╔════════════════════════════════════════════════════════════╗");
            LOGGER.info("║       PERK SYSTEM INITIALIZATION COMPLETED                 ║");
            LOGGER.info("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize perk system", e);
            clearData();
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Loads all perk configurations from YAML files.
     */
    private void loadConfigurations() {
        LOGGER.info("→ Loading perk configurations...");

        // Load system configuration
        perkSystemSection = loadSystemConfig();

        // Create perks directory if it doesn't exist
        File perksDir = new File(rdq.getPlugin().getDataFolder(), FILE_PATH);
        if (perksDir.mkdirs()) {
            LOGGER.fine("Created perks directory");
        }

        // Load all perk configuration files
        loadPerkConfigs();

        LOGGER.info("  ✓ Loaded " + perkSections.size() + " perk configurations");
    }

    /**
     * Loads the global perk system configuration.
     */
    private PerkSystemSection loadSystemConfig() {
        try {
            ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), FILE_PATH);
            ConfigKeeper<PerkSystemSection> cfgKeeper = new ConfigKeeper<>(cfgManager, SYSTEM_FILE_NAME, PerkSystemSection.class);
            return cfgKeeper.rootSection;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading perk system config, using defaults", e);
            return new PerkSystemSection(new EvaluationEnvironmentBuilder());
        }
    }

    /**
     * Loads all perk configuration files from the perks directory.
     * Uses ConfigManager/ConfigKeeper which automatically copies files from JAR resources if they don't exist.
     */
    private void loadPerkConfigs() {
        File perksDir = new File(rdq.getPlugin().getDataFolder(), FILE_PATH);
        if (!perksDir.exists() || !perksDir.isDirectory()) {
            LOGGER.info("Perks directory does not exist, creating: " + perksDir.getAbsolutePath());
            if (perksDir.mkdirs()) {
                LOGGER.info("Created perks directory");
            }
        }

        LOGGER.info("→ Scanning for perk configuration files in: " + perksDir.getAbsolutePath());
        
        // List of all default perk configuration files
        // ConfigKeeper will automatically copy these from JAR resources if they don't exist
        String[] defaultPerkFiles = {
            "combat_heal.yml",
            "critical_strike.yml",
            "double_experience.yml",
            "fire_resistance.yml",
            "fish_rate.yml",
            "fish_xp.yml",
            "fly.yml",
            "glow.yml",
            "haste.yml",
            "jump_boost.yml",
            "keep_experience.yml",
            "keep_inventory.yml",
            "night_vision.yml",
            "no_fall_damage.yml",
            "resistance.yml",
            "saturation.yml",
            "speed.yml",
            "strength.yml",
            "potion_save.yml",
            "potion_extend.yml",
            "potion_enhance.yml"
        };
        
        int loadedCount = 0;
        int skippedCount = 0;
        
        // Load all default perk files - ConfigKeeper will handle resource copying
        for (String fileName : defaultPerkFiles) {
            LOGGER.info("  → Processing: " + fileName);
            boolean loaded = loadSinglePerkConfig(fileName);
            if (loaded) {
                loadedCount++;
            } else {
                skippedCount++;
            }
        }
        
        // Also scan for any additional custom perk files
        File[] files = perksDir.listFiles((dir, name) -> 
            name.endsWith(".yml") && !name.equals(SYSTEM_FILE_NAME)
        );
        
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                // Skip if already loaded from default list
                boolean isDefault = false;
                for (String defaultFile : defaultPerkFiles) {
                    if (defaultFile.equalsIgnoreCase(fileName)) {
                        isDefault = true;
                        break;
                    }
                }
                
                if (!isDefault) {
                    LOGGER.info("  → Processing custom perk: " + fileName);
                    boolean loaded = loadSinglePerkConfig(fileName);
                    if (loaded) {
                        loadedCount++;
                    } else {
                        skippedCount++;
                    }
                }
            }
        }
        
        LOGGER.info("  ═══════════════════════════════════════════════════════");
        LOGGER.info("  📊 Loading Summary:");
        LOGGER.info("     Total files processed: " + (loadedCount + skippedCount));
        LOGGER.info("     Successfully loaded: " + loadedCount);
        LOGGER.info("     Skipped/Failed: " + skippedCount);
        LOGGER.info("  ═══════════════════════════════════════════════════════");
    }

    /**
     * Loads a single perk configuration file.
     * ConfigKeeper automatically copies the file from JAR resources if it doesn't exist in the data folder.
     * 
     * @return true if the perk was successfully loaded, false if skipped or failed
     */
    private boolean loadSinglePerkConfig(String fileName) {
        LOGGER.info("    ├─ Loading: " + fileName);
        
        try {
            String perkId = toIdentifier(fileName);
            LOGGER.fine("       ├─ Derived identifier from filename: " + perkId);
            
            // Parse YAML configuration
            LOGGER.fine("       ├─ Parsing YAML configuration...");
            ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), FILE_PATH);
            ConfigKeeper<PerkSection> cfgKeeper = new ConfigKeeper<>(cfgManager, fileName, PerkSection.class);

            PerkSection section = cfgKeeper.rootSection;
            LOGGER.fine("       ├─ YAML parsed successfully");
            
            // After parsing hook - MUST be called before accessing section data
            // This validates and sets defaults for identifier, perkType, and category
            try {
                section.afterParsing(new ArrayList<>());
                LOGGER.fine("       ├─ After-parsing hook executed");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "       └─ ✗ SKIPPED: After-parsing validation failed for " + fileName, e);
                LOGGER.warning("          Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("identifier")) {
                    LOGGER.warning("          The YAML file must contain an 'identifier' field");
                }
                return false;
            }
            
            // Validate identifier (should not be null after afterParsing)
            if (section.getIdentifier() == null || section.getIdentifier().isEmpty()) {
                LOGGER.warning("       └─ ✗ SKIPPED: Perk config " + fileName + " has no identifier field after parsing");
                LOGGER.warning("          Expected identifier field in YAML, but found null or empty");
                return false;
            }
            
            LOGGER.info("       ├─ Identifier validated: " + section.getIdentifier());
            
            // Validate perk type (should have default after afterParsing)
            if (section.getPerkType() == null || section.getPerkType().isEmpty()) {
                LOGGER.warning("       └─ ✗ SKIPPED: Perk " + section.getIdentifier() + " has no perkType after parsing");
                return false;
            }
            LOGGER.fine("       ├─ Perk type: " + section.getPerkType());
            
            // Validate category (should have default after afterParsing)
            if (section.getCategory() == null || section.getCategory().isEmpty()) {
                LOGGER.warning("       └─ ✗ SKIPPED: Perk " + section.getIdentifier() + " has no category after parsing");
                return false;
            }
            LOGGER.fine("       ├─ Category: " + section.getCategory());
            
            // Validate enum conversions
            try {
                PerkType.valueOf(section.getPerkType());
                LOGGER.fine("       ├─ PerkType enum validation: OK");
            } catch (IllegalArgumentException e) {
                LOGGER.warning("       └─ ✗ SKIPPED: Invalid PerkType '" + section.getPerkType() + "' for perk " + section.getIdentifier());
                LOGGER.warning("          Valid values: " + Arrays.toString(PerkType.values()));
                return false;
            }
            
            try {
                PerkCategory.valueOf(section.getCategory());
                LOGGER.fine("       ├─ PerkCategory enum validation: OK");
            } catch (IllegalArgumentException e) {
                LOGGER.warning("       └─ ✗ SKIPPED: Invalid PerkCategory '" + section.getCategory() + "' for perk " + section.getIdentifier());
                LOGGER.warning("          Valid values: " + Arrays.toString(PerkCategory.values()));
                return false;
            }
            
            // Validate effect configuration
            if (section.getEffect() == null) {
                LOGGER.warning("       ├─ ⚠ WARNING: Perk " + section.getIdentifier() + " has no effect configuration");
            } else {
                LOGGER.fine("       ├─ Effect configuration present");
            }

            // Store in map
            perkSections.put(section.getIdentifier(), section);
            LOGGER.info("       └─ ✓ Successfully loaded: " + section.getIdentifier());
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "       └─ ✗ FAILED: Exception while loading " + fileName, e);
            LOGGER.warning("          Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                LOGGER.warning("          Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            return false;
        }
    }

    /**
     * Validates all loaded configurations.
     */
    private void validateConfigurations() {
        List<String> errors = new ArrayList<>();

        // Validate that we have at least one perk
        if (perkSections.isEmpty()) {
            LOGGER.warning("No perks configured - perk system will be inactive");
            return;
        }

        // Validate each perk configuration
        perkSections.forEach((perkId, config) -> {
            // Validate identifier
            if (perkId == null || perkId.isEmpty()) {
                errors.add("Perk has null or empty identifier");
            }

            // Validate perk type
            String perkType = config.getPerkType();
            if (perkType == null || perkType.isEmpty()) {
                errors.add("Perk " + perkId + " has no perk type specified");
            }

            // Validate category
            String category = config.getCategory();
            if (category == null || category.isEmpty()) {
                errors.add("Perk " + perkId + " has no category specified");
            }

            // Validate effect configuration
            PerkEffectSection effect = config.getEffect();
            if (effect == null) {
                errors.add("Perk " + perkId + " has no effect configuration");
            }
        });

        if (!errors.isEmpty()) {
            errors.forEach(e -> LOGGER.warning("Validation warning: " + e));
            // Don't throw exception, just log warnings
        }
    }

    /**
     * Creates perk entities in the database from loaded configurations.
     */
    private void createPerks() {
        if (perkSections.isEmpty()) {
            LOGGER.warning("⚠ No perk configurations to create");
            return;
        }

        LOGGER.info("→ Creating perk entities in database...");
        LOGGER.info("  Total configurations loaded: " + perkSections.size());

        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

        for (Map.Entry<String, PerkSection> entry : perkSections.entrySet()) {
            String perkId = entry.getKey();
            PerkSection config = entry.getValue();
            
            try {
                LOGGER.info("  ├─ Processing perk: " + perkId);
                Perk existing = findPerkByIdentifier(perkId);
                
                if (existing != null) {
                    LOGGER.info("     ├─ Found existing perk entity, updating...");
                } else {
                    LOGGER.info("     ├─ No existing entity found, creating new...");
                }
                
                Perk perk = createOrUpdatePerk(perkId, config);
                
                if (perk != null) {
                    perks.put(perkId, perk);
                    if (existing != null) {
                        updatedCount++;
                        LOGGER.info("     └─ ✓ Updated perk entity: " + perkId);
                    } else {
                        createdCount++;
                        LOGGER.info("     └─ ✓ Created perk entity: " + perkId);
                    }
                } else {
                    failedCount++;
                    LOGGER.warning("     └─ ✗ Failed to create/update perk entity: " + perkId);
                }
            } catch (Exception e) {
                failedCount++;
                LOGGER.log(Level.WARNING, "     └─ ✗ Exception while creating perk: " + perkId, e);
            }
        }

        LOGGER.info("  ═══════════════════════════════════════════════════════");
        LOGGER.info("  📊 Entity Creation Summary:");
        LOGGER.info("     New perks created: " + createdCount);
        LOGGER.info("     Existing perks updated: " + updatedCount);
        LOGGER.info("     Failed: " + failedCount);
        LOGGER.info("     Total in memory: " + perks.size());
        LOGGER.info("  ═══════════════════════════════════════════════════════");

        // Update requirements and rewards for all perks
        LOGGER.info("→ Processing requirements and rewards...");
        int reqSuccessCount = 0;
        int reqFailCount = 0;
        int rewSuccessCount = 0;
        int rewFailCount = 0;
        
        for (Map.Entry<String, PerkSection> entry : perkSections.entrySet()) {
            String perkId = entry.getKey();
            PerkSection config = entry.getValue();
            
            try {
                LOGGER.fine("  ├─ Updating requirements for: " + perkId);
                updatePerkRequirements(perkId, config);
                reqSuccessCount++;
            } catch (Exception e) {
                reqFailCount++;
                LOGGER.log(Level.WARNING, "  ├─ ✗ Failed to update requirements for perk: " + perkId, e);
            }
            
            try {
                LOGGER.fine("  ├─ Updating unlock rewards for: " + perkId);
                updatePerkUnlockRewards(perkId, config);
                rewSuccessCount++;
            } catch (Exception e) {
                rewFailCount++;
                LOGGER.log(Level.WARNING, "  └─ ✗ Failed to update rewards for perk: " + perkId, e);
            }
        }

        // Count total requirements and rewards
        int reqCount = 0;
        int rewCount = 0;
        for (Perk perk : perks.values()) {
            reqCount += perk.getRequirements().size();
            rewCount += perk.getUnlockRewards().size();
        }

        LOGGER.info("  ═══════════════════════════════════════════════════════");
        LOGGER.info("  📊 Requirements & Rewards Summary:");
        LOGGER.info("     Perks with requirements processed: " + reqSuccessCount + "/" + perkSections.size());
        LOGGER.info("     Total requirements created: " + reqCount);
        LOGGER.info("     Perks with rewards processed: " + rewSuccessCount + "/" + perkSections.size());
        LOGGER.info("     Total unlock rewards created: " + rewCount);
        if (reqFailCount > 0 || rewFailCount > 0) {
            LOGGER.warning("     Failed requirement updates: " + reqFailCount);
            LOGGER.warning("     Failed reward updates: " + rewFailCount);
        }
        LOGGER.info("  ═══════════════════════════════════════════════════════");
    }

    /**
     * Creates or updates a perk entity.
     */
    private Perk createOrUpdatePerk(String perkId, PerkSection config) {
        LOGGER.fine("       ├─ Checking database for existing perk: " + perkId);
        Perk existing = findPerkByIdentifier(perkId);

        if (existing != null) {
            LOGGER.fine("       ├─ Updating existing perk entity");
            // Update existing perk - convert String to enum types
            try {
                existing.setPerkType(PerkType.valueOf(config.getPerkType()));
                existing.setCategory(PerkCategory.valueOf(config.getCategory()));
                existing.setEnabled(config.getEnabled());
                existing.setDisplayOrder(config.getDisplayOrder());
                existing.setIcon(config.getIcon());
                existing.setConfigJson(serializeEffectConfig(config.getEffect()));
                
                rdq.getPerkRepository().update(existing);
                LOGGER.fine("       ├─ Database update successful");
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "       └─ ✗ Failed to convert enum values for perk: " + perkId, e);
                LOGGER.warning("          PerkType: " + config.getPerkType() + ", Category: " + config.getCategory());
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "       └─ ✗ Failed to update perk entity: " + perkId, e);
                return null;
            }
            
            return findPerkByIdentifier(perkId);
        }

        LOGGER.fine("       ├─ Creating new perk entity");
        // Create new perk - convert String to enum types and use correct constructor
        try {
            Perk newPerk = new Perk(
                perkId,
                PerkType.valueOf(config.getPerkType()),
                PerkCategory.valueOf(config.getCategory()),
                config.getIcon()
            );
            
            // Set additional properties
            newPerk.setEnabled(config.getEnabled());
            newPerk.setDisplayOrder(config.getDisplayOrder());
            newPerk.setConfigJson(serializeEffectConfig(config.getEffect()));

            rdq.getPerkRepository().create(newPerk);
            LOGGER.fine("       ├─ Database insert successful");
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "       └─ ✗ Failed to convert enum values for perk: " + perkId, e);
            LOGGER.warning("          PerkType: " + config.getPerkType() + ", Category: " + config.getCategory());
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "       └─ ✗ Failed to create perk entity: " + perkId, e);
            return null;
        }

        return findPerkByIdentifier(perkId);
    }

    /**
     * Serializes the effect configuration to JSON.
     */
    private String serializeEffectConfig(PerkEffectSection effect) {
        if (effect == null) {
            return "{}";
        }

        // Simple JSON serialization - in production, use a proper JSON library
        StringBuilder json = new StringBuilder("{");
        
        if (effect.getPotionEffectType() != null) {
            json.append("\"potionEffectType\":\"").append(effect.getPotionEffectType()).append("\",");
            json.append("\"amplifier\":").append(effect.getAmplifier()).append(",");
            json.append("\"durationTicks\":").append(effect.getDurationTicks()).append(",");
            json.append("\"ambient\":").append(effect.getAmbient()).append(",");
            json.append("\"particles\":").append(effect.getParticles()).append(",");
        }
        
        if (effect.getTriggerEvent() != null) {
            json.append("\"triggerEvent\":\"").append(effect.getTriggerEvent()).append("\",");
            json.append("\"cooldownMillis\":").append(effect.getCooldownMillis()).append(",");
            json.append("\"triggerChance\":").append(effect.getTriggerChance()).append(",");
        }
        
        if (effect.getSpecialType() != null) {
            json.append("\"specialType\":\"").append(effect.getSpecialType()).append("\",");
        }
        
        if (effect.getHandlerClass() != null) {
            json.append("\"handlerClass\":\"").append(effect.getHandlerClass()).append("\",");
        }
        
        // Remove trailing comma if present
        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * Updates perk requirements with proper entity lifecycle management.
     */
    private void updatePerkRequirements(String perkId, PerkSection config) {
        try {
            Perk perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            var configReqs = config.getRequirements();

            if (configReqs == null || configReqs.isEmpty()) {
                if (!perk.getRequirements().isEmpty()) {
                    perk.getRequirements().clear();
                    rdq.getPerkRepository().update(perk);
                }
                return;
            }

            int existingCount = perk.getRequirements().size();
            if (existingCount > 0) {
                // Skip if requirements already exist
                return;
            }

            List<PerkRequirement> newRequirements = parseRequirements(perk, configReqs);

            if (newRequirements.isEmpty()) {
                return;
            }

            // PerkRequirement already contains AbstractRequirement, no need to persist separately
            // Just add them to the perk
            for (PerkRequirement perkReq : newRequirements) {
                perkReq.setPerk(perk);
                perk.addRequirement(perkReq);
            }

            // Refetch perk
            perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            if (!perk.getRequirements().isEmpty()) {
                return;
            }

            // Create PerkRequirement entities
            for (PerkRequirement newReq : newRequirements) {
                newReq.setPerk(perk);
                perk.addRequirement(newReq);
            }

            try {
                rdq.getPerkRepository().update(perk);
            } catch (jakarta.persistence.OptimisticLockException ole) {
                // Retry once
                perk = findPerkByIdentifier(perkId);
                if (perk != null && perk.getRequirements().isEmpty()) {
                    for (PerkRequirement newReq : newRequirements) {
                        newReq.setPerk(perk);
                        perk.addRequirement(newReq);
                    }
                    rdq.getPerkRepository().update(perk);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update requirements for perk: " + perkId, e);
        }
    }

    /**
     * Parses BaseRequirementSection map into PerkRequirement list.
     */
    private List<PerkRequirement> parseRequirements(
            Perk perk,
            Map<String, BaseRequirementSection> configReqs
    ) {
        if (configReqs == null || configReqs.isEmpty()) {
            return List.of();
        }

        List<PerkRequirement> requirements = new ArrayList<>();
        int displayOrder = 0;

        for (var entry : configReqs.entrySet()) {
            String key = entry.getKey();
            BaseRequirementSection section = entry.getValue();

            try {
                AbstractRequirement abstractReq = requirementFactory.fromSection(section);

                PerkRequirement perkReq = new PerkRequirement(
                    null,
                    abstractReq,
                    section.getIcon()
                );
                perkReq.setDisplayOrder(section.getDisplayOrder() != null ? section.getDisplayOrder() : displayOrder);
                
                requirements.add(perkReq);
                displayOrder++;

                LOGGER.fine("Parsed requirement '" + key + "' of type: " + section.getType());

            } catch (Exception e) {
                LOGGER.warning("Failed to parse requirement '" + key + "': " + e.getMessage());
            }
        }

        return requirements;
    }

    /**
     * Updates perk unlock rewards with proper entity lifecycle management.
     */
    private void updatePerkUnlockRewards(String perkId, PerkSection config) {
        try {
            Perk perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            var configRewards = config.getUnlockRewards();

            if (configRewards == null || configRewards.isEmpty()) {
                if (!perk.getUnlockRewards().isEmpty()) {
                    perk.getUnlockRewards().clear();
                    rdq.getPerkRepository().update(perk);
                }
                return;
            }

            int existingCount = perk.getUnlockRewards().size();
            if (existingCount > 0) {
                // Skip if rewards already exist
                return;
            }

            List<PerkUnlockReward> newRewards = parseUnlockRewards(perk, configRewards);

            if (newRewards.isEmpty()) {
                return;
            }

            // PerkUnlockReward already contains AbstractReward, no need to persist separately
            // Just add them to the perk
            for (PerkUnlockReward perkReward : newRewards) {
                perkReward.setPerk(perk);
                perk.addUnlockReward(perkReward);
            }

            // Refetch perk
            perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            if (!perk.getUnlockRewards().isEmpty()) {
                return;
            }

            // Create PerkUnlockReward entities
            for (PerkUnlockReward newReward : newRewards) {
                newReward.setPerk(perk);
                perk.addUnlockReward(newReward);
            }

            try {
                rdq.getPerkRepository().update(perk);
            } catch (jakarta.persistence.OptimisticLockException ole) {
                // Retry once
                perk = findPerkByIdentifier(perkId);
                if (perk != null && perk.getUnlockRewards().isEmpty()) {
                    for (PerkUnlockReward newReward : newRewards) {
                        newReward.setPerk(perk);
                        perk.addUnlockReward(newReward);
                    }
                    rdq.getPerkRepository().update(perk);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update unlock rewards for perk: " + perkId, e);
        }
    }

    /**
     * Parses RewardSection map into PerkUnlockReward list.
     */
    private List<PerkUnlockReward> parseUnlockRewards(
            Perk perk,
            Map<String, RewardSection> configRewards
    ) {
        if (configRewards == null || configRewards.isEmpty()) {
            return List.of();
        }

        List<PerkUnlockReward> rewards = new ArrayList<>();
        int displayOrder = 0;

        for (var entry : configRewards.entrySet()) {
            String key = entry.getKey();
            RewardSection section = entry.getValue();

            try {
                LOGGER.info("Parsing unlock reward '" + key + "' of type: " + section.getType());

                @SuppressWarnings("unchecked")
                final RewardFactory<RewardSection> rewardFactory = (RewardFactory<RewardSection>) (RewardFactory<?>) RewardFactory.getInstance();
                AbstractReward abstractReward;
                
                try {
                    abstractReward = rewardFactory.fromSection(section);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to convert section to AbstractReward for '" + key + "'", e);
                    continue;
                }

                if (abstractReward == null) {
                    LOGGER.warning("Failed to parse unlock reward '" + key + "': AbstractReward is null");
                    continue;
                }
                
                LOGGER.info("Successfully created AbstractReward for '" + key + "', type: " + abstractReward.getClass().getSimpleName());

                // Get or generate icon for the reward
                IconSection rewardIcon = section.getIcon();
                if (rewardIcon == null || rewardIcon.getMaterial() == null || rewardIcon.getMaterial().equals("PAPER")) {
                    rewardIcon = generateDefaultIcon(section.getType(), section);
                    LOGGER.info("Generated default icon for '" + key + "': " + rewardIcon.getMaterial());
                }

                // Create PerkUnlockReward that contains the AbstractReward directly
                PerkUnlockReward perkReward = new PerkUnlockReward(
                    null,
                    abstractReward,
                    rewardIcon
                );
                perkReward.setDisplayOrder(section.getDisplayOrder() != null ? section.getDisplayOrder() : displayOrder);
                
                rewards.add(perkReward);
                displayOrder++;

                LOGGER.info("Successfully parsed unlock reward '" + key + "' of type: " + section.getType());

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to parse unlock reward '" + key + "'", e);
            }
        }

        return rewards;
    }

    /**
     * Generates a default icon for a reward based on its type.
     */
    private IconSection generateDefaultIcon(String rewardType, RewardSection section) {
        IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        
        String material = switch (rewardType.toUpperCase()) {
            case "ITEM" -> {
                // Try to get material from the item config
                if (section.getItem() != null && section.getItem().containsKey("material")) {
                    yield section.getItem().get("material").toString();
                }
                yield "CHEST";
            }
            case "CURRENCY" -> "GOLD_INGOT";
            case "EXPERIENCE" -> "EXPERIENCE_BOTTLE";
            case "COMMAND" -> "COMMAND_BLOCK";
            case "COMPOSITE" -> "BUNDLE";
            case "CHOICE" -> "ENDER_CHEST";
            case "PERMISSION" -> "PAPER";
            default -> "PAPER";
        };
        
        icon.setMaterial(material);
        icon.setDisplayNameKey("reward." + rewardType.toLowerCase());
        icon.setDescriptionKey("reward." + rewardType.toLowerCase() + ".description");
        
        return icon;
    }

    /**
     * Finds a perk by its identifier.
     */
    @Nullable
    private Perk findPerkByIdentifier(String identifier) {
        return rdq.getPerkRepository().findByAttributes(Map.of("identifier", identifier)).orElse(null);
    }

    /**
     * Converts a filename to an identifier.
     */
    private String toIdentifier(String fileName) {
        return fileName.replace(".yml", "")
            .replace(" ", "_")
            .replace("-", "_")
            .toLowerCase();
    }

    /**
     * Clears all cached data.
     */
    private void clearData() {
        perkSections.clear();
        perks.clear();
    }

    /**
     * Gets all loaded perks.
     */
    public Map<String, Perk> getPerks() {
        return Map.copyOf(perks);
    }

    /**
     * Checks if the perk system is initialized.
     */
    public boolean isInitialized() {
        return !perks.isEmpty();
    }

    /**
     * Reloads the perk system configuration.
 *
 * <p>This method reloads all perk configurations from YAML files and updates the database.
     * It preserves player perk ownership and states while updating perk definitions.
     */
    public void reload() {
        if (isInitializing) {
            LOGGER.warning("Cannot reload while initialization is in progress");
            return;
        }

        isInitializing = true;
        try {
            LOGGER.info("╔════════════════════════════════════════════════════════════╗");
            LOGGER.info("║          PERK SYSTEM RELOAD STARTED                        ║");
            LOGGER.info("╚════════════════════════════════════════════════════════════╝");

            // Store old configurations for rollback
            Map<String, PerkSection> oldPerkSections = new HashMap<>(perkSections);
            Map<String, Perk> oldPerks = new HashMap<>(perks);
            PerkSystemSection oldSystemSection = perkSystemSection;

            try {
                // Clear current data
                perkSections.clear();
                perks.clear();

                // Load new configurations
                loadConfigurations();

                // Validate new configurations
                validateConfigurations();

                // Update existing perks and create new ones
                updatePerksFromConfig();

                LOGGER.info("╔════════════════════════════════════════════════════════════╗");
                LOGGER.info("║       PERK SYSTEM RELOAD COMPLETED                         ║");
                LOGGER.info("╚════════════════════════════════════════════════════════════╝");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reload perk system, rolling back", e);
                
                // Rollback to old configuration
                perkSections.clear();
                perkSections.putAll(oldPerkSections);
                perks.clear();
                perks.putAll(oldPerks);
                perkSystemSection = oldSystemSection;
                
                throw e;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Perk system reload failed", e);
        } finally {
            isInitializing = false;
        }
    }

    /**
     * Updates perks from configuration during reload.
     * This method updates existing perks and creates new ones while preserving player data.
     */
    private void updatePerksFromConfig() {
        LOGGER.info("Updating perks from configuration...");

        // Get all existing perks from database
        List<Perk> existingPerks = rdq.getPerkRepository().findAll();
        Set<String> existingPerkIds = new HashSet<>();
        for (Perk perk : existingPerks) {
            existingPerkIds.add(perk.getIdentifier());
        }

        // Update or create perks from configuration
        perkSections.forEach((perkId, config) -> {
            try {
                Perk perk = createOrUpdatePerk(perkId, config);
                if (perk != null) {
                    perks.put(perkId, perk);
                    
                    // Update requirements and rewards
                    updatePerkRequirementsForReload(perkId, config);
                    updatePerkUnlockRewardsForReload(perkId, config);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to update perk during reload: " + perkId, e);
            }
        });

        // Disable perks that are no longer in configuration
        for (String existingPerkId : existingPerkIds) {
            if (!perkSections.containsKey(existingPerkId)) {
                try {
                    Perk perk = findPerkByIdentifier(existingPerkId);
                    if (perk != null && perk.isEnabled()) {
                        perk.setEnabled(false);
                        rdq.getPerkRepository().update(perk);
                        LOGGER.info("Disabled perk no longer in configuration: " + existingPerkId);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to disable removed perk: " + existingPerkId, e);
                }
            }
        }

        LOGGER.info("Updated " + perks.size() + " perks from configuration");
    }

    /**
     * Updates perk requirements during reload.
     * This method handles updating requirements while preserving player progress.
     */
    private void updatePerkRequirementsForReload(String perkId, PerkSection config) {
        try {
            Perk perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            var configReqs = config.getRequirements();

            // If no requirements in config, clear existing requirements
            if (configReqs == null || configReqs.isEmpty()) {
                if (!perk.getRequirements().isEmpty()) {
                    LOGGER.info("Clearing requirements for perk: " + perkId);
                    perk.getRequirements().clear();
                    rdq.getPerkRepository().update(perk);
                }
                return;
            }

            // Check if requirements have changed
            boolean requirementsChanged = hasRequirementsChanged(perk, configReqs);
            
            if (!requirementsChanged) {
                LOGGER.fine("Requirements unchanged for perk: " + perkId);
                return;
            }

            LOGGER.info("Updating requirements for perk: " + perkId);

            // Clear old requirements
            perk.getRequirements().clear();
            rdq.getPerkRepository().update(perk);

            // Refetch perk
            perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            // Parse and add new requirements
            List<PerkRequirement> newRequirements = parseRequirements(perk, configReqs);

            if (newRequirements.isEmpty()) {
                return;
            }

            // Create PerkRequirement entities
            for (PerkRequirement newReq : newRequirements) {
                newReq.setPerk(perk);
                perk.addRequirement(newReq);
            }

            rdq.getPerkRepository().update(perk);
            LOGGER.info("Updated requirements for perk: " + perkId);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update requirements during reload for perk: " + perkId, e);
        }
    }

    /**
     * Updates perk unlock rewards during reload.
     */
    private void updatePerkUnlockRewardsForReload(String perkId, PerkSection config) {
        try {
            Perk perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            var configRewards = config.getUnlockRewards();

            // If no rewards in config, clear existing rewards
            if (configRewards == null || configRewards.isEmpty()) {
                if (!perk.getUnlockRewards().isEmpty()) {
                    LOGGER.info("Clearing unlock rewards for perk: " + perkId);
                    perk.getUnlockRewards().clear();
                    rdq.getPerkRepository().update(perk);
                }
                return;
            }

            // Check if rewards have changed
            boolean rewardsChanged = hasUnlockRewardsChanged(perk, configRewards);
            
            if (!rewardsChanged) {
                LOGGER.fine("Unlock rewards unchanged for perk: " + perkId);
                return;
            }

            LOGGER.info("Updating unlock rewards for perk: " + perkId);

            // Clear old rewards
            perk.getUnlockRewards().clear();
            rdq.getPerkRepository().update(perk);

            // Refetch perk
            perk = findPerkByIdentifier(perkId);
            if (perk == null) {
                return;
            }

            // Parse and add new rewards
            List<PerkUnlockReward> newRewards = parseUnlockRewards(perk, configRewards);

            if (newRewards.isEmpty()) {
                return;
            }

            // Create PerkUnlockReward entities
            for (PerkUnlockReward newReward : newRewards) {
                newReward.setPerk(perk);
                perk.addUnlockReward(newReward);
            }

            rdq.getPerkRepository().update(perk);
            LOGGER.info("Updated unlock rewards for perk: " + perkId);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update unlock rewards during reload for perk: " + perkId, e);
        }
    }

    /**
     * Checks if requirements have changed compared to the database.
     */
    private boolean hasRequirementsChanged(Perk perk, Map<String, BaseRequirementSection> configReqs) {
        // Simple check: if count differs, requirements changed
        if (perk.getRequirements().size() != configReqs.size()) {
            return true;
        }

        // For now, assume requirements changed if count is the same but we're reloading
        // A more sophisticated check would compare requirement types and values
        return true;
    }

    /**
     * Checks if unlock rewards have changed compared to the database.
     */
    private boolean hasUnlockRewardsChanged(Perk perk, Map<String, RewardSection> configRewards) {
        // Simple check: if count differs, rewards changed
        if (perk.getUnlockRewards().size() != configRewards.size()) {
            return true;
        }

        // For now, assume rewards changed if count is the same but we're reloading
        // A more sophisticated check would compare reward types and values
        return true;
    }
}
