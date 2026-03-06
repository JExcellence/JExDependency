package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view listing the player's frozen storages with unpaid debt.
 *
 * <p>Clicking an entry routes through the normal storage launcher, which applies due-payment
 * checks and unfreezes the storage when the debt is paid successfully.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageFrozenStorageView extends APaginatedView<RStorage> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the frozen-storage list view.
     */
    public StorageFrozenStorageView() {
        super(StorageTaxView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return frozen-storage translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_frozen_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered layout with pagination controls
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    /**
     * Resolves frozen storage entries for pagination.
     *
     * @param context active menu context
     * @return async list of player's frozen storages
     */
    @Override
    protected @NotNull CompletableFuture<List<RStorage>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RDR plugin = this.rdr.get(context);
        return CompletableFuture.completedFuture(this.findFrozenStorages(plugin, context.getPlayer()));
    }

    /**
     * Renders a single frozen storage entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry frozen storage entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RStorage entry
    ) {
        final RDR plugin = this.rdr.get(context);
        builder.withItem(this.createFrozenEntryItem(context.getPlayer(), plugin, entry))
            .onClick(clickContext -> StorageViewLauncher.openStorage(clickContext.getPlayer(), plugin, entry));
    }

    /**
     * Renders static controls for the frozen-storage view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final List<RStorage> storages = this.findFrozenStorages(this.rdr.get(render), player);
        render.layoutSlot('s', this.createSummaryItem(player, storages.size()));
        if (storages.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull List<RStorage> findFrozenStorages(
        final @NotNull RDR plugin,
        final @NotNull Player player
    ) {
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository == null) {
            return List.of();
        }

        return storageRepository.findByPlayerUuid(player.getUniqueId()).stream()
            .filter(RStorage::hasTaxDebt)
            .sorted((first, second) -> first.getStorageKey().compareToIgnoreCase(second.getStorageKey()))
            .toList();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int frozenStorageCount
    ) {
        return UnifiedBuilderFactory.item(Material.ICE)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("frozen_storage_count", frozenStorageCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFrozenEntryItem(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage
    ) {
        final List<Component> lore = new ArrayList<>(this.i18n("entry.lore", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .children());

        for (final Map.Entry<String, Double> debtEntry : storage.getTaxDebtEntries().entrySet()) {
            if (debtEntry.getKey() == null || debtEntry.getKey().isBlank() || debtEntry.getValue() == null) {
                continue;
            }
            final String currencyId = debtEntry.getKey().trim().toLowerCase(Locale.ROOT);
            final double amount = Math.max(0.0D, debtEntry.getValue());
            if (amount <= 1.0E-6D) {
                continue;
            }

            lore.add(this.i18n("entry.debt_line", player)
                .withPlaceholders(Map.of(
                    "currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyId),
                    "amount", StorageStorePricingSupport.formatCurrency(plugin, currencyId, amount)
                ))
                .build()
                .component());
        }

        return UnifiedBuilderFactory.item(Material.BLUE_ICE)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("storage_key", storage.getStorageKey())
                .build()
                .component())
            .setLore(lore)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
