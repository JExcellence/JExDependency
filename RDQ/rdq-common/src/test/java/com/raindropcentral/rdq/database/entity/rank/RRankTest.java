package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RRankTest {

    @Test
    void itPopulatesMandatoryFieldsAndDefaultsOptionalFlags() {
        final IconSection rankIcon = createIcon();
        final RRankTree rankTree = new RRankTree(
            "tree.starters",
            "tree.starters.name",
            "tree.starters.description",
            createIcon(),
            1,
            0,
            true,
            false
        );

        final RRank rank = new RRank(
            "rank.initiate",
            "rank.initiate.name",
            "rank.initiate.description",
            "group.initiate",
            "rank.initiate.prefix",
            "rank.initiate.suffix",
            rankIcon,
            true,
            1,
            5,
            rankTree
        );

        assertEquals("rank.initiate", rank.getIdentifier(), "Constructor should persist the identifier");
        assertEquals("rank.initiate.name", rank.getDisplayNameKey(), "Constructor should persist the display name key");
        assertEquals("rank.initiate.description", rank.getDescriptionKey(), "Constructor should persist the description key");
        assertEquals("group.initiate", rank.getAssignedLuckPermsGroup(), "Constructor should persist the assigned group");
        assertEquals("rank.initiate.prefix", rank.getPrefixKey(), "Constructor should persist the prefix key");
        assertEquals("rank.initiate.suffix", rank.getSuffixKey(), "Constructor should persist the suffix key");
        assertSame(rankIcon, rank.getIcon(), "Constructor should retain the provided icon instance");
        assertTrue(rank.isInitialRank(), "Constructor should propagate the initial rank flag");
        assertFalse(rank.isFinalRank(), "Constructor should default the final rank flag to false");
        assertTrue(rank.isEnabled(), "Constructor should default ranks to enabled");
        assertEquals(1, rank.getTier(), "Constructor should persist the tier value");
        assertEquals(5, rank.getWeight(), "Constructor should persist the weight value");
        assertSame(rankTree, rank.getRankTree(), "Constructor should retain the provided rank tree reference");
        assertTrue(rank.getPreviousRanks().isEmpty(), "Constructor should initialise previous ranks to an empty list");
        assertTrue(rank.getNextRanks().isEmpty(), "Constructor should initialise next ranks to an empty list");
        assertTrue(rank.getUpgradeRequirements().isEmpty(), "Constructor should initialise upgrade requirements to an empty set");
    }

    @Test
    void itProvidesImmutableRankConnections() {
        final RRank rank = createRank("rank.challenger");

        final List<String> previousRanks = new ArrayList<>(Arrays.asList("rank.initiate", "rank.rookie"));
        rank.setPreviousRanks(previousRanks);

        assertIterableEquals(List.of("rank.initiate", "rank.rookie"), rank.getPreviousRanks(),
            "setPreviousRanks should persist the provided identifiers");
        assertThrows(UnsupportedOperationException.class, () -> rank.getPreviousRanks().add("rank.extra"),
            "getPreviousRanks should expose an immutable view");

        previousRanks.add("rank.extra");
        assertIterableEquals(List.of("rank.initiate", "rank.rookie"), rank.getPreviousRanks(),
            "Mutating the source list after setPreviousRanks should not affect the stored identifiers");

        final List<String> nextRanks = new ArrayList<>(List.of("rank.elite"));
        rank.setNextRanks(nextRanks);

        assertIterableEquals(List.of("rank.elite"), rank.getNextRanks(),
            "setNextRanks should persist the provided identifiers");
        assertThrows(UnsupportedOperationException.class, () -> rank.getNextRanks().add("rank.legend"),
            "getNextRanks should expose an immutable view");

        nextRanks.add("rank.legend");
        assertIterableEquals(List.of("rank.elite"), rank.getNextRanks(),
            "Mutating the source list after setNextRanks should not affect the stored identifiers");

        rank.setNextRanks(List.of("rank.master"));
        assertIterableEquals(List.of("rank.master"), rank.getNextRanks(),
            "setNextRanks should replace the previously stored identifiers");
    }

    @Test
    void itMaintainsBidirectionalUpgradeRequirementAssociations() {
        final RRank rank = createRank("rank.hero");
        final RRankUpgradeRequirement requirement = new RRankUpgradeRequirement(
            null,
            createRequirement(),
            createIcon()
        );

        assertTrue(rank.addUpgradeRequirement(requirement), "addUpgradeRequirement should report newly added associations");
        assertTrue(rank.getUpgradeRequirements().contains(requirement),
            "addUpgradeRequirement should add the requirement to the rank");
        assertSame(rank, requirement.getRank(),
            "addUpgradeRequirement should update the reverse association on the requirement");

        final Set<RRankUpgradeRequirement> view = rank.getUpgradeRequirements();
        assertThrows(UnsupportedOperationException.class, () -> view.add(requirement),
            "getUpgradeRequirements should expose an immutable view");
        assertFalse(rank.addUpgradeRequirement(requirement),
            "addUpgradeRequirement should reject duplicate associations");

        assertTrue(rank.removeUpgradeRequirement(requirement),
            "removeUpgradeRequirement should report when an association is removed");
        assertNull(requirement.getRank(),
            "removeUpgradeRequirement should clear the reverse association on the requirement");
        assertFalse(rank.getUpgradeRequirements().contains(requirement),
            "removeUpgradeRequirement should remove the requirement from the rank");
        assertFalse(rank.removeUpgradeRequirement(requirement),
            "removeUpgradeRequirement should report when an association is absent");
    }

    private static RRank createRank(final String identifier) {
        return new RRank(
            identifier,
            identifier + ".name",
            identifier + ".description",
            "group." + identifier,
            identifier + ".prefix",
            identifier + ".suffix",
            createIcon(),
            false,
            2,
            3,
            null
        );
    }

    private static IconSection createIcon() {
        return new IconSection(new EvaluationEnvironmentBuilder());
    }

    private static RRequirement createRequirement() {
        return new RRequirement(new RecordingRequirement(), createIcon());
    }

    private static final class RecordingRequirement extends AbstractRequirement {

        private RecordingRequirement() {
            super(Type.CUSTOM);
        }

        @Override
        public boolean isMet(final @NotNull Player player) {
            return false;
        }

        @Override
        public double calculateProgress(final @NotNull Player player) {
            return 0.0D;
        }

        @Override
        public void consume(final @NotNull Player player) {
            // no-op for tests
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return "requirement.recording.description";
        }
    }
}
