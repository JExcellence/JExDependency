package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * InventoryFramework view for creating a new bounty in the RaindropQuests system.
 * <p>
 * This view allows players to:
 * <ul>
 * <li>Select a target player for the bounty</li>
 * <li>Add item and currency rewards</li>
 * <li>Confirm and submit the bounty</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyCreationView extends BaseView {

    private static final int SLOT_TARGET_SELECTOR = 11;
    private static final int SLOT_ITEM_ADDER = 13;
    private static final int SLOT_CURRENCY_ADDER = 15;
    private static final int SLOT_CONFIRM = 31;

    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<Set<RewardItem>> rewardItems = initialState("rewardItems");
    private final MutableState<Map<String, Double>> rewardCurrencies = initialState("rewardCurrencies");
    private final State<Optional<RBounty>> bounty = initialState("bounty");
    private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");

    private boolean isReopening;

    private final State<ItemStack> targetSelectorButton = computedState(context -> {
        final Player player = context.getPlayer();
        final Optional<OfflinePlayer> targetPlayer = this.target.get(context);
        final String targetName = targetPlayer.map(OfflinePlayer::getName).orElse("");

        return UnifiedBuilderFactory
                .safeHead()
                .setPlayerHead(targetPlayer.orElse(null))
                .setName(
                        this.i18n("select_target.name", player)
                                .with("target_name", targetName)
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("select_target.lore", player)
                                .with("target_name", targetName)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> itemAdderButton = computedState(context -> {
        final Player player = context.getPlayer();
        final boolean enabled = this.target.get(context).isPresent();

        return UnifiedBuilderFactory
                .item(enabled ? Material.CHEST : Material.BARRIER)
                .setName(
                        this.i18n("add_items." + (enabled ? "name" : "name_disabled"), player)
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("add_items." + (enabled ? "lore" : "lore_disabled"), player)
                                .with("count", this.rewardItems.get(context).size())
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> currencyAdderButton = computedState(context -> {
        final Player player = context.getPlayer();
        final boolean enabled = this.target.get(context).isPresent();

        return UnifiedBuilderFactory
                .item(enabled ? Material.GOLD_INGOT : Material.BARRIER)
                .setName(
                        this.i18n("add_currency." + (enabled ? "name" : "name_disabled"), player)
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("add_currency." + (enabled ? "lore" : "lore_disabled"), player)
                                .with("count", this.rewardCurrencies.get(context).size())
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> confirmButton = computedState(context -> {
        final Player player = context.getPlayer();
        final boolean canConfirm = this.target.get(context).isPresent() && !this.rewardItems.get(context).isEmpty();

        return UnifiedBuilderFactory
                .item(canConfirm ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .setName(
                        this.i18n("confirm_bounty.name", player)
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("confirm_bounty.lore", player)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    public BountyCreationView() {
        super(BountyMainView.class);
    }

    @Override
    protected int getSize() {
        return 5;
    }

    @Override
    protected int getUpdateSchedule() {
        return 20;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return Map.of(
                "target_name", this.target.get(open).map(OfflinePlayer::getName).orElse("not_defined")
        );
    }

    @Override
    protected String getKey() {
        return "bounty.creation";
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        this.renderTargetSelector(render, player);
        this.renderItemAdder(render, player);
        this.renderCurrencyAdder(render, player);
        this.renderConfirmButton(render, player);
    }

    private void renderTargetSelector(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .slot(SLOT_TARGET_SELECTOR)
                .watch(this.targetSelectorButton)
                .renderWith(() -> this.targetSelectorButton.get(render))
                .onClick(context -> {
                    this.isReopening = true;
                    context.openForPlayer(
                            PaginatedPlayerView.class,
                            Map.of(
                                    "titleKey", "bounty.creation.select_target.title",
                                    "parentClazz", this.getClass(),
                                    "initialData", context.getInitialData()
                            )
                    );
                });
    }

    private void renderItemAdder(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .slot(SLOT_ITEM_ADDER)
                .watch(this.itemAdderButton)
                .renderWith(() -> this.itemAdderButton.get(render))
                .displayIf(() -> this.target.get(render).isPresent())
                .onClick(context -> {
                    if (this.target.get(context).isEmpty()) {
                        this.i18n("add_items.disabled", player)
                                .withPrefix()
                                .send();
                        return;
                    }

                    this.isReopening = true;

                    final BountyService service = BountyServiceProvider.getInstance();
                    final Optional<OfflinePlayer> targetPlayer = this.target.get(context);

                    if (targetPlayer.isEmpty()) {
                        return;
                    }

                    service.getBountyByPlayer(targetPlayer.get().getUniqueId())
                            .thenAccept(bountyOpt -> {
                                if (bountyOpt.isPresent()) {
                                    context.openForPlayer(
                                            BountyPlayerInfoView.class,
                                            Map.of(
                                                    "bounty", bountyOpt.get(),
                                                    "target", targetPlayer.get()
                                            )
                                    );
                                } else {
                                    context.openForPlayer(
                                            BountyRewardView.class,
                                            context.getInitialData()
                                    );
                                }
                            });
                });
    }

    private void renderCurrencyAdder(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .slot(SLOT_CURRENCY_ADDER)
                .watch(this.currencyAdderButton)
                .renderWith(() -> this.currencyAdderButton.get(render))
                .displayIf(() -> this.target.get(render).isPresent());
    }

    private void renderConfirmButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .slot(SLOT_CONFIRM)
                .watch(this.confirmButton)
                .renderWith(() -> this.confirmButton.get(render))
                .displayIf(() -> this.target.get(render).isPresent() && !this.rewardItems.get(render).isEmpty())
                .onClick(clickContext -> this.handleConfirm(clickContext, player))
                .closeOnClick();
    }

    private void handleConfirm(
            final @NotNull me.devnatan.inventoryframework.context.SlotClickContext clickContext,
            final @NotNull Player player
    ) {
        if (this.target.get(clickContext).isEmpty()) {
            this.i18n("confirm.no_player_selected", player)
                    .withPrefix()
                    .send();
            return;
        }

        if (this.rewardItems.get(clickContext).isEmpty()) {
            this.i18n("confirm.no_rewards_selected", player)
                    .withPrefix()
                    .send();
            return;
        }

        final BountyService service = BountyServiceProvider.getInstance();
        final Optional<OfflinePlayer> target = this.target.get(clickContext);

        if (target.isEmpty()) {
            this.i18n("confirm.error", player)
                    .withPrefix()
                    .with("target_name", "not_defined")
                    .send();
            return;
        }

        this.i18n("confirm.confirmation", player)
                .withPrefix()
                .with("target_name", target.get().getName())
                .send();

        service.getBountyByPlayer(target.get().getUniqueId())
                .thenAccept(existingBounty -> {
                    if (existingBounty.isPresent()) {
                        clickContext.openForPlayer(
                                BountyPlayerInfoView.class,
                                Map.of(
                                        "bounty", existingBounty.get(),
                                        "target", target.get()
                                )
                        );
                    } else {
                        this.i18n("confirm.success", player)
                                .withPrefix()
                                .with("target_name", target.get().getName())
                                .send();

                        this.insertedItems.get(clickContext).remove(player.getUniqueId());
                        this.isReopening = false;
                    }
                });
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        if (this.isReopening) {
            return;
        }

        this.refundInsertedItems(
                close.getPlayer(),
                this.insertedItems.get(close).containsKey(close.getPlayer().getUniqueId())
                        ? this.insertedItems.get(close).get(close.getPlayer().getUniqueId()).values()
                        : new ArrayList<>()
        );
    }

    private void refundInsertedItems(
            final @NotNull Player player,
            final @NotNull Collection<ItemStack> items
    ) {
        if (items.isEmpty()) {
            return;
        }

        player.getInventory()
                .addItem(items.toArray(new ItemStack[0]))
                .forEach((i, item) ->
                        player.getWorld().dropItem(
                                player.getLocation().clone().add(0, 0.5, 0),
                                item
                        )
                );

        this.i18n("left_overs", player)
                .withPrefix()
                .send();
    }
}