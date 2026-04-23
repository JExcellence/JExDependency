package de.jexcellence.quests.machine;

import de.jexcellence.quests.database.entity.Machine;
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
 * Serialises and deserialises {@link Machine}-owned {@link ItemStack}
 * grids to/from Base64 blobs so they can be stored in the
 * {@code jexquests_machine.storage_data} text column. Uses Bukkit's
 * native {@link BukkitObjectOutputStream} — no XSeries / reflection
 * required, NBT is preserved.
 *
 * <p>Grid shape is a flat {@code ItemStack[]} — the calling view
 * decides which indices are input vs. output vs. fuel slots based on
 * the {@link MachineType#properties()} map.
 */
public final class MachineStorageCodec {

    private MachineStorageCodec() {
    }

    /** Encodes the full grid. Empty slots persist as {@code null}. */
    public static @NotNull String encode(@Nullable ItemStack[] contents) {
        if (contents == null || contents.length == 0) return "";
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
            out.writeInt(contents.length);
            for (final ItemStack item : contents) {
                if (item == null || item.getType() == Material.AIR) {
                    out.writeObject(null);
                } else {
                    out.writeObject(item);
                }
            }
            out.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (final IOException ex) {
            throw new IllegalStateException("storage encode failed", ex);
        }
    }

    /**
     * Decodes a Base64 storage blob into an item grid of at least
     * {@code minSize} slots — shorter persisted grids are padded to the
     * expected size so config changes (add more slots) don't crash
     * the view.
     */
    public static @NotNull ItemStack[] decode(@Nullable String base64, int minSize) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[Math.max(0, minSize)];
        final byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64);
        } catch (final IllegalArgumentException ex) {
            throw new IllegalStateException("storage decode failed: invalid Base64", ex);
        }
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(raw);
             final BukkitObjectInputStream in = new BukkitObjectInputStream(bais)) {
            final int persistedSize = in.readInt();
            final int resultSize = Math.max(persistedSize, minSize);
            final ItemStack[] out = new ItemStack[resultSize];
            for (int i = 0; i < persistedSize; i++) {
                final Object obj = in.readObject();
                out[i] = obj instanceof ItemStack item ? item : null;
            }
            return out;
        } catch (final IOException | ClassNotFoundException ex) {
            throw new IllegalStateException("storage decode failed", ex);
        }
    }
}
