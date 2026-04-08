package de.jexcellence.oneblock.view.storage;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.storage.StorageCategory;
import de.jexcellence.oneblock.manager.IslandStorageManager;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageItemDetailView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    private final State<Material> material = initialState("material");
    private final MutableState<Long> quantity = mutableState(0L);
    
    public StorageItemDetailView() {
    }
    
    @Override
    protected String getKey() {
        return "storage_item_detail_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X       X",
            "X   I   X",
            "X       X",
            "X W   T X",
            "XXXBXXXXX"
        };
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        Material mat = material.get(render);
        Long qty = quantity.get(render);
        
        Object initialQty = render.getInitialData() != null && render.getInitialData() instanceof Map ? 
            ((Map<?, ?>) render.getInitialData()).get("quantity") : null;
        if (initialQty instanceof Long && qty == 0L) {
            quantity.set((Long) initialQty, render);
            qty = (Long) initialQty;
        }
        
        if (mat == null || qty == null || qty == 0L) {
            renderErrorState(render, player);
            return;
        }
        
        renderItemDisplay(render, player, mat, qty);
        renderActionButtons(render, player, mat, qty);
        renderBorder(render, player);
    }
    
    private void renderItemDisplay(@NotNull RenderContext render, @NotNull Player player, @NotNull Material mat, @NotNull Long qty) {
        StorageCategory category = StorageCategory.categorize(mat);
        
        List<Component> lore = new ArrayList<>();
        lore.addAll(i18n("storage_item_detail.item_lore", player)
            .withPlaceholders(Map.of(
                "quantity", formatNumber(qty),
                "category", category != null ? category.getDisplayName() : "Unknown",
                "material", mat.name()
            ))
            .build().children());
        
        render.layoutSlot('I', UnifiedBuilderFactory
            .item(mat)
            .setName(i18n("storage_item_detail.item_name", player)
                .withPlaceholder("material", formatMaterialName(mat))
                .build().component())
            .setLore(lore)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        );
    }
    
    private void renderActionButtons(@NotNull RenderContext render, @NotNull Player player, @NotNull Material mat, @NotNull Long qty) {
        render.layoutSlot('W', UnifiedBuilderFactory
            .item(Material.CHEST)
            .setName(i18n("storage_item_detail.withdraw", player).build().component())
            .setLore(i18n("storage_item_detail.withdraw_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> handleWithdraw(ctx, mat, qty));
        
        render.layoutSlot('T', UnifiedBuilderFactory
            .item(Material.ENDER_CHEST)
            .setName(i18n("storage_item_detail.transfer", player).build().component())
            .setLore(i18n("storage_item_detail.transfer_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> handleTransfer(ctx, mat));
        
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.back", player).build().component())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.back());
    }
    
    private void renderBorder(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(" "))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        );
    }
    
    private void handleWithdraw(@NotNull SlotClickContext ctx, @NotNull Material mat, @NotNull Long qty) {
        Player player = ctx.getPlayer();
        OneblockIsland islandData = island.get(ctx);
        JExOneblock pluginInstance = plugin.get(ctx);
        
        if (islandData == null || pluginInstance == null) {
            i18n("storage_item_detail.error", player).includePrefix().build().sendMessage();
            return;
        }
        
        IslandStorageManager storageManager = pluginInstance.getIslandStorageManager();
        if (storageManager == null) {
            i18n("storage_item_detail.error", player).includePrefix().build().sendMessage();
            return;
        }
        
        int withdrawAmount = (int) Math.min(64, qty);
        var storage = storageManager.getOrCreateStorage(islandData.getId(), player.getUniqueId());
        
        if (storage.removeItem(mat, withdrawAmount)) {
            player.getInventory().addItem(new ItemStack(mat, withdrawAmount));
            
            i18n("storage_item_detail.withdraw_success", player)
                .withPlaceholders(Map.of(
                    "amount", String.valueOf(withdrawAmount),
                    "material", formatMaterialName(mat)
                ))
                .includePrefix().build().sendMessage();
            
            long newQty = qty - withdrawAmount;
            if (newQty <= 0) {
                ctx.back();
            } else {
                quantity.set(newQty, ctx);
                ctx.update();
            }
        } else {
            i18n("storage_item_detail.withdraw_failed", player).includePrefix().build().sendMessage();
        }
    }
    
    private void handleTransfer(@NotNull SlotClickContext ctx, @NotNull Material mat) {
        Player player = ctx.getPlayer();
        i18n("storage_item_detail.transfer_coming_soon", player).includePrefix().build().sendMessage();
    }
    
    private void renderErrorState(@NotNull RenderContext render, @NotNull Player player) {
        render.slot(22, UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("storage_item_detail.error_title", player).build().component())
            .setLore(i18n("storage_item_detail.error_lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        );
    }
    
    private @NotNull String formatMaterialName(@NotNull Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        StringBuilder formatted = new StringBuilder();
        
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                formatted.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    private @NotNull String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
}
