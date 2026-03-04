/*
 * ServerBankView.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.ServerBank;
import com.raindropcentral.rds.database.repository.RServerBank;
import com.raindropcentral.rds.service.bank.AdminShopServerBankScheduler;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Renders the global server-bank admin view.
 */
public class ServerBankView extends APaginatedView<ServerBankView.ServerBankEntry> {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";
    private static final DateTimeFormatter NEXT_PULL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the server-bank view.
     */
    public ServerBankView() {
        super(ShopAdminView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "server_bank_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "  <w p>  "
        };
    }

    /**
     * Resolves paginated server-bank entries.
     *
     * @param context active menu context
     * @return async list of server-bank entries
     */
    @Override
    protected CompletableFuture<List<ServerBankEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        if (!context.getPlayer().hasPermission(ADMIN_COMMAND_PERMISSION)) {
            return CompletableFuture.completedFuture(List.of());
        }

        final RServerBank repository = this.rds.get(context).getServerBankRepository();
        if (repository == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<ServerBankEntry> entries = new ArrayList<>();
        for (final ServerBank entry : repository.findAllEntries()) {
            if (entry.getAmount() <= 0D) {
                continue;
            }

            entries.add(new ServerBankEntry(entry.getId(), entry.getCurrencyType(), entry.getAmount()));
        }

        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders a single server-bank currency entry.
     *
     * @param context menu context
     * @param builder item component builder
     * @param index rendered index
     * @param entry entry payload
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ServerBankEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
                .onClick(clickContext -> this.handleWithdrawCurrency(clickContext, entry));
    }

    /**
     * Renders static controls for the paginated server-bank view.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!player.hasPermission(ADMIN_COMMAND_PERMISSION)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(render, player));
        render.layoutSlot('w', this.createWithdrawAllButton(player))
                .onClick(this::handleWithdrawAll);

        final RServerBank repository = this.rds.get(render).getServerBankRepository();
        if (repository == null || this.countPositiveEntries(repository) < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleWithdrawCurrency(
            final @NotNull SlotClickContext clickContext,
            final @NotNull ServerBankEntry entry
    ) {
        if (!clickContext.getPlayer().hasPermission(ADMIN_COMMAND_PERMISSION)) {
            this.i18n("feedback.locked_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final RServerBank repository = this.rds.get(clickContext).getServerBankRepository();
        if (repository == null) {
            this.openFreshView(clickContext);
            return;
        }

        final ServerBank current = this.findMatchingEntry(repository, entry);
        if (current == null || current.getAmount() <= 0D) {
            this.i18n("feedback.entry_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final String currencyType = current.getCurrencyType();
        final String currencyName = this.getCurrencyDisplayName(currencyType);
        final double withdrawAmount = current.getAmount();
        final WithdrawalResult result = this.tryWithdrawToPlayer(
                clickContext,
                currencyType,
                withdrawAmount
        );
        if (!result.success()) {
            this.i18n(result.failureKey(), clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "amount", this.formatAmount(withdrawAmount),
                            "currency_type", currencyType,
                            "currency_name", currencyName
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        current.withdraw(withdrawAmount);
        repository.update(current);

        this.i18n("feedback.withdrawn", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "amount", this.formatAmount(withdrawAmount),
                        "remaining_amount", this.formatAmount(current.getAmount()),
                        "currency_type", currencyType,
                        "currency_name", currencyName,
                        "currency_count", this.countPositiveEntries(repository)
                ))
                .includePrefix()
                .build()
                .sendMessage();
        this.openFreshView(clickContext);
    }

    private void handleWithdrawAll(
            final @NotNull SlotClickContext clickContext
    ) {
        if (!clickContext.getPlayer().hasPermission(ADMIN_COMMAND_PERMISSION)) {
            this.i18n("feedback.locked_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final RServerBank repository = this.rds.get(clickContext).getServerBankRepository();
        if (repository == null) {
            this.openFreshView(clickContext);
            return;
        }

        final List<ServerBank> entries = repository.findAllEntries();
        int withdrawnCurrencies = 0;
        int failedCurrencies = 0;
        final List<String> withdrawnSummary = new ArrayList<>();

        for (final ServerBank entry : entries) {
            final double amount = entry.getAmount();
            if (amount <= 0D) {
                continue;
            }

            final String currencyType = entry.getCurrencyType();
            final WithdrawalResult result = this.tryWithdrawToPlayer(
                    clickContext,
                    currencyType,
                    amount
            );
            if (!result.success()) {
                failedCurrencies++;
                continue;
            }

            entry.withdraw(amount);
            repository.update(entry);
            withdrawnCurrencies++;
            withdrawnSummary.add(
                    this.getCurrencyDisplayName(currencyType) + ": " + this.formatAmount(amount)
            );
        }

        if (withdrawnCurrencies < 1) {
            this.i18n("feedback.withdraw_all_failed", clickContext.getPlayer())
                    .withPlaceholder("failed_currency_count", failedCurrencies)
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        this.i18n("feedback.withdraw_all", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "withdrawn_currency_count", withdrawnCurrencies,
                        "failed_currency_count", failedCurrencies,
                        "withdrawn_summary", String.join(", ", withdrawnSummary),
                        "remaining_currency_count", this.countPositiveEntries(repository)
                ))
                .includePrefix()
                .build()
                .sendMessage();
        this.openFreshView(clickContext);
    }

    private @Nullable ServerBank findMatchingEntry(
            final @NotNull RServerBank repository,
            final @NotNull ServerBankEntry entry
    ) {
        final ServerBank found = repository.findByCurrencyType(entry.currencyType());
        if (found == null) {
            return null;
        }

        if (entry.entryId() != null && found.getId() != null && !entry.entryId().equals(found.getId())) {
            return null;
        }

        return found;
    }

    private @NotNull WithdrawalResult tryWithdrawToPlayer(
            final @NotNull Context context,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return WithdrawalResult.successful();
        }

        final Player player = context.getPlayer();
        if (this.usesVaultCurrency(currencyType)) {
            final RDS plugin = this.rds.get(context);
            if (!plugin.hasVaultEconomy()) {
                return WithdrawalResult.failure("feedback.currency_unavailable");
            }

            return plugin.depositVault(player, amount)
                    ? WithdrawalResult.successful()
                    : WithdrawalResult.failure("feedback.withdraw_failed");
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !this.hasCustomCurrency(bridge, currencyType)) {
            return WithdrawalResult.failure("feedback.currency_unavailable");
        }

        return this.depositCustomCurrency(bridge, player, currencyType, amount)
                ? WithdrawalResult.successful()
                : WithdrawalResult.failure("feedback.withdraw_failed");
    }

    private boolean hasCustomCurrency(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull String currencyType
    ) {
        try {
            final Method hasCurrencyMethod = JExEconomyBridge.class.getMethod("hasCurrency", String.class);
            return Boolean.TRUE.equals(hasCurrencyMethod.invoke(bridge, currencyType));
        } catch (ReflectiveOperationException ignored) {
            try {
                final Method findCurrencyMethod = JExEconomyBridge.class.getDeclaredMethod("findCurrency", String.class);
                findCurrencyMethod.setAccessible(true);
                return findCurrencyMethod.invoke(bridge, currencyType) != null;
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }
    }

    private boolean depositCustomCurrency(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull OfflinePlayer player,
            final @NotNull String currencyType,
            final double amount
    ) {
        try {
            final Method depositMethod = JExEconomyBridge.class.getMethod(
                    "deposit",
                    OfflinePlayer.class,
                    String.class,
                    double.class
            );
            final Object futureObject = depositMethod.invoke(bridge, player, currencyType, amount);
            if (futureObject instanceof CompletableFuture<?> future) {
                return Boolean.TRUE.equals(future.join());
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return this.invokeBridgeAdapterDeposit(bridge, player, currencyType, amount);
    }

    private boolean invokeBridgeAdapterDeposit(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull OfflinePlayer player,
            final @NotNull String currencyType,
            final double amount
    ) {
        try {
            final Method findCurrencyMethod = JExEconomyBridge.class.getDeclaredMethod("findCurrency", String.class);
            findCurrencyMethod.setAccessible(true);
            final Object currency = findCurrencyMethod.invoke(bridge, currencyType);
            if (currency == null) {
                return false;
            }

            final Field adapterField = JExEconomyBridge.class.getDeclaredField("adapter");
            final Field adapterClassField = JExEconomyBridge.class.getDeclaredField("adapterClass");
            final Field responseClassField = JExEconomyBridge.class.getDeclaredField("responseClass");
            final Field currencyClassField = JExEconomyBridge.class.getDeclaredField("currencyClass");

            adapterField.setAccessible(true);
            adapterClassField.setAccessible(true);
            responseClassField.setAccessible(true);
            currencyClassField.setAccessible(true);

            final Object adapter = adapterField.get(bridge);
            final Class<?> adapterClass = (Class<?>) adapterClassField.get(bridge);
            final Class<?> responseClass = (Class<?>) responseClassField.get(bridge);
            final Class<?> currencyClass = (Class<?>) currencyClassField.get(bridge);

            final Method depositMethod = adapterClass.getMethod(
                    "deposit",
                    OfflinePlayer.class,
                    currencyClass,
                    double.class
            );
            final Object futureObject = depositMethod.invoke(adapter, player, currency, amount);
            if (!(futureObject instanceof CompletableFuture<?> future)) {
                return false;
            }

            final Object response = future.join();
            final Method isSuccessMethod = responseClass.getMethod("isSuccess");
            return Boolean.TRUE.equals(isSuccessMethod.invoke(response));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private void openFreshView(
            final @NotNull Context context
    ) {
        context.openForPlayer(
                ServerBankView.class,
                Map.of("plugin", this.rds.get(context))
        );
    }

    private int countPositiveEntries(
            final @NotNull RServerBank repository
    ) {
        int count = 0;
        for (final ServerBank entry : repository.findAllEntries()) {
            if (entry.getAmount() > 0D) {
                count++;
            }
        }
        return count;
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(context);
        final RServerBank repository = plugin.getServerBankRepository();
        final List<ServerBank> entries = repository == null
                ? List.of()
                : repository.findAllEntries();
        final TransferTiming timing = this.resolveTransferTiming(plugin, player);

        return UnifiedBuilderFactory.item(Material.BEACON)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_count", this.countPositiveEntries(entries),
                                "income_summary", this.formatIncomeSummary(plugin, entries, player),
                                "next_pull_at", timing.nextPullAtDisplay(),
                                "time_until_next_pull", timing.timeUntilDisplay()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createWithdrawAllButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.HOPPER)
                .setName(this.i18n("withdraw_all.name", player).build().component())
                .setLore(this.i18n("withdraw_all.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Player player,
            final @NotNull ServerBankEntry entry
    ) {
        final String currencyName = this.getCurrencyDisplayName(entry.currencyType());
        final Material material = this.usesVaultCurrency(entry.currencyType())
                ? Material.GOLD_INGOT
                : Material.EMERALD;

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("entry.name", player)
                        .withPlaceholder("currency_name", currencyName)
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "amount", this.formatAmount(entry.amount()),
                                "currency_type", entry.currencyType(),
                                "currency_name", currencyName
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
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

    private @NotNull TransferTiming resolveTransferTiming(
            final @NotNull RDS plugin,
            final @NotNull Player player
    ) {
        final AdminShopServerBankScheduler scheduler = plugin.getAdminShopServerBankScheduler();
        if (scheduler == null || !scheduler.isRunning()) {
            return new TransferTiming(
                    this.i18n("summary.next_pull.disabled", player).build().getI18nVersionWrapper().asPlaceholder(),
                    this.i18n("summary.time_until.disabled", player).build().getI18nVersionWrapper().asPlaceholder()
            );
        }

        final Instant nextPullAt = scheduler.getNextTransferAt();
        if (nextPullAt == null) {
            return new TransferTiming(
                    this.i18n("summary.next_pull.pending", player).build().getI18nVersionWrapper().asPlaceholder(),
                    this.i18n("summary.time_until.pending", player).build().getI18nVersionWrapper().asPlaceholder()
            );
        }

        final ZonedDateTime zonedNext = ZonedDateTime.ofInstant(nextPullAt, ZoneId.systemDefault());
        return new TransferTiming(
                zonedNext.format(NEXT_PULL_FORMAT),
                this.formatRemainingTime(Duration.between(Instant.now(), nextPullAt))
        );
    }

    private @NotNull String formatIncomeSummary(
            final @NotNull RDS plugin,
            final @NotNull Collection<ServerBank> entries,
            final @NotNull Player player
    ) {
        final List<String> parts = new ArrayList<>();
        for (final ServerBank entry : entries) {
            if (entry.getAmount() <= 0D) {
                continue;
            }

            final String amount = this.usesVaultCurrency(entry.getCurrencyType())
                    ? plugin.formatVaultCurrency(entry.getAmount())
                    : this.formatAmount(entry.getAmount());
            parts.add(this.getCurrencyDisplayName(entry.getCurrencyType()) + ": " + amount);
        }

        if (parts.isEmpty()) {
            return this.i18n("summary.income.none", player)
                    .build()
                    .getI18nVersionWrapper()
                    .asPlaceholder();
        }

        return String.join(", ", parts);
    }

    private int countPositiveEntries(
            final @NotNull Collection<ServerBank> entries
    ) {
        int count = 0;
        for (final ServerBank entry : entries) {
            if (entry.getAmount() > 0D) {
                count++;
            }
        }
        return count;
    }

    private @NotNull String formatRemainingTime(
            final @NotNull Duration duration
    ) {
        long totalMinutes = Math.max(0L, duration.toMinutes());
        final long days = totalMinutes / (24L * 60L);
        totalMinutes %= 24L * 60L;
        final long hours = totalMinutes / 60L;
        final long minutes = totalMinutes % 60L;

        final List<String> parts = new ArrayList<>();
        if (days > 0L) {
            parts.add(days + "d");
        }
        if (hours > 0L) {
            parts.add(hours + "h");
        }
        if (minutes > 0L) {
            parts.add(minutes + "m");
        }

        if (parts.isEmpty()) {
            return "<1m";
        }

        final int limit = Math.min(parts.size(), 3);
        return String.join(" ", parts.subList(0, limit));
    }

    private boolean usesVaultCurrency(
            final @NotNull String currencyType
    ) {
        return "vault".equalsIgnoreCase(currencyType);
    }

    private @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if (this.usesVaultCurrency(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    private @NotNull String formatAmount(
            final double amount
    ) {
        return String.format(Locale.US, "%.2f", amount);
    }

    /**
     * Represents a server-bank entry in the paginated list.
     *
     * @param entryId entry id
     * @param currencyType currency type
     * @param amount amount
     */
    public record ServerBankEntry(
            @Nullable Long entryId,
            @NotNull String currencyType,
            double amount
    ) {
    }

    private record TransferTiming(
            @NotNull String nextPullAtDisplay,
            @NotNull String timeUntilDisplay
    ) {
    }

    private record WithdrawalResult(
            boolean success,
            @NotNull String failureKey
    ) {
        private static @NotNull WithdrawalResult successful() {
            return new WithdrawalResult(true, "");
        }

        private static @NotNull WithdrawalResult failure(
                final @NotNull String failureKey
        ) {
            return new WithdrawalResult(false, failureKey);
        }
    }
}
