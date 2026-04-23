package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 signer. Stateless; instances reuse a pre-initialized MAC.
 */
final class PayloadSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    PayloadSigner(@NotNull String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    @NotNull String sign(byte @NotNull [] body) {
        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(this.key);
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (final NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC init failed", ex);
        }
    }
}
