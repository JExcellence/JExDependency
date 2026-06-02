package de.jexcellence.dependency.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DependencyCoordinate")
class DependencyCoordinateTest {

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("three-part GAV returns coordinate with null classifier")
        void threePartGav() {
            final var coord = DependencyCoordinate.parse("com.example:my-lib:1.0.0");

            assertNotNull(coord);
            assertEquals("com.example", coord.groupId());
            assertEquals("my-lib", coord.artifactId());
            assertEquals("1.0.0", coord.version());
            assertNull(coord.classifier());
        }

        @Test
        @DisplayName("four-part GAV populates classifier")
        void fourPartGav() {
            final var coord = DependencyCoordinate.parse("com.example:my-lib:1.0.0:sources");

            assertNotNull(coord);
            assertEquals("sources", coord.classifier());
        }

        @ParameterizedTest(name = "returns null for \"{0}\"")
        @ValueSource(strings = {"com.example:my-lib", "com.example", ""})
        @DisplayName("fewer than three colon-separated segments returns null")
        void tooFewParts(final String gav) {
            assertNull(DependencyCoordinate.parse(gav));
        }

        @Test
        @DisplayName("null input throws NullPointerException")
        void nullInput() {
            assertThrows(NullPointerException.class, () -> DependencyCoordinate.parse(null));
        }
    }

    @Nested
    @DisplayName("toGavString()")
    class ToGavString {

        @Test
        @DisplayName("formats group:artifact:version without classifier")
        void withoutClassifier() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0");

            assertEquals("com.example:my-lib:1.0.0", coord.toGavString());
        }

        @Test
        @DisplayName("appends :classifier when classifier is non-empty")
        void withClassifier() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0", "sources");

            assertEquals("com.example:my-lib:1.0.0:sources", coord.toGavString());
        }

        @Test
        @DisplayName("empty string classifier is omitted from output")
        void emptyClassifierOmitted() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0", "");
            final var gav = coord.toGavString();

            assertEquals("com.example:my-lib:1.0.0", gav);
            assertEquals(2, gav.chars().filter(c -> c == ':').count(), "expected exactly two colons in " + gav);
        }
    }

    @Nested
    @DisplayName("toFileName()")
    class ToFileName {

        @Test
        @DisplayName("without classifier returns artifact-version.jar")
        void withoutClassifier() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0");

            assertEquals("my-lib-1.0.0.jar", coord.toFileName());
        }

        @Test
        @DisplayName("with classifier inserts it before .jar extension")
        void withClassifier() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0", "sources");

            assertEquals("my-lib-1.0.0-sources.jar", coord.toFileName());
        }

        @Test
        @DisplayName("result always ends with .jar")
        void alwaysEndsWithJar() {
            final var coord = new DependencyCoordinate("g", "a", "v");

            assertTrue(coord.toFileName().endsWith(".jar"));
        }
    }

    @Nested
    @DisplayName("toRepositoryPath()")
    class ToRepositoryPath {

        @Test
        @DisplayName("dots in groupId become path separators")
        void groupDotsConverted() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0");

            assertEquals("com/example/my-lib/1.0.0/my-lib-1.0.0.jar", coord.toRepositoryPath());
        }

        @Test
        @DisplayName("classifier is included in the leaf file name")
        void classifierInPath() {
            final var coord = new DependencyCoordinate("com.example", "my-lib", "1.0.0", "sources");

            assertEquals("com/example/my-lib/1.0.0/my-lib-1.0.0-sources.jar", coord.toRepositoryPath());
        }
    }

    @Nested
    @DisplayName("constructor null-checks")
    class NullChecks {

        @Test
        @DisplayName("null groupId throws NullPointerException")
        void nullGroupId() {
            assertThrows(NullPointerException.class, () -> new DependencyCoordinate(null, "a", "v"));
        }

        @Test
        @DisplayName("null artifactId throws NullPointerException")
        void nullArtifactId() {
            assertThrows(NullPointerException.class, () -> new DependencyCoordinate("g", null, "v"));
        }

        @Test
        @DisplayName("null version throws NullPointerException")
        void nullVersion() {
            assertThrows(NullPointerException.class, () -> new DependencyCoordinate("g", "a", null));
        }
    }
}
