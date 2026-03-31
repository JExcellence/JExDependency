package de.jexcellence.dependency.remapper;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Remaps package prefixes for classes and resources contained within a JAR. The implementation preserves manifests,.
 * rewrites {@code META-INF/services} descriptors, supports multi-release layouts and strips signature files that become
 * invalid once content is modified. It is used by both the Paper loader and legacy remapping manager to relocate
 * third-party dependencies into plugin-specific namespaces.
 */
public class PackageRemapper {

    private static final Logger LOGGER = Logger.getLogger(PackageRemapper.class.getName());

    private final Map<String, String> packageMappings = new LinkedHashMap<>();

    /**
     * Registers a relocation from the supplied original package to the target package. Package names are normalised and.
     * empty/self-mapping values are ignored.
     *
     * @param originalPackage source package to relocate (dot notation)
     * @param remappedPackage destination package (dot notation)
     */
    public void addMapping(@NotNull final String originalPackage, @NotNull final String remappedPackage) {
        final String from = normalizePackage(originalPackage);
        final String to = normalizePackage(remappedPackage);
        if (from.isEmpty() || to.isEmpty() || from.equals(to)) {
            return;
        }
        packageMappings.put(from, to);
    }

    /**
     * Registers all mappings from the provided map using {@link #addMapping(String, String)} semantics.
     *
     * @param mappings map of {@code fromPackage -> toPackage} entries in dot notation
     */
    public void addMappings(@NotNull final Map<String, String> mappings) {
        mappings.forEach(this::addMapping);
    }

    /**
     * Applies registered relocations to the input jar and writes the transformed jar to {@code outputJar}. When no.
     * relocations are registered the method performs a verbatim copy while still stripping invalid signature files.
     *
     * @param inputJar   path to the jar that should be remapped
     * @param outputJar  destination path that will be overwritten
     *
     * @throws IOException when reading or writing jar contents fails
     */
    public void remap(@NotNull final Path inputJar, @NotNull final Path outputJar) throws IOException {
        if (packageMappings.isEmpty()) {
            // No relocations -> verbatim copy with manifest preservation
            copyVerbatim(inputJar, outputJar);
            return;
        }

        Files.createDirectories(outputJar.getParent());

        try (JarFile in = new JarFile(inputJar.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(outputJar), copyOrCreateManifest(in))) {

            final Set<String> written = new LinkedHashSet<>();
            // JarOutputStream writes the manifest immediately when constructed with it.
            // Prevent re-writing it while iterating entries.
            written.add("META-INF/MANIFEST.MF");

            final PrefixRelocationRemapper remapper = new PrefixRelocationRemapper(packageMappings);

            in.stream()
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .forEach(entry -> {
                        try {
                            if (entry.isDirectory()) {
                                return;
                            }

                            final String name = entry.getName();

                            // Strip signature files since remapping changes content
                            if (isSignatureFile(name)) {
                                return;
                            }

                            // Skip manifest/index: manifest already written by JarOutputStream, index is unnecessary
                            if (isManifestFile(name) || isIndexList(name)) {
                                return;
                            }

                            if (isServiceFile(name)) {
                                final byte[] data = readAll(in, entry);
                                final byte[] rewritten = rewriteServiceFile(data, packageMappings);
                                writeBytes(out, written, name, rewritten);
                                return;
                            }

                            if (name.startsWith("META-INF/") && !name.startsWith("META-INF/versions/")) {
                                // Copy other META-INF entries as-is
                                writeEntryIfAbsent(in, entry, out, written, name);
                                return;
                            }

                            if (name.endsWith(".class")) {
                                final byte[] original = readAll(in, entry);
                                final byte[] transformed = transformClass(original, remapper);
                                final String remappedName = relocateResourcePath(name, packageMappings);
                                writeBytes(out, written, remappedName, transformed);
                            } else {
                                final String remappedName = relocateResourcePath(name, packageMappings);
                                writeEntryIfAbsent(in, entry, out, written, remappedName);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            out.flush();
        } catch (RuntimeException rte) {
            // unwrap IOExceptions
            if (rte.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw rte;
        } catch (IOException ioe) {
            try {
                Files.deleteIfExists(outputJar);
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Failed to delete incomplete output: " + outputJar, ex);
            }
            throw ioe;
        }
    }

    // --------- helpers ---------

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

    private static void copyVerbatim(@NotNull final Path inputJar, @NotNull final Path outputJar) throws IOException {
        Files.createDirectories(outputJar.getParent());
        try (JarFile in = new JarFile(inputJar.toFile());
             JarOutputStream out = new JarOutputStream(Files.newOutputStream(outputJar), copyOrCreateManifest(in))) {

            final Set<String> written = new LinkedHashSet<>();
            // Prevent rewriting manifest; JarOutputStream has already written it.
            written.add("META-INF/MANIFEST.MF");

            in.stream()
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .forEach(entry -> {
                        try {
                            if (entry.isDirectory()) {
                                return;
                            }
                            final String name = entry.getName();

                            // Strip signature files that will be invalid if content changes (even if we don't transform)
                            if (isSignatureFile(name)) {
                                return;
                            }
                            // Skip manifest (already present) and index list
                            if (isManifestFile(name) || isIndexList(name)) {
                                return;
                            }

                            writeEntryIfAbsent(in, entry, out, written, name);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            out.flush();
        } catch (RuntimeException rte) {
            if (rte.getCause() instanceof IOException ioe) {
                throw ioe;
            }
            throw rte;
        }
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
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(8192, Math.toIntExact(entry.getSize() > 0 ? entry.getSize() : 0)));
            is.transferTo(baos);
            return baos.toByteArray();
        }
    }

    private static byte[] transformClass(final byte[] original, final Remapper remapper) {
        final ClassReader reader = new ClassReader(original);
        final ClassWriter writer = new ClassWriter(0);
        final ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
        reader.accept(classRemapper, 0);
        return writer.toByteArray();
    }

    private static boolean isServiceFile(final String name) {
        return name.startsWith("META-INF/services/") && !name.endsWith("/");
    }

    private static boolean isManifestFile(final String name) {
        return "META-INF/MANIFEST.MF".equalsIgnoreCase(name);
    }

    private static boolean isIndexList(final String name) {
        return "META-INF/INDEX.LIST".equalsIgnoreCase(name);
    }

    private static boolean isSignatureFile(final String name) {
        final String lower = name.toLowerCase();
        return lower.startsWith("meta-inf/")
                && (lower.endsWith(".sf")
                || lower.endsWith(".dsa")
                || lower.endsWith(".rsa")
                || lower.endsWith(".ec")
                || lower.endsWith(".p7s"));
    }

    private static String relocateResourcePath(final String path, final Map<String, String> mappings) {
        String bestFrom = null;
        String bestTo = null;
        int bestLen = -1;

        for (Map.Entry<String, String> e : mappings.entrySet()) {
            final String fromPath = e.getKey().replace('.', '/') + "/";
            if (path.startsWith(fromPath) && fromPath.length() > bestLen) {
                bestFrom = fromPath;
                bestTo = e.getValue().replace('.', '/') + "/";
                bestLen = fromPath.length();
            }
        }

        if (bestFrom != null) {
            return bestTo + path.substring(bestFrom.length());
        }
        return path;
    }

    private static byte[] rewriteServiceFile(final byte[] original, final Map<String, String> mappings) throws IOException {
        final String content = new String(original, StandardCharsets.UTF_8);
        final String[] lines = content.split("\\R", -1);
        final StringBuilder out = new StringBuilder(content.length() + 64);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            final int hash = line.indexOf('#');
            final String comment = hash >= 0 ? line.substring(hash) : null;
            String body = hash >= 0 ? line.substring(0, hash) : line;

            final String trimmed = body.trim();
            if (!trimmed.isEmpty()) {
                final String relocated = applyDotRelocations(trimmed, mappings);
                // Preserve original leading whitespace
                final int leading = body.indexOf(trimmed);
                final String leadingWs = leading >= 0 ? body.substring(0, leading) : "";
                body = leadingWs + relocated;
            }

            out.append(body);
            if (comment != null) {
                out.append(comment);
            }
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }

        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String applyDotRelocations(final String fqcn, final Map<String, String> mappings) {
        String bestFrom = null;
        String bestTo = null;
        int bestLen = -1;
        for (Map.Entry<String, String> e : mappings.entrySet()) {
            final String from = e.getKey();
            if (fqcn.equals(from) || fqcn.startsWith(from + ".")) {
                if (from.length() > bestLen) {
                    bestFrom = from;
                    bestTo = e.getValue();
                    bestLen = from.length();
                }
            }
        }
        if (bestFrom != null) {
            return bestTo + fqcn.substring(bestFrom.length());
        }
        return fqcn;
    }

    private static String normalizePackage(final String pkg) {
        if (pkg == null) return "";
        return pkg.trim().replaceAll("\\.$", "");
    }

    // ---------- ASM remapper for package-prefix mapping ----------

    private static final class PrefixRelocationRemapper extends Remapper {
        private final Map<String, String> prefixMapInternal; // e.g., "com/example" -> "my/prefix/com/example"

        PrefixRelocationRemapper(final Map<String, String> mappings) {
            this.prefixMapInternal = new LinkedHashMap<>();
            mappings.entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(e -> -e.getKey().length())) // longest first
                    .forEach(e -> this.prefixMapInternal.put(
                            e.getKey().replace('.', '/'),
                            e.getValue().replace('.', '/')
                    ));
        }

        /**
         * Executes map.
         */
        @Override
        public String map(final String internalName) {
            if (internalName == null) return null;
            if (internalName.startsWith("[")) {
                // arrays/descriptors handled by base logic; keep simple
                return super.map(internalName);
            }
            return relocateInternal(internalName);
        }

        /**
         * Executes mapPackageName.
         */
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
}
