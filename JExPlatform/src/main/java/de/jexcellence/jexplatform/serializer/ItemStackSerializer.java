package de.jexcellence.jexplatform.serializer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Binary and Base64 serialization for {@link ItemStack} instances and arrays.
 *
 * <p>Uses Bukkit's {@link BukkitObjectOutputStream} for faithful round-trip
 * reproduction of all item metadata. Null stacks are normalized to
 * {@link Material#AIR} during serialization.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class ItemStackSerializer {

    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    private ItemStackSerializer() {
    }

    // ── Single item ───────────────────────────────────────────────────────────

    /**
     * Serializes a single {@link ItemStack} to a binary array.
     *
     * @param itemStack stack to serialize (may be {@code null})
     * @return binary representation
     * @throws IllegalStateException if the Bukkit stream cannot be written
     */
    public static byte[] serialize(@Nullable ItemStack itemStack) {
        var stack = itemStack != null ? itemStack : new ItemStack(Material.AIR);

        try (var byteStream = new ByteArrayOutputStream();
             var bukkitOut = new BukkitObjectOutputStream(byteStream)) {

            bukkitOut.writeObject(stack);
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ItemStack", e);
        }
    }

    /**
     * Reconstructs an {@link ItemStack} from the binary payload.
     *
     * @param data binary data produced by {@link #serialize(ItemStack)}
     * @return deserialized item stack
     * @throws IllegalStateException if the data cannot be read
     */
    public static @NotNull ItemStack deserialize(byte[] data) {
        try (var byteStream = new ByteArrayInputStream(data);
             var bukkitIn = new BukkitObjectInputStream(byteStream)) {

            return (ItemStack) bukkitIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize ItemStack", e);
        }
    }

    // ── Array ─────────────────────────────────────────────────────────────────

    /**
     * Serializes an array of item stacks to binary.
     *
     * @param items array to serialize (may be {@code null})
     * @return binary representation
     * @throws IllegalStateException if the array cannot be written
     */
    public static byte[] serializeArray(@Nullable ItemStack[] items) {
        var array = items != null ? items : new ItemStack[0];

        try (var byteStream = new ByteArrayOutputStream();
             var bukkitOut = new BukkitObjectOutputStream(byteStream)) {

            bukkitOut.writeInt(array.length);
            for (var item : array) {
                bukkitOut.writeObject(item != null ? item : new ItemStack(Material.AIR));
            }

            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize ItemStack array", e);
        }
    }

    /**
     * Deserializes an array of {@link ItemStack ItemStacks} from binary.
     *
     * @param data binary representation
     * @return reconstructed item stack array
     * @throws IllegalStateException if the data cannot be read
     */
    public static @NotNull ItemStack[] deserializeArray(byte[] data) {
        try (var byteStream = new ByteArrayInputStream(data);
             var bukkitIn = new BukkitObjectInputStream(byteStream)) {

            var length = bukkitIn.readInt();
            var items = new ItemStack[length];

            for (var i = 0; i < length; i++) {
                items[i] = (ItemStack) bukkitIn.readObject();
            }

            return items;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize ItemStack array", e);
        }
    }

    // ── Base64 ────────────────────────────────────────────────────────────────

    /**
     * Encodes a single item stack as a Base64 string.
     *
     * @param itemStack stack to encode (may be {@code null})
     * @return Base64 text
     * @throws IllegalStateException if serialization fails
     */
    public static @NotNull String toBase64(@Nullable ItemStack itemStack) {
        return B64_ENCODER.encodeToString(serialize(itemStack));
    }

    /**
     * Decodes a Base64 representation into an {@link ItemStack}.
     *
     * @param base64 Base64 encoded stack
     * @return deserialized item stack
     * @throws IllegalStateException if decoding or deserialization fails
     */
    public static @NotNull ItemStack fromBase64(@NotNull String base64) {
        try {
            return deserialize(B64_DECODER.decode(base64));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode Base64 ItemStack", e);
        }
    }

    /**
     * Encodes an array of item stacks as a Base64 string.
     *
     * @param items array to encode (may be {@code null})
     * @return Base64 encoded representation
     * @throws IllegalStateException if serialization fails
     */
    public static @NotNull String arrayToBase64(@Nullable ItemStack[] items) {
        return B64_ENCODER.encodeToString(serializeArray(items));
    }

    /**
     * Decodes a Base64 string back into an array of stacks.
     *
     * @param base64 Base64 encoded stack array
     * @return reconstructed array
     * @throws IllegalStateException if decoding or deserialization fails
     */
    public static @NotNull ItemStack[] arrayFromBase64(@NotNull String base64) {
        try {
            return deserializeArray(B64_DECODER.decode(base64));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode Base64 ItemStack array", e);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Determines whether the provided stack is effectively empty.
     *
     * @param itemStack stack to inspect (may be {@code null})
     * @return {@code true} when the stack should be treated as empty
     */
    public static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null
                || itemStack.getType() == Material.AIR
                || itemStack.getAmount() <= 0;
    }

    /**
     * Creates a deep copy of the provided stack via binary serialization.
     *
     * @param itemStack stack to clone (may be {@code null})
     * @return deep copy, or {@code null} when the input is {@code null}
     * @throws IllegalStateException if the stack cannot be round-tripped
     */
    public static @Nullable ItemStack deepClone(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return deserialize(serialize(itemStack));
    }
}
