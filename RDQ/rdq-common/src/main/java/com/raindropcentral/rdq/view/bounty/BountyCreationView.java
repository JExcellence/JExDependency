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
import me.devnatan.inventoryframework.context.SlotClickContext;
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

    /**
     * Creates a new bounty creation view bound to {@link BountyMainView} as the parent.
     */
    public BountyCreationView() {
        super(BountyMainView.class);
    }

    /**
     * Retrieves the number of rows displayed by the inventory.
     *
     * @return the amount of rows available in the view.
     */
    @Override
    protected int getSize() {
        return 5;
    }

    /**
     * Defines how frequently the view should refresh dynamic components.
     *
     * @return the refresh interval in ticks.
     */
    @Override
    protected int getUpdateSchedule() {
        return 20;
    }

    /**
     * Supplies placeholder values used when rendering the translated title.
     *
     * @param open the context describing the view opening sequence.
     * @return a map containing the selected target name placeholder.
     */
    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return Map.of(
                "target_name", this.target.get(open).map(OfflinePlayer::getName).orElse("not_defined")
        );
    }

    /**
     * Provides the translation key for InventoryFramework's internal caching.
     *
     * @return the unique key describing this view.
     */
    @Override
    protected String getKey() {
        return "bounty.creation";
    }

    /**
     * Renders the initial state of the creation interface including the target selector,
     * reward addition buttons, and confirmation control.
     *
     * @param render the render context used to populate slots.
     * @param player the player opening the view.
     */
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

    /**
     * Places and wires the target selection button allowing players to choose a bounty target.
     *
     * @param render the render context for the current view frame.
     * @param player the interacting player.
     */
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

    /**
     * Configures the slot responsible for adding reward items to the bounty.
     *
     * @param render the render context used to interact with slots.
     * @param player the player viewing the GUI.
     */
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

    /**
     * Sets up the currency adder button that becomes available once a target is selected.
     *
     * @param render the render context used for updating the slot.
     * @param player the player interacting with the interface.
     */
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

    /**
     * Registers the confirm button responsible for finalizing the bounty submission.
     *
     * @param render the render context responsible for slot handling.
     * @param player the player attempting to create the bounty.
     */
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

    /**
     * Handles the confirmation click by validating selections, notifying the player, and re-opening
     * context-sensitive views when required.
     *
     * @param clickContext the InventoryFramework slot click context.
     * @param player       the player confirming the bounty creation.
     */
    private void handleConfirm(
            final @NotNull SlotClickContext clickContext,
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

    /**
     * Returns any inserted items to the player when the view closes without confirmation.
     *
     * @param close the close context generated by InventoryFramework.
     */
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

    /**
     * Refunds the provided items to the player, dropping any excess on the ground if the inventory is full.
     *
     * @param player the player receiving the refunded items.
     * @param items  the collection of items to refund.
     */
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