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

/**
 * Utility responsible for attaching downloaded JAR files to a target class loader at runtime. The injector keeps track.
 * of previously injected URLs to prevent duplicates, de-encapsulates modules on first use and exposes guarded methods
 * for both exception-throwing and best-effort injection.
 */
public class ClasspathInjector {

    private static final String ADD_URL_METHOD_NAME = "addURL";
    private static final Set<URL> INJECTED_URLS_GLOBAL = Collections.synchronizedSet(new HashSet<>());
    private static boolean moduleDeencapsulated = false;

    private final Logger logger;
    private final Set<URL> injectedUrls;

    /**
     * Creates a new injector with its own logger and URL tracking set.
     */
    public ClasspathInjector() {
        this.logger = Logger.getLogger(getClass().getName());
        this.injectedUrls = new HashSet<>();
    }

    /**
     * Injects the provided JAR into the supplied class loader. Module boundaries are opened on the first invocation to.
     * ensure reflective access works on modern JVMs.
     *
     * @param classLoader class loader that should gain visibility of the JAR's classes
     * @param jarFile     resolved file to inject; must exist and be readable
     *
     * @throws InjectionException if validation or reflection calls fail
     */
    public void inject(@NotNull final ClassLoader classLoader, @NotNull final File jarFile) {
        validateJarFile(jarFile);
        ensureModuleDeencapsulation();

        try {
            final URL jarUrl = jarFile.toURI().toURL();

            if (isAlreadyInjected(jarUrl)) {
                logger.log(Level.FINE, () -> "Already injected: " + jarFile.getName());
                return;
            }

            performInjection(classLoader, jarUrl);
            injectedUrls.add(jarUrl);

            logger.log(Level.FINE, () -> "Successfully injected: " + jarFile.getName());

        } catch (final Exception exception) {
            throw new InjectionException(
                    "Failed to inject JAR into classpath: " + jarFile.getName(),
                    exception
            );
        }
    }

    /**
     * Attempts to inject the provided JAR into the supplied class loader while capturing any {@link InjectionException}.
     * as logged warnings.
     *
     * @param classLoader class loader that should gain visibility of the JAR's classes
     * @param jarFile     resolved file to inject; must exist and be readable
     *
     * @return {@code true} when the injection succeeds; {@code false} otherwise
     */
    public boolean tryInject(@NotNull final ClassLoader classLoader, @NotNull final File jarFile) {
        try {
            inject(classLoader, jarFile);
            return true;
        } catch (final InjectionException exception) {
            logger.log(Level.WARNING, "Failed to inject: " + jarFile.getName(), exception);
            return false;
        }
    }

    /**
     * Returns an immutable view of the URLs that have been successfully injected during the lifetime of this.
     * injector.
     *
     * @return immutable set of injected URLs
     */
    public @NotNull Set<URL> getInjectedUrls() {
        return Collections.unmodifiableSet(injectedUrls);
    }

    /**
     * Convenience helper used by callers to check whether a class is already visible to the application class path.
     *
     * @param className fully qualified class name to probe
     *
     * @return {@code true} when the class can be resolved, {@code false} otherwise
     */
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
    ) throws InjectionException {
        try {
            final Method addUrlMethod = URLClassLoader.class.getDeclaredMethod(ADD_URL_METHOD_NAME, URL.class);
            addUrlMethod.setAccessible(true);
            addUrlMethod.invoke(classLoader, jarUrl);
        } catch (final Exception exception) {
            throw new InjectionException("Failed to inject into URLClassLoader", exception);
        }
    }

    private void injectUsingReflection(
            @NotNull final ClassLoader classLoader,
            @NotNull final URL jarUrl
    ) throws InjectionException {
        try {
            final Method addUrlMethod = classLoader.getClass().getDeclaredMethod(ADD_URL_METHOD_NAME, URL.class);
            addUrlMethod.setAccessible(true);
            addUrlMethod.invoke(classLoader, jarUrl);
        } catch (final Exception exception) {
            throw new InjectionException("Failed to inject using reflection", exception);
        }
    }
}
