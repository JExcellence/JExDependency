package com.raindropcentral.rdq2.view.rank.graph;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphNode<T> {

    private final T value;
    private final List<GraphNode<T>> children;
    private final List<GraphNode<T>> parents;

    public GraphNode(final @NotNull T value) {
        this.value = value;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    public @NotNull T getValue() {
        return value;
    }

    public @NotNull List<GraphNode<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public @NotNull List<GraphNode<T>> getParents() {
        return Collections.unmodifiableList(parents);
    }

    public void addChild(final @NotNull GraphNode<T> child) {
        if (!children.contains(child)) {
            children.add(child);
        }
        if (!child.parents.contains(this)) {
            child.parents.add(this);
        }
    }

    public void addParent(final @NotNull GraphNode<T> parent) {
        if (!parents.contains(parent)) {
            parents.add(parent);
        }
        if (!parent.children.contains(this)) {
            parent.children.add(this);
        }
    }

    public boolean isRoot() {
        return parents.isEmpty();
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
        return "GraphNode{value=" + value + ", children=" + children.size() + ", parents=" + parents.size() + '}';
    }
}