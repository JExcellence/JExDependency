package com.raindropcentral.rdq.database.entity.reward;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RewardItemTest {

    @Test
    void itInitializesAmountAndLeavesMetadataUnsetWhenConstructedWithItemOnly() throws Exception {
        final ItemStack stack = new ItemStack(Material.DIAMOND, 4);

        final RewardItem rewardItem = new RewardItem(stack);

        assertSame(stack, rewardItem.getItem(), "getItem should return the provided stack instance");
        assertEquals(4, rewardItem.getAmount(), "Constructor should copy the stack amount");
        assertNull(rewardItem.getContributorUniqueId(),
                "Single-argument constructor should leave contributor metadata unset");

        assertNull(readField(rewardItem, "uniqueId", UUID.class),
                "Single-argument constructor should not assign a UUID");
        assertNull(readField(rewardItem, "contributedAt", LocalDateTime.class),
                "Single-argument constructor should not record a timestamp");
    }

    @Test
    void itCapturesPlayerContributionDetailsWhenConstructedWithContributor() throws Exception {
        final ItemStack stack = new ItemStack(Material.EMERALD, 2);
        final Player contributor = mock(Player.class);
        final UUID contributorId = UUID.randomUUID();
        when(contributor.getUniqueId()).thenReturn(contributorId);

        final LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        final RewardItem rewardItem = new RewardItem(stack, contributor);
        final LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertSame(stack, rewardItem.getItem(), "Constructor should retain the contributed stack");
        assertEquals(2, rewardItem.getAmount(), "Constructor should copy the stack amount");
        assertEquals(contributorId, rewardItem.getContributorUniqueId(),
                "Constructor should capture the contributor identifier");

        final UUID generatedId = readField(rewardItem, "uniqueId", UUID.class);
        assertNotNull(generatedId, "Two-argument constructor should generate a unique identifier");

        final LocalDateTime timestamp = readField(rewardItem, "contributedAt", LocalDateTime.class);
        assertNotNull(timestamp, "Two-argument constructor should record a contribution timestamp");
        assertFalse(timestamp.isBefore(before), "Timestamp should not be earlier than the pre-construction instant");
        assertFalse(timestamp.isAfter(after), "Timestamp should be recorded near the construction time");
    }

    @Test
    void itAllowsPersistenceLayersToRehydrateProtectedFields() throws Exception {
        final Constructor<RewardItem> constructor = RewardItem.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final RewardItem rewardItem = constructor.newInstance();

        final ItemStack storedStack = new ItemStack(Material.GOLD_INGOT, 1);
        final Method setItem = RewardItem.class.getDeclaredMethod("setItem", ItemStack.class);
        setItem.setAccessible(true);
        setItem.invoke(rewardItem, storedStack);

        rewardItem.setAmount(9);

        final UUID contributorId = UUID.randomUUID();
        final Method setContributorUniqueId = RewardItem.class.getDeclaredMethod("setContributorUniqueId", UUID.class);
        setContributorUniqueId.setAccessible(true);
        setContributorUniqueId.invoke(rewardItem, contributorId);

        assertSame(storedStack, rewardItem.getItem(),
                "setItem should allow persistence frameworks to replace the stored stack");
        assertEquals(9, rewardItem.getAmount(),
                "setAmount should update the tracked contribution quantity");
        assertEquals(contributorId, rewardItem.getContributorUniqueId(),
                "setContributorUniqueId should update the contributor metadata");
    }

    private static <T> T readField(final RewardItem rewardItem, final String fieldName, final Class<T> type)
            throws Exception {
        final Field field = RewardItem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(rewardItem));
    }
}

