package de.jexcellence.core.stats;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PayloadSignerTest {

    @Test
    void producesExpectedHmacSha256ForKnownVector() {
        // RFC 4231 test case 1: key = "\x0b"*20, data = "Hi There"
        final StringBuilder keyBuilder = new StringBuilder();
        for (int i = 0; i < 20; i++) keyBuilder.append((char) 0x0b);
        final PayloadSigner signer = new PayloadSigner(keyBuilder.toString());
        final String sig = signer.sign("Hi There".getBytes(StandardCharsets.UTF_8));
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7", sig);
    }

    @Test
    void differentSecretProducesDifferentSignature() {
        final byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        final String a = new PayloadSigner("secret-a").sign(body);
        final String b = new PayloadSigner("secret-b").sign(body);
        assertNotEquals(a, b);
    }

    @Test
    void differentBodyProducesDifferentSignature() {
        final PayloadSigner signer = new PayloadSigner("same-secret");
        final String a = signer.sign("body-a".getBytes(StandardCharsets.UTF_8));
        final String b = signer.sign("body-b".getBytes(StandardCharsets.UTF_8));
        assertNotEquals(a, b);
    }

    @Test
    void signatureIsDeterministic() {
        final PayloadSigner signer = new PayloadSigner("same-secret");
        final byte[] body = "deterministic".getBytes(StandardCharsets.UTF_8);
        assertEquals(signer.sign(body), signer.sign(body));
    }
}
