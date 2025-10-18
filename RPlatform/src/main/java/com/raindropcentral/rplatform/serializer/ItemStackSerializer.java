package com.raindropcentral.rplatform.serializer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Handles binary and Base64 serialization of {@link ItemStack} instances and arrays for storage or
 * transmission across the RDC platform.
 *
 * <p>The serializer relies on Bukkit's {@link BukkitObjectOutputStream} and corresponding input
 * stream to faithfully reproduce the game items.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ItemStackSerializer {

    /**
     * Serializes a single {@link ItemStack} to a binary array using Bukkit's object stream.
     *
     * <p>{@code null} inputs are normalized to a {@link Material#AIR Material.AIR} stack. IO failures
     * cause an {@link IllegalStateException} with the original {@link IOException} attached.</p>
     *
     * @param itemStack stack to serialize, may be {@code null}
     * @return binary representation of the stack
     * @throws IllegalStateException if the Bukkit object stream cannot be written
     */
    public byte[] serialize(final @Nullable ItemStack itemStack) {
        final ItemStack stack = itemStack != null ? itemStack : new ItemStack(Material.AIR);

        try (final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             final BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(byteStream)) {
            
            bukkitOut.writeObject(stack);
            return byteStream.toByteArray();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to serialize ItemStack", e);
        }
    }

    /**
     * Reconstructs an {@link ItemStack} from the binary payload created by {@link #serialize(ItemStack)}.
     *
     * <p>If the stream cannot be read or the expected Bukkit class is missing, an
     * {@link IllegalStateException} is thrown wrapping the original error.</p>
     *
     * @param data binary data previously produced by {@link #serialize(ItemStack)}
     * @return deserialized item stack
     * @throws IllegalStateException if the data cannot be read or the class is missing
     */
    public @NotNull ItemStack deserialize(final byte[] data) {
        try (final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             final BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(byteStream)) {
            
            return (ItemStack) bukkitIn.readObject();
        } catch (final IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize ItemStack", e);
        }
    }

    /**
     * Serializes an array of item stacks by writing the array length followed by each normalized
     * element to a Bukkit object stream.
     *
     * <p>{@code null} arrays are treated as empty collections and {@code null} elements are replaced
     * with {@link Material#AIR Material.AIR}. IO exceptions surface as {@link IllegalStateException}
     * instances.</p>
     *
     * @param items array of stacks to serialize, may be {@code null}
     * @return binary representation of the array
     * @throws IllegalStateException if the array cannot be written
     */
    public byte[] serializeArray(final @Nullable ItemStack[] items) {
        final ItemStack[] array = items != null ? items : new ItemStack[0];

        try (final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             final BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(byteStream)) {
            
            bukkitOut.writeInt(array.length);
            for (final ItemStack item : array) {
                bukkitOut.writeObject(item != null ? item : new ItemStack(Material.AIR));
            }
            
            return byteStream.toByteArray();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to serialize ItemStack array", e);
        }
    }

    /**
     * Deserializes an array of {@link ItemStack ItemStacks} previously created by
     * {@link #serializeArray(ItemStack[])}.
     *
     * <p>Any failure to read the declared length or individual elements results in an
     * {@link IllegalStateException}. The returned array size matches the value encoded by the
     * serializer.</p>
     *
     * @param data binary representation created by {@link #serializeArray(ItemStack[])}
     * @return reconstructed item stack array
     * @throws IllegalStateException if the binary data cannot be read
     */
    public @NotNull ItemStack[] deserializeArray(final byte[] data) {
        try (final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             final BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(byteStream)) {
            
            final int length = bukkitIn.readInt();
            final ItemStack[] items = new ItemStack[length];
            
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) bukkitIn.readObject();
            }
            
            return items;
        } catch (final IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize ItemStack array", e);
        }
    }

    /**
     * Encodes a single item stack as a Base64 string after binary serialization.
     *
     * <p>Any serialization issue triggers the same {@link IllegalStateException} thrown by
     * {@link #serialize(ItemStack)}.</p>
     *
     * @param itemStack stack to encode, may be {@code null}
     * @return Base64 text representing the stack
     * @throws IllegalStateException if serialization fails
     */
    public @NotNull String toBase64(final @Nullable ItemStack itemStack) {
        final byte[] serialized = serialize(itemStack);
        return Base64Coder.encodeLines(serialized);
    }

    /**
     * Decodes a Base64 representation produced by {@link #toBase64(ItemStack)} and deserializes it
     * into an {@link ItemStack}.
     *
     * <p>Malformed Base64 or deserialization errors result in an {@link IllegalStateException}.</p>
     *
     * @param base64 Base64 encoded stack
     * @return deserialized item stack
     * @throws IllegalStateException if decoding or deserialization fails
     */
    public @NotNull ItemStack fromBase64(final @NotNull String base64) {
        try {
            final byte[] data = Base64Coder.decodeLines(base64);
            return deserialize(data);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to decode Base64 ItemStack", e);
        }
    }

    /**
     * Encodes an array of item stacks as a Base64 string suitable for storage.
     *
     * <p>Delegates to {@link #serializeArray(ItemStack[])} for the binary payload and therefore
     * inherits its null handling and exception behaviour.</p>
     *
     * @param items array of stacks to encode, may be {@code null}
     * @return Base64 encoded representation of the array
     * @throws IllegalStateException if serialization fails
     */
    public @NotNull String arrayToBase64(final @Nullable ItemStack[] items) {
        final byte[] serialized = serializeArray(items);
        return Base64Coder.encodeLines(serialized);
    }

    /**
     * Decodes a Base64 string produced by {@link #arrayToBase64(ItemStack[])} back into an array of
     * stacks.
     *
     * <p>Any decoding or deserialization problem surfaces as an {@link IllegalStateException}.</p>
     *
     * @param base64 Base64 encoded stack array
     * @return reconstructed array of item stacks
     * @throws IllegalStateException if decoding or deserialization fails
     */
    public @NotNull ItemStack[] arrayFromBase64(final @NotNull String base64) {
        try {
            final byte[] data = Base64Coder.decodeLines(base64);
            return deserializeArray(data);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to decode Base64 ItemStack array", e);
        }
    }

    /**
     * Determines whether the provided stack is effectively empty based on Bukkit semantics.
     *
     * <p>The stack is considered empty if it is {@code null}, contains {@link Material#AIR
     * Material.AIR}, or has a non-positive amount.</p>
     *
     * @param itemStack stack to inspect, may be {@code null}
     * @return {@code true} when the stack should be treated as empty
     */
    public boolean isEmpty(final @Nullable ItemStack itemStack) {
        return itemStack == null || 
               itemStack.getType() == Material.AIR || 
               itemStack.getAmount() <= 0;
    }

    /**
     * Creates a deep copy of the provided stack using the binary serializer for fidelity.
     *
     * <p>Returns {@code null} when the input is {@code null}. Serialization errors bubble up as the
     * standard {@link IllegalStateException} thrown by {@link #serialize(ItemStack)} and
     * {@link #deserialize(byte[])}.</p>
     *
     * @param itemStack stack to clone, may be {@code null}
     * @return deep copy of the provided stack, or {@code null}
     * @throws IllegalStateException if the stack cannot be serialized or deserialized
     */
    public @Nullable ItemStack deepClone(final @Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        
        final byte[] serialized = serialize(itemStack);
        return deserialize(serialized);
    }
}
