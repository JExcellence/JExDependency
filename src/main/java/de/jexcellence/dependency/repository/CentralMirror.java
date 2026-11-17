package de.jexcellence.dependency.repository;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decides where Maven Central artifacts are downloaded from. Maven Central's terms do not allow using it as a
 * download CDN for end users, and Central rate-limits networks that download too much, so artifacts come from a
 * mirror by default and Central itself is only the fallback.
 *
 * <p>The mirror is chosen in this order:</p>
 * <ol>
 *     <li>the system property {@value #PROPERTY},</li>
 *     <li>the environment variable {@value #PAPER_ENVIRONMENT_VARIABLE}, which Paper also uses for its own
 *     library loader, so one setting covers both,</li>
 *     <li>{@value #DEFAULT_MIRROR}, the Google-hosted Central mirror that Paper uses by default.</li>
 * </ol>
 *
 * <p>Setting the property to Central's own URL ({@code https://repo1.maven.org/maven2}) downloads from Central
 * directly. A value that is not an {@code https} or {@code http} URL is ignored with a warning.</p>
 */
public final class CentralMirror {

    /** System property that sets the Maven Central mirror. */
    public static final String PROPERTY = "jedependency.repository.central";

    /** Environment variable Paper reads for the same purpose; honoured so one setting covers both. */
    public static final String PAPER_ENVIRONMENT_VARIABLE = "PAPER_DEFAULT_CENTRAL_REPOSITORY";

    /** Mirror used when nothing is configured. */
    public static final String DEFAULT_MIRROR = "https://maven-central.storage-download.googleapis.com/maven2";

    private static final Logger LOGGER = Logger.getLogger("JExDependency");

    private CentralMirror() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the mirror configured for this JVM.
     *
     * @return mirror base URL without a trailing slash
     */
    public static @NotNull String configured() {
        final String mirror = resolve(System::getProperty, System::getenv,
                warning -> LOGGER.log(Level.WARNING, warning));
        if (!DEFAULT_MIRROR.equals(mirror)) {
            LOGGER.log(Level.INFO, "Maven Central artifacts are downloaded from {0}", mirror);
        }
        return mirror;
    }

    /**
     * Resolves the mirror from the given sources. Separated from {@link #configured()} so it can be tested
     * without touching the real environment.
     *
     * @param properties  system property lookup
     * @param environment environment variable lookup
     * @param warnings    receives a message for every configured value that is ignored
     * @return mirror base URL without a trailing slash
     */
    static @NotNull String resolve(
            @NotNull final UnaryOperator<String> properties,
            @NotNull final UnaryOperator<String> environment,
            @NotNull final Consumer<String> warnings
    ) {
        final String fromProperty = RepositoryUrl.parse(properties.apply(PROPERTY), "-D" + PROPERTY, warnings);
        if (fromProperty != null) {
            return fromProperty;
        }
        final String fromEnvironment = RepositoryUrl.parse(environment.apply(PAPER_ENVIRONMENT_VARIABLE),
                PAPER_ENVIRONMENT_VARIABLE, warnings);
        return fromEnvironment != null ? fromEnvironment : DEFAULT_MIRROR;
    }
}
