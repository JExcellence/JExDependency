package de.jexcellence.core.serialize;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
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
 * Round-trip Bukkit {@link ItemStack} arrays to/from Base64 strings using
 * Bukkit's native {@link BukkitObjectOutputStream}. Stateless and
 * thread-safe; safe to call from any thread (serialisation itself doesn't
 * touch live inventory state, so callers must still read the source array
 * on the main thread before calling {@link #encode}).
 */
public final class PlayerInventorySerializer {

    private PlayerInventorySerializer() {
    }

    /**
     * Encodes an {@link ItemStack} array into a Base64-wrapped Bukkit blob.
     * AIR slots are preserved as {@code null} and restored on decode.
     *
     * @param items array of item stacks (may contain {@code null} / AIR)
     * @return Base64 string, or an empty string when {@code items} has length 0
     */
    public static @NotNull String encode(@Nullable ItemStack[] items) {
        if (items == null || items.length == 0) return "";
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
            out.writeInt(items.length);
            for (final ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR) {
                    out.writeObject(null);
                } else {
                    out.writeObject(item);
                }
            }
            out.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (final IOException ex) {
            throw new IllegalStateException("encode failed", ex);
        }
    }

    /**
     * Decodes a Base64 blob produced by {@link #encode} back into an
     * {@link ItemStack} array. Returns an empty array when the blob is
     * {@code null} or empty.
     *
     * @param base64 Base64-encoded blob
     * @return decoded item stacks with {@code null} for AIR slots
     */
    public static @NotNull ItemStack[] decode(@Nullable String base64) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[0];
        final byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64);
        } catch (final IllegalArgumentException ex) {
            throw new IllegalStateException("decode failed: invalid Base64", ex);
        }
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(raw);
             final BukkitObjectInputStream in = new BukkitObjectInputStream(bais)) {
            final int size = in.readInt();
            final ItemStack[] out = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                final Object obj = in.readObject();
                out[i] = obj instanceof ItemStack item ? item : null;
            }
            return out;
        } catch (final IOException | ClassNotFoundException ex) {
            throw new IllegalStateException("decode failed", ex);
        }
    }

    /**
     * Forces Bukkit's configuration-serialization registry to load
     * {@link ItemStack}. Call once during plugin startup on classloaders
     * where static init of {@link ItemStack} may not have run yet.
     */
    public static void primeRegistry() {
        ConfigurationSerialization.registerClass(ItemStack.class);
    }
}
