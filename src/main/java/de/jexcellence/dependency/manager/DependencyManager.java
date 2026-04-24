package de.jexcellence.dependency.manager;

import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.loader.YamlDependencyLoader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.model.ProcessingResult;
import de.jexcellence.dependency.module.Deencapsulation;
import de.jexcellence.dependency.resolver.TransitiveDependencyResolver;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Core coordinator for synchronous dependency handling within a plugin lifecycle. The manager loads dependency
 * coordinates from bundled YAML files and optional runtime additions, downloads missing artifacts, performs module
 * de-encapsulation and injects the resulting JARs into the plugin's class loader. Logging is routed through the plugin
 * logger to match Bukkit/Paper diagnostics expectations.
 *
 * <h2>Transitive resolution</h2>
 * Call {@link #setTransitiveResolutionEnabled(boolean) setTransitiveResolutionEnabled(true)} before
 * {@link #initialize} to automatically resolve and download transitive Maven dependencies.  The resolver
 * downloads each artifact's {@code .pom} file, walks the parent POM chain for property inheritance, and
 * recursively collects all {@code compile}/{@code runtime} scoped non-optional dependencies.  POMs are cached
 * in a {@code libraries/poms/} subdirectory to avoid repeated network requests on restarts.
 */
public class DependencyManager {

    /**
     * Directory name under the plugin's data folder where downloaded dependencies are cached.
     */
    private static final String LIBRARIES_DIRECTORY = "libraries";

    private final Logger logger;
    private final JavaPlugin plugin;
    private final Class<?> anchorClass;
    private final DependencyDownloader downloader;
    private final ClasspathInjector injector;
    private final YamlDependencyLoader yamlLoader;

    /**
     * When {@code true}, {@link #initialize} expands the explicit dependency list with all
     * transitive Maven dependencies resolved via POM parsing before downloading.
     */
    private boolean transitiveResolutionEnabled = false;

    /**
     * Creates a dependency manager bound to a plugin and anchor class. The anchor class determines both where YAML.
     * descriptors are located and which class loader will receive injected jars.
     *
     * @param plugin      owning plugin providing loggers and the data directory
     * @param anchorClass class used to discover dependency descriptors and as injection target
     */
    public DependencyManager(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        this.plugin = plugin;
        this.anchorClass = anchorClass;
        this.logger = plugin.getLogger();
        this.downloader = new DependencyDownloader();
        this.injector = new ClasspathInjector();
        this.yamlLoader = new YamlDependencyLoader();
    }

    /**
     * Enables or disables automatic transitive dependency resolution.
     *
     * <p>When enabled, every coordinate declared in YAML or passed to {@link #initialize} is
     * treated as a root, its Maven POM is downloaded, and the full transitive closure
     * (compile + runtime scope, non-optional) is added to the download queue automatically.
     *
     * <p>Disabled by default to preserve backward-compatible behaviour for plugins that
     * explicitly enumerate all required artifacts.
     *
     * @param enabled {@code true} to resolve transitives, {@code false} to use explicit list only
     * @return this manager for fluent chaining
     */
    public @NotNull DependencyManager setTransitiveResolutionEnabled(final boolean enabled) {
        this.transitiveResolutionEnabled = enabled;
        return this;
    }

    /**
     * Performs synchronous dependency resolution on the calling thread. The method blocks while downloads complete,.
     * performs module de-encapsulation to allow reflective classpath injection, and injects each successfully
     * downloaded artifact into the plugin class loader.
     *
     * @param additionalDependencies optional coordinates ({@code group:artifact:version[:classifier]}) appended to the
     *                               YAML-provided list
     */
    public void initialize(@Nullable final String[] additionalDependencies) {
        final long startTime = System.currentTimeMillis();

        final File librariesDirectory = setupLibrariesDirectory();
        final ClassLoader targetClassLoader = anchorClass.getClassLoader();

        performModuleDeencapsulation();

        List<DependencyCoordinate> coordinates = collectDependencies(additionalDependencies);

        if (coordinates.isEmpty()) {
            logger.info("No dependencies to process");
            return;
        }

        if (transitiveResolutionEnabled) {
            coordinates = expandWithTransitives(coordinates, librariesDirectory);
        }

        logger.info("Resolving " + coordinates.size() + " dependencies...");

        final ProcessingResult result = processDependencies(coordinates, librariesDirectory, targetClassLoader);

        final long duration = System.currentTimeMillis() - startTime;
        logProcessingSummary(result, duration);
    }

    /**
     * Runs dependency resolution asynchronously using virtual threads. Downloading and injection are executed on the.
     * returned {@link CompletableFuture}, while YAML parsing and configuration happen within the async task. Callers
     * should inspect the resulting {@link ProcessingResult} to surface failures.
     *
     * @param additionalDependencies optional coordinates ({@code group:artifact:version[:classifier]}) appended to the
     *                               YAML-provided list
     *
     * @return future reporting successful and failed coordinates together with total processing time
     */
    public @NotNull CompletableFuture<ProcessingResult> initializeAsync(
            @Nullable final String[] additionalDependencies
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final long startTime = System.currentTimeMillis();

            final File librariesDirectory = setupLibrariesDirectory();
            final ClassLoader targetClassLoader = anchorClass.getClassLoader();

            performModuleDeencapsulation();

            List<DependencyCoordinate> coordinates = collectDependencies(additionalDependencies);

            if (coordinates.isEmpty()) {
                return new ProcessingResult(List.of(), List.of(), 0L);
            }

            if (transitiveResolutionEnabled) {
                coordinates = expandWithTransitives(coordinates, librariesDirectory);
            }

            logger.info("Resolving " + coordinates.size() + " dependencies...");

            final ProcessingResult result = processDependencies(coordinates, librariesDirectory, targetClassLoader);

            final long duration = System.currentTimeMillis() - startTime;
            logProcessingSummary(result, duration);
            return result;
        });
    }

    /**
     * Expands the given explicit dependency list with all transitives discovered through POM
     * resolution.  Root coordinates are preserved at the front of the returned list; newly
     * discovered transitives are appended in breadth-first order.
     */
    private @NotNull List<DependencyCoordinate> expandWithTransitives(
            @NotNull final List<DependencyCoordinate> roots,
            @NotNull final File librariesDirectory
    ) {
        logger.info("Transitive resolution enabled — analysing POM files...");

        final TransitiveDependencyResolver resolver = new TransitiveDependencyResolver(librariesDirectory);
        final Set<DependencyCoordinate> transitives = resolver.resolve(roots);

        // Merge: roots first (preserve their order), then new transitives
        final Set<DependencyCoordinate> merged = new LinkedHashSet<>(roots);
        merged.addAll(transitives);

        final int added = merged.size() - roots.size();
        if (added > 0) {
            logger.info("Transitive resolution discovered " + added + " additional dependencies");
        } else {
            logger.fine("Transitive resolution found no additional dependencies");
        }

        return new ArrayList<>(merged);
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
                    logger.log(Level.FINE, () -> "YAML dependency: " + dependency);
                } else {
                    logger.log(Level.WARNING, "Invalid dependency format: {0}", dependency);
                }
            }
        }

        if (additionalDependencies != null) {
            logger.log(Level.INFO, "Adding {0} additional dependencies", additionalDependencies.length);
            for (final String dependency : additionalDependencies) {
                final DependencyCoordinate coordinate = DependencyCoordinate.parse(dependency);
                if (coordinate != null) {
                    coordinates.add(coordinate);
                    logger.log(Level.FINE, () -> "Additional dependency: " + dependency);
                } else {
                    logger.log(Level.WARNING, "Invalid dependency format: {0}", dependency);
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

        // Fire all downloads in parallel using the downloader's virtual-thread executor
        final List<CompletableFuture<DownloadResult>> futures = coordinates.stream()
                .map(coord -> downloader.downloadAsync(coord, librariesDirectory))
                .toList();

        final List<DownloadResult> downloadResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Inject sequentially — classpath mutation is not thread-safe
        for (final DownloadResult result : downloadResults) {
            if (result.success() && result.file() != null) {
                if (injector.tryInject(classLoader, result.file())) {
                    successful.add(result.coordinate());
                } else {
                    failed.add(DownloadResult.failure(result.coordinate(), "Injection failed"));
                }
            } else {
                failed.add(result);
            }
        }

        final long duration = System.currentTimeMillis() - startTime;
        return new ProcessingResult(successful, failed, duration);
    }

    private void logProcessingSummary(@NotNull final ProcessingResult result, final long totalDuration) {
        if (result.hasFailures()) {
            logger.log(Level.INFO, "Loaded {0}/{1} dependencies in {2}ms ({3} failed)",
                    new Object[]{
                            result.getSuccessCount(),
                            result.getTotalCount(),
                            totalDuration,
                            result.getFailureCount()
                    });
            final String failedList = result.getFailed().stream()
                    .map(dr -> dr.coordinate().toGavString())
                    .collect(Collectors.joining(", "));
            logger.log(Level.WARNING, "Failed: {0}", failedList);
        } else {
            logger.log(Level.INFO, "Loaded {0} dependencies in {1}ms",
                    new Object[]{result.getTotalCount(), totalDuration});
        }
    }

    /**
     * Shuts down executor-backed collaborators such as {@link DependencyDownloader}. Invoking this during plugin.
     * disable ensures no lingering async download tasks remain.
     */
    public void shutdown() {
        downloader.shutdown();
    }
}
