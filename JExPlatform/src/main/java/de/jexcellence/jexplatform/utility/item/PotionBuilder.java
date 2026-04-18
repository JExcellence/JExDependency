package de.jexcellence.jexplatform.utility.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Paper-first fluent builder for potion items with base type, custom effects,
 * and visual customization support.
 *
 * <pre>{@code
 * var potion = PotionBuilder.of(Material.SPLASH_POTION)
 *     .baseType(PotionType.HEALING)
 *     .addEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1))
 *     .color(Color.BLUE)
 *     .name(Component.text("Swift Splash"))
 *     .build();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class PotionBuilder {

    private final ItemStack item;
    private final PotionMeta meta;

    private PotionBuilder(@NotNull Material material) {
        this.item = new ItemStack(material);
        this.meta = (PotionMeta) item.getItemMeta();
    }

    /**
     * Creates a potion builder for the standard drinkable potion.
     *
     * @return a new builder instance
     */
    public static @NotNull PotionBuilder create() {
        return new PotionBuilder(Material.POTION);
    }

    /**
     * Creates a potion builder for the specified potion material
     * (e.g. {@code POTION}, {@code SPLASH_POTION}, {@code LINGERING_POTION}).
     *
     * @param material the potion material
     * @return a new builder instance
     */
    public static @NotNull PotionBuilder of(@NotNull Material material) {
        return new PotionBuilder(material);
    }

    // ── Potion Data ────────────────────────────────────────────────────────────

    /**
     * Sets the base potion type.
     *
     * @param type the Bukkit potion type
     * @return this builder
     */
    @SuppressWarnings("deprecation")
    public @NotNull PotionBuilder baseType(@NotNull PotionType type) {
        meta.setBasePotionType(type);
        return this;
    }

    /**
     * Adds a custom potion effect.
     *
     * @param effect    the potion effect to add
     * @param overwrite whether to replace an existing effect of the same type
     * @return this builder
     */
    public @NotNull PotionBuilder addEffect(@NotNull PotionEffect effect, boolean overwrite) {
        meta.addCustomEffect(effect, overwrite);
        return this;
    }

    /**
     * Adds a custom potion effect, overwriting any existing effect of the same type.
     *
     * @param effect the potion effect to add
     * @return this builder
     */
    public @NotNull PotionBuilder addEffect(@NotNull PotionEffect effect) {
        return addEffect(effect, true);
    }

    /**
     * Sets the potion color for visual display.
     *
     * @param color the RGB color
     * @return this builder
     */
    public @NotNull PotionBuilder color(@NotNull Color color) {
        meta.setColor(color);
        return this;
    }

    // ── Display ────────────────────────────────────────────────────────────────

    /**
     * Sets the display name of the potion item.
     *
     * @param name the display name component
     * @return this builder
     */
    public @NotNull PotionBuilder name(@NotNull Component name) {
        meta.displayName(name);
        return this;
    }

    /**
     * Sets the lore of the potion item.
     *
     * @param lore ordered lore lines
     * @return this builder
     */
    public @NotNull PotionBuilder lore(@NotNull List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    /**
     * Appends a lore line to the existing lore.
     *
     * @param line the lore component to append
     * @return this builder
     */
    public @NotNull PotionBuilder addLore(@NotNull Component line) {
        var current = currentLore();
        current.add(line);
        meta.lore(current);
        return this;
    }

    // ── Build ──────────────────────────────────────────────────────────────────

    /**
     * Commits metadata changes and returns the built potion item.
     *
     * @return the fully configured potion item stack
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
