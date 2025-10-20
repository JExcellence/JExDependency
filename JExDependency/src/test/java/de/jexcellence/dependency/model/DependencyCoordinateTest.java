package de.jexcellence.dependency.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DependencyCoordinateTest {

    @Test
    void toGavStringOmitsClassifierWhenAbsent() {
        final DependencyCoordinate coordinate = new DependencyCoordinate(
                "com.example",
                "demo-artifact",
                "1.0.0"
        );

        assertEquals("com.example:demo-artifact:1.0.0", coordinate.toGavString());
    }

    @Test
    void toGavStringIncludesClassifierWhenPresent() {
        final DependencyCoordinate coordinate = new DependencyCoordinate(
                "com.example",
                "demo-artifact",
                "1.0.0",
                "sources"
        );

        assertEquals("com.example:demo-artifact:1.0.0:sources", coordinate.toGavString());
    }

    @Test
    void toFileNameOmitsClassifierWhenAbsent() {
        final DependencyCoordinate coordinate = new DependencyCoordinate(
                "com.example",
                "demo-artifact",
                "1.0.0"
        );

        assertEquals("demo-artifact-1.0.0.jar", coordinate.toFileName());
    }

    @Test
    void toFileNameIncludesClassifierWhenPresent() {
        final DependencyCoordinate coordinate = new DependencyCoordinate(
                "com.example",
                "demo-artifact",
                "1.0.0",
                "sources"
        );

        assertEquals("demo-artifact-1.0.0-sources.jar", coordinate.toFileName());
    }

    @Test
    void toRepositoryPathUsesGroupDirectoryStructureWithoutClassifier() {
        final DependencyCoordinate coordinate = new DependencyCoordinate(
                "com.example.lib",
                "demo-artifact",
                "1.0.0"
        );

        assertEquals(
                "com/example/lib/demo-artifact/1.0.0/demo-artifact-1.0.0.jar",
                coordinate.toRepositoryPath()
        );
    }

    @Test
    void toRepositoryPathUsesGroupDirectoryStructureWithClassifier() {
        final DependencyCoordinate coordinate = new DependencyCoordinate(
                "com.example.lib",
                "demo-artifact",
                "1.0.0",
                "sources"
        );

        assertEquals(
                "com/example/lib/demo-artifact/1.0.0/demo-artifact-1.0.0-sources.jar",
                coordinate.toRepositoryPath()
        );
    }

    @Test
    void parseReturnsCoordinateForValidStringWithoutClassifier() {
        final DependencyCoordinate coordinate = DependencyCoordinate.parse("com.example:demo-artifact:1.0.0");

        assertNotNull(coordinate);
        assertEquals(new DependencyCoordinate("com.example", "demo-artifact", "1.0.0"), coordinate);
    }

    @Test
    void parseReturnsCoordinateForValidStringWithClassifier() {
        final DependencyCoordinate coordinate = DependencyCoordinate.parse("com.example:demo-artifact:1.0.0:sources");

        assertNotNull(coordinate);
        assertEquals(new DependencyCoordinate("com.example", "demo-artifact", "1.0.0", "sources"), coordinate);
    }

    @Test
    void parseReturnsNullWhenTooFewSegmentsProvided() {
        assertNull(DependencyCoordinate.parse("com.example:demo-artifact"));
    }

    @Test
    void parseRejectsNullInput() {
        assertThrows(NullPointerException.class, () -> DependencyCoordinate.parse(null));
    }

    @Test
    void canonicalConstructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new DependencyCoordinate(null, "artifact", "1.0.0", null));
        assertThrows(NullPointerException.class, () -> new DependencyCoordinate("com.example", null, "1.0.0", null));
        assertThrows(NullPointerException.class, () -> new DependencyCoordinate("com.example", "artifact", null, null));
    }

    @Test
    void convenienceConstructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new DependencyCoordinate(null, "artifact", "1.0.0"));
        assertThrows(NullPointerException.class, () -> new DependencyCoordinate("com.example", null, "1.0.0"));
        assertThrows(NullPointerException.class, () -> new DependencyCoordinate("com.example", "artifact", null));
    }
}
