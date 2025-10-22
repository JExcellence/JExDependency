package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RRequirementTest {

    @Test
    void constructorRequiresNonNullRequirement() {
        final IconSection icon = mock(IconSection.class);

        final NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RRequirement(null, icon)
        );

        assertEquals("requirement cannot be null", exception.getMessage());
    }

    @Test
    void constructorRequiresNonNullIcon() {
        final AbstractRequirement requirement = mock(AbstractRequirement.class);

        final NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new RRequirement(requirement, null)
        );

        assertEquals("icon cannot be null", exception.getMessage());
    }

    @Test
    void gettersReturnInjectedValues() {
        final AbstractRequirement requirement = mock(AbstractRequirement.class);
        final IconSection icon = mock(IconSection.class);

        final RRequirement entity = new RRequirement(requirement, icon);

        assertSame(requirement, entity.getRequirement());
        assertSame(icon, entity.getShowcase());
    }

    @Test
    void settersUpdateDelegatedValues() {
        final AbstractRequirement initialRequirement = mock(AbstractRequirement.class);
        final IconSection initialIcon = mock(IconSection.class);
        final RRequirement entity = new RRequirement(initialRequirement, initialIcon);

        final AbstractRequirement newRequirement = mock(AbstractRequirement.class);
        final IconSection newIcon = mock(IconSection.class);

        entity.setRequirement(newRequirement);
        entity.setShowcase(newIcon);

        assertSame(newRequirement, entity.getRequirement());
        assertSame(newIcon, entity.getShowcase());
    }

    @Test
    void isMetDelegatesToUnderlyingRequirement() {
        final AbstractRequirement requirement = mock(AbstractRequirement.class);
        final IconSection icon = mock(IconSection.class);
        final RRequirement entity = new RRequirement(requirement, icon);
        final Player player = mock(Player.class);

        when(requirement.isMet(player)).thenReturn(true);

        assertTrue(entity.isMet(player));
        verify(requirement).isMet(player);
    }

    @Test
    void calculateProgressDelegatesToUnderlyingRequirement() {
        final AbstractRequirement requirement = mock(AbstractRequirement.class);
        final IconSection icon = mock(IconSection.class);
        final RRequirement entity = new RRequirement(requirement, icon);
        final Player player = mock(Player.class);

        when(requirement.calculateProgress(player)).thenReturn(0.42D);

        assertEquals(0.42D, entity.calculateProgress(player));
        verify(requirement).calculateProgress(player);
    }

    @Test
    void consumeDelegatesToUnderlyingRequirement() {
        final AbstractRequirement requirement = mock(AbstractRequirement.class);
        final IconSection icon = mock(IconSection.class);
        final RRequirement entity = new RRequirement(requirement, icon);
        final Player player = mock(Player.class);

        entity.consume(player);

        verify(requirement).consume(player);
    }
}
