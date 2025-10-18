package de.jexcellence.dependency;

import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.manager.DependencyManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class JEDependency {

    private static final String REMAPPING_MANAGER_CLASS = "de.jexcellence.dependency.remapper.RemappingDependencyManager";
    private static final String REMAP_PROPERTY = "jedependency.remap";
    private static final String PAPER_LOADER_PROPERTY = "paper.plugin.loader.active";

    private JEDependency() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void initialize(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        initialize(plugin, anchorClass, null);
    }

    public static void initialize(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        performInitialization(plugin, anchorClass, additionalDependencies, false);
    }

    public static void initializeWithRemapping(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        initializeWithRemapping(plugin, anchorClass, null);
    }

    public static void initializeWithRemapping(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        performInitialization(plugin, anchorClass, additionalDependencies, true);
    }

    public static @NotNull CompletableFuture<Void> initializeAsync(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        return initializeAsync(plugin, anchorClass, null);
    }

    public static @NotNull CompletableFuture<Void> initializeAsync(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies
    ) {
        return CompletableFuture.runAsync(() -> {
            performInitialization(plugin, anchorClass, additionalDependencies, false);
        });
    }

    public static @NotNull String getServerType() {
        if (isPaperPluginLoaderActive()) {
            return "Paper (with plugin loader)";
        } else if (isPaperServer()) {
            return "Paper (legacy mode)";
        } else {
            return "Spigot/CraftBukkit";
        }
    }

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

    private static void performInitialization(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass,
            @Nullable final String[] additionalDependencies,
            final boolean forceRemapping
    ) {
        final String serverType = getServerType();
        plugin.getLogger().info("JEDependency initializing on " + serverType);

        // On Paper plugin loader, inject pre-downloaded libraries but do NOT return early.
        // Continue with full initialization to leverage the complete feature set.
        if (isPaperPluginLoaderActive()) {
            plugin.getLogger().info("Paper plugin loader detected - injecting pre-downloaded libraries, then continuing with full initialization");
            injectPreDownloadedLibraries(plugin, anchorClass);
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

    private static void logServerSpecificLoading(@NotNull final JavaPlugin plugin) {
        if (isPaperServer()) {
            plugin.getLogger().info("Server-specific dependency loading: Paper dependencies will be prioritized");
        } else {
            plugin.getLogger().info("Server-specific dependency loading: Spigot dependencies will be prioritized");
        }
    }

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