package de.jexcellence.dependency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import de.jexcellence.dependency.injector.ClasspathInjector;
import de.jexcellence.dependency.manager.DependencyManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.ForwardingJavaFileManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class JEDependencyTest {

    private static final String REMAPPER_CLASS_NAME = "de.jexcellence.dependency.remapper.RemappingDependencyManager";
    private static final String REMAP_CALLED_PROPERTY = "jedependency.test.remap.called";
    private static final String REMAP_FAIL_PROPERTY = "jedependency.test.remap.fail";
    private static final String DEP_MANAGER_CALLED_PROPERTY = "jedependency.test.dependency";

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        plugin.getDataFolder().mkdirs();
        clearRemappingProperties();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        clearRemappingProperties();
    }

    private static void clearRemappingProperties() {
        System.clearProperty(REMAP_CALLED_PROPERTY);
        System.clearProperty(REMAP_FAIL_PROPERTY);
        System.clearProperty(DEP_MANAGER_CALLED_PROPERTY);
    }

    @Test
    void initializeDelegatesToDependencyManagerWhenRemappingDisabled() {
        final String[] additional = new String[]{"group:artifact:1.0.0"};

        try (MockedConstruction<DependencyManager> construction = Mockito.mockConstruction(DependencyManager.class, (mock, context) -> {
            Mockito.doNothing().when(mock).initialize(Mockito.any());
        })) {
            JEDependency.initialize(plugin, JEDependencyTest.class, additional);

            final List<DependencyManager> constructed = construction.constructed();
            assertEquals(1, constructed.size());
            final DependencyManager manager = constructed.get(0);
            final ArgumentCaptor<String[]> depsCaptor = ArgumentCaptor.forClass(String[].class);
            verify(manager).initialize(depsCaptor.capture());
            assertArrayEquals(additional, depsCaptor.getValue());
        }
    }

    @Test
    void initializeAsyncDelegatesToDependencyManager() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.addDefinition("de.jexcellence.dependency.manager.DependencyManager", compileDependencyManagerStub());

        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());
        final Method method = isolated.getMethod("initializeAsync", JavaPlugin.class, Class.class, String[].class);
        final CompletableFuture<?> future = (CompletableFuture<?>) method.invoke(null, plugin, JEDependencyTest.class, (Object) new String[]{"async:dep"});
        future.join();

        assertEquals("async:dep", System.getProperty(DEP_MANAGER_CALLED_PROPERTY));
    }

    @Test
    void initializeWithRemappingUsesRemappingManagerWhenAvailable() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.addDefinition(REMAPPER_CLASS_NAME, compileRemapperStub());

        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());

        try (MockedConstruction<DependencyManager> construction = Mockito.mockConstruction(DependencyManager.class)) {
            invokeInitializeWithRemapping(isolated, plugin, JEDependencyTest.class, new String[]{"extra:dep:2.0"});

            assertEquals("extra:dep:2.0", System.getProperty(REMAP_CALLED_PROPERTY));
            assertTrue(construction.constructed().isEmpty());
        }
    }

    @Test
    void initializeWithRemappingFallsBackWhenRemapperThrows() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.addDefinition(REMAPPER_CLASS_NAME, compileRemapperStub());

        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());

        try (MockedConstruction<DependencyManager> construction = Mockito.mockConstruction(DependencyManager.class)) {
            try {
                System.setProperty(REMAP_FAIL_PROPERTY, "true");
                invokeInitializeWithRemapping(isolated, plugin, JEDependencyTest.class, null);
            } finally {
                System.clearProperty(REMAP_FAIL_PROPERTY);
            }

            assertEquals("null", System.getProperty(REMAP_CALLED_PROPERTY));
            final List<DependencyManager> constructed = construction.constructed();
            assertEquals(1, constructed.size());
            verify(constructed.get(0)).initialize(null);
        }
    }

    @Test
    void initializeFallsBackWhenRemappingRequestedButManagerMissing() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.hideClass(REMAPPER_CLASS_NAME);

        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());

        try (MockedConstruction<DependencyManager> construction = Mockito.mockConstruction(DependencyManager.class)) {
            try {
                System.setProperty("jedependency.remap", "yes");
                invokeInitialize(isolated, plugin, JEDependencyTest.class, null);
            } finally {
                System.clearProperty("jedependency.remap");
            }

            final List<DependencyManager> constructed = construction.constructed();
            assertEquals(1, constructed.size());
            verify(constructed.get(0)).initialize(null);
        }
    }

    @Test
    void initializeInjectsLibrariesWhenPaperLoaderActive() throws IOException {
        final File dataFolder = plugin.getDataFolder();
        final File remappedDir = new File(dataFolder, "libraries/remapped");
        remappedDir.mkdirs();
        final File jarFile = new File(remappedDir, "example.jar");
        Files.writeString(jarFile.toPath(), "stub");

        try (MockedConstruction<ClasspathInjector> injectorConstruction = Mockito.mockConstruction(ClasspathInjector.class, (mock, context) -> {
                 Mockito.when(mock.tryInject(Mockito.any(), Mockito.any())).thenReturn(true);
             });
             MockedConstruction<DependencyManager> managerConstruction = Mockito.mockConstruction(DependencyManager.class)) {

            try {
                System.setProperty("paper.plugin.loader.active", "true");
                JEDependency.initialize(plugin, JEDependencyTest.class);
            } finally {
                System.clearProperty("paper.plugin.loader.active");
            }

            final List<ClasspathInjector> injectors = injectorConstruction.constructed();
            assertEquals(1, injectors.size());
            final ClasspathInjector injector = injectors.get(0);

            final ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
            verify(injector).tryInject(Mockito.any(), fileCaptor.capture());
            assertEquals(jarFile, fileCaptor.getValue());

            final List<DependencyManager> constructed = managerConstruction.constructed();
            assertEquals(1, constructed.size());
            verify(constructed.get(0)).initialize(null);
        }
    }

    @Test
    void getServerTypeReturnsPaperLoaderWhenPropertyActive() {
        try {
            System.setProperty("paper.plugin.loader.active", "true");
            assertEquals("Paper (with plugin loader)", JEDependency.getServerType());
        } finally {
            System.clearProperty("paper.plugin.loader.active");
        }
    }

    @Test
    void getServerTypeReturnsPaperLegacyWhenPaperServerDetected() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.addDefinition("com.destroystokyo.paper.PaperConfig", compileSimpleClass("com.destroystokyo.paper.PaperConfig", ""));

        final String serverType = invokeGetServerType(loader);
        assertEquals("Paper (legacy mode)", serverType);
    }

    @Test
    void getServerTypeReturnsSpigotWhenPaperClassesMissing() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));

        final String serverType = invokeGetServerType(loader);
        assertEquals("Spigot/CraftBukkit", serverType);
    }

    @Test
    void isPaperServerFallsBackToModernConfigurationClass() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.hideClass("com.destroystokyo.paper.PaperConfig");
        loader.addDefinition("io.papermc.paper.configuration.Configuration", compileSimpleClass("io.papermc.paper.configuration.Configuration", ""));

        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());
        final Method method = isolated.getMethod("isPaperServer");
        assertTrue((boolean) method.invoke(null));
    }

    @Test
    void isPaperServerReturnsFalseWhenNoPaperClassesFound() throws Exception {
        final IsolatedClassLoader loader = new IsolatedClassLoader();
        loader.addDefinition(JEDependency.class.getName(), readClassBytes(JEDependency.class));
        loader.hideClass("com.destroystokyo.paper.PaperConfig");
        loader.hideClass("io.papermc.paper.configuration.Configuration");

        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());
        final Method method = isolated.getMethod("isPaperServer");
        assertFalse((boolean) method.invoke(null));
    }

    private static void invokeInitializeWithRemapping(
            final Class<?> jedependencyClass,
            final JavaPlugin plugin,
            final Class<?> anchorClass,
            final String[] additional
    ) throws Exception {
        final Method method = jedependencyClass.getMethod("initializeWithRemapping", JavaPlugin.class, Class.class, String[].class);
        method.invoke(null, plugin, anchorClass, (Object) additional);
    }

    private static void invokeInitialize(
            final Class<?> jedependencyClass,
            final JavaPlugin plugin,
            final Class<?> anchorClass,
            final String[] additional
    ) throws Exception {
        final Method method = jedependencyClass.getMethod("initialize", JavaPlugin.class, Class.class, String[].class);
        method.invoke(null, plugin, anchorClass, (Object) additional);
    }

    private static String invokeGetServerType(final IsolatedClassLoader loader) throws Exception {
        final Class<?> isolated = loader.loadClass(JEDependency.class.getName());
        final Method method = isolated.getMethod("getServerType");
        return (String) method.invoke(null);
    }

    private static byte[] readClassBytes(final Class<?> clazz) {
        final String resourceName = clazz.getName().replace('.', '/') + ".class";
        try (InputStream stream = clazz.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new IllegalStateException("Unable to locate class bytes for " + clazz.getName());
            }
            return stream.readAllBytes();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static byte[] compileRemapperStub() {
        return compileJava(
                REMAPPER_CLASS_NAME,
                "package de.jexcellence.dependency.remapper;" +
                        "import org.bukkit.plugin.java.JavaPlugin;" +
                        "public class RemappingDependencyManager {" +
                        "  public RemappingDependencyManager(JavaPlugin plugin, Class<?> anchor) {}" +
                        "  public RemappingDependencyManager() {}" +
                        "  public void initialize(String[] deps) {" +
                        "    String value = (deps == null) ? \"null\" : String.join(\"|\", deps);" +
                        "    System.setProperty(\"" + REMAP_CALLED_PROPERTY + "\", value);" +
                        "    if (Boolean.getBoolean(\"" + REMAP_FAIL_PROPERTY + "\")) {" +
                        "      throw new RuntimeException(\"fail\");" +
                        "    }" +
                        "  }" +
                        "}"
        );
    }

    private static byte[] compileDependencyManagerStub() {
        return compileJava(
                "de.jexcellence.dependency.manager.DependencyManager",
                "package de.jexcellence.dependency.manager;" +
                        "import org.bukkit.plugin.java.JavaPlugin;" +
                        "public class DependencyManager {" +
                        "  public DependencyManager(JavaPlugin plugin, Class<?> anchor) {}" +
                        "  public void initialize(String[] deps) {" +
                        "    String value = (deps == null) ? \"null\" : String.join(\"|\", deps);" +
                        "    System.setProperty(\"" + DEP_MANAGER_CALLED_PROPERTY + "\", value);" +
                        "  }" +
                        "}"
        );
    }

    private static byte[] compileSimpleClass(final String className, final String body) {
        final String packageName;
        final String simpleName;
        final int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            packageName = className.substring(0, lastDot);
            simpleName = className.substring(lastDot + 1);
        } else {
            packageName = "";
            simpleName = className;
        }

        final StringBuilder source = new StringBuilder();
        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";");
        }
        source.append("public class ").append(simpleName).append(" {").append(body).append("}");

        return compileJava(className, source.toString());
    }

    private static byte[] compileJava(final String className, final String source) {
        return CompileCache.INSTANCE.compile(className, source);
    }

    private static final class CompileCache {
        private static final CompileCache INSTANCE = new CompileCache();

        private final Map<String, byte[]> cache = new HashMap<>();

        private byte[] compile(final String className, final String source) {
            return cache.computeIfAbsent(className, name -> doCompile(name, source));
        }

        private byte[] doCompile(final String className, final String source) {
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("Java compiler not available");
            }

            final InMemorySource sourceObject = new InMemorySource(className, source);

            try (StandardJavaFileManager standard = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
                 ByteArrayJavaFileManager fileManager = new ByteArrayJavaFileManager(standard)) {

                final List<String> options = List.of("-classpath", System.getProperty("java.class.path"));
                final Boolean success = compiler.getTask(null, fileManager, null, options, null, List.of(sourceObject)).call();
                if (!Boolean.TRUE.equals(success)) {
                    throw new IllegalStateException("Compilation failed for " + className);
                }
                return fileManager.getBytes();
            } catch (final IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }

    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String code;

        private InMemorySource(final String className, final String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static final class InMemoryClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private InMemoryClassFile(final String className, final Kind kind) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }

        private byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }

    private static final class ByteArrayJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> implements AutoCloseable {
        private InMemoryClassFile classFile;

        private ByteArrayJavaFileManager(final JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                final JavaFileManager.Location location,
                final String className,
                final JavaFileObject.Kind kind,
                final FileObject sibling
        ) {
            classFile = new InMemoryClassFile(className, kind);
            return classFile;
        }

        private byte[] getBytes() {
            return classFile != null ? classFile.getBytes() : new byte[0];
        }
    }

    private static final class IsolatedClassLoader extends ClassLoader {
        private final Map<String, byte[]> definitions = new HashMap<>();
        private final Set<String> hidden = new HashSet<>();

        private IsolatedClassLoader() {
            super(JEDependency.class.getClassLoader());
        }

        private void addDefinition(final String className, final byte[] bytes) {
            definitions.put(className, bytes);
        }

        private void hideClass(final String className) {
            hidden.add(className);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                final Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }

                if (definitions.containsKey(name)) {
                    final byte[] bytes = definitions.remove(name);
                    final Class<?> defined = defineClass(name, bytes, 0, bytes.length);
                    if (resolve) {
                        resolveClass(defined);
                    }
                    return defined;
                }

                if (hidden.contains(name)) {
                    throw new ClassNotFoundException(name + " is hidden");
                }

                return super.loadClass(name, resolve);
            }
        }
    }
}
