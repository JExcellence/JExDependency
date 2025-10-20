package de.jexcellence.dependency.injector;

import de.jexcellence.dependency.exception.InjectionException;
import de.jexcellence.dependency.module.Deencapsulation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathInjectorTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetModuleDeencapsulationFlag() throws Exception {
        final Field moduleFlag = ClasspathInjector.class.getDeclaredField("moduleDeencapsulated");
        moduleFlag.setAccessible(true);
        moduleFlag.set(null, false);
    }

    @Test
    void injectExposesClassesAndTracksUrls() throws Exception {
        final ClasspathInjector injector = new ClasspathInjector();
        final File jarFile = createTemporaryJar("com.example.TempInjected");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            assertThrows(ClassNotFoundException.class, () -> classLoader.loadClass("com.example.TempInjected"));

            injector.inject(classLoader, jarFile);

            assertNotNull(classLoader.loadClass("com.example.TempInjected"));
            assertEquals(1, injector.getInjectedUrls().size());
            assertTrue(injector.getInjectedUrls().contains(jarFile.toURI().toURL()));

            injector.inject(classLoader, jarFile);
            assertEquals(1, injector.getInjectedUrls().size());
        }
    }

    @Test
    void injectRejectsInvalidJarFiles() throws Exception {
        final ClasspathInjector injector = new ClasspathInjector();
        final File missingFile = tempDir.resolve("missing.jar").toFile();

        try (URLClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            final InjectionException missingException = assertThrows(
                    InjectionException.class,
                    () -> injector.inject(classLoader, missingFile)
            );
            assertTrue(missingException.getMessage().contains("does not exist"));

            final File unreadableFile = Mockito.mock(File.class);
            Mockito.when(unreadableFile.exists()).thenReturn(true);
            Mockito.when(unreadableFile.isFile()).thenReturn(true);
            Mockito.when(unreadableFile.canRead()).thenReturn(false);
            Mockito.when(unreadableFile.getAbsolutePath()).thenReturn(tempDir.resolve("unreadable.jar").toString());

            final InjectionException unreadableException = assertThrows(
                    InjectionException.class,
                    () -> injector.inject(classLoader, unreadableFile)
            );
            assertTrue(unreadableException.getMessage().contains("not readable"));
        }
    }

    @Test
    void tryInjectLogsFailures() throws Exception {
        final ClasspathInjector injector = new ClasspathInjector();
        final Logger logger = extractLogger(injector);
        final TestLogHandler handler = new TestLogHandler();

        logger.setUseParentHandlers(false);
        logger.addHandler(handler);

        final File missingFile = tempDir.resolve("missing.jar").toFile();

        try (URLClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
            assertFalse(injector.tryInject(classLoader, missingFile));
        } finally {
            logger.removeHandler(handler);
        }

        assertTrue(handler.contains(Level.WARNING, "Failed to inject"));
    }

    @Test
    void moduleDeencapsulationOccursOnlyOnce() throws Exception {
        final File jarFile = createTemporaryJar("com.example.AnotherInjected");

        try (MockedStatic<Deencapsulation> mocked = Mockito.mockStatic(Deencapsulation.class, Mockito.CALLS_REAL_METHODS);
             URLClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {

            final ClasspathInjector injector = new ClasspathInjector();

            injector.inject(classLoader, jarFile);
            injector.inject(classLoader, jarFile);

            mocked.verify(() -> Deencapsulation.deencapsulate(Mockito.eq(ClasspathInjector.class)), Mockito.times(1));
        }
    }

    private File createTemporaryJar(final String className) throws IOException {
        final String packageName = className.substring(0, className.lastIndexOf('.'));
        final String simpleName = className.substring(className.lastIndexOf('.') + 1);

        final Path sourcesRoot = tempDir.resolve("sources");
        final Path classesRoot = tempDir.resolve("classes");
        Files.createDirectories(sourcesRoot);
        Files.createDirectories(classesRoot);

        final Path sourceFile = sourcesRoot
                .resolve(packageName.replace('.', '/'))
                .resolve(simpleName + ".java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(
                sourceFile,
                "package " + packageName + ";\n" +
                        "public class " + simpleName + " {\n" +
                        "    public String message() { return \"hello\"; }\n" +
                        "}\n"
        );

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classesRoot.toFile()));
            final Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            final JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("--release", "21"),
                    null,
                    compilationUnits
            );
            assertTrue(task.call(), "Compilation failed");
        }

        final Path classFile = classesRoot
                .resolve(packageName.replace('.', '/'))
                .resolve(simpleName + ".class");
        final Path jarPath = tempDir.resolve(simpleName + System.nanoTime() + ".jar");

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            final JarEntry entry = new JarEntry(packageName.replace('.', '/') + "/" + simpleName + ".class");
            jarOutputStream.putNextEntry(entry);
            Files.copy(classFile, jarOutputStream);
            jarOutputStream.closeEntry();
        }

        return jarPath.toFile();
    }

    private Logger extractLogger(final ClasspathInjector injector) throws Exception {
        final Field loggerField = ClasspathInjector.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        return (Logger) loggerField.get(injector);
    }

    private static class TestLogHandler extends Handler {

        private final List<LogRecord> records = new java.util.ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        boolean contains(final Level level, final String messagePart) {
            return records.stream()
                    .anyMatch(record -> record.getLevel().equals(level) && record.getMessage().contains(messagePart));
        }
    }
}
