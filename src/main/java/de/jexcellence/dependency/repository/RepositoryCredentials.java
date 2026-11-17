package de.jexcellence.dependency.repository;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Credentials for private repositories, read from environment variables on the server so they never ship inside a
 * plugin jar.
 *
 * <p>For a repository on {@code repo.example.org}, set {@code JEDEPENDENCY_AUTH_REPO_EXAMPLE_ORG} to either
 * {@code user:password} (sent as HTTP Basic) or {@code Bearer <token>}. The variable name is {@value #PREFIX}
 * followed by the host name in upper case with every character other than a letter or digit replaced by
 * {@code _}. Credentials are only sent over {@code https} and only to that exact host, never after a redirect to
 * another host.</p>
 */
public final class RepositoryCredentials {

    /** Prefix of the environment variables that hold credentials. */
    public static final String PREFIX = "JEDEPENDENCY_AUTH_";

    private final UnaryOperator<String> environment;

    /**
     * Creates a lookup backed by the given environment.
     *
     * @param environment environment variable lookup, usually {@code System::getenv}
     */
    public RepositoryCredentials(@NotNull final UnaryOperator<String> environment) {
        this.environment = environment;
    }

    /**
     * Returns the environment variable that holds credentials for a host.
     *
     * @param host repository host name
     * @return variable name
     */
    public static @NotNull String variableFor(@NotNull final String host) {
        return PREFIX + host.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
    }

    /**
     * Returns the {@code Authorization} header value for a request, if credentials are configured for its host.
     *
     * @param uri request URI
     * @return header value, or {@code null} when the request must go out without credentials
     */
    public @Nullable String authorizationFor(@NotNull final URI uri) {
        if (uri.getHost() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }
        final String value = environment.apply(variableFor(uri.getHost()));
        if (value == null || value.isBlank()) {
            return null;
        }
        final String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed;
        }
        if (trimmed.indexOf(':') <= 0) {
            return null;
        }
        return "Basic " + Base64.getEncoder().encodeToString(trimmed.getBytes(StandardCharsets.UTF_8));
    }
}
