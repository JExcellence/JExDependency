package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RankNode {

    private final RRank rank;
    private final List<RankNode> children;
    private final List<RankNode> parents;

    public RankNode(final @NotNull RRank rank) {
        this.rank = rank;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    public @NotNull RRank getRank() {
        return rank;
    }

    public @NotNull List<RankNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public @NotNull List<RankNode> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public void addChild(final @NotNull RankNode child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    public void addParent(final @NotNull RankNode parent) {
        if (!parents.contains(parent)) {
            parents.add(parent);
        }
    }

    public boolean isRoot() {
        return parents.isEmpty() || rank.isInitialRank();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public int getChildCount() {
        return children.size();
    }

    public int getParentCount() {
        return parents.size();
    }

    @Override
    public String toString() {
        return "RankNode{rank=" + rank.getIdentifier() + ", children=" + children.size() + ", parents=" + parents.size() + '}';
    }
}