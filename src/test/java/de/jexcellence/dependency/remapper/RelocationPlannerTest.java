package de.jexcellence.dependency.remapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelocationPlannerTest {

    private static final String PREFIX = "com.example.plugin.libs";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("relocates every package of a library the plugin references under the prefix")
    void relocatesReferencedLibrary() throws IOException {
        final Path gson = jar("gson.jar", Map.of(
                "com/google/gson/Gson", "java/lang/Object",
                "com/google/gson/internal/Excluder", "java/lang/Object"));
        final Path guava = jar("guava.jar", Map.of("com/google/common/collect/Lists", "java/lang/Object"));
        final Path plugin = jar("plugin.jar", Map.of(
                "com/example/plugin/Main", "com/example/plugin/libs/com/google/gson/Gson"));

        final Map<String, String> plan = RelocationPlanner.plan(List.of(gson, guava), plugin, PREFIX, List.of());

        assertEquals(Map.of(
                "com.google.gson", PREFIX + ".com.google.gson",
                "com.google.gson.internal", PREFIX + ".com.google.gson.internal"), plan);
    }

    @Test
    @DisplayName("relocates nothing when the plugin uses original package names or no plugin jar is known")
    void nothingWithoutReferences() throws IOException {
        final Path gson = jar("gson.jar", Map.of("com/google/gson/Gson", "java/lang/Object"));
        final Path plugin = jar("plugin.jar", Map.of("com/example/plugin/Main", "com/google/gson/Gson"));

        assertTrue(RelocationPlanner.plan(List.of(gson), plugin, PREFIX, List.of()).isEmpty());
        assertTrue(RelocationPlanner.plan(List.of(gson), null, PREFIX, List.of()).isEmpty());
    }

    @Test
    @DisplayName("never relocates JDK or Jakarta packages, nor configured excludes")
    void honoursExcludes() throws IOException {
        final Path library = jar("lib.jar", Map.of(
                "javax/annotation/Nonnull", "java/lang/Object",
                "org/hibernate/Session", "java/lang/Object",
                "org/example/Tool", "java/lang/Object"));
        final Path plugin = jar("plugin.jar", Map.of("com/example/plugin/Main", "com/example/plugin/libs/org/example/Tool"));

        final Map<String, String> plan = RelocationPlanner.plan(List.of(library), plugin, PREFIX, List.of("org.hibernate"));

        assertEquals(Map.of("org.example", PREFIX + ".org.example"), plan);
    }

    @Test
    @DisplayName("prefix comes from the descriptor before the server default")
    void prefixOrder() {
        assertEquals("my.libs", RelocationPlanner.prefix(" my.libs. "));
        assertEquals(RelocationPlanner.DEFAULT_PREFIX, RelocationPlanner.prefix(null));
    }

    private Path jar(final String name, final Map<String, String> classes) throws IOException {
        final Path target = tempDir.resolve(name);
        try (OutputStream file = Files.newOutputStream(target);
             JarOutputStream jar = new JarOutputStream(file)) {
            for (final Map.Entry<String, String> entry : classes.entrySet()) {
                final ClassWriter writer = new ClassWriter(0);
                writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, entry.getKey(), null, entry.getValue(), null);
                writer.visitEnd();
                jar.putNextEntry(new JarEntry(entry.getKey() + ".class"));
                jar.write(writer.toByteArray());
                jar.closeEntry();
            }
        }
        return target;
    }
}
