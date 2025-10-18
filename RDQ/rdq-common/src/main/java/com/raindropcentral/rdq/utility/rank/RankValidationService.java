package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RankValidationService {

    private static final Logger LOGGER = CentralLogger.getLogger(RankValidationService.class.getName());

    CompletableFuture<Void> validateConfigurationsAsync(final @NotNull RankSystemState state,
                                                        final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> validateConfigurations(state), executor);
    }

    CompletableFuture<Void> validateSystemAsync(final @NotNull RankSystemState state,
                                                final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> validateSystem(state), executor);
    }

    private void validateConfigurations(final RankSystemState state) {
        final List<String> errors = new ArrayList<>();

        for (Map.Entry<String, RankTreeSection> entry : state.rankTreeSections().entrySet()) {
            validateTreeReferences(entry.getKey(), entry.getValue(), state.rankTreeSections().keySet(), errors);
        }
        for (Map.Entry<String, Map<String, RankSection>> treeEntry : state.rankSections().entrySet()) {
            validateRankReferences(treeEntry.getKey(), treeEntry.getValue(), errors);
        }

        if (!errors.isEmpty()) {
            errors.forEach(err -> LOGGER.log(Level.SEVERE, "Configuration validation error: {0}", err));
            throw new IllegalStateException("Configuration validation failed");
        }

        LOGGER.info("Configuration validation completed");
    }

    private void validateSystem(final RankSystemState state) {
        final List<String> errors = new ArrayList<>();
        for (String treeId : state.rankTreeSections().keySet()) {
            if (hasCycle(treeId, state.rankTreeSections(), new HashSet<>(), new HashSet<>())) {
                errors.add("Cycle detected in prerequisites starting at tree: " + treeId);
            }
        }
        if (!errors.isEmpty()) {
            errors.forEach(err -> LOGGER.log(Level.SEVERE, "System validation error: {0}", err));
            throw new IllegalStateException("System validation failed");
        }
        LOGGER.info("System validation completed");
    }

    private void validateTreeReferences(final String treeId,
                                        final RankTreeSection config,
                                        final Set<String> validTreeIds,
                                        final List<String> errors) {
        for (String pre : config.getPrerequisiteRankTrees()) {
            if (!validTreeIds.contains(pre)) errors.add("Tree " + treeId + " missing prerequisite: " + pre);
        }
        for (String unlocked : config.getUnlockedRankTrees()) {
            if (!validTreeIds.contains(unlocked)) errors.add("Tree " + treeId + " missing unlocked tree: " + unlocked);
        }
        for (String connected : config.getConnectedRankTrees()) {
            if (!validTreeIds.contains(connected))
                errors.add("Tree " + treeId + " missing connected tree: " + connected);
        }
    }

    private void validateRankReferences(final String treeId,
                                        final Map<String, RankSection> ranks,
                                        final List<String> errors) {
        final Set<String> valid = ranks.keySet();
        for (Map.Entry<String, RankSection> e : ranks.entrySet()) {
            final String rankId = e.getKey();
            final RankSection cfg = e.getValue();
            for (String prev : cfg.getPreviousRanks()) {
                if (!valid.contains(prev))
                    errors.add("Rank " + rankId + " in tree " + treeId + " missing previous: " + prev);
            }
            for (String next : cfg.getNextRanks()) {
                if (!valid.contains(next))
                    errors.add("Rank " + rankId + " in tree " + treeId + " missing next: " + next);
            }
        }
    }

    private boolean hasCycle(final String treeId,
                             final Map<String, RankTreeSection> sections,
                             final Set<String> visited,
                             final Set<String> stack) {
        if (stack.contains(treeId)) return true;
        if (visited.contains(treeId)) return false;
        visited.add(treeId);
        stack.add(treeId);
        final RankTreeSection sec = sections.get(treeId);
        if (sec != null) {
            for (String pre : sec.getPrerequisiteRankTrees()) {
                if (hasCycle(pre, sections, visited, stack)) return true;
            }
        }
        stack.remove(treeId);
        return false;
    }
}