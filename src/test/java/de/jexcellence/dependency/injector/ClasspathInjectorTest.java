package de.jexcellence.dependency.injector;

import de.jexcellence.dependency.exception.InjectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ClasspathInjector")
class ClasspathInjectorTest {

    private ClasspathInjector injector;

    @BeforeEach
    void setUp() {
        injector = new ClasspathInjector();
    }

    @Nested
    @DisplayName("inject() — file validation")
    class FileValidation {

        @Test
        @DisplayName("non-existent file throws InjectionException")
        void nonExistentFileThrows(@TempDir final Path tmpDir) {
            final File missing = tmpDir.resolve("does-not-exist.jar").toFile();

            assertThrows(InjectionException.class,
                () -> injector.inject(ClassLoader.getSystemClassLoader(), missing));
        }

        @Test
        @DisplayName("directory path throws InjectionException")
        void directoryThrows(@TempDir final Path tmpDir) {
            assertThrows(InjectionException.class,
                () -> injector.inject(ClassLoader.getSystemClassLoader(), tmpDir.toFile()));
        }

        @Test
        @DisplayName("tryInject() returns false for non-existent file")
        void tryInjectReturnsFalseForMissing(@TempDir final Path tmpDir) {
            final File missing = tmpDir.resolve("missing.jar").toFile();

            assertFalse(injector.tryInject(ClassLoader.getSystemClassLoader(), missing));
        }

        @Test
        @DisplayName("tryInject() returns false for a directory")
        void tryInjectReturnsFalseForDirectory(@TempDir final Path tmpDir) {
            assertFalse(injector.tryInject(ClassLoader.getSystemClassLoader(), tmpDir.toFile()));
        }
    }

    @Nested
    @DisplayName("initial state")
    class InitialState {

        @Test
        @DisplayName("getInjectedUrls() is empty on a fresh instance")
        void initiallyEmpty() {
            assertTrue(injector.getInjectedUrls().isEmpty());
        }

        @Test
        @DisplayName("getInjectedUrls() returns an unmodifiable view")
        void urlsUnmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                () -> injector.getInjectedUrls().clear());
        }
    }

    @Nested
    @DisplayName("isClassAvailable()")
    class IsClassAvailable {

        @Test
        @DisplayName("returns true for a class already on the JVM boot classpath")
        void knownJdkClass() {
            assertTrue(injector.isClassAvailable("java.lang.String"));
        }

        @Test
        @DisplayName("returns true for a class in this module's own classpath")
        void ownModuleClass() {
            assertTrue(injector.isClassAvailable("de.jexcellence.dependency.model.DependencyCoordinate"));
        }

        @Test
        @DisplayName("returns false for a class that does not exist")
        void unknownClass() {
            assertFalse(injector.isClassAvailable("com.nonexistent.phantom.Clazz"));
        }
    }

    @Nested
    @DisplayName("inject() with a real JAR")
    class InjectRealJar {

        @Test
        @DisplayName("successfully injecting a JAR adds its URL to getInjectedUrls()")
        void injectAddsUrl(@TempDir final Path tmpDir) throws Exception {
            final File jar = buildMinimalJar(tmpDir.resolve("test.jar").toFile());

            try (var classLoader = new URLClassLoader(new java.net.URL[0])) {
                injector.inject(classLoader, jar);

                assertEquals(1, injector.getInjectedUrls().size());
            }
        }

        @Test
        @DisplayName("injecting the same JAR twice does not duplicate the URL")
        void duplicateInjectionIdempotent(@TempDir final Path tmpDir) throws Exception {
            final File jar = buildMinimalJar(tmpDir.resolve("test.jar").toFile());

            try (var classLoader = new URLClassLoader(new java.net.URL[0])) {
                injector.inject(classLoader, jar);
                injector.inject(classLoader, jar);

                assertEquals(1, injector.getInjectedUrls().size());
            }
        }

        @Test
        @DisplayName("two distinct JARs each contribute one URL")
        void twoDistinctJars(@TempDir final Path tmpDir) throws Exception {
            final File jarA = buildMinimalJar(tmpDir.resolve("a.jar").toFile());
            final File jarB = buildMinimalJar(tmpDir.resolve("b.jar").toFile());

            try (var classLoader = new URLClassLoader(new java.net.URL[0])) {
                injector.inject(classLoader, jarA);
                injector.inject(classLoader, jarB);

                assertEquals(2, injector.getInjectedUrls().size());
            }
        }
    }

    private static File buildMinimalJar(final File target) throws IOException {
        final var manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        try (var out = new JarOutputStream(new FileOutputStream(target), manifest)) {
            // No class entries needed; a valid manifest is sufficient.
        }
        return target;
    }
}
