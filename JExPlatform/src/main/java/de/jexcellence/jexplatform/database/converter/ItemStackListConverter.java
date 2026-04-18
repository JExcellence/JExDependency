package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA converter that persists {@link List lists} of {@link ItemStack} values
 * as semicolon-delimited Base64 payloads.
 *
 * <p>Empty or null stacks are skipped during serialization. Empty tokens
 * during deserialization map to {@link Material#AIR}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class ItemStackListConverter implements AttributeConverter<List<ItemStack>, String> {

    private static final String DELIM = ";";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable List<ItemStack> items) {
        if (items == null) {
            return null;
        }
        if (items.isEmpty()) {
            return "";
        }
        return items.stream()
                .filter(item -> item != null && !item.isEmpty())
                .map(item -> B64_ENCODER.encodeToString(item.serializeAsBytes()))
                .collect(Collectors.joining(DELIM));
    }

    @Override
    public List<ItemStack> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        var parts = columnValue.split(DELIM, -1);
        var result = new ArrayList<ItemStack>(parts.length);

        for (var part : parts) {
            var token = part == null ? "" : part.trim();
            if (token.isEmpty()) {
                result.add(new ItemStack(Material.AIR));
                continue;
            }
            try {
                result.add(ItemStack.deserializeBytes(B64_DECODER.decode(token)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid Base64 ItemStack in list.", ex);
            }
        }

        return result;
    }
}
