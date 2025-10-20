package de.jexcellence.dependency.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DependencyTest {

    @Test
    void canonicalConstructorPopulatesAllFields() {
        final Dependency dependency = new Dependency(
                "com.example",
                "demo-artifact",
                "1.2.3",
                "classifier",
                "custom-repo"
        );

        assertEquals("com.example", dependency.groupId());
        assertEquals("demo-artifact", dependency.artifactId());
        assertEquals("1.2.3", dependency.version());
        assertEquals("classifier", dependency.classifier());
        assertEquals("custom-repo", dependency.repository());
    }

    @Test
    void convenienceConstructorDefaultsOptionalFieldsToNull() {
        final Dependency dependency = new Dependency("com.example", "demo-artifact", "1.2.3");

        assertEquals("com.example", dependency.groupId());
        assertEquals("demo-artifact", dependency.artifactId());
        assertEquals("1.2.3", dependency.version());
        assertNull(dependency.classifier());
        assertNull(dependency.repository());
    }

    @Test
    void toFileNameIncludesClassifierWhenPresent() {
        final Dependency dependency = new Dependency("com.example", "demo-artifact", "1.2.3", "sources", null);

        assertEquals("demo-artifact-1.2.3-sources.jar", dependency.toFileName());
    }

    @Test
    void toFileNameOmitsClassifierWhenAbsent() {
        final Dependency dependency = new Dependency("com.example", "demo-artifact", "1.2.3");

        assertEquals("demo-artifact-1.2.3.jar", dependency.toFileName());
    }

    @Test
    void toRepositoryPathBuildsExpectedStructure() {
        final Dependency dependency = new Dependency("com.example", "demo-artifact", "1.2.3", "sources", null);

        assertEquals("com/example/demo-artifact/1.2.3/demo-artifact-1.2.3-sources.jar", dependency.toRepositoryPath());
    }

    @Test
    void toGavStringIncludesClassifierWhenPresent() {
        final Dependency dependency = new Dependency("com.example", "demo-artifact", "1.2.3", "sources", null);

        assertEquals("com.example:demo-artifact:1.2.3:sources", dependency.toGavString());
    }

    @Test
    void toGavStringOmitsClassifierWhenAbsent() {
        final Dependency dependency = new Dependency("com.example", "demo-artifact", "1.2.3");

        assertEquals("com.example:demo-artifact:1.2.3", dependency.toGavString());
    }

    @Test
    void parseRetainsClassifierWhenProvided() {
        final Dependency dependency = Dependency.parse("com.example:demo-artifact:1.2.3:sources");

        assertEquals("com.example", dependency.groupId());
        assertEquals("demo-artifact", dependency.artifactId());
        assertEquals("1.2.3", dependency.version());
        assertEquals("sources", dependency.classifier());
        assertNull(dependency.repository());
    }

    @Test
    void parseThrowsWhenFormatIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Dependency.parse("invalid-format"));
        assertThrows(IllegalArgumentException.class, () -> Dependency.parse("only:two"));
    }

    @Test
    void nullGuardsThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Dependency(null, "demo-artifact", "1.2.3"));
        assertThrows(NullPointerException.class, () -> new Dependency("com.example", null, "1.2.3"));
        assertThrows(NullPointerException.class, () -> new Dependency("com.example", "demo-artifact", null));
        assertThrows(NullPointerException.class, () -> Dependency.parse(null));
    }
}
