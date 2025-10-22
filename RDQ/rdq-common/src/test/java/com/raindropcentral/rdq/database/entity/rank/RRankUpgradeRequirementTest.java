package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RRankUpgradeRequirementTest {

    @Test
    void constructorHandlesOptionalRankAndRegistersWhenProvided() {
        final AbstractRequirement orphanPayload = mock(AbstractRequirement.class);
        final IconSection orphanRequirementIcon = mock(IconSection.class);
        final RRequirement orphanRequirement = new RRequirement(orphanPayload, orphanRequirementIcon);
        final IconSection orphanDisplayIcon = mock(IconSection.class);

        final RRankUpgradeRequirement orphaned = new RRankUpgradeRequirement(null, orphanRequirement, orphanDisplayIcon);

        assertNull(orphaned.getRank(), "Constructor should allow creating an unattached requirement");
        assertSame(orphanRequirement, orphaned.getRequirement(), "Constructor should store the requirement reference");
        assertSame(orphanDisplayIcon, orphaned.getIcon(), "Constructor should store the icon reference");
        assertEquals(0, orphaned.getDisplayOrder(), "Constructor should default display order to zero");

        final AbstractRequirement attachedPayload = mock(AbstractRequirement.class);
        final IconSection attachedRequirementIcon = mock(IconSection.class);
        final RRequirement attachedRequirement = new RRequirement(attachedPayload, attachedRequirementIcon);
        final IconSection attachedDisplayIcon = mock(IconSection.class);
        final IconSection rankIcon = mock(IconSection.class);
        final RRank rank = new RRank(
            "rank.identifier",
            "rank.display",
            "rank.description",
            "rank.group",
            "rank.prefix",
            "rank.suffix",
            rankIcon,
            false,
            1,
            10,
            null
        );

        final RRankUpgradeRequirement attached = new RRankUpgradeRequirement(rank, attachedRequirement, attachedDisplayIcon);

        assertSame(rank, attached.getRank(), "Constructor should register the owning rank");
        assertTrue(rank.getUpgradeRequirements().contains(attached),
            "Constructor should register the requirement with the provided rank");
    }

    @Test
    void settersUpdateStateAndEnforceNullChecks() {
        final AbstractRequirement initialPayload = mock(AbstractRequirement.class);
        final IconSection initialRequirementIcon = mock(IconSection.class);
        final RRequirement initialRequirement = new RRequirement(initialPayload, initialRequirementIcon);
        final IconSection initialDisplayIcon = mock(IconSection.class);
        final RRankUpgradeRequirement requirement = new RRankUpgradeRequirement(null, initialRequirement, initialDisplayIcon);

        final AbstractRequirement replacementPayload = mock(AbstractRequirement.class);
        final IconSection replacementRequirementIcon = mock(IconSection.class);
        final RRequirement replacementRequirement = new RRequirement(replacementPayload, replacementRequirementIcon);
        final IconSection replacementDisplayIcon = mock(IconSection.class);

        requirement.setRequirement(replacementRequirement);
        requirement.setIcon(replacementDisplayIcon);
        requirement.setDisplayOrder(5);

        assertSame(replacementRequirement, requirement.getRequirement(),
            "setRequirement should replace the stored requirement");
        assertSame(replacementDisplayIcon, requirement.getIcon(),
            "setIcon should replace the stored icon");
        assertEquals(5, requirement.getDisplayOrder(),
            "setDisplayOrder should update the ordering");

        final NullPointerException requirementException = assertThrows(NullPointerException.class,
            () -> requirement.setRequirement(null),
            "setRequirement should reject null assignments");
        assertEquals("requirement cannot be null", requirementException.getMessage());

        final NullPointerException iconException = assertThrows(NullPointerException.class,
            () -> requirement.setIcon(null),
            "setIcon should reject null assignments");
        assertEquals("icon cannot be null", iconException.getMessage());
    }

    @Test
    void playerFacingHelpersDelegateToRequirement() {
        final RRequirement requirement = mock(RRequirement.class);
        final IconSection icon = mock(IconSection.class);
        final RRankUpgradeRequirement upgradeRequirement = new RRankUpgradeRequirement(null, requirement, icon);
        final Player player = mock(Player.class);

        when(requirement.isMet(player)).thenReturn(true);
        when(requirement.calculateProgress(player)).thenReturn(0.75D);

        assertTrue(upgradeRequirement.isMet(player), "isMet should return the delegated result");
        assertEquals(0.75D, upgradeRequirement.calculateProgress(player),
            "calculateProgress should return the delegated result");
        upgradeRequirement.consume(player);

        verify(requirement).isMet(player);
        verify(requirement).calculateProgress(player);
        verify(requirement).consume(player);
    }
}
