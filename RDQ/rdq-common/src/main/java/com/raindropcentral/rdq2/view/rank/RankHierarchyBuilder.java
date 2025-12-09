/*
package com.raindropcentral.rdq2.view.rank;

import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankTree;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankHierarchyBuilder {

    private static final Logger LOGGER = CentralLogger.getLogger(RankHierarchyBuilder.class.getName());

    public @NotNull Map<String, RankNode> buildHierarchy(final @NotNull RRankTree rankTree) {
        final Map<String, RankNode> nodeMap = new HashMap<>();

        try {
            for (final RRank rank : rankTree.getRanks()) {
                nodeMap.put(rank.getIdentifier(), new RankNode(rank));
            }

            for (final RRank rank : rankTree.getRanks()) {
                final RankNode currentNode = nodeMap.get(rank.getIdentifier());
                if (currentNode == null) {
                    continue;
                }

                for (final String nextRankId : rank.getNextRanks()) {
                    final RankNode childNode = nodeMap.get(nextRankId);
                    if (childNode != null) {
                        currentNode.addChild(childNode);
                        childNode.addParent(currentNode);
                    }
                }

                for (final String prevRankId : rank.getPreviousRanks()) {
                    final RankNode parentNode = nodeMap.get(prevRankId);
                    if (parentNode != null && !currentNode.getParents().contains(parentNode)) {
                        currentNode.addParent(parentNode);
                        if (!parentNode.getChildren().contains(currentNode)) {
                            parentNode.addChild(currentNode);
                        }
                    }
                }
            }

            LOGGER.log(Level.FINE, "Built hierarchy with " + nodeMap.size() + " nodes");
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error building rank node hierarchy", exception);
        }

        return nodeMap;
    }
}*/
