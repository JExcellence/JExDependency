package com.raindropcentral.rplatform.utility.itembuilder;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class LegacyItemBuilder extends AItemBuilder<ItemMeta, LegacyItemBuilder> implements IUnifiedItemBuilder<ItemMeta, LegacyItemBuilder> {
    
    public LegacyItemBuilder(
        final @NotNull Material material
    ) {
        super(material);
    }
    
    public LegacyItemBuilder(final @NotNull ItemStack item) {
        super(item);
    }
    
}