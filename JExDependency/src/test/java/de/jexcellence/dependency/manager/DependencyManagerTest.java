package de.jexcellence.dependency.manager;

import org.mockbukkit.mockbukkit.MockBukkit;
import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.loader.YamlDependencyLoader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.model.ProcessingResult;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DependencyManagerTest {

    @TempDir
    Path tempDir;

    private JavaPlugin plugin;
    private Logger testLogger;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        setPluginDataFolder(plugin, tempDir);

        testLogger = Logger.getLogger(DependencyManager.class.getName());
        testLogger.setUseParentHandlers(false);
        testLogger.setLevel(Level.ALL);
        logHandler = new TestLogHandler();
        testLogger.addHandler(logHandler);
    }

    @AfterEach
    void tearDown() {
        testLogger.removeHandler(logHandler);
        logHandler.clear();
        MockBukkit.unmock();
    }

    @Test
    void initializeLogsProcessingSummary() {
        try (MockedConstruction<DependencyDownloader> downloaderConstruction =
                     Mockito.mockConstruction(DependencyDownloader.class);
             MockedConstruction<ClasspathInjector> injectorConstruction =
                     Mockito.mockConstruction(ClasspathInjector.class);
             MockedConstruction<YamlDependencyLoader> yamlLoaderConstruction =
                     Mockito.mockConstruction(YamlDependencyLoader.class)) {

            final DependencyManager manager = new DependencyManager(plugin, DependencyManagerTest.class);

            final DependencyDownloader downloader = downloaderConstruction.constructed().get(0);
            final ClasspathInjector injector = injectorConstruction.constructed().get(0);
            final YamlDependencyLoader yamlLoader = yamlLoaderConstruction.constructed().get(0);

            Mockito.when(yamlLoader.loadDependencies(DependencyManagerTest.class))
                    .thenReturn(List.of(
                            "com.example:success:1.0",
                            "com.example:download-fail:1.0"
                    ));

            Mockito.when(downloader.download(any(DependencyCoordinate.class), any(File.class)))
                    .thenAnswer(invocation -> {
                        final DependencyCoordinate coordinate = invocation.getArgument(0);
                        final File librariesDir = invocation.getArgument(1);

                        if ("success".equals(coordinate.artifactId())) {
                            final File jarFile = createJarFile(librariesDir, coordinate);
                            return DownloadResult.success(coordinate, jarFile);
                        }

                        return DownloadResult.failure(coordinate, "boom");
                    });

            Mockito.when(injector.tryInject(any(), any(File.class)))
                    .thenAnswer(invocation -> {
                        final File file = invocation.getArgument(1);
                        return file.getName().contains("success");
                    });

            manager.initialize(null);

            assertTrue(logHandler.contains("Initializing dependency management for"));
            assertTrue(logHandler.contains("Processing 2 dependencies"));
            assertTrue(logHandler.contains("Dependency processing summary:"));
            assertTrue(logHandler.contains("Total: 2 | Success: 1 | Failed: 1"));
            assertTrue(logHandler.contains("Libraries directory contains 1 JAR files"));

            verify(downloader, Mockito.times(2)).download(any(DependencyCoordinate.class), any(File.class));
            verify(injector).tryInject(any(), any(File.class));
        }
    }

    @Test
    void initializeHandlesEmptyDependencyLists() {
        try (MockedConstruction<DependencyDownloader> downloaderConstruction =
                     Mockito.mockConstruction(DependencyDownloader.class);
             MockedConstruction<ClasspathInjector> injectorConstruction =
                     Mockito.mockConstruction(ClasspathInjector.class);
             MockedConstruction<YamlDependencyLoader> yamlLoaderConstruction =
                     Mockito.mockConstruction(YamlDependencyLoader.class)) {

            final DependencyManager manager = new DependencyManager(plugin, DependencyManagerTest.class);

            final DependencyDownloader downloader = downloaderConstruction.constructed().get(0);
            final ClasspathInjector injector = injectorConstruction.constructed().get(0);
            final YamlDependencyLoader yamlLoader = yamlLoaderConstruction.constructed().get(0);

            Mockito.when(yamlLoader.loadDependencies(DependencyManagerTest.class))
                    .thenReturn(List.of());

            manager.initialize(null);

            assertTrue(logHandler.contains("No dependencies to process"));
            verifyNoInteractions(downloader);
            verifyNoInteractions(injector);
        }
    }

    @Test
    void initializeStopsWhenPluginJarCannotBeResolved() {
        try (MockedConstruction<DependencyDownloader> downloaderConstruction =
                     Mockito.mockConstruction(DependencyDownloader.class);
             MockedConstruction<ClasspathInjector> injectorConstruction =
                     Mockito.mockConstruction(ClasspathInjector.class);
             MockedConstruction<YamlDependencyLoader> yamlLoaderConstruction =
                     Mockito.mockConstruction(YamlDependencyLoader.class)) {

            final DependencyManager manager = new DependencyManager(plugin, Object.class);

            final DependencyDownloader downloader = downloaderConstruction.constructed().get(0);
            final ClasspathInjector injector = injectorConstruction.constructed().get(0);
            final YamlDependencyLoader yamlLoader = yamlLoaderConstruction.constructed().get(0);

            Mockito.when(yamlLoader.loadDependencies(Object.class))
                    .thenReturn(List.of("com.example:unused:1.0"));

            manager.initialize(null);

            assertTrue(logHandler.contains("Failed to determine plugin JAR location"));
            verifyNoInteractions(downloader);
            verifyNoInteractions(injector);
            verifyNoInteractions(yamlLoader);
        }
    }

    @Test
    void initializeAsyncReflectsProcessingOutcomeAndShutdownDelegates() {
        try (MockedConstruction<DependencyDownloader> downloaderConstruction =
                     Mockito.mockConstruction(DependencyDownloader.class);
             MockedConstruction<ClasspathInjector> injectorConstruction =
                     Mockito.mockConstruction(ClasspathInjector.class);
             MockedConstruction<YamlDependencyLoader> yamlLoaderConstruction =
                     Mockito.mockConstruction(YamlDependencyLoader.class)) {

            final DependencyManager manager = new DependencyManager(plugin, DependencyManagerTest.class);

            final DependencyDownloader downloader = downloaderConstruction.constructed().get(0);
            final ClasspathInjector injector = injectorConstruction.constructed().get(0);
            final YamlDependencyLoader yamlLoader = yamlLoaderConstruction.constructed().get(0);

            Mockito.when(yamlLoader.loadDependencies(DependencyManagerTest.class))
                    .thenReturn(List.of(
                            "com.example:success:1.0",
                            "com.example:inject-fail:1.0",
                            "com.example:download-fail:1.0"
                    ));

            Mockito.when(downloader.downloadAsync(any(DependencyCoordinate.class), any(File.class)))
                    .thenAnswer(invocation -> {
                        final DependencyCoordinate coordinate = invocation.getArgument(0);
                        final File librariesDir = invocation.getArgument(1);

                        if (coordinate.artifactId().contains("download-fail")) {
                            return CompletableFuture.completedFuture(DownloadResult.failure(coordinate, "boom"));
                        }

                        final File jarFile = createJarFile(librariesDir, coordinate);
                        return CompletableFuture.completedFuture(DownloadResult.success(coordinate, jarFile));
                    });

            Mockito.when(injector.tryInject(any(), any(File.class)))
                    .thenAnswer(invocation -> {
                        final File file = invocation.getArgument(1);
                        return !file.getName().contains("inject-fail");
                    });

            final ProcessingResult result = manager.initializeAsync(null).join();

            assertEquals(1, result.getSuccessCount());
            assertEquals(2, result.getFailureCount());
            assertTrue(result.hasFailures());
            assertFalse(result.isFullySuccessful());
            assertTrue(result.getSuccessful().stream()
                    .map(DependencyCoordinate::artifactId)
                    .anyMatch("success"::equals));
            assertTrue(result.getFailed().stream()
                    .map(downloadResult -> downloadResult.coordinate().artifactId())
                    .toList()
                    .containsAll(List.of("inject-fail", "download-fail")));

            manager.shutdown();
            verify(downloader).shutdown();
        }
    }

    private static void setPluginDataFolder(final JavaPlugin plugin, final Path folder) {
        try {
            final VarHandle handle = MethodHandles.privateLookupIn(JavaPlugin.class, MethodHandles.lookup())
                    .findVarHandle(JavaPlugin.class, "dataFolder", File.class);
            handle.set(plugin, folder.toFile());
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set plugin data folder", exception);
        }
    }

    private static File createJarFile(final File librariesDir, final DependencyCoordinate coordinate) {
        try {
            final File jarFile = new File(librariesDir, coordinate.toFileName());
            Files.createDirectories(jarFile.getParentFile().toPath());
            if (!jarFile.exists()) {
                Files.createFile(jarFile.toPath());
            }
            return jarFile;
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create jar file", exception);
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        private boolean contains(final String fragment) {
            return messages.stream().anyMatch(message -> message.contains(fragment));
        }

        private void clear() {
            messages.clear();
        }
    }
}
