package de.jexcellence.jexplatform.utility.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Paper-first fluent builder for constructing {@link ItemStack} instances with
 * Adventure-native display names, lore, enchantments, and visual effects.
 *
 * <pre>{@code
 * var sword = ItemBuilder.of(Material.DIAMOND_SWORD)
 *     .name(Component.text("Excalibur"))
 *     .lore(List.of(Component.text("Legendary blade")))
 *     .enchant(Enchantment.SHARPNESS, 5)
 *     .glow(true)
 *     .build();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    private ItemBuilder(@NotNull ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
    }

    /**
     * Creates a builder for a new item of the given material.
     *
     * @param material the item material
     * @return a new builder instance
     */
    public static @NotNull ItemBuilder of(@NotNull Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    /**
     * Creates a builder wrapping an existing item stack.
     *
     * @param item the item stack to modify
     * @return a new builder instance
     */
    public static @NotNull ItemBuilder from(@NotNull ItemStack item) {
        return new ItemBuilder(item.clone());
    }

    // ── Display ────────────────────────────────────────────────────────────────

    /**
     * Sets the display name using an Adventure component.
     *
     * @param name the display name component
     * @return this builder
     */
    public @NotNull ItemBuilder name(@NotNull Component name) {
        meta.displayName(name);
        return this;
    }

    /**
     * Replaces the lore with the provided component lines.
     *
     * @param lore ordered lore lines
     * @return this builder
     */
    public @NotNull ItemBuilder lore(@NotNull List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    /**
     * Appends a single lore line to the existing lore.
     *
     * @param line the lore component to append
     * @return this builder
     */
    public @NotNull ItemBuilder addLore(@NotNull Component line) {
        var current = currentLore();
        current.add(line);
        meta.lore(current);
        return this;
    }

    /**
     * Appends multiple lore lines to the existing lore.
     *
     * @param lines the lore components to append
     * @return this builder
     */
    public @NotNull ItemBuilder addLore(@NotNull Component... lines) {
        var current = currentLore();
        current.addAll(Arrays.asList(lines));
        meta.lore(current);
        return this;
    }

    /**
     * Appends multiple lore lines from a list to the existing lore.
     *
     * @param lines the lore components to append
     * @return this builder
     */
    public @NotNull ItemBuilder addLoreLines(@NotNull List<Component> lines) {
        var current = currentLore();
        current.addAll(lines);
        meta.lore(current);
        return this;
    }

    // ── Enchantments & Flags ───────────────────────────────────────────────────

    /**
     * Adds an enchantment, allowing levels above the natural maximum.
     *
     * @param enchantment the enchantment to add
     * @param level       the enchantment level
     * @return this builder
     */
    public @NotNull ItemBuilder enchant(@NotNull Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    /**
     * Adds item flags to hide specific tooltip information.
     *
     * @param flags the flags to add
     * @return this builder
     */
    public @NotNull ItemBuilder flags(@NotNull ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    /**
     * Toggles the glowing enchantment effect by adding a hidden enchantment.
     *
     * @param glowing whether the item should glow
     * @return this builder
     */
    public @NotNull ItemBuilder glow(boolean glowing) {
        if (glowing) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(Enchantment.LURE);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    // ── Metadata ───────────────────────────────────────────────────────────────

    /**
     * Sets the stack amount.
     *
     * @param amount the stack size
     * @return this builder
     */
    public @NotNull ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * Sets the custom model data for resource-pack overrides.
     *
     * @param data the custom model data value
     * @return this builder
     */
    public @NotNull ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    // ── Build ──────────────────────────────────────────────────────────────────

    /**
     * Commits all metadata changes and returns the built item stack.
     *
     * @return the fully configured item stack
     */
    public @NotNull ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private @NotNull List<Component> currentLore() {
        @Nullable var existing = meta.lore();
        return existing != null ? new ArrayList<>(existing) : new ArrayList<>();
    }
}
