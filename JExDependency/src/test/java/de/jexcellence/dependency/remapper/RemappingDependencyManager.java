package de.jexcellence.dependency.remapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Dummy implementation used in tests to satisfy reflective lookups performed by {@code PaperPluginLoader}.
 * It copies jars directly without applying bytecode transforms so tests can
 * focus on directory selection logic without depending on the real remapper implementation.
 */
public final class RemappingDependencyManager {

    private static volatile boolean failRemapping;
    public RemappingDependencyManager(final Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
    }

    public static void setFailRemapping(final boolean fail) {
        failRemapping = fail;
    }

    public static void reset() {
        failRemapping = false;
    }

    public void relocate(final String from, final String to) {
        // no-op in tests
    }

    public void remap(final Path input, final Path output) throws IOException {
        if (failRemapping) {
            throw new IOException("Configured remapper failure");
        }

        Files.createDirectories(output.getParent());
        Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
    }

}
