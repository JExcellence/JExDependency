package de.jexcellence.dependency.module;

import org.jetbrains.annotations.NotNull;
import sun.reflect.ReflectionFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Deencapsulation {

    private static final Logger LOGGER = Logger.getLogger(Deencapsulation.class.getName());
    private static final int JAVA_VERSION = determineJavaVersion();
    private static final Map<Module, Set<String>> openedPackages = new HashMap<>();

    private Deencapsulation() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void deencapsulate(@NotNull final Class<?> anchorClass) {
        if (JAVA_VERSION < 9) {
            LOGGER.fine("Java version < 9, module deencapsulation not required");
            return;
        }

        try {
            performDeencapsulation(anchorClass);
            LOGGER.fine("Module deencapsulation completed successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Module deencapsulation failed", exception);
        }
    }

    public static @NotNull MethodHandles.Lookup createPrivilegedLookup(@NotNull final Class<?> lookupClass) {
        try {
            final Constructor<?> lookupConstructor = ReflectionFactory.getReflectionFactory()
                    .newConstructorForSerialization(
                            MethodHandles.Lookup.class,
                            MethodHandles.Lookup.class.getDeclaredConstructor(Class.class)
                    );
            return (MethodHandles.Lookup) lookupConstructor.newInstance(lookupClass);
        } catch (final ReflectiveOperationException exception) {
            final Throwable cause = exception instanceof InvocationTargetException
                    ? ((InvocationTargetException) exception).getTargetException()
                    : exception;
            throw new IllegalStateException("Failed to create privileged lookup", cause);
        }
    }

    public static void closeOpenedPackages() {
        if (JAVA_VERSION < 9) {
            return;
        }

        try {
            final MethodHandle closePackageMethod = createPrivilegedLookup(Module.class)
                    .findVirtual(Module.class, "implRemoveOpens", MethodType.methodType(void.class, String.class));

            for (final Map.Entry<Module, Set<String>> moduleEntry : openedPackages.entrySet()) {
                final Module module = moduleEntry.getKey();
                for (final String packageName : moduleEntry.getValue()) {
                    try {
                        closePackageMethod.invokeExact(module, packageName);
                    } catch (final Throwable throwable) {
                        LOGGER.finest("Failed to close package: " + packageName);
                    }
                }
            }

            openedPackages.clear();
            LOGGER.fine("Closed all opened packages");

        } catch (final Throwable throwable) {
            LOGGER.log(Level.WARNING, "Failed to close opened packages", throwable);
        }
    }

    private static void performDeencapsulation(@NotNull final Class<?> anchorClass) {
        final Set<Module> relevantModules = collectRelevantModules(anchorClass);

        try {
            final MethodHandle openPackageMethod = createPrivilegedLookup(Module.class)
                    .findVirtual(Module.class, "implAddOpens", MethodType.methodType(void.class, String.class));

            for (final Module module : relevantModules) {
                openModulePackages(module, openPackageMethod);
            }

        } catch (final Throwable throwable) {
            throw new IllegalStateException("Failed to perform deencapsulation", throwable);
        }
    }

    private static @NotNull Set<Module> collectRelevantModules(@NotNull final Class<?> anchorClass) {
        final Set<Module> modules = new HashSet<>();

        final Module anchorModule = anchorClass.getModule();
        final ModuleLayer anchorModuleLayer = anchorModule.getLayer();

        if (anchorModuleLayer != null) {
            modules.addAll(anchorModuleLayer.modules());
        }

        modules.addAll(ModuleLayer.boot().modules());

        for (ClassLoader classLoader = anchorClass.getClassLoader();
             classLoader != null;
             classLoader = classLoader.getParent()) {
            modules.add(classLoader.getUnnamedModule());
        }

        return modules;
    }

    private static void openModulePackages(
            @NotNull final Module module,
            @NotNull final MethodHandle openPackageMethod
    ) throws Throwable {
        final Set<String> moduleOpenedPackages = openedPackages.computeIfAbsent(module, k -> new HashSet<>());

        for (final String packageName : module.getPackages()) {
            if (moduleOpenedPackages.add(packageName)) {
                try {
                    openPackageMethod.invokeExact(module, packageName);
                    LOGGER.finest("Opened package: " + packageName + " in module: " + module.getName());
                } catch (final Throwable throwable) {
                    LOGGER.finest("Could not open package: " + packageName);
                }
            }
        }
    }

    private static int determineJavaVersion() {
        final String version = System.getProperty("java.version");

        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }

        final int dotIndex = version.indexOf('.');
        if (dotIndex != -1) {
            return Integer.parseInt(version.substring(0, dotIndex));
        }

        return Integer.parseInt(version);
    }
}
