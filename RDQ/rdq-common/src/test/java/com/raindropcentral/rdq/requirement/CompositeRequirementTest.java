package com.raindropcentral.rdq.requirement;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeRequirementTest {

    private static final double DELTA = 1.0E-6;

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("CompositeTester");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isMetRequiresAllRequirementsForAndOperator() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(true, 1.0D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(true, 0.5D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(false, 0.25D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.AND
        );

        assertFalse(composite.isMet(this.player));

        third.setMet(true);

        assertTrue(composite.isMet(this.player));
    }

    @Test
    void isMetAcceptsAnyRequirementForOrOperator() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(false, 0.2D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.1D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(true, 0.9D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.OR
        );

        assertTrue(composite.isMet(this.player));

        third.setMet(false);

        assertFalse(composite.isMet(this.player));
    }

    @Test
    void isMetRequiresMinimumSatisfiedForMinimumOperator() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(true, 1.0D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(true, 0.5D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(false, 0.9D);

        CompositeRequirement composite = new CompositeRequirement(List.of(first, second, third), 2);

        assertTrue(composite.isMet(this.player));

        second.setMet(false);

        assertFalse(composite.isMet(this.player));
    }

    @Test
    void calculateProgressAveragesForAndOperator() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(false, 0.25D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.5D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(false, 0.75D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.AND
        );

        double expected = (0.25D + 0.5D + 0.75D) / 3.0D;
        assertEquals(expected, composite.calculateProgress(this.player), DELTA);

        first.setProgress(1.0D);
        second.setProgress(1.0D);
        third.setProgress(1.0D);

        assertEquals(1.0D, composite.calculateProgress(this.player), DELTA);
    }

    @Test
    void calculateProgressUsesMaximumForOrOperator() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(false, 0.15D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.65D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(false, 0.35D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.OR
        );

        assertEquals(0.65D, composite.calculateProgress(this.player), DELTA);

        second.setProgress(0.1D);
        third.setProgress(0.75D);

        assertEquals(0.75D, composite.calculateProgress(this.player), DELTA);
    }

    @Test
    void calculateProgressAggregatesTopMinimumWhenPartialProgressAllowed() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(false, 0.2D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.7D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(false, 0.4D);
        ConfigurableRequirement fourth = ConfigurableRequirement.metProgress(false, 0.9D);

        CompositeRequirement composite = new CompositeRequirement(List.of(first, second, third, fourth), 2);

        double expected = (0.9D + 0.7D) / 2.0D;
        assertEquals(expected, composite.calculateProgress(this.player), DELTA);
    }

    @Test
    void calculateProgressCountsOnlyCompletedWhenPartialProgressDisallowed() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(true, 1.0D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.85D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(true, 1.0D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.MINIMUM,
                2,
                null,
                false
        );

        assertEquals(1.0D, composite.calculateProgress(this.player), DELTA);

        third.setMet(false);

        assertEquals(0.5D, composite.calculateProgress(this.player), DELTA);
    }

    @Test
    void consumePassesThroughToAllRequirementsForAndOperator() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(true, 1.0D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(true, 1.0D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(true, 1.0D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.AND
        );

        composite.consume(this.player);

        assertEquals(1, first.getConsumeCount());
        assertEquals(1, second.getConsumeCount());
        assertEquals(1, third.getConsumeCount());
    }

    @Test
    void consumeTargetsHighestProgressForOrOperator() {
        ConfigurableRequirement lowest = ConfigurableRequirement.metProgress(false, 0.1D);
        ConfigurableRequirement middle = ConfigurableRequirement.metProgress(false, 0.5D);
        ConfigurableRequirement highest = ConfigurableRequirement.metProgress(false, 0.75D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(lowest, middle, highest),
                CompositeRequirement.Operator.OR
        );

        composite.consume(this.player);

        assertEquals(0, lowest.getConsumeCount());
        assertEquals(0, middle.getConsumeCount());
        assertEquals(1, highest.getConsumeCount());
    }

    @Test
    void consumeTargetsTopMinimumRequirements() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(false, 0.9D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.4D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(false, 0.7D);

        CompositeRequirement composite = new CompositeRequirement(List.of(first, second, third), 2);

        composite.consume(this.player);

        assertEquals(1, first.getConsumeCount());
        assertEquals(0, second.getConsumeCount());
        assertEquals(1, third.getConsumeCount());
    }

    @Test
    void helperMethodsReflectOperatorConfigurationAndOrdering() {
        ConfigurableRequirement first = ConfigurableRequirement.metProgress(true, 0.8D);
        ConfigurableRequirement second = ConfigurableRequirement.metProgress(false, 0.2D);
        ConfigurableRequirement third = ConfigurableRequirement.metProgress(true, 0.6D);

        CompositeRequirement composite = new CompositeRequirement(
                List.of(first, second, third),
                CompositeRequirement.Operator.OR
        );

        assertFalse(composite.isAndLogic());
        assertTrue(composite.isOrLogic());
        assertFalse(composite.isMinimumLogic());

        List<AbstractRequirement> ordered = composite.getRequirementsByProgress(this.player);
        assertEquals(List.of(first, third, second), ordered);

        List<AbstractRequirement> completed = composite.getCompletedRequirements(this.player);
        assertEquals(List.of(first, third), completed);

        List<CompositeRequirement.RequirementProgress> detail = composite.getDetailedProgress(this.player);
        assertEquals(3, detail.size());
        assertEquals(0, detail.get(0).index());
        assertEquals(first, detail.get(0).requirement());
        assertEquals(80, detail.get(0).getProgressPercentage());
    }

    @Test
    void validateDetectsInvalidConfiguration() {
        ConfigurableRequirement requirement = ConfigurableRequirement.metProgress(true, 1.0D);
        CompositeRequirement composite = new CompositeRequirement(List.of(requirement), 1);

        assertDoesNotThrow(composite::validate);

        CompositeRequirement invalidMinimum = new CompositeRequirement(List.of(requirement), 1);
        setMinimumRequired(invalidMinimum, 0);
        assertThrows(IllegalStateException.class, invalidMinimum::validate);

        CompositeRequirement excessiveMinimum = new CompositeRequirement(List.of(requirement), 1);
        setMinimumRequired(excessiveMinimum, 5);
        assertThrows(IllegalStateException.class, excessiveMinimum::validate);

        CompositeRequirement nullRequirement = new CompositeRequirement(List.of(requirement, null), 1);
        assertThrows(IllegalStateException.class, nullRequirement::validate);
    }

    @Test
    void fromStringRejectsInvalidOperatorsAndBounds() {
        List<AbstractRequirement> requirements = List.of(
                ConfigurableRequirement.metProgress(true, 1.0D)
        );

        IllegalArgumentException invalidOperator = assertThrows(
                IllegalArgumentException.class,
                () -> CompositeRequirement.fromString(requirements, "xor", 1)
        );
        assertTrue(invalidOperator.getMessage().contains("Invalid operator"));

        assertThrows(
                IllegalArgumentException.class,
                () -> CompositeRequirement.fromString(requirements, "and", 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> CompositeRequirement.fromString(requirements, "or", 5)
        );

        CompositeRequirement parsed = CompositeRequirement.fromString(requirements, "minimum", 1);
        assertEquals(CompositeRequirement.Operator.MINIMUM, parsed.getOperator());
    }

    private static void setMinimumRequired(CompositeRequirement requirement, int minimum) {
        try {
            java.lang.reflect.Field field = CompositeRequirement.class.getDeclaredField("minimumRequired");
            field.setAccessible(true);
            field.setInt(requirement, minimum);
        } catch (ReflectiveOperationException e) {
            fail(e);
        }
    }

    private static final class ConfigurableRequirement extends AbstractRequirement {

        private double progress;
        private boolean met;
        private int consumeCount;

        private ConfigurableRequirement(boolean met, double progress) {
            super(Type.CUSTOM);
            this.met = met;
            this.progress = progress;
        }

        static ConfigurableRequirement metProgress(boolean met, double progress) {
            return new ConfigurableRequirement(met, progress);
        }

        void setProgress(double progress) {
            this.progress = progress;
        }

        void setMet(boolean met) {
            this.met = met;
        }

        int getConsumeCount() {
            return this.consumeCount;
        }

        @Override
        public boolean isMet(Player player) {
            return this.met;
        }

        @Override
        public double calculateProgress(Player player) {
            return this.progress;
        }

        @Override
        public void consume(Player player) {
            this.consumeCount++;
        }

        @Override
        public String getDescriptionKey() {
            return "requirement.test";
        }
    }
}
