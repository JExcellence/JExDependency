package com.raindropcentral.rdq.command.completion;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankTree;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RankNameCompleter {

    private final RDQCore core;
    private final List<String> cachedRankIds = new CopyOnWriteArrayList<>();
    private volatile long lastRefresh = 0;
    private static final long CACHE_TTL_MS = 30_000;

    public RankNameCompleter(final @NotNull RDQCore core) {
        this.core = core;
    }

    public List<String> complete(final @NotNull String input) {
        refreshCacheIfNeeded();
        var lowerInput = input.toLowerCase();
        return cachedRankIds.stream()
                .filter(id -> id.toLowerCase().startsWith(lowerInput))
                .sorted()
                .toList();
    }

    public List<String> completeForTree(final @NotNull String input, final @NotNull String treeId) {
        refreshCacheIfNeeded();
        var lowerInput = input.toLowerCase();
        return cachedRankIds.stream()
                .filter(id -> id.startsWith(treeId + "_"))
                .filter(id -> id.toLowerCase().startsWith(lowerInput))
                .sorted()
                .toList();
    }

    private void refreshCacheIfNeeded() {
        var now = System.currentTimeMillis();
        if (now - lastRefresh < CACHE_TTL_MS && !cachedRankIds.isEmpty()) {
            return;
        }

        try {
            var trees = core.getRankService().getAvailableRankTrees().join();
            var ids = trees.stream()
                    .flatMap(tree -> tree.ranks().stream())
                    .map(Rank::id)
                    .sorted()
                    .toList();

            cachedRankIds.clear();
            cachedRankIds.addAll(ids);
            lastRefresh = now;
        } catch (Exception ignored) {
        }
    }

    public void invalidateCache() {
        cachedRankIds.clear();
        lastRefresh = 0;
    }
}
