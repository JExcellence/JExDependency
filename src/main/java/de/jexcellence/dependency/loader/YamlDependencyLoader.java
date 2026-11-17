package de.jexcellence.dependency.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads a plugin's {@code dependencies.yml} from its jar or class path. A server-specific file
 * ({@code dependency/paper/dependencies.yml} or {@code dependency/spigot/dependencies.yml}) wins over the generic
 * {@code dependency/dependencies.yml}. See {@link DependencyDescriptor} for the format.
 */
public class YamlDependencyLoader {

    private static final String LOGGER_NAME = "JExDependency";
    // Classpath resource locations of the bundled dependency descriptors.
    // Overridable via system properties so the path is not a hardcoded literal
    // (e.g. -Djedependency.path.paper=/custom/deps.yml).
    private static final String DEPENDENCIES_YAML_PATH =
            System.getProperty("jedependency.path.default", "/dependency/dependencies.yml");
    private static final String PAPER_DEPENDENCIES_PATH =
            System.getProperty("jedependency.path.paper", "/dependency/paper/dependencies.yml");
    private static final String SPIGOT_DEPENDENCIES_PATH =
            System.getProperty("jedependency.path.spigot", "/dependency/spigot/dependencies.yml");

    private final Logger logger;
    private final ServerType serverType;

    /**
     * Creates a loader that immediately determines the current server type for subsequent lookups.
     */
    public YamlDependencyLoader() {
        this.logger = Logger.getLogger(LOGGER_NAME);
        this.serverType = detectServerType();
    }

    /**
     * Loads the descriptor bundled next to the given class.
     *
     * @param anchorClass class whose class loader resolves the YAML resources
     * @return the descriptor, or {@code null} when no file exists
     */
    public @Nullable DependencyDescriptor loadDescriptor(@NotNull final Class<?> anchorClass) {
        final String serverSpecificPath = getServerSpecificPath();
        if (serverSpecificPath != null) {
            final DependencyDescriptor descriptor = loadFromPath(anchorClass, serverSpecificPath);
            if (descriptor != null) {
                logger.log(Level.FINE, () -> "Loaded " + descriptor.entries().size()
                        + " dependencies from " + serverSpecificPath);
                return descriptor;
            }
        }

        final DependencyDescriptor descriptor = loadFromPath(anchorClass, DEPENDENCIES_YAML_PATH);
        if (descriptor != null) {
            logger.log(Level.FINE, () -> "Loaded " + descriptor.entries().size()
                    + " dependencies from " + DEPENDENCIES_YAML_PATH);
        } else {
            logger.log(Level.FINE, "No dependency configuration found");
        }
        return descriptor;
    }

    /**
     * Loads the descriptor packaged inside a plugin jar. A file without dependencies is skipped in favour of the
     * next candidate.
     *
     * @param jarPath plugin jar
     * @return the descriptor, or {@code null} when the jar has no descriptor with dependencies
     */
    public @Nullable DependencyDescriptor loadDescriptorFromJar(@NotNull final Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            final String serverSpecificPath = getServerSpecificPath();
            if (serverSpecificPath != null) {
                final DependencyDescriptor descriptor = readEntry(jarFile, serverSpecificPath);
                if (descriptor != null && !descriptor.isEmpty()) {
                    return descriptor;
                }
            }
            final DependencyDescriptor descriptor = readEntry(jarFile, DEPENDENCIES_YAML_PATH);
            if (descriptor != null && !descriptor.isEmpty()) {
                return descriptor;
            }
            logger.log(Level.FINE, "No dependency configuration found in plugin JAR");
            return null;
        } catch (final IOException | SecurityException exception) {
            logger.log(Level.WARNING, exception, () -> "Failed to load dependencies from plugin JAR: " + jarPath);
            return null;
        }
    }

    /**
     * Loads the dependency coordinates bundled next to the given class.
     *
     * @param anchorClass class whose class loader resolves the YAML resources
     * @return coordinate strings, or {@code null} when no file exists
     */
    public @Nullable List<String> loadDependencies(@NotNull final Class<?> anchorClass) {
        final DependencyDescriptor descriptor = loadDescriptor(anchorClass);
        return descriptor == null ? null : descriptor.dependencies();
    }

    /**
     * Loads the dependency coordinates packaged inside a plugin jar.
     *
     * @param jarPath plugin jar
     * @return coordinate strings, or {@code null} when the jar has no descriptor with dependencies
     */
    public @Nullable List<String> loadDependenciesFromJar(@NotNull final Path jarPath) {
        final DependencyDescriptor descriptor = loadDescriptorFromJar(jarPath);
        return descriptor == null ? null : descriptor.dependencies();
    }

    private @Nullable DependencyDescriptor readEntry(@NotNull final JarFile jarFile, @NotNull final String path)
            throws IOException {
        final JarEntry entry = jarFile.getJarEntry(path.startsWith("/") ? path.substring(1) : path);
        if (entry == null) {
            return null;
        }
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            return parse(inputStream, path);
        }
    }

    private @Nullable DependencyDescriptor loadFromPath(
            @NotNull final Class<?> anchorClass,
            @NotNull final String path
    ) {
        try (InputStream inputStream = anchorClass.getResourceAsStream(path)) {
            if (inputStream == null) {
                logger.log(Level.FINEST, () -> "No configuration found at: " + path);
                return null;
            }
            return parse(inputStream, path);
        } catch (final IOException exception) {
            logger.log(Level.WARNING, exception, () -> "Failed to load dependencies from: " + path);
            return null;
        }
    }

    private @NotNull DependencyDescriptor parse(@NotNull final InputStream inputStream, @NotNull final String path)
            throws IOException {
        return DescriptorParser.parse(inputStream, warning -> logger.log(Level.WARNING, "{0} ({1})",
                new Object[]{warning, path}));
    }

    private @Nullable String getServerSpecificPath() {
        return switch (serverType) {
            case PAPER -> PAPER_DEPENDENCIES_PATH;
            case SPIGOT -> SPIGOT_DEPENDENCIES_PATH;
        };
    }

    private @NotNull ServerType detectServerType() {
        if (isPaperServer()) {
            logger.log(Level.FINE, "Detected Paper server");
            return ServerType.PAPER;
        }

        logger.log(Level.FINE, "Detected Spigot/CraftBukkit server");
        return ServerType.SPIGOT;
    }

    private boolean isPaperServer() {
        return isClassPresent("com.destroystokyo.paper.PaperConfig")
                || isClassPresent("io.papermc.paper.configuration.Configuration");
    }

    private boolean isClassPresent(@NotNull final String className) {
        try {
            classForName(className);
            return true;
        } catch (final ClassNotFoundException exception) {
            return false;
        }
    }

    static @NotNull Class<?> classForName(@NotNull final String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    private enum ServerType {
        PAPER,
        SPIGOT
    }
}
