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

public class YamlDependencyLoader {

    private static final String DEPENDENCIES_YAML_PATH = "/dependency/dependencies.yml";
    private static final String PAPER_DEPENDENCIES_PATH = "/dependency/paper/dependencies.yml";
    private static final String SPIGOT_DEPENDENCIES_PATH = "/dependency/spigot/dependencies.yml";
    private static final String DEPENDENCIES_SECTION = "dependencies:";
    private static final String LIST_PREFIX = "- ";
    private static final String QUOTE = "\"";
    private static final String COMMENT = "#";

    private final Logger logger;
    private final ServerType serverType;

    public YamlDependencyLoader() {
        this.logger = Logger.getLogger(getClass().getName());
        this.serverType = detectServerType();
    }

    public @Nullable List<String> loadDependencies(@NotNull final Class<?> anchorClass) {
        final String serverSpecificPath = getServerSpecificPath();
        if (serverSpecificPath != null) {
            final List<String> dependencies = loadFromPath(anchorClass, serverSpecificPath);
            if (dependencies != null) {
                logger.info("Loaded " + dependencies.size() + " dependencies from server-specific configuration");
                return dependencies;
            }
        }

        final List<String> dependencies = loadFromPath(anchorClass, DEPENDENCIES_YAML_PATH);
        if (dependencies != null) {
            logger.info("Loaded " + dependencies.size() + " dependencies from generic configuration");
        } else {
            logger.fine("No YAML dependency configuration found");
        }

        return dependencies;
    }

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
                            logger.info("Loaded " + dependencies.size() + " dependencies from server-specific configuration in plugin JAR");
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
                        logger.info("Loaded " + dependencies.size() + " dependencies from generic configuration in plugin JAR");
                        return dependencies;
                    }
                }
            }
            
            logger.fine("No YAML dependency configuration found in plugin JAR: " + jarPath);
            return null;
            
        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Failed to load dependencies from plugin JAR: " + jarPath, exception);
            return null;
        }
    }

    private @Nullable List<String> loadFromPath(
            @NotNull final Class<?> anchorClass,
            @NotNull final String path
    ) {
        try (final InputStream inputStream = anchorClass.getResourceAsStream(path)) {
            if (inputStream == null) {
                logger.finest("No configuration found at: " + path);
                return null;
            }

            return parseDependencies(inputStream);

        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Failed to load dependencies from: " + path, exception);
            return null;
        }
    }

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

                    final String dependency = extractDependency(trimmed);
                    if (dependency != null) {
                        dependencies.add(dependency);
                        logger.finest("Parsed dependency: " + dependency);
                    }
                }
            }

        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Error parsing YAML dependencies", exception);
        }

        return dependencies;
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
            logger.fine("Detected Paper server");
            return ServerType.PAPER;
        }

        logger.fine("Detected Spigot/CraftBukkit server");
        return ServerType.SPIGOT;
    }

    private boolean isPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (final ClassNotFoundException exception) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (final ClassNotFoundException exception2) {
                return false;
            }
        }
    }

    private enum ServerType {
        PAPER,
        SPIGOT,
        UNKNOWN
    }
}
