/*
 * StorageView.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interactive inventory view for a single persisted player storage.
 *
 * <p>The view loads the current contents of the targeted
 * {@link com.raindropcentral.rdr.database.entity.RStorage}, allows the player to move items freely,
 * and persists the updated sparse slot map when the inventory closes.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageView extends BaseView {

    private final State<RDR> rdr = initialState("plugin");
    private final State<Long> storageId = initialState("storage_id");
    private final State<String> storageKey = initialState("storage_key");
    private final State<Integer> storageSize = initialState("storage_size");
    private final State<Map<Integer, ItemStack>> storageInventory = initialState("storage_inventory");
    private final State<UUID> leaseToken = initialState("lease_token");
    private final State<Boolean> storageCanDeposit = initialState("storage_can_deposit");
    private final State<Boolean> storageCanWithdraw = initialState("storage_can_withdraw");
    private final MutableState<Boolean> leaseLostNotified = mutableState(false);

    /**
     * Creates the storage editor view.
     */
    public StorageView() {
        super(StoragePlayerView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage view translation key prefix
     */
    @Override
    protected String getKey() {
        return "storage_view";
    }

    /**
     * Returns the fallback inventory size used before the storage-specific size is resolved.
     *
     * @return default six-row chest size
     */
    @Override
    protected int getSize() {
        return 54;
    }

    /**
     * Returns how frequently the active storage lease should be renewed while the inventory remains open.
     *
     * @return lease renewal schedule in ticks
     */
    @Override
    protected int getUpdateSchedule() {
        return (int) StorageLeasePolicy.LEASE_RENEW_INTERVAL_TICKS;
    }

    /**
     * Disables automatic filler item placement so storage slots remain fully interactive.
     *
     * @return {@code false} so item slots remain empty unless populated by storage contents
     */
    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    /**
     * Returns title placeholders for the currently targeted storage.
     *
     * @param open open context for the current viewer
     * @return placeholder map containing the storage key
     */
    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        final String resolvedStorageKey = this.storageKey.get(open);
        return Map.of("storage_key", resolvedStorageKey == null ? "unknown" : resolvedStorageKey);
    }

    /**
     * Applies the resolved storage inventory size to the view before it opens.
     *
     * @param open open context for the current viewer
     */
    @Override
    public void onOpen(final @NotNull OpenContext open) {
        super.onOpen(open);
        open.modifyConfig().size(this.resolveViewSize(open));
    }

    /**
     * Suppresses default navigation controls so every slot remains usable for storage contents.
     *
     * @param render render context
     * @param player player opening the storage
     */
    @Override
    public void renderNavigationButtons(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        // Storage inventories reserve all slots for item interaction.
    }

    /**
     * Populates the storage contents or renders a missing-storage state when the storage cannot be loaded.
     *
     * @param render render context for the current viewer
     * @param player player viewing the storage
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final Map<Integer, ItemStack> inventory = this.storageInventory.get(render);
        if (!this.hasActiveLease(render) || inventory == null) {
            render.slot(4)
                .withItem(this.createMissingStorageItem(player))
                .onClick(click -> click.setCancelled(true));
            return;
        }

        for (Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
            render.slot(entry.getKey(), entry.getValue().clone());
        }
    }

    /**
     * Enforces storage interaction permissions for shared storage viewers.
     *
     * <p>Associates may deposit items but cannot withdraw them, while trusted players retain full
     * inventory interaction.</p>
     *
     * @param click click context for the current interaction
     */
    @Override
    public void onClick(final @NotNull me.devnatan.inventoryframework.context.SlotClickContext click) {
        final ItemStack blockedDepositItem = this.resolveBlockedDepositItem(click);
        if (!this.isAir(blockedDepositItem)) {
            click.setCancelled(true);
            this.sendBlacklistedItemMessage(click.getPlayer(), blockedDepositItem);
            return;
        }

        if (!Boolean.TRUE.equals(this.storageCanDeposit.get(click))) {
            click.setCancelled(true);
            return;
        }

        if (Boolean.TRUE.equals(this.storageCanWithdraw.get(click))) {
            click.setCancelled(false);
            return;
        }

        if (click.isOutsideClick()) {
            click.setCancelled(false);
            return;
        }

        if (click.getClickedContainer().isEntityContainer()) {
            click.setCancelled(false);
            return;
        }

        if (click.isShiftClick() || click.isKeyboardClick() || click.isMiddleClick()) {
            click.setCancelled(true);
            return;
        }

        final ItemStack cursorItem = click.getClickOrigin().getCursor();
        final ItemStack currentSlotItem = click.getClickOrigin().getCurrentItem();
        if (this.canAssociateDeposit(cursorItem, currentSlotItem)) {
            click.setCancelled(false);
            return;
        }

        click.setCancelled(true);
    }

    /**
     * Renews the storage lease while the inventory remains open.
     *
     * @param context active view context
     */
    @Override
    public void onUpdate(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        final RRStorage storageRepository = plugin.getStorageRepository();
        final Long activeStorageId = this.storageId.get(context);
        final String activeStorageKey = this.storageKey.get(context);
        final UUID activeLeaseToken = this.leaseToken.get(context);
        final Player player = context.getPlayer();

        if (storageRepository == null || activeStorageId == null || activeLeaseToken == null) {
            return;
        }

        storageRepository.renewLeaseAsync(
                activeStorageId,
                plugin.getServerUuid(),
                player.getUniqueId(),
                activeLeaseToken,
                StorageLeasePolicy.nextExpiry()
            )
            .thenAccept(renewed -> {
                if (!renewed) {
                    plugin.getScheduler().runSync(() -> this.handleLeaseLost(context, player));
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().warning(
                    "Failed to renew storage lease for " + activeStorageKey + ": " + throwable.getMessage()
                );
                plugin.getScheduler().runSync(() -> this.handleLeaseLost(context, player));
                return null;
            });
    }

    /**
     * Persists the current storage contents when the inventory closes.
     *
     * @param close close context supplied by Inventory Framework
     */
    @Override
    public void onClose(final @NotNull CloseContext close) {
        final RDR plugin = this.rdr.get(close);
        final RRStorage storageRepository = plugin.getStorageRepository();
        final Long activeStorageId = this.storageId.get(close);
        final String activeStorageKey = this.storageKey.get(close);
        final UUID activeLeaseToken = this.leaseToken.get(close);
        final Integer inventorySize = this.storageSize.get(close);
        final Player player = close.getPlayer();

        if (storageRepository == null || activeStorageId == null || activeLeaseToken == null || inventorySize == null) {
            plugin.getLogger().warning("Storage repository was unavailable while saving storage contents.");
            return;
        }

        final Map<Integer, ItemStack> inventorySnapshot = snapshotInventory(
            close.getParent().getInventory().getContents(),
            inventorySize
        );

        storageRepository.saveInventoryAndReleaseLeaseAsync(
                activeStorageId,
                plugin.getServerUuid(),
                player.getUniqueId(),
                activeLeaseToken,
                inventorySnapshot
            )
            .thenAccept(saved -> {
                if (!saved) {
                    plugin.getScheduler().runSync(() -> this.handleLeaseLost(close, player));
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().warning(
                    "Failed to save storage " + activeStorageKey + " for " + player.getUniqueId() + ": " + throwable.getMessage()
                );
                plugin.getScheduler().runSync(() -> this.handleSaveFailure(close, player));
                return null;
            });
    }

    static @NotNull Map<Integer, ItemStack> snapshotInventory(
        final ItemStack @Nullable [] contents,
        final int maxSlots
    ) {
        final Map<Integer, ItemStack> inventory = new HashMap<>();
        if (contents == null) {
            return inventory;
        }

        final int slotLimit = Math.min(contents.length, maxSlots);
        for (int slot = 0; slot < slotLimit; slot++) {
            final ItemStack itemStack = contents[slot];
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            inventory.put(slot, itemStack.clone());
        }
        return inventory;
    }

    private int resolveViewSize(final @NotNull OpenContext open) {
        final Integer resolvedStorageSize = this.storageSize.get(open);
        return resolvedStorageSize == null ? 9 : resolvedStorageSize;
    }

    private boolean hasActiveLease(final @NotNull Context context) {
        return this.storageId.get(context) != null
            && this.storageKey.get(context) != null
            && this.storageSize.get(context) != null
            && this.leaseToken.get(context) != null;
    }

    private @NotNull ItemStack createMissingStorageItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void handleLeaseLost(
        final @NotNull Context context,
        final @NotNull Player player
    ) {
        if (this.leaseLostNotified.get(context)) {
            return;
        }

        this.leaseLostNotified.set(true, context);
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }

        new I18n.Builder("storage.message.lease_lost", player)
            .withPlaceholder("storage_key", this.storageKey.get(context))
            .build()
            .sendMessage();
    }

    private void handleSaveFailure(
        final @NotNull Context context,
        final @NotNull Player player
    ) {
        if (this.leaseLostNotified.get(context)) {
            return;
        }

        new I18n.Builder("storage.message.save_failed", player)
            .withPlaceholder("storage_key", this.storageKey.get(context))
            .build()
            .sendMessage();
    }

    private boolean canAssociateDeposit(
        final @Nullable ItemStack cursorItem,
        final @Nullable ItemStack currentSlotItem
    ) {
        if (this.isAir(cursorItem)) {
            return false;
        }

        return this.isAir(currentSlotItem) || currentSlotItem.isSimilar(cursorItem);
    }

    private boolean isAir(final @Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty() || itemStack.getType().isAir();
    }

    private @Nullable ItemStack resolveBlockedDepositItem(
        final @NotNull me.devnatan.inventoryframework.context.SlotClickContext click
    ) {
        final ConfigSection config = this.rdr.get(click).getDefaultConfig();
        if (click.isOutsideClick()) {
            return null;
        }

        if (click.getClickedContainer().isEntityContainer()) {
            final ItemStack shiftedItem = click.isShiftClick() ? click.getClickOrigin().getCurrentItem() : null;
            return this.isGloballyBlacklisted(config, shiftedItem) ? shiftedItem : null;
        }

        final ItemStack cursorItem = click.getClickOrigin().getCursor();
        if (this.isGloballyBlacklisted(config, cursorItem)) {
            return cursorItem;
        }

        if (click.isKeyboardClick()) {
            final int hotbarButton = click.getClickOrigin().getHotbarButton();
            if (hotbarButton >= 0) {
                final ItemStack hotbarItem = click.getPlayer().getInventory().getItem(hotbarButton);
                if (this.isGloballyBlacklisted(config, hotbarItem)) {
                    return hotbarItem;
                }
            }
        }

        return null;
    }

    private boolean isGloballyBlacklisted(
        final @NotNull ConfigSection config,
        final @Nullable ItemStack itemStack
    ) {
        return !this.isAir(itemStack) && config.isGloballyBlacklisted(itemStack.getType().name());
    }

    private void sendBlacklistedItemMessage(
        final @NotNull Player player,
        final @NotNull ItemStack itemStack
    ) {
        new I18n.Builder("storage.message.blacklisted_item", player)
            .withPlaceholder("item", this.formatMaterialName(itemStack.getType().name()))
            .build()
            .sendMessage();
    }

    private @NotNull String formatMaterialName(final @NotNull String materialName) {
        final String normalized = materialName.replace('_', ' ').toLowerCase(Locale.ROOT);
        final String[] words = normalized.split("\\s+");
        final StringBuilder builder = new StringBuilder();

        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }

        return builder.length() == 0 ? materialName : builder.toString();
    }
}
