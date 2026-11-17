package de.jexcellence.dependency.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentralMirrorTest {

    private final List<String> warnings = new ArrayList<>();

    private String resolve(final Map<String, String> properties, final Map<String, String> environment) {
        final UnaryOperator<String> propertyLookup = properties::get;
        final UnaryOperator<String> environmentLookup = environment::get;
        return CentralMirror.resolve(propertyLookup, environmentLookup, warnings::add);
    }

    @Test
    @DisplayName("uses the Google-hosted Central mirror when nothing is configured")
    void defaultMirror() {
        assertEquals(CentralMirror.DEFAULT_MIRROR, resolve(Map.of(), Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("honours Paper's environment variable")
    void paperEnvironmentVariable() {
        assertEquals("https://mirror.example.org/maven2",
                resolve(Map.of(), Map.of(CentralMirror.PAPER_ENVIRONMENT_VARIABLE, "https://mirror.example.org/maven2")));
    }

    @Test
    @DisplayName("the system property wins over the environment variable")
    void propertyWins() {
        assertEquals("https://own.example.org/central",
                resolve(Map.of(CentralMirror.PROPERTY, "https://own.example.org/central"),
                        Map.of(CentralMirror.PAPER_ENVIRONMENT_VARIABLE, "https://mirror.example.org/maven2")));
    }

    @Test
    @DisplayName("removes a trailing slash and surrounding blanks")
    void normalises() {
        assertEquals("https://mirror.example.org/maven2",
                resolve(Map.of(CentralMirror.PROPERTY, "  https://mirror.example.org/maven2/  "), Map.of()));
    }

    @Test
    @DisplayName("ignores values that are not http or https URLs and falls back")
    void ignoresInvalidValues() {
        assertEquals("https://mirror.example.org/maven2",
                resolve(Map.of(CentralMirror.PROPERTY, "ftp://mirror.example.org/maven2"),
                        Map.of(CentralMirror.PAPER_ENVIRONMENT_VARIABLE, "https://mirror.example.org/maven2")));
        assertEquals(CentralMirror.DEFAULT_MIRROR,
                resolve(Map.of(CentralMirror.PROPERTY, "not a url"), Map.of()));
        assertEquals(2, warnings.size());
    }

    @Test
    @DisplayName("accepts plain http but warns that it is unencrypted")
    void warnsOnHttp() {
        assertEquals("http://nexus.internal:8081/repository/central",
                resolve(Map.of(CentralMirror.PROPERTY, "http://nexus.internal:8081/repository/central"), Map.of()));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("not encrypted"), warnings.get(0));
    }

    @Test
    @DisplayName("treats blank values as not configured")
    void blankIsUnset() {
        assertEquals(CentralMirror.DEFAULT_MIRROR,
                resolve(Map.of(CentralMirror.PROPERTY, "  "), Map.of(CentralMirror.PAPER_ENVIRONMENT_VARIABLE, "")));
        assertTrue(warnings.isEmpty());
    }
}
