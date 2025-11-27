package com.raindropcentral.rdq.rank.repository;

import com.raindropcentral.rdq.rank.RankTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RankTreeRepository {

    private final Map<String, RankTree> trees = new ConcurrentHashMap<>();

    public void register(@NotNull RankTree tree) {
        trees.put(tree.id(), tree);
    }

    public void registerAll(@NotNull List<RankTree> treesToRegister) {
        treesToRegister.forEach(this::register);
    }

    @NotNull
    public Optional<RankTree> findById(@NotNull String id) {
        return Optional.ofNullable(trees.get(id));
    }

    @NotNull
    public List<RankTree> findAll() {
        return List.copyOf(trees.values());
    }

    @NotNull
    public List<RankTree> findAllEnabled() {
        return trees.values().stream()
            .filter(RankTree::enabled)
            .sorted((a, b) -> Integer.compare(a.displayOrder(), b.displayOrder()))
            .toList();
    }

    public boolean exists(@NotNull String id) {
        return trees.containsKey(id);
    }

    public int count() {
        return trees.size();
    }

    public void clear() {
        trees.clear();
    }

    public void reload(@NotNull List<RankTree> newTrees) {
        trees.clear();
        registerAll(newTrees);
    }
}
