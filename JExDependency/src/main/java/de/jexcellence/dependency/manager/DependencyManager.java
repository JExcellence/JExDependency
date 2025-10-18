package de.jexcellence.dependency.manager;

import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.loader.YamlDependencyLoader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.model.ProcessingResult;
import de.jexcellence.dependency.module.Deencapsulation;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DependencyManager {

    private static final String LIBRARIES_DIRECTORY = "libraries";

    private final Logger logger;
    private final JavaPlugin plugin;
    private final Class<?> anchorClass;
    private final DependencyDownloader downloader;
    private final ClasspathInjector injector;
    private final YamlDependencyLoader yamlLoader;

    public DependencyManager(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        this.plugin = plugin;
        this.anchorClass = anchorClass;
        this.logger = Logger.getLogger(getClass().getName());
        this.downloader = new DependencyDownloader();
        this.injector = new ClasspathInjector();
        this.yamlLoader = new YamlDependencyLoader();
    }

    public void initialize(@Nullable final String[] additionalDependencies) {
        logger.info("Initializing dependency management for: " + plugin.getName());

        final long startTime = System.currentTimeMillis();

        final File pluginJar = determinePluginJarLocation();
        if (pluginJar == null) {
            logger.severe("Failed to determine plugin JAR location");
            return;
        }

        final File librariesDirectory = setupLibrariesDirectory();
        final ClassLoader targetClassLoader = anchorClass.getClassLoader();

        performModuleDeencapsulation();

        final List<DependencyCoordinate> coordinates = collectDependencies(additionalDependencies);

        if (coordinates.isEmpty()) {
            logger.info("No dependencies to process");
            return;
        }

        logger.info("Processing " + coordinates.size() + " dependencies...");

        final ProcessingResult result = processDependencies(coordinates, librariesDirectory, targetClassLoader);

        final long duration = System.currentTimeMillis() - startTime;
        logProcessingSummary(result, librariesDirectory, duration);

        logger.info("Dependency management initialization completed in " + duration + "ms");
    }

    public @NotNull CompletableFuture<ProcessingResult> initializeAsync(
            @Nullable final String[] additionalDependencies
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final long startTime = System.currentTimeMillis();

            final File librariesDirectory = setupLibrariesDirectory();
            final ClassLoader targetClassLoader = anchorClass.getClassLoader();

            performModuleDeencapsulation();

            final List<DependencyCoordinate> coordinates = collectDependencies(additionalDependencies);

            if (coordinates.isEmpty()) {
                return new ProcessingResult(List.of(), List.of(), 0L);
            }

            final List<CompletableFuture<DownloadResult>> downloadFutures = coordinates.stream()
                    .map(coord -> downloader.downloadAsync(coord, librariesDirectory))
                    .toList();

            final List<DownloadResult> downloadResults = downloadFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            final List<DependencyCoordinate> successful = new ArrayList<>();
            final List<DownloadResult> failed = new ArrayList<>();

            for (final DownloadResult result : downloadResults) {
                if (result.success() && result.file() != null) {
                    if (injector.tryInject(targetClassLoader, result.file())) {
                        successful.add(result.coordinate());
                    } else {
                        failed.add(result);
                    }
                } else {
                    failed.add(result);
                }
            }

            final long duration = System.currentTimeMillis() - startTime;
            return new ProcessingResult(successful, failed, duration);
        });
    }

    private @Nullable File determinePluginJarLocation() {
        try {
            final CodeSource codeSource = anchorClass.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }

            final URL location = codeSource.getLocation();
            return new File(location.toURI());

        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Failed to determine plugin JAR location", exception);
            return null;
        }
    }

    private @NotNull File setupLibrariesDirectory() {
        final File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        final File librariesDir = new File(dataFolder, LIBRARIES_DIRECTORY);
        if (!librariesDir.exists()) {
            librariesDir.mkdirs();
        }

        return librariesDir;
    }

    private void performModuleDeencapsulation() {
        try {
            Deencapsulation.deencapsulate(anchorClass);
            logger.fine("Module deencapsulation completed");
        } catch (final Exception exception) {
            logger.log(Level.WARNING, "Module deencapsulation failed", exception);
        }
    }

    private @NotNull List<DependencyCoordinate> collectDependencies(
            @Nullable final String[] additionalDependencies
    ) {
        final List<DependencyCoordinate> coordinates = new ArrayList<>();

        final List<String> yamlDependencies = yamlLoader.loadDependencies(anchorClass);
        if (yamlDependencies != null) {
            for (final String dependency : yamlDependencies) {
                final DependencyCoordinate coordinate = DependencyCoordinate.parse(dependency);
                if (coordinate != null) {
                    coordinates.add(coordinate);
                    logger.fine("YAML dependency: " + dependency);
                } else {
                    logger.warning("Invalid dependency format: " + dependency);
                }
            }
        }

        if (additionalDependencies != null) {
            logger.info("Adding " + additionalDependencies.length + " additional dependencies");
            for (final String dependency : additionalDependencies) {
                final DependencyCoordinate coordinate = DependencyCoordinate.parse(dependency);
                if (coordinate != null) {
                    coordinates.add(coordinate);
                    logger.fine("Additional dependency: " + dependency);
                } else {
                    logger.warning("Invalid dependency format: " + dependency);
                }
            }
        }

        return coordinates;
    }

    private @NotNull ProcessingResult processDependencies(
            @NotNull final List<DependencyCoordinate> coordinates,
            @NotNull final File librariesDirectory,
            @NotNull final ClassLoader classLoader
    ) {
        final long startTime = System.currentTimeMillis();
        final List<DependencyCoordinate> successful = new ArrayList<>();
        final List<DownloadResult> failed = new ArrayList<>();

        for (final DependencyCoordinate coordinate : coordinates) {
            final DownloadResult result = downloader.download(coordinate, librariesDirectory);

            if (result.success() && result.file() != null) {
                if (injector.tryInject(classLoader, result.file())) {
                    successful.add(coordinate);
                } else {
                    failed.add(DownloadResult.failure(coordinate, "Injection failed"));
                }
            } else {
                failed.add(result);
            }
        }

        final long duration = System.currentTimeMillis() - startTime;
        return new ProcessingResult(successful, failed, duration);
    }

    private void logProcessingSummary(
            @NotNull final ProcessingResult result,
            @NotNull final File librariesDirectory,
            final long totalDuration
    ) {
        logger.info("Dependency processing summary:");
        logger.info(String.format("  Total: %d | Success: %d | Failed: %d | Time: %dms",
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailureCount(),
                totalDuration));

        if (result.hasFailures()) {
            final String failedList = result.getFailed().stream()
                    .map(dr -> dr.coordinate().toGavString())
                    .collect(Collectors.joining(", "));
            logger.warning("Failed dependencies: " + failedList);
        }

        final File[] jarFiles = librariesDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        final int totalLibraries = jarFiles != null ? jarFiles.length : 0;
        logger.info("Libraries directory contains " + totalLibraries + " JAR files");
    }

    public void shutdown() {
        downloader.shutdown();
    }
}
