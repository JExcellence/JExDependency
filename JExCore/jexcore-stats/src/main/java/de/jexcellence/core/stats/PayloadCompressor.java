package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP helper for batch bodies.
 */
final class PayloadCompressor {

    private PayloadCompressor() {
    }

    static byte @NotNull [] gzip(byte @NotNull [] raw) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(raw.length / 2);
             final GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(raw);
            gzip.finish();
            return baos.toByteArray();
        } catch (final IOException ex) {
            throw new IllegalStateException("gzip failed", ex);
        }
    }
}
