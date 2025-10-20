package de.jexcellence.dependency.loader;

import de.jexcellence.dependency.downloader.DependencyDownloader;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.remapper.RemappingDependencyManager;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperPluginLoaderTest {

    private static final String REMAP_PROPERTY = "jedependency.remap";
    private static final String PAPER_LOADER_PROPERTY = "paper.plugin.loader.active";

    @TempDir
    Path tempDirectory;

    private PaperPluginLoader loader;
    private DependencyDownloader downloaderSpy;
    private PluginClasspathBuilder classpathBuilder;
    private PluginProviderContext providerContext;
    private Path dataDirectory;

    @BeforeEach
    void setUp() throws Exception {
        RemappingDependencyManager.reset();
        System.clearProperty(REMAP_PROPERTY);
        System.clearProperty(PAPER_LOADER_PROPERTY);

        loader = new PaperPluginLoader();
        downloaderSpy = org.mockito.Mockito.spy(new DependencyDownloader());
        replaceDownloader(downloaderSpy);

        classpathBuilder = mock(PluginClasspathBuilder.class);
        providerContext = mock(PluginProviderContext.class);

        when(classpathBuilder.getContext()).thenReturn(providerContext);
        when(classpathBuilder.addLibrary(any(JarLibrary.class))).thenReturn(classpathBuilder);

        dataDirectory = tempDirectory.resolve("data");
        Files.createDirectories(dataDirectory);
        when(providerContext.getDataDirectory()).thenReturn(dataDirectory);

        final Path pluginSource = tempDirectory.resolve("plugin.jar");
        Files.writeString(pluginSource, "stub");
        when(providerContext.getPluginSource()).thenReturn(pluginSource);

        stubDownloader();
    }

    @AfterEach
    void tearDown() {
        RemappingDependencyManager.reset();
        System.clearProperty(REMAP_PROPERTY);
        System.clearProperty(PAPER_LOADER_PROPERTY);
    }

    @Test
    void classloaderAddsDownloadedJarsToClasspathWhenRemappingDisabled() {
        System.setProperty(REMAP_PROPERTY, "false");

        loader.classloader(classpathBuilder);

        final ArgumentCaptor<JarLibrary> captor = ArgumentCaptor.forClass(JarLibrary.class);
        verify(classpathBuilder, times(2)).addLibrary(captor.capture());

        final List<Path> jarPaths = captor.getAllValues().stream()
                .map(this::resolveLibraryPath)
                .toList();

        assertEquals(2, jarPaths.size(), "Expected two libraries to be injected");
        jarPaths.forEach(path -> {
            assertEquals(dataDirectory.resolve("libraries"), path.getParent());
            assertTrue(Files.exists(path));
        });
    }

    @Test
    void classloaderUsesRemappedLibrariesWhenRemappingEnabled() {
        System.setProperty(REMAP_PROPERTY, "true");

        loader.classloader(classpathBuilder);

        final ArgumentCaptor<JarLibrary> captor = ArgumentCaptor.forClass(JarLibrary.class);
        verify(classpathBuilder, times(2)).addLibrary(captor.capture());

        final Path remappedDirectory = dataDirectory.resolve("libraries").resolve("remapped");
        final List<Path> jarPaths = captor.getAllValues().stream()
                .map(this::resolveLibraryPath)
                .toList();

        assertEquals(2, jarPaths.size(), "Expected two remapped libraries to be injected");
        jarPaths.forEach(path -> assertEquals(remappedDirectory, path.getParent()));
    }

    @Test
    void classloaderFallsBackWhenRemappingFails() {
        System.setProperty(REMAP_PROPERTY, "true");
        RemappingDependencyManager.setFailRemapping(true);

        loader.classloader(classpathBuilder);

        final ArgumentCaptor<JarLibrary> captor = ArgumentCaptor.forClass(JarLibrary.class);
        verify(classpathBuilder, times(2)).addLibrary(captor.capture());

        final List<Path> jarPaths = captor.getAllValues().stream()
                .map(this::resolveLibraryPath)
                .toList();

        final Path librariesDirectory = dataDirectory.resolve("libraries");
        jarPaths.forEach(path -> assertEquals(librariesDirectory, path.getParent()));
    }

    private void replaceDownloader(final DependencyDownloader replacement) throws Exception {
        final Field field = PaperPluginLoader.class.getDeclaredField("dependencyDownloader");
        field.setAccessible(true);
        field.set(loader, replacement);
    }

    private void stubDownloader() {
        doAnswer(invocation -> {
            final DependencyCoordinate coordinate = invocation.getArgument(0);
            final File targetDirectory = invocation.getArgument(1);

            final Path jarPath = targetDirectory.toPath().resolve(coordinate.toFileName());
            createJar(jarPath);
            return DownloadResult.success(coordinate, jarPath.toFile());
        }).when(downloaderSpy).download(any(DependencyCoordinate.class), any(File.class));
    }

    private void createJar(final Path jarPath) throws IOException {
        Files.createDirectories(jarPath.getParent());
        try (var outputStream = new java.util.jar.JarOutputStream(Files.newOutputStream(jarPath))) {
            final var entry = new java.util.jar.JarEntry("test/Sample.class");
            outputStream.putNextEntry(entry);
            final byte[] data = new byte[4096];
            new java.util.Random(jarPath.hashCode()).nextBytes(data);
            outputStream.write(data);
            outputStream.closeEntry();
        }
    }

    private Path resolveLibraryPath(final JarLibrary library) {
        try {
            final Field pathField = JarLibrary.class.getDeclaredField("path");
            pathField.setAccessible(true);
            return ((Path) pathField.get(library)).toAbsolutePath();
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read JarLibrary path", exception);
        }
    }
}
