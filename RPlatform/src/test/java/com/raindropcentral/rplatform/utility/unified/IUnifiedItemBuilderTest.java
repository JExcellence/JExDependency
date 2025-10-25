package com.raindropcentral.rplatform.utility.unified;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IUnifiedItemBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void fluentChainingReturnsConcreteBuilderType() {
        FakeItemBuilder builder = new FakeItemBuilder();
        Component name = Component.text("Example");

        assertSame(builder, builder.setName(name));
        assertSame(builder, builder.setLore(List.of(name)));
        assertSame(builder, builder.addLoreLine(name));
        assertSame(builder, builder.addLoreLines(List.of(name)));
        assertSame(builder, builder.addLoreLines(name));
        assertSame(builder, builder.setAmount(4));
        assertSame(builder, builder.setCustomModelData(17));
        assertSame(builder, builder.addEnchantment(Enchantment.DAMAGE_ALL, 2));
        assertSame(builder, builder.addItemFlags(ItemFlag.HIDE_ENCHANTS));
        assertSame(builder, builder.setGlowing(true));
    }

    @Test
    void methodSequencingIsDocumentedViaRecordedCalls() {
        FakeItemBuilder builder = new FakeItemBuilder();
        Component first = Component.text("First");
        Component second = Component.text("Second");
        Component third = Component.text("Third");

        builder.setName(first)
                .setLore(List.of(first))
                .addLoreLine(second)
                .addLoreLines(List.of(third))
                .addLoreLines(first, second)
                .setAmount(6)
                .setCustomModelData(99)
                .addEnchantment(Enchantment.LURE, 1)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .setGlowing(true)
                .setGlowing(false)
                .build();

        List<String> expected = List.of(
                "setName",
                "setLore",
                "addLoreLine",
                "addLoreLinesList",
                "addLoreLinesVarArgs",
                "setAmount",
                "setCustomModelData",
                "addEnchantment",
                "addItemFlags",
                "setGlowing:true",
                "setGlowing:false",
                "build"
        );

        assertEquals(expected, builder.getCalls());
    }

    @Test
    void buildReturnsConfiguredItemStack() {
        FakeItemBuilder builder = new FakeItemBuilder();
        Component name = Component.text("Configured");
        Component loreLine = Component.text("Lore");

        ItemStack stack = builder.setName(name)
                .setLore(List.of(loreLine))
                .setAmount(3)
                .setCustomModelData(123)
                .addEnchantment(Enchantment.LURE, 4)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(true)
                .build();

        assertSame(stack, builder.build());
        assertEquals(3, stack.getAmount());
        ItemMeta meta = stack.getItemMeta();
        assertEquals(123, meta.getCustomModelData());
        assertEquals(PlainTextComponentSerializer.plainText().serialize(name), meta.getDisplayName());
        List<String> lore = meta.getLore();
        assertNotNull(lore);
        assertEquals(1, lore.size());
        assertEquals(PlainTextComponentSerializer.plainText().serialize(loreLine), lore.get(0));
        assertTrue(meta.hasEnchant(Enchantment.LURE));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
    }

    private static final class FakeItemBuilder implements IUnifiedItemBuilder<ItemMeta, FakeItemBuilder> {

        private final ItemStack delegate = new ItemStack(Material.PAPER);
        private final List<String> calls = new ArrayList<>();

        @Override
        public FakeItemBuilder setName(@NotNull Component name) {
            calls.add("setName");
            ItemMeta meta = delegate.getItemMeta();
            meta.setDisplayName(PlainTextComponentSerializer.plainText().serialize(name));
            delegate.setItemMeta(meta);
            return this;
        }

        @Override
        public FakeItemBuilder setLore(@NotNull List<Component> lore) {
            calls.add("setLore");
            ItemMeta meta = delegate.getItemMeta();
            meta.setLore(serializeComponents(lore));
            delegate.setItemMeta(meta);
            return this;
        }

        @Override
        public FakeItemBuilder addLoreLine(@NotNull Component line) {
            calls.add("addLoreLine");
            ItemMeta meta = delegate.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(PlainTextComponentSerializer.plainText().serialize(line));
            meta.setLore(lore);
            delegate.setItemMeta(meta);
            return this;
        }

        @Override
        public FakeItemBuilder addLoreLines(@NotNull List<Component> lore) {
            calls.add("addLoreLinesList");
            ItemMeta meta = delegate.getItemMeta();
            List<String> existing = meta.getLore();
            if (existing == null) {
                existing = new ArrayList<>();
            }
            existing.addAll(serializeComponents(lore));
            meta.setLore(existing);
            delegate.setItemMeta(meta);
            return this;
        }

        @Override
        public FakeItemBuilder addLoreLines(@NotNull Component... lore) {
            calls.add("addLoreLinesVarArgs");
            return addLoreLines(Arrays.asList(lore));
        }

        @Override
        public FakeItemBuilder setAmount(int amount) {
            calls.add("setAmount");
            delegate.setAmount(amount);
            return this;
        }

        @Override
        public FakeItemBuilder setCustomModelData(int data) {
            calls.add("setCustomModelData");
            ItemMeta meta = delegate.getItemMeta();
            meta.setCustomModelData(data);
            delegate.setItemMeta(meta);
            return this;
        }

        @Override
        public FakeItemBuilder addEnchantment(@NotNull Enchantment enchantment, int level) {
            calls.add("addEnchantment");
            delegate.addUnsafeEnchantment(enchantment, level);
            return this;
        }

        @Override
        public FakeItemBuilder addItemFlags(@NotNull ItemFlag... flags) {
            calls.add("addItemFlags");
            ItemMeta meta = delegate.getItemMeta();
            meta.addItemFlags(flags);
            delegate.setItemMeta(meta);
            return this;
        }

        @Override
        public FakeItemBuilder setGlowing(boolean glowing) {
            calls.add("setGlowing:" + glowing);
            if (glowing) {
                delegate.addUnsafeEnchantment(Enchantment.LURE, 1);
                ItemMeta meta = delegate.getItemMeta();
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                delegate.setItemMeta(meta);
            } else {
                delegate.removeEnchantment(Enchantment.LURE);
                ItemMeta meta = delegate.getItemMeta();
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                delegate.setItemMeta(meta);
            }
            return this;
        }

        @Override
        public ItemStack build() {
            calls.add("build");
            return delegate;
        }

        List<String> getCalls() {
            return List.copyOf(calls);
        }

        private static List<String> serializeComponents(List<Component> components) {
            return components.stream()
                    .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
