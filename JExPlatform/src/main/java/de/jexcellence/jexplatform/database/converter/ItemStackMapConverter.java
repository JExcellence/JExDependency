package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA converter that persists {@link Map} entries of string keys to
 * {@link ItemStack} values using a delimiter-safe Base64 format.
 *
 * <p>Keys are UTF-8 encoded before Base64. Empty or null stacks are
 * represented by an empty value token that rebuilds to {@link Material#AIR}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class ItemStackMapConverter implements AttributeConverter<Map<String, ItemStack>, String> {

    private static final String ENTRY_DELIM = ";";
    private static final String KV_DELIM = ":";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable Map<String, ItemStack> map) {
        if (map == null) {
            return null;
        }
        if (map.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder(map.size() * 64);
        var first = true;

        for (var entry : map.entrySet()) {
            var key = entry.getKey();
            if (key == null) {
                continue;
            }

            var keyToken = B64_ENCODER.encodeToString(
                    key.getBytes(StandardCharsets.UTF_8));
            var value = entry.getValue();
            var valueToken = (value == null || value.isEmpty())
                    ? ""
                    : B64_ENCODER.encodeToString(value.serializeAsBytes());

            if (!first) {
                sb.append(ENTRY_DELIM);
            } else {
                first = false;
            }
            sb.append(keyToken).append(KV_DELIM).append(valueToken);
        }
        return sb.toString();
    }

    @Override
    public Map<String, ItemStack> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null) {
            return null;
        }
        var result = new HashMap<String, ItemStack>();
        if (columnValue.isBlank()) {
            return result;
        }

        var entries = columnValue.split(ENTRY_DELIM, -1);
        for (var entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            var sep = entry.indexOf(KV_DELIM);
            if (sep < 0) {
                throw new IllegalArgumentException(
                        "Invalid map entry (missing key/value delimiter): '" + entry + "'");
            }

            var keyToken = entry.substring(0, sep);
            var valueToken = entry.substring(sep + 1);

            try {
                var key = new String(B64_DECODER.decode(keyToken), StandardCharsets.UTF_8);
                var item = valueToken.isEmpty()
                        ? new ItemStack(Material.AIR)
                        : ItemStack.deserializeBytes(B64_DECODER.decode(valueToken));
                result.put(key, item);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid Base64 in map entry: '" + entry + "'", ex);
            }
        }

        return result;
    }
}
