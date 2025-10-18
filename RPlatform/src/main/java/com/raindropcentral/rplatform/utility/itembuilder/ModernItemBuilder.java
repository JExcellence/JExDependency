package com.raindropcentral.rplatform.utility.itembuilder;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ModernItemBuilder extends AItemBuilder<ItemMeta, ModernItemBuilder> implements IUnifiedItemBuilder<ItemMeta, ModernItemBuilder> {

    public ModernItemBuilder(Material material) {
        super(material);
    }
    
    public ModernItemBuilder(final @NotNull ItemStack item) {
        super(item);
    }
    
}