package com.raindropcentral.rdq.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.database.entity.RRequirement;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.utility.requirement.RequirementFactory;
import com.raindropcentral.rplatform.logging.CentralLogger;
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

    private static final String FILE_PATH = "rank";
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
        this.requirementFactory = new RequirementFactory(rdq);
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
            LOGGER.info("Starting rank system initialization...");

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

            LOGGER.info("Rank system initialization completed successfully");
            logSummary();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize rank system", e);
            clearData();
        } finally {
            isInitializing = false;
        }
    }


    // ==================== Configuration Loading ====================

    private void loadConfigurations() {
        LOGGER.info("Loading configurations...");

        // Load system config
        rankSystemSection = loadSystemConfig();

        // Ensure paths directory exists
        File pathsDir = new File(rdq.getPlugin().getDataFolder(), FILE_PATH + "/" + FILE_RANK_PATH);
        if (pathsDir.mkdirs()) {
            LOGGER.info("Created rank paths directory");
        }

        // Load rank tree configs
        loadRankTreeConfigs();

        // Load rank configs from trees
        loadRankConfigs();

        LOGGER.info("Loaded " + rankTreeSections.size() + " trees with " +
            rankSections.values().stream().mapToInt(Map::size).sum() + " total ranks");
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

            LOGGER.info("Loading " + treeRanks.size() + " ranks from tree: " + treeId);
            
            treeRanks.forEach((rankId, rankSection) -> {
                rankSection.setRankTreeName(treeId);
                rankSection.setRankName(rankId);
                
                // Log requirements found in config
                var configRequirements = rankSection.getRequirements();
                if (configRequirements != null && !configRequirements.isEmpty()) {
                    LOGGER.info("  Config: Rank '" + rankId + "' has " + configRequirements.size() + " requirements: " + configRequirements.keySet());
                } else {
                    LOGGER.info("  Config: Rank '" + rankId + "' has no requirements configured");
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

        LOGGER.info("Creating ranks...");

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
        LOGGER.info("Created/updated " + total + " ranks");
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

            // Update requirements separately (fresh fetch to avoid version conflict)
            updateRankRequirements(rankId, config);

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

        // Add requirements to the newly created rank
        updateRankRequirements(rankId, config);

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

            // Clean up existing player progress first
            cleanupPlayerProgress(rank);

            // Debug: Log what requirements we're getting from config
            var configReqs = config.getRequirements();
            LOGGER.info("updateRankRequirements for '" + rankId + "': config has " + 
                (configReqs != null ? configReqs.size() : 0) + " requirements: " + 
                (configReqs != null ? configReqs.keySet() : "null"));

            // Parse new requirements
            List<RRankUpgradeRequirement> newRequirements = requirementFactory.parseRequirements(
                rank, configReqs
            );

            if (newRequirements.isEmpty()) {
                // Clear existing and update
                rank.getUpgradeRequirements().clear();
                rdq.getRankRepository().update(rank);
                LOGGER.info("No requirements found for rank: " + rankId);
                return;
            }

            // Save each requirement entity first
            List<RRequirement> savedRequirements = new ArrayList<>();
            for (RRankUpgradeRequirement upgradeReq : newRequirements) {
                RRequirement req = upgradeReq.getRequirement();
                if (req.getId() == null) {
                    req = rdq.getRequirementRepository().create(req);
                }
                savedRequirements.add(req);
            }

            // Fetch fresh rank again before modifying
            rank = findRankByIdentifier(rankId);
            if (rank == null) return;

            // Clear existing requirements
            rank.getUpgradeRequirements().clear();

            // Add new requirements
            for (int i = 0; i < newRequirements.size(); i++) {
                RRankUpgradeRequirement template = newRequirements.get(i);
                RRankUpgradeRequirement newUpgradeReq = new RRankUpgradeRequirement(
                    null, // Don't set rank yet - addUpgradeRequirement will handle it
                    savedRequirements.get(i),
                    template.getIcon()
                );
                newUpgradeReq.setDisplayOrder(template.getDisplayOrder());
                rank.addUpgradeRequirement(newUpgradeReq);
            }

            // Single update at the end
            rdq.getRankRepository().update(rank);
            LOGGER.info("Updated " + newRequirements.size() + " requirements for rank: " + rankId);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update requirements for rank: " + rankId, e);
        }
    }

    private void cleanupPlayerProgress(RRank rank) {
        try {
            for (RRankUpgradeRequirement upgradeReq : rank.getUpgradeRequirements()) {
                List<RPlayerRankUpgradeProgress> progress = rdq.getPlayerRankUpgradeProgressRepository()
                    .findListByAttributes(Map.of("upgradeRequirement", upgradeReq));

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
        // Always fetch fresh entity
        RRank rank = findRankByIdentifier(rankId);
        if (rank == null) return;

        List<String> prevRanks = config.getPreviousRanks().stream()
            .filter(validRankIds::contains)
            .collect(Collectors.toList());
        rank.setPreviousRanks(prevRanks);

        List<String> nextRanks = config.getNextRanks().stream()
            .filter(validRankIds::contains)
            .collect(Collectors.toList());
        rank.setNextRanks(nextRanks);

        rdq.getRankRepository().update(rank);

        // Also update the cached rank with the connections
        Map<String, RRank> cachedTreeRanks = ranks.get(treeId);
        if (cachedTreeRanks != null) {
            RRank cachedRank = cachedTreeRanks.get(rankId);
            if (cachedRank != null) {
                cachedRank.setPreviousRanks(prevRanks);
                cachedRank.setNextRanks(nextRanks);
            }
        }
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
        return rdq.getRankRepository().findByAttributes(Map.of("identifier", identifier));
    }

    @Nullable
    private RRankTree findTreeByIdentifier(String identifier) {
        return rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", identifier));
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
