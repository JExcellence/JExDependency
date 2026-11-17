package de.jexcellence.dependency.loader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperPluginLoaderRemapTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("remaps only the plugin's own jars, never other jars in the shared cache")
    void remapsOnlyRequestedJars() throws IOException {
        final Path sharedJars = Files.createDirectories(tempDir.resolve("cache/jars"));
        final Path ownJar = writeJar(sharedJars.resolve("alpha-1.0.jar"), "com/example/alpha/Alpha", null);
        writeJar(sharedJars.resolve("beta-1.0.jar"), "com/example/beta/Beta", null); // another plugin's library

        final Path dataDirectory = Files.createDirectories(tempDir.resolve("plugins/Example"));
        final Path remapped = dataDirectory.resolve("libraries/remapped");

        final List<Path> result = new PaperPluginLoader().remapJars(List.of(ownJar), dataDirectory, remapped, plugin());

        assertEquals(1, result.size());
        final Path setDirectory = result.get(0).getParent();
        assertEquals(remapped, setDirectory.getParent());
        try (var files = Files.list(setDirectory)) {
            assertEquals(List.of("alpha-1.0.jar"), files.map(path -> path.getFileName().toString()).toList());
        }
        try (JarFile jar = new JarFile(result.get(0).toFile())) {
            assertNotNull(jar.getEntry("de/jexcellence/remapped/com/example/alpha/Alpha.class"));
        }
    }

    @Test
    @DisplayName("loads all original jars when remapping any one of them fails")
    void fallsBackForWholeSet() throws IOException {
        final Path sharedJars = Files.createDirectories(tempDir.resolve("cache/jars"));
        final Path goodJar = writeJar(sharedJars.resolve("alpha-1.0.jar"), "com/example/alpha/Alpha", null);
        final Path brokenJar = Files.write(sharedJars.resolve("broken-1.0.jar"), new byte[]{1, 2, 3});

        final Path dataDirectory = Files.createDirectories(tempDir.resolve("plugins/Example"));
        final Path remapped = dataDirectory.resolve("libraries/remapped");

        final List<Path> result = new PaperPluginLoader().remapJars(List.of(goodJar, brokenJar), dataDirectory, remapped, plugin());

        assertEquals(List.of(goodJar, brokenJar), result);
        try (var files = Files.walk(remapped)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().equals("broken-1.0.jar")),
                    "a failed remap must not leave a partial copy that a later start would reuse");
        }
    }

    @Test
    @DisplayName("does not reuse a remapped copy after the relocation set changed")
    void changedRelocationSetIsRemappedAgain() throws IOException {
        final Path sharedJars = Files.createDirectories(tempDir.resolve("cache/jars"));
        // alpha references gamma's package; gamma is only added to the plugin later.
        final Path alpha = writeJar(sharedJars.resolve("alpha-1.0.jar"), "com/example/alpha/Alpha", "org/gamma/lib/Gamma");
        final Path gamma = writeJar(sharedJars.resolve("gamma-1.0.jar"), "org/gamma/lib/Gamma", null);

        final Path dataDirectory = Files.createDirectories(tempDir.resolve("plugins/Example"));
        final Path remapped = dataDirectory.resolve("libraries/remapped");

        final List<Path> first = new PaperPluginLoader().remapJars(List.of(alpha), dataDirectory, remapped, plugin());
        final List<Path> second = new PaperPluginLoader().remapJars(List.of(alpha, gamma), dataDirectory, remapped, plugin());

        assertNotEquals(first.get(0).getParent(), second.get(0).getParent());
        assertEquals("org/gamma/lib/Gamma",
                superNameOf(first.get(0), "de/jexcellence/remapped/com/example/alpha/Alpha.class"));
        assertEquals("de/jexcellence/remapped/org/gamma/lib/Gamma",
                superNameOf(second.get(0), "de/jexcellence/remapped/com/example/alpha/Alpha.class"));
    }

    @Test
    @DisplayName("reuses remapped copies when nothing changed")
    void unchangedSetReusesCopies() throws IOException {
        final Path sharedJars = Files.createDirectories(tempDir.resolve("cache/jars"));
        final Path alpha = writeJar(sharedJars.resolve("alpha-1.0.jar"), "com/example/alpha/Alpha", null);

        final Path dataDirectory = Files.createDirectories(tempDir.resolve("plugins/Example"));
        final Path remapped = dataDirectory.resolve("libraries/remapped");

        final List<Path> first = new PaperPluginLoader().remapJars(List.of(alpha), dataDirectory, remapped, plugin());
        final long firstModified = Files.getLastModifiedTime(first.get(0)).toMillis();
        final List<Path> second = new PaperPluginLoader().remapJars(List.of(alpha), dataDirectory, remapped, plugin());

        assertEquals(first, second);
        assertEquals(firstModified, Files.getLastModifiedTime(second.get(0)).toMillis());
    }

    @Test
    @DisplayName("leaves libraries alone that the plugin uses by their original package names")
    void unreferencedLibrariesStayUnrelocated() throws IOException {
        final Path sharedJars = Files.createDirectories(tempDir.resolve("cache/jars"));
        final Path delta = writeJar(sharedJars.resolve("delta-1.0.jar"), "org/delta/Delta", null);
        final Path dataDirectory = Files.createDirectories(tempDir.resolve("plugins/Example"));
        final Path remapped = dataDirectory.resolve("libraries/remapped");

        final List<Path> result = new PaperPluginLoader().remapJars(List.of(delta), dataDirectory, remapped, plugin());

        try (JarFile jar = new JarFile(result.get(0).toFile())) {
            assertNotNull(jar.getEntry("org/delta/Delta.class"));
        }
    }

    /** A plugin jar compiled against relocated alpha, beta and gamma, as Shadow's relocate produces. */
    private Path plugin() throws IOException {
        final Path target = tempDir.resolve("plugin.jar");
        if (Files.exists(target)) {
            return target;
        }
        try (OutputStream file = Files.newOutputStream(target);
             JarOutputStream jar = new JarOutputStream(file)) {
            int index = 0;
            for (final String referenced : List.of("com/example/alpha/Alpha", "com/example/beta/Beta",
                    "org/gamma/lib/Gamma", "com/example/broken/Broken")) {
                final ClassWriter writer = new ClassWriter(0);
                writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "com/example/plugin/Uses" + index, null,
                        "de/jexcellence/remapped/" + referenced, null);
                writer.visitEnd();
                jar.putNextEntry(new JarEntry("com/example/plugin/Uses" + index++ + ".class"));
                jar.write(writer.toByteArray());
                jar.closeEntry();
            }
        }
        return target;
    }

    private static String superNameOf(final Path jarPath, final String entryName) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            final JarEntry entry = jar.getJarEntry(entryName);
            assertNotNull(entry, entryName);
            try (var input = jar.getInputStream(entry)) {
                return new ClassReader(input.readAllBytes()).getSuperName();
            }
        }
    }

    private static Path writeJar(final Path target, final String internalClassName, final String superName)
            throws IOException {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalClassName, null,
                superName == null ? "java/lang/Object" : superName, null);
        writer.visitEnd();

        final byte[] padding = new byte[4096]; // the loader treats jars under 1 KiB as invalid output
        new Random(42).nextBytes(padding);

        try (OutputStream file = Files.newOutputStream(target);
             JarOutputStream jar = new JarOutputStream(file)) {
            jar.putNextEntry(new JarEntry(internalClassName + ".class"));
            jar.write(writer.toByteArray());
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("padding.bin"));
            jar.write(padding);
            jar.closeEntry();
        }
        return target;
    }

    @Test
    @DisplayName("libraries with the same file name from different groups get different remapped names")
    void sameFileNameDifferentGroups() {
        final Path first = tempDir.resolve("jars/a/x/1.0/x-1.0.jar");
        final Path second = tempDir.resolve("jars/b/x/1.0/x-1.0.jar");
        final Path unique = tempDir.resolve("jars/c/y/1.0/y-1.0.jar");
        final List<Path> inputs = List.of(first, second, unique);

        assertNotEquals(PaperPluginLoader.remappedFileName(inputs, first), PaperPluginLoader.remappedFileName(inputs, second));
        assertEquals("y-1.0.jar", PaperPluginLoader.remappedFileName(inputs, unique));
    }
}
