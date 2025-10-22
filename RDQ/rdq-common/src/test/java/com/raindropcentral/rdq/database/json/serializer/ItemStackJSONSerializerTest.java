package com.raindropcentral.rdq.database.json.serializer;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackJSONSerializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();

        final SimpleModule module = new SimpleModule();
        module.addSerializer(ItemStack.class, new ItemStackJSONSerializer());

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(module);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void serializesFullItemStackWithMeta() throws Exception {
        final ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
        itemStack.setAmount(3);
        itemStack.setDurability((short) 17);

        final ItemMeta meta = itemStack.getItemMeta();
        assertNotNull(meta);

        meta.displayName(Component.text("Radiant Blade", NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("Forged in serialization", NamedTextColor.GRAY),
                Component.text("Meta coverage complete", NamedTextColor.YELLOW)
        ));
        meta.setCustomModelData(321);
        meta.addEnchant(Enchantment.DAMAGE_ALL, 4, true);
        meta.addEnchant(Enchantment.DURABILITY, 2, true);
        itemStack.setItemMeta(meta);

        final JsonNode root = this.mapper.readTree(this.mapper.writeValueAsString(itemStack));

        assertEquals("DIAMOND_SWORD", root.get("type").asText());
        assertEquals(3, root.get("amount").asInt());
        assertEquals(17, root.get("durability").asInt());

        final JsonNode metaNode = root.get("meta");
        assertNotNull(metaNode);
        assertEquals("Radiant Blade", metaNode.get("displayName").asText());

        final JsonNode loreNode = metaNode.get("lore");
        assertNotNull(loreNode);
        assertTrue(loreNode.isArray());
        assertEquals(2, loreNode.size());
        assertEquals("Forged in serialization", loreNode.get(0).asText());
        assertEquals("Meta coverage complete", loreNode.get(1).asText());

        assertEquals(321, metaNode.get("customModelData").asInt());

        final JsonNode enchantments = metaNode.get("enchantments");
        assertNotNull(enchantments);
        assertEquals(4, enchantments.get(Enchantment.DAMAGE_ALL.toString()).asInt());
        assertEquals(2, enchantments.get(Enchantment.DURABILITY.toString()).asInt());
    }

    @Test
    void serializesItemStackWithoutMeta() throws Exception {
        final ItemStack itemStack = new ItemStack(Material.STONE);
        itemStack.setAmount(5);
        itemStack.setDurability((short) 2);

        final JsonNode root = this.mapper.readTree(this.mapper.writeValueAsString(itemStack));

        assertEquals("STONE", root.get("type").asText());
        assertEquals(5, root.get("amount").asInt());
        assertEquals(2, root.get("durability").asInt());
        assertNull(root.get("meta"));
    }

    @Test
    void serializesNullItemStackAsJsonNull() throws Exception {
        final String json = this.mapper.writeValueAsString(null);
        assertEquals("null", json);
    }
}
