package de.jexcellence.dependency.remapper;

import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.loader.YamlDependencyLoader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RemappingDependencyManager} focusing on relocation registration, remapping and
 * the legacy {@link RemappingDependencyManager#initialize(String[])} / {@link RemappingDependencyManager#loadAll(ClassLoader)}
 * pipeline. The tests replace the internal collaborators with controllable mocks to ensure deterministic behaviour.
 */
class RemappingDependencyManagerTest {

    private static final Logger LOGGER = Logger.getLogger(RemappingDependencyManager.class.getName());

    @TempDir
    Path tempDir;

    private final List<LogRecord> capturedLogs = new CopyOnWriteArrayList<>();
    private Handler handler;

    @BeforeEach
    void setUpLoggerCapture() {
        capturedLogs.clear();
        handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                capturedLogs.add(record);
            }

            @Override
            public void flush() {
                // No-op
            }

            @Override
            public void close() {
                // No-op
            }
        };
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
    }

    @AfterEach
    void tearDownLoggerCapture() {
        if (handler != null) {
            LOGGER.removeHandler(handler);
        }
        capturedLogs.clear();
        handler = null;
    }

    @Test
    void relocateShouldRegisterMappingsAndLogWarningsForInvalidInput() {
        final RemappingDependencyManager manager = new RemappingDependencyManager(tempDir);

        manager.relocate(" com.example.lib ", " shadow.example.lib ");
        manager.relocate("java.util", "shadow.blocked");
        manager.relocate("", "shadow.empty");

        final Map<String, String> relocations = readRelocations(manager);
        assertEquals(1, relocations.size());
        assertEquals("shadow.example.lib", relocations.get("com.example.lib"));
        assertFalse(relocations.containsKey("java.util"));

        assertTrue(capturedLogs.stream()
                .filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
                .anyMatch(record -> record.getMessage().contains("Ignoring relocation")));
    }

    @Test
    void remapShouldApplyRelocationsToJarEntries() throws IOException {
        final RemappingDependencyManager manager = new RemappingDependencyManager(tempDir);
        manager.relocate("com.example", "shadow.example");

        final Path inputJar = tempDir.resolve("demo-1.0.0.jar");
        createJar(inputJar, Map.of(
                "com/example/Demo.class", generateSimpleClass("com/example/Demo"),
                "padding.bin", randomBytes(2048)
        ));

        final Path outputJar = tempDir.resolve("remapped/demo-1.0.0.jar");
        Files.createDirectories(outputJar.getParent());

        manager.remap(inputJar, outputJar);

        assertTrue(Files.exists(outputJar));
        assertTrue(Files.size(outputJar) >= 1024);

        try (JarFile jarFile = new JarFile(outputJar.toFile())) {
            assertNotNull(jarFile.getJarEntry("shadow/example/Demo.class"));
            assertFalse(jarFile.stream().anyMatch(entry -> entry.getName().equals("com/example/Demo.class")));
        }
    }

    @Test
    void initializeAndLoadAllShouldRemapInjectAndReuseCachedOutputs() throws Exception {
        final Path pluginDir = Files.createDirectory(tempDir.resolve("plugin"));
        final JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(pluginDir.toFile());

        final RemappingDependencyManager manager = Mockito.spy(new RemappingDependencyManager(
                plugin,
                RemappingDependencyManagerTest.class
        ));

        final DependencyDownloader downloader = Mockito.mock(DependencyDownloader.class);
        final ClasspathInjector injector = Mockito.mock(ClasspathInjector.class);
        final YamlDependencyLoader yamlLoader = Mockito.mock(YamlDependencyLoader.class);

        setInternalField(manager, "downloader", downloader);
        setInternalField(manager, "injector", injector);
        setInternalField(manager, "yamlLoader", yamlLoader);

        final DependencyCoordinate coordinate = DependencyCoordinate.parse("com.example:demo:1.0.0");
        final Path downloadedJar = pluginDir.resolve("demo-1.0.0.jar");
        createJar(downloadedJar, Map.of(
                "com/example/Demo.class", generateSimpleClass("com/example/Demo"),
                "padding.bin", randomBytes(2048)
        ));

        Mockito.when(yamlLoader.loadDependencies(RemappingDependencyManagerTest.class))
                .thenReturn(List.of(coordinate.toGavString()));
        Mockito.when(downloader.download(Mockito.any(), Mockito.any()))
                .thenReturn(DownloadResult.success(coordinate, downloadedJar.toFile()));
        Mockito.when(injector.tryInject(Mockito.any(), Mockito.any())).thenReturn(true);

        manager.relocate("com.example", "shadow.example");

        manager.initialize(null);

        final Path remappedJar = pluginDir
                .resolve("libraries")
                .resolve("remapped")
                .resolve(downloadedJar.getFileName());

        assertTrue(Files.exists(remappedJar), "Remapped jar should exist after initialization");
        assertTrue(Files.size(remappedJar) >= 1024);

        try (JarFile jarFile = new JarFile(remappedJar.toFile())) {
            assertNotNull(jarFile.getJarEntry("shadow/example/Demo.class"));
        }

        final FileTime originalTime = Files.getLastModifiedTime(remappedJar);

        manager.loadAll(RemappingDependencyManagerTest.class.getClassLoader());

        Mockito.verify(manager, Mockito.times(1)).remap(Mockito.any(), Mockito.any());
        Mockito.verify(injector, Mockito.atLeast(2)).tryInject(Mockito.any(), Mockito.any());

        final FileTime afterTime = Files.getLastModifiedTime(remappedJar);
        assertTrue(afterTime.compareTo(originalTime) >= 0, "Cached remapped jar timestamp should not decrease");
    }

    @Test
    void loadAllShouldLogWarningsWhenDownloadsFail() throws Exception {
        final RemappingDependencyManager manager = new RemappingDependencyManager(tempDir);
        final DependencyDownloader downloader = Mockito.mock(DependencyDownloader.class);
        final ClasspathInjector injector = Mockito.mock(ClasspathInjector.class);
        final YamlDependencyLoader yamlLoader = Mockito.mock(YamlDependencyLoader.class);

        setInternalField(manager, "downloader", downloader);
        setInternalField(manager, "injector", injector);
        setInternalField(manager, "yamlLoader", yamlLoader);

        manager.addDependencies(new String[]{"com.example:missing:1.0.0"});

        Mockito.when(downloader.download(Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    final DependencyCoordinate coord = invocation.getArgument(0);
                    return DownloadResult.failure(coord, "simulated failure");
                });

        assertDoesNotThrow(() -> manager.loadAll(getClass().getClassLoader()));

        assertTrue(capturedLogs.stream()
                .filter(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
                .anyMatch(record -> record.getMessage().contains("Failed to download dependency")));
    }

    private static void setInternalField(@NotNull final Object target, @NotNull final String fieldName, @NotNull final Object value)
            throws NoSuchFieldException, IllegalAccessException {
        final Field field = RemappingDependencyManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readRelocations(final RemappingDependencyManager manager) {
        try {
            final Field field = RemappingDependencyManager.class.getDeclaredField("relocations");
            field.setAccessible(true);
            return (Map<String, String>) field.get(manager);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to access relocations", exception);
        }
    }

    private static byte[] generateSimpleClass(final String internalName) {
        final ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        final MethodVisitor constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] randomBytes(final int size) {
        final byte[] bytes = new byte[size];
        new Random(0L).nextBytes(bytes);
        return bytes;
    }

    private static void createJar(final Path jarPath, final Map<String, byte[]> entries) throws IOException {
        final Path parent = jarPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                final JarEntry jarEntry = new JarEntry(entry.getKey());
                jarEntry.setLastModifiedTime(FileTime.from(Instant.now()));
                out.putNextEntry(jarEntry);
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
    }
}

