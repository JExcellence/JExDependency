package de.jexcellence.dependency.loader;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptorParserTest {

    private static final String HASH = "a".repeat(64);
    private final List<String> warnings = new ArrayList<>();

    private DependencyDescriptor parse(final String yaml) throws IOException {
        return DescriptorParser.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), warnings::add);
    }

    @Test
    @DisplayName("reads the plain list format of earlier versions")
    void legacyFormat() throws IOException {
        final DependencyDescriptor descriptor = parse("""
                # comment
                dependencies:
                  - "com.google.code.gson:gson:2.13.1"
                  - org.slf4j:slf4j-api:2.0.17  # trailing comment
                """);

        assertEquals(List.of("com.google.code.gson:gson:2.13.1", "org.slf4j:slf4j-api:2.0.17"),
                descriptor.dependencies());
        assertTrue(descriptor.repositories().isEmpty());
        assertFalse(descriptor.communityRepositories());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("reads repositories, the community switch, excludes, pins and hashes")
    void fullFormat() throws IOException {
        final DependencyDescriptor descriptor = parse("""
                repositories:
                  - https://repo.example.org/releases/
                  - https://repo.example.org/releases
                community-repositories: true
                relocation-excludes:
                  - org.hibernate
                dependencies:
                  - com.example:tool:1.0 @ https://private.example.org/maven
                  - com.example:pinned:2.0 sha256=%s
                  - com.example:both:3.0 @https://private.example.org/maven SHA256=%s
                """.formatted(HASH, HASH.toUpperCase()));

        assertEquals(List.of("https://repo.example.org/releases"), descriptor.repositories());
        assertTrue(descriptor.communityRepositories());
        assertEquals(List.of("org.hibernate"), descriptor.relocationExcludes());

        final List<DependencyEntry> entries = descriptor.entries();
        assertEquals(3, entries.size());
        assertEquals(new DependencyCoordinate("com.example", "tool", "1.0"), entries.get(0).coordinate());
        assertEquals("https://private.example.org/maven", entries.get(0).repository());
        assertNull(entries.get(0).sha256());
        assertNull(entries.get(1).repository());
        assertEquals(HASH, entries.get(1).sha256());
        assertEquals("https://private.example.org/maven", entries.get(2).repository());
        assertEquals(HASH, entries.get(2).sha256());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    @DisplayName("skips invalid lines with a warning instead of failing")
    void invalidLines() throws IOException {
        final DependencyDescriptor descriptor = parse("""
                repositories:
                  - ftp://repo.example.org
                  - https://user:secret@repo.example.org/maven
                relocation-excludes:
                  - not a package
                dependencies:
                  - not-a-coordinate
                  - com.example:bad-hash:1.0 sha256=1234
                  - com.example:bad-repo:1.0 @ file:///etc
                  - com.example:good:1.0 --unknown
                """);

        assertTrue(descriptor.repositories().isEmpty());
        assertTrue(descriptor.relocationExcludes().isEmpty());
        assertEquals(List.of("com.example:good:1.0"), descriptor.dependencies());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("credentials")), warnings.toString());
        assertTrue(warnings.size() >= 7, warnings.toString());
    }

    @Test
    @DisplayName("keeps '#' inside quotes and reads indented section keys")
    void quotesAndIndentation() throws IOException {
        final DependencyDescriptor descriptor = parse("""
                  dependencies:
                  - "com.example:lib:1.0 @ https://repo.example.org/a#b"
                """);

        assertEquals("https://repo.example.org/a#b", descriptor.entries().get(0).repository());
    }

    @Test
    @DisplayName("reads the relocation prefix and explicit relocations")
    void relocationSettings() throws IOException {
        final DependencyDescriptor descriptor = parse("""
                relocation-prefix: com.example.plugin.libs
                relocations:
                  - com.google.gson => com.example.plugin.libs.gson
                  - broken
                """);

        assertEquals("com.example.plugin.libs", descriptor.relocationPrefix());
        assertEquals(java.util.Map.of("com.google.gson", "com.example.plugin.libs.gson"), descriptor.relocations());
        assertEquals(1, warnings.size(), warnings.toString());
    }
}
