package de.jexcellence.dependency.repository;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryTypeTest {

    private static final DependencyCoordinate COORDINATE = new DependencyCoordinate(
            "de.jexcellence",
            "dependency-test",
            "1.2.3"
    );

    @Test
    void getBaseUrlReturnsNormalisedUrlsForMultipleRepositories() {
        assertEquals("https://central.maven.org/maven2/", RepositoryType.MAVEN_CENTRAL.getBaseUrl());
        assertEquals("https://oss.sonatype.org/content/groups/public/", RepositoryType.SONATYPE_OSS.getBaseUrl());
        assertEquals("https://repo.tcoded.com/releases/", RepositoryType.TCODED.getBaseUrl());
    }

    @Test
    void buildUrlCombinesBaseUrlAndCoordinatePath() {
        final String repositoryPath = COORDINATE.toRepositoryPath();
        final Map<RepositoryType, String> expectations = Map.of(
                RepositoryType.MAVEN_CENTRAL, "https://central.maven.org/maven2/" + repositoryPath,
                RepositoryType.JITPACK, "https://jitpack.io/" + repositoryPath,
                RepositoryType.GRADLE_PLUGINS, "https://plugins.gradle.org/m2/" + repositoryPath
        );

        expectations.forEach((type, expected) -> assertEquals(expected, type.buildUrl(COORDINATE)));
    }

    @Test
    void buildPathAssemblesExpectedJarLocation() {
        final String expected = "https://repo.maven.apache.org/maven2/de/jexcellence/dependency-test/1.2.3/dependency-test-1.2.3.jar";

        assertEquals(expected, RepositoryType.APACHE_MAVEN.buildPath("de.jexcellence", "dependency-test", "1.2.3"));
    }

    @Test
    void buildPathRejectsBlankCoordinateSegments() {
        assertThrows(IllegalArgumentException.class, () -> RepositoryType.PAPERMC.buildPath(" ", "dependency-test", "1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> RepositoryType.PAPERMC.buildPath("de.jexcellence", "", "1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> RepositoryType.PAPERMC.buildPath("de.jexcellence", "dependency-test", ""));
    }
}
