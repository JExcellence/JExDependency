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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TimedRequirementTest {

    private static final long FIVE_SECONDS = 5_000L;

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("TimedRequirementTester");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void isMetRespectsAutoStartAndRejectsExpiredWindows() throws Exception {
        final ControllableRequirement delegate = new ControllableRequirement(true, 1.0d);
        final TimedRequirement autoRequirement = new TimedRequirement(delegate, FIVE_SECONDS, true, "auto");

        assertFalse(autoRequirement.isStarted(), "Auto-start requirement should begin idle");
        assertTrue(autoRequirement.isMet(this.player), "Auto-start requirement should delegate when within the window");
        assertTrue(autoRequirement.isStarted(), "Auto-start requirement should start the timer on first evaluation");

        setStartTime(autoRequirement, System.currentTimeMillis() - FIVE_SECONDS - 250L);
        assertFalse(autoRequirement.isMet(this.player), "Expired requirements must reject success even when the delegate passes");

        final ControllableRequirement manualDelegate = new ControllableRequirement(true, 1.0d);
        final TimedRequirement manualRequirement = new TimedRequirement(manualDelegate, FIVE_SECONDS, false, "manual");

        assertFalse(manualRequirement.isMet(this.player), "Manual requirements should not start until explicitly triggered");
        manualRequirement.start();
        setStartTime(manualRequirement, System.currentTimeMillis() - 1_000L);
        assertTrue(manualRequirement.isMet(this.player), "Manual requirements should succeed once started and within the window");
    }

    @Test
    void calculateProgressScalesDelegateProgressByRemainingTime() throws Exception {
        final ControllableRequirement delegate = new ControllableRequirement(true, 0.8d);
        final TimedRequirement requirement = new TimedRequirement(delegate, 10_000L, true, "progress");

        requirement.start();
        setStartTime(requirement, System.currentTimeMillis() - 2_500L);

        final double expectedProgress = 0.8d * 0.75d;
        assertEquals(expectedProgress, requirement.calculateProgress(this.player), 1.0e-6,
                "Progress should be scaled by the remaining time fraction");

        setStartTime(requirement, System.currentTimeMillis() - 10_500L);
        assertEquals(0.0d, requirement.calculateProgress(this.player),
                "Progress should drop to zero when the requirement has expired");

        final ControllableRequirement manualDelegate = new ControllableRequirement(true, 0.5d);
        final TimedRequirement manualRequirement = new TimedRequirement(manualDelegate, 10_000L, false, "manualProgress");
        assertEquals(0.0d, manualRequirement.calculateProgress(this.player),
                "Manual timers that have not started should report zero progress");
    }

    @Test
    void consumeOnlyFiresWhenDelegateSucceedsWithinTimeLimit() throws Exception {
        final ControllableRequirement successfulDelegate = new ControllableRequirement(true, 1.0d);
        final TimedRequirement successfulRequirement = new TimedRequirement(successfulDelegate, FIVE_SECONDS, true, "success");

        successfulRequirement.start();
        setStartTime(successfulRequirement, System.currentTimeMillis());
        successfulRequirement.consume(this.player);
        assertEquals(1, successfulDelegate.getConsumeCount(), "Delegate should be consumed when the timer is active and met");
        assertSame(this.player, successfulDelegate.getLastConsumedPlayer(), "Consumed player should be forwarded to the delegate");

        final ControllableRequirement failingDelegate = new ControllableRequirement(false, 0.0d);
        final TimedRequirement failingRequirement = new TimedRequirement(failingDelegate, FIVE_SECONDS, true, "failing");

        failingRequirement.start();
        setStartTime(failingRequirement, System.currentTimeMillis());
        failingRequirement.consume(this.player);
        assertEquals(0, failingDelegate.getConsumeCount(), "Delegates should not be consumed when they are not met");

        final ControllableRequirement expiredDelegate = new ControllableRequirement(true, 1.0d);
        final TimedRequirement expiredRequirement = new TimedRequirement(expiredDelegate, FIVE_SECONDS, true, "expired");

        expiredRequirement.start();
        setStartTime(expiredRequirement, System.currentTimeMillis() - FIVE_SECONDS - 100L);
        expiredRequirement.consume(this.player);
        assertEquals(0, expiredDelegate.getConsumeCount(), "Delegates should not consume when the timer window has passed");
    }

    @Test
    void helperAccessorsReflectTimerState() throws Exception {
        final ControllableRequirement delegate = new ControllableRequirement(true, 1.0d);
        final TimedRequirement requirement = new TimedRequirement(delegate, FIVE_SECONDS, false, "helpers");

        assertEquals(FIVE_SECONDS, requirement.getRemainingTimeMillis(),
                "Unstarted timers should report the full remaining window");
        assertEquals("5s", requirement.getFormattedRemainingTime(),
                "Unstarted timers should describe the full window");
        assertEquals("Not started", requirement.getFormattedStartTime(),
                "Unstarted timers should report that no start time is available");

        final long fixedStart = Instant.parse("2023-03-01T12:00:00Z").toEpochMilli();
        setStartTime(requirement, fixedStart);
        final String expectedStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(fixedStart), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        assertEquals(expectedStart, requirement.getFormattedStartTime(),
                "Formatted start time should respect the configured pattern and system zone");

        setStartTime(requirement, System.currentTimeMillis() - FIVE_SECONDS - 1_000L);
        assertEquals(0L, requirement.getRemainingTimeMillis(),
                "Expired timers should clamp remaining time to zero");
        assertEquals("Expired", requirement.getFormattedRemainingTime(),
                "Expired timers should report their status");
    }

    @Test
    void validateEnsuresConfigurationAndNestedDelegates() {
        final ControllableRequirement delegate = new ControllableRequirement(true, 1.0d);
        final TimedRequirement requirement = new TimedRequirement(delegate, FIVE_SECONDS, true, "valid");

        assertDoesNotThrow(requirement::validate, "Valid configurations should pass validation");

        final TimedRequirement invalidDelegateRequirement = new TimedRequirement(null, FIVE_SECONDS, true, "invalid");
        final IllegalStateException missingDelegate = assertThrows(IllegalStateException.class,
                invalidDelegateRequirement::validate,
                "Validation should fail when the delegate is missing");
        assertEquals("Delegate requirement cannot be null.", missingDelegate.getMessage());

        final TimedRequirement inner = spy(new TimedRequirement(delegate, FIVE_SECONDS, true, "inner"));
        final TimedRequirement outer = new TimedRequirement(inner, FIVE_SECONDS, true, "outer");
        assertDoesNotThrow(outer::validate, "Outer validation should cascade to nested timed delegates");
        verify(inner).validate();

        final TimedRequirement failingInner = spy(new TimedRequirement(delegate, FIVE_SECONDS, true, "failingInner"));
        doThrow(new IllegalStateException("Delegate requirement cannot be null.")).when(failingInner).validate();
        final TimedRequirement failingOuter = new TimedRequirement(failingInner, FIVE_SECONDS, true, "failingOuter");
        final IllegalStateException nestedFailure = assertThrows(IllegalStateException.class,
                failingOuter::validate,
                "Outer validation should surface nested validation failures");
        assertEquals("Delegate requirement cannot be null.", nestedFailure.getMessage());
    }

    private static void setStartTime(final TimedRequirement requirement, final long startTimeMillis) throws Exception {
        final Field field = TimedRequirement.class.getDeclaredField("startTimeMillis");
        field.setAccessible(true);
        final AtomicLong state = (AtomicLong) field.get(requirement);
        state.set(startTimeMillis);
    }

    private static final class ControllableRequirement extends AbstractRequirement {

        private boolean met;
        private double progress;
        private int consumeCount;
        private Player lastConsumedPlayer;

        private ControllableRequirement(final boolean met,
                                        final double progress) {
            super(Type.CUSTOM);
            this.met = met;
            this.progress = progress;
        }

        @Override
        public boolean isMet(final @NotNull Player player) {
            return this.met;
        }

        @Override
        public double calculateProgress(final @NotNull Player player) {
            return this.progress;
        }

        @Override
        public void consume(final @NotNull Player player) {
            this.consumeCount++;
            this.lastConsumedPlayer = player;
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return "requirement.test";
        }

        private int getConsumeCount() {
            return this.consumeCount;
        }

        private Player getLastConsumedPlayer() {
            return this.lastConsumedPlayer;
        }
    }
}
