package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.BountyRequest;
import com.raindropcentral.rdq.bounty.RewardItem;
import com.raindropcentral.rdq.bounty.config.BountyConfig;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * View for creating new bounties on players.
 * Uses computed states for reactive UI updates following RDQ2 patterns.
 */
public final class BountyCreationView extends BaseView {

    private static final Logger LOGGER = Logger.getLogger(BountyCreationView.class.getName());

    // Immutable state
    private final State<RDQCore> rdqCore = initialState("rdqCore");

    // Mutable states for bounty creation - initial data must be passed when opening the view
    // Note: initialState() creates states backed by initial data - they can only be modified
    // by mutating the underlying objects (e.g., Map.put(), Set.add()), not by calling .set()
    private final MutableState<Optional<OfflinePlayer>> selectedTarget = initialState("target");
    private final MutableState<Set<RewardItem>> rewardItems = initialState("reward_items");
    private final MutableState<Map<Integer, ItemStack>> insertedItems = initialState("inserted_items");
    // Use a Map to store amount/currency so we can modify them in place
    private final MutableState<Map<String, Object>> bountySettings = initialState("bounty_settings");

    // Flag to track if we're reopening (to prevent refund on navigation)
    private boolean isReopening;

    // Computed state for target button
    private final State<ItemStack> targetButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var targetOpt = this.selectedTarget.get(ctx);
        var targetName = targetOpt != null && targetOpt.isPresent() 
            ? targetOpt.get().getName() 
            : "None";

        return UnifiedBuilderFactory.unifiedHead(targetOpt != null ? targetOpt.orElse(null) : null)
            .setDisplayName(this.i18n("select_target.name", player).build().component())
            .setLore(this.i18n("select_target.lore", player)
                .with(Placeholder.of("target_name", targetName))
                .build().splitLines())
            .build();
    });

    // Computed state for amount display
    private final State<ItemStack> amountButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var settings = this.bountySettings.get(ctx);
        var amount = settings != null ? (BigDecimal) settings.get("amount") : BigDecimal.ZERO;
        var currency = settings != null ? (String) settings.get("currency") : "coins";
        var amountStr = amount != null ? amount.toPlainString() : "0";
        var currencyStr = currency != null ? currency : "coins";

        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
            .setName(this.i18n("amount_display.name", player)
                .with(Placeholder.of("amount", amountStr))
                .with(Placeholder.of("currency", currencyStr))
                .build().component())
            .setLore(this.i18n("amount_display.lore", player).build().splitLines())
            .build();
    });

    // Computed state for item reward button
    private final State<ItemStack> itemRewardButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var targetOpt = this.selectedTarget.get(ctx);
        var enabled = targetOpt != null && targetOpt.isPresent();
        var rewards = this.rewardItems.get(ctx);
        var rewardCount = rewards != null ? rewards.size() : 0;

        return UnifiedBuilderFactory.item(enabled ? Material.CHEST : Material.BARRIER)
            .setName(this.i18n(enabled ? "item_rewards.name" : "item_rewards_disabled.name", player).build().component())
            .setLore(this.i18n(enabled ? "item_rewards.lore" : "item_rewards_disabled.lore", player)
                .with(Placeholder.of("reward_count", rewardCount))
                .build().splitLines())
            .build();
    });

    // Computed state for confirm button
    private final State<ItemStack> confirmButton = computedState(ctx -> {
        var player = ctx.getPlayer();
        var targetOpt = this.selectedTarget.get(ctx);
        var settings = this.bountySettings.get(ctx);
        var amount = settings != null ? (BigDecimal) settings.get("amount") : BigDecimal.ZERO;
        var hasTarget = targetOpt != null && targetOpt.isPresent();
        var hasAmount = amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
        var canConfirm = hasTarget && hasAmount;

        return UnifiedBuilderFactory.item(canConfirm ? Material.EMERALD_BLOCK : Material.BARRIER)
            .setName(this.i18n(canConfirm ? "confirm.name" : "confirm_disabled.name", player).build().component())
            .setLore(this.i18n(canConfirm ? "confirm.lore" : "confirm_disabled.lore", player).build().splitLines())
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
        return new String[]{
            "XXXXXXXXX",
            "XXtXaXiXX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "XXXXcXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var rdq = this.rdqCore.get(render);
        var config = rdq != null ? rdq.getBountyConfig() : null;

        // Render all buttons using computed states
        // Initial data is passed when opening the view via createInitialData()
        renderTargetButton(render, player, config);
        renderAmountButton(render, player, config);
        renderItemRewardButton(render, player);
        renderConfirmButton(render, player, config);
    }

    // No longer needed - initial data is passed when opening the view via createInitialData()

    /**
     * Renders the target selection button using computed state.
     */
    private void renderTargetButton(@NotNull RenderContext render, @NotNull Player player, BountyConfig config) {
        render.layoutSlot('t')
            .watch(this.targetButton)
            .renderWith(() -> this.targetButton.get(render))
            .onClick(ctx -> {
                this.isReopening = true;
                
                // Open PaginatedPlayerView for target selection using ctx.openForPlayer
                // The PaginatedPlayerView will call back() with the selected target
                ctx.openForPlayer(
                    com.raindropcentral.rplatform.view.PaginatedPlayerView.class,
                    ctx.getInitialData()
                );
            });
    }

    /**
     * Renders the amount selection button using computed state.
     */
    private void renderAmountButton(@NotNull RenderContext render, @NotNull Player player, BountyConfig config) {
        render.layoutSlot('a')
            .watch(this.amountButton)
            .renderWith(() -> this.amountButton.get(render))
            .onClick(ctx -> {
                if (config == null) return;

                var settings = this.bountySettings.get(ctx);
                if (settings == null) return;

                // Cycle through preset amounts
                var currentAmount = (BigDecimal) settings.get("amount");
                if (currentAmount == null) currentAmount = config.minAmount();
                
                var amounts = new BigDecimal[]{
                    config.minAmount(),
                    config.minAmount().multiply(BigDecimal.valueOf(5)),
                    config.minAmount().multiply(BigDecimal.valueOf(10)),
                    config.minAmount().multiply(BigDecimal.valueOf(100))
                };

                // Find next amount in cycle
                int currentIndex = -1;
                for (int i = 0; i < amounts.length; i++) {
                    if (amounts[i].equals(currentAmount)) {
                        currentIndex = i;
                        break;
                    }
                }

                int nextIndex = (currentIndex + 1) % amounts.length;
                var nextAmount = amounts[nextIndex].min(config.maxAmount());

                // Modify the map in place (this works because the map is mutable)
                settings.put("amount", nextAmount);
                ctx.update();

                var currency = (String) settings.get("currency");
                this.i18n("amount_changed", player)
                    .withPrefix()
                    .with(Placeholder.of("amount", nextAmount.toPlainString()))
                    .with(Placeholder.of("currency", currency != null ? currency : "coins"))
                    .send();
            });
    }

    /**
     * Renders the item reward button using computed state.
     */
    private void renderItemRewardButton(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('i')
            .watch(this.itemRewardButton)
            .renderWith(() -> this.itemRewardButton.get(render))
            .onClick(ctx -> {
                var targetOpt = this.selectedTarget.get(ctx);
                if (targetOpt == null || targetOpt.isEmpty()) {
                    this.i18n("select_target_first", player).withPrefix().send();
                    return;
                }

                this.isReopening = true;
                ctx.openForPlayer(BountyRewardView.class, ctx.getInitialData());
            });
    }

    /**
     * Renders the confirm button using computed state.
     */
    private void renderConfirmButton(@NotNull RenderContext render, @NotNull Player player, BountyConfig config) {
        render.layoutSlot('c')
            .watch(this.confirmButton)
            .renderWith(() -> this.confirmButton.get(render))
            .onClick(ctx -> {
                var rdq = this.rdqCore.get(ctx);
                if (rdq == null) return;

                var targetOpt = this.selectedTarget.get(ctx);
                var settings = this.bountySettings.get(ctx);
                var amount = settings != null ? (BigDecimal) settings.get("amount") : null;
                var currency = settings != null ? (String) settings.get("currency") : "coins";

                // Validate target
                if (targetOpt == null || targetOpt.isEmpty()) {
                    this.i18n("confirm_no_target", player).withPrefix().send();
                    return;
                }

                // Validate amount
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    this.i18n("confirm_no_amount", player).withPrefix().send();
                    return;
                }

                var target = targetOpt.get();

                // Prevent self-targeting if not allowed
                if (config != null && !config.selfTargetAllowed() && target.getUniqueId().equals(player.getUniqueId())) {
                    this.i18n("self_target_not_allowed", player).withPrefix().send();
                    return;
                }

                // Create bounty
                var request = new BountyRequest(
                    player.getUniqueId(),
                    target.getUniqueId(),
                    amount,
                    currency != null ? currency : "coins"
                );

                rdq.getBountyService().createBounty(request)
                    .thenAccept(bounty -> {
                        rdq.getPlatform().getScheduler().runSync(() -> {
                            // Clear state
                            this.insertedItems.get(ctx).clear();
                            this.rewardItems.get(ctx).clear();

                            this.i18n("creation_success", player)
                                .withPrefix()
                                .with(Placeholder.of("amount", amount.toPlainString()))
                                .with(Placeholder.of("currency", currency))
                                .with(Placeholder.of("target_name", target.getName()))
                                .send();

                            this.isReopening = false;
                            ctx.closeForPlayer();
                        });
                    })
                    .exceptionally(ex -> {
                        rdq.getPlatform().getScheduler().runSync(() -> {
                            // Handle RDQException with proper error formatting
                            var cause = ex.getCause();
                            if (cause instanceof com.raindropcentral.rdq.shared.error.RDQException rdqEx) {
                                var error = rdqEx.getError();
                                var key = com.raindropcentral.rdq.shared.error.ErrorHandler.getTranslationKey(error);
                                var placeholders = com.raindropcentral.rdq.shared.error.ErrorHandler.getPlaceholders(error);
                                
                                var builder = this.i18n(key.key(), player).withPrefix();
                                for (var placeholder : placeholders) {
                                    builder = builder.with(placeholder);
                                }
                                builder.send();
                            } else {
                                this.i18n("creation_failed", player)
                                    .withPrefix()
                                    .with(Placeholder.of("error", ex.getMessage()))
                                    .send();
                            }
                        });
                        return null;
                    });
            });
    }

    @Override
    public void onResume(@NotNull Context origin, @NotNull Context target) {
        super.onResume(origin, target);
        this.isReopening = false;
    }

    @Override
    public void onClose(@NotNull CloseContext closeCtx) {
        if (this.isReopening) {
            return;
        }

        // Refund inserted items
        var items = this.insertedItems.get(closeCtx);
        if (items != null && !items.isEmpty()) {
            refundItems(closeCtx.getPlayer(), new ArrayList<>(items.values()));
        }
    }

    private void refundItems(@NotNull Player player, @NotNull List<ItemStack> itemsToRefund) {
        if (itemsToRefund.isEmpty()) return;

        var leftOvers = player.getInventory().addItem(itemsToRefund.toArray(new ItemStack[0]));

        if (!leftOvers.isEmpty()) {
            leftOvers.forEach((index, item) -> {
                player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), item);
            });
            this.i18n("refund_with_drops", player)
                .withPrefix()
                .with(Placeholder.of("dropped_count", leftOvers.size()))
                .send();
        } else {
            this.i18n("refund_success", player).withPrefix().send();
        }
    }
}
