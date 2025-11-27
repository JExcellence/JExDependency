package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.view.rank.grid.GridPosition;
import com.raindropcentral.rdq.view.rank.grid.GridPositioner;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankPositionCalculator {

    private static final Logger LOGGER = CentralLogger.getLogger(RankPositionCalculator.class.getName());
    private static final GridPosition ROOT_ANCHOR = new GridPosition(3, 2);
    private static final int GRID_STEP = 5;

    private final GridPositioner positioner;

    public RankPositionCalculator() {
        this.positioner = new GridPositioner(ROOT_ANCHOR, GRID_STEP);
    }

    public @NotNull Map<String, GridPosition> calculatePositions(final @NotNull Map<String, RankNode> nodeHierarchy) {
        try {
            return positioner.layout(
                    nodeHierarchy,
                    node -> node.getChildren().stream().map(child -> child.getRank().getIdentifier()).toList(),
                    node -> node.getParents().stream().map(parent -> parent.getRank().getIdentifier()).toList(),
                    node -> node.getRank().getTier()
            );
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error calculating rank world positions", exception);
            return Map.of();
        }
    }
}