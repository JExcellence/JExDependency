package com.raindropcentral.rdq.database.json.reward;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.raindropcentral.rdq.reward.AbstractReward;
import com.raindropcentral.rdq.reward.CommandReward;
import com.raindropcentral.rdq.reward.CompositeReward;
import com.raindropcentral.rdq.reward.CurrencyReward;
import com.raindropcentral.rdq.reward.ExperienceReward;
import com.raindropcentral.rdq.reward.ItemReward;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for {@link RewardMixin} to ensure that supported {@link AbstractReward} implementations
 * can be (de)serialized via Jackson using the configured type discriminator.
 */
final class RewardMixinTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .addMixIn(AbstractReward.class, RewardMixin.class);
    }

    @Test
    void deserializesItemReward() throws Exception {
        final String json = """
                {
                  \"type\": \"ITEM\",
                  \"material\": \"DIAMOND\",
                  \"amount\": 3,
                  \"dropIfFull\": true
                }
                """;

        final AbstractReward reward = this.objectMapper.readValue(json, AbstractReward.class);

        final ItemReward itemReward = assertInstanceOf(ItemReward.class, reward);
        assertEquals(AbstractReward.Type.ITEM, itemReward.getType());
        assertEquals(Material.DIAMOND, itemReward.getMaterial());
        assertEquals(3, itemReward.getAmount());

        assertTypePreserved(itemReward, "ITEM");
    }

    @Test
    void deserializesCurrencyReward() throws Exception {
        final String json = """
                {
                  \"type\": \"CURRENCY\",
                  \"amount\": 150.5,
                  \"currencyIdentifier\": \"GEMS\",
                  \"currencyPlugin\": \"JECurrency\",
                  \"timeoutMillis\": 750
                }
                """;

        final AbstractReward reward = this.objectMapper.readValue(json, AbstractReward.class);

        final CurrencyReward currencyReward = assertInstanceOf(CurrencyReward.class, reward);
        assertEquals(AbstractReward.Type.CURRENCY, currencyReward.getType());
        assertEquals(150.5, currencyReward.getAmount());
        assertEquals("GEMS", currencyReward.getCurrencyIdentifier());
        assertEquals("JECurrency", currencyReward.getCurrencyPlugin());
        assertEquals(750, currencyReward.getTimeoutMillis());

        assertTypePreserved(currencyReward, "CURRENCY");
    }

    @Test
    void deserializesExperienceReward() throws Exception {
        final String json = """
                {
                  \"type\": \"EXPERIENCE_LEVEL\",
                  \"amount\": 7,
                  \"experienceType\": \"LEVELS\"
                }
                """;

        final AbstractReward reward = this.objectMapper.readValue(json, AbstractReward.class);

        final ExperienceReward experienceReward = assertInstanceOf(ExperienceReward.class, reward);
        assertEquals(AbstractReward.Type.EXPERIENCE, experienceReward.getType());
        assertEquals(7, experienceReward.getAmount());
        assertEquals(ExperienceReward.ExperienceType.LEVELS, experienceReward.getExperienceType());

        assertTypePreserved(experienceReward, "EXPERIENCE_LEVEL");
    }

    @Test
    void deserializesCommandReward() throws Exception {
        final String json = """
                {
                  \"type\": \"COMMAND\",
                  \"command\": \"/say hello\",
                  \"executeAsPlayer\": true,
                  \"delay\": 20
                }
                """;

        final AbstractReward reward = this.objectMapper.readValue(json, AbstractReward.class);

        final CommandReward commandReward = assertInstanceOf(CommandReward.class, reward);
        assertEquals(AbstractReward.Type.COMMAND, commandReward.getType());
        assertEquals("/say hello", commandReward.getCommand());
        assertEquals(true, commandReward.isExecuteAsPlayer());
        assertEquals(20L, commandReward.getDelayTicks());

        assertTypePreserved(commandReward, "COMMAND");
    }

    @Test
    void deserializesCompositeReward() throws Exception {
        final String json = """
                {
                  \"type\": \"COMPOSITE\",
                  \"continueOnError\": false,
                  \"rewards\": [
                    {
                      \"type\": \"ITEM\",
                      \"material\": \"EMERALD\",
                      \"amount\": 2,
                      \"dropIfFull\": false
                    },
                    {
                      \"type\": \"COMMAND\",
                      \"command\": \"/msg %player% Congrats!\",
                      \"executeAsPlayer\": false,
                      \"delay\": 0
                    }
                  ]
                }
                """;

        final AbstractReward reward = this.objectMapper.readValue(json, AbstractReward.class);

        final CompositeReward compositeReward = assertInstanceOf(CompositeReward.class, reward);
        assertEquals(AbstractReward.Type.COMPOSITE, compositeReward.getType());
        assertEquals(false, compositeReward.isContinueOnError());
        assertEquals(2, compositeReward.getRewardCount());

        final List<AbstractReward> nestedRewards = compositeReward.getRewards();
        assertInstanceOf(ItemReward.class, nestedRewards.get(0));
        assertInstanceOf(CommandReward.class, nestedRewards.get(1));

        final String serialized = this.objectMapper.writeValueAsString(compositeReward);
        final JsonNode tree = this.objectMapper.readTree(serialized);

        assertEquals("COMPOSITE", tree.get("type").asText());
        assertEquals("ITEM", tree.get("rewards").get(0).get("type").asText());
        assertEquals("COMMAND", tree.get("rewards").get(1).get("type").asText());
    }

    private void assertTypePreserved(final AbstractReward reward, final String expectedType) throws Exception {
        final String serialized = this.objectMapper.writeValueAsString(reward);
        final JsonNode tree = this.objectMapper.readTree(serialized);
        assertEquals(expectedType, tree.get("type").asText());
    }
}
