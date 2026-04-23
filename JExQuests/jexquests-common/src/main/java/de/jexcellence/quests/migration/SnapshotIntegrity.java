package de.jexcellence.quests.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.quests.api.PlayerSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic SHA-256 hash over a {@link PlayerSnapshot}'s payload,
 * used as a tamper-detection stamp. The hash is computed over the
 * JSON encoding of the snapshot with {@code integrityHash} set to
 * {@code null} — that way a snapshot signs every other field
 * including itself's metadata (schema version, source server,
 * exported-at timestamp, player identity, and all state rows).
 *
 * <p>On the import side, a receiver strips the loaded hash, recomputes
 * the digest over the remaining payload, and compares — any mismatch
 * indicates the file was edited (deliberately or via a botched
 * transfer) since export.
 *
 * <p>Snapshots with a {@code null} {@code integrityHash} (e.g. schema
 * v1 snapshots produced before hashing was introduced) are treated as
 * unsigned and pass verification unconditionally — that's intentional,
 * so old exports can still be imported.
 */
final class SnapshotIntegrity {

    private static final String ALGORITHM = "SHA-256";

    private SnapshotIntegrity() {
    }

    /**
     * Compute the hex-encoded SHA-256 of the snapshot with its
     * {@code integrityHash} zeroed out. Never returns {@code null}.
     */
    static @NotNull String hash(@NotNull PlayerSnapshot snapshot, @NotNull ObjectMapper mapper) {
        try {
            final byte[] payload = mapper.writeValueAsBytes(snapshot.withIntegrityHash(null));
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("canonical serialization failed: " + ex.getMessage(), ex);
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    /**
     * Verifies the snapshot's embedded {@code integrityHash} against
     * a fresh recomputation. Returns {@code true} when the snapshot
     * has no embedded hash (unsigned / legacy) or when the hash
     * matches.
     */
    static boolean verify(@NotNull PlayerSnapshot snapshot, @NotNull ObjectMapper mapper) {
        final String embedded = snapshot.integrityHash();
        if (embedded == null || embedded.isBlank()) return true;
        final String fresh = hash(snapshot, mapper);
        return constantTimeEquals(embedded, fresh);
    }

    /**
     * Detect whether a snapshot was signed — {@code false} for legacy
     * v1 files and for v2 files whose exporter chose not to sign.
     */
    static boolean isSigned(@Nullable PlayerSnapshot snapshot) {
        return snapshot != null
                && snapshot.integrityHash() != null
                && !snapshot.integrityHash().isBlank();
    }

    private static boolean constantTimeEquals(@NotNull String a, @NotNull String b) {
        final byte[] aBytes = a.getBytes(StandardCharsets.US_ASCII);
        final byte[] bBytes = b.getBytes(StandardCharsets.US_ASCII);
        if (aBytes.length != bBytes.length) return false;
        int diff = 0;
        for (int i = 0; i < aBytes.length; i++) diff |= aBytes[i] ^ bBytes[i];
        return diff == 0;
    }
}
