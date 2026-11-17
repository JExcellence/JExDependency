package de.jexcellence.dependency.remapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decides which library packages are relocated, the same way on Spigot and Paper.
 *
 * <p>A library jar is relocated only when the plugin's own classes already reference one of its packages under
 * the relocation prefix, which is what Shadow's {@code relocate("com.google.gson", "<prefix>.com.google.gson")}
 * produces in a plugin that declares the library as {@code compileOnly}. A plugin that references a library by its
 * original package names gets it unrelocated, so relocation can no longer break it. When a jar is relocated, all
 * of its packages move together so its internal references stay consistent.</p>
 *
 * <p>Packages of the JDK, Jakarta and XML APIs are never relocated. Projects add their own exclusions with
 * {@code relocation-excludes:} in {@code dependencies.yml}; servers with {@value #EXCLUDES_PROPERTY}.</p>
 */
public final class RelocationPlanner {

    /** System property with the default package prefix for automatic relocation. */
    public static final String PREFIX_PROPERTY = "jedependency.relocations.prefix";
    /** System property with comma-separated packages that are never relocated. */
    public static final String EXCLUDES_PROPERTY = "jedependency.relocations.excludes";
    /** Prefix used when neither the plugin nor the server sets one. */
    public static final String DEFAULT_PREFIX = "de.jexcellence.remapped";

    /** Packages that are never relocated. */
    public static final List<String> DEFAULT_EXCLUDES = List.of(
            "java", "javax", "jakarta", "sun", "com.sun", "jdk", "org.w3c", "org.xml", "org.ietf");

    private static final Logger LOGGER = Logger.getLogger("JExDependency");
    private static final String VERSIONED_PREFIX = "META-INF/versions/";

    private RelocationPlanner() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Plans the relocations for a plugin.
     *
     * @param libraryJars  the plugin's libraries
     * @param pluginJar    the plugin jar whose classes are scanned for relocated references; {@code null} plans
     *                     nothing
     * @param prefix       package prefix, for example {@code com.example.myplugin.libs}
     * @param extraExcludes packages to leave alone in addition to {@link #DEFAULT_EXCLUDES}
     * @return mapping from original package to relocated package, in a stable order
     */
    public static @NotNull Map<String, String> plan(
            @NotNull final List<Path> libraryJars,
            @Nullable final Path pluginJar,
            @NotNull final String prefix,
            @NotNull final Collection<String> extraExcludes
    ) {
        final String normalizedPrefix = trimDots(prefix);
        if (pluginJar == null || normalizedPrefix.isEmpty()) {
            return Map.of();
        }

        final Set<String> referenced = referencedPackages(pluginJar, normalizedPrefix);
        if (referenced.isEmpty()) {
            LOGGER.log(Level.FINE, "The plugin references no packages under {0}; nothing is relocated", normalizedPrefix);
            return Map.of();
        }

        final List<String> excludes = new ArrayList<>(DEFAULT_EXCLUDES);
        extraExcludes.stream().map(RelocationPlanner::trimDots).filter(value -> !value.isEmpty()).forEach(excludes::add);

        final Map<String, String> relocations = new LinkedHashMap<>();
        for (final Path jar : libraryJars) {
            final Set<String> packages = new TreeSet<>(packagesOf(jar));
            packages.removeIf(name -> isExcluded(name, excludes));
            if (packages.stream().noneMatch(referenced::contains)) {
                continue;
            }
            for (final String name : packages) {
                relocations.put(name, normalizedPrefix + "." + name);
            }
            LOGGER.log(Level.FINE, () -> "Relocating " + jar.getFileName() + " (" + packages.size() + " packages)");
        }
        return relocations;
    }

    /**
     * Resolves the prefix: the plugin's {@code relocation-prefix:}, then {@value #PREFIX_PROPERTY}, then
     * {@value #DEFAULT_PREFIX}.
     *
     * @param fromDescriptor value from {@code dependencies.yml}, may be {@code null}
     * @return the prefix without leading or trailing dots
     */
    public static @NotNull String prefix(@Nullable final String fromDescriptor) {
        if (fromDescriptor != null && !trimDots(fromDescriptor).isEmpty()) {
            return trimDots(fromDescriptor);
        }
        final String property = System.getProperty(PREFIX_PROPERTY);
        return property != null && !trimDots(property).isEmpty() ? trimDots(property) : DEFAULT_PREFIX;
    }

    /**
     * Returns the excludes from {@value #EXCLUDES_PROPERTY} plus the given ones.
     *
     * @param fromDescriptor excludes from {@code dependencies.yml}
     * @return combined list
     */
    public static @NotNull List<String> excludes(@NotNull final Collection<String> fromDescriptor) {
        final List<String> excludes = new ArrayList<>(fromDescriptor);
        final String property = System.getProperty(EXCLUDES_PROPERTY);
        if (property != null) {
            for (final String value : property.split(",")) {
                if (!value.isBlank()) {
                    excludes.add(value.trim());
                }
            }
        }
        return excludes;
    }

    /**
     * Tells whether a package must never be relocated.
     *
     * @param packageName package in dot notation
     * @return {@code true} for JDK, Jakarta and XML API packages
     */
    public static boolean isAlwaysExcluded(@NotNull final String packageName) {
        return isExcluded(packageName, DEFAULT_EXCLUDES);
    }

    // -------------------------------------------------------------------------

    /** Package names of the classes in a jar, including multi-release entries. */
    static @NotNull Set<String> packagesOf(@NotNull final Path jar) {
        final Set<String> packages = new LinkedHashSet<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.endsWith("module-info.class")) {
                    continue;
                }
                if (name.startsWith(VERSIONED_PREFIX)) {
                    final int slash = name.indexOf('/', VERSIONED_PREFIX.length());
                    if (slash < 0) {
                        continue;
                    }
                    name = name.substring(slash + 1);
                }
                final int lastSlash = name.lastIndexOf('/');
                if (lastSlash > 0) {
                    packages.add(name.substring(0, lastSlash).replace('/', '.'));
                }
            }
        } catch (final IOException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Could not read packages of " + jar);
        }
        return packages;
    }

    /**
     * Finds the packages the plugin's classes reference under the prefix, by scanning the class files for the
     * prefix in internal-name form. Constant-pool strings of ASCII names are stored as plain bytes, so a byte
     * search finds every class reference, descriptor and signature.
     */
    static @NotNull Set<String> referencedPackages(@NotNull final Path pluginJar, @NotNull final String prefix) {
        final byte[] needle = (prefix.replace('.', '/') + "/").getBytes(StandardCharsets.US_ASCII);
        final Set<String> packages = new LinkedHashSet<>();
        try (JarFile jarFile = new JarFile(pluginJar.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream input = jarFile.getInputStream(entry)) {
                    collect(input.readAllBytes(), needle, packages);
                }
            }
        } catch (final IOException exception) {
            LOGGER.log(Level.WARNING, exception, () -> "Could not scan the plugin jar " + pluginJar
                    + " for relocated references; nothing is relocated");
            return Set.of();
        }
        return packages;
    }

    private static void collect(final byte[] bytes, final byte[] needle, final Set<String> packages) {
        for (int index = indexOf(bytes, needle, 0); index >= 0; index = indexOf(bytes, needle, index + 1)) {
            int end = index + needle.length;
            while (end < bytes.length && isNameByte(bytes[end])) {
                end++;
            }
            final String internalName = new String(bytes, index + needle.length, end - index - needle.length,
                    StandardCharsets.US_ASCII);
            final int lastSlash = internalName.lastIndexOf('/');
            if (lastSlash > 0) {
                packages.add(internalName.substring(0, lastSlash).replace('/', '.'));
            }
        }
    }

    private static boolean isNameByte(final byte value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z') || (value >= '0' && value <= '9')
                || value == '/' || value == '_' || value == '$';
    }

    private static int indexOf(final byte[] haystack, final byte[] needle, final int from) {
        outer:
        for (int index = from; index <= haystack.length - needle.length; index++) {
            for (int offset = 0; offset < needle.length; offset++) {
                if (haystack[index + offset] != needle[offset]) {
                    continue outer;
                }
            }
            return index;
        }
        return -1;
    }

    private static boolean isExcluded(@NotNull final String packageName, @NotNull final Collection<String> excludes) {
        for (final String exclude : excludes) {
            if (packageName.equals(exclude) || packageName.startsWith(exclude + ".")) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull String trimDots(@NotNull final String value) {
        String trimmed = value.trim();
        while (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
