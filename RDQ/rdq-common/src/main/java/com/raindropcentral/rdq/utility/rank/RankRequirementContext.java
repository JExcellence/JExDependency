package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankRequirementContext {

    private RankRequirementContext() {
    }

    public static @NotNull CompletableFuture<Void> applyAsync(final @NotNull RankSection rankSection,
                                                              final @NotNull String treeId,
                                                              final @NotNull String rankId,
                                                              final @NotNull Logger logger) {
        return applyAsync(rankSection, treeId, rankId, logger, ForkJoinPool.commonPool());
    }

    public static @NotNull CompletableFuture<Void> applyAsync(final @NotNull RankSection rankSection,
                                                              final @NotNull String treeId,
                                                              final @NotNull String rankId,
                                                              final @NotNull Logger logger,
                                                              final @NotNull Executor executor) {
        Objects.requireNonNull(rankSection, "rankSection");
        Objects.requireNonNull(treeId, "treeId");
        Objects.requireNonNull(rankId, "rankId");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(executor, "executor");

        return CompletableFuture.runAsync(() -> {
            final Map<String, BaseRequirementSection> requirements = rankSection.getRequirements();
            if (requirements == null || requirements.isEmpty()) {
                return;
            }
            requirements.forEach((key, section) -> {
                if (section == null) {
                    return;
                }
                section.setContext(treeId, rankId, key);
                try {
                    section.afterParsing(new ArrayList<>());
                } catch (Exception e) {
                    logger.log(
                            Level.WARNING,
                            "Failed to process requirement {0} for rank {1} in tree {2}: {3}",
                            new Object[]{key, rankId, treeId, e.getMessage()}
                    );
                }
            });
        }, executor);
    }

    public static void apply(final @NotNull RankSection rankSection,
                             final @NotNull String treeId,
                             final @NotNull String rankId,
                             final @NotNull Logger logger) {
        Objects.requireNonNull(rankSection, "rankSection");
        Objects.requireNonNull(treeId, "treeId");
        Objects.requireNonNull(rankId, "rankId");
        Objects.requireNonNull(logger, "logger");
        final Map<String, BaseRequirementSection> requirements = rankSection.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        requirements.forEach((key, section) -> {
            if (section == null) {
                return;
            }
            section.setContext(treeId, rankId, key);
            try {
                section.afterParsing(new ArrayList<>());
            } catch (Exception e) {
                logger.log(
                        Level.WARNING,
                        "Failed to process requirement {0} for rank {1} in tree {2}: {3}",
                        new Object[]{key, rankId, treeId, e.getMessage()}
                );
            }
        });
    }
}