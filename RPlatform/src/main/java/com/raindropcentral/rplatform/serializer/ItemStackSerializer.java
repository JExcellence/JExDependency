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

public class ItemStackSerializer {

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

    public @NotNull ItemStack deserialize(final byte[] data) {
        try (final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             final BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(byteStream)) {
            
            return (ItemStack) bukkitIn.readObject();
        } catch (final IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize ItemStack", e);
        }
    }

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

    public @NotNull String toBase64(final @Nullable ItemStack itemStack) {
        final byte[] serialized = serialize(itemStack);
        return Base64Coder.encodeLines(serialized);
    }

    public @NotNull ItemStack fromBase64(final @NotNull String base64) {
        try {
            final byte[] data = Base64Coder.decodeLines(base64);
            return deserialize(data);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to decode Base64 ItemStack", e);
        }
    }

    public @NotNull String arrayToBase64(final @Nullable ItemStack[] items) {
        final byte[] serialized = serializeArray(items);
        return Base64Coder.encodeLines(serialized);
    }

    public @NotNull ItemStack[] arrayFromBase64(final @NotNull String base64) {
        try {
            final byte[] data = Base64Coder.decodeLines(base64);
            return deserializeArray(data);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to decode Base64 ItemStack array", e);
        }
    }

    public boolean isEmpty(final @Nullable ItemStack itemStack) {
        return itemStack == null || 
               itemStack.getType() == Material.AIR || 
               itemStack.getAmount() <= 0;
    }

    public @Nullable ItemStack deepClone(final @Nullable ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        
        final byte[] serialized = serialize(itemStack);
        return deserialize(serialized);
    }
}
