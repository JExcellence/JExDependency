package com.raindropcentral.rdq.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChoiceRequirementTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("RequirementTester");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isMetRespectsMinimumChoices() {
        TestRequirement first = new TestRequirement(true, 1.0d);
        TestRequirement second = new TestRequirement(false, 0.25d);
        TestRequirement third = new TestRequirement(true, 0.75d);

        ChoiceRequirement minimumTwo = new ChoiceRequirement(List.of(first, second, third), 2);
        assertTrue(minimumTwo.isMet(this.player));
        assertEquals(1, first.getIsMetCalls());
        assertEquals(1, second.getIsMetCalls());
        assertEquals(1, third.getIsMetCalls());

        ChoiceRequirement minimumThree = new ChoiceRequirement(List.of(first, second, third), 3);
        assertFalse(minimumThree.isMet(this.player));
    }

    @Test
    void calculateProgressUsesMaxWhenSingleChoice() {
        TestRequirement low = new TestRequirement(false, 0.2d);
        TestRequirement high = new TestRequirement(true, 0.8d);
        TestRequirement mid = new TestRequirement(false, 0.5d);

        ChoiceRequirement requirement = new ChoiceRequirement(List.of(low, high, mid), 1);

        assertEquals(0.8d, requirement.calculateProgress(this.player), 1.0e-6);
        assertEquals(1, low.getCalculateProgressCalls());
        assertEquals(1, high.getCalculateProgressCalls());
        assertEquals(1, mid.getCalculateProgressCalls());
    }

    @Test
    void calculateProgressAveragesTopChoicesWhenPartialAllowed() {
        TestRequirement first = new TestRequirement(false, 1.0d);
        TestRequirement second = new TestRequirement(false, 0.5d);
        TestRequirement third = new TestRequirement(false, 0.2d);

        ChoiceRequirement requirement = new ChoiceRequirement(List.of(first, second, third), 2);

        assertEquals(0.75d, requirement.calculateProgress(this.player), 1.0e-6);
    }

    @Test
    void calculateProgressCountsCompletedChoicesWhenPartialDisallowed() {
        TestRequirement first = new TestRequirement(true, 1.0d);
        TestRequirement second = new TestRequirement(false, 0.6d);
        TestRequirement third = new TestRequirement(true, 1.0d);

        ChoiceRequirement requirement = new ChoiceRequirement(
                List.of(first, second, third),
                2,
                null,
                false
        );

        assertEquals(1.0d, requirement.calculateProgress(this.player), 1.0e-6);

        third.setProgress(0.4d);
        third.setMet(false);

        assertEquals(0.5d, requirement.calculateProgress(this.player), 1.0e-6);
    }

    @Test
    void consumeOnlyDelegatesToHighestProgressChoices() {
        TestRequirement top = new TestRequirement(true, 0.9d);
        TestRequirement runnerUp = new TestRequirement(false, 0.8d);
        TestRequirement ignored = new TestRequirement(false, 0.1d);

        ChoiceRequirement requirement = new ChoiceRequirement(List.of(top, runnerUp, ignored), 2);

        requirement.consume(this.player);

        assertEquals(1, top.getConsumeCalls());
        assertEquals(1, runnerUp.getConsumeCalls());
        assertEquals(0, ignored.getConsumeCalls());
    }

    @Test
    void helperAccessorsReflectConfiguredState() {
        TestRequirement completed = new TestRequirement(true, 1.0d);
        TestRequirement inProgress = new TestRequirement(false, 0.4d);
        List<AbstractRequirement> supplied = new ArrayList<>();
        supplied.add(completed);
        supplied.add(inProgress);

        ChoiceRequirement requirement = new ChoiceRequirement(supplied, 1);

        List<AbstractRequirement> returnedChoices = requirement.getChoices();
        assertNotSame(supplied, returnedChoices);
        assertEquals(supplied, returnedChoices);

        assertTrue(requirement.getBestChoice(this.player).isPresent());
        assertSame(completed, requirement.getBestChoice(this.player).orElseThrow());

        List<AbstractRequirement> completedChoices = requirement.getCompletedChoices(this.player);
        assertEquals(List.of(completed), completedChoices);

        assertTrue(requirement.isSingleChoice());
    }

    @Test
    void validateAcceptsValidConfiguration() {
        ChoiceRequirement requirement = new ChoiceRequirement(
                List.of(new TestRequirement(true, 1.0d)),
                1
        );

        assertDoesNotThrow(requirement::validate);
    }

    @Test
    void validateRejectsEmptyChoices() throws Exception {
        ChoiceRequirement requirement = new ChoiceRequirement(
                List.of(new TestRequirement(true, 1.0d)),
                1
        );

        setField(requirement, "choices", new ArrayList<>());

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void validateRejectsMinimumLessThanOne() throws Exception {
        ChoiceRequirement requirement = new ChoiceRequirement(
                List.of(new TestRequirement(true, 1.0d)),
                1
        );

        setField(requirement, "minimumChoicesRequired", 0);

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void validateRejectsMinimumGreaterThanChoices() throws Exception {
        ChoiceRequirement requirement = new ChoiceRequirement(
                List.of(new TestRequirement(true, 1.0d), new TestRequirement(false, 0.3d)),
                2
        );

        setField(requirement, "minimumChoicesRequired", 3);

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    @Test
    void validateRejectsNullChoiceEntries() {
        List<AbstractRequirement> choices = new ArrayList<>();
        choices.add(null);
        choices.add(new TestRequirement(true, 1.0d));

        ChoiceRequirement requirement = new ChoiceRequirement(choices, 1);

        assertThrows(IllegalStateException.class, requirement::validate);
    }

    private static void setField(final ChoiceRequirement requirement, final String name, final Object value)
            throws ReflectiveOperationException {
        Field field = ChoiceRequirement.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(requirement, value);
    }

    private static final class TestRequirement extends AbstractRequirement {

        private boolean met;
        private double progress;
        private int isMetCalls;
        private int calculateProgressCalls;
        private int consumeCalls;

        private TestRequirement(final boolean met, final double progress) {
            super(Type.CUSTOM);
            this.met = met;
            this.progress = progress;
        }

        @Override
        public boolean isMet(final @NotNull Player player) {
            this.isMetCalls++;
            return this.met;
        }

        @Override
        public double calculateProgress(final @NotNull Player player) {
            this.calculateProgressCalls++;
            return this.progress;
        }

        @Override
        public void consume(final @NotNull Player player) {
            this.consumeCalls++;
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return "requirement.test";
        }

        private void setMet(final boolean met) {
            this.met = met;
        }

        private void setProgress(final double progress) {
            this.progress = progress;
        }

        private int getIsMetCalls() {
            return this.isMetCalls;
        }

        private int getCalculateProgressCalls() {
            return this.calculateProgressCalls;
        }

        private int getConsumeCalls() {
            return this.consumeCalls;
        }
    }
}
