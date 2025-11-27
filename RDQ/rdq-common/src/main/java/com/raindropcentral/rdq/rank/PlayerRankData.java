package com.raindropcentral.rdq.rank;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PlayerRankData(
    @NotNull UUID playerId,
    @NotNull List<ActivePath> activePaths,
    @NotNull Map<String, Instant> unlockedRanks
) {
    public PlayerRankData {
        Objects.requireNonNull(playerId, "playerId");
        activePaths = activePaths != null ? List.copyOf(activePaths) : List.of();
        unlockedRanks = unlockedRanks != null ? Map.copyOf(unlockedRanks) : Map.of();
    }

    public record ActivePath(
        @NotNull String treeId,
        @NotNull String currentRankId,
        @NotNull Instant startedAt
    ) {
        public ActivePath {
            Objects.requireNonNull(treeId, "treeId");
            Objects.requireNonNull(currentRankId, "currentRankId");
            Objects.requireNonNull(startedAt, "startedAt");
        }
    }

    public boolean hasUnlockedRank(@NotNull String rankId) {
        return unlockedRanks.containsKey(rankId);
    }

    public Optional<Instant> getUnlockTime(@NotNull String rankId) {
        return Optional.ofNullable(unlockedRanks.get(rankId));
    }

    public boolean hasActivePath(@NotNull String treeId) {
        return activePaths.stream().anyMatch(p -> p.treeId().equals(treeId));
    }

    public Optional<ActivePath> getActivePath(@NotNull String treeId) {
        return activePaths.stream()
            .filter(p -> p.treeId().equals(treeId))
            .findFirst();
    }

    public int activePathCount() {
        return activePaths.size();
    }

    public int unlockedRankCount() {
        return unlockedRanks.size();
    }
}
