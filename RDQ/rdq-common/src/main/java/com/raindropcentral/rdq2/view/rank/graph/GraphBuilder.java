package com.raindropcentral.rdq2.view.rank.graph;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class GraphBuilder<T> {

    public @NotNull Map<String, GraphNode<T>> build(
            final @NotNull Map<String, T> modelMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, List<String>> prevIdsExtractor
    ) {
        final Map<String, GraphNode<T>> nodeMap = new HashMap<>();

        for (final Map.Entry<String, T> entry : modelMap.entrySet()) {
            nodeMap.put(entry.getKey(), new GraphNode<>(entry.getValue()));
        }

        for (final Map.Entry<String, T> entry : modelMap.entrySet()) {
            final String nodeId = entry.getKey();
            final T model = entry.getValue();
            final GraphNode<T> node = nodeMap.get(nodeId);

            if (node == null) {
                continue;
            }

            for (final String nextId : nextIdsExtractor.apply(model)) {
                final GraphNode<T> nextNode = nodeMap.get(nextId);
                if (nextNode != null) {
                    node.addChild(nextNode);
                }
            }

            for (final String prevId : prevIdsExtractor.apply(model)) {
                final GraphNode<T> prevNode = nodeMap.get(prevId);
                if (prevNode != null) {
                    node.addParent(prevNode);
                }
            }
        }

        return nodeMap;
    }
}