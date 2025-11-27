package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.RewardItem;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
    private final State<RDQCore> rdqCore = initialState("rdqCore");
    
    // Mutable states shared with BountyCreationView
    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<Set<RewardItem>> rewardItems = initialState("reward_items");
    private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("inserted_items");
    
    private boolean isReturning;

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
                "    t    ",
                "         ",
                " xxxxxxx ",
                "         ",
                "         ",
                "b       c"
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderTargetHead(render, player);
        renderInputSlots(render, player);
        renderBackButton(render, player);
        renderConfirmButton(render, player);
    }
    
    /**
     * Renders the target player head.
     */
    private void renderTargetHead(@NotNull RenderContext render, @NotNull Player player) {
        final OfflinePlayer target = this.target.get(render).orElse(null);
        
        if (target != null) {
            render.layoutSlot('t', UnifiedBuilderFactory
                    .item(Material.PLAYER_HEAD)
                    .setName(this.i18n("target.name", player)
                            .with(Placeholder.of("target_name", target.getName() == null ? "" : target.getName()))
                            .build().component())
                    .setLore(this.i18n("target.lore", player)
                            .with(Placeholder.of("target_name", target.getName() == null ? "" : target.getName()))
                            .build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }
    }
    
    /**
     * Renders green glass pane input slots.
     */
    private void renderInputSlots(@NotNull RenderContext render, @NotNull Player player) {
        final Map<Integer, ItemStack> playerSlots = this.insertedItems.get(render)
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        
        render.layoutSlot('x', buildPane(Material.GREEN_STAINED_GLASS_PANE, player,
                        "bounty_reward_ui.input_slot.name",
                        "bounty_reward_ui.input_slot.lore"))
                .onClick(this::handleSlotClick);
        
        // Render already inserted items
        if (!playerSlots.isEmpty()) {
            playerSlots.forEach((slot, item) -> 
                    render.slot(slot, item).onClick(this::handleSlotClick));
        }
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
                    this.isReturning = true;
                    ctx.openForPlayer(BountyCreationView.class, ctx.getInitialData());
                });
    }
    
    /**
     * Renders a confirm button to proceed with the rewards.
     */
    private void renderConfirmButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('c', UnifiedBuilderFactory
                .item(Material.LIME_CONCRETE)
                .setName(this.i18n("confirm_button.name", player).build().component())
                .setLore(this.i18n("confirm_button.lore", player).build().splitLines())
                .build())
                .updateOnStateChange(this.insertedItems)
                .onClick(clickContext -> {
                    Map<Integer, ItemStack> playerSlots = this.insertedItems.get(render)
                            .get(player.getUniqueId());
                    
                    if (playerSlots != null && !playerSlots.isEmpty()) {
                        Set<RewardItem> rewards = new HashSet<>();
                        for (ItemStack stack : playerSlots.values()) {
                            rewards.add(RewardItem.fromItemStack(stack, 1.0));
                        }
                        this.rewardItems.get(clickContext).addAll(rewards);
                        this.isReturning = true;
                        clickContext.openForPlayer(BountyCreationView.class, clickContext.getInitialData());
                    } else {
                        player.sendMessage(this.i18n("no_new_items_inserted", player).build().component());
                    }
                });
    }

    @Override
    public void onClick(@NotNull SlotClickContext click) {
        // Block double-clicks in player inventory (DOUBLE_CLICK click type)
        if (click.getClickedContainer().isEntityContainer() && 
            click.getClickOrigin().getClick().name().contains("DOUBLE")) {
            click.setCancelled(true);
            return;
        }
        
        if (click.isShiftClick() && click.getClickedContainer().isEntityContainer()) {
            this.handleShiftClick(click);
            return;
        }
        
        if (!click.isShiftClick() && click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(false);
        }
    }

    @Override
    public void onClose(@NotNull CloseContext close) {
        if (this.isReturning) {
            return;
        }
        
        if (this.insertedItems.get(close).containsKey(close.getPlayer().getUniqueId())) {
            refundInsertedItems(close.getPlayer(), 
                    this.insertedItems.get(close).get(close.getPlayer().getUniqueId()).values());
            this.insertedItems.get(close).remove(close.getPlayer().getUniqueId());
        }
        
        this.rdqCore.get(close).getViewFrame().open(BountyCreationView.class, 
                close.getPlayer(), close.getInitialData());
    }

    private void handleSlotClick(@NotNull SlotClickContext clickContext) {
        final ItemStack cursorItem = clickContext.getClickOrigin().getCursor();
        final int clickedSlot = clickContext.getClickedSlot();
        final ItemStack currentSlotItem = clickContext.getClickOrigin().getCurrentItem();
        
        boolean isSlotEmptyOrGreenPane = currentSlotItem == null 
                || currentSlotItem.getType() == Material.AIR 
                || currentSlotItem.getType() == Material.GREEN_STAINED_GLASS_PANE;
        
        Map<Integer, ItemStack> playerSlots = this.insertedItems.get(clickContext)
                .computeIfAbsent(clickContext.getPlayer().getUniqueId(), k -> new HashMap<>());
        
        if (clickContext.getClickedContainer().isEntityContainer() && clickContext.isShiftClick()) {
            clickContext.setCancelled(true);
            return;
        }
        
        if (clickContext.isLeftClick()) {
            if (isSlotEmptyOrGreenPane && cursorItem.getType() != Material.AIR) {
                clickContext.getClickOrigin().setCursor(null);
                playerSlots.put(clickedSlot, cursorItem.clone());
                clickContext.getClickedContainer().renderItem(clickedSlot, cursorItem);
            }
            return;
        }
        
        if (clickContext.isRightClick()) {
            if (!isSlotEmptyOrGreenPane && currentSlotItem.getType() != Material.AIR) {
                ItemStack removed = playerSlots.remove(clickedSlot);
                if (removed != null) {
                    refundInsertedItems(clickContext.getPlayer(), List.of(removed));
                }
                clickContext.getClickedContainer().renderItem(clickedSlot, 
                        buildPane(Material.GREEN_STAINED_GLASS_PANE, clickContext.getPlayer(),
                                "bounty_reward_ui.input_slot.name",
                                "bounty_reward_ui.input_slot.lore"));
            }
        }
    }

    private void handleShiftClick(@NotNull SlotClickContext click) {
        final Player player = click.getPlayer();
        final ItemStack clickedItem = click.getClickOrigin().getCurrentItem();
        
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            Inventory guiInv = player.getOpenInventory().getTopInventory();
            int targetSlot = findFirstPaneSlot(guiInv, Set.of(Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE));
            
            if (targetSlot != -1) {
                player.getInventory().removeItem(clickedItem);
                guiInv.setItem(targetSlot, clickedItem.clone());
                this.insertedItems.get(click)
                        .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                        .put(targetSlot, clickedItem.clone());
                click.setCancelled(true);
                return;
            }
        }
        click.setCancelled(true);
    }

    private int findFirstPaneSlot(@NotNull Inventory inv, @NotNull Set<Material> paneTypes) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slotItem = inv.getItem(i);
            if (slotItem != null && paneTypes.contains(slotItem.getType())) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack buildPane(@NotNull Material paneType, @NotNull Player player,
                                @NotNull String nameKey, @NotNull String loreKey) {
        return UnifiedBuilderFactory
                .item(paneType)
                .setName(this.i18n(nameKey, player).build().component())
                .setLore(this.i18n(loreKey, player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void refundInsertedItems(@NotNull Player player, @NotNull Collection<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        player.getInventory().addItem(items.toArray(new ItemStack[0]))
                .forEach((i, item) -> player.getWorld()
                        .dropItem(player.getLocation().clone().add(0, 0.5, 0), item));
        player.sendMessage(this.i18n("left_overs", player).build().component());
    }
}
