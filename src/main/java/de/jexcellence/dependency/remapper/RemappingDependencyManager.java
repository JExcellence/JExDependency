package de.jexcellence.dependency.remapper;

import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.loader.YamlDependencyLoader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.module.Deencapsulation;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates dependency downloading, package remapping and classpath injection when remapping support is available.
 * The manager is constructed reflectively by both the Paper plugin loader and the legacy {@link
 * de.jexcellence.dependency.JEDependency} API. It collects dependency coordinates, applies explicit or automatically
 * detected relocations using {@link PackageRemapper} and injects the resulting jars into the target class loader.
 */
@SuppressWarnings("unused")
public class RemappingDependencyManager {

    // ----------------- Constants -----------------

    private static final Logger LOGGER = Logger.getLogger(RemappingDependencyManager.class.getName());

    private static final String LIBRARIES_DIR_NAME = "libraries";
    private static final String REMAPPED_DIR_NAME = "remapped";

    // System properties for optional behavior parity with the Paper loader path
    private static final String RELOCATIONS_PROPERTY = "jedependency.relocations"; // "from=>to,org.foo=>my.pkg.org.foo"
    private static final String RELOCATIONS_PREFIX_PROPERTY = "jedependency.relocations.prefix"; // base auto prefix
    private static final String RELOCATIONS_EXCLUDES_PROPERTY = "jedependency.relocations.excludes"; // exclude roots

    private static final long MINIMUM_VALID_JAR_SIZE = 1024L;
    /** Path separator used in JAR entries (always forward slash per JAR specification). */
    private static final char JAR_PATH_SEPARATOR = '/';

    // ----------------- Dependencies -----------------

    private final DependencyDownloader downloader = new DependencyDownloader();
    private final ClasspathInjector injector = new ClasspathInjector();
    private final YamlDependencyLoader yamlLoader = new YamlDependencyLoader();

    // ----------------- Context -----------------

    private final @Nullable JavaPlugin plugin;
    private final @Nullable Class<?> anchorClass;

    private final Path dataDirectory;
    private final Path librariesDirectory;
    private final Path remappedDirectory;

    // ----------------- State -----------------

    private final List<DependencyCoordinate> coordinates = new ArrayList<>();
    // LinkedHashMap preserves insertion order; also used for longest-prefix match (scan in order of decreasing key length)
    private final Map<String, String> relocations = new LinkedHashMap<>();

    // ----------------- Constructors -----------------

    /**
     * Constructor used by {@link de.jexcellence.dependency.loader.PaperPluginLoader} via reflection.
     *
     * @param dataDirectory plugin data directory supplied by the Paper loader context
     */
    public RemappingDependencyManager(@NotNull final Path dataDirectory) {
        this.plugin = null;
        this.anchorClass = null;
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.librariesDirectory = this.dataDirectory.resolve(LIBRARIES_DIR_NAME);
        this.remappedDirectory = this.librariesDirectory.resolve(REMAPPED_DIR_NAME);
        ensureDirectories();
        // Optional: preload relocations from system properties to support CLI-only usage.
        loadRelocationsFromSystemProperties();
    }

    /**
     * Constructor used by the legacy {@link de.jexcellence.dependency.JEDependency} API via reflection.
     *
     * @param plugin      owning plugin providing the data directory and logger context
     * @param anchorClass class used to discover YAML descriptors and determine the injection class loader
     */
    public RemappingDependencyManager(
            @NotNull final JavaPlugin plugin,
            @NotNull final Class<?> anchorClass
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.anchorClass = Objects.requireNonNull(anchorClass, "anchorClass");
        this.dataDirectory = plugin.getDataFolder().toPath();
        this.librariesDirectory = this.dataDirectory.resolve(LIBRARIES_DIR_NAME);
        this.remappedDirectory = this.librariesDirectory.resolve(REMAPPED_DIR_NAME);
        ensureDirectories();
        loadRelocationsFromSystemProperties();
    }

    /**
     * Fallback no-arg constructor for compatibility that defaults to the working directory. Mainly intended for.
     * command-line usage or testing scenarios.
     */
    public RemappingDependencyManager() {
        this.plugin = null;
        this.anchorClass = null;
        final String base = System.getProperty("jedependency.dataDir", ".");
        this.dataDirectory = Path.of(base);
        this.librariesDirectory = this.dataDirectory.resolve(LIBRARIES_DIR_NAME);
        this.remappedDirectory = this.librariesDirectory.resolve(REMAPPED_DIR_NAME);
        ensureDirectories();
        loadRelocationsFromSystemProperties();
    }

    // ----------------- Public API: relocation and remapping -----------------

    /**
     * Registers a package relocation mapping. Package names are normalised and restricted roots (e.g. {@code java}) are.
     * ignored to avoid interfering with core classes.
     *
     * @param fromPackage original package name in dot notation
     * @param toPackage   destination package name in dot notation
     */
    public void relocate(@NotNull final String fromPackage, @NotNull final String toPackage) {
        final String from = normalizePackage(fromPackage);
        final String to = normalizePackage(toPackage);

        if (from.isEmpty() || to.isEmpty()) {
            LOGGER.log(Level.WARNING, "Ignoring relocation with empty package: ''{0}'' => ''{1}''",
                    new Object[]{fromPackage, toPackage});
            return;
        }
        if (isRestrictedRoot(from)) {
            LOGGER.log(Level.WARNING, "Ignoring relocation from restricted root: ''{0}''", from);
            return;
        }
        if (from.equals(to)) {
            return;
        }

        relocations.put(from, to);
        LOGGER.fine(() -> "Registered relocation: " + from + " => " + to);
    }

    /**
     * Remaps the specified jar into the configured remapped directory using registered relocations. When no relocations.
     * are registered the jar is copied verbatim while stripping invalid signature files.
     *
     * @param inputJar  original jar file
     * @param outputJar destination jar that will be overwritten
     *
     * @throws IOException if remapping fails or produces invalid output
     */
    public void remap(@NotNull final Path inputJar, @NotNull final Path outputJar) throws IOException {
        Objects.requireNonNull(inputJar, "inputJar");
        Objects.requireNonNull(outputJar, "outputJar");

        if (relocations.isEmpty()) {
            // If no relocations are registered, just copy preserving manifest.
            copyJarVerbatim(inputJar, outputJar);
            return;
        }

        ensureParent(outputJar);

        try {
            // Delegate to the shared utility to avoid duplicate logic.
            final PackageRemapper remapper = new PackageRemapper();
            remapper.addMappings(relocations);
            remapper.remap(inputJar, outputJar);
        } catch (IOException ioe) {
            safeDelete(outputJar);
            throw ioe;
        }

        if (!isValidJar(outputJar)) {
            safeDelete(outputJar);
            throw new IOException("Remapper produced no valid output for: " + inputJar.getFileName());
        }

        LOGGER.fine(() -> "Remapped " + inputJar.getFileName() + " -> " + outputJar.getFileName() + " at " + Instant.now());
    }

    // ----------------- Legacy JEDependency-compatible lifecycle -----------------

    /**
     * Legacy entry point that downloads declared dependencies, applies relocations when configured and injects them.
     * into the plugin's class loader. The method is synchronous and logs failures rather than propagating them.
     *
     * @param additionalDependencies optional coordinates ({@code group:artifact:version[:classifier]}) appended to the YAML list
     */
    public void initialize(@Nullable final String[] additionalDependencies) {
        final Class<?> deencapClass = anchorClass != null ? anchorClass : getClass();
        try {
            Deencapsulation.deencapsulate(deencapClass);
        } catch (final Exception ex) {
            LOGGER.log(Level.FINE, ex, () -> "Deencapsulation failed (continuing): " + ex.getMessage());
        }

        loadYamlDependencies(deencapClass);
        if (additionalDependencies != null && additionalDependencies.length > 0) {
            addDependencies(additionalDependencies);
        }

        final ClassLoader cl = anchorClass != null
                ? anchorClass.getClassLoader()
                : Thread.currentThread().getContextClassLoader();

        try {
            downloadRemapAndInject(cl);
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dependencies with remapping", ex);
        }
    }

    /**
     * Registers additional dependency coordinates provided at runtime. Invalid coordinates are logged and skipped.
     *
     * @param additionalDependencies coordinates to add ({@code group:artifact:version[:classifier]})
     */
    public void addDependencies(@Nullable final String[] additionalDependencies) {
        if (additionalDependencies == null || additionalDependencies.length == 0) {
            return;
        }
        for (final String dep : additionalDependencies) {
            final DependencyCoordinate coord = DependencyCoordinate.parse(dep);
            if (coord != null) {
                coordinates.add(coord);
                LOGGER.log(Level.FINE, () -> "Added additional dependency: " + dep);
            } else {
                LOGGER.log(Level.WARNING, "Invalid dependency coordinate: {0}", dep);
            }
        }
    }

    /**
     * Performs downloading, remapping and injection using the supplied class loader rather than the anchor class's.
     * loader. Intended for legacy reflective invocation by {@link de.jexcellence.dependency.JEDependency}.
     *
     * @param classLoader class loader that should receive injected jars
     */
    public void loadAll(@NotNull final ClassLoader classLoader) {
        try {
            downloadRemapAndInject(classLoader);
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to load all dependencies with remapping", ex);
        }
    }

    /**
     * Downloads, remaps and injects dependencies. Refactored to reduce cognitive complexity.
     *
     * @param classLoader the class loader to inject into
     * @throws IOException if operations fail
     */
    private void downloadRemapAndInject(@NotNull final ClassLoader classLoader) throws IOException {
        final List<DependencyCoordinate> toProcess = new ArrayList<>(coordinates);

        if (toProcess.isEmpty()) {
            LOGGER.info("No dependencies registered for remapping/injection");
            return;
        }

        final List<Path> inputJars = downloadDependencies(toProcess);
        final List<Path> jarsToInject = remapIfNeeded(inputJars);
        injectJars(jarsToInject, classLoader);
    }

    /**
     * Downloads all dependencies and returns the list of downloaded JAR paths.
     *
     * @param toProcess list of coordinates to download
     * @return list of successfully downloaded JAR paths
     */
    private @NotNull List<Path> downloadDependencies(@NotNull final List<DependencyCoordinate> toProcess) {
        final List<Path> inputJars = new ArrayList<>();

        for (final DependencyCoordinate coordinate : toProcess) {
            final DownloadResult result = downloader.download(coordinate, librariesDirectory.toFile());
            if (result.success() && result.file() != null) {
                inputJars.add(result.file().toPath());
                LOGGER.log(Level.FINE, () -> "Downloaded " + coordinate.toGavString() + " -> " + result.file().getName());
            } else {
                LOGGER.log(Level.WARNING, "Failed to download dependency: {0}", coordinate.toGavString());
            }
        }

        return inputJars;
    }

    /**
     * Remaps JARs if relocations are configured, otherwise returns the input list unchanged.
     *
     * @param inputJars list of input JAR paths
     * @return list of JAR paths to inject (remapped or original)
     * @throws IOException if remapping fails critically
     */
    private @NotNull List<Path> remapIfNeeded(@NotNull final List<Path> inputJars) throws IOException {
        final List<Path> jarsToInject = new ArrayList<>();

        if (relocations.isEmpty()) {
            jarsToInject.addAll(inputJars);
            LOGGER.info("No relocations specified; injecting original libraries");
            return jarsToInject;
        }

        ensureDirectories();
        for (final Path input : inputJars) {
            final Path output = remappedDirectory.resolve(input.getFileName().toString());

            if (isOutputUpToDate(output, input)) {
                LOGGER.log(Level.FINE, () -> "Using cached remapped JAR: " + output.getFileName());
                jarsToInject.add(output);
                continue;
            }

            try {
                remap(input, output);
                jarsToInject.add(output);
            } catch (final IOException ex) {
                LOGGER.log(Level.WARNING, ex, () -> "Remapping failed for " + input.getFileName() + " (injecting original)");
                jarsToInject.add(input);
            }
        }

        LOGGER.log(Level.INFO, "Remapping complete: {0} artifact(s) prepared", jarsToInject.size());
        return jarsToInject;
    }

    /**
     * Injects the provided JARs into the class loader.
     *
     * @param jarsToInject list of JAR paths to inject
     * @param classLoader  target class loader
     */
    private void injectJars(@NotNull final List<Path> jarsToInject, @NotNull final ClassLoader classLoader) {
        int injected = 0;
        for (final Path jar : jarsToInject) {
            try {
                if (injector.tryInject(classLoader, jar.toFile())) {
                    injected++;
                    LOGGER.log(Level.FINE, () -> "Injected into classpath: " + jar.getFileName());
                } else {
                    LOGGER.log(Level.WARNING, "Injection failed: {0}", jar.getFileName());
                }
            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING, ex, () -> "Injection error for " + jar.getFileName());
            }
        }
        LOGGER.log(Level.INFO, "Injected {0} of {1} JAR(s) into classpath",
                new Object[]{injected, jarsToInject.size()});
    }

    private void loadYamlDependencies(@NotNull final Class<?> contextClass) {
        try {
            final List<String> yamlDeps = yamlLoader.loadDependencies(contextClass);
            if (yamlDeps == null || yamlDeps.isEmpty()) {
                LOGGER.info("No dependencies found in YAML configuration");
                return;
            }
            LOGGER.log(Level.INFO, "Loaded {0} dependencies from YAML", yamlDeps.size());
            for (final String dep : yamlDeps) {
                final DependencyCoordinate coord = DependencyCoordinate.parse(dep);
                if (coord != null) {
                    coordinates.add(coord);
                    LOGGER.log(Level.FINE, () -> "YAML dependency: " + dep);
                } else {
                    LOGGER.log(Level.WARNING, "Invalid dependency format in YAML: {0}", dep);
                }
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to load YAML dependencies", ex);
        }
    }

    // ----------------- Utilities: remapping helpers -----------------

    private static Manifest copyOrCreateManifest(final JarFile in) throws IOException {
        final Manifest manifest = in.getManifest();
        if (manifest != null) {
            final Attributes main = manifest.getMainAttributes();
            if (main.getValue(Attributes.Name.MANIFEST_VERSION) == null) {
                main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            }
            return manifest;
        }
        final Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return mf;
    }

    private static void writeEntryIfAbsent(
            final JarFile in,
            final JarEntry src,
            final JarOutputStream out,
            final Set<String> written,
            final String name
    ) throws IOException {
        if (written.add(name)) {
            final JarEntry dst = new JarEntry(name);
            if (src.getLastModifiedTime() != null) {
                dst.setLastModifiedTime(src.getLastModifiedTime());
            }
            out.putNextEntry(dst);
            try (InputStream is = in.getInputStream(src)) {
                is.transferTo(out);
            }
            out.closeEntry();
        }
    }

    private static void writeBytes(
            final JarOutputStream out,
            final Set<String> written,
            final String name,
            final byte[] bytes
    ) throws IOException {
        if (written.add(name)) {
            final JarEntry dst = new JarEntry(name);
            out.putNextEntry(dst);
            out.write(bytes);
            out.closeEntry();
        }
    }

    private static byte[] readAll(final JarFile in, final JarEntry entry) throws IOException {
        try (InputStream is = in.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    private static byte[] transformClass(final byte[] original, final Remapper remapper) {
        final ClassReader cr = new ClassReader(original);
        final ClassWriter cw = new ClassWriter(0);
        final ClassRemapper classRemapper = new ClassRemapper(cw, remapper);
        cr.accept(classRemapper, 0);
        return cw.toByteArray();
    }

    private static void copyJarVerbatim(@NotNull final Path inputJar, @NotNull final Path outputJar) throws IOException {
        ensureParent(outputJar);
        try (JarFile in = new JarFile(inputJar.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(outputJar), copyOrCreateManifest(in))) {

            final AtomicInteger count = new AtomicInteger(0);
            in.stream()
              .sorted(Comparator.comparing(JarEntry::getName))
              .forEach(entry -> {
                  try {
                      if (!entry.isDirectory()) {
                          writeEntryIfAbsent(in, entry, out, new LinkedHashSet<>(), entry.getName());
                          count.incrementAndGet();
                      }
                  } catch (IOException e) {
                      throw new UncheckedIOException(e);
                  }
              });
            out.flush();
            LOGGER.log(Level.FINE, () -> "Copied " + count.get() + " entries: " + inputJar.getFileName() + " -> " + outputJar.getFileName());
        } catch (UncheckedIOException uioe) {
            safeDelete(outputJar);
            throw uioe.getCause();
        } catch (IOException ioe) {
            safeDelete(outputJar);
            throw ioe;
        }
    }

    private static boolean isValidJar(@NotNull final Path jar) {
        try {
            return Files.isRegularFile(jar)
                    && Files.size(jar) >= MINIMUM_VALID_JAR_SIZE
                    && jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
        } catch (IOException e) {
            return false;
        }
    }

    private static void ensureParent(@NotNull final Path file) throws IOException {
        final Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void safeDelete(@NotNull final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, ex, () -> "Failed to delete file: " + file);
        }
    }

    private static String normalizePackage(@Nullable final String pkg) {
        if (pkg == null) return "";
        return pkg.trim().replaceAll("\\.$", "");
    }

    private static boolean isRestrictedRoot(final String pkg) {
        // JDK internals
        if (pkg.equals("java") || pkg.startsWith("java.")
                || pkg.equals("javax") || pkg.startsWith("javax.")
                || pkg.equals("jakarta") || pkg.startsWith("jakarta.")) {
            return true;
        }
        // Hibernate - must not be relocated as it expects original Jackson paths
        if (pkg.equals("org.hibernate") || pkg.startsWith("org.hibernate.")) {
            return true;
        }
        // Jackson 3.x: com.fasterxml only contains annotations now, but Hibernate
        // references com.fasterxml.jackson.core which doesn't exist in Jackson 3.x.
        // Excluding com.fasterxml prevents broken relocations.
        if (pkg.equals("com.fasterxml") || pkg.startsWith("com.fasterxml.")) {
            return true;
        }
        return false;
    }

    private static String relocateResourcePath(final String name, final Map<String, String> relocations) {
        String bestFrom = null;
        String bestTo = null;
        int bestLen = -1;

        for (Map.Entry<String, String> e : relocations.entrySet()) {
            final String fromPath = e.getKey().replace('.', JAR_PATH_SEPARATOR) + JAR_PATH_SEPARATOR;
            if (name.startsWith(fromPath) && fromPath.length() > bestLen) {
                bestFrom = fromPath;
                bestTo = e.getValue().replace('.', JAR_PATH_SEPARATOR) + JAR_PATH_SEPARATOR;
                bestLen = fromPath.length();
            }
        }

        if (bestFrom != null) {
            return bestTo + name.substring(bestFrom.length());
        }
        return name;
    }

    /**
     * Loads relocations from system properties. Refactored to reduce loop complexity.
     */
    private void loadRelocationsFromSystemProperties() {
        // Explicit mappings
        final String spec = System.getProperty(RELOCATIONS_PROPERTY);
        if (spec != null && !spec.trim().isEmpty()) {
            final String[] pairs = spec.split(",");
            for (final String pair : pairs) {
                processRelocationPair(pair);
            }
        }

        // Automatic base prefix can be used by external loaders; we don't auto-detect roots here in legacy mode.
        final String basePrefix = System.getProperty(RELOCATIONS_PREFIX_PROPERTY);
        if (basePrefix != null && !basePrefix.trim().isEmpty()) {
            LOGGER.log(Level.FINE, () -> "Relocation base prefix configured (no automatic detection in legacy path): " + basePrefix.trim());
        }

        final String excludes = System.getProperty(RELOCATIONS_EXCLUDES_PROPERTY);
        if (excludes != null && !excludes.trim().isEmpty()) {
            LOGGER.log(Level.FINE, () -> "Additional excluded relocation roots: " + excludes);
        }
    }

    /**
     * Processes a single relocation pair from system properties.
     *
     * @param pair the relocation pair string in "from=>to" format
     */
    private void processRelocationPair(@NotNull final String pair) {
        final String t = pair.trim();
        if (t.isEmpty()) {
            return;
        }

        final int idx = t.indexOf("=>");
        if (idx <= 0 || idx >= t.length() - 2) {
            LOGGER.log(Level.WARNING, "Invalid relocation mapping (expected ''from=>to''): {0}", t);
            return;
        }

        final String from = normalizePackage(t.substring(0, idx));
        final String to = normalizePackage(t.substring(idx + 2));

        if (from.isEmpty() || to.isEmpty()) {
            LOGGER.log(Level.WARNING, "Ignoring empty relocation mapping: {0}", t);
            return;
        }

        relocate(from, to);
    }

    // ----------------- Utilities: up-to-date check -----------------

    private static boolean isOutputUpToDate(@NotNull final Path output, @NotNull final Path input) {
        try {
            if (!Files.exists(output) || !Files.isRegularFile(output)) {
                return false;
            }
            final FileTime inTime = Files.getLastModifiedTime(input);
            final FileTime outTime = Files.getLastModifiedTime(output);
            final long outSize = Files.size(output);
            final boolean sizeValid = outSize > 0;
            final boolean timeValid = outTime.compareTo(inTime) >= 0;
            return sizeValid && timeValid;
        } catch (IOException e) {
            return false;
        }
    }

    // ----------------- Utilities: directory management -----------------

    private void ensureDirectories() {
        try {
            Files.createDirectories(librariesDirectory);
            Files.createDirectories(remappedDirectory);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "Failed to create libraries directories: " + e.getMessage());
        }
    }

    // ----------------- Debug helper -----------------

    /**
     * Provides a diagnostic summary of the manager's state including directories, dependency and relocation counts.
     *
     * @return formatted debug string useful for logging
     */
    @SuppressWarnings("unused")
    public String debugInfo() {
        final String base = "dataDir=" + dataDirectory
                + ", libraries=" + librariesDirectory
                + ", remapped=" + remappedDirectory
                + ", deps=" + coordinates.size()
                + ", relocations=" + relocations.size();
        
        final String pluginInfo;
        if (plugin != null) {
            final String anchorName = anchorClass != null ? anchorClass.getName() : "null";
            pluginInfo = ", plugin=" + plugin.getName() + ", anchor=" + anchorName;
        } else {
            pluginInfo = ", plugin=null, anchor=null";
        }
        
        return base + pluginInfo;
    }

    // ----------------- Optional: helper to decode file URL to path -----------------

    @SuppressWarnings("unused")
    private static Path pathFromUrl(@NotNull final String url) {
        final String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        return Path.of(decoded.replace("file:", ""));
    }
}
