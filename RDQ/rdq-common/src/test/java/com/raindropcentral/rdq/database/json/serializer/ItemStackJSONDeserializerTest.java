package com.raindropcentral.rdq.database.json.serializer;

import be.seeseemelk.mockbukkit.MockBukkit;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackJSONDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();

        final SimpleModule module = new SimpleModule();
        module.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());

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
    void deserializesFullItemStackWithMeta() throws Exception {
        final ItemStack itemStack = this.mapper.readValue(
                this.readFixture("json/itemstack/full_item.json"),
                ItemStack.class
        );

        assertEquals(Material.DIAMOND_SWORD, itemStack.getType());
        assertEquals(2, itemStack.getAmount());
        assertEquals(12, itemStack.getDurability());

        final ItemMeta meta = itemStack.getItemMeta();
        assertNotNull(meta);
        assertEquals(Component.text("Epic Blade", NamedTextColor.GREEN), meta.displayName());
        assertEquals(
                List.of(
                        Component.text("Forged in tests", NamedTextColor.GRAY),
                        Component.text("Unit coverage rising", NamedTextColor.YELLOW)
                ),
                meta.lore()
        );
        assertEquals(456, meta.getCustomModelData());
        assertTrue(meta.hasEnchant(Enchantment.DAMAGE_ALL));
        assertEquals(4, meta.getEnchantLevel(Enchantment.DAMAGE_ALL));
        assertTrue(meta.hasEnchant(Enchantment.DURABILITY));
        assertEquals(2, meta.getEnchantLevel(Enchantment.DURABILITY));
    }

    @Test
    void throwsExceptionForInvalidMaterial() {
        assertThrows(
                IllegalArgumentException.class,
                () -> this.mapper.readValue(
                        this.readFixture("json/itemstack/invalid_material.json"),
                        ItemStack.class
                )
        );
    }

    @Test
    void missingOptionalFieldsLeaveDefaults() throws Exception {
        final ItemStack itemStack = this.mapper.readValue(
                this.readFixture("json/itemstack/defaults.json"),
                ItemStack.class
        );

        assertEquals(Material.STONE, itemStack.getType());
        assertEquals(1, itemStack.getAmount());
        assertEquals(0, itemStack.getDurability());

        final ItemMeta meta = itemStack.getItemMeta();
        assertNotNull(meta);
        assertNull(meta.displayName());
        assertFalse(meta.hasLore());
        assertFalse(meta.hasEnchants());
        assertNull(meta.getCustomModelData());
    }

    private String readFixture(final String path) throws IOException {
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Fixture not found: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
