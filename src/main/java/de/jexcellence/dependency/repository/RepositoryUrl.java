package de.jexcellence.dependency.repository;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Validates repository URLs from configuration: system properties, environment variables and
 * {@code dependencies.yml}.
 */
public final class RepositoryUrl {

    private RepositoryUrl() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks a configured repository URL. Accepted are {@code https} URLs and, with a warning, plain {@code http}
     * URLs. URLs that carry a user name or password are refused: credentials do not belong in a plugin jar or a
     * command line.
     *
     * @param value    configured value, may be {@code null} or blank for "not configured"
     * @param source   where the value came from, used in warnings
     * @param warnings receives a message for every refused value and for plain {@code http}
     * @return the trimmed URL without a trailing slash, or {@code null} when the value is unset or refused
     */
    public static @Nullable String parse(
            @Nullable final String value,
            @NotNull final String source,
            @NotNull final Consumer<String> warnings
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        final String trimmed = value.trim();
        final URI uri;
        try {
            uri = new URI(trimmed);
        } catch (final URISyntaxException exception) {
            warnings.accept("Ignoring " + source + ": not a valid URL: " + trimmed);
            return null;
        }

        final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (uri.getHost() == null || !("https".equals(scheme) || "http".equals(scheme))) {
            warnings.accept("Ignoring " + source + ": expected an https URL, got " + trimmed);
            return null;
        }
        if (uri.getRawUserInfo() != null) {
            warnings.accept("Ignoring " + source + ": the URL contains credentials; use a repository without "
                    + "them or configure credentials outside the plugin");
            return null;
        }
        if ("http".equals(scheme)) {
            warnings.accept(source + " uses plain http; downloads from " + trimmed + " are not encrypted");
        }

        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
