package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * JPA attribute converter for mapping {@link ItemStack} to Base64 strings and back.
 *
 * Behavior:
 * - null ItemStack -> null DB value
 * - empty ItemStack -> empty string DB value
 * - null DB value -> null ItemStack
 * - empty string DB value -> ItemStack of Material.AIR
 */
@Converter(autoApply = true)
public class ItemStackConverter implements AttributeConverter<ItemStack, String> {

    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable final ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        if (itemStack.isEmpty()) {
            return "";
        }
        return B64_ENCODER.encodeToString(itemStack.serializeAsBytes());
    }

    @Override
    public ItemStack convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isEmpty()) {
            return new ItemStack(Material.AIR);
        }

        try {
            return ItemStack.deserializeBytes(B64_DECODER.decode(columnValue));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid Base64 ItemStack data in column.", ex);
        }
    }
}