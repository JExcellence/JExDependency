package com.raindropcentral.rds.items.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rplatform.json.ItemStackJSONDeserializer;
import com.raindropcentral.rplatform.json.ItemStackJSONSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents item parser.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ItemParser {

    private static ObjectMapper objectMapper;
    private static final Object MAPPER_LOCK = new Object();

    private ItemParser() {
    }

    /**
     * Returns the object mapper.
     *
     * @return the object mapper
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            synchronized (MAPPER_LOCK) {
                if (objectMapper == null) {
                    objectMapper = createObjectMapper();
                }
            }
        }
        return objectMapper;
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(ItemStack.class, new ItemStackJSONSerializer());
        bukkitModule.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());
        mapper.registerModule(bukkitModule);

        return mapper;
    }

    /**
     * Executes parse.
     *
     * @param json serialized JSON payload
     * @return the parse result
     * @throws RuntimeException if the payload cannot be parsed
     */
    public static AbstractItem parse(@NotNull final String json) {
        try {
            return getObjectMapper().readValue(json, AbstractItem.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse item JSON", e);
        }
    }

    /**
     * Executes serialize.
     *
     * @param item target item payload
     * @return the serialize result
     * @throws RuntimeException if the payload cannot be serialized
     */
    public static String serialize(@NotNull final AbstractItem item) {
        try {
            return getObjectMapper().writeValueAsString(item);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize item", e);
        }
    }

    /**
     * Parses list.
     *
     * @param json serialized JSON payload
     * @return the parsed list
     * @throws RuntimeException if the payload cannot be parsed
     */
    public static @NotNull List<AbstractItem> parseList(final String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return getObjectMapper().readValue(
                    json,
                    new TypeReference<List<AbstractItem>>() {
                    }
            );
        } catch (Exception ignored) {
            try {
                final JsonNode root = getObjectMapper().readTree(json);
                final List<AbstractItem> items = new ArrayList<>();

                if (root == null || root.isNull()) {
                    return items;
                }

                if (!root.isArray()) {
                    items.add(parseNode(root));
                    return items;
                }

                for (JsonNode node : root) {
                    if (node == null || node.isNull()) {
                        continue;
                    }
                    items.add(parseNode(node));
                }

                return items;
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse item list JSON", e);
            }
        }
    }

    /**
     * Serializes list.
     *
     * @param items item payloads to serialize or assign
     * @return the serialized list
     * @throws RuntimeException if the payload cannot be serialized
     */
    public static String serializeList(final List<? extends AbstractItem> items) {
        try {
            final List<AbstractItem> safeItems = new ArrayList<>();
            if (items != null) {
                safeItems.addAll(items);
            }

            return getObjectMapper()
                    .writerFor(new TypeReference<List<AbstractItem>>() {
                    })
                    .writeValueAsString(safeItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize item list", e);
        }
    }

    private static AbstractItem parseNode(final @NotNull JsonNode node) {
        try {
            if (node.hasNonNull("type")) {
                return getObjectMapper().treeToValue(node, AbstractItem.class);
            }

            if (node.has("item")) {
                return getObjectMapper().treeToValue(node, ShopItem.class);
            }

            throw new IllegalArgumentException("Unsupported shop item payload: missing type and item fields");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse item node", e);
        }
    }

    /**
     * Executes reset mapper.
     */
    public static void resetMapper() {
        objectMapper = null;
    }
}
