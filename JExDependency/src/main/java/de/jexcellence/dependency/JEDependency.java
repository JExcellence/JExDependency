package de.jexcellence.dependency;

import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.manager.DependencyManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for plugins to bootstrap the runtime dependency system. The class coordinates YAML discovery,
 * repository downloads, optional remapping and classpath injection while keeping behaviour consistent with the Paper
 * plugin loader. All invocations log progress to the hosting {@link JavaPlugin}'s logger so operators can follow the
 * bootstrap sequence.
 */
public class JEDependency {

    /**
     * Fully qualified name of the optional remapping manager that is loaded reflectively to avoid a hard dependency.
     */
    private static final String REMAPPING_MANAGER_CLASS = "de.jexcellence.dependency.remapper.RemappingDependencyManager";
    /**
     * System property controlling whether the remapping pipeline should be activated. Supported values mirror the
     * Paper loader ("auto", "true", "false", and common synonyms).
     */
    private static final String REMAP_PROPERTY = "jedependency.remap";
    /**
     * System property set when the Paper bootstrap loader is active to trigger injection of pre-downloaded libraries.
     */
    private static final String PAPER_LOADER_PROPERTY = "paper.plugin.loader.active";

    private JEDependency() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initializes the dependency system synchronously using dependencies declared next to the supplied anchor class.
     * The call blocks the server thread while dependencies are downloaded, remapped (if applicable) and injected.
     *
     * @param plugin       owning plugin providing the logger and data directory
     * @param anchorClass  class used to locate dependency descriptors and the runtime class loader to inject into
     */
    public static void initialize(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        initialize(plugin, anchorClass, null);
    }

    /**
     * Initializes the dependency system synchronously using both YAML descriptors and optional ad-hoc coordinates.
     * The method is safe to invoke during {@code onLoad()} or {@code onEnable()} provided the caller can tolerate the
     * blocking download/remapping work.
     *
     * @param plugin                  owning plugin providing the logger and data directory
     * @param anchorClass             class used to locate dependency descriptors and the runtime class loader
     * @param additionalDependencies  optional coordinates ({@code group:artifact:version[:classifier]}) appended to the YAML list
     */
    public static void initialize(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        performInitialization(plugin, anchorClass, additionalDependencies, false);
    }

    /**
     * Initializes the dependency system synchronously while forcing the remapping pipeline. Use this when the plugin
     * must guarantee relocations even if {@code -Djedependency.remap} is unset or evaluates to {@code false}.
     *
     * @param plugin       owning plugin providing the logger and data directory
     * @param anchorClass  class used to locate dependency descriptors and the runtime class loader
     */
    public static void initializeWithRemapping(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        initializeWithRemapping(plugin, anchorClass, null);
    }

    /**
     * Initializes the dependency system synchronously while forcing the remapping pipeline and allowing for additional
     * dependency coordinates.
     *
     * @param plugin                  owning plugin providing the logger and data directory
     * @param anchorClass             class used to locate dependency descriptors and the runtime class loader
     * @param additionalDependencies  optional coordinates ({@code group:artifact:version[:classifier]}) appended to the YAML list
     */
    public static void initializeWithRemapping(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        performInitialization(plugin, anchorClass, additionalDependencies, true);
    }

    /**
     * Initializes the dependency system on a separate thread, returning immediately with a {@link CompletableFuture}.
     * The caller is responsible for awaiting the future before using classes provided by the managed dependencies.
     *
     * @param plugin       owning plugin providing the logger and data directory
     * @param anchorClass  class used to locate dependency descriptors and the runtime class loader
     *
     * @return future that completes when downloads, optional remapping and injection have finished
     */
    public static @NotNull CompletableFuture<Void> initializeAsync(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        return initializeAsync(plugin, anchorClass, null);
    }

    /**
     * Initializes the dependency system on a separate thread using both YAML descriptors and optional coordinates.
     * The returned future executes downloads and optional remapping on a background worker but still performs logging
     * via the plugin's logger.
     *
     * @param plugin                  owning plugin providing the logger and data directory
     * @param anchorClass             class used to locate dependency descriptors and the runtime class loader
     * @param additionalDependencies  optional coordinates ({@code group:artifact:version[:classifier]}) appended to the YAML list
     *
     * @return future that completes when downloads, optional remapping and injection have finished
     */
    public static @NotNull CompletableFuture<Void> initializeAsync(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        return CompletableFuture.runAsync(() -> {
            performInitialization(plugin, anchorClass, additionalDependencies, false);
        });
    }

    /**
     * Determines the server distribution currently running so the dependency system can align logging and loader
     * expectations (e.g. whether the Paper plugin loader pre-downloaded libraries).
     *
     * @return human-readable server type string for logging purposes
     */
    public static @NotNull String getServerType() {
        if (isPaperPluginLoaderActive() && isVersion120OrHigher()) {
            return "Paper (with plugin loader)";
        } else if (isPaperPluginLoaderActive() && !isVersion120OrHigher()) {
            return "Paper (plugin loader disabled - version < 1.20)";
        } else if (isPaperServer()) {
            return "Paper (legacy mode)";
        } else {
            return "Spigot/CraftBukkit";
        }
    }

    /**
     * Checks whether the runtime includes Paper-specific configuration classes, indicating Paper or a derivative build
     * is in use. This allows the loader to favour Paper dependency sets when both Paper and Spigot descriptors exist.
     *
     * @return {@code true} if Paper classes are present, {@code false} otherwise
     */
    public static boolean isPaperServer() {
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

    /**
     * Checks if the server version is 1.20 or higher.
     * Paper plugin loader features are only available on 1.20+.
     *
     * @return {@code true} if server version is 1.20 or higher, {@code false} otherwise
     */
    public static boolean isVersion120OrHigher() {
        try {
            final String version = Bukkit.getVersion();
            // Extract version number from strings like "1.20.1-R0.1-SNAPSHOT" or "git-Paper-123 (MC: 1.20.1)"
            final String mcVersion;
            if (version.contains("MC: ")) {
                // Paper format: "git-Paper-123 (MC: 1.20.1)"
                final int mcStart = version.indexOf("MC: ") + 4;
                final int mcEnd = version.indexOf(")", mcStart);
                mcVersion = version.substring(mcStart, mcEnd);
            } else {
                // Spigot format: "1.20.1-R0.1-SNAPSHOT"
                final int dashIndex = version.indexOf("-");
                mcVersion = dashIndex > 0 ? version.substring(0, dashIndex) : version;
            }
            
            final String[] parts = mcVersion.split("\\.");
            if (parts.length >= 2) {
                final int major = Integer.parseInt(parts[0]);
                final int minor = Integer.parseInt(parts[1]);
                
                // Check if version is 1.20 or higher
                return major > 1 || (major == 1 && minor >= 20);
            }
        } catch (final Exception e) {
            // If version parsing fails, assume it's an older version for safety
            return false;
        }
        return false;
    }

    private static void performInitialization(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies,
            final boolean forceRemapping
    ) {
        final String serverType = getServerType();
        plugin.getLogger().info("JEDependency initializing on " + serverType);

        // On Paper plugin loader (1.20+), inject pre-downloaded libraries but do NOT return early.
        // Continue with full initialization to leverage the complete feature set.
        if (isPaperPluginLoaderActive() && isVersion120OrHigher()) {
            plugin.getLogger().info("Paper plugin loader detected (1.20+) - injecting pre-downloaded libraries, then continuing with full initialization");
            injectPreDownloadedLibraries(plugin, anchorClass);
        } else if (isPaperPluginLoaderActive() && !isVersion120OrHigher()) {
            plugin.getLogger().info("Paper plugin loader detected but server version is below 1.20 - skipping pre-downloaded library injection for compatibility");
        }

        logServerSpecificLoading(plugin);

        final boolean shouldUseRemapping = forceRemapping || shouldEnableRemapping();
        final boolean remappingAvailable = isRemappingManagerAvailable();

        if (shouldUseRemapping && remappingAvailable) {
            final String remappingSource = forceRemapping ? "API" : "system property";
            plugin.getLogger().info("Using RemappingDependencyManager (remapping requested via " + remappingSource + ")");

            if (initializeWithRemappingManager(plugin, anchorClass, additionalDependencies)) {
                plugin.getLogger().info("JEDependency initialization completed with remapping");
                return;
            }

            plugin.getLogger().warning("Remapping initialization failed - falling back to standard DependencyManager");
        } else if (shouldUseRemapping) {
            plugin.getLogger().warning("Remapping requested but RemappingDependencyManager not found - falling back to standard DependencyManager");
        }

        final DependencyManager dependencyManager = new DependencyManager(plugin, anchorClass);
        dependencyManager.initialize(additionalDependencies);
        plugin.getLogger().info("JEDependency initialization completed");
    }

    // Replace the injectPreDownloadedLibraries method body with:
    /**
     * Injects libraries that were prepared by the Paper plugin loader before the plugin's lifecycle callbacks run.
     * The method prefers remapped outputs when available, logs successes/failures per file and keeps track of how many
     * jars ultimately entered the runtime class loader.
     *
     * @param plugin       owning plugin used for logging and resolving the data directory
     * @param anchorClass  class whose class loader receives the injected libraries
     */
    private static void injectPreDownloadedLibraries(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        final ClasspathInjector injector = new ClasspathInjector();

        // Find libraries directory
        final File pluginDataFolder = plugin.getDataFolder();
        final File remappedLibsFolder = new File(pluginDataFolder, "libraries/remapped");
        final File libsFolder = new File(pluginDataFolder, "libraries");

        // Prefer remapped libraries if they exist
        final File targetFolder = (remappedLibsFolder.exists() && remappedLibsFolder.isDirectory())
                ? remappedLibsFolder
                : libsFolder;

        if (!targetFolder.exists() || !targetFolder.isDirectory()) {
            plugin.getLogger().warning("Libraries folder not found: " + targetFolder);
            return;
        }

        final File[] jarFiles = targetFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            plugin.getLogger().info("No JAR files found in: " + targetFolder);
            return;
        }

        final ClassLoader pluginClassLoader = anchorClass.getClassLoader();
        int injected = 0;
        int failed = 0;

        for (final File jarFile : jarFiles) {
            final boolean ok = injector.tryInject(pluginClassLoader, jarFile);
            if (ok) {
                injected++;
                plugin.getLogger().fine("Injected: " + jarFile.getName());
            } else {
                failed++;
                plugin.getLogger().warning("Failed to inject " + jarFile.getName());
            }
        }

        plugin.getLogger().info("Injected " + injected + " libraries"
                + (failed > 0 ? (" (" + failed + " failed)") : "")
                + " from " + targetFolder.getName() + " into runtime classloader");
    }

    /**
     * Logs which dependency flavour (Paper or Spigot) will be preferred based on runtime detection.
     *
     * @param plugin plugin used for logging
     */
    private static void logServerSpecificLoading(@NotNull final JavaPlugin plugin) {
        if (isPaperServer()) {
            plugin.getLogger().info("Server-specific dependency loading: Paper dependencies will be prioritized");
        } else {
            plugin.getLogger().info("Server-specific dependency loading: Spigot dependencies will be prioritized");
        }
    }

    /**
     * Resolves whether remapping should be enabled based on the {@link #REMAP_PROPERTY} system property. The method
     * recognises common boolean synonyms for user convenience.
     *
     * @return {@code true} if remapping is explicitly requested, {@code false} otherwise
     */
    private static boolean shouldEnableRemapping() {
        final String propertyValue = System.getProperty(REMAP_PROPERTY);
        if (propertyValue == null) {
            return false;
        }

        final String normalizedValue = propertyValue.trim().toLowerCase();
        return "true".equals(normalizedValue)
                || "1".equals(normalizedValue)
                || "yes".equals(normalizedValue)
                || "on".equals(normalizedValue);
    }

    private static boolean isRemappingManagerAvailable() {
        try {
            Class.forName(REMAPPING_MANAGER_CLASS);
            return true;
        } catch (final ClassNotFoundException exception) {
            return false;
        }
    }

    private static boolean initializeWithRemappingManager(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        try {
            final Class<?> remapperClass = Class.forName(REMAPPING_MANAGER_CLASS);

            final Constructor<?> constructor = findSuitableConstructor(remapperClass);
            if (constructor == null) {
                plugin.getLogger().severe("No compatible constructor found in RemappingDependencyManager");
                return false;
            }

            final Object manager = createRemappingManagerInstance(constructor, plugin, anchorClass);
            if (manager == null) {
                return false;
            }

            return invokeInitializeMethod(manager, additionalDependencies, plugin, anchorClass);

        } catch (final Throwable throwable) {
            plugin.getLogger().severe("Failed to initialize RemappingDependencyManager: "
                    + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return false;
        }
    }

    private static @Nullable Constructor<?> findSuitableConstructor(@NotNull final Class<?> remapperClass) {
        try {
            return remapperClass.getConstructor(JavaPlugin.class, Class.class);
        } catch (final NoSuchMethodException exception) {
            try {
                return remapperClass.getConstructor();
            } catch (final NoSuchMethodException exception2) {
                return null;
            }
        }
    }

    private static @Nullable Object createRemappingManagerInstance(
            @NotNull final Constructor<?> constructor,
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        try {
            return constructor.getParameterCount() == 2
                    ? constructor.newInstance(plugin, anchorClass)
                    : constructor.newInstance();
        } catch (final Exception exception) {
            return null;
        }
    }

    private static boolean invokeInitializeMethod(
            @NotNull final Object manager,
            @Nullable final String[] additionalDependencies,
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        try {
            final Method initializeMethod = manager.getClass().getMethod("initialize", String[].class);
            initializeMethod.invoke(manager, (Object) additionalDependencies);
            return true;
        } catch (final NoSuchMethodException exception) {
            return tryAlternativeInitialization(manager, additionalDependencies, anchorClass);
        } catch (final Exception exception) {
            return false;
        }
    }

    private static boolean tryAlternativeInitialization(
            @NotNull final Object manager,
            @Nullable final String[] additionalDependencies,
            @NotNull final Class<?> anchorClass
    ) {
        try {
            Method addDependenciesMethod = null;
            try {
                addDependenciesMethod = manager.getClass().getMethod("addDependencies", String[].class);
            } catch (final NoSuchMethodException ignored) {
            }

            final Method loadAllMethod = manager.getClass().getMethod("loadAll", ClassLoader.class);

            if (addDependenciesMethod != null && additionalDependencies != null) {
                addDependenciesMethod.invoke(manager, (Object) additionalDependencies);
            }

            final ClassLoader classLoader = anchorClass.getClassLoader();
            loadAllMethod.invoke(manager, classLoader);

            return true;

        } catch (final Exception exception) {
            return false;
        }
    }

    private static boolean isPaperPluginLoaderActive() {
        final String paperLoaderActive = System.getProperty(PAPER_LOADER_PROPERTY);
        if ("true".equals(paperLoaderActive)) {
            return true;
        }

        try {
            Class.forName("io.papermc.paper.plugin.loader.PluginLoader");
            Class.forName("io.papermc.paper.plugin.bootstrap.PluginBootstrap");
            return "true".equals(paperLoaderActive);
        } catch (final ClassNotFoundException exception) {
            return false;
        }
    }
}