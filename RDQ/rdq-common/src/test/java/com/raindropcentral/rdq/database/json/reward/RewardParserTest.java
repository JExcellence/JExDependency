package com.raindropcentral.rdq.database.json.reward;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.raindropcentral.rdq.reward.AbstractReward;
import com.raindropcentral.rdq.reward.CommandReward;
import com.raindropcentral.rdq.reward.CurrencyReward;
import com.raindropcentral.rdq.reward.ExperienceReward;
import com.raindropcentral.rdq.reward.ItemReward;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RewardParserTest {

    @Test
    void serializeAndParseRoundTripsCurrencyReward() throws IOException {
        final CurrencyReward reward = new CurrencyReward(
                245.75,
                "GEMS",
                "JECurrency",
                1500L
        );

        final String json = RewardParser.serialize(reward);

        assertTrue(json.contains("\"type\":\"CURRENCY\""));
        assertTrue(json.contains("\"currencyIdentifier\":\"GEMS\""));

        final AbstractReward parsedReward = RewardParser.parse(json);
        final CurrencyReward parsed = assertInstanceOf(CurrencyReward.class, parsedReward);

        assertEquals(reward.getType(), parsed.getType());
        assertEquals(reward.getAmount(), parsed.getAmount());
        assertEquals(reward.getCurrencyIdentifier(), parsed.getCurrencyIdentifier());
        assertEquals(reward.getCurrencyPlugin(), parsed.getCurrencyPlugin());
        assertEquals(reward.getTimeoutMillis(), parsed.getTimeoutMillis());
    }

    @Test
    void serializeAndParseRoundTripsExperienceReward() throws IOException {
        final ExperienceReward reward = new ExperienceReward(
                12,
                ExperienceReward.ExperienceType.POINTS
        );

        final String json = RewardParser.serialize(reward);

        assertTrue(json.contains("\"type\":\"EXPERIENCE_LEVEL\""));
        assertTrue(json.contains("\"experienceType\":\"POINTS\""));

        final AbstractReward parsedReward = RewardParser.parse(json);
        final ExperienceReward parsed = assertInstanceOf(ExperienceReward.class, parsedReward);

        assertEquals(reward.getType(), parsed.getType());
        assertEquals(reward.getAmount(), parsed.getAmount());
        assertEquals(reward.getExperienceType(), parsed.getExperienceType());
    }

    @Test
    void serializeAndParseRoundTripsCommandReward() throws IOException {
        final CommandReward reward = new CommandReward(
                "/msg %player% congratulations",
                true,
                40L
        );

        final String json = RewardParser.serialize(reward);

        assertTrue(json.contains("\"type\":\"COMMAND\""));
        assertTrue(json.contains("\"executeAsPlayer\":true"));

        final AbstractReward parsedReward = RewardParser.parse(json);
        final CommandReward parsed = assertInstanceOf(CommandReward.class, parsedReward);

        assertEquals(reward.getType(), parsed.getType());
        assertEquals(reward.getCommand(), parsed.getCommand());
        assertEquals(reward.isExecuteAsPlayer(), parsed.isExecuteAsPlayer());
        assertEquals(reward.getDelayTicks(), parsed.getDelayTicks());
    }

    @Test
    void serializeItemRewardWithItemStackPayload() throws IOException {
        MockBukkit.mock();

        try {
            final ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, 1);
            final ItemMeta meta = stack.getItemMeta();
            assertNotNull(meta);
            meta.setDisplayName("Reward Blade");
            meta.setLore(List.of("Legendary quest reward"));
            stack.setItemMeta(meta);

            final ItemReward reward = new ItemReward(stack);

            final String json = RewardParser.serialize(reward);

            assertTrue(json.contains("\"type\":\"ITEM\""));
            assertTrue(json.contains("\"itemStack\""));

            final AbstractReward parsedReward = RewardParser.parse(json);
            final ItemReward parsed = assertInstanceOf(ItemReward.class, parsedReward);

            assertEquals(reward.getType(), parsed.getType());

            final ItemStack parsedStack = parsed.getItemStack();
            assertNotNull(parsedStack);
            assertEquals(stack.getType(), parsedStack.getType());
            assertEquals(stack.getAmount(), parsedStack.getAmount());

            final ItemMeta parsedMeta = parsedStack.getItemMeta();
            assertNotNull(parsedMeta);
            assertEquals(meta.getDisplayName(), parsedMeta.getDisplayName());
            assertEquals(meta.getLore(), parsedMeta.getLore());
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void parseInvalidJsonPropagatesIOException() {
        assertThrows(IOException.class, () -> RewardParser.parse("{\"type\""));
    }
}
