package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminCommandAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminCommandDelayAnvilView;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders command-management controls for an admin shop item.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopItemAdminCommandView extends BaseView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> sourceItem = initialState("shopItem");
    private final MutableState<ShopItem> editedItem = mutableState((ShopItem) null);
    private final MutableState<ShopItem.CommandExecutionMode> commandExecutionMode = mutableState((ShopItem.CommandExecutionMode) null);
    private final MutableState<Long> commandDelayTicks = mutableState((Long) null);

    /**
     * Creates the admin command manager view.
     */
    public ShopItemAdminCommandView() {
        super(ShopItemEditView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_item_admin_command_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                "    e    ",
                "  d a r  ",
                "    c    "
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.getEditedItem(context);
        return Map.of(
                "item_type", item.getItem().getType().name(),
                "command_count", item.getAdminPurchaseCommands().size(),
                "command_mode", this.getCommandExecutionModeLabel(context.getPlayer(), this.getCommandExecutionMode(context)),
                "command_delay_ticks", this.getCommandDelayTicks(context)
        );
    }

    /**
     * Restores command settings and edited item data when returning from nested anvil views.
     *
     * @param origin previous context
     * @param target active context
     */
    @Override
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        this.restoreEditedItem(target);
        if (this.editedItem.get(target) == null) {
            this.restoreEditedItem(origin, target);
        }

        this.restoreCommandExecutionMode(target);
        if (this.commandExecutionMode.get(target) == null) {
            this.restoreCommandExecutionMode(origin, target);
        }
        if (this.commandExecutionMode.get(target) == null) {
            this.commandExecutionMode.set(ShopItem.CommandExecutionMode.SERVER, target);
        }

        this.restoreCommandDelayTicks(target);
        if (this.commandDelayTicks.get(target) == null) {
            this.restoreCommandDelayTicks(origin, target);
        }
        if (this.commandDelayTicks.get(target) == null) {
            this.commandDelayTicks.set(0L, target);
        }

        target.update();
    }

    /**
     * Renders the command-management controls and wiring for this editor view.
     *
     * @param render active render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (this.editedItem.get(render) == null) {
            this.restoreEditedItem(render);
        }
        if (this.editedItem.get(render) == null) {
            this.editedItem.set(this.sourceItem.get(render), render);
        }
        if (this.commandExecutionMode.get(render) == null) {
            this.commandExecutionMode.set(ShopItem.CommandExecutionMode.SERVER, render);
        }
        if (this.commandDelayTicks.get(render) == null) {
            this.commandDelayTicks.set(0L, render);
        }

        render.layoutSlot('s')
                .renderWith(() -> this.createSummaryItem(
                        player,
                        this.getEditedItem(render),
                        this.getCommandExecutionMode(render),
                        this.getCommandDelayTicks(render)
                ))
                .updateOnStateChange(this.editedItem)
                .updateOnStateChange(this.commandExecutionMode)
                .updateOnStateChange(this.commandDelayTicks);

        render.layoutSlot('e')
                .renderWith(() -> this.createExecutionModeItem(player, this.getCommandExecutionMode(render)))
                .updateOnStateChange(this.commandExecutionMode)
                .onClick(this::handleExecutionModeClick);

        render.layoutSlot('d')
                .renderWith(() -> this.createDelayItem(player, this.getCommandDelayTicks(render)))
                .updateOnStateChange(this.commandDelayTicks)
                .onClick(this::handleDelayClick);

        render.layoutSlot('a')
                .renderWith(() -> this.createAddCommandItem(
                        player,
                        this.getCommandExecutionMode(render),
                        this.getCommandDelayTicks(render)
                ))
                .updateOnStateChange(this.commandExecutionMode)
                .updateOnStateChange(this.commandDelayTicks)
                .onClick(this::handleAddCommandClick);

        render.layoutSlot('r')
                .renderWith(() -> this.createRemoveCommandItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(this::handleRemoveCommandClick);

        render.layoutSlot(
                'c',
                UnifiedBuilderFactory.item(new Proceed().getHead(player))
                        .setName(this.i18n("confirm.name", player).build().component())
                        .setLore(this.i18n("confirm.lore", player).build().children())
                        .build()
        ).onClick(this::handleConfirm);
    }

    /**
     * Cancels vanilla inventory interaction so the menu behaves as an action UI.
     *
     * @param click click context for the current interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleExecutionModeClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasManagePermission(clickContext)) {
            return;
        }

        final ShopItem.CommandExecutionMode updatedMode = this.getCommandExecutionMode(clickContext).next();
        this.commandExecutionMode.set(updatedMode, clickContext);
        clickContext.update();

        this.i18n("feedback.command_mode_updated", clickContext.getPlayer())
                .withPlaceholder("command_mode", this.getCommandExecutionModeLabel(clickContext.getPlayer(), updatedMode))
                .build()
                .sendMessage();
    }

    private void handleDelayClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasManagePermission(clickContext)) {
            return;
        }

        clickContext.openForPlayer(
                ShopItemAdminCommandDelayAnvilView.class,
                this.createCommandAnvilData(
                        clickContext,
                        this.getEditedItem(clickContext),
                        this.getCommandExecutionMode(clickContext),
                        this.getCommandDelayTicks(clickContext)
                )
        );
    }

    private void handleAddCommandClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasManagePermission(clickContext)) {
            return;
        }

        clickContext.openForPlayer(
                ShopItemAdminCommandAnvilView.class,
                this.createCommandAnvilData(
                        clickContext,
                        this.getEditedItem(clickContext),
                        this.getCommandExecutionMode(clickContext),
                        this.getCommandDelayTicks(clickContext)
                )
        );
    }

    private void handleRemoveCommandClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasManagePermission(clickContext)) {
            return;
        }

        final ShopItem currentItem = this.getEditedItem(clickContext);
        final ShopItem updatedItem;
        final String feedbackKey;
        if (clickContext.isShiftClick()) {
            updatedItem = currentItem.clearAdminPurchaseCommands();
            feedbackKey = "feedback.commands_cleared";
        } else {
            if (!currentItem.hasAdminPurchaseCommands()) {
                this.i18n("feedback.commands_empty", clickContext.getPlayer())
                        .build()
                        .sendMessage();
                return;
            }
            updatedItem = currentItem.withoutLastAdminPurchaseCommand();
            feedbackKey = "feedback.command_removed";
        }

        this.editedItem.set(updatedItem, clickContext);
        clickContext.update();

        this.i18n(feedbackKey, clickContext.getPlayer())
                .withPlaceholder("command_count", updatedItem.getAdminPurchaseCommands().size())
                .build()
                .sendMessage();
    }

    private void handleConfirm(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasManagePermission(clickContext)) {
            return;
        }

        clickContext.openForPlayer(
                ShopItemEditView.class,
                this.createItemViewData(clickContext, this.getEditedItem(clickContext))
        );
    }

    private boolean hasManagePermission(
            final @NotNull SlotClickContext clickContext
    ) {
        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return false;
        }

        if (!this.canManage(clickContext, shop)) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return false;
        }
        return true;
    }

    private Shop getCurrentShop(
            final @NotNull Context context
    ) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private boolean canManage(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        return shop.canManage(context.getPlayer().getUniqueId()) || ShopAdminAccessSupport.hasOwnerOverride(context);
    }

    private @NotNull Map<String, Object> createBaseViewData(
            final @NotNull Context context
    ) {
        final Map<String, Object> viewData = new HashMap<>();
        viewData.put("plugin", this.rds.get(context));
        viewData.put("shopLocation", this.shopLocation.get(context));
        if (ShopAdminAccessSupport.hasOwnerOverride(context)) {
            viewData.put(ShopAdminAccessSupport.ADMIN_OWNER_OVERRIDE_KEY, true);
        }
        return viewData;
    }

    private @NotNull Map<String, Object> createItemViewData(
            final @NotNull Context context,
            final @NotNull ShopItem shopItem
    ) {
        final Map<String, Object> viewData = this.createBaseViewData(context);
        viewData.put("shopItem", shopItem);
        return viewData;
    }

    private @NotNull Map<String, Object> createCommandAnvilData(
            final @NotNull Context context,
            final @NotNull ShopItem shopItem,
            final @NotNull ShopItem.CommandExecutionMode mode,
            final long commandDelay
    ) {
        final Map<String, Object> viewData = this.createItemViewData(context, shopItem);
        viewData.put("commandExecutionMode", mode);
        viewData.put("commandDelayTicks", commandDelay);
        return viewData;
    }

    private @NotNull ShopItem getEditedItem(
            final @NotNull Context context
    ) {
        final ShopItem current = this.editedItem.get(context);
        return current == null ? this.sourceItem.get(context) : current;
    }

    private @NotNull ShopItem.CommandExecutionMode getCommandExecutionMode(
            final @NotNull Context context
    ) {
        final ShopItem.CommandExecutionMode mode = this.commandExecutionMode.get(context);
        return mode == null ? ShopItem.CommandExecutionMode.SERVER : mode;
    }

    private long getCommandDelayTicks(
            final @NotNull Context context
    ) {
        final Long delayTicks = this.commandDelayTicks.get(context);
        return delayTicks == null ? 0L : Math.max(0L, delayTicks);
    }

    private void restoreEditedItem(
            final @NotNull Context context
    ) {
        this.restoreEditedItem(context, context);
    }

    private void restoreEditedItem(
            final @NotNull Context dataContext,
            final @NotNull Context stateContext
    ) {
        final Object initialData = dataContext.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return;
        }

        final Object updatedItem = data.get("shopItem");
        if (updatedItem instanceof ShopItem shopItem) {
            this.editedItem.set(shopItem, stateContext);
        }
    }

    private void restoreCommandExecutionMode(
            final @NotNull Context context
    ) {
        this.restoreCommandExecutionMode(context, context);
    }

    private void restoreCommandExecutionMode(
            final @NotNull Context dataContext,
            final @NotNull Context stateContext
    ) {
        final Object initialData = dataContext.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return;
        }

        final Object mode = data.get("commandExecutionMode");
        if (mode instanceof ShopItem.CommandExecutionMode commandMode) {
            this.commandExecutionMode.set(commandMode, stateContext);
        }
    }

    private void restoreCommandDelayTicks(
            final @NotNull Context context
    ) {
        this.restoreCommandDelayTicks(context, context);
    }

    private void restoreCommandDelayTicks(
            final @NotNull Context dataContext,
            final @NotNull Context stateContext
    ) {
        final Object initialData = dataContext.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return;
        }

        final Object delayTicks = data.get("commandDelayTicks");
        if (delayTicks instanceof Number number) {
            this.commandDelayTicks.set(Math.max(0L, number.longValue()), stateContext);
        }
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull ShopItem item,
            final @NotNull ShopItem.CommandExecutionMode mode,
            final long delayTicks
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "command_count", item.getAdminPurchaseCommands().size(),
                                "latest_command", this.getLatestCommandPreview(player, item),
                                "command_mode", this.getCommandExecutionModeLabel(player, mode),
                                "command_delay_ticks", delayTicks,
                                "command_delay_seconds", String.format(java.util.Locale.US, "%.2f", delayTicks / 20.0D)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createExecutionModeItem(
            final @NotNull Player player,
            final @NotNull ShopItem.CommandExecutionMode mode
    ) {
        return UnifiedBuilderFactory.item(mode == ShopItem.CommandExecutionMode.PLAYER
                        ? Material.PLAYER_HEAD
                        : Material.COMMAND_BLOCK)
                .setName(this.i18n("command_executor.name", player).build().component())
                .setLore(this.i18n("command_executor.lore", player)
                        .withPlaceholders(Map.of(
                                "command_mode", this.getCommandExecutionModeLabel(player, mode),
                                "next_mode", this.getCommandExecutionModeLabel(player, mode.next())
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createDelayItem(
            final @NotNull Player player,
            final long delayTicks
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("command_delay.name", player).build().component())
                .setLore(this.i18n("command_delay.lore", player)
                        .withPlaceholders(Map.of(
                                "command_delay_ticks", delayTicks,
                                "command_delay_seconds", String.format(java.util.Locale.US, "%.2f", delayTicks / 20.0D)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createAddCommandItem(
            final @NotNull Player player,
            final @NotNull ShopItem.CommandExecutionMode mode,
            final long delayTicks
    ) {
        return UnifiedBuilderFactory.item(Material.ANVIL)
                .setName(this.i18n("add_command.name", player).build().component())
                .setLore(this.i18n("add_command.lore", player)
                        .withPlaceholders(Map.of(
                                "command_mode", this.getCommandExecutionModeLabel(player, mode),
                                "command_delay_ticks", delayTicks,
                                "command_delay_seconds", String.format(java.util.Locale.US, "%.2f", delayTicks / 20.0D)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createRemoveCommandItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("remove_command.name", player).build().component())
                .setLore(this.i18n("remove_command.lore", player)
                        .withPlaceholders(Map.of(
                                "command_count", item.getAdminPurchaseCommands().size()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull String getCommandExecutionModeLabel(
            final @NotNull Player player,
            final @NotNull ShopItem.CommandExecutionMode mode
    ) {
        final String key = mode == ShopItem.CommandExecutionMode.PLAYER
                ? "command_executor.player"
                : "command_executor.server";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getLatestCommandPreview(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        if (!item.hasAdminPurchaseCommands()) {
            return this.i18n("summary.none", player)
                    .build()
                    .getI18nVersionWrapper()
                    .asPlaceholder();
        }

        final ShopItem.AdminPurchaseCommand latest = item.getAdminPurchaseCommands()
                .get(item.getAdminPurchaseCommands().size() - 1);
        return latest.command();
    }
}
