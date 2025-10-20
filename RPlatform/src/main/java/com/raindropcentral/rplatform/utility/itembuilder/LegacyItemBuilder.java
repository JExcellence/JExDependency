package com.raindropcentral.rplatform.utility.itembuilder;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Legacy {@link ItemMeta} builder that targets Spigot/Bukkit APIs predating Adventure support.
 *
 * <p>Instances of this builder are typically obtained through
 * {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#item(Material)} when the
 * runtime environment is not Paper. It inherits the fluent operations from
 * {@link AItemBuilder} to keep chains consistent across server implementations.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings("deprecation")
public class LegacyItemBuilder extends AItemBuilder<ItemMeta, LegacyItemBuilder> implements IUnifiedItemBuilder<ItemMeta, LegacyItemBuilder> {

    /**
     * Creates a builder for a new {@link ItemStack} derived from the provided material.
     *
     * @param material material backing the legacy item
     */
    public LegacyItemBuilder(
        final @NotNull Material material
    ) {
        super(material);
    }

    /**
     * Wraps an existing {@link ItemStack} that already contains legacy metadata.
     *
     * @param item item stack to mutate
     */
    public LegacyItemBuilder(final @NotNull ItemStack item) {
        super(item);
    }

}