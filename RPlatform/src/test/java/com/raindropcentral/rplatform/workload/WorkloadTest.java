package com.raindropcentral.rplatform.workload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadTest {

    @Test
    @DisplayName("Default computeCost should report a nominal cost of 1")
    void defaultComputeCostShouldReportNominalValue() {
        final Workload workload = () -> { };

        assertEquals(1, workload.computeCost(),
            "Default workload cost should be 1 to represent a nominal execution weight");
    }

    @Test
    @DisplayName("Custom workloads can override computeCost to communicate heavier operations")
    void customWorkloadsMayOverrideComputeCost() {
        final TrackingWorkload workload = new TrackingWorkload(5);

        assertEquals(5, workload.computeCost(),
            "Custom workloads should surface the configured cost override");
    }

    @Nested
    @DisplayName("Builder helpers")
    class BuilderHelperTests {

        @Test
        @DisplayName("Workload.of should delegate execution to the wrapped runnable")
        void ofShouldDelegateToRunnable() {
            final AtomicBoolean executed = new AtomicBoolean();
            final Workload workload = Workload.of(() -> executed.set(true));

            workload.execute();

            assertTrue(executed.get(),
                "Wrapped runnable should execute when invoking the workload");
            assertEquals(1, workload.computeCost(),
                "Workload.of should preserve the nominal cost contract");
        }

        @Test
        @DisplayName("andThen should execute workloads sequentially and sum their costs")
        void andThenShouldComposeExecutionAndCosts() {
            final List<String> order = new ArrayList<>();
            final TrackingWorkload first = new TrackingWorkload(2, () -> order.add("first"));
            final TrackingWorkload second = new TrackingWorkload(3, () -> order.add("second"));

            final Workload composite = first.andThen(second);
            composite.execute();

            assertEquals(List.of("first", "second"), order,
                "andThen should execute workloads in declaration order");
            assertEquals(5, composite.computeCost(),
                "Composite workload cost should equal the sum of both participants");
        }
    }

    private static final class TrackingWorkload implements Workload {

        private final int cost;
        private final Runnable callback;

        private TrackingWorkload(final int cost) {
            this(cost, () -> { });
        }

        private TrackingWorkload(final int cost, final Runnable callback) {
            this.cost = cost;
            this.callback = callback;
        }

        @Override
        public void execute() {
            callback.run();
        }

        @Override
        public int computeCost() {
            return cost;
        }
    }
}
