package com.raindropcentral.rdq.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Custom serializer for Bukkit {@link ItemStack} objects.
 * <p>
 * Converts {@code ItemStack} objects to their JSON representation for use with Jackson.
 * Handles serialization of material type, amount, durability, display name, lore, custom model data, and enchantments.
 * </p>
 *
 * <p>
 * Example JSON output:
 * <pre>
 * {
 *   "type": "DIAMOND_SWORD",
 *   "amount": 1,
 *   "durability": 10,
 *   "meta": {
 *     "displayName": "Epic Sword",
 *     "lore": ["A legendary blade", "Sharp and shiny"],
 *     "customModelData": 123,
 *     "enchantments": {
 *       "minecraft:sharpness": 5,
 *       "minecraft:unbreaking": 3
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
public class ItemStackJSONSerializer extends JsonSerializer<ItemStack> {

    /**
     * Serializes an {@link ItemStack} object to JSON.
     * <p>
     * Writes the basic item properties and, if present, the item meta properties.
     * </p>
     *
     * @param itemStack          the {@code ItemStack} to serialize (may be {@code null})
     * @param jsonGenerator      the JSON generator to write to
     * @param serializerProvider the serializer provider
     * @throws IOException if an I/O error occurs during serialization
     */
    @Override
    public void serialize(
            @Nullable final ItemStack itemStack,
            @NotNull final JsonGenerator jsonGenerator,
            @NotNull final SerializerProvider serializerProvider
    ) throws IOException {
        if (itemStack == null) {
            jsonGenerator.writeNull();
            return;
        }

        jsonGenerator.writeStartObject();
        this.writeBasicItemProperties(itemStack, jsonGenerator);

        if (itemStack.hasItemMeta()) {
            this.writeItemMeta(itemStack.getItemMeta(), jsonGenerator);
        }

        jsonGenerator.writeEndObject();
    }

    /**
     * Writes the basic properties of an {@link ItemStack} to the JSON generator.
     * <ul>
     *   <li>type: The material type name</li>
     *   <li>amount: The item stack amount</li>
     *   <li>durability: The item durability value</li>
     * </ul>
     *
     * @param itemStack     the {@code ItemStack} to serialize
     * @param jsonGenerator the JSON generator
     * @throws IOException if an I/O error occurs during serialization
     */
    private void writeBasicItemProperties(
            @NotNull final ItemStack itemStack,
            @NotNull final JsonGenerator jsonGenerator
    ) throws IOException {
        jsonGenerator.writeStringField("type", itemStack.getType().name());
        jsonGenerator.writeNumberField("amount", itemStack.getAmount());
        jsonGenerator.writeNumberField("durability", itemStack.getDurability());
    }

    /**
     * Writes the {@link ItemMeta} properties to the JSON generator.
     * <ul>
     *   <li>displayName: The display name of the item (if present)</li>
     *   <li>lore: The lore lines of the item (if present)</li>
     *   <li>customModelData: The custom model data value (if present)</li>
     *   <li>enchantments: The enchantments map (if present)</li>
     * </ul>
     *
     * @param meta          the {@code ItemMeta} to serialize
     * @param jsonGenerator the JSON generator
     * @throws IOException if an I/O error occurs during serialization
     */
    private void writeItemMeta(
            @NotNull final ItemMeta meta,
            @NotNull final JsonGenerator jsonGenerator
    ) throws IOException {
        jsonGenerator.writeObjectFieldStart("meta");

        this.writeDisplayNameIfPresent(meta, jsonGenerator);
        this.writeLoreIfPresent(meta, jsonGenerator);
        this.writeCustomModelDataIfPresent(meta, jsonGenerator);
        this.writeEnchantsIfPresent(meta, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    /**
     * Writes the display name to the JSON generator if present.
     *
     * @param meta          the {@code ItemMeta} to check
     * @param jsonGenerator the JSON generator
     * @throws IOException if an I/O error occurs during serialization
     */
    private void writeDisplayNameIfPresent(
            @NotNull final ItemMeta meta,
            @NotNull final JsonGenerator jsonGenerator
    ) throws IOException {
        if (meta.hasDisplayName()) {
            final String displayName = this.serializeComponent(meta.displayName());
            jsonGenerator.writeStringField("displayName", displayName);
        }
    }

    /**
     * Writes the lore to the JSON generator if present.
     * Each lore line is serialized as plain text.
     *
     * @param meta          the {@code ItemMeta} to check
     * @param jsonGenerator the JSON generator
     * @throws IOException if an I/O error occurs during serialization
     */
    private void writeLoreIfPresent(
            @NotNull final ItemMeta meta,
            @NotNull final JsonGenerator jsonGenerator
    ) throws IOException {
        if (meta.hasLore()) {
            jsonGenerator.writeArrayFieldStart("lore");
            final List<Component> lore = meta.lore();

            if (lore != null) {
                for (final Component line : lore) {
                    jsonGenerator.writeString(this.serializeComponent(line));
                }
            }

            jsonGenerator.writeEndArray();
        }
    }

    /**
     * Writes the custom model data to the JSON generator if present.
     *
     * @param meta          the {@code ItemMeta} to check
     * @param jsonGenerator the JSON generator
     * @throws IOException if an I/O error occurs during serialization
     */
    private void writeCustomModelDataIfPresent(
            @NotNull final ItemMeta meta,
            @NotNull final JsonGenerator jsonGenerator
    ) throws IOException {
        if (meta.hasCustomModelData()) {
            jsonGenerator.writeNumberField("customModelData", meta.getCustomModelData());
        }
    }

    /**
     * Writes the enchantments to the JSON generator if present.
     * Enchantments are serialized as a map of enchantment key names to their levels.
     *
     * @param meta          the {@code ItemMeta} to check
     * @param jsonGenerator the JSON generator
     * @throws IOException if an I/O error occurs during serialization
     */
    private void writeEnchantsIfPresent(
            @NotNull final ItemMeta meta,
            @NotNull final JsonGenerator jsonGenerator
    ) throws IOException {
        final Map<?, Integer> enchants = meta.getEnchants();

        if (!enchants.isEmpty()) {
            jsonGenerator.writeObjectFieldStart("enchantments");

            enchants.forEach((enchantment, level) -> {
                try {
                    jsonGenerator.writeNumberField(enchantment.toString(), level);
                } catch (IOException ignored) {
                    // Silently ignore IO exceptions for individual enchantments
                }
            });

            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Serializes a {@link Component} to a plain text string.
     *
     * @param component the {@code Component} to serialize
     * @return the serialized plain text string
     */
    private @NotNull String serializeComponent(
            @NotNull final Component component
    ) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
