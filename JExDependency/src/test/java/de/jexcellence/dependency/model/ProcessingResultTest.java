package de.jexcellence.dependency.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingResultTest {

    @Test
    void gettersReturnUnmodifiableLists() {
        final List<DependencyCoordinate> successful = new ArrayList<>(List.of(
                new DependencyCoordinate("group", "artifact", "1.0")
        ));
        final List<DownloadResult> failed = new ArrayList<>(List.of(
                DownloadResult.failure(new DependencyCoordinate("group", "broken", "1.0"), "network")
        ));

        final ProcessingResult result = new ProcessingResult(successful, failed, 42L);

        final List<DependencyCoordinate> successfulView = result.getSuccessful();
        final List<DownloadResult> failedView = result.getFailed();

        assertEquals(1, successfulView.size());
        assertEquals(1, failedView.size());
        assertThrows(UnsupportedOperationException.class, () -> successfulView.add(new DependencyCoordinate("group", "extra", "1.1")));
        assertThrows(UnsupportedOperationException.class, () -> failedView.add(DownloadResult.failure(new DependencyCoordinate("group", "other", "2.0"), "bad")));

        successful.add(new DependencyCoordinate("group", "later", "3.0"));
        failed.add(DownloadResult.failure(new DependencyCoordinate("group", "later", "3.0"), "timeout"));

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    void emptyResultHasZeroCountsAndIsNotSuccessful() {
        final ProcessingResult result = new ProcessingResult(List.of(), List.of(), 0L);

        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertFalse(result.hasFailures());
        assertFalse(result.isFullySuccessful());
    }

    @Test
    void partialResultReportsFailuresAndTotals() {
        final ProcessingResult result = new ProcessingResult(
                List.of(new DependencyCoordinate("group", "artifact", "1.0")),
                List.of(DownloadResult.failure(new DependencyCoordinate("group", "broken", "1.0"), "network")),
                100L
        );

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.hasFailures());
        assertFalse(result.isFullySuccessful());
    }

    @Test
    void fullySuccessfulResultReportsNoFailures() {
        final ProcessingResult result = new ProcessingResult(
                List.of(
                        new DependencyCoordinate("group", "artifact", "1.0"),
                        new DependencyCoordinate("group", "artifact-two", "1.0")
                ),
                List.of(),
                200L
        );

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertFalse(result.hasFailures());
        assertTrue(result.isFullySuccessful());
    }
}
