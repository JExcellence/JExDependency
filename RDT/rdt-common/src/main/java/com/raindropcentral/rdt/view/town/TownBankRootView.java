/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.service.TownBankService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Actionable root view for bank currency storage, shared item storage, and cache management.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownBankRootView extends BaseView {

    private static final List<Integer> CURRENCY_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25);

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the actionable town bank root view.
     */
    public TownBankRootView() {
        super(TownOverviewView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_bank_root_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "         ",
            "         ",
            "         ",
            "         ",
            "         "
        };
    }

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        final RDT plugin = this.plugin.get(open);
        final UUID resolvedTownUuid = this.townUuid.get(open);
        if (plugin.getTownBankService() != null
            && resolvedTownUuid != null
            && !plugin.getTownBankService().acquireBankAccess(resolvedTownUuid, open.getPlayer().getUniqueId())) {
            new I18n.Builder("town_bank_shared.messages.in_use", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        super.onOpen(open);
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        final RDT plugin = this.plugin.get(close);
        final UUID resolvedTownUuid = this.townUuid.get(close);
        if (plugin.getTownBankService() != null && resolvedTownUuid != null) {
            plugin.getTownBankService().releaseBankAccess(resolvedTownUuid, close.getPlayer().getUniqueId());
        }
        super.onClose(close);
    }

    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> targetData = TownBankViewSupport.copyInitialData(target);
        final Map<String, Object> data = targetData != null
            ? targetData
            : TownBankViewSupport.copyInitialData(origin);
        if (data == null || !(data.get(TownBankViewSupport.TRANSACTION_STATUS_KEY) instanceof String statusName)) {
            return;
        }

        final Player player = target.getPlayer();
        final String currencyId = data.get(TownBankViewSupport.CURRENCY_ID_KEY) instanceof String rawCurrencyId ? rawCurrencyId : "";
        final Object amount = data.getOrDefault(TownBankViewSupport.TRANSACTION_AMOUNT_KEY, 0.0D);
        final Object balance = data.getOrDefault(TownBankViewSupport.TRANSACTION_BALANCE_KEY, 0.0D);
        new I18n.Builder(this.getKey() + ".transaction." + statusName.toLowerCase(), player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", currencyId,
                "amount", amount,
                "town_balance", balance
            ))
            .build()
            .sendMessage();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDT plugin = this.plugin.get(render);
        final RTown town = this.resolveTown(render);
        final TownBankService bankService = plugin.getTownBankService();
        if (town == null || bankService == null) {
            render.slot(22).renderWith(() -> this.createMissingTownItem(player));
            return;
        }

        render.slot(4).renderWith(() -> this.createSummaryItem(player, town, bankService));
        this.renderCurrencyEntries(render, player, town, bankService);
        render.slot(30)
            .renderWith(() -> this.createSharedStorageItem(player, plugin, town, bankService))
            .onClick(click -> this.handleSharedStorageClick(click, town));
        render.slot(32).renderWith(() -> this.createRemoteStatusItem(player, plugin, town, bankService));
        render.slot(34)
            .renderWith(() -> this.createCacheItem(player, plugin, town, bankService))
            .onClick(click -> this.handleCacheClick(click, town));
        render.slot(40)
            .renderWith(() -> this.createPickupCacheItem(player, town, bankService))
            .onClick(click -> this.handlePickupCacheClick(click, town));
        render.slot(45)
            .renderWith(() -> this.createReturnItem(player))
            .onClick(SlotClickContext::back);
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void renderCurrencyEntries(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService
    ) {
        int slotIndex = 0;
        for (final String currencyId : bankService.getConfiguredCurrencies()) {
            if (slotIndex >= CURRENCY_SLOTS.size()) {
                break;
            }
            final int slot = CURRENCY_SLOTS.get(slotIndex++);
            render.slot(slot)
                .renderWith(() -> this.createCurrencyItem(player, town, bankService, currencyId))
                .onClick(click -> this.handleCurrencyClick(click, town, currencyId));
        }
    }

    private void handleCurrencyClick(
        final @NotNull SlotClickContext click,
        final @NotNull RTown town,
        final @NotNull String currencyId
    ) {
        final TownBankCurrencyAction action = click.isRightClick() ? TownBankCurrencyAction.WITHDRAW : TownBankCurrencyAction.DEPOSIT;
        click.openForPlayer(
            TownBankCurrencyInputView.class,
            TownBankViewSupport.mergeInitialData(
                click,
                Map.of(
                    "plugin", this.plugin.get(click),
                    "town_uuid", town.getTownUUID(),
                    TownBankViewSupport.CURRENCY_ID_KEY, currencyId,
                    TownBankViewSupport.CURRENCY_ACTION_KEY, action.name()
                )
            )
        );
    }

    private void handleSharedStorageClick(final @NotNull SlotClickContext click, final @NotNull RTown town) {
        final TownBankService bankService = this.plugin.get(click).getTownBankService();
        if (bankService == null || !bankService.isItemStorageUnlocked(town)) {
            new I18n.Builder(this.getKey() + ".messages.item_storage_locked", click.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        click.openForPlayer(
            TownBankStorageView.class,
            TownBankViewSupport.mergeInitialData(
                click,
                Map.of(
                    "plugin", this.plugin.get(click),
                    "town_uuid", town.getTownUUID(),
                    TownBankViewSupport.STORAGE_MODE_KEY, TownBankStorageMode.SHARED_STORAGE.name()
                )
            )
        );
    }

    private void handleCacheClick(final @NotNull SlotClickContext click, final @NotNull RTown town) {
        final TownBankService bankService = this.plugin.get(click).getTownBankService();
        if (bankService == null || !bankService.isCacheUnlocked(town)) {
            new I18n.Builder(this.getKey() + ".messages.cache_locked", click.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!bankService.hasPlacedCache(town)) {
            new I18n.Builder(this.getKey() + ".messages.cache_unplaced", click.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        click.openForPlayer(
            TownBankStorageView.class,
            TownBankViewSupport.mergeInitialData(
                click,
                Map.of(
                    "plugin", this.plugin.get(click),
                    "town_uuid", town.getTownUUID(),
                    TownBankViewSupport.STORAGE_MODE_KEY, TownBankStorageMode.LOCAL_CACHE.name()
                )
            )
        );
    }

    private void handlePickupCacheClick(final @NotNull SlotClickContext click, final @NotNull RTown town) {
        final TownBankService bankService = this.plugin.get(click).getTownBankService();
        if (bankService == null) {
            return;
        }

        final TownBankService.CachePickupResult result = bankService.pickupCacheChest(click.getPlayer(), town.getTownUUID());
        final String key = switch (result.status()) {
            case SUCCESS -> "pickup_cache.success";
            case NO_PERMISSION -> "pickup_cache.no_permission";
            case NOT_PLACED -> "pickup_cache.not_placed";
            case INVALID_TARGET, LOCKED, FAILED -> "pickup_cache.failed";
        };
        new I18n.Builder(this.getKey() + '.' + key, click.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
        click.update();
    }

    private @Nullable RTown resolveTown(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        return resolvedTownUuid == null || this.plugin.get(context).getTownBankService() == null
            ? null
            : this.plugin.get(context).getTownBankService().getTown(resolvedTownUuid);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "bank_level", bankService.getHighestBankLevel(town),
                    "viewer_lock", bankService.isBankAccessLocked(town.getTownUUID()) ? "Locked" : "Open"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCurrencyItem(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService,
        final @NotNull String currencyId
    ) {
        final boolean supported = bankService.isSupportedCurrency(currencyId);
        final boolean canDeposit = bankService.canDepositCurrency(player, town);
        final boolean canWithdraw = bankService.canWithdrawCurrency(player, town);
        return UnifiedBuilderFactory.item(supported ? Material.GOLD_INGOT : Material.BARRIER)
            .setName(this.i18n("currency.name", player)
                .withPlaceholder("currency", bankService.resolveCurrencyDisplayName(currencyId))
                .build()
                .component())
            .setLore(this.i18n("currency.lore", player)
                .withPlaceholders(Map.of(
                    "currency", bankService.resolveCurrencyDisplayName(currencyId),
                    "town_balance", bankService.getTownCurrencyBalance(town, currencyId),
                    "player_balance", bankService.getPlayerCurrencyBalance(player, currencyId),
                    "deposit_state", canDeposit ? "Enabled" : "No Access",
                    "withdraw_state", canWithdraw ? "Enabled" : "No Access",
                    "support_state", supported ? "Supported" : "Unavailable"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSharedStorageItem(
        final @NotNull Player player,
        final @NotNull RDT plugin,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService
    ) {
        final boolean unlocked = bankService.isItemStorageUnlocked(town);
        final String loreKey = unlocked ? "shared_storage.lore" : "shared_storage.locked_lore";
        return UnifiedBuilderFactory.item(unlocked ? Material.BARREL : Material.BARRIER)
            .setName(this.i18n("shared_storage.name", player).build().component())
            .setLore(this.i18n(loreKey, player)
                .withPlaceholders(Map.of(
                    "unlock_level", plugin.getBankConfig().getItemStorage().unlockLevel(),
                    "occupied_slots", town.getSharedBankStorage().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRemoteStatusItem(
        final @NotNull Player player,
        final @NotNull RDT plugin,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService
    ) {
        final boolean remoteUnlocked = bankService.isRemoteCommandUnlocked(town);
        final boolean remoteCache = bankService.supportsRemoteCacheDeposit(town);
        final Material material = remoteCache ? Material.ENDER_CHEST : remoteUnlocked ? Material.COMPASS : Material.BARRIER;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("remote.name", player).build().component())
            .setLore(this.i18n("remote.lore", player)
                .withPlaceholders(Map.of(
                    "unlock_level", plugin.getBankConfig().getRemoteAccess().unlockLevel(),
                    "cache_unlock_level", plugin.getBankConfig().getRemoteAccess().crossClusterCacheDepositUnlockLevel(),
                    "remote_state", remoteUnlocked ? "Unlocked" : "Locked",
                    "remote_cache_state", remoteCache ? "Unlocked" : "Locked"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCacheItem(
        final @NotNull Player player,
        final @NotNull RDT plugin,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService
    ) {
        final boolean unlocked = bankService.isCacheUnlocked(town);
        final boolean placed = bankService.hasPlacedCache(town);
        final Material material = !unlocked ? Material.BARRIER : placed ? Material.CHEST : Material.TRAPPED_CHEST;
        final String loreKey = !unlocked ? "cache.locked_lore" : placed ? "cache.placed_lore" : "cache.unplaced_lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("cache.name", player).build().component())
            .setLore(this.i18n(loreKey, player)
                .withPlaceholders(Map.of(
                    "unlock_level", plugin.getBankConfig().getCache().unlockLevel(),
                    "radius_blocks", plugin.getBankConfig().getCache().placementRadiusBlocks(),
                    "stored_slots", town.getBankCacheContents().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPickupCacheItem(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull TownBankService bankService
    ) {
        final boolean available = bankService.hasPlacedCache(town);
        final boolean canPickup = available && bankService.canManageCachePlacement(player, town);
        final String loreKey = !available ? "pickup_cache.unavailable_lore" : canPickup ? "pickup_cache.lore" : "pickup_cache.locked_lore";
        return UnifiedBuilderFactory.item(canPickup ? Material.HOPPER : available ? Material.GRAY_DYE : Material.BARRIER)
            .setName(this.i18n("pickup_cache.name", player).build().component())
            .setLore(this.i18n(loreKey, player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
