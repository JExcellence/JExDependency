package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * JPA converter that persists {@link ItemStack} values as Base64-encoded
 * binary strings using {@link ItemStack#serializeAsBytes()}.
 *
 * <p>Empty stacks are encoded as empty strings. {@code null} attributes
 * map to {@code null} columns.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class ItemStackConverter implements AttributeConverter<ItemStack, String> {

    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        if (itemStack.isEmpty()) {
            return "";
        }
        return B64_ENCODER.encodeToString(itemStack.serializeAsBytes());
    }

    @Override
    public ItemStack convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isEmpty()) {
            return new ItemStack(Material.AIR);
        }
        try {
            return ItemStack.deserializeBytes(B64_DECODER.decode(columnValue));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid Base64 ItemStack data in column.", ex);
        }
    }
}
