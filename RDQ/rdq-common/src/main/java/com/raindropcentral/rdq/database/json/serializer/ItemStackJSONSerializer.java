package com.raindropcentral.rdq.database.json.serializer;

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
 * Converts {@code ItemStack} instances into their JSON representation for storage in the RDQ
 * persistence layer. The serializer covers the core attributes (type, amount, durability) and
 * enriches the payload with {@link ItemMeta} details such as display name, lore, custom model data,
 * and enchantments. Textual components are flattened with
 * {@link PlainTextComponentSerializer#plainText()} to ensure the JSON output contains plain strings.
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
 * @version 1.0.1
 * @since 1.0.0
 */
public class ItemStackJSONSerializer extends JsonSerializer<ItemStack> {

    /**
     * Serializes an {@link ItemStack} object to JSON.
     * <p>
     * Writes the basic item properties and, if present, the item meta properties. When the supplied
     * item stack is {@code null}, a JSON {@code null} literal is emitted to keep the serialized form
     * aligned with Jackson expectations. The provided {@link SerializerProvider} is included to
     * satisfy the Jackson contract, even though it is not directly consulted by this serializer.
     * </p>
     *
     * @param itemStack          the {@code ItemStack} to serialize (may be {@code null})
     * @param jsonGenerator      the JSON generator responsible for producing the output structure
     * @param serializerProvider the serializer provider supplied by Jackson for contextual lookups
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
     * <p>
     * The generated object always contains:
     * </p>
     * <ul>
     *   <li>{@code type}: the material identifier</li>
     *   <li>{@code amount}: the number of items contained in the stack</li>
     *   <li>{@code durability}: the current durability value</li>
     * </ul>
     *
     * @param itemStack     the {@code ItemStack} whose scalar values are being written
     * @param jsonGenerator the JSON generator responsible for writing the fields
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
     * <p>
     * Optional fields such as display name, lore, custom model data, and enchantments are only
     * included when present, ensuring the JSON representation mirrors the in-game item metadata.
     * </p>
     *
     * @param meta          the {@code ItemMeta} providing optional item details
     * @param jsonGenerator the JSON generator used to construct the {@code meta} object
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
     * <p>
     * The display name is converted from a {@link Component} to plain text so downstream consumers
     * do not need to understand Adventure serialization formats.
     * </p>
     *
     * @param meta          the {@code ItemMeta} to check for a display name
     * @param jsonGenerator the JSON generator receiving the {@code displayName} field
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
     * <p>
     * Each lore line is serialized as plain text to avoid leaking Adventure formatting codes while
     * preserving the user-facing text.
     * </p>
     *
     * @param meta          the {@code ItemMeta} to check for lore entries
     * @param jsonGenerator the JSON generator that receives the {@code lore} array field
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
     * @param meta          the {@code ItemMeta} to check for a custom model identifier
     * @param jsonGenerator the JSON generator receiving the {@code customModelData} field
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
     * <p>
     * Enchantments are serialized as a map where the key is the enchantment's namespaced key (via
     * {@link Object#toString()}) and the value is the applied level. Individual write failures are
     * ignored so that a single problematic enchantment does not abort the entire serialization
     * process.
     * </p>
     *
     * @param meta          the {@code ItemMeta} to check for enchantments
     * @param jsonGenerator the JSON generator that receives the {@code enchantments} object field
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
     * @return the serialized plain text string produced by {@link PlainTextComponentSerializer}
     */
    private @NotNull String serializeComponent(
            @NotNull final Component component
    ) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
