package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyServiceContractTest {

    private static final String BOUNTY_SERVICE_CLASS_NAME = "com.raindropcentral.rdq.service.bounty.BountyService";

    @Test
    void itRemainsAnInterfaceWithExpectedMethods() throws Exception {
        final Class<?> bountyService = Class.forName(BOUNTY_SERVICE_CLASS_NAME);
        final Map<String, Method> declaredMethods = declaredMethodsByName(bountyService);
        final Set<String> expectedMethodNames = Set.of(
                "getAllBounties",
                "getBountyByPlayer",
                "createBounty",
                "deleteBounty",
                "updateBounty",
                "getTotalBountyCount",
                "isPremium",
                "getMaxBountiesPerPlayer",
                "getMaxRewardItems",
                "canCreateBounty"
        );

        assertTrue(bountyService.isInterface(), "BountyService should remain an interface");
        assertEquals(expectedMethodNames, declaredMethods.keySet(), "BountyService should expose the expected method set");
    }

    @Test
    void itExposesAsynchronousContractsWithNotNullGuards() throws Exception {
        final Class<?> bountyService = Class.forName(BOUNTY_SERVICE_CLASS_NAME);
        final Map<String, Method> declaredMethods = declaredMethodsByName(bountyService);

        assertAsyncMethodSignature(declaredMethods, "getAllBounties", int.class, int.class);
        assertAsyncMethodSignature(declaredMethods, "getBountyByPlayer", UUID.class);
        assertAsyncMethodSignature(declaredMethods, "createBounty", RDQPlayer.class, Player.class, Set.class, Map.class);
        assertAsyncMethodSignature(declaredMethods, "deleteBounty", Long.class);
        assertAsyncMethodSignature(declaredMethods, "updateBounty", RBounty.class);
        assertAsyncMethodSignature(declaredMethods, "getTotalBountyCount");
    }

    @Test
    void itExposesSynchronousHelperContracts() throws Exception {
        final Class<?> bountyService = Class.forName(BOUNTY_SERVICE_CLASS_NAME);
        final Map<String, Method> declaredMethods = declaredMethodsByName(bountyService);

        assertPrimitiveSignature(declaredMethods, "isPremium", boolean.class);
        assertPrimitiveSignature(declaredMethods, "getMaxBountiesPerPlayer", int.class);
        assertPrimitiveSignature(declaredMethods, "getMaxRewardItems", int.class);
        assertPrimitiveSignature(declaredMethods, "canCreateBounty", boolean.class, Player.class);
    }

    private static Map<String, Method> declaredMethodsByName(final Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .collect(Collectors.toMap(Method::getName, Function.identity()));
    }

    private static void assertAsyncMethodSignature(final Map<String, Method> methods,
                                                    final String name,
                                                    final Class<?>... parameterTypes) {
        final Method method = requireMethod(methods, name);

        assertMethodSignature(method, CompletableFuture.class, parameterTypes);
        assertTrue(method.isAnnotationPresent(NotNull.class),
                () -> name + " should retain its @NotNull contract on the return type");
    }

    private static void assertPrimitiveSignature(final Map<String, Method> methods,
                                                 final String name,
                                                 final Class<?> expectedReturnType,
                                                 final Class<?>... parameterTypes) {
        final Method method = requireMethod(methods, name);

        assertMethodSignature(method, expectedReturnType, parameterTypes);
        assertTrue(method.getReturnType().isPrimitive(),
                () -> name + " should return a primitive to signal a synchronous contract");
    }

    private static void assertMethodSignature(final Method method,
                                              final Class<?> expectedReturnType,
                                              final Class<?>... parameterTypes) {
        assertEquals(expectedReturnType, method.getReturnType(),
                () -> method.getName() + " should return " + expectedReturnType.getSimpleName());
        assertArrayEquals(parameterTypes, method.getParameterTypes(),
                () -> method.getName() + " should accept parameters " + Arrays.toString(parameterTypes));
    }

    private static Method requireMethod(final Map<String, Method> methods, final String name) {
        final Method method = methods.get(name);
        assertNotNull(method, () -> name + " should be declared on BountyService");
        return method;
    }
}
