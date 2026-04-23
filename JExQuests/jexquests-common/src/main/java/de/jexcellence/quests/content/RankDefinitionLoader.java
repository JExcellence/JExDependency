package de.jexcellence.quests.content;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import de.jexcellence.quests.database.repository.RankRepository;
import de.jexcellence.quests.database.repository.RankTreeRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Loads rank tree + rank definitions from
 * {@code plugins/JExQuests/ranks/paths/&lt;tree&gt;.yml}. One file
 * per tree. The YAML schema mirrors RDQ's {@code rank-system.yml}:
 *
 * <pre>{@code
 * identifier: warrior
 * displayName: "Warrior"
 * description: "..."
 * displayOrder: 1
 * enabled: true
 * icon:
 *   material: IRON_SWORD
 * finalTree: false
 * requiresAllDone: false
 * minimumTreesToBeDone: 0
 * prerequisiteTrees: []     # list of tree identifiers
 * allowSwitching: true
 * switchCooldownSeconds: 86400
 * ranks:
 *   recruit:
 *     tier: 1
 *     weight: 100
 *     orderIndex: 0            # optional — falls back to tier-based discovery
 *     initialRank: true
 *     luckPermsGroup: warrior_recruit
 *     prefixKey: "&8[&7Recruit&8]"
 *     displayName: "Recruit"
 *     description: "..."
 *     nextRanks: [soldier]     # list or comma-string
 *     previousRanks: []
 *     icon: { material: WOODEN_SWORD }
 *     requirement: { ... }
 *     reward: { ... }
 * }</pre>
 */
public final class RankDefinitionLoader {

    private static final String DEFINITIONS_DIR = "ranks/paths";

    private final JavaPlugin plugin;
    private final RankTreeRepository trees;
    private final RankRepository ranks;
    private final JExLogger logger;

    public RankDefinitionLoader(
            @NotNull JavaPlugin plugin,
            @NotNull RankTreeRepository trees,
            @NotNull RankRepository ranks,
            @NotNull JExLogger logger
    ) {
        this.plugin = plugin;
        this.trees = trees;
        this.ranks = ranks;
        this.logger = logger;
    }

    public int load() {
        final File root = new File(this.plugin.getDataFolder(), DEFINITIONS_DIR);
        if (!root.exists()) {
            this.logger.info("No rank definitions directory at {} — skipping load", root.getPath());
            return 0;
        }
        int applied = 0;
        for (final Path path : ContentLoaderSupport.yamlFiles(root)) {
            try {
                if (loadOne(path)) applied++;
            } catch (final RuntimeException ex) {
                this.logger.error("rank definition failed at {}: {}", path, ex.getMessage());
            }
        }
        this.logger.info("Rank trees loaded: {}", applied);
        return applied;
    }

    private boolean loadOne(@NotNull Path path) {
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(path.toFile());

        final String treeId = cfg.getString("identifier");
        if (treeId == null || treeId.isBlank()) {
            this.logger.warn("rank tree {} missing identifier — skipped", path);
            return false;
        }

        final Optional<RankTree> existing = this.trees.findByIdentifier(treeId);
        final RankTree tree = existing.orElseGet(() -> new RankTree(treeId, cfg.getString("displayName", treeId)));
        tree.setDisplayName(cfg.getString("displayName", treeId));
        tree.setDescription(cfg.getString("description"));
        tree.setDisplayOrder(cfg.getInt("displayOrder", 0));
        tree.setEnabled(cfg.getBoolean("enabled", true));
        tree.setFinalTree(cfg.getBoolean("finalTree", false));
        tree.setRequiresAllDone(cfg.getBoolean("requiresAllDone", false));
        tree.setMinimumTreesToBeDone(cfg.getInt("minimumTreesToBeDone", 0));
        tree.setAllowSwitching(cfg.getBoolean("allowSwitching", true));
        tree.setSwitchCooldownSeconds(cfg.getLong("switchCooldownSeconds", 0L));
        tree.setPrerequisiteTrees(joinCsv(cfg.getStringList("prerequisiteTrees")));

        final ConfigurationSection iconSection = cfg.getConfigurationSection("icon");
        if (iconSection != null) {
            tree.setIconMaterial(iconSection.getString("material", iconSection.getString("type")));
        }

        final RankTree persistedTree = existing.isPresent() ? this.trees.update(tree) : this.trees.create(tree);

        final ConfigurationSection ranksSection = cfg.getConfigurationSection("ranks");
        if (ranksSection == null) return true;

        int orderFallback = 0;
        for (final String rankId : ranksSection.getKeys(false)) {
            final ConfigurationSection rs = ranksSection.getConfigurationSection(rankId);
            if (rs == null) continue;

            final int orderIndex = rs.getInt("orderIndex", orderFallback++);
            // Upsert by (tree, identifier): reuse the existing row so
            // Hibernate emits UPDATE instead of an INSERT that would
            // violate the unique (rank_tree_id, identifier) constraint
            // on plugin reload / server restart.
            final Optional<Rank> existingRank = this.ranks.findByTreeAndIdentifier(persistedTree, rankId);
            final Rank rank = existingRank.orElseGet(() -> new Rank(
                    persistedTree,
                    rankId,
                    rs.getString("displayName", rankId),
                    orderIndex
            ));
            rank.setTree(persistedTree);
            rank.setDisplayName(rs.getString("displayName", rankId));
            rank.setOrderIndex(orderIndex);
            rank.setDescription(rs.getString("description"));
            rank.setIconData(ContentLoaderSupport.sectionToJson(rs.getConfigurationSection("icon")));
            rank.setRequirementData(ContentLoaderSupport.sectionToJson(rs.getConfigurationSection("requirement")));
            rank.setRewardData(ContentLoaderSupport.sectionToJson(rs.getConfigurationSection("reward")));
            rank.setTier(rs.getInt("tier", Math.max(1, orderIndex + 1)));
            rank.setWeight(rs.getInt("weight", 100));
            rank.setInitialRank(rs.getBoolean("initialRank", orderIndex == 0));
            rank.setLuckPermsGroup(rs.getString("luckPermsGroup"));
            rank.setPrefixKey(rs.getString("prefixKey"));
            rank.setSuffixKey(rs.getString("suffixKey"));
            rank.setPreviousRanks(joinCsv(rs.getStringList("previousRanks")));
            rank.setNextRanks(joinCsv(rs.getStringList("nextRanks")));
            if (existingRank.isPresent()) this.ranks.update(rank);
            else this.ranks.create(rank);
        }
        return true;
    }

    /** Joins a YAML string-list into a comma-separated column value. Returns null for empty. */
    private static String joinCsv(@NotNull List<String> raw) {
        if (raw.isEmpty()) return null;
        return String.join(",", raw);
    }
}
