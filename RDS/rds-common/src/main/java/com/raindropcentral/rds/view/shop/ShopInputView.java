package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Renders the shop input inventory view.
 */
public class ShopInputView extends BaseView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Shop> shop = initialState("shop");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final MutableState<Map<Integer, ItemStack>> insertedItems = mutableState(new HashMap<>());
    private final MutableState<Boolean> saving = mutableState(false);

    /**
     * Creates a new shop input view.
     */
    public ShopInputView() {
        super(ShopOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "shop_input_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                " xxxxxxx ",
                " xxxxxxx ",
                " xxxxxxx ",
                " xxxxxxx ",
                "    c    "
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final Shop shop = this.getCurrentShop(context);
        return Map.of(
                "owner", getOwnerName(shop),
                "location", formatLocation(shop)
        );
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Shop shop = this.getCurrentShop(render);
        this.insertedItems.set(new HashMap<>(), render);
        this.saving.set(false, render);

        if (!shop.canSupply(player.getUniqueId())) {
            render.layoutSlot(
                    's',
                    this.createLockedItem(player)
            );
            return;
        }

        render.layoutSlot(
                's',
                createSummaryItem(player, shop)
        );

        render.layoutSlot(
                'x',
                buildPane(
                        Material.GREEN_STAINED_GLASS_PANE,
                        player,
                        "input_slot.name",
                        "input_slot.lore"
                )
        ).onClick(this::handleSlotClick);

        render.layoutSlot(
                'c',
                new Proceed().getHead(player)
        ).onClick(this::handleConfirmClick);
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        if (!this.getCurrentShop(click).canSupply(click.getPlayer().getUniqueId())) {
            click.setCancelled(true);
            return;
        }

        if (!click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(true);
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
    public void onClose(
            final @NotNull CloseContext close
    ) {
        if (Boolean.TRUE.equals(this.saving.get(close))) {
            return;
        }

        final Map<Integer, ItemStack> playerItems = this.getVisibleInsertedItems(close.getPlayer());
        if (playerItems != null && !playerItems.isEmpty()) {
            refundInsertedItems(close.getPlayer(), playerItems.values());
        }
        this.insertedItems.get(close).clear();
    }

    private void handleConfirmClick(
            final @NotNull SlotClickContext clickContext
    ) {
        if (!this.getCurrentShop(clickContext).canSupply(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.no_permission", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final Map<Integer, ItemStack> playerItems = this.getVisibleInsertedItems(clickContext.getPlayer());
        this.insertedItems.get(clickContext).clear();
        this.insertedItems.get(clickContext).putAll(playerItems);

        if (playerItems == null || playerItems.isEmpty()) {
            this.i18n("no_new_items_inserted", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        try {
            final Shop shop = this.getCurrentShop(clickContext);
            final List<AbstractItem> storedItems = new ArrayList<>(shop.getItems());

            for (ItemStack stack : new TreeMap<>(playerItems).values()) {
                final ItemStack template = stack.clone();
                final int amount = template.getAmount();
                template.setAmount(1);
                this.mergeStoredItem(storedItems, template, amount);
            }

            this.normalizeStoredDisplayAmounts(storedItems);
            shop.setItems(storedItems);
            this.rds.get(clickContext).getShopRepository().update(shop);
            this.saving.set(true, clickContext);

            this.i18n("saved", clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "added_count", playerItems.size(),
                            "stored_count", shop.getStoredItemCount()
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();

            clickContext.openForPlayer(
                    ShopOverviewView.class,
                    Map.of(
                            "plugin", this.rds.get(clickContext),
                            "shopLocation", shop.getShopLocation()
                    )
            );
        } catch (Exception exception) {
            this.i18n("save_failed", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
        }
    }

    private void handleSlotClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final ItemStack cursorItem = clickContext.getClickOrigin().getCursor();
        final int clickedSlot = clickContext.getClickedSlot();
        final ItemStack currentSlotItem = clickContext.getClickOrigin().getCurrentItem();

        final boolean isSlotEmptyOrPane =
                isAir(currentSlotItem) ||
                currentSlotItem.getType() == Material.GREEN_STAINED_GLASS_PANE;

        final Map<Integer, ItemStack> playerItems = this.insertedItems.get(clickContext);

        if (clickContext.getClickedContainer().isEntityContainer() && clickContext.isShiftClick()) {
            clickContext.setCancelled(true);
            return;
        }

        if (clickContext.isLeftClick()) {
            if (isSlotEmptyOrPane && !isAir(cursorItem)) {
                final ItemStack inserted = cursorItem.clone();
                playerItems.put(clickedSlot, inserted.clone());
                clickContext.getClickedContainer().renderItem(clickedSlot, inserted);
                clickContext.getPlayer().setItemOnCursor(null);
            }
            return;
        }

        if (clickContext.isRightClick()) {
            if (!isSlotEmptyOrPane && !isAir(currentSlotItem)) {
                final ItemStack removed = playerItems.remove(clickedSlot);
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
    }

    private void handleShiftClick(
            final @NotNull SlotClickContext click
    ) {
        final Player player = click.getPlayer();
        final ItemStack clickedItem = click.getClickOrigin().getCurrentItem();

        if (!isAir(clickedItem)) {
            final Inventory guiInventory = player.getOpenInventory().getTopInventory();
            final int targetSlot = findFirstPaneSlot(
                    guiInventory,
                    Set.of(Material.GREEN_STAINED_GLASS_PANE)
            );

            if (targetSlot != -1) {
                player.getInventory().removeItem(clickedItem);
                guiInventory.setItem(targetSlot, clickedItem.clone());
                this.insertedItems.get(click).put(targetSlot, clickedItem.clone());
                click.setCancelled(true);
                return;
            }
        }

        click.setCancelled(true);
    }

    private int findFirstPaneSlot(
            final @NotNull Inventory inventory,
            final @NotNull Set<Material> paneTypes
    ) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack item = inventory.getItem(slot);
            if (item != null && paneTypes.contains(item.getType())) {
                return slot;
            }
        }
        return -1;
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", getOwnerName(shop),
                                "location", formatLocation(shop),
                                "stored_count", shop.getStoredItemCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.locked.name", player).build().component())
                .setLore(this.i18n("feedback.locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void mergeStoredItem(
            final @NotNull List<AbstractItem> storedItems,
            final @NotNull ItemStack template,
            final int amount
    ) {
        for (int index = 0; index < storedItems.size(); index++) {
            final AbstractItem existing = storedItems.get(index);
            if (existing instanceof ShopItem shopItem && shopItem.getItem().isSimilar(template)) {
                final int updatedAmount = shopItem.hasAdminStockLimit()
                        ? Math.min(shopItem.getAdminStockLimit(), shopItem.getAmount() + amount)
                        : shopItem.getAmount() + amount;
                storedItems.set(
                        index,
                        shopItem.withAmount(updatedAmount)
                );
                return;
            }
        }

        storedItems.add(new ShopItem(template, amount, "vault", 0D));
    }

    private void normalizeStoredDisplayAmounts(
            final @NotNull List<AbstractItem> storedItems
    ) {
        for (int index = 0; index < storedItems.size(); index++) {
            final AbstractItem storedItem = storedItems.get(index);
            if (!(storedItem instanceof ShopItem shopItem)) {
                continue;
            }

            storedItems.set(
                    index,
                    new ShopItem(
                            shopItem.getEntryId(),
                            shopItem.getItem(),
                            shopItem.getAmount(),
                            shopItem.getCurrencyType(),
                            shopItem.getValue(),
                            shopItem.hasAdminStockLimit() ? shopItem.getAdminStockLimit() : null,
                            shopItem.getAdminRestockIntervalTicks() > 0L ? shopItem.getAdminRestockIntervalTicks() : null,
                            shopItem.getAdminStockReferenceTime() >= 0L ? shopItem.getAdminStockReferenceTime() : null,
                            shopItem.getAvailabilityMode(),
                            shopItem.getAvailabilityRotationMinutes()
                    )
            );
        }
    }

    private @NotNull ItemStack buildPane(
            final @NotNull Material paneType,
            final @NotNull Player player,
            final @NotNull String nameKey,
            final @NotNull String loreKey
    ) {
        return UnifiedBuilderFactory.item(paneType)
                .setName(new I18n.Builder(this.getKey() + "." + nameKey, player).build().component())
                .setLore(new I18n.Builder(this.getKey() + "." + loreKey, player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void refundInsertedItems(
            final @NotNull Player player,
            final @NotNull Collection<ItemStack> items
    ) {
        if (items.isEmpty()) {
            return;
        }

        player.getInventory().addItem(items.toArray(new ItemStack[0]))
                .forEach((slot, item) -> player.getWorld().dropItem(
                        player.getLocation().clone().add(0, 0.5, 0),
                        item
                ));

        this.i18n("left_overs", player)
                .includePrefix()
                .build()
                .sendMessage();
    }

    private boolean isAir(final ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    private Map<Integer, ItemStack> getVisibleInsertedItems(
            final @NotNull Player player
    ) {
        final Inventory topInventory = player.getOpenInventory().getTopInventory();
        final Map<Integer, ItemStack> visibleItems = new TreeMap<>();

        for (int slot : getInputSlots()) {
            final ItemStack item = topInventory.getItem(slot);
            if (isPlacedItem(item)) {
                visibleItems.put(slot, item.clone());
            }
        }

        return visibleItems;
    }

    private List<Integer> getInputSlots() {
        final String[] layout = this.getLayout();
        if (layout == null || layout.length == 0) {
            return Collections.emptyList();
        }

        final List<Integer> slots = new ArrayList<>();
        for (int row = 0; row < layout.length; row++) {
            final String line = layout[row];
            for (int column = 0; column < line.length(); column++) {
                if (line.charAt(column) == 'x') {
                    slots.add((row * 9) + column);
                }
            }
        }
        return slots;
    }

    private boolean isPlacedItem(final ItemStack stack) {
        return !isAir(stack) && stack.getType() != Material.GREEN_STAINED_GLASS_PANE;
    }

    private @NotNull Shop getCurrentShop(
            final @NotNull Context context
    ) {
        final Shop current = this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
        return current == null ? this.shop.get(context) : current;
    }

    private @NotNull String formatLocation(final @NotNull Shop shop) {
        return "("
                + shop.getShopLocation().getBlockX() + ", "
                + shop.getShopLocation().getBlockY() + ", "
                + shop.getShopLocation().getBlockZ() + ")";
    }

    private @NotNull String getOwnerName(final @NotNull Shop shop) {
        final String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        return ownerName == null ? shop.getOwner().toString() : ownerName;
    }
}
