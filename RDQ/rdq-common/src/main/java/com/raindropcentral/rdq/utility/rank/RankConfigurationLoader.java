package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RankConfigurationLoader {

    private static final Logger LOGGER = CentralLogger.getLogger(RankConfigurationLoader.class.getName());

    private static final String DIR_ROOT = "rank";
    private static final String DIR_TREES = "paths";
    private static final String FILE_SYSTEM = "rank-system.yml";

    private static final List<String> INITIAL_TREE_FILES = List.of(
            "cleric.yml", "mage.yml", "merchant.yml", "ranger.yml", "rogue.yml", "warrior.yml"
    );

    private final @NotNull RDQ rdq;

    RankConfigurationLoader(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    CompletableFuture<RankSystemState> loadAllAsync(final @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(this::loadAll, executor);
    }

    private RankSystemState loadAll() {
        final RankSystemSection systemSection = loadSystemSection();
        final Map<String, RankTreeSection> treeSections = loadTreeSections();
        final Map<String, Map<String, RankSection>> rankSections = loadRankSections(treeSections);

        return RankSystemState.builder()
                .rankSystemSection(systemSection)
                .rankTreeSections(treeSections)
                .rankSections(rankSections)
                .build();
    }

    private RankSystemSection loadSystemSection() {
        try {
            final ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), DIR_ROOT);
            final ConfigKeeper<RankSystemSection> keeper = new ConfigKeeper<>(cfgManager, FILE_SYSTEM, RankSystemSection.class);
            ensureTreesDirectory();
            LOGGER.info("Loaded rank system configuration");
            return keeper.rootSection;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load rank system configuration, using fallback", e);
            return new RankSystemSection(new EvaluationEnvironmentBuilder());
        }
    }

    private Map<String, RankTreeSection> loadTreeSections() {
        final Map<String, RankTreeSection> sections = new HashMap<>();
        final File folder = new File(rdq.getPlugin().getDataFolder(), DIR_ROOT + "/" + DIR_TREES);

        if (!folder.exists() || !folder.isDirectory()) {
            LOGGER.log(Level.INFO, "Rank tree directory not found: {0}", folder.getAbsolutePath());
            return sections;
        }

        for (String fileName : INITIAL_TREE_FILES) {
            try {
                final String id = normalize(fileName);
                final RankTreeSection section = loadTree(fileName);
                section.setTreeId(id);
                section.afterParsing(new ArrayList<>());
                sections.put(id, section);
                LOGGER.log(Level.INFO, "Loaded rank tree configuration: {0}", id);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load initial rank tree configuration: " + fileName, e);
            }
        }

        final File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            LOGGER.info("No additional rank tree configuration files found");
            return sections;
        }

        for (File file : files) {
            final String name = file.getName();
            if (INITIAL_TREE_FILES.contains(name.toLowerCase(Locale.ROOT))) continue;

            try {
                final String id = normalize(name);
                final RankTreeSection section = loadTree(name);
                section.setTreeId(id);
                section.afterParsing(new ArrayList<>());
                sections.put(id, section);
                LOGGER.log(Level.INFO, "Loaded rank tree configuration: {0}", id);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load rank tree configuration: " + name, e);
            }
        }

        LOGGER.log(Level.INFO, "Loaded {0} rank tree configurations", sections.size());
        return sections;
    }

    private Map<String, Map<String, RankSection>> loadRankSections(final Map<String, RankTreeSection> treeSections) {
        final Map<String, Map<String, RankSection>> all = new HashMap<>();
        treeSections.forEach((treeId, treeSection) -> {
            final Map<String, RankSection> ranks = treeSection.getRanks();
            if (ranks == null || ranks.isEmpty()) return;

            ranks.forEach((rankId, section) -> {
                section.setRankTreeName(treeId);
                section.setRankName(rankId);
                RankRequirementContext.apply(section, treeId, rankId, LOGGER);
                try {
                    section.afterParsing(new ArrayList<>());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to parse rank {0} in tree {1}: {2}", new Object[]{rankId, treeId, e.getMessage()});
                }
            });

            all.put(treeId, ranks);
            LOGGER.log(Level.INFO, "Loaded {0} rank configurations for tree: {0}", ranks.size());
        });
        return all;
    }

    private RankTreeSection loadTree(final String file) throws Exception {
        try {
            final ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), DIR_ROOT + "/" + DIR_TREES);
            final ConfigKeeper<RankTreeSection> keeper = new ConfigKeeper<>(cfgManager, file, RankTreeSection.class);
            return keeper.rootSection;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("InstantiationException")) {
                LOGGER.log(Level.WARNING, "Polymorphic requirement loading failed for {0}, attempting fallback", file);
                final ConfigManager cfgManager = new ConfigManager(rdq.getPlugin(), DIR_ROOT + "/" + DIR_TREES);
                final ConfigKeeper<RankTreeSection> keeper = new ConfigKeeper<>(cfgManager, file, RankTreeSection.class);
                return keeper.rootSection;
            }
            throw e;
        }
    }

    private void ensureTreesDirectory() {
        final File dir = new File(rdq.getPlugin().getDataFolder(), DIR_ROOT + "/" + DIR_TREES);
        if (dir.mkdir()) {
            LOGGER.log(Level.INFO, "Created directory: {0}", dir.getAbsolutePath());
        }
    }

    private static String normalize(final String identifier) {
        return identifier.replace(".yml", "").replace(" ", "").replace("-", "_").toLowerCase(Locale.ROOT);
    }
}