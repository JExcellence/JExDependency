package de.jexcellence.dependency.remapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelocationFingerprintTest {

    @Test
    @DisplayName("does not depend on the order relocations were registered in")
    void orderIndependent() {
        final Map<String, String> first = new LinkedHashMap<>();
        first.put("com.a", "x.com.a");
        first.put("org.b", "x.org.b");
        final Map<String, String> second = new LinkedHashMap<>();
        second.put("org.b", "x.org.b");
        second.put("com.a", "x.com.a");

        assertEquals(RelocationFingerprint.of(first), RelocationFingerprint.of(second));
    }

    @Test
    @DisplayName("changes when a relocation is added or its target changes")
    void changesWithContent() {
        final String base = RelocationFingerprint.of(Map.of("com.a", "x.com.a"));

        assertNotEquals(base, RelocationFingerprint.of(Map.of("com.a", "x.com.a", "org.b", "x.org.b")));
        assertNotEquals(base, RelocationFingerprint.of(Map.of("com.a", "y.com.a")));
    }

    @Test
    @DisplayName("is a 16 character lowercase hex name")
    void fileNameSafe() {
        assertTrue(RelocationFingerprint.of(Map.of()).matches("[0-9a-f]{16}"));
    }
}
