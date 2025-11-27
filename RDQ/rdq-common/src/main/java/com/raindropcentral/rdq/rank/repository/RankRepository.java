package com.raindropcentral.rdq.rank.repository;

import com.raindropcentral.rdq.rank.Rank;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RankRepository {

    private final Map<String, Rank> ranks = new ConcurrentHashMap<>();

    public void register(@NotNull Rank rank) {
        ranks.put(rank.id(), rank);
    }

    public void registerAll(@NotNull List<Rank> ranksToRegister) {
        ranksToRegister.forEach(this::register);
    }

    @NotNull
    public Optional<Rank> findById(@NotNull String id) {
        return Optional.ofNullable(ranks.get(id));
    }

    @NotNull
    public List<Rank> findAll() {
        return List.copyOf(ranks.values());
    }

    @NotNull
    public List<Rank> findByTreeId(@NotNull String treeId) {
        return ranks.values().stream()
            .filter(r -> r.treeId().equals(treeId))
            .sorted((a, b) -> Integer.compare(a.tier(), b.tier()))
            .toList();
    }

    @NotNull
    public List<Rank> findEnabledByTreeId(@NotNull String treeId) {
        return ranks.values().stream()
            .filter(r -> r.treeId().equals(treeId) && r.enabled())
            .sorted((a, b) -> Integer.compare(a.tier(), b.tier()))
            .toList();
    }

    @NotNull
    public Optional<Rank> findNextRank(@NotNull String currentRankId) {
        return findById(currentRankId)
            .flatMap(current -> findEnabledByTreeId(current.treeId()).stream()
                .filter(r -> r.tier() > current.tier())
                .findFirst());
    }

    @NotNull
    public Optional<Rank> findFirstRankInTree(@NotNull String treeId) {
        return findEnabledByTreeId(treeId).stream().findFirst();
    }


    public boolean exists(@NotNull String id) {
        return ranks.containsKey(id);
    }

    public int count() {
        return ranks.size();
    }

    public void clear() {
        ranks.clear();
    }

    public void reload(@NotNull List<Rank> newRanks) {
        ranks.clear();
        registerAll(newRanks);
    }
}
