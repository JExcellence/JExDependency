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
 * RemappingDependencyManager:
 * - Paper loader mode: constructed reflectively via (Path) and used through relocate(String,String) and remap(Path,Path).
 * - Legacy JEDependency mode: constructed via (JavaPlugin, Class) or no-arg, then initialize/addDependencies/loadAll manage
 *   download, remapping and classpath injection.
 *
 * Implementation details:
 * - Uses ASM to remap class internal names, descriptors and package names based on longest-prefix package relocations.
 * - Remaps non-class resources by relocating their entry paths if they sit under relocated packages.
 * - Caches remapped outputs if up-to-date relative to inputs.
 *
 * Requirements:
 * - Add and shade ASM at build time:
 *   org.ow2.asm:asm:9.7.1
 *   org.ow2.asm:asm-commons:9.7.1
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
     * Constructor used by PaperPluginLoader via reflection.
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
     * Constructor used by legacy JEDependency via reflection, preferred when available.
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
     * Fallback no-arg constructor for compatibility. Uses working directory.
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
     * Register a package relocation from 'fromPackage' to 'toPackage'.
     * Both must be dot-notation package names.
     */
    public void relocate(@NotNull final String fromPackage, @NotNull final String toPackage) {
        final String from = normalizePackage(fromPackage);
        final String to = normalizePackage(toPackage);

        if (from.isEmpty() || to.isEmpty()) {
            LOGGER.warning("Ignoring relocation with empty package: '" + fromPackage + "' => '" + toPackage + "'");
            return;
        }
        if (isRestrictedRoot(from)) {
            LOGGER.warning("Ignoring relocation from restricted root: '" + from + "'");
            return;
        }
        if (from.equals(to)) {
            return;
        }

        relocations.put(from, to);
        LOGGER.fine(() -> "Registered relocation: " + from + " => " + to);
    }

    // Replace the existing remap method with this implementation
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
     * Preferred entrypoint in legacy mode. Loads YAML + additional dependencies, remaps and injects.
     */
    public void initialize(@Nullable final String[] additionalDependencies) {
        final Class<?> deencapClass = anchorClass != null ? anchorClass : getClass();
        try {
            Deencapsulation.deencapsulate(deencapClass);
        } catch (final Exception ex) {
            LOGGER.log(Level.FINE, "Deencapsulation failed (continuing): " + ex.getMessage(), ex);
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
     * Alternative path used by legacy fallback; only registers additional dependencies.
     */
    public void addDependencies(@Nullable final String[] additionalDependencies) {
        if (additionalDependencies == null || additionalDependencies.length == 0) {
            return;
        }
        for (final String dep : additionalDependencies) {
            final DependencyCoordinate coord = DependencyCoordinate.parse(dep);
            if (coord != null) {
                coordinates.add(coord);
                LOGGER.fine("Added additional dependency: " + dep);
            } else {
                LOGGER.warning("Invalid dependency coordinate: " + dep);
            }
        }
    }

    /**
     * Alternative path used by legacy fallback; performs download/remap/injection with provided classloader.
     */
    public void loadAll(@NotNull final ClassLoader classLoader) {
        try {
            downloadRemapAndInject(classLoader);
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to load all dependencies with remapping", ex);
        }
    }

    // ----------------- Internal orchestration -----------------

    private void downloadRemapAndInject(@NotNull final ClassLoader classLoader) throws IOException {
        // Download
        final List<Path> inputJars = new ArrayList<>();
        final List<DependencyCoordinate> toProcess = new ArrayList<>(coordinates);

        if (toProcess.isEmpty()) {
            LOGGER.info("No dependencies registered for remapping/injection");
            return;
        }

        for (final DependencyCoordinate coordinate : toProcess) {
            final DownloadResult result = downloader.download(coordinate, librariesDirectory.toFile());
            if (result.success() && result.file() != null) {
                inputJars.add(result.file().toPath());
                LOGGER.fine("Downloaded " + coordinate.toGavString() + " -> " + result.file().getName());
            } else {
                LOGGER.warning("Failed to download dependency: " + coordinate.toGavString());
            }
        }

        // Remap (if relocations exist)
        final List<Path> jarsToInject = new ArrayList<>();
        if (relocations.isEmpty()) {
            jarsToInject.addAll(inputJars);
            LOGGER.info("No relocations specified; injecting original libraries");
        } else {
            ensureDirectories();
            for (final Path input : inputJars) {
                final Path output = remappedDirectory.resolve(input.getFileName().toString());

                if (isOutputUpToDate(output, input)) {
                    LOGGER.fine("Using cached remapped JAR: " + output.getFileName());
                    jarsToInject.add(output);
                    continue;
                }

                try {
                    remap(input, output);
                    jarsToInject.add(output);
                } catch (final IOException ex) {
                    LOGGER.log(Level.WARNING, "Remapping failed for " + input.getFileName() + " (injecting original)", ex);
                    jarsToInject.add(input);
                }
            }
            LOGGER.info("Remapping complete: " + jarsToInject.size() + " artifact(s) prepared");
        }

        // Inject
        int injected = 0;
        for (final Path jar : jarsToInject) {
            try {
                if (injector.tryInject(classLoader, jar.toFile())) {
                    injected++;
                    LOGGER.fine("Injected into classpath: " + jar.getFileName());
                } else {
                    LOGGER.warning("Injection failed: " + jar.getFileName());
                }
            } catch (final Exception ex) {
                LOGGER.log(Level.WARNING, "Injection error for " + jar.getFileName(), ex);
            }
        }
        LOGGER.info("Injected " + injected + " of " + jarsToInject.size() + " JAR(s) into classpath");
    }

    private void loadYamlDependencies(@NotNull final Class<?> contextClass) {
        try {
            final List<String> yamlDeps = yamlLoader.loadDependencies(contextClass);
            if (yamlDeps == null || yamlDeps.isEmpty()) {
                LOGGER.info("No dependencies found in YAML configuration");
                return;
            }
            LOGGER.info("Loaded " + yamlDeps.size() + " dependencies from YAML");
            for (final String dep : yamlDeps) {
                final DependencyCoordinate coord = DependencyCoordinate.parse(dep);
                if (coord != null) {
                    coordinates.add(coord);
                    LOGGER.fine("YAML dependency: " + dep);
                } else {
                    LOGGER.warning("Invalid dependency format in YAML: " + dep);
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
            LOGGER.fine("Copied " + count.get() + " entries: " + inputJar.getFileName() + " -> " + outputJar.getFileName());
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
            LOGGER.log(Level.FINE, "Failed to delete file: " + file, ex);
        }
    }

    private static String normalizePackage(@Nullable final String pkg) {
        if (pkg == null) return "";
        return pkg.trim().replaceAll("\\.$", "");
    }

    private static boolean isRestrictedRoot(final String pkg) {
        return pkg.equals("java") || pkg.startsWith("java.")
                || pkg.equals("javax") || pkg.startsWith("javax.")
                || pkg.equals("jakarta") || pkg.startsWith("jakarta.");
    }

    private static String relocateResourcePath(final String name, final Map<String, String> relocations) {
        String bestFrom = null;
        String bestTo = null;
        int bestLen = -1;

        for (Map.Entry<String, String> e : relocations.entrySet()) {
            final String fromPath = e.getKey().replace('.', '/') + "/";
            if (name.startsWith(fromPath) && fromPath.length() > bestLen) {
                bestFrom = fromPath;
                bestTo = e.getValue().replace('.', '/') + "/";
                bestLen = fromPath.length();
            }
        }

        if (bestFrom != null) {
            return bestTo + name.substring(bestFrom.length());
        }
        return name;
    }

    // ----------------- Utilities: system property relocations (optional) -----------------

    private void loadRelocationsFromSystemProperties() {
        // Explicit mappings
        final String spec = System.getProperty(RELOCATIONS_PROPERTY);
        if (spec != null && !spec.trim().isEmpty()) {
            final String[] pairs = spec.split(",");
            for (final String pair : pairs) {
                final String t = pair.trim();
                if (t.isEmpty()) continue;
                final int idx = t.indexOf("=>");
                if (idx <= 0 || idx >= t.length() - 2) {
                    LOGGER.warning("Invalid relocation mapping (expected 'from=>to'): " + t);
                    continue;
                }
                final String from = normalizePackage(t.substring(0, idx));
                final String to = normalizePackage(t.substring(idx + 2));
                if (from.isEmpty() || to.isEmpty()) {
                    LOGGER.warning("Ignoring empty relocation mapping: " + t);
                    continue;
                }
                relocate(from, to);
            }
        }

        // Automatic base prefix can be used by external loaders; we don't auto-detect roots here in legacy mode.
        final String basePrefix = System.getProperty(RELOCATIONS_PREFIX_PROPERTY);
        if (basePrefix != null && !basePrefix.trim().isEmpty()) {
            LOGGER.fine("Relocation base prefix configured (no automatic detection in legacy path): " + basePrefix.trim());
        }

        final String excludes = System.getProperty(RELOCATIONS_EXCLUDES_PROPERTY);
        if (excludes != null && !excludes.trim().isEmpty()) {
            LOGGER.fine("Additional excluded relocation roots: " + excludes);
        }
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
            return outSize > 0 && outTime.compareTo(inTime) >= 0;
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
            LOGGER.log(Level.SEVERE, "Failed to create libraries directories: " + e.getMessage(), e);
        }
    }

    // ----------------- Internal ASM remapper -----------------

    /**
     * Maps class internal names and package names using longest-prefix package relocation.
     */
    private static final class PrefixRelocationRemapper extends Remapper {
        private final Map<String, String> prefixMapInternal; // "com/example" -> "my/prefix/com/example"

        PrefixRelocationRemapper(final Map<String, String> relocations) {
            // Convert to slash form and preserve insertion order; we'll do longest-prefix match in map()
            this.prefixMapInternal = new LinkedHashMap<>();
            relocations.entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(e -> -e.getKey().length())) // longest first
                    .forEach(e -> this.prefixMapInternal.put(
                            e.getKey().replace('.', '/'),
                            e.getValue().replace('.', '/')
                    ));
        }

        @Override
        public String map(final String internalName) {
            if (internalName == null) return null;
            // Handle arrays and descriptors via Remapper's default logic for simplicity
            if (internalName.startsWith("[")) {
                return super.map(internalName);
            }
            return relocateInternal(internalName);
        }

        @Override
        public String mapPackageName(final String name) {
            if (name == null) return null;
            final String internal = name.replace('.', '/');
            final String mapped = relocateInternal(internal);
            return mapped.replace('/', '.');
        }

        private String relocateInternal(final String internal) {
            for (Map.Entry<String, String> e : prefixMapInternal.entrySet()) {
                final String from = e.getKey();
                if (internal.equals(from) || internal.startsWith(from + "/")) {
                    return e.getValue() + internal.substring(from.length());
                }
            }
            return internal;
        }
    }

    // ----------------- Debug helper -----------------

    @SuppressWarnings("unused")
    public String debugInfo() {
        final String base = "dataDir=" + dataDirectory
                + ", libraries=" + librariesDirectory
                + ", remapped=" + remappedDirectory
                + ", deps=" + coordinates.size()
                + ", relocations=" + relocations.size();
        final String pluginInfo;
        if (plugin != null) {
            pluginInfo = ", plugin=" + plugin.getName()
                    + ", anchor=" + (anchorClass != null ? anchorClass.getName() : "null");
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