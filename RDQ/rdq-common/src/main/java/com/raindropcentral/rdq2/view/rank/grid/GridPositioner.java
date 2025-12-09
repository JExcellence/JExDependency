package com.raindropcentral.rdq2.view.rank.grid;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class GridPositioner {

    private final GridPosition rootAnchor;
    private final int gridStep;

    public GridPositioner(final GridPosition rootAnchor, final int gridStep) {
        this.rootAnchor = rootAnchor;
        this.gridStep = gridStep;
    }

    public @NotNull <T> Map<String, GridPosition> layout(
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, List<String>> prevIdsExtractor,
            final @NotNull Function<T, Integer> tierExtractor
    ) {
        final Map<String, GridPosition> positions = new HashMap<>();
        final Set<String> positioned = new HashSet<>();

        final List<String> roots = findRoots(nodeMap, prevIdsExtractor);
        if (roots.isEmpty()) {
            return positions;
        }

        if (roots.size() == 1) {
            layoutSingleRoot(roots.get(0), nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
        } else {
            layoutMultipleRoots(roots, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
        }

        return positions;
    }

    private @NotNull <T> List<String> findRoots(
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> prevIdsExtractor
    ) {
        return nodeMap.entrySet().stream()
                .filter(e -> prevIdsExtractor.apply(e.getValue()).isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private <T> void layoutSingleRoot(
            final @NotNull String rootId,
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, Integer> tierExtractor,
            final @NotNull Map<String, GridPosition> positions,
            final @NotNull Set<String> positioned
    ) {
        positions.put(rootId, rootAnchor);
        positioned.add(rootId);
        layoutChildren(rootId, rootAnchor, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
    }

    private <T> void layoutMultipleRoots(
            final @NotNull List<String> roots,
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, Integer> tierExtractor,
            final @NotNull Map<String, GridPosition> positions,
            final @NotNull Set<String> positioned
    ) {
        final int totalWidth = (roots.size() - 1) * gridStep;
        final int startX = rootAnchor.x() - (totalWidth / 2);

        for (int i = 0; i < roots.size(); i++) {
            final String rootId = roots.get(i);
            final GridPosition position = new GridPosition(startX + (i * gridStep), rootAnchor.y());
            positions.put(rootId, position);
            positioned.add(rootId);
            layoutChildren(rootId, position, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
        }
    }

    private <T> void layoutChildren(
            final @NotNull String parentId,
            final @NotNull GridPosition parentPosition,
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, Integer> tierExtractor,
            final @NotNull Map<String, GridPosition> positions,
            final @NotNull Set<String> positioned
    ) {
        final T parentNode = nodeMap.get(parentId);
        if (parentNode == null) {
            return;
        }

        final List<String> childIds = new ArrayList<>(nextIdsExtractor.apply(parentNode));
        if (childIds.isEmpty()) {
            return;
        }

        childIds.sort(Comparator.comparingInt(id -> tierExtractor.apply(nodeMap.get(id))));

        if (childIds.size() == 1) {
            layoutSingleChild(childIds.get(0), parentPosition, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
        } else {
            layoutMultipleChildren(childIds, parentPosition, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
        }
    }

    private <T> void layoutSingleChild(
            final @NotNull String childId,
            final @NotNull GridPosition parentPosition,
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, Integer> tierExtractor,
            final @NotNull Map<String, GridPosition> positions,
            final @NotNull Set<String> positioned
    ) {
        if (positioned.contains(childId)) {
            return;
        }

        final GridPosition childPosition = parentPosition.offset(0, gridStep);
        positions.put(childId, childPosition);
        positioned.add(childId);
        layoutChildren(childId, childPosition, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
    }

    private <T> void layoutMultipleChildren(
            final @NotNull List<String> childIds,
            final @NotNull GridPosition parentPosition,
            final @NotNull Map<String, T> nodeMap,
            final @NotNull Function<T, List<String>> nextIdsExtractor,
            final @NotNull Function<T, Integer> tierExtractor,
            final @NotNull Map<String, GridPosition> positions,
            final @NotNull Set<String> positioned
    ) {
        final int childY = parentPosition.y() + gridStep;
        final int totalWidth = (childIds.size() - 1) * gridStep;
        final int startX = parentPosition.x() - (totalWidth / 2);

        for (int i = 0; i < childIds.size(); i++) {
            final String childId = childIds.get(i);
            if (positioned.contains(childId)) {
                continue;
            }

            final GridPosition childPosition = new GridPosition(startX + (i * gridStep), childY);
            positions.put(childId, childPosition);
            positioned.add(childId);
            layoutChildren(childId, childPosition, nodeMap, nextIdsExtractor, tierExtractor, positions, positioned);
        }
    }
}