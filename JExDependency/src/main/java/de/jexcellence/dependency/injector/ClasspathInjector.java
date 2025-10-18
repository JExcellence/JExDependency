package de.jexcellence.dependency.injector;

import de.jexcellence.dependency.exception.InjectionException;
import de.jexcellence.dependency.module.Deencapsulation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClasspathInjector {

    private static final String ADD_URL_METHOD_NAME = "addURL";
    private static boolean moduleDeencapsulated = false;

    private final Logger logger;
    private final Set<URL> injectedUrls;

    public ClasspathInjector() {
        this.logger = Logger.getLogger(getClass().getName());
        this.injectedUrls = new HashSet<>();
    }

    public void inject(@NotNull final ClassLoader classLoader, @NotNull final File jarFile) {
        validateJarFile(jarFile);
        ensureModuleDeencapsulation();

        try {
            final URL jarUrl = jarFile.toURI().toURL();

            if (isAlreadyInjected(jarUrl)) {
                logger.fine("Already injected: " + jarFile.getName());
                return;
            }

            performInjection(classLoader, jarUrl);
            injectedUrls.add(jarUrl);

            logger.fine("Successfully injected: " + jarFile.getName());

        } catch (final Exception exception) {
            throw new InjectionException(
                    "Failed to inject JAR into classpath: " + jarFile.getName(),
                    exception
            );
        }
    }

    public boolean tryInject(@NotNull final ClassLoader classLoader, @NotNull final File jarFile) {
        try {
            inject(classLoader, jarFile);
            return true;
        } catch (final InjectionException exception) {
            logger.log(Level.WARNING, "Failed to inject: " + jarFile.getName(), exception);
            return false;
        }
    }

    public @NotNull Set<URL> getInjectedUrls() {
        return Collections.unmodifiableSet(injectedUrls);
    }

    public boolean isClassAvailable(@NotNull final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException exception) {
            return false;
        }
    }

    private void validateJarFile(@NotNull final File jarFile) {
        if (!jarFile.exists()) {
            throw new InjectionException("JAR file does not exist: " + jarFile.getAbsolutePath());
        }

        if (!jarFile.isFile()) {
            throw new InjectionException("Path is not a file: " + jarFile.getAbsolutePath());
        }

        if (!jarFile.canRead()) {
            throw new InjectionException("JAR file is not readable: " + jarFile.getAbsolutePath());
        }
    }

    private void ensureModuleDeencapsulation() {
        if (!moduleDeencapsulated) {
            try {
                Deencapsulation.deencapsulate(getClass());
                moduleDeencapsulated = true;
                logger.fine("Module deencapsulation completed");
            } catch (final Exception exception) {
                logger.log(Level.WARNING, "Module deencapsulation failed", exception);
            }
        }
    }

    private boolean isAlreadyInjected(@NotNull final URL jarUrl) {
        return injectedUrls.contains(jarUrl);
    }

    private void performInjection(
            @NotNull final ClassLoader classLoader,
            @NotNull final URL jarUrl
    ) throws Exception {
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            injectIntoUrlClassLoader(urlClassLoader, jarUrl);
        } else {
            injectUsingReflection(classLoader, jarUrl);
        }
    }

    private void injectIntoUrlClassLoader(
            @NotNull final URLClassLoader classLoader,
            @NotNull final URL jarUrl
    ) throws Exception {
        final Method addUrlMethod = URLClassLoader.class.getDeclaredMethod(ADD_URL_METHOD_NAME, URL.class);
        addUrlMethod.setAccessible(true);
        addUrlMethod.invoke(classLoader, jarUrl);
    }

    private void injectUsingReflection(
            @NotNull final ClassLoader classLoader,
            @NotNull final URL jarUrl
    ) throws Exception {
        final Method addUrlMethod = classLoader.getClass().getDeclaredMethod(ADD_URL_METHOD_NAME, URL.class);
        addUrlMethod.setAccessible(true);
        addUrlMethod.invoke(classLoader, jarUrl);
    }
}
