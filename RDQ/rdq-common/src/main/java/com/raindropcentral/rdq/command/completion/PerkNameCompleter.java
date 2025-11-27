package com.raindropcentral.rdq.command.completion;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.perk.Perk;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PerkNameCompleter {

    private final RDQCore core;
    private final List<String> cachedPerkIds = new CopyOnWriteArrayList<>();
    private volatile long lastRefresh = 0;
    private static final long CACHE_TTL_MS = 30_000;

    public PerkNameCompleter(final @NotNull RDQCore core) {
        this.core = core;
    }

    public List<String> complete(final @NotNull String input) {
        refreshCacheIfNeeded();
        var lowerInput = input.toLowerCase();
        return cachedPerkIds.stream()
                .filter(id -> id.toLowerCase().startsWith(lowerInput))
                .sorted()
                .toList();
    }

    public List<String> completeByCategory(final @NotNull String input, final @NotNull String category) {
        refreshCacheIfNeeded();
        var lowerInput = input.toLowerCase();
        try {
            var perks = core.getPerkService().getAllPerks().join();
            return perks.stream()
                    .filter(p -> p.category().equalsIgnoreCase(category))
                    .map(Perk::id)
                    .filter(id -> id.toLowerCase().startsWith(lowerInput))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return cachedPerkIds.stream()
                    .filter(id -> id.toLowerCase().startsWith(lowerInput))
                    .toList();
        }
    }

    private void refreshCacheIfNeeded() {
        var now = System.currentTimeMillis();
        if (now - lastRefresh < CACHE_TTL_MS && !cachedPerkIds.isEmpty()) {
            return;
        }

        try {
            var perks = core.getPerkService().getAllPerks().join();
            var ids = perks.stream()
                    .map(Perk::id)
                    .sorted()
                    .toList();

            cachedPerkIds.clear();
            cachedPerkIds.addAll(ids);
            lastRefresh = now;
        } catch (Exception ignored) {
        }
    }

    public void invalidateCache() {
        cachedPerkIds.clear();
        lastRefresh = 0;
    }
}
