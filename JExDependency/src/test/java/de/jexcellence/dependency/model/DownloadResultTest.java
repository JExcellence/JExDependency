package de.jexcellence.dependency.model;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadResultTest {

    private static final DependencyCoordinate COORDINATE = new DependencyCoordinate("group", "artifact", "1.0.0");

    @Test
    void successPopulatesFields() {
        final File file = new File("dependency.jar");

        final DownloadResult result = DownloadResult.success(COORDINATE, file);

        assertSame(COORDINATE, result.coordinate());
        assertSame(file, result.file());
        assertTrue(result.success());
        assertNull(result.errorMessage());
    }

    @Test
    void failurePopulatesFields() {
        final String errorMessage = "Unable to download";

        final DownloadResult result = DownloadResult.failure(COORDINATE, errorMessage);

        assertSame(COORDINATE, result.coordinate());
        assertNull(result.file());
        assertFalse(result.success());
        assertEquals(errorMessage, result.errorMessage());
    }

    @Test
    void constructorRejectsNullCoordinate() {
        assertThrows(NullPointerException.class, () -> new DownloadResult(null, new File("dependency.jar"), true, null));
    }
}
