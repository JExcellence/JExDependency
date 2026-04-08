/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.protection.impl;

import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Shared reflection helpers used by protection bridges.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
abstract class AbstractReflectionProtectionBridge implements RProtectionBridge {

    @Nullable
    protected final Object invokeOptional(@NotNull Object target, @NotNull String methodName, final Object... arguments) {
        final InvocationResult invocationResult = invokeTracked(target, methodName, arguments);
        if (!invocationResult.invocationSucceeded()) {
            return null;
        }
        return invocationResult.value();
    }

    @Nullable
    protected final Object invokeStaticOptional(@NotNull Class<?> owner, @NotNull String methodName, final Object... arguments) {
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

    protected final boolean tryInvoke(@NotNull Object target, @NotNull String methodName, final Object... arguments) {
        final InvocationResult invocationResult = invokeTracked(target, methodName, arguments);
        if (!invocationResult.methodFound() || !invocationResult.invocationSucceeded()) {
            return false;
        }
        return isSuccessfulResult(invocationResult.value());
    }

    @Nullable
    protected final Object readFieldOptional(@NotNull Object target, @NotNull String fieldName) {
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

    protected final boolean hasMethod(@NotNull Class<?> owner, @NotNull String methodName, int parameterCount) {
        for (final Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return true;
            }
        }

        Class<?> current = owner;
        while (current != null) {
            for (final Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }

        return false;
    }

    @Nullable
    protected final UUID asUuid(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    protected final Integer asInteger(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    @Nullable
    protected final Double asDouble(@Nullable Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    @Nullable
    protected final Object firstNonNull(@Nullable Object... values) {
        for (final Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    protected final boolean sameTown(@Nullable Object firstTown, @Nullable Object secondTown) {
        if (firstTown == null || secondTown == null) {
            return false;
        }
        if (firstTown == secondTown || firstTown.equals(secondTown)) {
            return true;
        }

        final Object firstUuid = firstNonNull(
                invokeOptional(firstTown, "getUUID"),
                invokeOptional(firstTown, "getUuid"),
                invokeOptional(firstTown, "getTownUUID"),
                invokeOptional(firstTown, "getTownUuid"),
                invokeOptional(firstTown, "getIdentifier")
        );
        final Object secondUuid = firstNonNull(
                invokeOptional(secondTown, "getUUID"),
                invokeOptional(secondTown, "getUuid"),
                invokeOptional(secondTown, "getTownUUID"),
                invokeOptional(secondTown, "getTownUuid"),
                invokeOptional(secondTown, "getIdentifier")
        );

        if (firstUuid != null && secondUuid != null) {
            return firstUuid.equals(secondUuid);
        }

        final Object firstName = firstNonNull(
                invokeOptional(firstTown, "getName"),
                invokeOptional(firstTown, "getTownName")
        );
        final Object secondName = firstNonNull(
                invokeOptional(secondTown, "getName"),
                invokeOptional(secondTown, "getTownName")
        );

        if (firstName instanceof String first && secondName instanceof String second) {
            return first.equalsIgnoreCase(second);
        }

        return false;
    }

    @Nullable
    protected final String resolveTownIdentifier(@Nullable Object town) {
        if (town == null) {
            return null;
        }

        final UUID uuid = asUuid(firstNonNull(
            invokeOptional(town, "getUUID"),
            invokeOptional(town, "getUuid"),
            invokeOptional(town, "getTownUUID"),
            invokeOptional(town, "getTownUuid"),
            invokeOptional(town, "getIdentifier")
        ));
        if (uuid != null) {
            return uuid.toString().toLowerCase(Locale.ROOT);
        }

        final Object nameObject = firstNonNull(
            invokeOptional(town, "getName"),
            invokeOptional(town, "getTownName")
        );
        if (nameObject instanceof String townName && !townName.isBlank()) {
            return townName.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    @Nullable
    protected final String resolveTownDisplayName(@Nullable Object town) {
        if (town == null) {
            return null;
        }

        final Object nameObject = firstNonNull(
            invokeOptional(town, "getName"),
            invokeOptional(town, "getTownName")
        );
        if (nameObject instanceof String townName && !townName.isBlank()) {
            return townName.trim();
        }
        return resolveTownIdentifier(town);
    }

    protected final boolean isMayorIdentityMatch(
        final @NotNull Player player,
        final @Nullable Object mayorObject
    ) {
        if (mayorObject == null) {
            return false;
        }

        if (mayorObject instanceof Player mayorPlayer) {
            return mayorPlayer.getUniqueId().equals(player.getUniqueId());
        }
        if (mayorObject instanceof UUID mayorUuid) {
            return mayorUuid.equals(player.getUniqueId());
        }
        if (mayorObject instanceof String mayorText) {
            if (mayorText.equalsIgnoreCase(player.getName())) {
                return true;
            }
            final UUID mayorUuid = asUuid(mayorText);
            return mayorUuid != null && mayorUuid.equals(player.getUniqueId());
        }

        final Object nestedUuid = firstNonNull(
            invokeOptional(mayorObject, "getUniqueId"),
            invokeOptional(mayorObject, "getUUID"),
            invokeOptional(mayorObject, "getUuid"),
            invokeOptional(mayorObject, "getIdentifier")
        );
        final UUID mayorUuid = asUuid(nestedUuid);
        if (mayorUuid != null) {
            return mayorUuid.equals(player.getUniqueId());
        }

        final Object nestedName = firstNonNull(
            invokeOptional(mayorObject, "getName"),
            invokeOptional(mayorObject, "getPlayerName")
        );
        return nestedName instanceof String mayorName && mayorName.equalsIgnoreCase(player.getName());
    }

    protected final boolean isSuccessfulResult(@Nullable Object result) {
        if (result == null) {
            return true;
        }

        if (result instanceof Boolean booleanResult) {
            return booleanResult;
        }

        if (result instanceof CompletableFuture<?> future) {
            try {
                return isSuccessfulResult(future.join());
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        final Object successFlag = firstNonNull(
                invokeOptional(result, "isSuccess"),
                invokeOptional(result, "isSuccessful"),
                invokeOptional(result, "success"),
                invokeOptional(result, "getSuccess")
        );
        if (successFlag instanceof Boolean booleanFlag) {
            return booleanFlag;
        }

        return true;
    }

    @NotNull
    private InvocationResult invokeTracked(@NotNull Object target, @NotNull String methodName, final Object... arguments) {
        final Method method = findMethod(target.getClass(), methodName, arguments);
        if (method == null) {
            return InvocationResult.missingMethod();
        }

        try {
            method.setAccessible(true);
            return InvocationResult.invokedSuccessfully(unwrapOptional(method.invoke(target, arguments)));
        } catch (ReflectiveOperationException ignored) {
            return InvocationResult.invocationFailed();
        }
    }

    @Nullable
    private Method findMethod(@NotNull Class<?> owner, @NotNull String methodName, final Object... arguments) {
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

    private boolean parametersMatch(@NotNull Class<?>[] parameterTypes, @Nullable Object[] arguments) {
        final int argumentCount = arguments == null ? 0 : arguments.length;
        if (parameterTypes.length != argumentCount) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            final Class<?> parameterType = wrapPrimitive(parameterTypes[i]);
            final Object argument = arguments[i];

            if (argument == null) {
                if (parameterTypes[i].isPrimitive()) {
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

    @NotNull
    private Class<?> wrapPrimitive(@NotNull Class<?> type) {
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

    @Nullable
    private Field findField(@NotNull Class<?> owner, @NotNull String fieldName) {
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

    @Nullable
    private Object unwrapOptional(@Nullable Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private record InvocationResult(boolean methodFound, boolean invocationSucceeded, @Nullable Object value) {

        @NotNull
        private static InvocationResult missingMethod() {
            return new InvocationResult(false, false, null);
        }

        @NotNull
        private static InvocationResult invocationFailed() {
            return new InvocationResult(true, false, null);
        }

        @NotNull
        private static InvocationResult invokedSuccessfully(@Nullable Object value) {
            return new InvocationResult(true, true, value);
        }
    }
}
