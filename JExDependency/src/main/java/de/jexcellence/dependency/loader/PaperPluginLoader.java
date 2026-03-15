package de.jexcellence.dependency.loader;

import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

/**
 * Paper-specific plugin loader that mirrors the runtime behaviour of {@link de.jexcellence.dependency.JEDependency}.
 * The loader runs before the plugin's lifecycle methods, downloads dependencies declared in bundled YAML files, applies
 * optional remapping/relocation rules and registers the resulting jars with Paper's {@link PluginClasspathBuilder}.
 * It also records its presence via {@code paper.plugin.loader.active} to signal the plugin-side bootstrap to reuse the
 * already downloaded libraries.
 */
@SuppressWarnings({"unused", "UnstableApiUsage"})
public class PaperPluginLoader implements PluginLoader {

    /**
     * Short logger name for clean console output.
     */
    private static final String LOGGER_NAME = "JExDependency";
    /**
     * System property controlling whether remapping should run: {@code auto} (default), {@code true}, {@code false}.
     */
    private static final String REMAP_PROPERTY = "jedependency.remap";
    /**
     * System property containing explicit relocation mappings in {@code from=>to} form separated by commas.
     */
    private static final String RELOCATIONS_PROPERTY = "jedependency.relocations";
    /**
     * System property specifying the prefix used for automatically detected package roots.
     */
    private static final String RELOCATIONS_PREFIX_PROPERTY = "jedependency.relocations.prefix";
    /**
     * System property listing package roots that should be excluded from automatic relocation detection.
     */
    private static final String RELOCATIONS_EXCLUDES_PROPERTY = "jedependency.relocations.excludes";
    /**
     * System property toggled to {@code true} while the Paper loader is active so plugin-side initialisation can react.
     */
    private static final String PAPER_LOADER_PROPERTY = "paper.plugin.loader.active";
    /**
     * Directory name that stores remapped jars when remapping succeeds.
     */
    private static final String REMAPPED_DIRECTORY_NAME = "remapped";
    /**
     * Directory name under the plugin data folder that stores downloaded libraries.
     */
    private static final String LIBRARIES_DIRECTORY_NAME = "libraries";
    /**
     * Minimum size threshold used to reject obviously invalid jar outputs.
     */
    private static final long MINIMUM_JAR_SIZE = 1024L;

    private final DependencyDownloader dependencyDownloader;
    private final YamlDependencyLoader yamlDependencyLoader;
    
    /**
     * Plugin name extracted from the data directory path, used for logging context.
     */
    private String pluginName = "Unknown";
    
    /**
     * Logger instance configured with the plugin name for clear identification.
     */
    private Logger logger;

    /**
     * Creates the loader with fresh downloader and YAML loader instances.
     */
    public PaperPluginLoader() {
        this.logger = createLogger(LOGGER_NAME);
        this.dependencyDownloader = new DependencyDownloader();
        this.yamlDependencyLoader = new YamlDependencyLoader();
    }

    /**
     * Creates a logger with a clean output format that includes the plugin name.
     *
     * @param loggerName the name to use for the logger (typically includes plugin name)
     */
    private Logger createLogger(final String loggerName) {
        final Logger log = Logger.getLogger(loggerName);
        log.setUseParentHandlers(false);
        
        // Remove existing handlers
        for (var handler : log.getHandlers()) {
            log.removeHandler(handler);
        }
        
        // Capture pluginName for use in formatter
        final String currentPluginName = this.pluginName;
        
        // Add console handler with simple format that includes plugin name
        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            /**
             * Executes format.
             */
            @Override
            public String format(final LogRecord record) {
                return String.format("[%s/%s] %s%n", LOGGER_NAME, currentPluginName, record.getMessage());
            }
        });
        handler.setLevel(Level.ALL);
        log.addHandler(handler);
        log.setLevel(Level.INFO);
        
        return log;
    }
    
    /**
     * Extracts the plugin name from the data directory path.
     * The data directory is typically named after the plugin (e.g., "plugins/MyPlugin").
     *
     * @param dataDirectory the plugin's data directory path
     * @return the extracted plugin name, or "Unknown" if extraction fails
     */
    private String extractPluginName(@NotNull final Path dataDirectory) {
        try {
            final Path fileName = dataDirectory.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        } catch (final Exception ignored) {
            // Fall through to default
        }
        return "Unknown";
    }

    /**
     * Prepares the plugin classpath by downloading declared dependencies, optionally remapping them and adding the.
     * resulting jar files to Paper's bootstrap classpath. The method is invoked by Paper on the main server thread
     * during plugin discovery and therefore performs logging rather than throwing checked exceptions.
     *
     * @param classpathBuilder Paper builder used to register {@link JarLibrary} instances
     */
    @Override
    public void classloader(@NotNull final PluginClasspathBuilder classpathBuilder) {
        System.setProperty(PAPER_LOADER_PROPERTY, "true");

        final Path dataDirectory = classpathBuilder.getContext().getDataDirectory().toAbsolutePath();
        final Path pluginSource = classpathBuilder.getContext().getPluginSource();
        final Path librariesDirectory = determineLibrariesDirectory(dataDirectory);
        final Path remappedDirectory = librariesDirectory.resolve(REMAPPED_DIRECTORY_NAME);

        // Extract plugin name from data directory and reinitialize logger with plugin context
        this.pluginName = extractPluginName(dataDirectory);
        this.logger = createLogger(LOGGER_NAME + "." + pluginName);

        try {
            logger.info("Loading dependencies...");

            ensureDirectoryExists(librariesDirectory);
            warnIfNestedLibraries(librariesDirectory);

            initializeDependencies(librariesDirectory.toFile(), pluginSource);

            final Path effectiveDirectory = determineEffectiveDirectory(librariesDirectory, remappedDirectory);
            loadJarFilesIntoClasspath(classpathBuilder, effectiveDirectory);

        } catch (final IOException exception) {
            throw new RuntimeException("Failed to initialize libraries directory: " + librariesDirectory, exception);
        }
    }

    private @NotNull Path determineLibrariesDirectory(@NotNull final Path dataDirectory) {
        return dataDirectory.resolve(LIBRARIES_DIRECTORY_NAME);
    }

    private void ensureDirectoryExists(@NotNull final Path directory) throws IOException {
        Files.createDirectories(directory);

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IOException("Failed to create or access directory: " + directory);
        }
    }

    private void warnIfNestedLibraries(@NotNull final Path librariesDirectory) {
        final Path nested = librariesDirectory.resolve(LIBRARIES_DIRECTORY_NAME);
        if (Files.isDirectory(nested)) {
            logger.warning("Detected nested libraries folder - please clean up: " + nested);
        }
    }

    private void initializeDependencies(@NotNull final File librariesDirectory, @NotNull final Path pluginSource) {
        try {
            // First, try to load dependencies from the plugin JAR itself
            List<String> yamlDependencies = yamlDependencyLoader.loadDependenciesFromJar(pluginSource);
            
            if (yamlDependencies == null || yamlDependencies.isEmpty()) {
                // Fall back to loader's own resources (JEDependency JAR)
                logger.fine("No dependencies in plugin JAR, checking loader resources");
                yamlDependencies = yamlDependencyLoader.loadDependencies(getClass());
            }

            if (yamlDependencies == null || yamlDependencies.isEmpty()) {
                logger.info("No dependencies configured");
                return;
            }

            logger.info("Found " + yamlDependencies.size() + " dependencies");
            processDependencies(yamlDependencies, librariesDirectory);

        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Failed to load dependencies", exception);
        }
    }

    private void processDependencies(
            @NotNull final List<String> dependencies,
            @NotNull final File librariesDirectory
    ) {
        final int total = dependencies.size();
        int completed = 0;
        int failed = 0;
        int lastLoggedPercent = 0;

        for (final String dependencyString : dependencies) {
            try {
                final DependencyCoordinate coordinate = DependencyCoordinate.parse(dependencyString);
                if (coordinate == null) {
                    logger.warning("Invalid dependency: " + dependencyString);
                    failed++;
                    completed++;
                    continue;
                }

                final DownloadResult result = dependencyDownloader.download(coordinate, librariesDirectory);
                completed++;
                
                if (result.success()) {
                    final int percent = (completed * 100) / total;
                    // Log at 25%, 50%, 75%, 100%
                    if (percent >= lastLoggedPercent + 25 || completed == total) {
                        logger.info("Downloading dependencies... " + completed + "/" + total + " (" + percent + "%)");
                        lastLoggedPercent = (percent / 25) * 25;
                    }
                } else {
                    failed++;
                    logger.warning("Failed to download: " + coordinate.artifactId());
                }

            } catch (final Exception exception) {
                completed++;
                failed++;
                logger.log(Level.FINE, "Error downloading dependency: " + dependencyString, exception);
            }
        }

        if (failed > 0) {
            logger.warning("Downloaded " + (total - failed) + "/" + total + " dependencies (" + failed + " failed)");
        }
    }

    // --- Remapping control ----------------------------------------------------

    private enum RemapMode { TRUE, FALSE, AUTO }

    private @NotNull RemapMode getRemapMode() {
        final String value = System.getProperty(REMAP_PROPERTY, "auto");
        if (value == null) {
            return RemapMode.AUTO;
        }
        final String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
            return RemapMode.TRUE;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
            return RemapMode.FALSE;
        }
        // Default/fallback to AUTO for unknown values
        return RemapMode.AUTO;
    }

    private boolean isRemapperAvailable() {
        try {
            Class.forName("de.jexcellence.dependency.remapper.RemappingDependencyManager", false, getClass().getClassLoader());
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Backwards-compatible helper retained for potential external usage.
     * Behavior: true -> enable; false -> disable; auto (default) -> enable only if remapper is available.
     */
    private boolean shouldEnableRemapping() {
        final RemapMode mode = getRemapMode();
        return switch (mode) {
            case TRUE -> true;
            case FALSE -> false;
            case AUTO -> isRemapperAvailable();
        };
    }

    private @NotNull Path determineEffectiveDirectory(
            @NotNull final Path librariesDirectory,
            @NotNull final Path remappedDirectory
    ) {
        final RemapMode mode = getRemapMode();

        if (mode == RemapMode.FALSE) {
            logger.fine("Remapping disabled via system property: " + REMAP_PROPERTY);
            return librariesDirectory;
        }

        if (!isRemapperAvailable()) {
            if (mode == RemapMode.TRUE) {
                logger.warning("Remapping requested but the remapper component is not present. " +
                        "Either add the remapper to the plugin classpath or disable remapping with -D" + REMAP_PROPERTY + "=false");
            } else {
                logger.fine("Remapper not present; skipping remapping (mode=auto)");
            }
            return librariesDirectory;
        }

        final boolean remappingSuccessful = attemptRemapping(librariesDirectory, remappedDirectory);

        if (remappingSuccessful) {
            logger.fine("Using remapped libraries from: " + remappedDirectory);
            return remappedDirectory;
        }

        logger.warning("Remapping failed, using original libraries");
        return librariesDirectory;
    }

    private boolean attemptRemapping(
            @NotNull final Path inputDirectory,
            @NotNull final Path outputDirectory
    ) {
        Objects.requireNonNull(inputDirectory, "inputDirectory cannot be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory cannot be null");

        try {
            Files.createDirectories(outputDirectory);
        } catch (final IOException exception) {
            logger.log(Level.WARNING, "Failed to create remapped directory: " + outputDirectory, exception);
            return false;
        }

        final List<Path> inputJars = collectJarFiles(inputDirectory);
        if (inputJars.isEmpty()) {
            logger.fine("No input JARs to remap in: " + inputDirectory);
            return false;
        }

        // IMPORTANT: RemappingDependencyManager expects the plugin data directory, not the libraries directory.
        // If we pass the libraries directory, it will append another "/libraries" and create a nested path.
        final Path dataDirectory = inputDirectory.getParent() != null ? inputDirectory.getParent() : inputDirectory;

        final Object remappingManager = createRemappingManager(dataDirectory);
        if (remappingManager == null) {
            return false;
        }

        final int relocationsApplied = applyRelocations(remappingManager, inputJars);
        if (relocationsApplied == 0) {
            logger.warning("No relocations were applied. Remapping will not change packages.");
        }

        return processRemapping(remappingManager, inputJars, outputDirectory);
    }

    private @NotNull List<Path> collectJarFiles(@NotNull final Path directory) {
        try (final Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (final IOException exception) {
            logger.log(Level.FINE, "Failed to list JAR files in " + directory, exception);
            return List.of();
        }
    }

    private Object createRemappingManager(@NotNull final Path dataDirectory) {
        try {
            final Class<?> managerClass = Class.forName("de.jexcellence.dependency.remapper.RemappingDependencyManager");
            final var constructor = managerClass.getConstructor(Path.class);
            return constructor.newInstance(dataDirectory);
        } catch (final Throwable throwable) {
            // If we reach here, the class was expected to exist (pre-checked earlier) but failed.
            logger.log(Level.WARNING, "RemappingDependencyManager not available or failed to initialize", throwable);
            return null;
        }
    }

    private int applyRelocations(@NotNull final Object remappingManager, @NotNull final List<Path> inputJars) {
        final int explicitRelocations = applyExplicitRelocations(remappingManager);

        if (explicitRelocations > 0) {
            return explicitRelocations;
        }

        final int automaticRelocations = applyAutomaticRelocations(remappingManager, inputJars);
        if (automaticRelocations > 0) {
            logger.fine("Applied " + automaticRelocations + " automatic relocations");
        }

        return automaticRelocations;
    }

    private int applyExplicitRelocations(@NotNull final Object remappingManager) {
        final String relocationsSpec = System.getProperty(RELOCATIONS_PROPERTY);
        if (relocationsSpec == null || relocationsSpec.trim().isEmpty()) {
            return 0;
        }

        int applied = 0;
        final String[] pairs = relocationsSpec.split(",");

        for (final String pair : pairs) {
            final String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) {
                continue;
            }

            final int separatorIndex = trimmedPair.indexOf("=>");
            if (separatorIndex <= 0 || separatorIndex >= trimmedPair.length() - 2) {
                logger.warning("Invalid relocation mapping, expected 'from=>to': " + trimmedPair);
                continue;
            }

            final String fromPackage = trimmedPair.substring(0, separatorIndex).trim();
            final String toPackage = trimmedPair.substring(separatorIndex + 2).trim();

            if (!fromPackage.isEmpty() && !toPackage.isEmpty()) {
                invokeRelocate(remappingManager, fromPackage, toPackage);
                applied++;
            } else {
                logger.warning("Ignoring empty relocation mapping: " + trimmedPair);
            }
        }

        return applied;
    }

    private int applyAutomaticRelocations(
            @NotNull final Object remappingManager,
            @NotNull final List<Path> jars
    ) {
        final String defaultPrefix = "de.jexcellence.remapped";
        String basePrefix = System.getProperty(RELOCATIONS_PREFIX_PROPERTY, defaultPrefix);

        if (basePrefix == null || basePrefix.trim().isEmpty()) {
            basePrefix = defaultPrefix;
        }

        basePrefix = basePrefix.trim().replaceAll("\\.$", "");

        final Set<String> excludedRoots = createExcludedRootsSet();
        final Set<String> detectedRoots = new HashSet<>();

        for (final Path jar : jars) {
            detectedRoots.addAll(detectPackageRoots(jar, excludedRoots));
        }

        int applied = 0;
        for (final String root : detectedRoots) {
            final String targetPackage = basePrefix + "." + root;
            invokeRelocate(remappingManager, root, targetPackage);
            applied++;
        }

        if (applied > 0) {
            logger.fine("Automatic relocation will map " + applied + " root package(s) under '" + basePrefix + "'");
        }

        return applied;
    }

    private @NotNull Set<String> createExcludedRootsSet() {
        final Set<String> excludes = new HashSet<>(Arrays.asList(
                // JDK internals
                "java", "javax", "jakarta", "sun", "com.sun", "jdk",
                "org.w3c", "org.xml", "org.ietf",
                // Hibernate - must not be relocated as it expects original paths
                "org.hibernate",
                // Database drivers - Hibernate loads these by class name from config
                "org.h2", "com.mysql", "org.postgresql", "org.mariadb", "com.microsoft",
                // Jackson 2.x (com.fasterxml) - compatible with server's bundled version
                "com.fasterxml"
        ));

        final String excludesProperty = System.getProperty(RELOCATIONS_EXCLUDES_PROPERTY);
        if (excludesProperty != null && !excludesProperty.trim().isEmpty()) {
            Arrays.stream(excludesProperty.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(excludes::add);
        }

        return excludes;
    }

    private @NotNull Set<String> detectPackageRoots(
            @NotNull final Path jarPath,
            @NotNull final Set<String> excludedRoots
    ) {
        final Set<String> roots = new HashSet<>();

        try (final JarFile jarFile = new JarFile(jarPath.toFile())) {
            for (final JarEntry entry : java.util.Collections.list(jarFile.entries())) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                if (entry.getName().endsWith("module-info.class")) {
                    continue;
                }

                final int lastSlashIndex = entry.getName().lastIndexOf('/');
                if (lastSlashIndex <= 0) {
                    continue;
                }

                final String packagePath = entry.getName().substring(0, lastSlashIndex);
                final String packageName = packagePath.replace('/', '.');
                final String rootPackage = extractFirstTwoSegments(packageName);

                if (!shouldExcludePackage(rootPackage, excludedRoots)) {
                    roots.add(rootPackage);
                }
            }
        } catch (final IOException exception) {
            logger.log(Level.FINE, "Failed to scan JAR for package roots: " + jarPath, exception);
        }

        return roots;
    }

    private @NotNull String extractFirstTwoSegments(@NotNull final String packageName) {
        final int firstDotIndex = packageName.indexOf('.');
        if (firstDotIndex < 0) {
            return packageName;
        }

        final int secondDotIndex = packageName.indexOf('.', firstDotIndex + 1);
        if (secondDotIndex < 0) {
            return packageName.substring(0, Math.max(firstDotIndex, packageName.length()));
        }

        return packageName.substring(0, secondDotIndex);
    }

    private boolean shouldExcludePackage(
            @NotNull final String rootPackage,
            @NotNull final Set<String> excludedRoots
    ) {
        if (rootPackage.isEmpty()) {
            return true;
        }

        for (final String excluded : excludedRoots) {
            if (rootPackage.equals(excluded) || rootPackage.startsWith(excluded + ".")) {
                return true;
            }
        }

        return false;
    }

    private void invokeRelocate(
            @NotNull final Object remappingManager,
            @NotNull final String fromPackage,
            @NotNull final String toPackage
    ) {
        try {
            final var method = remappingManager.getClass().getMethod("relocate", String.class, String.class);
            method.invoke(remappingManager, fromPackage, toPackage);
        } catch (final Exception exception) {
            logger.log(Level.FINE, "Failed to invoke relocate method", exception);
        }
    }

    private boolean processRemapping(
            @NotNull final Object remappingManager,
            @NotNull final List<Path> inputJars,
            @NotNull final Path outputDirectory
    ) {
        final int total = inputJars.size();
        int processedCount = 0;
        int remappedCount = 0;
        int lastLoggedPercent = 0;

        for (final Path inputJar : inputJars) {
            final Path outputJar = outputDirectory.resolve(inputJar.getFileName());

            if (isRemappedJarUpToDate(outputJar, inputJar)) {
                logger.fine("Using cached: " + outputJar.getFileName());
                processedCount++;
                remappedCount++;
            } else {
                deleteExistingFile(outputJar);

                if (performRemapping(remappingManager, inputJar, outputJar)) {
                    processedCount++;
                    if (isValidJarFile(outputJar)) {
                        remappedCount++;
                        logger.fine("Remapped: " + inputJar.getFileName());
                    } else {
                        logger.warning("Failed to remap: " + inputJar.getFileName());
                    }
                }
            }

            // Log at 25%, 50%, 75%, 100%
            final int percent = (processedCount * 100) / total;
            if (percent >= lastLoggedPercent + 25 || processedCount == total) {
                logger.info("Remapping libraries... " + processedCount + "/" + total + " (" + percent + "%)");
                lastLoggedPercent = (percent / 25) * 25;
            }
        }

        if (processedCount == 0) {
            logger.warning("No libraries to remap");
            return false;
        }

        return remappedCount > 0;
    }

    private boolean isRemappedJarUpToDate(@NotNull final Path outputJar, @NotNull final Path inputJar) {
        try {
            if (!Files.exists(outputJar) || !Files.isRegularFile(outputJar)) {
                return false;
            }

            final FileTime inputTime = Files.getLastModifiedTime(inputJar);
            final FileTime outputTime = Files.getLastModifiedTime(outputJar);
            final long outputSize = Files.size(outputJar);

            return outputSize > 0 && outputTime.compareTo(inputTime) >= 0;

        } catch (final IOException exception) {
            return false;
        }
    }

    private void deleteExistingFile(@NotNull final Path file) {
        try {
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (final IOException exception) {
            logger.log(Level.FINE, "Failed to delete existing file: " + file, exception);
        }
    }

    private boolean performRemapping(
            @NotNull final Object remappingManager,
            @NotNull final Path inputJar,
            @NotNull final Path outputJar
    ) {
        try {
            final var method = remappingManager.getClass().getMethod("remap", Path.class, Path.class);
            method.invoke(remappingManager, inputJar, outputJar);
            return true;
        } catch (final InvocationTargetException ite) {
            final Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            logger.warning("Failed to remap: " + inputJar.getFileName());
            logger.log(Level.FINE, "Remap error details for " + inputJar.getFileName(), cause);
            return false;
        } catch (final Throwable throwable) {
            logger.warning("Failed to remap: " + inputJar.getFileName());
            logger.log(Level.FINE, "Remap error details for " + inputJar.getFileName(), throwable);
            return false;
        }
    }

    private boolean isValidJarFile(@NotNull final Path file) {
        try {
            return Files.isRegularFile(file)
                    && Files.size(file) > MINIMUM_JAR_SIZE
                    && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
        } catch (final IOException exception) {
            return false;
        }
    }

    private void loadJarFilesIntoClasspath(
            @NotNull final PluginClasspathBuilder classpathBuilder,
            @NotNull final Path librariesDirectory
    ) {
        try (final Stream<Path> jarFiles = Files.walk(librariesDirectory, 1)) {
            final AtomicInteger jarCount = new AtomicInteger(0);

            jarFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .forEach(jarPath -> {
                        try {
                            classpathBuilder.addLibrary(new JarLibrary(jarPath));
                            jarCount.incrementAndGet();
                            logger.fine("Added: " + jarPath.getFileName());
                        } catch (final Exception exception) {
                            logger.log(Level.WARNING, "Failed to load: " + jarPath.getFileName(), exception);
                        }
                    });

            logger.info("Loaded " + jarCount.get() + " libraries");

        } catch (final Exception exception) {
            logger.log(Level.SEVERE, "Failed to load libraries", exception);
        }
    }
}
