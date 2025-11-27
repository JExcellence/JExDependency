package com.raindropcentral.rdq.command.completion;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.Bounty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BountyTargetCompleter {

    private final RDQCore core;
    private final List<String> cachedTargetNames = new CopyOnWriteArrayList<>();
    private volatile long lastRefresh = 0;
    private static final long CACHE_TTL_MS = 10_000;

    public BountyTargetCompleter(final @NotNull RDQCore core) {
        this.core = core;
    }

    public List<String> complete(final @NotNull String input) {
        refreshCacheIfNeeded();
        var lowerInput = input.toLowerCase();
        return cachedTargetNames.stream()
                .filter(name -> name.toLowerCase().startsWith(lowerInput))
                .sorted()
                .toList();
    }

    public List<String> completeWithBountyIds(final @NotNull String input) {
        refreshCacheIfNeeded();
        var lowerInput = input.toLowerCase();
        try {
            var bounties = core.getBountyService().getActiveBounties().join();
            return bounties.stream()
                    .map(b -> b.id() + ":" + b.target().name())
                    .map(String::valueOf)
                    .filter(s -> s.toLowerCase().startsWith(lowerInput))
                    .toList();
        } catch (Exception e) {
            return cachedTargetNames.stream()
                    .filter(name -> name.toLowerCase().startsWith(lowerInput))
                    .toList();
        }
    }

    private void refreshCacheIfNeeded() {
        var now = System.currentTimeMillis();
        if (now - lastRefresh < CACHE_TTL_MS && !cachedTargetNames.isEmpty()) {
            return;
        }

        try {
            var bounties = core.getBountyService().getActiveBounties().join();
            var names = bounties.stream()
                    .map(b -> b.target().name())
                    .distinct()
                    .sorted()
                    .toList();

            cachedTargetNames.clear();
            cachedTargetNames.addAll(names);
            lastRefresh = now;
        } catch (Exception ignored) {
        }
    }

    public void invalidateCache() {
        cachedTargetNames.clear();
        lastRefresh = 0;
    }
}
