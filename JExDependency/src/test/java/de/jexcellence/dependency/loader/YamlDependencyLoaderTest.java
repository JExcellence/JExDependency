package de.jexcellence.dependency.loader;

import de.jexcellence.dependency.loader.fixtures.ResourceAnchor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

class YamlDependencyLoaderTest {

    private static final List<String> EXPECTED_PAPER_DEPENDENCIES = List.of(
            "com.example:paper-only:2.0.0",
            "com.example:paper-overrides-generic:3.0.0"
    );

    private static final List<String> EXPECTED_SPIGOT_DEPENDENCIES = List.of(
            "com.example:spigot-default:4.0.0",
            "com.example:spigot-quoted:4.1.0"
    );

    private static final List<String> EXPECTED_GENERIC_DEPENDENCIES = List.of(
            "com.example:generic-one:1.0.0",
            "com.example:quoted-generic:1.0.1"
    );

    private final List<URLClassLoader> managedClassLoaders = new ArrayList<>();
    private final List<Path> managedJars = new ArrayList<>();

    @AfterEach
    void tearDown() throws IOException {
        for (final URLClassLoader classLoader : managedClassLoaders) {
            classLoader.close();
        }

        for (final Path jar : managedJars) {
            Files.deleteIfExists(jar);
        }

        managedClassLoaders.clear();
        managedJars.clear();
    }

    @Test
    void loadDependenciesPrefersPaperListWhenPaperServerDetected() throws Exception {
        try (MockedStatic<YamlDependencyLoader> loaderMock = Mockito.mockStatic(YamlDependencyLoader.class)) {
            mockPaperServerDetection(loaderMock);

            final YamlDependencyLoader loader = new YamlDependencyLoader();
            final Class<?> anchorClass = loadAnchorWithTestResources();

            final List<String> dependencies = loader.loadDependencies(anchorClass);

            Assertions.assertEquals(EXPECTED_PAPER_DEPENDENCIES, dependencies);
        }
    }

    @Test
    void loadDependenciesFallsBackToSpigotWhenPaperUnavailable() throws Exception {
        try (MockedStatic<YamlDependencyLoader> loaderMock = Mockito.mockStatic(YamlDependencyLoader.class)) {
            mockSpigotServerDetection(loaderMock);

            final YamlDependencyLoader loader = new YamlDependencyLoader();
            final Class<?> anchorClass = loadAnchorWithTestResources();

            final List<String> dependencies = loader.loadDependencies(anchorClass);

            Assertions.assertEquals(EXPECTED_SPIGOT_DEPENDENCIES, dependencies);
        }
    }

    @Test
    void loadDependenciesReturnsNullWhenResourcesMissing() throws Exception {
        try (MockedStatic<YamlDependencyLoader> loaderMock = Mockito.mockStatic(YamlDependencyLoader.class)) {
            mockSpigotServerDetection(loaderMock);

            final YamlDependencyLoader loader = new YamlDependencyLoader();

            Assertions.assertNull(loader.loadDependencies(Object.class));
        }
    }

    @Test
    void loadDependenciesFromJarPrefersPaperConfiguration() throws Exception {
        final Path jar = createJar(Map.of(
                "dependency/dependencies.yml", readResource("/dependency/dependencies.yml"),
                "dependency/paper/dependencies.yml", readResource("/dependency/paper/dependencies.yml"),
                "dependency/spigot/dependencies.yml", readResource("/dependency/spigot/dependencies.yml")
        ));

        try (MockedStatic<YamlDependencyLoader> loaderMock = Mockito.mockStatic(YamlDependencyLoader.class)) {
            mockPaperServerDetection(loaderMock);

            final YamlDependencyLoader loader = new YamlDependencyLoader();
            final List<String> dependencies = loader.loadDependenciesFromJar(jar);

            Assertions.assertEquals(EXPECTED_PAPER_DEPENDENCIES, dependencies);
        }
    }

    @Test
    void loadDependenciesFromJarFallsBackToGenericWhenServerSpecificMissing() throws Exception {
        final Path jar = createJar(Map.of(
                "dependency/dependencies.yml", readResource("/dependency/dependencies.yml")
        ));

        try (MockedStatic<YamlDependencyLoader> loaderMock = Mockito.mockStatic(YamlDependencyLoader.class)) {
            mockSpigotServerDetection(loaderMock);

            final YamlDependencyLoader loader = new YamlDependencyLoader();
            final List<String> dependencies = loader.loadDependenciesFromJar(jar);

            Assertions.assertEquals(EXPECTED_GENERIC_DEPENDENCIES, dependencies);
        }
    }

    @Test
    void loadDependenciesFromJarReturnsNullWhenDescriptorMissing() throws Exception {
        final Path jar = createJar(Map.of());

        try (MockedStatic<YamlDependencyLoader> loaderMock = Mockito.mockStatic(YamlDependencyLoader.class)) {
            mockSpigotServerDetection(loaderMock);

            final YamlDependencyLoader loader = new YamlDependencyLoader();
            final List<String> dependencies = loader.loadDependenciesFromJar(jar);

            Assertions.assertNull(dependencies);
        }
    }

    private Class<?> loadAnchorWithTestResources() throws Exception {
        final URL classesUrl = ResourceAnchor.class.getProtectionDomain().getCodeSource().getLocation();
        final URL dependencyResourceUrl = Objects.requireNonNull(
                ResourceAnchor.class.getResource("/dependency/dependencies.yml"),
                "Test resources must include dependency YAML"
        );
        final Path resourcesRoot = Path.of(dependencyResourceUrl.toURI()).getParent().getParent();
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{classesUrl, resourcesRoot.toUri().toURL()}, null);
        managedClassLoaders.add(classLoader);

        final Class<?> anchorClass = Class.forName(ResourceAnchor.class.getName(), true, classLoader);
        Assertions.assertNotNull(
                anchorClass.getResourceAsStream("/dependency/dependencies.yml"),
                "Isolated class loader should expose test dependency resources"
        );
        return anchorClass;
    }

    private Path createJar(final Map<String, String> entries) throws IOException {
        final Path jar = Files.createTempFile("yaml-dependencies", ".jar");
        managedJars.add(jar);

        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jar))) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                final JarEntry jarEntry = new JarEntry(entry.getKey());
                outputStream.putNextEntry(jarEntry);
                outputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                outputStream.closeEntry();
            }
        }

        return jar;
    }

    private String readResource(final String path) throws IOException {
        try (InputStream inputStream = ResourceAnchor.class.getResourceAsStream(path)) {
            Assertions.assertNotNull(inputStream, "Resource " + path + " should exist for test setup");
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void mockPaperServerDetection(final MockedStatic<YamlDependencyLoader> loaderMock) {
        loaderMock.when(() -> YamlDependencyLoader.classForName("com.destroystokyo.paper.PaperConfig"))
                .thenReturn(DummyPaperConfig.class);
        loaderMock.when(() -> YamlDependencyLoader.classForName("io.papermc.paper.configuration.Configuration"))
                .thenThrow(ClassNotFoundException.class);
    }

    private void mockSpigotServerDetection(final MockedStatic<YamlDependencyLoader> loaderMock) {
        loaderMock.when(() -> YamlDependencyLoader.classForName("com.destroystokyo.paper.PaperConfig"))
                .thenThrow(ClassNotFoundException.class);
        loaderMock.when(() -> YamlDependencyLoader.classForName("io.papermc.paper.configuration.Configuration"))
                .thenThrow(ClassNotFoundException.class);
    }

    private static final class DummyPaperConfig {
        private DummyPaperConfig() {
        }
    }
}
