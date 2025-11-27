package com.raindropcentral.rdq.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Custom deserializer for Bukkit {@link ItemStack} objects.
 * <p>
 * This class reconstructs {@code ItemStack} objects from their JSON representation,
 * handling material type, amount, durability, display name, lore, custom model data, and enchantments.
 * It is intended for use with Jackson's data binding and supports Adventure's {@link Component} system
 * for display name and lore.
 * </p>
 *
 * <p>
 * Example JSON structure:
 * <pre>
 * {
 *   "type": "DIAMOND_SWORD",
 *   "amount": 1,
 *   "durability": 10,
 *   "meta": {
 *     "displayName": "&lt;green&gt;Epic Sword",
 *     "lore": ["&lt;gray&gt;A legendary blade", "&lt;yellow&gt;Sharp and shiny"],
 *     "customModelData": 123,
 *     "enchantments": {
 *       "sharpness": 5,
 *       "unbreaking": 3
 *     }
 *   }
 * }
 * </pre>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class ItemStackJSONDeserializer extends JsonDeserializer<ItemStack> {

    /**
     * Deserializes a JSON representation into an {@link ItemStack} object.
     *
     * @param jsonParser             the JSON parser
     * @param deserializationContext the deserialization context
     * @return the deserialized {@code ItemStack}
     * @throws IOException if an I/O error occurs during parsing
     */
    @Override
    public @NotNull ItemStack deserialize(
            @NotNull final JsonParser jsonParser,
            @NotNull final DeserializationContext deserializationContext
    ) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        final ItemStack itemStack = this.createBaseItemStack(node);

        if (node.has("durability")) {
            itemStack.setDurability((short) node.get("durability").asInt());
        }

        if (node.has("meta")) {
            this.applyMetaData(itemStack, node.get("meta"));
        }

        return itemStack;
    }

    /**
     * Creates a base {@link ItemStack} with the specified material and amount from the JSON node.
     *
     * @param node the JSON node containing item data
     * @return the created {@code ItemStack}
     */
    private @NotNull ItemStack createBaseItemStack(
            @NotNull final JsonNode node
    ) {
        final String typeStr = node.get("type").asText();
        final Material material = Material.valueOf(typeStr);
        final int amount = node.get("amount").asInt(1);

        return new ItemStack(material, amount);
    }

    /**
     * Applies metadata to the {@link ItemStack} from the JSON representation.
     * Handles display name, lore, custom model data, and enchantments.
     *
     * @param itemStack the {@code ItemStack} to modify
     * @param metaNode  the JSON node containing metadata
     */
    private void applyMetaData(
            @NotNull final ItemStack itemStack,
            @NotNull final JsonNode metaNode
    ) {
        final ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) {
            return;
        }

        this.applyDisplayName(meta, metaNode);
        this.applyLore(meta, metaNode);
        this.applyCustomModelData(meta, metaNode);
        this.applyEnchantments(meta, metaNode);

        itemStack.setItemMeta(meta);
    }

    /**
     * Applies display name to the item metadata if present in the JSON.
     * Uses Adventure's {@link Component} system and MiniMessage formatting.
     *
     * @param meta     the {@link ItemMeta} to modify
     * @param metaNode the JSON node containing metadata
     */
    private void applyDisplayName(
            @NotNull final ItemMeta meta,
            @NotNull final JsonNode metaNode
    ) {
        if (metaNode.has("displayName")) {
            meta.displayName(this.deserializeComponent(metaNode.get("displayName").asText()));
        }
    }

    /**
     * Applies lore to the item metadata if present in the JSON.
     * Each lore line is deserialized as a MiniMessage {@link Component}.
     *
     * @param meta     the {@link ItemMeta} to modify
     * @param metaNode the JSON node containing metadata
     */
    private void applyLore(
            @NotNull final ItemMeta meta,
            @NotNull final JsonNode metaNode
    ) {
        if (metaNode.has("lore")) {
            final List<Component> lore = new ArrayList<>();
            final JsonNode loreNode = metaNode.get("lore");

            if (loreNode.isArray()) {
                for (final JsonNode line : loreNode) {
                    lore.add(this.deserializeComponent(line.asText()));
                }
            }

            meta.lore(lore);
        }
    }

    /**
     * Applies custom model data to the item metadata if present in the JSON.
     *
     * @param meta     the {@link ItemMeta} to modify
     * @param metaNode the JSON node containing metadata
     */
    private void applyCustomModelData(
            @NotNull final ItemMeta meta,
            @NotNull final JsonNode metaNode
    ) {
        if (metaNode.has("customModelData")) {
            meta.setCustomModelData(metaNode.get("customModelData").asInt());
        }
    }

    /**
     * Applies enchantments to the item metadata if present in the JSON.
     * Enchantments are specified as a map of enchantment key names to levels.
     *
     * @param meta     the {@link ItemMeta} to modify
     * @param metaNode the JSON node containing metadata
     */
    private void applyEnchantments(
            @NotNull final ItemMeta meta,
            @NotNull final JsonNode metaNode
    ) {
        if (metaNode.has("enchantments")) {
            final JsonNode enchantmentsNode = metaNode.get("enchantments");
            final Iterator<Map.Entry<String, JsonNode>> fields = enchantmentsNode.fields();

            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                final Enchantment enchantment = this.getEnchantment(entry.getKey());

                if (enchantment != null) {
                    meta.addEnchant(enchantment, entry.getValue().asInt(), true);
                }
            }
        }
    }

    /**
     * Gets an {@link Enchantment} by its key name.
     *
     * @param key the enchantment key name (e.g., "sharpness")
     * @return the {@link Enchantment} or {@code null} if not found
     */
    private @Nullable Enchantment getEnchantment(
            @NotNull final String key
    ) {
        return Enchantment.getByKey(NamespacedKey.minecraft(key));
    }

    /**
     * Deserializes a MiniMessage string into an Adventure {@link Component}.
     *
     * @param text the MiniMessage formatted text
     * @return the deserialized {@link Component}
     */
    private @NotNull Component deserializeComponent(
            @NotNull final String text
    ) {
        return MiniMessage.miniMessage().deserialize(text);
    }
}
