package de.jexcellence.quests.machine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Factory + parser for machine {@link ItemStack}s. Identity is carried
 * in the item's {@link org.bukkit.persistence.PersistentDataContainer}
 * under the {@code jexquests:machine_type} key as a String holding the
 * {@link MachineType#identifier()}.
 *
 * <p>All methods are stateless and thread-safe. Item creation /
 * decoration must run on the main server thread (Paper restriction);
 * is safe anywhere.
 */
public final class MachineItem {

    public static final String KEY_NAME = "machine_type";

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MachineItem() {
    }

    /** Cached namespaced key for the plugin. */
    public static @NotNull NamespacedKey typeKey(@NotNull Plugin plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    /** Creates the in-world item stack for the given machine type. */
    public static @NotNull ItemStack createFor(@NotNull Plugin plugin, @NotNull MachineType type) {
        final Material icon = resolveMaterial(type.iconMaterial());
        final ItemStack stack = new ItemStack(icon);
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(MINI.deserialize(type.displayName()));
        meta.lore(List.of(
                MINI.deserialize("<gray>ID: <white>" + type.identifier() + "</white>"),
                MINI.deserialize("<gray>Category: <white>" + type.category() + "</white>"),
                MINI.deserialize("<gray>Size: <white>"
                        + type.width() + "x" + type.height() + "x" + type.depth() + "</white>"),
                Component.empty(),
                MINI.deserialize("<gradient:#a5f3fc:#06b6d4>▸ Place to deploy</gradient>")
        ));
        meta.getPersistentDataContainer().set(
                typeKey(plugin), PersistentDataType.STRING, type.identifier());
        stack.setItemMeta(meta);
        return stack;
    }

    /** Reads the machine-type identifier tagged on the stack, or {@code null}. */
    public static @Nullable String typeKeyOf(@NotNull Plugin plugin, @Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        final ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
                .get(typeKey(plugin), PersistentDataType.STRING);
    }

    private static @NotNull Material resolveMaterial(@NotNull String key) {
        try {
            return Material.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return Material.BEACON;
        }
    }
}
