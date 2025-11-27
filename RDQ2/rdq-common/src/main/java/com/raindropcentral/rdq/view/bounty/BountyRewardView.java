package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * View for selecting reward items for a bounty.
 * Allows players to insert items into reward slots, remove them, and automatically merges similar items.
 * 
 * Requirements: 4.1, 4.2, 15.1, 15.2
 */
public class BountyRewardView extends BaseView {

    // Immutable state
    private final State<RDQ> rdq = initialState("plugin");
    
    // Mutable states shared with BountyCreationView
    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<Set<com.raindropcentral.rdq.bounty.dto.RewardItem>> rewardItems = initialState("reward_items");
    private final MutableState<Map<String, Double>> rewardCurrencies = initialState("reward_currencies");
    private final MutableState<Map<Integer, ItemStack>> insertedItems = initialState("inserted_items");
    
    // Slot range for item insertion (rows 2-4, slots 9-44)
    private static final int REWARD_SLOT_START = 9;
    private static final int REWARD_SLOT_END = 44;

    public BountyRewardView() {
        super(BountyCreationView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_reward_ui";
    }

    @Override
    protected int getSize() {
        return 6;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
                "XXXXiXXXX",
                "         ",
                "         ",
                "         ",
                "         ",
                "XXXXbXXXX"
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        // Don't auto-fill since we want empty slots for item insertion
        return false;
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderRewardSlots(render, player);
        renderInfoButton(render, player);
        renderBackButton(render, player);
    }
    
    /**
     * Renders decorative glass panes.
     */
    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(net.kyori.adventure.text.Component.empty())
                .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Renders empty slots for item insertion and handles item insertion/removal.
     * Requirements: 4.2, 4.3, 4.4
     */
    private void renderRewardSlots(@NotNull RenderContext render, @NotNull Player player) {
        var items = this.insertedItems.get(render);
        
        // Render all reward slots
        for (int slot = REWARD_SLOT_START; slot <= REWARD_SLOT_END; slot++) {
            final int currentSlot = slot;
            
            render.slot(slot)
                    .onRender(onRender -> {
                        // Display inserted item if present
                        if (items.containsKey(currentSlot)) {
                            onRender.setItem(items.get(currentSlot));
                        } else {
                            // Empty slot - allow insertion
                            onRender.setItem(null);
                        }
                    })
                    .onClick(ctx -> {
                        var clickPlayer = ctx.getPlayer();
                        var cursor = clickPlayer.getItemOnCursor();
                        var currentItem = ctx.getItem();
                        
                        // Handle item insertion
                        if (cursor != null && !cursor.getType().isAir()) {
                            // Player is placing an item
                            items.put(currentSlot, cursor.clone());
                            clickPlayer.setItemOnCursor(null);
                            
                            // Merge similar items and update reward items state
                            updateRewardItems(ctx);
                            ctx.update();
                        } 
                        // Handle item removal
                        else if (currentItem != null && !currentItem.getType().isAir()) {
                            // Player is removing an item
                            items.remove(currentSlot);
                            clickPlayer.setItemOnCursor(currentItem.clone());
                            
                            // Update reward items state
                            updateRewardItems(ctx);
                            ctx.update();
                        }
                    })
                    .cancelOnClick();
        }
    }

    /**
     * Renders an info button showing current reward count.
     */
    private void renderInfoButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('i')
                .watch(this.rewardItems)
                .onRender(onRender -> {
                    var rewardCount = this.rewardItems.get(onRender).size();
                    var infoItem = UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("info.name", player).build().component())
                            .setLore(this.i18n("info.lore", player)
                                    .with(Placeholder.of("reward_count", rewardCount))
                                    .build().splitLines())
                            .build();
                    onRender.setItem(infoItem);
                })
                .cancelOnClick();
    }
    
    /**
     * Renders a back button to return to bounty creation.
     */
    private void renderBackButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('b', UnifiedBuilderFactory
                .item(Material.ARROW)
                .setName(this.i18n("back_button.name", player).build().component())
                .setLore(this.i18n("back_button.lore", player).build().splitLines())
                .build())
                .onClick(ctx -> {
                    // Return to bounty creation view
                    ctx.openForPlayer(BountyCreationView.class, ctx.getInitialData());
                });
    }

    /**
     * Updates the reward items state by merging similar items.
     * Requirements: 4.6
     */
    private void updateRewardItems(@NotNull me.devnatan.inventoryframework.context.Context ctx) {
        var items = this.insertedItems.get(ctx);
        var rewards = this.rewardItems.get(ctx);
        
        // Clear current rewards
        rewards.clear();
        
        // Group items by material and merge similar items
        Map<Material, List<ItemStack>> groupedItems = new HashMap<>();
        for (ItemStack item : items.values()) {
            if (item != null && !item.getType().isAir()) {
                groupedItems.computeIfAbsent(item.getType(), k -> new ArrayList<>()).add(item);
            }
        }
        
        // Create RewardItem DTOs with merged amounts
        for (Map.Entry<Material, List<ItemStack>> entry : groupedItems.entrySet()) {
            List<ItemStack> itemList = entry.getValue();
            if (!itemList.isEmpty()) {
                // Use first item as template
                ItemStack template = itemList.get(0).clone();
                
                // Calculate total amount
                int totalAmount = itemList.stream()
                        .mapToInt(ItemStack::getAmount)
                        .sum();
                
                // Estimate value (placeholder - would integrate with economy system)
                double estimatedValue = totalAmount * 1.0;
                
                // Create RewardItem DTO
                var rewardItem = new com.raindropcentral.rdq.bounty.dto.RewardItem(
                        template,
                        totalAmount,
                        estimatedValue
                );
                
                rewards.add(rewardItem);
            }
        }
    }
}
