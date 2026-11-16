package de.jexcellence.dependency.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation") // The retired constants are referenced on purpose.
class RepositoryTypeTest {

    @Test
    @DisplayName("search order starts with Maven Central and skips retired entries")
    void searchOrderSkipsRetired() {
        final List<RepositoryType> order = RepositoryType.searchOrder();

        assertEquals(RepositoryType.MAVEN_CENTRAL, order.get(0));
        assertTrue(order.stream().noneMatch(RepositoryType::isRetired));
        for (final RepositoryType retired : List.of(
                RepositoryType.MAVEN_CENTRAL_REPO1,
                RepositoryType.APACHE_MAVEN,
                RepositoryType.SONATYPE_OSS,
                RepositoryType.SONATYPE_OSS_SNAPSHOTS,
                RepositoryType.JCENTER)) {
            assertTrue(retired.isRetired(), retired.name());
            assertFalse(order.contains(retired), retired.name());
        }
    }

    @Test
    @DisplayName("search order contacts every host at most once")
    void searchOrderHasNoDuplicateHosts() {
        final Set<String> urls = new HashSet<>();
        for (final RepositoryType repository : RepositoryType.searchOrder()) {
            assertTrue(urls.add(repository.getBaseUrl()), repository.name());
        }
    }

    @Test
    @DisplayName("Maven Central no longer points to the dead central.maven.org host")
    void mavenCentralUsesLiveHost() {
        assertEquals("https://repo1.maven.org/maven2/", RepositoryType.MAVEN_CENTRAL.getBaseUrl());
    }

    @Test
    @DisplayName("search order cannot be modified by callers")
    void searchOrderIsUnmodifiable() {
        final List<RepositoryType> order = RepositoryType.searchOrder();

        assertThrows(UnsupportedOperationException.class, () -> order.add(RepositoryType.JCENTER));
    }

    @Test
    @DisplayName("every base URL uses HTTPS and ends with a slash")
    void baseUrlsAreNormalised() {
        for (final RepositoryType repository : RepositoryType.values()) {
            assertTrue(repository.getBaseUrl().startsWith("https://"), repository.name());
            assertTrue(repository.getBaseUrl().endsWith("/"), repository.name());
        }
    }
}
