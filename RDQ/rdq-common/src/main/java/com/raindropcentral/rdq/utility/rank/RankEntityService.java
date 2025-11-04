package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.utility.requirement.RequirementFactory;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Coordinates the creation and synchronization of rank entities, trees, and their relationships
 * against configuration-driven definitions.
 *
 * <p>This service is invoked during rank system initialization and ensures that default ranks,
 * rank trees, and individual ranks are present in persistent storage. It also maintains
 * connections between these entities and resolves any requirement prerequisites before the
 * entities become available to gameplay systems.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RankEntityService {

    private static final Logger LOGGER = CentralLogger.getLogger(RankEntityService.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull RequirementFactory requirementFactory;

    /**
     * Creates a new service using the provided RDQ plugin instance.
     *
     * @param rdq the RDQ plugin dependency that exposes repositories and configuration
     */
    RankEntityService(final @NotNull RDQ rdq) {
        this.rdq = rdq;
        this.requirementFactory = new RequirementFactory(rdq);
    }

    /**
     * Ensures the configured default rank exists within persistence and associates it with the
     * provided state.
     *
     * @param state    the aggregated rank system state awaiting mutation
     * @param executor the executor used to perform the persistence operations asynchronously
     * @return a future that completes once the default rank has been validated or created
     */
    CompletableFuture<Void> createDefaultRankAsync(final @NotNull RankSystemState state,
                                                   final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            if (state.rankSystemSection() == null) return;
            final String id = state.rankSystemSection().getDefaultRank().getDefaultRankIdentifier();
            if (id == null || id.isBlank()) return;

            final RRank existing = rdq.getRankRepository().findByAttributes(Map.of("identifier", id));
            if (existing != null) {
                state.setDefaultRank(existing);
                LOGGER.log(Level.INFO, "Found existing default rank: {0}", id);
                return;
            }

            final RRank created = new RRank(
                    id,
                    state.rankSystemSection().getDefaultRank().getDisplayNameKey(),
                    state.rankSystemSection().getDefaultRank().getDescriptionKey(),
                    state.rankSystemSection().getDefaultRank().getLuckPermsGroup(),
                    state.rankSystemSection().getDefaultRank().getPrefixKey(),
                    state.rankSystemSection().getDefaultRank().getSuffixKey(),
                    state.rankSystemSection().getDefaultRank().getIcon(),
                    true,
                    state.rankSystemSection().getDefaultRank().getTier(),
                    state.rankSystemSection().getDefaultRank().getWeight(),
                    null
            );
            rdq.getRankRepository().create(created);
            state.setDefaultRank(created);
            LOGGER.log(Level.INFO, "Created default rank: {0}", id);
        }, executor);
    }

    /**
     * Synchronizes rank tree definitions from configuration into the backing repository.
     *
     * @param state    the aggregated rank system state containing configured trees
     * @param executor the executor used for asynchronous persistence work
     * @return a future that resolves after rank trees have been created or updated
     */
    CompletableFuture<Void> createRankTreesAsync(final @NotNull RankSystemState state,
                                                 final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            if (state.rankTreeSections().isEmpty()) return;

            for (Map.Entry<String, RankTreeSection> e : state.rankTreeSections().entrySet()) {
                final String treeId = e.getKey();
                final RankTreeSection cfg = ensureTreePrereqConsistency(e.getValue());

                final RRankTree existing = rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", treeId));
                if (existing != null) {
                    existing.setDisplayOrder(cfg.getDisplayOrder());
                    existing.setMinimumRankTreesToBeDone(cfg.getMinimumRankTreesToBeDone());
                    existing.setFinalRankTree(cfg.getFinalRankTree());
                    rdq.getRankTreeRepository().update(existing);
                    state.rankTrees().put(treeId, existing);
                    LOGGER.log(Level.INFO, "Updated rank tree: {0}", treeId);
                    continue;
                }

                final RRankTree tree = new RRankTree(
                        treeId,
                        cfg.getDisplayNameKey(),
                        cfg.getDescriptionKey(),
                        cfg.getIcon(),
                        cfg.getDisplayOrder(),
                        cfg.getMinimumRankTreesToBeDone(),
                        cfg.getEnabled(),
                        cfg.getFinalRankTree()
                );
                rdq.getRankTreeRepository().create(tree);
                state.rankTrees().put(treeId, tree);
                LOGGER.log(Level.INFO, "Created rank tree: {0}", treeId);
            }
        }, executor);
    }

    /**
     * Creates or updates rank entities for the provided configuration payload and binds them to
     * the correct rank trees.
     *
     * @param state    the aggregated rank system state containing configured ranks
     * @param executor the executor used for asynchronous persistence work
     * @return a future that resolves when all rank entities have been synchronized
     */
    CompletableFuture<Void> createRanksAsync(final @NotNull RankSystemState state,
                                             final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            if (state.rankSections().isEmpty()) return;

            for (Map.Entry<String, Map<String, RankSection>> entry : state.rankSections().entrySet()) {
                final String treeId = entry.getKey();
                final RRankTree tree = state.rankTrees().get(treeId);
                if (tree == null) {
                    LOGGER.log(Level.WARNING, "Rank tree not found for ID: {0}, skipping ranks", treeId);
                    continue;
                }
                final Map<String, RRank> treeRanks = new HashMap<>();
                for (Map.Entry<String, RankSection> re : entry.getValue().entrySet()) {
                    final String rankId = re.getKey();
                    final RankSection cfg = re.getValue();

                    final RRank existing = rdq.getRankRepository().findByAttributes(Map.of("identifier", rankId));
                    if (existing != null) {
                        boolean assocDiffers = existing.getRankTree() == null || existing.getRankTree() != null && !Objects.equals(existing.getRankTree().getId(), tree.getId());
                        if (assocDiffers) {
                            existing.setRankTree(tree);
                            rdq.getRankRepository().update(existing);
                            LOGGER.log(Level.INFO, "Updated rank tree association for rank: {0}", rankId);
                        }
                        updateRankRequirements(existing, cfg);
                        treeRanks.put(rankId, existing);
                        LOGGER.log(Level.INFO, "Reused existing rank: {0}", rankId);
                        continue;
                    }

                    final RRank created = new RRank(
                            rankId,
                            cfg.getDisplayNameKey(),
                            cfg.getDescriptionKey(),
                            cfg.getLuckPermsGroup(),
                            cfg.getPrefixKey(),
                            cfg.getSuffixKey(),
                            cfg.getIcon(),
                            cfg.getInitialRank(),
                            cfg.getTier(),
                            cfg.getWeight(),
                            tree
                    );
                    rdq.getRankRepository().create(created);
                    updateRankRequirements(created, cfg);
                    treeRanks.put(rankId, created);
                    LOGGER.log(Level.INFO, "Created rank: {0}", rankId);
                }
                state.ranks().put(treeId, treeRanks);
            }

            final int total = state.ranks().values().stream().mapToInt(Map::size).sum();
            LOGGER.log(Level.INFO, "Created/updated {0} ranks across {1} trees", new Object[]{total, state.ranks().size()});
        }, executor);
    }

    /**
     * Aligns the connections between ranks and rank trees after all entities have been created.
     *
     * @param state    the aggregated rank system state containing the source configuration
     * @param executor the executor used to update persistence asynchronously
     * @return a future that completes when rank and tree connections are synchronized
     */
    CompletableFuture<Void> establishConnectionsAsync(final @NotNull RankSystemState state,
                                                      final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            for (Map.Entry<String, Map<String, RankSection>> e : state.rankSections().entrySet()) {
                updateRankConnections(e.getKey(), e.getValue());
            }
            for (Map.Entry<String, RRankTree> e : state.rankTrees().entrySet()) {
                updateRankTreeConnections(e.getKey(), state.rankTreeSections().get(e.getKey()));
            }
            LOGGER.info("Connection establishment completed");
        }, executor);
    }

    /**
     * Ensures a rank tree configuration maintains consistency between prerequisites and minimum
     * completion requirements.
     *
     * @param section the configuration section describing the tree
     * @return the updated configuration section with consistent prerequisite data
     */
    private RankTreeSection ensureTreePrereqConsistency(final RankTreeSection section) {
        if (section.getMinimumRankTreesToBeDone() > 0 && section.getPrerequisiteRankTrees().isEmpty()) {
            section.setMinimumRankTreesToBeDone(0);
            LOGGER.info("Adjusted 'minimumRankTreesToBeDone' to 0 due to empty prerequisites");
        }
        return section;
    }

    /**
     * Parses requirements from configuration, persists any missing dependencies, and ensures the
     * rank entity references the refreshed set.
     *
     * @param rank the rank entity whose upgrade requirements should be replaced
     * @param cfg  the configuration describing the requirements
     */
    private void updateRankRequirements(final RRank rank, final RankSection cfg) {
        try {
            cleanupProgress(rank);
            final List<RRankUpgradeRequirement> parsed = requirementFactory.parse(
                    cfg.getRequirements(),
                    base -> "rank '" + rank.getIdentifier() + "'",
                    (req, icon) -> new RRankUpgradeRequirement(rank, req, icon)
            );

            if (parsed.isEmpty()) {
                rank.replaceUpgradeRequirements(Collections.emptyList());
                rdq.getRankRepository().update(rank);
                return;
            }

            final List<RRankUpgradeRequirement> processed = new ArrayList<>(parsed.size());
            for (RRankUpgradeRequirement ur : parsed) {
                RRequirement req = ur.getRequirement();
                if (req.getId() == null) {
                    req = rdq.getRequirementRepository().create(req);
                }
                final RRankUpgradeRequirement persisted = new RRankUpgradeRequirement(null, req, ur.getIcon());
                persisted.setDisplayOrder(ur.getDisplayOrder());
                processed.add(persisted);
            }

            rank.replaceUpgradeRequirements(processed);

            try {
                rdq.getRankRepository().update(rank);
            } catch (Exception updateEx) {
                final RRank fresh = rdq.getRankRepository().findByAttributes(Map.of("identifier", rank.getIdentifier()));
                if (fresh == null) {
                    throw updateEx;
                }
                fresh.replaceUpgradeRequirements(processed);
                rdq.getRankRepository().update(fresh);
            }

            LOGGER.log(Level.INFO, "Updated {0} upgrade requirements for rank: {1}",
                    new Object[]{processed.size(), rank.getIdentifier()});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update requirements for rank: " + rank.getIdentifier(), e);
            throw new RuntimeException("Failed to update rank requirements", e);
        }
    }

    /**
     * Removes orphaned player upgrade progress that references requirements slated for
     * replacement.
     *
     * @param rank the rank whose associated upgrade progress should be cleaned up
     */
    private void cleanupProgress(final RRank rank) {
        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = new HashSet<>(rank.getUpgradeRequirements());
            if (upgradeRequirements.isEmpty()) return;

            for (RRankUpgradeRequirement req : upgradeRequirements) {
                final List<RPlayerRankUpgradeProgress> progresses = rdq.getPlayerRankUpgradeProgressRepository().findListByAttributes(Map.of("upgradeRequirement", req));
                if (!progresses.isEmpty()) {
                    for (RPlayerRankUpgradeProgress progress : progresses) {
                        rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to cleanup player progress for rank: " + rank.getIdentifier(), e);
        }
    }

    /**
     * Updates the previous and next rank relationships for all ranks in the specified tree.
     *
     * @param treeId the tree identifier to use when looking up ranks
     * @param ranks  the configured ranks mapped by their identifiers
     */
    private void updateRankConnections(final String treeId, final Map<String, RankSection> ranks) {
        try {
            final Set<String> valid = ranks.keySet();
            for (Map.Entry<String, RankSection> entry : ranks.entrySet()) {
                final String rankId = entry.getKey();
                final RankSection cfg = entry.getValue();

                final RRank rank = rdq.getRankRepository().findByAttributes(Map.of("identifier", rankId));
                if (rank == null) {
                    LOGGER.log(Level.WARNING, "Rank not found for connection update: {0}", rankId);
                    continue;
                }

                final List<String> prev = cfg.getPreviousRanks().stream().filter(valid::contains).collect(Collectors.toList());
                final List<String> next = cfg.getNextRanks().stream().filter(valid::contains).collect(Collectors.toList());
                rank.setPreviousRanks(prev);
                rank.setNextRanks(next);
                rdq.getRankRepository().update(rank);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update rank connections for tree: " + treeId, e);
            throw new RuntimeException("Failed to update rank connections", e);
        }
    }

    /**
     * Updates prerequisite, unlocked, and connected tree relationships for the referenced tree.
     *
     * @param treeId the identifier for the tree being updated
     * @param cfg    the configuration that defines the tree connections
     */
    private void updateRankTreeConnections(final String treeId, final RankTreeSection cfg) {
        try {
            if (cfg == null) return;

            final RRankTree tree = rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", treeId));
            if (tree == null) {
                LOGGER.log(Level.WARNING, "Rank tree not found for connection update: {0}", treeId);
                return;
            }

            final List<RRankTree> prereqs = cfg.getPrerequisiteRankTrees().stream()
                    .map(id -> rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", id)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            final List<RRankTree> unlocked = cfg.getUnlockedRankTrees().stream()
                    .map(id -> rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", id)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            final List<RRankTree> connected = cfg.getConnectedRankTrees().stream()
                    .map(id -> rdq.getRankTreeRepository().findByAttributes(Map.of("identifier", id)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            tree.setPrerequisiteRankTrees(prereqs);
            tree.setUnlockedRankTrees(unlocked);
            tree.setConnectedRankTrees(connected);
            rdq.getRankTreeRepository().update(tree);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update rank tree connections for: " + treeId, e);
            throw new RuntimeException("Failed to update rank tree connections", e);
        }
    }
}