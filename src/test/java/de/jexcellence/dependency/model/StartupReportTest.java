package de.jexcellence.dependency.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupReportTest {

    @Test
    @DisplayName("summarises counts and only the phases that ran")
    void summary() {
        final StartupReport report = new StartupReport();
        assertEquals("ok", report.time(StartupReport.Phase.DOWNLOAD, () -> "ok"));
        report.time(StartupReport.Phase.INJECT, () -> null);
        report.libraries(10, 2).downloads(9, 2, 1);

        final String summary = report.summary();

        assertTrue(summary.startsWith("Loaded 11 libraries (2 transitive) in "), summary);
        assertTrue(summary.contains("download ") && summary.contains("(9 cached, 2 downloaded)"), summary);
        assertTrue(summary.contains("inject "), summary);
        assertFalse(summary.contains("resolve") || summary.contains("remap"), summary);
        assertTrue(summary.endsWith("; 1 failed"), summary);
    }

    @Test
    @DisplayName("a load result is successful only without failures")
    void loadResult() {
        final DependencyCoordinate coordinate = new DependencyCoordinate("g", "a", "1");
        assertTrue(new LoadResult(List.of(coordinate), List.of(), 0, 5).isSuccessful());
        assertFalse(new LoadResult(List.of(), List.of(DownloadResult.failure(coordinate, "x")), 0, 5).isSuccessful());
    }
}
