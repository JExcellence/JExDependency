package com.destroystokyo.paper;

/**
 * Minimal marker class exposed for tests to ensure Paper-specific code paths
 * are exercised when {@link com.raindropcentral.rplatform.version.ServerEnvironment}
 * performs classpath detection.
 */
public final class ParticleBuilder {

    private ParticleBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }
}
