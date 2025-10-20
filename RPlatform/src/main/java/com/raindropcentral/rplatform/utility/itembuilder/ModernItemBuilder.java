package com.raindropcentral.rplatform.utility.itembuilder;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-first {@link ItemMeta} builder that leverages Adventure-aware APIs when available.
 *
 * <p>Created through {@link com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory#item(Material)}
 * when the platform reports Paper support, this builder reuses the shared fluent contract to keep
 * menu assembly consistent regardless of server branch.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ModernItemBuilder extends AItemBuilder<ItemMeta, ModernItemBuilder> implements IUnifiedItemBuilder<ItemMeta, ModernItemBuilder> {

    /**
     * Creates a builder for a new Paper-backed item stack.
     *
     * @param material material backing the modern item
     */
    public ModernItemBuilder(Material material) {
        super(material);
    }

    /**
     * Wraps an existing Paper item stack to mutate its metadata fluently.
     *
     * @param item item stack to mutate
     */
    public ModernItemBuilder(final @NotNull ItemStack item) {
        super(item);
    }

}