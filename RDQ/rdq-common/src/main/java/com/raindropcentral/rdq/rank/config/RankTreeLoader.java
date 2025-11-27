package com.raindropcentral.rdq.rank.config;

import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankRequirement;
import com.raindropcentral.rdq.rank.RankTree;
import com.raindropcentral.rdq.rank.repository.RankRepository;
import com.raindropcentral.rdq.rank.repository.RankTreeRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankTreeLoader {

    private static final Logger LOGGER = Logger.getLogger(RankTreeLoader.class.getName());

    private final File pathsDirectory;
    private final RankTreeRepository treeRepository;
    private final RankRepository rankRepository;

    public RankTreeLoader(
        @NotNull File pathsDirectory,
        @NotNull RankTreeRepository treeRepository,
        @NotNull RankRepository rankRepository
    ) {
        this.pathsDirectory = pathsDirectory;
        this.treeRepository = treeRepository;
        this.rankRepository = rankRepository;
    }

    public void load() {
        if (!pathsDirectory.exists() || !pathsDirectory.isDirectory()) {
            LOGGER.warning("Rank paths directory does not exist: " + pathsDirectory.getAbsolutePath());
            return;
        }

        var files = pathsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            LOGGER.warning("No rank tree files found in: " + pathsDirectory.getAbsolutePath());
            return;
        }

        var trees = new ArrayList<RankTree>();
        var allRanks = new ArrayList<Rank>();

        for (var file : files) {
            try {
                var result = loadTreeFromFile(file);
                if (result != null) {
                    trees.add(result.tree());
                    allRanks.addAll(result.ranks());
                    LOGGER.info("Loaded rank tree: " + result.tree().id() + " with " + result.ranks().size() + " ranks");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load rank tree from: " + file.getName(), e);
            }
        }

        treeRepository.reload(trees);
        rankRepository.reload(allRanks);
        LOGGER.info("Loaded " + trees.size() + " rank trees with " + allRanks.size() + " total ranks");
    }


    public void reload() {
        treeRepository.clear();
        rankRepository.clear();
        load();
    }

    private record LoadResult(RankTree tree, List<Rank> ranks) {}

    private LoadResult loadTreeFromFile(@NotNull File file) {
        var config = YamlConfiguration.loadConfiguration(file);

        var id = config.getString("id");
        if (id == null || id.isBlank()) {
            LOGGER.warning("Rank tree file missing 'id': " + file.getName());
            return null;
        }

        var displayNameKey = config.getString("displayNameKey", "rank.tree." + id + ".name");
        var descriptionKey = config.getString("descriptionKey", "rank.tree." + id + ".description");
        var iconMaterial = config.getString("iconMaterial", "DIAMOND");
        var displayOrder = config.getInt("displayOrder", 0);
        var enabled = config.getBoolean("enabled", true);

        var ranksSection = config.getConfigurationSection("ranks");
        var ranks = new ArrayList<Rank>();

        if (ranksSection != null) {
            for (var rankKey : ranksSection.getKeys(false)) {
                var rankSection = ranksSection.getConfigurationSection(rankKey);
                if (rankSection != null) {
                    var rank = loadRank(id, rankKey, rankSection);
                    if (rank != null) {
                        ranks.add(rank);
                    }
                }
            }
        }

        ranks.sort((a, b) -> Integer.compare(a.tier(), b.tier()));

        var tree = new RankTree(id, displayNameKey, descriptionKey, iconMaterial, displayOrder, enabled, ranks);
        return new LoadResult(tree, ranks);
    }

    private Rank loadRank(@NotNull String treeId, @NotNull String rankId, @NotNull ConfigurationSection section) {
        var displayNameKey = section.getString("displayNameKey", "rank." + treeId + "." + rankId + ".name");
        var descriptionKey = section.getString("descriptionKey", "rank." + treeId + "." + rankId + ".description");
        var tier = section.getInt("tier", 1);
        var weight = section.getInt("weight", tier);
        var luckPermsGroup = section.getString("luckPermsGroup");
        var prefixKey = section.getString("prefixKey");
        var suffixKey = section.getString("suffixKey");
        var iconMaterial = section.getString("iconMaterial", "STONE");
        var enabled = section.getBoolean("enabled", true);

        var requirements = loadRequirements(section.getConfigurationSection("requirements"));

        return new Rank(
            rankId, treeId, displayNameKey, descriptionKey, tier, weight,
            luckPermsGroup, prefixKey, suffixKey, iconMaterial, enabled, requirements
        );
    }


    private List<RankRequirement> loadRequirements(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }

        var requirements = new ArrayList<RankRequirement>();

        for (var key : section.getKeys(false)) {
            var reqSection = section.getConfigurationSection(key);
            if (reqSection == null) continue;

            var type = reqSection.getString("type", "").toUpperCase();
            var requirement = switch (type) {
                case "STATISTIC" -> new RankRequirement.StatisticRequirement(
                    reqSection.getString("statistic", ""),
                    reqSection.getInt("amount", 0)
                );
                case "PERMISSION" -> new RankRequirement.PermissionRequirement(
                    reqSection.getString("permission", "")
                );
                case "PREVIOUS_RANK", "RANK" -> new RankRequirement.PreviousRankRequirement(
                    reqSection.getString("rankId", "")
                );
                case "CURRENCY" -> new RankRequirement.CurrencyRequirement(
                    reqSection.getString("currency", "default"),
                    BigDecimal.valueOf(reqSection.getDouble("amount", 0))
                );
                case "ITEM" -> new RankRequirement.ItemRequirement(
                    reqSection.getString("material", "STONE"),
                    reqSection.getInt("amount", 1)
                );
                case "LEVEL" -> new RankRequirement.LevelRequirement(
                    reqSection.getInt("level", 0)
                );
                case "PLAYTIME" -> new RankRequirement.PlaytimeRequirement(
                    reqSection.getLong("seconds", 0)
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
}
