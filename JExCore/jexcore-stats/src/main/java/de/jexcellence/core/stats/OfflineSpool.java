package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Disk-backed spool for batches that couldn't be delivered. Each batch
 * lands in one file named {@code <timestamp>_<batchId>.bin}. Replay reads
 * oldest-first.
 */
final class OfflineSpool {

    private final Path root;

    OfflineSpool(@NotNull Path root) throws IOException {
        this.root = root;
        Files.createDirectories(root);
    }

    void write(@NotNull BatchPayload batch) throws IOException {
        final String name = batch.createdAt().toEpochMilli() + "_" + batch.batchId() + ".bin";
        Files.write(this.root.resolve(name), batch.body(),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    @NotNull List<Path> pending() throws IOException {
        try (final Stream<Path> stream = Files.list(this.root)) {
            return stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        }
    }

    byte @NotNull [] read(@NotNull Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    void delete(@NotNull Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    @NotNull Path root() {
        return this.root;
    }
}
