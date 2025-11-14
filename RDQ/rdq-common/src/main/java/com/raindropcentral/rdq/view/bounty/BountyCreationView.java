package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import me.devnatan.inventoryframework.context.*;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

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

    private final State<RDQ> rdq = initialState("plugin");
    private final MutableState<Optional<OfflinePlayer>> target = mutableState(Optional.empty());
    private final MutableState<Set<RewardItem>> rewardItems = mutableState(new HashSet<>());
    private final MutableState<Map<String, Double>> rewardCurrencies = mutableState(new HashMap<>());
    private final MutableState<Optional<RBounty>> bounty = mutableState(Optional.empty());
    private final MutableState<Map<UUID, Map<Integer, ItemStack>>> insertedItems = mutableState(new HashMap<>());

    private boolean isReopening;

    private final State<ItemStack> targetSelectorButton = computedState(context -> {
        final Player player = context.getPlayer();
        final Optional<OfflinePlayer> targetPlayer = this.target.get(context);
        final String targetName = targetPlayer.isPresent() ? targetPlayer.map(OfflinePlayer::getName).orElse("not_defined") : "not_defined";

        return UnifiedBuilderFactory
                .safeHead()
                .setPlayerHead(targetPlayer.isPresent() ? targetPlayer.orElse(null) : null)
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
        final Optional<OfflinePlayer> targetPlayer = this.target.get(context);
        final boolean enabled = targetPlayer != null && targetPlayer.isPresent();
        final Set<RewardItem> items = this.rewardItems.get(context);

        return UnifiedBuilderFactory
                .item(enabled ? Material.CHEST : Material.BARRIER)
                .setName(
                        this.i18n("add_items." + (enabled ? "name" : "name_disabled"), player)
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("add_items." + (enabled ? "lore" : "lore_disabled"), player)
                                .with("count", items != null ? items.size() : 0)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> currencyAdderButton = computedState(context -> {
        final Player player = context.getPlayer();
        final Optional<OfflinePlayer> targetPlayer = this.target.get(context);
        final boolean enabled = targetPlayer != null && targetPlayer.isPresent();
        final Map<String, Double> currencies = this.rewardCurrencies.get(context);

        return UnifiedBuilderFactory
                .item(enabled ? Material.GOLD_INGOT : Material.BARRIER)
                .setName(
                        this.i18n("add_currency." + (enabled ? "name" : "name_disabled"), player)
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("add_currency." + (enabled ? "lore" : "lore_disabled"), player)
                                .with("count", currencies != null ? currencies.size() : 0)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    });

    private final State<ItemStack> confirmButton = computedState(context -> {
        final Player player = context.getPlayer();
        final Optional<OfflinePlayer> targetPlayer = this.target.get(context);
        final Set<RewardItem> items = this.rewardItems.get(context);
        final boolean canConfirm = targetPlayer != null && targetPlayer.isPresent() 
                && items != null && !items.isEmpty();

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

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        super.onOpen(open);
        
        // Initialize all states from initialData if present
        if (open.getInitialData() instanceof Map<?, ?> data) {
            // Initialize target
            if (data.containsKey("target")) {
                final Object rawTarget = data.get("target");
                if (rawTarget instanceof Optional<?>) {
                    @SuppressWarnings("unchecked")
                    final Optional<OfflinePlayer> cast = (Optional<OfflinePlayer>) rawTarget;
                    this.target.set(cast, open);
                } else if (rawTarget instanceof OfflinePlayer offlinePlayer) {
                    this.target.set(Optional.of(offlinePlayer), open);
                }
            }
            
            // Initialize rewardItems
            if (data.containsKey("rewardItems") && data.get("rewardItems") instanceof Set<?>) {
                @SuppressWarnings("unchecked")
                final Set<RewardItem> items = (Set<RewardItem>) data.get("rewardItems");
                this.rewardItems.set(items, open);
            }
            
            // Initialize rewardCurrencies
            if (data.containsKey("rewardCurrencies") && data.get("rewardCurrencies") instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                final Map<String, Double> currencies = (Map<String, Double>) data.get("rewardCurrencies");
                this.rewardCurrencies.set(currencies, open);
            }
            
            // Initialize bounty
            if (data.containsKey("bounty")) {
                final Object rawBounty = data.get("bounty");
                if (rawBounty instanceof Optional<?>) {
                    @SuppressWarnings("unchecked")
                    final Optional<RBounty> cast = (Optional<RBounty>) rawBounty;
                    this.bounty.set(cast, open);
                } else if (rawBounty instanceof RBounty rBounty) {
                    this.bounty.set(Optional.of(rBounty), open);
                }
            }
            
            // Initialize insertedItems
            if (data.containsKey("insertedItems") && data.get("insertedItems") instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                final Map<UUID, Map<Integer, ItemStack>> items = (Map<UUID, Map<Integer, ItemStack>>) data.get("insertedItems");
                this.insertedItems.set(items, open);
            }
        }
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
        final Optional<OfflinePlayer> targetPlayer = this.target.get(open);
        return Map.of(
                "target_name", targetPlayer.isPresent() ? targetPlayer.map(OfflinePlayer::getName).orElse("not_defined") : "not_defined"
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
                .displayIf(() -> {
                    final Optional<OfflinePlayer> targetPlayer = this.target.get(render);
                    return targetPlayer != null && targetPlayer.isPresent();
                })
                .onClick(context -> {
                    final Optional<OfflinePlayer> targetPlayer = this.target.get(context);
                    if (targetPlayer.isEmpty()) {
                        this.i18n("add_items.disabled", player)
                                .withPrefix()
                                .send();
                        return;
                    }

                    this.isReopening = true;

                    final BountyService service = BountyServiceProvider.getInstance();

                    service.getBountyByPlayer(targetPlayer.get().getUniqueId())
                            .thenAccept(bountyOpt -> {
                                Bukkit.getScheduler().runTask(rdq.get(context).getPlugin(), () -> {
                                    try {
                                        var initialData = new HashMap<>((Map<String, Object>) context.getInitialData());
                                        initialData.put("bounty", bountyOpt);

                                        context.openForPlayer(
                                                BountyRewardView.class,
                                                initialData
                                        );
                                    } catch (final Exception ex) {
                                        CentralLogger.getLogger(BountyCreationView.class).log(Level.SEVERE, "Failed to open bounty reward view", ex);
                                    }
                                });
                            }).exceptionally(throwable -> {
                                CentralLogger.getLogger(BountyCreationView.class).log(Level.WARNING, "Failed to load bounty", throwable);
                                Bukkit.getScheduler().runTask(rdq.get(context).getPlugin(), context::closeForPlayer);
                                return null;
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
                .displayIf(() -> {
                    final Optional<OfflinePlayer> targetPlayer = this.target.get(render);
                    return targetPlayer != null && targetPlayer.isPresent();
                });
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
                .displayIf(() -> {
                    final Optional<OfflinePlayer> targetPlayer = this.target.get(render);
                    final Set<RewardItem> items = this.rewardItems.get(render);
                    return targetPlayer != null && targetPlayer.isPresent() 
                            && items != null && !items.isEmpty();
                })
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
        final Optional<OfflinePlayer> targetPlayer = this.target.get(clickContext);
        final Set<RewardItem> items = this.rewardItems.get(clickContext);
        final Map<UUID, Map<Integer, ItemStack>> insertedItemsMap = this.insertedItems.get(clickContext);

        if (targetPlayer == null || targetPlayer.isEmpty()) {
            this.i18n("confirm.no_player_selected", player)
                    .withPrefix()
                    .send();
            return;
        }

        if (items == null || items.isEmpty()) {
            this.i18n("confirm.no_rewards_selected", player)
                    .withPrefix()
                    .send();
            return;
        }

        final BountyService service = BountyServiceProvider.getInstance();
        final OfflinePlayer target = targetPlayer.get();

        this.i18n("confirm.confirmation", player)
                .withPrefix()
                .with("target_name", target.getName())
                .send();

        service.getBountyByPlayer(target.getUniqueId())
                .thenAccept(existingBounty -> {
                    Bukkit.getScheduler().runTask(rdq.get(clickContext).getPlugin(), () -> {
                        try {
                            if (existingBounty.isPresent()) {
                                clickContext.openForPlayer(
                                        BountyPlayerInfoView.class,
                                        Map.of(
                                                "bounty", existingBounty.get(),
                                                "target", target
                                        )
                                );
                            } else {
                                this.i18n("confirm.success", player)
                                        .withPrefix()
                                        .with("target_name", target.getName())
                                        .send();

                                if (insertedItemsMap != null) {
                                    insertedItemsMap.remove(player.getUniqueId());
                                }
                                this.isReopening = false;
                            }
                        } catch (final Exception ex) {
                            CentralLogger.getLogger(BountyCreationView.class).log(Level.SEVERE, "Failed to handle bounty confirmation", ex);
                        }
                    });
                }).exceptionally(throwable -> {
                    CentralLogger.getLogger(BountyCreationView.class).log(Level.SEVERE, "Failed to check existing bounty", throwable);
                    return null;
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

        final Map<UUID, Map<Integer, ItemStack>> insertedItemsMap = this.insertedItems.get(close);
        if (insertedItemsMap != null && insertedItemsMap.containsKey(close.getPlayer().getUniqueId())) {
            this.refundInsertedItems(
                    close.getPlayer(),
                    insertedItemsMap.get(close.getPlayer().getUniqueId()).values()
            );
        }
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

    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Player player = target.getPlayer();
        final Object initialData = target.getInitialData();

        if (initialData instanceof Map<?, ?> data) {
            if (data.containsKey("target")) {
                final Object rawTarget = data.get("target");
                Optional<OfflinePlayer> newTarget = Optional.empty();

                if (rawTarget instanceof Optional<?>) {
                    @SuppressWarnings("unchecked")
                    final Optional<OfflinePlayer> cast = (Optional<OfflinePlayer>) rawTarget;
                    newTarget = cast;
                } else if (rawTarget instanceof OfflinePlayer offlinePlayer) {
                    newTarget = Optional.of(offlinePlayer);
                }

                if (newTarget.isPresent()) {
                    this.target.set(newTarget, target);
                }
            }
        }
    }
}
