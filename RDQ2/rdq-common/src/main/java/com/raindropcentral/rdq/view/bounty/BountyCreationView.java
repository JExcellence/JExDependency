package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.dto.RewardItem;
import com.raindropcentral.rplatform.utility.map.Maps;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * View for creating new bounties on players.
 * Allows target selection, reward item/currency configuration, and bounty confirmation.
 * 
 * Requirements: 2.1, 15.1, 15.2, 15.6
 */
public class BountyCreationView extends BaseView {

    // Immutable state
    private final State<RDQ> rdq = initialState("plugin");
    
    // Mutable states for bounty creation
    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<Set<RewardItem>> rewardItems = initialState("reward_items");
    private final MutableState<Map<String, Double>> rewardCurrencies = initialState("reward_currencies");
    private final MutableState<Map<Integer, ItemStack>> insertedItems = initialState("inserted_items");
    
    // Flag to track if we're reopening (to prevent refund on navigation)
    private boolean isReopening;

    // Computed states that react to mutable state changes
    private final State<ItemStack> targetButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var targetOpt = this.target.get(ctx);
        var targetName = targetOpt.map(OfflinePlayer::getName).orElse("None");
        
        return UnifiedBuilderFactory.unifiedHead(targetOpt.orElse(null))
                .setDisplayName(this.i18n("select_target.name", player).build().component())
                .setLore(this.i18n("select_target.lore", player)
                        .with(Placeholder.of("target_name", targetName))
                        .build().splitLines())
                .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> itemButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var enabled = this.target.get(ctx).isPresent();
        var itemCount = this.rewardItems.get(ctx).size();
        
        return UnifiedBuilderFactory
                .item(enabled ? Material.CHEST : Material.BARRIER)
                .setName(this.i18n(enabled ? "select_items.name" : "select_items_disabled.name", player).build().component())
                .setLore(this.i18n(enabled ? "select_items.lore" : "select_items_disabled.lore", player)
                        .with(Placeholder.of("item_count", itemCount))
                        .build().splitLines())
                .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> currencyButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var enabled = this.target.get(ctx).isPresent();
        var currencyCount = this.rewardCurrencies.get(ctx).size();
        
        return UnifiedBuilderFactory
                .item(enabled ? Material.GOLD_INGOT : Material.BARRIER)
                .setName(this.i18n(enabled ? "select_currency.name" : "select_currency_disabled.name", player).build().component())
                .setLore(this.i18n(enabled ? "select_currency.lore" : "select_currency_disabled.lore", player)
                        .with(Placeholder.of("currency_count", currencyCount))
                        .build().splitLines())
                .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> confirmButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var hasTarget = this.target.get(ctx).isPresent();
        var hasRewards = !this.rewardItems.get(ctx).isEmpty() || !this.rewardCurrencies.get(ctx).isEmpty();
        var canConfirm = hasTarget && hasRewards;
        
        return UnifiedBuilderFactory
                .item(canConfirm ? Material.GREEN_DYE : Material.BARRIER)
                .setName(this.i18n(canConfirm ? "confirm.name" : "confirm_disabled.name", player).build().component())
                .setLore(this.i18n(canConfirm ? "confirm.lore" : "confirm_disabled.lore", player).build().splitLines())
                .addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    public BountyCreationView() {
        super(BountyMainView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_creation_ui";
    }

    @Override
    protected int getSize() {
        return 5;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
                "XXXXXXXXX",
                "XXtXiXcXX",
                "XXXXXXXXX",
                "XXXXXXXXX",
                "XXXXfXXXX"
        };
    }

    @Override
    protected int getUpdateSchedule() {
        return 20;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        return Map.of(
                "target_name", this.target.get(open).map(OfflinePlayer::getName).orElse("None")
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderTargetButton(render, player);
        renderItemButton(render, player);
        renderCurrencyButton(render, player);
        renderConfirmButton(render, player);
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
     * Renders the target selection button.
     * Requirements: 2.2, 2.3, 3.1, 3.3, 3.4, 3.5
     */
    private void renderTargetButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('t')
                .watch(this.targetButton)
                .renderWith(() -> this.targetButton.get(render))
                .onClick(ctx -> {
                    this.isReopening = true;
                    // Open player selection view
                    var rdq = this.rdq.get(ctx);
                    rdq.getViewFrame().open(
                        com.raindropcentral.rplatform.view.PaginatedPlayerView.class,
                        player,
                        com.raindropcentral.rplatform.utility.map.Maps.merge(ctx.getInitialData())
                            .with(Map.of(
                                "callback_view", BountyCreationView.class,
                                "callback_key", "target",
                                "title_key", "bounty.select_target.title",
                                "filter_self", true
                            ))
                            .immutable()
                    );
                });
    }

    /**
     * Renders the item reward button (disabled without target).
     * Requirements: 2.2, 2.3, 2.4, 4.1, 4.3, 4.5
     */
    private void renderItemButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('i')
                .watch(this.itemButton)
                .renderWith(() -> this.itemButton.get(render))
                .onClick(ctx -> {
                    if (this.target.get(ctx).isEmpty()) {
                        this.i18n("select_items_disabled.message", player)
                                .withPrefix()
                                .send();
                        return;
                    }
                    
                    this.isReopening = true;
                    ctx.openForPlayer(BountyRewardView.class, ctx.getInitialData());
                });
    }

    /**
     * Renders the currency reward button (disabled without target).
     * Requirements: 2.2, 2.3, 5.1, 5.3, 5.4, 5.5
     */
    private void renderCurrencyButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('c')
                .watch(this.currencyButton)
                .renderWith(() -> this.currencyButton.get(render))
                .onClick(ctx -> {
                    if (this.target.get(ctx).isEmpty()) {
                        this.i18n("select_currency_disabled.message", player)
                                .withPrefix()
                                .send();
                        return;
                    }
                    
                    this.isReopening = true;
                    // Open currency selection view
                    ctx.openForPlayer(BountyCurrencySelectionView.class, ctx.getInitialData());
                });
    }

    /**
     * Renders the confirm button (disabled without target and rewards).
     * Requirements: 2.5, 2.6
     */
    private void renderConfirmButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('f')
                .watch(this.confirmButton)
                .renderWith(() -> this.confirmButton.get(render))
                .onClick(ctx -> {
                    var targetOpt = this.target.get(ctx);
                    var rewards = this.rewardItems.get(ctx);
                    var currencies = this.rewardCurrencies.get(ctx);
                    var rdq = this.rdq.get(ctx);

                    // Validate target is selected
                    if (targetOpt.isEmpty()) {
                        this.i18n("confirm_no_player_selected", player)
                                .withPrefix()
                                .send();
                        return;
                    }

                    var targetPlayer = targetOpt.get();

                    // Prevent self-targeting
                    if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
                        this.i18n("confirm_self_target", player)
                                .withPrefix()
                                .send();
                        return;
                    }

                    // Validate rewards are selected
                    if (rewards.isEmpty() && currencies.isEmpty()) {
                        this.i18n("confirm_no_rewards_selected", player)
                                .withPrefix()
                                .send();
                        return;
                    }

                    // Convert rewards to DTO RewardItems for BountyService
                    Set<com.raindropcentral.rdq.bounty.dto.RewardItem> rewardItemDtos = rewards.stream()
                            .map(dtoItem -> new com.raindropcentral.rdq.bounty.dto.RewardItem(
                                    dtoItem.item(),
                                    dtoItem.amount(),
                                    dtoItem.estimatedValue()
                            ))
                            .collect(java.util.stream.Collectors.toSet());

                    // Create bounty creation request
                    var creationRequest = new com.raindropcentral.rdq.bounty.dto.BountyCreationRequest(
                            targetPlayer.getUniqueId(),
                            player.getUniqueId(),
                            rewardItemDtos,
                            currencies,
                            java.util.Optional.empty() // No custom expiration
                    );

                    // Create bounty via BountyService
                    try {
                        rdq.getBountyService().createBounty(creationRequest).thenAccept(bounty -> {
                            // Clear temporary storage on success (on main thread)
                            rdq.getPlatform().getScheduler().runSync(() -> {
                                // Clear the inserted items map
                                this.insertedItems.get(ctx).clear();
                                this.rewardItems.get(ctx).clear();
                                this.rewardCurrencies.get(ctx).clear();
                                
                                // Display success message
                                this.i18n("confirm.success", player)
                                        .withPrefix()
                                        .with(Placeholder.of("target_name", targetPlayer.getName()))
                                        .send();
                                
                                this.isReopening = false;
                                ctx.closeForPlayer();
                            });
                        }).exceptionally(throwable -> {
                            rdq.getPlatform().getScheduler().runSync(() -> {
                                this.i18n("confirm.error", player)
                                        .withPrefix()
                                        .with(Placeholder.of("error", throwable.getMessage()))
                                        .send();
                            });
                            return null;
                        });
                    } catch (com.raindropcentral.rdq.bounty.exception.BountyException e) {
                        this.i18n("confirm.error", player)
                                .withPrefix()
                                .with(Placeholder.of("error", e.getMessage()))
                                .send();
                    }
                })
                .closeOnClick();
    }

    /**
     * Called when resuming from a child view.
     * Resets the reopening flag to allow proper close handling.
     */
    @Override
    public void onResume(@NotNull Context origin, @NotNull Context target) {
        super.onResume(origin, target);
        this.isReopening = false;
    }

    /**
     * Handles view close by refunding inserted items if not navigating to another view.
     * Requirements: 2.7, 2.8
     */
    @Override
    public void onClose(@NotNull CloseContext closeCtx) {
        // Don't refund if we're just navigating to another view
        if (this.isReopening) {
            return;
        }

        // Refund all inserted items
        var items = this.insertedItems.get(closeCtx);
        if (items != null && !items.isEmpty()) {
            refundItems(closeCtx.getPlayer(), new ArrayList<>(items.values()));
        }
    }

    /**
     * Refunds items to player inventory, dropping excess if inventory is full.
     * Requirements: 2.7, 2.8
     */
    private void refundItems(@NotNull Player player, @NotNull List<ItemStack> itemsToRefund) {
        if (itemsToRefund.isEmpty()) {
            return;
        }

        // Try to add items to inventory
        var leftOvers = player.getInventory().addItem(itemsToRefund.toArray(new ItemStack[0]));

        // Drop excess items at player location
        if (!leftOvers.isEmpty()) {
            leftOvers.forEach((index, item) -> {
                player.getWorld().dropItem(
                        player.getLocation().clone().add(0, 0.5, 0),
                        item
                );
            });

            // Display refund message
            this.i18n("refund.with_drops", player)
                    .withPrefix()
                    .with(Placeholder.of("dropped_count", leftOvers.size()))
                    .send();
        } else {
            // Display refund message
            this.i18n("refund.success", player)
                    .withPrefix()
                    .send();
        }
    }
}
