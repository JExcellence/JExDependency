package com.raindropcentral.rplatform.job.impl;

import com.raindropcentral.rplatform.job.JobBridge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Shared reflection helpers for {@link JobBridge} implementations.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
abstract class AbstractReflectionJobBridge implements JobBridge {

    protected final @Nullable Object invokeOptional(
        final @Nullable Object target,
        final @NotNull String methodName,
        final Object... arguments
    ) {
        if (target == null) {
            return null;
        }

        try {
            final Method method = findMethod(target.getClass(), methodName, arguments);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return unwrapOptional(method.invoke(target, arguments));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    protected final @Nullable Object invokeStaticOptional(
        final @Nullable Class<?> owner,
        final @NotNull String methodName,
        final Object... arguments
    ) {
        if (owner == null) {
            return null;
        }

        try {
            final Method method = findMethod(owner, methodName, arguments);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return unwrapOptional(method.invoke(null, arguments));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    protected final @Nullable Object readFieldOptional(
        final @Nullable Object target,
        final @NotNull String fieldName
    ) {
        if (target == null) {
            return null;
        }

        try {
            final Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    protected final @Nullable Class<?> loadClass(
        final @NotNull Plugin plugin,
        final @NotNull String className
    ) {
        try {
            return Class.forName(className, true, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException ignored) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException fallbackIgnored) {
                return null;
            }
        }
    }

    protected final @Nullable Plugin resolvePlugin(
        final @NotNull String primaryName,
        final @NotNull String... aliases
    ) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(primaryName);
        if (plugin != null) {
            return plugin;
        }

        for (final String alias : aliases) {
            plugin = Bukkit.getPluginManager().getPlugin(alias);
            if (plugin != null) {
                return plugin;
            }
        }
        return null;
    }

    protected final @Nullable Object firstNonNull(final @Nullable Object... values) {
        for (final Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected final @Nullable Double asDouble(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    protected final @NotNull String normalizeLookupKey(final @NotNull String input) {
        return input.trim()
            .toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "");
    }

    protected final @Nullable Object resolveNamedEntry(
        final @Nullable Iterable<?> candidates,
        final @NotNull String requestedName
    ) {
        if (candidates == null) {
            return null;
        }

        final String target = normalizeLookupKey(requestedName);
        for (final Object candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            final Object candidateName = firstNonNull(
                invokeOptional(candidate, "getId"),
                invokeOptional(candidate, "getID"),
                invokeOptional(candidate, "getKey"),
                invokeOptional(candidate, "getIdentifier"),
                invokeOptional(candidate, "getName"),
                invokeOptional(candidate, "name"),
                invokeOptional(candidate, "getJobName")
            );

            if (candidateName != null && target.equals(normalizeLookupKey(candidateName.toString()))) {
                return candidate;
            }
        }
        return null;
    }

    protected final @Nullable Double resolveFromNamedMap(
        final @Nullable Object rawMap,
        final @NotNull String requestedName
    ) {
        if (!(rawMap instanceof Map<?, ?> map)) {
            return null;
        }

        final String target = normalizeLookupKey(requestedName);
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (!target.equals(normalizeLookupKey(entry.getKey().toString()))) {
                continue;
            }
            return asDouble(entry.getValue());
        }
        return null;
    }

    private @Nullable Method findMethod(
        final @NotNull Class<?> owner,
        final @NotNull String methodName,
        final Object... arguments
    ) {
        for (final Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && parametersMatch(method.getParameterTypes(), arguments)) {
                return method;
            }
        }

        Class<?> current = owner;
        while (current != null) {
            for (final Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && parametersMatch(method.getParameterTypes(), arguments)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }

        return null;
    }

    private boolean parametersMatch(final @NotNull Class<?>[] parameterTypes, final @Nullable Object[] arguments) {
        final int argumentCount = arguments == null ? 0 : arguments.length;
        if (parameterTypes.length != argumentCount) {
            return false;
        }

        for (int index = 0; index < parameterTypes.length; index++) {
            final Class<?> parameterType = wrapPrimitive(parameterTypes[index]);
            final Object argument = arguments[index];

            if (argument == null) {
                if (parameterTypes[index].isPrimitive()) {
                    return false;
                }
                continue;
            }

            if (!parameterType.isInstance(argument)) {
                return false;
            }
        }

        return true;
    }

    private @NotNull Class<?> wrapPrimitive(final @NotNull Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "char" -> Character.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            default -> type;
        };
    }

    private @Nullable Field findField(final @NotNull Class<?> owner, final @NotNull String fieldName) {
        Class<?> current = owner;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private @Nullable Object unwrapOptional(final @Nullable Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }
}
