package de.jexcellence.dependency.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DownloadResult")
class DownloadResultTest {

    private static final DependencyCoordinate COORD = new DependencyCoordinate("com.example", "lib", "1.0.0");
    private static final File FAKE_FILE = new File("/tmp/lib-1.0.0.jar");

    @Test
    @DisplayName("success() sets success=true, carries file, no error message")
    void successFactory() {
        final var result = DownloadResult.success(COORD, FAKE_FILE);

        assertTrue(result.success());
        assertEquals(FAKE_FILE, result.file());
        assertNull(result.errorMessage());
        assertEquals(COORD, result.coordinate());
    }

    @Test
    @DisplayName("failure() sets success=false, null file, carries error message")
    void failureFactory() {
        final var result = DownloadResult.failure(COORD, "Connection refused");

        assertFalse(result.success());
        assertNull(result.file());
        assertEquals("Connection refused", result.errorMessage());
        assertEquals(COORD, result.coordinate());
    }

    @Test
    @DisplayName("null coordinate throws NullPointerException")
    void nullCoordinateThrows() {
        assertThrows(NullPointerException.class, () -> new DownloadResult(null, null, false, null));
    }

    @Test
    @DisplayName("success and failure results with same coordinate are not equal to each other")
    void successAndFailureNotEqual() {
        final var success = DownloadResult.success(COORD, FAKE_FILE);
        final var failure = DownloadResult.failure(COORD, "error");

        assertFalse(success.equals(failure));
    }
}
