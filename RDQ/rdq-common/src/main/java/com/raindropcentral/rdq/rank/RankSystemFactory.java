package com.raindropcentral.rdq.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSectionAdapter;
import com.raindropcentral.rdq.config.utility.RewardSection;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.config.RequirementFactory;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.config.RewardFactory;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Factory responsible for loading, constructing, and validating the rank system from configuration files.
 * <p>
 * Simplified version with proper entity lifecycle management to avoid OptimisticLockException.
 * Key improvements:
 * - Single-pass entity creation and updates
 * - Fresh entity fetches before each modification
 * - Proper transaction boundaries
 * - Reduced complexity and duplicate code
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since TBD
 */
public class RankSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLogger("RDQ");

    private static final String FILE_PATH = "ranks";
    private static final String FILE_RANK_PATH = "paths";
    private static final String FILE_NAME = "rank-system.yml";
    private static final List<String> INITIAL_RANKS = List.of(
        "cleric.yml", "mage.yml", "merchant.yml", "ranger.yml", "rogue.yml", "warrior.yml"
    );

    private final RDQ rdq;
    private final RequirementFactory requirementFactory;

    private volatile boolean isInitializing = false;
    private RankSystemSection rankSystemSection;

    // Cached data - populated during initialization
    private final Map<String, RankTreeSection> rankTreeSections = new HashMap<>();
    private final Map<String, Map<String, RankSection>> rankSections = new HashMap<>();
    private final Map<String, RRankTree> rankTrees = new HashMap<>();
    private final Map<String, Map<String, RRank>> ranks = new HashMap<>();
    private RRank defaultRank;

    public RankSystemFactory(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementFactory = RequirementFactory.getInstance();

        this.requirementFactory.registerSectionAdapter(
            BaseRequirementSection.class,
            BaseRequirementSectionAdapter.getInstance()
        );
    }

    /**
     * Initializes the rank system.
     */
    public void initialize() {
        if (isInitializing) {
            LOGGER.warning("Rank system initialization already in progress");
            return;
        }

        isInitializing = true;
        try {
            LOGGER.info("╔════════════════════════════════════════════════════════════╗");
            LOGGER.info("║          RANK SYSTEM INITIALIZATION STARTED                ║");
            LOGGER.info("╚════════════════════════════════════════════════════════════╝");

            // Phase 1: Load all configurations
            loadConfigurations();

            // Phase 2: Validate configurations
            validateConfigurations();

            // Phase 3: Create/update entities (single pass per entity type)
            createDefaultRank();
            createRankTrees();
            createRanks();

            // Phase 4: Establish connections (separate pass to avoid version conflicts)
            establishConnections();
            
            // Phase 5: Clean up orphaned player progress entries (after requirements are updated)
            cleanupOrphanedPlayerProgress();

            LOGGER.info("╔════════════════════════════════════════════════════════════╗");
            LOGGER.info("║       RANK SYSTEM INITIALIZATION COMPLETED                 ║");
            LOGGER.info("╚════════════════════════════════════════════════════════════╝");
            logSummary();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize rank system", e);
            clearData();
        } finally {
            isInitializing = false;
        }
    }
    
    /**
     * Cleans up orphaned player progress entries that reference non-existent requirements.
     * This should be called AFTER requirements are updated to avoid foreign key violations.
     */
    private void cleanupOrphanedPlayerProgress() {
        try {
            LOGGER.info("Cleaning up orphaned player progress entries...");
            List<RPlayerRankUpgradeProgress> allProgress = rdq.getPlayerRankUpgradeProgressRepository().findAll();
            int deletedCount = 0;
            int keptCount = 0;
            
            for (RPlayerRankUpgradeProgress progress : allProgress) {
                try {
                    // Check if the upgrade requirement still exists
                    RRankUpgradeRequirement upgradeReq = progress.getUpgradeRequirement();
                    if (upgradeReq == null || upgradeReq.getId() == null) {
                        // Orphaned - delete it
                        rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId());
                        deletedCount++;
                    } else {
                        // Verify the requirement actually exists in the database
                        boolean exists = false;
                        for (Map<String, RRank> treeRanks : ranks.values()) {
                            for (RRank rank : treeRanks.values()) {
                                if (rank.getUpgradeRequirements().stream()
                                    .anyMatch(req -> req.getId() != null && req.getId().equals(upgradeReq.getId()))) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (exists) break;
                        }
                        
                        if (!exists) {
                            // Orphaned - delete it
                            rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId());
                            deletedCount++;
                        } else {
                            keptCount++;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to check/delete progress entry " + progress.getId() + ": " + e.getMessage());
                }
            }
            
            LOGGER.info("Cleaned up " + deletedCount + " orphaned progress entries, kept " + keptCount + " valid entries");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to cleanup orphaned player progress", e);
        }
    }


    // ==================== Configuration Loading ====================

    private void loadConfigurations() {
        LOGGER.info("→ Loading rank configurations...");

        // Load system config
        rankSystemSection = loadSystemConfig();

        // Ensure paths directory exists
        File pathsDir = new File(rdq.getPlugin().getDataFolder(), FILE_PATH + "/" + FILE_RANK_PATH);
        if (pathsDir.mkdirs()) {
            LOGGER.fine("Created rank paths directory");
        }

        // Load rank tree configs
        loadRankTreeConfigs();

        // Load rank configs from trees
        loadRankConfigs();

        int totalRanks = rankSections.values().stream().mapToInt(Map::size).sum();
        LOGGER.info("  ✓ Loaded " + rankTreeSections.size() + " trees with " + totalRanks + " ranks");
    }

    private RankSystemSection loadSystemConfig() {
        try {
            ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), FILE_PATH);
            ConfigKeeper<RankSystemSection> cfgKeeper = new ConfigKeeper<>(cfgManager, FILE_NAME, RankSystemSection.class);
            return cfgKeeper.rootSection;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading rank system config, using defaults", e);
            return new RankSystemSection(new EvaluationEnvironmentBuilder());
        }
    }

    private void loadRankTreeConfigs() {
        File folder = new File(rdq.getPlugin().getDataFolder(), FILE_PATH + "/" + FILE_RANK_PATH);
        if (!folder.exists() || !folder.isDirectory()) return;

        // Load initial ranks first
        for (String fileName : INITIAL_RANKS) {
            loadSingleTreeConfig(fileName);
        }

        // Load additional files
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                if (!INITIAL_RANKS.contains(file.getName().toLowerCase())) {
                    loadSingleTreeConfig(file.getName());
                }
            }
        }
    }

    private void loadSingleTreeConfig(String fileName) {
        try {
            String treeId = toIdentifier(fileName);
            ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), FILE_PATH + "/" + FILE_RANK_PATH);
            ConfigKeeper<RankTreeSection> cfgKeeper = new ConfigKeeper<>(cfgManager, fileName, RankTreeSection.class);

            RankTreeSection section = cfgKeeper.rootSection;
            section.setTreeId(treeId);
            section.afterParsing(new ArrayList<>());

            rankTreeSections.put(treeId, section);
            LOGGER.fine("Loaded tree config: " + treeId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load tree config: " + fileName, e);
        }
    }

    private void loadRankConfigs() {
        rankTreeSections.forEach((treeId, treeSection) -> {
            Map<String, RankSection> treeRanks = treeSection.getRanks();
            if (treeRanks == null || treeRanks.isEmpty()) {
                LOGGER.warning("No ranks found in tree config: " + treeId);
                return;
            }

            LOGGER.fine("  Loading " + treeRanks.size() + " ranks from tree: " + treeId);
            
            treeRanks.forEach((rankId, rankSection) -> {
                rankSection.setRankTreeName(treeId);
                rankSection.setRankName(rankId);
                
                // Log requirements and rewards found in config (FINE level only)
                var configRequirements = rankSection.getRequirements();
                var configRewards = rankSection.getRewards();
                if (configRequirements != null && !configRequirements.isEmpty()) {
                    LOGGER.fine("    Rank '" + rankId + "' has " + configRequirements.size() + " requirements");
                }
                if (configRewards != null && !configRewards.isEmpty()) {
                    LOGGER.fine("    Rank '" + rankId + "' has " + configRewards.size() + " rewards");
                }
                
                processRequirementContext(rankSection, treeId, rankId);
                try {
                    rankSection.afterParsing(new ArrayList<>());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to process rank " + rankId, e);
                }
            });

            rankSections.put(treeId, treeRanks);
        });
    }

    private void processRequirementContext(RankSection rankSection, String treeId, String rankId) {
        Map<String, BaseRequirementSection> requirements = rankSection.getRequirements();
        if (requirements == null) return;

        requirements.forEach((reqKey, reqSection) -> {
            if (reqSection != null) {
                reqSection.setContext(treeId, rankId, reqKey);
                try {
                    reqSection.afterParsing(new ArrayList<>());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to process requirement " + reqKey, e);
                }
            }
        });
    }

    // ==================== Validation ====================

    private void validateConfigurations() {
        List<String> errors = new ArrayList<>();
        Set<String> validTreeIds = rankTreeSections.keySet();

        // Validate tree references
        rankTreeSections.forEach((treeId, config) -> {
            validateReferences(config.getPrerequisiteRankTrees(), validTreeIds, errors,
                "Tree " + treeId + " has invalid prerequisite: ");
            validateReferences(config.getUnlockedRankTrees(), validTreeIds, errors,
                "Tree " + treeId + " has invalid unlocked tree: ");
            validateReferences(config.getConnectedRankTrees(), validTreeIds, errors,
                "Tree " + treeId + " has invalid connected tree: ");
        });

        // Validate rank references within each tree
        rankSections.forEach((treeId, treeRanks) -> {
            Set<String> validRankIds = treeRanks.keySet();
            treeRanks.forEach((rankId, config) -> {
                validateReferences(config.getPreviousRanks(), validRankIds, errors,
                    "Rank " + rankId + " has invalid previous rank: ");
                validateReferences(config.getNextRanks(), validRankIds, errors,
                    "Rank " + rankId + " has invalid next rank: ");
            });
        });

        // Check for cycles in prerequisites
        rankTreeSections.keySet().forEach(treeId -> {
            if (hasCycle(treeId, new HashSet<>(), new HashSet<>())) {
                errors.add("Cycle detected in prerequisites starting at: " + treeId);
            }
        });

        if (!errors.isEmpty()) {
            errors.forEach(e -> LOGGER.severe("Validation error: " + e));
            throw new IllegalStateException("Configuration validation failed");
        }
    }

    private void validateReferences(List<String> refs, Set<String> valid, List<String> errors, String prefix) {
        if (refs == null) return;
        refs.stream().filter(r -> !valid.contains(r)).forEach(r -> errors.add(prefix + r));
    }

    private boolean hasCycle(String treeId, Set<String> visited, Set<String> stack) {
        if (stack.contains(treeId)) return true;
        if (visited.contains(treeId)) return false;

        visited.add(treeId);
        stack.add(treeId);

        RankTreeSection section = rankTreeSections.get(treeId);
        if (section != null) {
            for (String preReq : section.getPrerequisiteRankTrees()) {
                if (hasCycle(preReq, visited, stack)) return true;
            }
        }

        stack.remove(treeId);
        return false;
    }


    // ==================== Entity Creation ====================

    private void createDefaultRank() {
        if (rankSystemSection == null) return;

        String defaultRankId = rankSystemSection.getDefaultRank().getDefaultRankIdentifier();
        if (defaultRankId == null || defaultRankId.isBlank()) return;

        LOGGER.info("Creating default rank: " + defaultRankId);

        try {
            RRank existing = findRankByIdentifier(defaultRankId);
            if (existing != null) {
                defaultRank = existing;
                LOGGER.info("Using existing default rank: " + defaultRankId);
                return;
            }

            var cfg = rankSystemSection.getDefaultRank();
            RRank newRank = new RRank(
                defaultRankId,
                cfg.getDisplayNameKey(),
                cfg.getDescriptionKey(),
                cfg.getLuckPermsGroup(),
                cfg.getPrefixKey(),
                cfg.getSuffixKey(),
                cfg.getIcon(),
                true,
                cfg.getTier(),
                cfg.getWeight()
            );

            rdq.getRankRepository().create(newRank);
            defaultRank = newRank;
            LOGGER.info("Created default rank: " + defaultRankId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create default rank", e);
        }
    }

    private void createRankTrees() {
        if (rankTreeSections.isEmpty()) return;

        LOGGER.info("Creating " + rankTreeSections.size() + " rank trees...");

        rankTreeSections.forEach((treeId, config) -> {
            try {
                // Fix: force minimumRankTreesToBeDone to 0 if no prerequisites
                if (config.getMinimumRankTreesToBeDone() > 0 && config.getPrerequisiteRankTrees().isEmpty()) {
                    config.setMinimumRankTreesToBeDone(0);
                    LOGGER.info("Reset minimumRankTreesToBeDone to 0 for tree: " + treeId);
                }

                RRankTree existing = findTreeByIdentifier(treeId);
                if (existing != null) {
                    // Update existing tree
                    existing.setDisplayOrder(config.getDisplayOrder());
                    existing.setMinimumRankTreesToBeDone(config.getMinimumRankTreesToBeDone());
                    existing.setFinalRankTree(config.getFinalRankTree());
                    rdq.getRankTreeRepository().update(existing);
                    rankTrees.put(treeId, existing);
                    LOGGER.fine("Updated tree: " + treeId);
                } else {
                    // Create new tree
                    RRankTree newTree = new RRankTree(
                        treeId,
                        config.getDisplayNameKey(),
                        config.getDescriptionKey(),
                        config.getIcon(),
                        config.getDisplayOrder(),
                        config.getMinimumRankTreesToBeDone(),
                        config.getEnabled(),
                        config.getFinalRankTree()
                    );
                    
                    LOGGER.info("Creating new tree in DB: " + treeId + ", enabled=" + config.getEnabled());
                    RRankTree created = rdq.getRankTreeRepository().create(newTree);
                    LOGGER.info("Created tree with ID: " + (created != null ? created.getId() : "null"));
                    
                    rankTrees.put(treeId, created != null ? created : newTree);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to create/update tree: " + treeId, e);
            }
        });

        LOGGER.info("Created/updated " + rankTrees.size() + " rank trees");
    }

    private void createRanks() {
        if (rankSections.isEmpty()) return;

        LOGGER.info("→ Creating/updating ranks...");

        rankSections.forEach((treeId, treeRanks) -> {
            RRankTree rankTree = rankTrees.get(treeId);
            if (rankTree == null) {
                LOGGER.warning("Tree not found for ranks: " + treeId);
                return;
            }

            Map<String, RRank> createdRanks = new HashMap<>();

            treeRanks.forEach((rankId, config) -> {
                try {
                    RRank rank = createOrUpdateRank(rankId, config, rankTree);
                    if (rank != null) {
                        createdRanks.put(rankId, rank);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to create rank: " + rankId, e);
                }
            });

            ranks.put(treeId, createdRanks);
        });

        // Populate the ranks collection on cached RRankTree objects
        populateRankTreeRanksCollections();

        int total = ranks.values().stream().mapToInt(Map::size).sum();
        LOGGER.info("  ✓ Created/updated " + total + " ranks");
        
        // Now update requirements and rewards for all ranks
        LOGGER.info("→ Processing requirements and rewards...");
        int reqCount = 0;
        int rewCount = 0;
        rankSections.forEach((treeId, treeRanks) -> {
            treeRanks.forEach((rankId, config) -> {
                try {
                    updateRankRequirements(rankId, config);
                    updateRankRewards(rankId, config);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to update requirements/rewards for rank: " + rankId, e);
                }
            });
        });
        
        // Count total requirements and rewards
        for (Map<String, RRank> treeRanks : ranks.values()) {
            for (RRank rank : treeRanks.values()) {
                reqCount += rank.getUpgradeRequirements().size();
                rewCount += rank.getRewards().size();
            }
        }
        LOGGER.info("  ✓ Processed " + reqCount + " requirements and " + rewCount + " rewards");
        
        // Refresh cached ranks after updating requirements/rewards
        refreshCachedRanks();
    }
    
    /**
     * Refreshes the cached ranks by fetching fresh entities from the database.
     * This ensures that the cached ranks have the latest requirements and rewards.
     */
    private void refreshCachedRanks() {
        LOGGER.info("Refreshing cached ranks...");
        
        // Refresh the rank maps
        ranks.forEach((treeId, treeRanks) -> {
            treeRanks.forEach((rankId, rank) -> {
                RRank freshRank = findRankByIdentifier(rankId);
                if (freshRank != null) {
                    treeRanks.put(rankId, freshRank);
                }
            });
        });
        
        // CRITICAL: Also refresh the RRankTree objects themselves
        // The trees have their own ranks collection that needs to be updated
        rankTrees.forEach((treeId, tree) -> {
            RRankTree freshTree = findTreeByIdentifier(treeId);
            if (freshTree != null) {
                rankTrees.put(treeId, freshTree);
                LOGGER.fine("Refreshed tree: " + treeId);
            }
        });
        
        // Re-populate the ranks collection on the refreshed trees
        populateRankTreeRanksCollections();
        
        LOGGER.info("Cached ranks and trees refreshed");
    }

    /**
     * Populates the ranks collection on each cached RRankTree with the created ranks.
     * This ensures the in-memory cache has the ranks available without lazy loading.
     */
    private void populateRankTreeRanksCollections() {
        ranks.forEach((treeId, treeRanks) -> {
            RRankTree rankTree = rankTrees.get(treeId);
            if (rankTree != null) {
                List<RRank> rankList = new ArrayList<>(treeRanks.values());
                rankTree.setRanks(rankList);
                LOGGER.fine("Populated " + rankList.size() + " ranks for tree: " + treeId);
            }
        });
    }

    private RRank createOrUpdateRank(String rankId, RankSection config, RRankTree rankTree) {
        RRank existing = findRankByIdentifier(rankId);

        if (existing != null) {
            // Update tree association if needed
            boolean needsTreeUpdate = !Objects.equals(
                existing.getRankTree() != null ? existing.getRankTree().getId() : null,
                rankTree != null ? rankTree.getId() : null
            );

            if (needsTreeUpdate) {
                existing.setRankTree(rankTree);
                rdq.getRankRepository().update(existing);
            }

            // Note: Requirements and rewards are updated separately in their own methods
            // to avoid version conflicts. They will be updated after all ranks are created.

            return findRankByIdentifier(rankId); // Return fresh entity
        }

        // Create new rank
        RRank newRank = new RRank(
            rankId,
            config.getDisplayNameKey(),
            config.getDescriptionKey(),
            config.getLuckPermsGroup(),
            config.getPrefixKey(),
            config.getSuffixKey(),
            config.getIcon(),
            config.getInitialRank(),
            config.getTier(),
            config.getWeight(),
            rankTree
        );

        rdq.getRankRepository().create(newRank);
        LOGGER.fine("Created rank: " + rankId);

        return findRankByIdentifier(rankId); // Return fresh entity
    }

    /**
     * Updates rank requirements with proper entity lifecycle management.
     * Always fetches fresh entity to avoid OptimisticLockException.
     */
    private void updateRankRequirements(String rankId, RankSection config) {
        try {
            // Always fetch fresh entity
            RRank rank = findRankByIdentifier(rankId);
            if (rank == null) {
                LOGGER.warning("Rank not found for requirements update: " + rankId);
                return;
            }

            // Debug: Log what requirements we're getting from config
            var configReqs = config.getRequirements();
            LOGGER.fine("Processing requirements for '" + rankId + "': " + 
                (configReqs != null ? configReqs.size() : 0) + " configured");

            if (configReqs == null || configReqs.isEmpty()) {
                // Clear existing and update only if there are requirements to clear
                if (!rank.getUpgradeRequirements().isEmpty()) {
                    LOGGER.fine("Clearing " + rank.getUpgradeRequirements().size() + " existing requirements for rank: " + rankId);
                    // Clean up player progress before clearing requirements
                    cleanupPlayerProgress(rank);
                    rank.getUpgradeRequirements().clear();
                    rdq.getRankRepository().update(rank);
                }
                return;
            }

            // Check if rank already has requirements - skip if it does to avoid duplicates
            int existingCount = rank.getUpgradeRequirements().size();
            if (existingCount > 0) {
                LOGGER.fine("Rank '" + rankId + "' already has " + existingCount + " requirements (preserved)");
                return;
            }

            // Parse requirements using the new adapter-based API
            List<RRankUpgradeRequirement> newRequirements = parseRequirements(rank, configReqs);

            if (newRequirements.isEmpty()) {
                return;
            }

            // Save each requirement entity first
            List<BaseRequirement> savedRequirements = new ArrayList<>();
            for (RRankUpgradeRequirement upgradeReq : newRequirements) {
                BaseRequirement req = upgradeReq.getRequirement();
                if (req.getId() == null) {
                    req = rdq.getRequirementRepository().create(req);
                }
                savedRequirements.add(req);
            }

            // Fetch fresh rank again before modifying to ensure we have the latest version
            rank = findRankByIdentifier(rankId);
            if (rank == null) {
                LOGGER.warning("Rank disappeared after saving requirements: " + rankId);
                return;
            }

            // Double-check requirements haven't been added by another thread
            if (!rank.getUpgradeRequirements().isEmpty()) {
                LOGGER.fine("Rank '" + rankId + "' already has requirements (race condition detected)");
                return;
            }

            // Add new requirements to the rank entity
            // IMPORTANT: Set the rank directly in the constructor to avoid double-adding to the Set
            for (int i = 0; i < newRequirements.size(); i++) {
                RRankUpgradeRequirement template = newRequirements.get(i);
                RRankUpgradeRequirement newUpgradeReq = new RRankUpgradeRequirement(
                    rank, // Set rank in constructor - it will call addUpgradeRequirement internally
                    savedRequirements.get(i),
                    template.getIcon()
                );
                newUpgradeReq.setDisplayOrder(template.getDisplayOrder());
                // Don't call rank.addUpgradeRequirement() - already done in constructor!
            }

            // Single update at the end - this persists the rank with all its requirements
            try {
                rdq.getRankRepository().update(rank);
                LOGGER.fine("Added " + newRequirements.size() + " requirements to rank: " + rankId);
            } catch (jakarta.persistence.OptimisticLockException ole) {
                LOGGER.fine("OptimisticLockException for rank " + rankId + ", retrying...");
                
                // Retry once with a fresh fetch
                rank = findRankByIdentifier(rankId);
                if (rank != null && rank.getUpgradeRequirements().isEmpty()) {
                    // Re-add requirements (set rank in constructor to avoid double-adding)
                    for (int i = 0; i < newRequirements.size(); i++) {
                        RRankUpgradeRequirement template = newRequirements.get(i);
                        RRankUpgradeRequirement newUpgradeReq = new RRankUpgradeRequirement(
                            rank, // Set rank in constructor
                            savedRequirements.get(i),
                            template.getIcon()
                        );
                        newUpgradeReq.setDisplayOrder(template.getDisplayOrder());
                        // Don't call addUpgradeRequirement - already done in constructor
                    }
                    rdq.getRankRepository().update(rank);
                    LOGGER.fine("Retry successful for rank: " + rankId);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update requirements for rank: " + rankId, e);
        }
    }
    
    /**
     * Updates rank rewards with proper entity lifecycle management.
     * Always fetches fresh entity to avoid OptimisticLockException.
     */
    private void updateRankRewards(String rankId, RankSection config) {
        try {
            // Always fetch fresh entity
            RRank rank = findRankByIdentifier(rankId);
            if (rank == null) {
                LOGGER.warning("Rank not found for rewards update: " + rankId);
                return;
            }

            var configRewards = config.getRewards();
            LOGGER.fine("Processing rewards for '" + rankId + "': " + 
                (configRewards != null ? configRewards.size() : 0) + " configured");

            if (configRewards == null || configRewards.isEmpty()) {
                // Clear existing rewards only if there are any
                if (!rank.getRewards().isEmpty()) {
                    rank.getRewards().clear();
                    rdq.getRankRepository().update(rank);
                }
                return;
            }

            // Parse rewards using the reward factory
            List<RRankReward> newRewards = parseRewards(rank, configRewards);

            if (newRewards.isEmpty()) {
                // Clear existing rewards only if there are any
                if (!rank.getRewards().isEmpty()) {
                    rank.getRewards().clear();
                    rdq.getRankRepository().update(rank);
                }
                return;
            }

            // Check if rewards actually changed to avoid unnecessary updates
            boolean rewardsChanged = rank.getRewards().size() != newRewards.size();
            
            if (!rewardsChanged) {
                // Quick check if rewards are different (simplified comparison)
                // If sizes match but we still want to update, set to true
                rewardsChanged = true; // For now, always update if we have rewards
            }

            if (rewardsChanged) {
                // Clear existing rewards
                rank.getRewards().clear();
                
                // Update the rank to persist the cleared rewards
                rdq.getRankRepository().update(rank);
                
                // Fetch fresh rank after clearing
                rank = findRankByIdentifier(rankId);
                if (rank == null) return;

                // Add new rewards one by one
                for (RRankReward reward : newRewards) {
                    rank.addReward(reward);
                }

                // Final update to persist all new rewards
                rdq.getRankRepository().update(rank);
                LOGGER.fine("Added " + newRewards.size() + " rewards to rank: " + rankId);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update rewards for rank: " + rankId, e);
        }
    }

    /**
     * Parses BaseRequirementSection map into RRankUpgradeRequirement list.
     */
    private List<RRankUpgradeRequirement> parseRequirements(
            RRank rank,
            Map<String, BaseRequirementSection> configReqs
    ) {
        if (configReqs == null || configReqs.isEmpty()) {
            return List.of();
        }

        List<RRankUpgradeRequirement> requirements = new ArrayList<>();
        int displayOrder = 0;

        for (var entry : configReqs.entrySet()) {
            String key = entry.getKey();
            BaseRequirementSection section = entry.getValue();

            try {
                // Convert section to AbstractRequirement using the adapter
                AbstractRequirement abstractReq = requirementFactory.fromSection(section);

                // Wrap in BaseRequirement entity
                BaseRequirement baseReq = new BaseRequirement(abstractReq, section.getIcon());

                // Create upgrade requirement
                RRankUpgradeRequirement upgradeReq = new RRankUpgradeRequirement(
                    null, // rank will be set later
                    baseReq,
                    section.getIcon()
                );
                upgradeReq.setDisplayOrder(section.getDisplayOrder() != null ? section.getDisplayOrder() : displayOrder);
                
                requirements.add(upgradeReq);
                displayOrder++;

                LOGGER.fine("Parsed requirement '" + key + "' of type: " + section.getType());

            } catch (Exception e) {
                LOGGER.warning("Failed to parse requirement '" + key + "': " + e.getMessage());
            }
        }

        return requirements;
    }
    
    /**
     * Parses RewardSection map into RRankReward list.
     */
    private List<RRankReward> parseRewards(
            RRank rank,
            Map<String, RewardSection> configRewards
    ) {
        if (configRewards == null || configRewards.isEmpty()) {
            return List.of();
        }

        List<RRankReward> rewards = new ArrayList<>();
        int displayOrder = 0;

        for (var entry : configRewards.entrySet()) {
            String key = entry.getKey();
            RewardSection section = entry.getValue();

            try {
                // Convert section to AbstractReward using the reward factory
                @SuppressWarnings("unchecked")
                final RewardFactory<RewardSection> rewardFactory = (RewardFactory<RewardSection>) (RewardFactory<?>) RewardFactory.getInstance();
                AbstractReward abstractReward = rewardFactory.fromSection(section);

                // Create rank reward WITHOUT setting rank (will be set by addReward method)
                RRankReward rankReward = new RRankReward();
                rankReward.setReward(abstractReward);
                rankReward.setDisplayOrder(displayOrder);
                rankReward.setAutoGrant(true); // Default to auto-grant
                
                rewards.add(rankReward);
                displayOrder++;

                LOGGER.fine("Parsed reward '" + key + "' of type: " + section.getType());

            } catch (Exception e) {
                LOGGER.warning("Failed to parse reward '" + key + "': " + e.getMessage());
            }
        }

        return rewards;
    }

    private void cleanupPlayerProgress(RRank rank) {
        try {
            for (RRankUpgradeRequirement upgradeReq : rank.getUpgradeRequirements()) {
                List<RPlayerRankUpgradeProgress> progress = rdq.getPlayerRankUpgradeProgressRepository()
                    .findAllByAttributes(Map.of("upgradeRequirement", upgradeReq));

                for (RPlayerRankUpgradeProgress p : progress) {
                    rdq.getPlayerRankUpgradeProgressRepository().delete(p.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to cleanup player progress for rank: " + rank.getIdentifier(), e);
        }
    }


    // ==================== Connection Establishment ====================

    private void establishConnections() {
        LOGGER.info("Establishing connections...");

        // Establish rank connections
        rankSections.forEach((treeId, treeRanks) -> {
            Set<String> validRankIds = treeRanks.keySet();
            treeRanks.forEach((rankId, config) -> {
                try {
                    updateRankConnections(rankId, config, validRankIds, treeId);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to update connections for rank: " + rankId, e);
                }
            });
        });

        // Establish tree connections
        rankTreeSections.forEach((treeId, config) -> {
            try {
                updateTreeConnections(treeId, config);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to update connections for tree: " + treeId, e);
            }
        });

        LOGGER.info("Connections established");
    }

    private void updateRankConnections(String rankId, RankSection config, Set<String> validRankIds, String treeId) {
        // During initialization, we only update the cached rank objects, not the database
        // This avoids OptimisticLockException from too many sequential updates
        Map<String, RRank> cachedTreeRanks = ranks.get(treeId);
        if (cachedTreeRanks == null) return;
        
        RRank cachedRank = cachedTreeRanks.get(rankId);
        if (cachedRank == null) return;

        List<String> prevRanks = config.getPreviousRanks().stream()
            .filter(validRankIds::contains)
            .collect(Collectors.toList());
        
        List<String> nextRanks = config.getNextRanks().stream()
            .filter(validRankIds::contains)
            .collect(Collectors.toList());

        // Only update the cached object - no database update during initialization
        cachedRank.setPreviousRanks(prevRanks);
        cachedRank.setNextRanks(nextRanks);
        
        LOGGER.fine("Set connections for cached rank: " + rankId + 
            " (prev: " + prevRanks.size() + ", next: " + nextRanks.size() + ")");
    }

    private void updateTreeConnections(String treeId, RankTreeSection config) {
        // Always fetch fresh entity
        RRankTree tree = findTreeByIdentifier(treeId);
        if (tree == null) return;

        // Resolve prerequisite trees
        List<RRankTree> preReqTrees = config.getPrerequisiteRankTrees().stream()
            .map(this::findTreeByIdentifier)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        tree.setPrerequisiteRankTrees(preReqTrees);

        // Resolve unlocked trees
        List<RRankTree> unlockedTrees = config.getUnlockedRankTrees().stream()
            .map(this::findTreeByIdentifier)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        tree.setUnlockedRankTrees(unlockedTrees);

        // Resolve connected trees
        List<RRankTree> connectedTrees = config.getConnectedRankTrees().stream()
            .map(this::findTreeByIdentifier)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        tree.setConnectedRankTrees(connectedTrees);

        rdq.getRankTreeRepository().update(tree);
    }

    // ==================== Helper Methods ====================

    @Nullable
    private RRank findRankByIdentifier(String identifier) {
        return rdq.getRankRepository().findByAttributes(Map.of("identifier", identifier)).orElse(null);
    }

    @Nullable
    private RRankTree findTreeByIdentifier(String identifier) {
        return rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", identifier)).orElse(null);
    }

    private String toIdentifier(String fileName) {
        return fileName.replace(".yml", "")
            .replace(" ", "")
            .replace("-", "_")
            .toLowerCase();
    }

    private void clearData() {
        rankTreeSections.clear();
        rankSections.clear();
        rankTrees.clear();
        ranks.clear();
        defaultRank = null;
    }

    private void logSummary() {
        int totalRanks = ranks.values().stream().mapToInt(Map::size).sum();
        int totalRequirements = 0;
        
        LOGGER.info("=== Rank System Summary ===");
        LOGGER.info("Rank Trees: " + rankTrees.size());
        LOGGER.info("Total Ranks: " + totalRanks);
        LOGGER.info("Default Rank: " + (defaultRank != null ? defaultRank.getIdentifier() : "None"));
        
        // Log requirements per tree and rank
        for (var treeEntry : ranks.entrySet()) {
            String treeId = treeEntry.getKey();
            Map<String, RRank> treeRanks = treeEntry.getValue();
            int treeReqCount = 0;
            
            for (var rankEntry : treeRanks.entrySet()) {
                RRank rank = rankEntry.getValue();
                int reqCount = rank.getUpgradeRequirements() != null ? rank.getUpgradeRequirements().size() : 0;
                treeReqCount += reqCount;
                
                if (reqCount > 0) {
                    LOGGER.info("  [" + treeId + "] Rank '" + rank.getIdentifier() + "' has " + reqCount + " requirements");
                }
            }
            
            LOGGER.info("Tree '" + treeId + "': " + treeRanks.size() + " ranks, " + treeReqCount + " total requirements");
            totalRequirements += treeReqCount;
        }
        
        LOGGER.info("Total Requirements: " + totalRequirements);
        LOGGER.info("===========================");
    }

    // ==================== Public Getters ====================

    public Map<String, RRankTree> getRankTrees() {
        return Map.copyOf(rankTrees);
    }

    public Map<String, Map<String, RRank>> getRanks() {
        return ranks.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
    }

    @Nullable
    public RRank getDefaultRank() {
        return defaultRank;
    }

    public boolean isInitialized() {
        return !rankTrees.isEmpty() || defaultRank != null;
    }

    public RankSystemSection getRankSystemSection() {
        return rankSystemSection;
    }
}
