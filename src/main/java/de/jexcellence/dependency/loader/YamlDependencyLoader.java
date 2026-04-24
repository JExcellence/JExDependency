package de.jexcellence.dependency.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads dependency coordinate strings from YAML descriptors bundled alongside plugins or within their jars. The loader.
 * honours server-specific overrides (Paper vs Spigot) and parses simple list-style YAML without requiring additional
 * libraries during bootstrap.
 */
public class YamlDependencyLoader {

    private static final String LOGGER_NAME = "JExDependency";
    private static final String DEPENDENCIES_YAML_PATH = "/dependency/dependencies.yml";
    private static final String PAPER_DEPENDENCIES_PATH = "/dependency/paper/dependencies.yml";
    private static final String SPIGOT_DEPENDENCIES_PATH = "/dependency/spigot/dependencies.yml";
    private static final String DEPENDENCIES_SECTION = "dependencies:";
    private static final String LIST_PREFIX = "- ";
    private static final String QUOTE = "\"";
    private static final String COMMENT = "#";
    private static final String LOADED_PREFIX = "Loaded ";

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
     * Loads dependency coordinates from YAML resources bundled alongside the provided anchor class. Server-specific.
     * YAML files are preferred when available and fall back to a generic descriptor.
     *
     * @param anchorClass class whose class loader will be used to resolve the YAML resources
     *
     * @return list of dependency coordinate strings or {@code null} when no descriptor exists
     */
    public @Nullable List<String> loadDependencies(@NotNull final Class<?> anchorClass) {
        final String serverSpecificPath = getServerSpecificPath();
        if (serverSpecificPath != null) {
            final List<String> dependencies = loadFromPath(anchorClass, serverSpecificPath);
            if (dependencies != null) {
                logger.log(Level.FINE, () -> LOADED_PREFIX + dependencies.size() + " dependencies from server-specific config");
                return dependencies;
            }
        }

        final List<String> dependencies = loadFromPath(anchorClass, DEPENDENCIES_YAML_PATH);
        if (dependencies != null) {
            logger.log(Level.FINE, () -> LOADED_PREFIX + dependencies.size() + " dependencies from config");
        } else {
            logger.log(Level.FINE, "No dependency configuration found");
        }

        return dependencies;
    }

    /**
     * Loads dependency coordinates from YAML resources packaged inside the given plugin jar. The method first attempts.
     * server-specific descriptors before falling back to the generic descriptor.
     *
     * @param jarPath path to the plugin jar to inspect
     *
     * @return list of dependency coordinate strings or {@code null} when no descriptor exists in the jar
     */
    public @Nullable List<String> loadDependenciesFromJar(@NotNull final Path jarPath) {
        try (final JarFile jarFile = new JarFile(jarPath.toFile())) {
            final String serverSpecificPath = getServerSpecificPath();
            
            // Try server-specific path first
            if (serverSpecificPath != null) {
                final String entryPath = serverSpecificPath.startsWith("/") 
                        ? serverSpecificPath.substring(1) 
                        : serverSpecificPath;
                final JarEntry entry = jarFile.getJarEntry(entryPath);
                
                if (entry != null) {
                    try (final InputStream inputStream = jarFile.getInputStream(entry)) {
                        final List<String> dependencies = parseDependencies(inputStream);
                        if (!dependencies.isEmpty()) {
                            logger.log(Level.FINE, () -> LOADED_PREFIX + dependencies.size() + " dependencies from plugin JAR");
                            return dependencies;
                        }
                    }
                }
            }
            
            // Fall back to generic path
            final String genericPath = DEPENDENCIES_YAML_PATH.startsWith("/") 
                    ? DEPENDENCIES_YAML_PATH.substring(1) 
                    : DEPENDENCIES_YAML_PATH;
            final JarEntry entry = jarFile.getJarEntry(genericPath);
            
            if (entry != null) {
                try (final InputStream inputStream = jarFile.getInputStream(entry)) {
                    final List<String> dependencies = parseDependencies(inputStream);
                    if (!dependencies.isEmpty()) {
                        logger.log(Level.FINE, () -> LOADED_PREFIX + dependencies.size() + " dependencies from plugin JAR");
                        return dependencies;
                    }
                }
            }
            
            logger.log(Level.FINE, "No dependency configuration found in plugin JAR");
            return null;
            
        } catch (final Exception exception) {
            logger.log(Level.WARNING, exception, () -> "Failed to load dependencies from plugin JAR: " + jarPath);
            return null;
        }
    }

    private @Nullable List<String> loadFromPath(
            @NotNull final Class<?> anchorClass,
            @NotNull final String path
    ) {
        try (final InputStream inputStream = anchorClass.getResourceAsStream(path)) {
            if (inputStream == null) {
                logger.log(Level.FINEST, () -> "No configuration found at: " + path);
                return null;
            }

            return parseDependencies(inputStream);

        } catch (final Exception exception) {
            logger.log(Level.WARNING, exception, () -> "Failed to load dependencies from: " + path);
            return null;
        }
    }

    /**
     * Parses dependencies from a YAML input stream, extracting list items under the dependencies section.
     * Refactored to reduce loop complexity by extracting entry processing logic.
     *
     * @param inputStream the YAML input stream to parse
     * @return list of parsed dependency coordinates
     */
    private @NotNull List<String> parseDependencies(@NotNull final InputStream inputStream) {
        final List<String> dependencies = new ArrayList<>();

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            boolean inDependenciesSection = false;
            String line;

            while ((line = reader.readLine()) != null) {
                final String trimmed = line.trim();

                if (trimmed.equals(DEPENDENCIES_SECTION)) {
                    inDependenciesSection = true;
                    continue;
                }

                if (inDependenciesSection) {
                    if (isEndOfSection(trimmed)) {
                        break;
                    }

                    processDependencyLine(trimmed, dependencies);
                }
            }

        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Error parsing YAML dependencies", exception);
        }

        return dependencies;
    }

    /**
     * Processes a single line from the dependencies section, extracting and adding valid dependencies.
     *
     * @param trimmed      the trimmed line to process
     * @param dependencies the list to add extracted dependencies to
     */
    private void processDependencyLine(
            @NotNull final String trimmed,
            @NotNull final List<String> dependencies
    ) {
        final String dependency = extractDependency(trimmed);
        if (dependency != null) {
            dependencies.add(dependency);
            logger.log(Level.FINEST, () -> "Parsed dependency: " + dependency);
        }
    }

    private boolean isEndOfSection(@NotNull final String line) {
        return !line.isEmpty()
                && !line.startsWith(COMMENT)
                && !line.startsWith(LIST_PREFIX);
    }

    private @Nullable String extractDependency(@NotNull final String line) {
        if (!line.startsWith(LIST_PREFIX)) {
            return null;
        }

        String content = line.substring(LIST_PREFIX.length()).trim();

        if (content.startsWith(QUOTE) && content.endsWith(QUOTE)) {
            content = content.substring(1, content.length() - 1);
        }

        if (content.isEmpty() || content.startsWith(COMMENT)) {
            return null;
        }

        return content;
    }

    private @Nullable String getServerSpecificPath() {
        return switch (serverType) {
            case PAPER -> PAPER_DEPENDENCIES_PATH;
            case SPIGOT -> SPIGOT_DEPENDENCIES_PATH;
            case UNKNOWN -> null;
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
        SPIGOT,
        UNKNOWN
    }
}
