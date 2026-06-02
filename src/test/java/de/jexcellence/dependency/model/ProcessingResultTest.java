package de.jexcellence.dependency.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProcessingResult")
class ProcessingResultTest {

    private static final DependencyCoordinate COORD_A = new DependencyCoordinate("com.example", "lib-a", "1.0");
    private static final DependencyCoordinate COORD_B = new DependencyCoordinate("com.example", "lib-b", "2.0");
    private static final DownloadResult FAILURE_B = DownloadResult.failure(COORD_B, "404 Not Found");

    @Nested
    @DisplayName("counts")
    class Counts {

        @Test
        @DisplayName("getTotalCount() sums successful and failed lists")
        void totalCount() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(FAILURE_B), 100L);

            assertEquals(2, result.getTotalCount());
        }

        @Test
        @DisplayName("getSuccessCount() reflects only the successful list")
        void successCount() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(FAILURE_B), 0L);

            assertEquals(1, result.getSuccessCount());
        }

        @Test
        @DisplayName("getFailureCount() reflects only the failed list")
        void failureCount() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(FAILURE_B), 0L);

            assertEquals(1, result.getFailureCount());
        }

        @Test
        @DisplayName("empty result has total count of zero")
        void emptyTotal() {
            final var result = new ProcessingResult(List.of(), List.of(), 0L);

            assertEquals(0, result.getTotalCount());
        }

        @Test
        @DisplayName("processingTimeMillis is preserved exactly")
        void timingPreserved() {
            final var result = new ProcessingResult(List.of(), List.of(), 1_234L);

            assertEquals(1_234L, result.getProcessingTimeMillis());
        }
    }

    @Nested
    @DisplayName("hasFailures()")
    class HasFailures {

        @Test
        @DisplayName("returns true when at least one failure is present")
        void withFailure() {
            final var result = new ProcessingResult(List.of(), List.of(FAILURE_B), 0L);

            assertTrue(result.hasFailures());
        }

        @Test
        @DisplayName("returns false when failure list is empty")
        void noFailures() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(), 0L);

            assertFalse(result.hasFailures());
        }
    }

    @Nested
    @DisplayName("isFullySuccessful()")
    class IsFullySuccessful {

        @Test
        @DisplayName("returns true when successes exist and failure list is empty")
        void allSucceeded() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(), 50L);

            assertTrue(result.isFullySuccessful());
        }

        @Test
        @DisplayName("returns false when no dependencies were processed at all")
        void empty() {
            final var result = new ProcessingResult(List.of(), List.of(), 0L);

            assertFalse(result.isFullySuccessful());
        }

        @Test
        @DisplayName("returns false when any failure is present alongside successes")
        void mixedOutcome() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(FAILURE_B), 0L);

            assertFalse(result.isFullySuccessful());
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("getSuccessful() returns an unmodifiable list")
        void successfulUnmodifiable() {
            final var result = new ProcessingResult(List.of(COORD_A), List.of(), 0L);

            assertThrows(UnsupportedOperationException.class, () -> result.getSuccessful().add(COORD_B));
        }

        @Test
        @DisplayName("getFailed() returns an unmodifiable list")
        void failedUnmodifiable() {
            final var result = new ProcessingResult(List.of(), List.of(FAILURE_B), 0L);

            assertThrows(UnsupportedOperationException.class, () -> result.getFailed().add(FAILURE_B));
        }

        @Test
        @DisplayName("mutating the source list after construction does not affect the result")
        void defensiveCopy() {
            final var mutableSuccessful = new java.util.ArrayList<>(List.of(COORD_A));
            final var result = new ProcessingResult(mutableSuccessful, List.of(), 0L);

            mutableSuccessful.add(COORD_B);

            assertEquals(1, result.getSuccessCount());
        }
    }
}
