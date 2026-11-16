package de.jexcellence.dependency.loader;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Records which jars the Paper plugin loader put on a plugin's classpath, so the plugin side can inject exactly
 * that set instead of every jar found in a folder. The file lives in the plugin's {@code libraries/} folder and
 * holds one absolute path per line.
 */
@ApiStatus.Internal
public final class LoaderClasspathFile {

    /** Name of the per-plugin libraries folder inside the plugin's data folder. */
    public static final String LIBRARIES_DIRECTORY = "libraries";

    /** File name inside the plugin's {@code libraries/} folder. */
    public static final String FILE_NAME = "paper-loader-classpath.txt";

    private LoaderClasspathFile() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Writes the list of loaded jars, replacing any earlier list.
     *
     * @param librariesDirectory the plugin's {@code libraries/} folder
     * @param jars               jars that were added to the classpath
     * @throws IOException if the file cannot be written
     */
    public static void write(@NotNull final Path librariesDirectory, @NotNull final List<Path> jars) throws IOException {
        Files.createDirectories(librariesDirectory);
        final List<String> lines = new ArrayList<>(jars.size());
        for (final Path jar : jars) {
            lines.add(jar.toAbsolutePath().normalize().toString());
        }

        final Path target = librariesDirectory.resolve(FILE_NAME);
        final Path tempFile = Files.createTempFile(librariesDirectory, FILE_NAME, ".tmp");
        try {
            Files.write(tempFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException exception) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Reads the recorded jars. Blank lines, unparsable paths and jars that no longer exist are skipped.
     *
     * @param librariesDirectory the plugin's {@code libraries/} folder
     * @return recorded jars that still exist; empty when nothing was recorded
     * @throws IOException if the file exists but cannot be read
     */
    public static @NotNull List<Path> read(@NotNull final Path librariesDirectory) throws IOException {
        final Path file = librariesDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return List.of();
        }

        final List<Path> jars = new ArrayList<>();
        for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                final Path jar = Path.of(trimmed);
                if (Files.isRegularFile(jar)) {
                    jars.add(jar);
                }
            } catch (final InvalidPathException exception) {
                // Skip lines that are not paths on this system.
            }
        }
        return jars;
    }
}
