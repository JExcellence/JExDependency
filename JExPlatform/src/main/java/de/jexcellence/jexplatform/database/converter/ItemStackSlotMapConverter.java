package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * JPA converter for slot-indexed {@link ItemStack} inventories stored as
 * a delimiter-safe string payload.
 *
 * <p>Keys represent slot indices. Empty or null stacks are omitted so
 * persisted payloads remain sparse.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class ItemStackSlotMapConverter
        implements AttributeConverter<Map<Integer, ItemStack>, String> {

    private static final String ENTRY_DELIM = ";";
    private static final String VALUE_DELIM = ":";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable Map<Integer, ItemStack> inventory) {
        if (inventory == null) {
            return null;
        }
        if (inventory.isEmpty()) {
            return "";
        }

        var payload = new StringBuilder(inventory.size() * 64);
        var first = true;

        for (var entry : new TreeMap<>(inventory).entrySet()) {
            var slot = entry.getKey();
            var item = entry.getValue();
            if (slot == null || slot < 0) {
                continue;
            }
            if (item == null || item.isEmpty()) {
                continue;
            }

            if (!first) {
                payload.append(ENTRY_DELIM);
            } else {
                first = false;
            }

            payload.append(slot)
                    .append(VALUE_DELIM)
                    .append(B64_ENCODER.encodeToString(item.serializeAsBytes()));
        }

        return payload.toString();
    }

    @Override
    public Map<Integer, ItemStack> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null) {
            return null;
        }

        var inventory = new HashMap<Integer, ItemStack>();
        if (columnValue.isBlank()) {
            return inventory;
        }

        var entries = columnValue.split(ENTRY_DELIM, -1);
        for (var entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            var sep = entry.indexOf(VALUE_DELIM);
            if (sep < 0) {
                throw new IllegalArgumentException(
                        "Invalid inventory entry without slot delimiter: '" + entry + "'");
            }

            var slotToken = entry.substring(0, sep).trim();
            var valueToken = entry.substring(sep + 1).trim();
            if (valueToken.isEmpty()) {
                continue;
            }

            try {
                var slot = Integer.parseInt(slotToken);
                if (slot < 0) {
                    throw new IllegalArgumentException(
                            "Inventory slot cannot be negative: " + slot);
                }
                inventory.put(slot,
                        ItemStack.deserializeBytes(B64_DECODER.decode(valueToken)));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "Invalid slot inventory entry: '" + entry + "'", ex);
            }
        }

        return inventory;
    }
}
