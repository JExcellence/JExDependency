package de.jexcellence.jexplatform.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jackson-based JSON serializer and deserializer for Bukkit {@link ItemStack}.
 *
 * <p>Supports two serialization formats:
 * <ul>
 *   <li><strong>Binary</strong> — uses {@link ItemStack#serializeAsBytes()} for
 *       complete metadata preservation (including persistent data containers)</li>
 *   <li><strong>Map</strong> — uses {@link ItemStack#serialize()} as a fallback
 *       for broader compatibility</li>
 * </ul>
 *
 * <p>Register the inner {@link Serializer} and {@link Deserializer} with a Jackson
 * {@code ObjectMapper} module to enable transparent JSON conversion.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class ItemStackJsonSerializer {

    private ItemStackJsonSerializer() {
    }

    // ── Serializer ────────────────────────────────────────────────────────────

    /**
     * Jackson serializer that writes an {@link ItemStack} as a JSON object
     * containing either a binary or map payload.
     */
    public static final class Serializer extends StdSerializer<ItemStack> {

        /**
         * Creates a new ItemStack serializer.
         */
        public Serializer() {
            super(ItemStack.class);
        }

        @Override
        public void serialize(@Nullable ItemStack itemStack,
                              @NotNull JsonGenerator gen,
                              @NotNull SerializerProvider provider) throws IOException {
            if (itemStack == null) {
                gen.writeNull();
                return;
            }

            var normalized = itemStack.clone();
            normalized.setAmount(1);

            gen.writeStartObject();

            try {
                var data = Base64.getEncoder()
                        .encodeToString(normalized.serializeAsBytes());
                gen.writeStringField("binaryData", data);
                gen.writeStringField("serializationType", "binary");
            } catch (Exception e) {
                var serialized = normalized.serialize();
                gen.writeFieldName("mapData");
                provider.defaultSerializeValue(serialized, gen);
                gen.writeStringField("serializationType", "map");
            }

            gen.writeEndObject();
        }
    }

    // ── Deserializer ──────────────────────────────────────────────────────────

    /**
     * Jackson deserializer that reconstructs an {@link ItemStack} from
     * binary, map, or legacy JSON formats.
     */
    public static final class Deserializer extends StdDeserializer<ItemStack> {

        /**
         * Creates a new ItemStack deserializer.
         */
        public Deserializer() {
            super(ItemStack.class);
        }

        @Override
        public @NotNull ItemStack deserialize(@NotNull JsonParser parser,
                                              @NotNull DeserializationContext ctx) throws IOException {
            var node = (JsonNode) parser.readValueAsTree();

            if (node.has("serializationType")) {
                var type = node.get("serializationType").asText();

                if ("binary".equals(type) && node.has("binaryData")) {
                    return deserializeFromBinary(node.get("binaryData").asText());
                } else if ("map".equals(type) && node.has("mapData")) {
                    return deserializeFromMap(node.get("mapData"));
                }
            }

            return deserializeLegacyFormat(node);
        }

        private @NotNull ItemStack deserializeFromBinary(@NotNull String binaryData) {
            try {
                var data = Base64.getDecoder().decode(binaryData);
                return ItemStack.deserializeBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize ItemStack from binary data", e);
            }
        }

        private @NotNull ItemStack deserializeFromMap(@NotNull JsonNode mapNode) {
            var map = new HashMap<String, Object>();
            mapNode.fields().forEachRemaining(entry ->
                    map.put(entry.getKey(), convertNode(entry.getValue())));

            var amount = resolveAmount(map.get("amount"));
            map.put("amount", 1);

            var item = ItemStack.deserialize(map);
            item.setAmount(amount);
            return item;
        }

        private @NotNull ItemStack deserializeLegacyFormat(@NotNull JsonNode node) {
            if (node.has("type")) {
                var typeStr = node.get("type").asText();
                try {
                    var material = Material.valueOf(typeStr);
                    var amount = node.has("amount") ? node.get("amount").asInt(1) : 1;
                    return new ItemStack(material, Math.max(1, amount));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid material type: " + typeStr, e);
                }
            }
            throw new RuntimeException(
                    "Unable to deserialize ItemStack: missing required fields");
        }

        private @Nullable Object convertNode(@NotNull JsonNode node) {
            if (node.isTextual()) return node.asText();
            if (node.isInt()) return node.asInt();
            if (node.isDouble()) return node.asDouble();
            if (node.isBoolean()) return node.asBoolean();
            if (node.isArray()) {
                var list = new ArrayList<>();
                node.forEach(el -> list.add(convertNode(el)));
                return list;
            }
            if (node.isObject()) {
                var map = new HashMap<String, Object>();
                node.fields().forEachRemaining(e ->
                        map.put(e.getKey(), convertNode(e.getValue())));
                return map;
            }
            return null;
        }

        private int resolveAmount(@Nullable Object raw) {
            return switch (raw) {
                case Number n -> Math.max(1, n.intValue());
                case String s -> {
                    try {
                        yield Math.max(1, Integer.parseInt(s));
                    } catch (NumberFormatException ignored) {
                        yield 1;
                    }
                }
                case null, default -> 1;
            };
        }
    }
}
