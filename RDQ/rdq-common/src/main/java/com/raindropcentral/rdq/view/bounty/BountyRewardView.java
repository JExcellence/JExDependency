package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * View for managing bounty rewards with interactive item insertion.
 * <p>
 * This view allows players to add items to a bounty by placing them in designated slots.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyRewardView extends APaginatedView<RewardItem> {

    private static final Logger LOGGER = CentralLogger.getLogger(BountyRewardView.class);

    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<Set<RewardItem>> rewardItems = initialState("rewardItems");
    private final MutableState<Map<String, Double>> rewardCurrencies = initialState("rewardCurrencies");
    private final State<Optional<RBounty>> bounty = initialState("bounty");
    private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");

    private boolean isReturning;

    /**
     * Creates a new bounty reward view and wires the navigation target to the creation view.
     */
    public BountyRewardView() {
        super(BountyCreationView.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getKey() {
        return "bounty.reward";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "    t    ",
                "< OOOOO >",
                "         ",
                " xxxxxxx ",
                "         ",
                "b       c"
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CompletableFuture<List<RewardItem>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        try {
            if (this.bounty.get(context).isEmpty() || this.bounty.get(context).get().getRewardItems().isEmpty()) {
                final RewardItem pseudoItem = new RewardItem(
                        this.buildPane(
                                Material.GRAY_STAINED_GLASS_PANE,
                                context.getPlayer(),
                                "pseudo.name",
                                "pseudo.lore"
                        )
                );

                return CompletableFuture.completedFuture(
                        List.of(pseudoItem, pseudoItem, pseudoItem, pseudoItem, pseudoItem)
                );
            }

            return CompletableFuture.completedFuture(
                    this.bounty.get(context).get().getRewardItems().stream().toList()
            );
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to load bounty rewards for pagination", ex);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RewardItem rewardItem
    ) {
        if (this.bounty.get(context).isEmpty()) {
            builder
                    .renderWith(() -> UnifiedBuilderFactory.item(rewardItem.getItem()).build())
                    .updateOnStateChange(this.bounty);
            return;
        }

        this.splitToMaxStacks(rewardItem).forEach(item ->
                builder.renderWith(() ->
                        UnifiedBuilderFactory
                                .item(item.clone())
                                .setName(item.clone().displayName())
                                .setLore(
                                        this.i18n("reward_item.lore", context.getPlayer())
                                                .withAll(
                                                        Map.of(
                                                                "contributor_name",
                                                                Bukkit.getOfflinePlayer(rewardItem.getContributorUniqueId()).getName()
                                                        )
                                                )
                                                .build()
                                                .splitLines()
                                )
                                .addLoreLines(
                                        item.lore() == null ? new ArrayList<>() : Objects.requireNonNull(item.lore())
                                )
                                .build()
                ).updateOnStateChange(this.bounty)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final OfflinePlayer target = this.target.get(render).orElse(null);

        this.renderInputSlots(render, player);
        this.renderTargetHead(render, player, target);
        this.renderConfirmButton(render, player);
    }

    /**
     * Renders the interactive slots where players can insert reward items and
     * restores previously added items for the current session.
     *
     * @param render the render context managing the slot state
     * @param player the player viewing the interface
     */
    private void renderInputSlots(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .layoutSlot(
                        'x',
                        buildPane(
                                Material.GREEN_STAINED_GLASS_PANE,
                                player,
                                "input_slot.name",
                                "input_slot.lore"
                        )
                )
                .onClick(this::handleSlotClick);

        try {
            final Map<UUID, Map<Integer, ItemStack>> itemsMap = this.insertedItems.get(render);
            if (itemsMap != null && itemsMap.containsKey(player.getUniqueId())
                    && !itemsMap.get(player.getUniqueId()).isEmpty()) {
                itemsMap.get(player.getUniqueId())
                        .forEach((slot, item) ->
                                render.slot(slot, item).onClick(this::handleSlotClick)
                        );
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to render inserted items for player " + player.getName(), ex);
        }
    }

    /**
     * Shows the targeted player head in the top row, exposing name and lore translations.
     *
     * @param render the render context providing slot access
     * @param player the viewer opening the bounty reward view
     * @param target the targeted player for the bounty, may be {@code null}
     */
    private void renderTargetHead(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final OfflinePlayer target
    ) {
        render.layoutSlot(
                't',
                UnifiedBuilderFactory
                        .head()
                        .setPlayerHead(target != null ? target.getPlayer() : null)
                        .setName(
                                this.i18n("target.name", player)
                                        .with("target_name", target != null ? target.getName() : "")
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("target.lore", player)
                                        .withAll(
                                                Map.of("target_name", target != null ? target.getName() : "")
                                        )
                                        .build()
                                        .splitLines()
                        )
                        .build()
        );
    }

    /**
     * Renders the confirmation button that validates inserted items and navigates back
     * to the bounty creation view with the accumulated rewards.
     *
     * @param render the render context managing the confirm slot
     * @param player the player confirming the inserted rewards
     */
    private void renderConfirmButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .layoutSlot('c', new Proceed().getHead(player))
                .updateOnStateChange(this.insertedItems)
                .onClick(clickContext -> {
                    try {
                        final Map<UUID, Map<Integer, ItemStack>> itemsMap = this.insertedItems.get(render);
                        final Map<Integer, ItemStack> playerSlots = itemsMap != null ? itemsMap.get(player.getUniqueId()) : null;

                        if (playerSlots != null && !playerSlots.isEmpty()) {
                            final Set<RewardItem> rewards = new HashSet<>();
                            for (final ItemStack stack : playerSlots.values()) {
                                rewards.add(new RewardItem(stack, player));
                            }

                            this.rewardItems.get(clickContext).addAll(rewards);
                            this.isReturning = true;

                            clickContext.openForPlayer(
                                    BountyCreationView.class,
                                    clickContext.getInitialData()
                            );
                        } else {
                            this.i18n("no_new_items_inserted", player).withPrefix().send();
                        }
                    } catch (final Exception ex) {
                        LOGGER.log(Level.SEVERE, "Failed to confirm bounty rewards for player " + player.getName(), ex);
                        this.i18n("no_new_items_inserted", player).withPrefix().send();
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        if (click.isShiftClick() && click.getClickedContainer().isEntityContainer()) {
            this.handleShiftClick(click);
            return;
        }

        if (!click.isShiftClick() && click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(final @NotNull CloseContext close) {
        if (this.isReturning) {
            return;
        }

        try {
            final Map<UUID, Map<Integer, ItemStack>> itemsMap = this.insertedItems.get(close);
            if (itemsMap != null && itemsMap.containsKey(close.getPlayer().getUniqueId())) {
                refundInsertedItems(
                        close.getPlayer(),
                        itemsMap.get(close.getPlayer().getUniqueId()).values()
                );
                itemsMap.remove(close.getPlayer().getUniqueId());
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to refund items on close for player " + close.getPlayer().getName(), ex);
        }
    }

    /**
     * Handles clicks on designated input slots and manages placing or removing items
     * from the bounty reward buffer.
     *
     * @param clickContext the click context describing the player action
     */
    private void handleSlotClick(final @NotNull SlotClickContext clickContext) {
        try {
            final ItemStack cursorItem = clickContext.getClickOrigin().getCursor();
            final int clickedSlot = clickContext.getClickedSlot();
            final ItemStack currentSlotItem = clickContext.getClickOrigin().getCurrentItem();

            final boolean isSlotEmptyOrGreenPane = currentSlotItem == null
                    || currentSlotItem.getType() == Material.AIR
                    || currentSlotItem.getType() == Material.GREEN_STAINED_GLASS_PANE;

            final Map<UUID, Map<Integer, ItemStack>> itemsMap = this.insertedItems.get(clickContext);
            if (itemsMap == null) {
                LOGGER.log(Level.WARNING, "Inserted items map is null for player " + clickContext.getPlayer().getName());
                return;
            }

            final Map<Integer, ItemStack> playerSlots = itemsMap.computeIfAbsent(
                    clickContext.getPlayer().getUniqueId(),
                    k -> new HashMap<>()
            );

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
                    final ItemStack removed = playerSlots.remove(clickedSlot);
                    if (removed != null) {
                        refundInsertedItems(clickContext.getPlayer(), List.of(removed));
                    }

                    clickContext.getClickedContainer().renderItem(
                            clickedSlot,
                            buildPane(
                                    Material.GREEN_STAINED_GLASS_PANE,
                                    clickContext.getPlayer(),
                                    "input_slot.name",
                                    "input_slot.lore"
                            )
                    );
                }
            }
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error handling slot click for player " + clickContext.getPlayer().getName(), ex);
        }
    }

    /**
     * Converts shift-click actions into automatic placement of items into the green pane slots.
     *
     * @param click the slot click context tied to the shift-click interaction
     */
    private void handleShiftClick(final @NotNull SlotClickContext click) {
        try {
            final Player player = click.getPlayer();
            final ItemStack clickedItem = click.getClickOrigin().getCurrentItem();

            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                final Inventory guiInv = player.getOpenInventory().getTopInventory();
                final int targetSlot = findFirstPaneSlot(
                        guiInv,
                        Set.of(Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE)
                );

                if (targetSlot != -1) {
                    player.getInventory().removeItem(clickedItem);
                    guiInv.setItem(targetSlot, clickedItem.clone());

                    final Map<UUID, Map<Integer, ItemStack>> itemsMap = this.insertedItems.get(click);
                    if (itemsMap != null) {
                        itemsMap.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                                .put(targetSlot, clickedItem.clone());
                    }

                    click.setCancelled(true);
                    return;
                }
            }

            click.setCancelled(true);
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error handling shift-click for player " + click.getPlayer().getName(), ex);
            click.setCancelled(true);
        }
    }

    /**
     * Finds the first slot in the provided inventory that matches any of the supplied pane types.
     *
     * @param inv       the inventory to search
     * @param paneTypes the material types that count as available pane slots
     * @return the first matching slot index or {@code -1} if none are available
     */
    private int findFirstPaneSlot(
            final @NotNull Inventory inv,
            final @NotNull Set<Material> paneTypes
    ) {
        for (int i = 0; i < inv.getSize(); i++) {
            final ItemStack slotItem = inv.getItem(i);
            if (slotItem != null && paneTypes.contains(slotItem.getType())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Builds a translated glass pane item used in the GUI layout for interaction hints.
     *
     * @param paneType the type of glass pane to create
     * @param player   the player for whom translations should be resolved
     * @param nameKey  the translation key for the pane display name
     * @param loreKey  the translation key for the pane lore lines
     * @return the constructed item stack ready for rendering
     */
    private ItemStack buildPane(
            final @NotNull Material paneType,
            final @NotNull Player player,
            final @NotNull String nameKey,
            final @NotNull String loreKey
    ) {
        return UnifiedBuilderFactory
                .item(paneType)
                .setName(
                        TranslationService.create(TranslationKey.of(this.getKey(), nameKey), player)
                                .build()
                                .component()
                )
                .setLore(
                        TranslationService.create(TranslationKey.of(this.getKey(), loreKey), player)
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Splits a reward item into the maximum possible stack sizes for presentation in the GUI.
     *
     * @param rewardItem the reward item to split
     * @return a list of item stacks representing the split reward
     */
    private List<ItemStack> splitToMaxStacks(final @NotNull RewardItem rewardItem) {
        final List<ItemStack> result = new ArrayList<>();
        final ItemStack base = rewardItem.getItem();
        int total = rewardItem.getAmount();
        final int maxStack = base.getMaxStackSize();

        while (total > 0) {
            final int stackAmount = Math.min(total, maxStack);
            final ItemStack stack = base.clone();
            stack.setAmount(stackAmount);
            result.add(stack);
            total -= stackAmount;
        }

        return result;
    }

    /**
     * Refunds inserted items back to the player's inventory, dropping leftovers nearby.
     *
     * @param player the player receiving the refunded items
     * @param items  the items that should be returned to the player
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