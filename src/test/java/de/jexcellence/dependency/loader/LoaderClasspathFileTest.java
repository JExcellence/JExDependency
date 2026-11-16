package de.jexcellence.dependency.loader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoaderClasspathFileTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("round-trips the recorded jars")
    void roundTrip() throws IOException {
        final Path libraries = tempDir.resolve("libraries");
        final Path first = Files.createFile(tempDir.resolve("first.jar"));
        final Path second = Files.createFile(tempDir.resolve("second.jar"));

        LoaderClasspathFile.write(libraries, List.of(first, second));

        assertEquals(List.of(first.toAbsolutePath().normalize(), second.toAbsolutePath().normalize()),
                LoaderClasspathFile.read(libraries));
    }

    @Test
    @DisplayName("replaces the previous record instead of appending")
    void replacesPreviousRecord() throws IOException {
        final Path libraries = tempDir.resolve("libraries");
        final Path old = Files.createFile(tempDir.resolve("old.jar"));
        final Path current = Files.createFile(tempDir.resolve("current.jar"));

        LoaderClasspathFile.write(libraries, List.of(old, current));
        LoaderClasspathFile.write(libraries, List.of(current));

        assertEquals(List.of(current.toAbsolutePath().normalize()), LoaderClasspathFile.read(libraries));
    }

    @Test
    @DisplayName("skips jars that no longer exist")
    void skipsMissingJars() throws IOException {
        final Path libraries = tempDir.resolve("libraries");
        final Path kept = Files.createFile(tempDir.resolve("kept.jar"));
        final Path removed = Files.createFile(tempDir.resolve("removed.jar"));

        LoaderClasspathFile.write(libraries, List.of(kept, removed));
        Files.delete(removed);

        assertEquals(List.of(kept.toAbsolutePath().normalize()), LoaderClasspathFile.read(libraries));
    }

    @Test
    @DisplayName("returns an empty list when nothing was recorded")
    void emptyWithoutRecord() throws IOException {
        assertTrue(LoaderClasspathFile.read(tempDir.resolve("libraries")).isEmpty());
    }

    @Test
    @DisplayName("leaves no temporary files behind")
    void noTemporaryFiles() throws IOException {
        final Path libraries = tempDir.resolve("libraries");
        LoaderClasspathFile.write(libraries, List.of(Files.createFile(tempDir.resolve("a.jar"))));

        try (var files = Files.list(libraries)) {
            assertEquals(List.of(LoaderClasspathFile.FILE_NAME),
                    files.map(path -> path.getFileName().toString()).toList());
        }
    }
}
