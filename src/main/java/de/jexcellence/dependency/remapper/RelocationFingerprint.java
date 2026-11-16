package de.jexcellence.dependency.remapper;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Names the folder that holds jars remapped with one particular relocation set. A remapped jar is only valid for
 * the relocations it was produced with: adding a library can add a relocation that already remapped siblings must
 * also apply. Keying the output folder on the relocation set means a changed set never reuses an old copy.
 */
@ApiStatus.Internal
public final class RelocationFingerprint {

    /**
     * Version of the remapping output. Raise it when the remapper changes how it rewrites jars, so copies made by
     * an older version are not reused.
     */
    static final int REMAP_FORMAT = 1;

    private static final int FINGERPRINT_BYTES = 8;

    private RelocationFingerprint() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns a short, stable, file-name-safe fingerprint of the relocation set. The iteration order of
     * {@code relocations} does not matter.
     *
     * @param relocations mapping from original package to relocated package
     * @return 16 lowercase hex characters
     */
    public static @NotNull String of(@NotNull final Map<String, String> relocations) {
        final StringBuilder canonical = new StringBuilder("format=").append(REMAP_FORMAT).append('\n');
        for (final Map.Entry<String, String> entry : new TreeMap<>(relocations).entrySet()) {
            canonical.append(entry.getKey()).append("=>").append(entry.getValue()).append('\n');
        }

        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, FINGERPRINT_BYTES);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", exception);
        }
    }
}
