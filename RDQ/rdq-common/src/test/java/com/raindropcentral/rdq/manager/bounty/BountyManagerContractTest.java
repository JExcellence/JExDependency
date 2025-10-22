package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyManagerContractTest {

    @Test
    void bountyManagerInterfaceContractIsStable() throws NoSuchMethodException {
        Class<BountyManager> type = BountyManager.class;

        assertAll(
                () -> assertTrue(type.isInterface(), "BountyManager should remain an interface"),
                () -> assertTrue(Modifier.isPublic(type.getModifiers()), "BountyManager should remain public")
        );

        Method createBounty = type.getMethod(
                "createBounty",
                RDQPlayer.class,
                Player.class,
                Set.class,
                Map.class
        );
        assertAll(
                () -> assertEquals(void.class, createBounty.getReturnType(), "createBounty return type changed"),
                () -> assertParameterAnnotatedWith(createBounty.getParameters()[0], NotNull.class, "targetPlayer"),
                () -> assertParameterAnnotatedWith(createBounty.getParameters()[1], NotNull.class, "commissioner"),
                () -> assertParameterAnnotatedWith(createBounty.getParameters()[2], NotNull.class, "rewardItems"),
                () -> assertParameterizedType(createBounty.getGenericParameterTypes()[2], Set.class, RewardItem.class, "rewardItems"),
                () -> assertParameterAnnotatedWith(createBounty.getParameters()[3], NotNull.class, "rewardCurrencies"),
                () -> assertParameterizedType(createBounty.getGenericParameterTypes()[3], Map.class, String.class, Double.class, "rewardCurrencies")
        );

        Method removeBounty = type.getMethod("removeBounty", UUID.class);
        assertAll(
                () -> assertEquals(void.class, removeBounty.getReturnType(), "removeBounty return type changed"),
                () -> assertParameterAnnotatedWith(removeBounty.getParameters()[0], NotNull.class, "targetUniqueId")
        );

        Method trackDamage = type.getMethod("trackDamage", UUID.class, UUID.class, double.class);
        assertAll(
                () -> assertEquals(void.class, trackDamage.getReturnType(), "trackDamage return type changed"),
                () -> assertParameterAnnotatedWith(trackDamage.getParameters()[0], NotNull.class, "targetUniqueId"),
                () -> assertParameterAnnotatedWith(trackDamage.getParameters()[1], NotNull.class, "attackerUniqueId"),
                () -> assertParameterHasNoNullableAnnotations(trackDamage.getParameters()[2], "damage")
        );

        Method handleBountyKill = type.getMethod("handleBountyKill", Player.class);
        assertAll(
                () -> assertEquals(void.class, handleBountyKill.getReturnType(), "handleBountyKill return type changed"),
                () -> assertParameterAnnotatedWith(handleBountyKill.getParameters()[0], NotNull.class, "killedPlayer")
        );

        Method addItemRewards = type.getMethod("addItemRewards", RBounty.class, List.class);
        assertAll(
                () -> assertEquals(RBounty.class, addItemRewards.getReturnType(), "addItemRewards return type changed"),
                () -> assertAnnotatedWith(addItemRewards, NotNull.class, "addItemRewards should stay @NotNull"),
                () -> assertParameterAnnotatedWith(addItemRewards.getParameters()[0], NotNull.class, "bounty"),
                () -> assertParameterAnnotatedWith(addItemRewards.getParameters()[1], NotNull.class, "items"),
                () -> assertParameterizedType(addItemRewards.getGenericParameterTypes()[1], List.class, ItemStack.class, "items")
        );

        Method addCurrencyReward = type.getMethod("addCurrencyReward", RBounty.class, String.class, double.class);
        assertAll(
                () -> assertEquals(RBounty.class, addCurrencyReward.getReturnType(), "addCurrencyReward return type changed"),
                () -> assertAnnotatedWith(addCurrencyReward, NotNull.class, "addCurrencyReward should stay @NotNull"),
                () -> assertParameterAnnotatedWith(addCurrencyReward.getParameters()[0], NotNull.class, "bounty"),
                () -> assertParameterAnnotatedWith(addCurrencyReward.getParameters()[1], NotNull.class, "currencyName"),
                () -> assertParameterHasNoNullableAnnotations(addCurrencyReward.getParameters()[2], "amount")
        );

        Method updateBountyPlayerDisplay = type.getMethod("updateBountyPlayerDisplay", UUID.class);
        assertAll(
                () -> assertEquals(void.class, updateBountyPlayerDisplay.getReturnType(), "updateBountyPlayerDisplay return type changed"),
                () -> assertParameterAnnotatedWith(updateBountyPlayerDisplay.getParameters()[0], NotNull.class, "playerUniqueId")
        );

        Method hasActiveBounty = type.getMethod("hasActiveBounty", UUID.class);
        assertAll(
                () -> assertEquals(boolean.class, hasActiveBounty.getReturnType(), "hasActiveBounty return type changed"),
                () -> assertParameterAnnotatedWith(hasActiveBounty.getParameters()[0], NotNull.class, "playerUniqueId")
        );

        Method getBounty = type.getMethod("getBounty", UUID.class);
        assertAll(
                () -> assertEquals(RBounty.class, getBounty.getReturnType(), "getBounty return type changed"),
                () -> assertAnnotatedWith(getBounty, Nullable.class, "getBounty should stay @Nullable"),
                () -> assertParameterAnnotatedWith(getBounty.getParameters()[0], NotNull.class, "playerUniqueId")
        );

        Method giveRewardItemsToPlayer = type.getMethod("giveRewardItemsToPlayer", Player.class, Set.class);
        assertAll(
                () -> assertEquals(void.class, giveRewardItemsToPlayer.getReturnType(), "giveRewardItemsToPlayer return type changed"),
                () -> assertParameterAnnotatedWith(giveRewardItemsToPlayer.getParameters()[0], NotNull.class, "player"),
                () -> assertParameterAnnotatedWith(giveRewardItemsToPlayer.getParameters()[1], NotNull.class, "rewardItems"),
                () -> assertParameterizedType(giveRewardItemsToPlayer.getGenericParameterTypes()[1], Set.class, RewardItem.class, "rewardItems")
        );
    }

    private void assertAnnotatedWith(AnnotatedElement element, Class<? extends Annotation> annotation, String message) {
        assertTrue(element.isAnnotationPresent(annotation), message);
    }

    private void assertParameterAnnotatedWith(Parameter parameter, Class<? extends Annotation> annotation, String parameterName) {
        assertTrue(parameter.isAnnotationPresent(annotation), () -> parameterName + " should be annotated with @" + annotation.getSimpleName());
        assertFalse(parameter.isAnnotationPresent(NotNull.class) && annotation == Nullable.class, () -> parameterName + " should not be annotated with @NotNull");
        assertFalse(parameter.isAnnotationPresent(Nullable.class) && annotation == NotNull.class, () -> parameterName + " should not be annotated with @Nullable");
    }

    private void assertParameterHasNoNullableAnnotations(Parameter parameter, String parameterName) {
        assertFalse(parameter.isAnnotationPresent(NotNull.class), () -> parameterName + " should not be annotated with @NotNull");
        assertFalse(parameter.isAnnotationPresent(Nullable.class), () -> parameterName + " should not be annotated with @Nullable");
    }

    private void assertParameterizedType(Type type, Class<?> rawType, Class<?> argumentType, String parameterName) {
        assertParameterizedType(type, rawType, new Class<?>[]{argumentType}, parameterName);
    }

    private void assertParameterizedType(Type type, Class<?> rawType, Class<?> firstArgument, Class<?> secondArgument, String parameterName) {
        assertParameterizedType(type, rawType, new Class<?>[]{firstArgument, secondArgument}, parameterName);
    }

    private void assertParameterizedType(Type type, Class<?> rawType, Class<?>[] argumentTypes, String parameterName) {
        assertTrue(type instanceof ParameterizedType, () -> parameterName + " should remain parameterized as " + rawType.getSimpleName());
        ParameterizedType parameterizedType = (ParameterizedType) type;
        assertEquals(rawType, parameterizedType.getRawType(), () -> parameterName + " should retain raw type " + rawType.getSimpleName());
        Type[] actualArguments = parameterizedType.getActualTypeArguments();
        assertEquals(argumentTypes.length, actualArguments.length, () -> parameterName + " should contain " + argumentTypes.length + " type arguments");
        for (int i = 0; i < argumentTypes.length; i++) {
            assertEquals(argumentTypes[i], actualArguments[i], () -> parameterName + " type argument " + i + " should remain " + argumentTypes[i].getSimpleName());
        }
    }
}
