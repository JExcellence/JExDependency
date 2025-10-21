package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * Serialises Bukkit {@link ItemStack} payloads to Base64 encoded strings for database storage and back again.
 *
 * <p>The converter stores the binary {@link ItemStack#serializeAsBytes()} payload using a Base64 encoding.
 * Empty stacks are encoded as an empty string to preserve meaning while minimising storage costs.
 * {@code null} attributes map to {@code null} columns and vice versa, and invalid Base64 data triggers an
 * {@link IllegalArgumentException} to signal corrupt persistence state.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class ItemStackConverter implements AttributeConverter<ItemStack, String> {

    /** Encoder for transforming {@link ItemStack} bytes into a compact Base64 column payload. */
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    /** Decoder for rebuilding {@link ItemStack} instances from the stored column payload. */
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    /**
     * Converts the supplied {@link ItemStack} into the Base64 column representation.
     *
     * @param itemStack the stack being persisted; may be {@code null}
     * @return {@code null} when the stack is {@code null}, an empty string when {@link ItemStack#isEmpty()},
     *         or the Base64 payload otherwise
     */
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

    /**
     * Rebuilds an {@link ItemStack} instance from its stored Base64 column representation.
     *
     * @param columnValue the raw database value; {@code null} produces {@code null} and an empty string yields
     *                    an {@link Material#AIR} stack
     * @return the reconstructed stack, {@code null}, or an {@code AIR} stack depending on the input
     * @throws IllegalArgumentException when the stored payload is not valid Base64 or cannot be deserialised
     */
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