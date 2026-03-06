package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RTownShopBank;
import com.raindropcentral.rds.database.repository.RRTownShopBank;
import com.raindropcentral.rds.service.tax.ShopTownTaxBankService;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mayor transfer view for protection tax balances collected from player shops.
 *
 * <p>Each entry represents one currency currently stored in the plugin-managed town shop-tax ledger.
 * Transfer currently supports {@code vault} currency because supported protection plugins expose a
 * single-value town bank API path.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopTaxView extends APaginatedView<RTownShopBank> {

    private static final double EPSILON = 1.0E-6D;
    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the town tax-transfer view.
     */
    public ShopTaxView() {
        super(ShopSearchView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return shop tax translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_tax_ui";
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
     * Resolves town-tax ledger entries for pagination.
     *
     * @param context active menu context
     * @return async list of town-tax entries
     */
    @Override
    protected @NotNull CompletableFuture<List<RTownShopBank>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDS plugin = this.rds.get(context);
        final Player player = context.getPlayer();
        return CompletableFuture.completedFuture(this.findTownEntries(plugin, player));
    }

    /**
     * Renders a single town-tax ledger entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry town-tax ledger entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RTownShopBank entry
    ) {
        final RDS plugin = this.rds.get(context);
        final boolean canTransfer = ShopTownTaxBankService.canTransferToTownBank(context.getPlayer());
        builder.withItem(this.createEntryItem(context.getPlayer(), plugin, entry, canTransfer))
                .onClick(clickContext -> this.handleTransferClick(clickContext, plugin, entry));
    }

    /**
     * Renders static controls for the town tax-transfer view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final ShopTownTaxBankService.TownScope townScope = ShopTownTaxBankService.resolveTownScope(player);
        final boolean canTransfer = townScope != null && ShopTownTaxBankService.canTransferToTownBank(player);

        final int entryCount = townScope == null ? 0 : this.findTownEntries(plugin, player).size();
        final String townName = townScope == null ? "-" : townScope.townDisplayName();
        final String protectionPlugin = townScope == null ? "-" : townScope.protectionPlugin();

        render.layoutSlot('s', this.createSummaryItem(player, townName, protectionPlugin, entryCount, canTransfer));
        if (townScope == null) {
            render.slot(22).renderWith(() -> this.createNoTownItem(player));
            return;
        }

        if (entryCount < 1) {
            render.slot(22).renderWith(() -> canTransfer
                    ? this.createEmptyItem(player)
                    : this.createMayorLockedItem(player)
            );
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

    private @NotNull List<RTownShopBank> findTownEntries(
            final @NotNull RDS plugin,
            final @NotNull Player player
    ) {
        final ShopTownTaxBankService.TownScope townScope = ShopTownTaxBankService.resolveTownScope(player);
        final RRTownShopBank repository = plugin.getTownShopBankRepository();
        if (townScope == null || repository == null) {
            return List.of();
        }

        return repository.findByTown(townScope.protectionPlugin(), townScope.townIdentifier()).stream()
                .filter(entry -> entry.getAmount() > EPSILON)
                .toList();
    }

    private void handleTransferClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull RDS plugin,
            final @NotNull RTownShopBank entry
    ) {
        clickContext.setCancelled(true);

        if (!ShopTownTaxBankService.canTransferToTownBank(clickContext.getPlayer())) {
            this.i18n("feedback.mayor_required", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final String currencyType = entry.getCurrencyType().trim().toLowerCase(Locale.ROOT);
        if (!"vault".equals(currencyType)) {
            this.i18n("feedback.currency_unsupported", clickContext.getPlayer())
                    .includePrefix()
                    .withPlaceholder("currency", this.getCurrencyDisplayName(currencyType))
                    .build()
                    .sendMessage();
            return;
        }

        final double transferredAmount = ShopTownTaxBankService.transferToTownBank(
                plugin,
                clickContext.getPlayer(),
                currencyType
        );
        if (transferredAmount <= EPSILON) {
            this.i18n("feedback.transfer_failed", clickContext.getPlayer())
                    .includePrefix()
                    .withPlaceholder("currency", this.getCurrencyDisplayName(currencyType))
                    .build()
                    .sendMessage();
            return;
        }

        this.i18n("feedback.transfer_success", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "currency", this.getCurrencyDisplayName(currencyType),
                        "amount", this.formatAmount(plugin, currencyType, transferredAmount)
                ))
                .build()
                .sendMessage();

        clickContext.openForPlayer(
                ShopTaxView.class,
                Map.of("plugin", plugin)
        );
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull String townName,
            final @NotNull String protectionPlugin,
            final int entryCount,
            final boolean mayorTransferEnabled
    ) {
        return UnifiedBuilderFactory.item(Material.EMERALD_BLOCK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "town_name", townName,
                                "protection_plugin", protectionPlugin,
                                "entry_count", entryCount,
                                "mayor_transfer_enabled", mayorTransferEnabled
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Player player,
            final @NotNull RDS plugin,
            final @NotNull RTownShopBank entry,
            final boolean canTransfer
    ) {
        final String currencyType = entry.getCurrencyType();
        final boolean transferable = "vault".equalsIgnoreCase(currencyType) && canTransfer;
        final String statusPlaceholder = transferable
                ? this.i18n("entry.status.transferable", player).build().getI18nVersionWrapper().asPlaceholder()
                : "vault".equalsIgnoreCase(currencyType)
                ? this.i18n("entry.status.mayor_required", player).build().getI18nVersionWrapper().asPlaceholder()
                : this.i18n("entry.status.unavailable", player).build().getI18nVersionWrapper().asPlaceholder();

        return UnifiedBuilderFactory.item(transferable ? Material.GOLD_BLOCK : Material.COPPER_BLOCK)
                .setName(this.i18n("entry.name", player)
                        .withPlaceholder("currency", this.getCurrencyDisplayName(currencyType))
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "amount", this.formatAmount(plugin, currencyType, entry.getAmount()),
                                "transfer_status", statusPlaceholder
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createNoTownItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("no_town.name", player).build().component())
                .setLore(this.i18n("no_town.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createMayorLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("locked.name", player).build().component())
                .setLore(this.i18n("locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(this.i18n("empty.name", player).build().component())
                .setLore(this.i18n("empty.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    private @NotNull String formatAmount(
            final @NotNull RDS plugin,
            final @NotNull String currencyType,
            final double amount
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
            return plugin.formatVaultCurrency(amount);
        }

        return String.format(Locale.US, "%.2f %s", amount, this.getCurrencyDisplayName(currencyType));
    }
}
