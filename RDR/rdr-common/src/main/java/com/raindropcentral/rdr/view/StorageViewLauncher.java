package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rdr.service.StorageTownTaxBankService;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Shared launcher for lease-aware storage view opens.
 *
 * <p>This helper centralizes storage lease acquisition, delayed view transitions, and user feedback
 * so commands and inventory buttons open storages through the same cross-server-safe path.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class StorageViewLauncher {

    private static final double EPSILON = 1.0E-6D;

    private StorageViewLauncher() {}

    /**
     * Attempts to acquire a lease for the supplied storage and opens {@link StorageView} when the.
     * lease succeeds.
     *
     * @param player player requesting access to the storage
     * @param plugin active plugin instance
     * @param storage storage to open
     * @throws NullPointerException if any argument is {@code null}
     */
    public static void openStorage(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage
    ) {
        final ConfigSection config = plugin.getDefaultConfig();

        if (!storage.canAccess(player.getUniqueId())) {
            sendAccessDeniedMessage(player, storage);
            return;
        }

        if (!resolveDuePayment(player, plugin, storage)) {
            return;
        }

        if (!passesProtectionRestrictions(player, storage, config)) {
            return;
        }

        if (!applyOpenStorageTaxes(player, plugin, storage, config)) {
            return;
        }

        final RRStorage storageRepository = plugin.getStorageRepository();
        final Long storageId = storage.getId();
        if (storageRepository == null || storageId == null) {
            sendUnavailableMessage(player, storage);
            return;
        }

        final UUID leaseToken = UUID.randomUUID();
        storageRepository.tryAcquireLeaseAsync(
                storageId,
                plugin.getServerUuid(),
                player.getUniqueId(),
                leaseToken,
                StorageLeasePolicy.nextExpiry()
            )
            .thenAccept(result -> plugin.getScheduler().runSync(() -> handleLeaseAcquireResult(
                player,
                plugin,
                storage,
                storageId,
                leaseToken,
                result
            )))
            .exceptionally(throwable -> {
                plugin.getLogger().warning(
                    "Failed to acquire storage lease for " + storage.getStorageKey() + ": " + throwable.getMessage()
                );
                plugin.getScheduler().runSync(() -> sendUnavailableMessage(player, storage));
                return null;
            });
    }

    private static void handleLeaseAcquireResult(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull Long storageId,
        final @NotNull UUID leaseToken,
        final @NotNull RRStorage.LeaseAcquireResult result
    ) {
        switch (result) {
            case ACQUIRED -> openStorageView(player, plugin, storage, storageId, leaseToken);
            case LOCKED -> sendLockedMessage(player, storage);
            case MISSING -> sendUnavailableMessage(player, storage);
        }
    }

    private static void openStorageView(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull Long storageId,
        final @NotNull UUID leaseToken
    ) {
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("plugin", plugin);
        initialData.put("storage_id", storageId);
        initialData.put("storage_key", storage.getStorageKey());
        initialData.put("storage_size", storage.getInventorySize());
        initialData.put("storage_inventory", storage.getInventory());
        initialData.put("lease_token", leaseToken);
        initialData.put("storage_can_deposit", storage.canDeposit(player.getUniqueId()));
        initialData.put("storage_can_withdraw", storage.canWithdraw(player.getUniqueId()));

        player.closeInventory();
        plugin.getScheduler().runDelayed(() -> {
            if (!player.isOnline()) {
                releaseLease(plugin, storageId, player.getUniqueId(), leaseToken);
                return;
            }

            try {
                plugin.getViewFrame().open(StorageView.class, player, initialData);
            } catch (Exception exception) {
                plugin.getLogger().warning(
                    "Failed to open storage view for " + storage.getStorageKey() + ": " + exception.getMessage()
                );
                releaseLease(plugin, storageId, player.getUniqueId(), leaseToken);
                sendUnavailableMessage(player, storage);
            }
        }, 1L);
    }

    private static void sendLockedMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.locked", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendUnavailableMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.unavailable", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendAccessDeniedMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.access_denied", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static boolean resolveDuePayment(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage
    ) {
        if (!storage.hasTaxDebt()) {
            return true;
        }

        if (!storage.isOwner(player.getUniqueId())) {
            sendStorageFrozenMessage(player, storage);
            sendStorageDebtLines(player, plugin, storage, storage.getTaxDebtEntries());
            return false;
        }

        final Map<String, Double> debtEntries = storage.getTaxDebtEntries();
        final DebtSettlement settlement = settleStorageDebt(player, plugin, debtEntries);
        if (!settlement.success()) {
            sendStorageFrozenMessage(player, storage);
            sendStorageDebtLines(player, plugin, storage, debtEntries);
            return false;
        }

        storage.setTaxDebtEntries(Map.of());
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository != null) {
            storageRepository.update(storage);
        }

        recordCollectedTaxCharges(plugin, player, settlement.paidCharges());
        sendStorageDebtPaidMessage(player, storage);
        sendStorageDebtPaidLines(player, settlement.paidCharges(), plugin);
        return true;
    }

    private static boolean passesProtectionRestrictions(
        final @NotNull Player player,
        final @NotNull RStorage storage,
        final @NotNull ConfigSection config
    ) {
        if (!config.isProtectionRestrictedStorage(storage.getStorageKey())) {
            return true;
        }

        final RProtectionBridge bridge = RProtectionBridge.getBridge();
        if (bridge == null) {
            sendProtectionUnavailableMessage(player, storage);
            return false;
        }

        if (!bridge.isPlayerStandingInOwnTown(player)) {
            sendProtectionDeniedMessage(player, storage, bridge.getPluginName());
            return false;
        }

        return true;
    }

    private static boolean applyOpenStorageTaxes(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull ConfigSection config
    ) {
        if (!config.isProtectionTaxedStorage(storage.getStorageKey())) {
            return true;
        }

        final List<TaxCharge> taxCharges = resolveTaxCharges(config);
        if (taxCharges.isEmpty()) {
            return true;
        }

        final JExEconomyBridge economyBridge = JExEconomyBridge.getBridge();
        for (final TaxCharge taxCharge : taxCharges) {
            final TaxCheckStatus taxCheckStatus = checkTaxFunds(player, plugin, economyBridge, taxCharge);
            if (taxCheckStatus == TaxCheckStatus.UNAVAILABLE) {
                sendTaxCurrencyUnavailableMessage(player, storage, taxCharge.currencyId());
                return false;
            }
            if (taxCheckStatus == TaxCheckStatus.INSUFFICIENT) {
                sendTaxInsufficientMessage(player, plugin, storage, taxCharge);
                return false;
            }
        }

        final List<TaxCharge> collectedCharges = new ArrayList<>();
        for (final TaxCharge taxCharge : taxCharges) {
            if (withdrawTax(player, plugin, economyBridge, taxCharge)) {
                collectedCharges.add(taxCharge);
                continue;
            }

            refundCollectedTaxes(player, plugin, economyBridge, collectedCharges);
            sendTaxWithdrawFailedMessage(player, plugin, storage, taxCharge);
            return false;
        }

        recordCollectedTaxCharges(plugin, player, collectedCharges);
        sendTaxChargedMessages(player, plugin, storage, collectedCharges);
        return true;
    }

    private static @NotNull List<TaxCharge> resolveTaxCharges(final @NotNull ConfigSection config) {
        final List<TaxCharge> charges = new ArrayList<>();
        for (final Map.Entry<String, Double> entry : config.getProtectionOpenStorageTaxes().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }

            final double amount = entry.getValue() == null ? 0.0D : entry.getValue();
            if (amount <= EPSILON) {
                continue;
            }

            charges.add(new TaxCharge(normalizeCurrencyId(entry.getKey()), amount));
        }
        return charges;
    }

    private static @NotNull TaxCheckStatus checkTaxFunds(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final JExEconomyBridge economyBridge,
        final @NotNull TaxCharge taxCharge
    ) {
        if (isVaultCurrency(taxCharge.currencyId())) {
            if (!plugin.hasVaultEconomy()) {
                return TaxCheckStatus.UNAVAILABLE;
            }
            return plugin.hasVaultFunds(player, taxCharge.amount())
                ? TaxCheckStatus.SUFFICIENT
                : TaxCheckStatus.INSUFFICIENT;
        }

        if (economyBridge == null || !economyBridge.hasCurrency(taxCharge.currencyId())) {
            return TaxCheckStatus.UNAVAILABLE;
        }

        return economyBridge.has(player, taxCharge.currencyId(), taxCharge.amount())
            ? TaxCheckStatus.SUFFICIENT
            : TaxCheckStatus.INSUFFICIENT;
    }

    private static boolean withdrawTax(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final JExEconomyBridge economyBridge,
        final @NotNull TaxCharge taxCharge
    ) {
        if (isVaultCurrency(taxCharge.currencyId())) {
            return plugin.withdrawVault(player, taxCharge.amount());
        }

        return economyBridge != null && safeJoin(economyBridge.withdraw(player, taxCharge.currencyId(), taxCharge.amount()));
    }

    private static void refundCollectedTaxes(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final JExEconomyBridge economyBridge,
        final @NotNull List<TaxCharge> collectedCharges
    ) {
        for (int index = collectedCharges.size() - 1; index >= 0; index--) {
            final TaxCharge charge = collectedCharges.get(index);
            final boolean refunded = depositTax(player, plugin, economyBridge, charge);
            if (!refunded) {
                plugin.getLogger().warning(
                    "Failed to refund storage tax charge for "
                        + player.getName()
                        + " in currency "
                        + charge.currencyId()
                        + " amount "
                        + charge.amount()
                );
            }
        }
    }

    private static boolean depositTax(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final JExEconomyBridge economyBridge,
        final @NotNull TaxCharge taxCharge
    ) {
        if (isVaultCurrency(taxCharge.currencyId())) {
            return plugin.depositVault(player, taxCharge.amount());
        }

        return economyBridge != null && safeJoin(economyBridge.deposit(player, taxCharge.currencyId(), taxCharge.amount()));
    }

    private static void sendProtectionUnavailableMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.protection_unavailable", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendProtectionDeniedMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage,
        final @NotNull String protectionPlugin
    ) {
        new I18n.Builder("storage.message.protection_denied", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .withPlaceholder("protection_plugin", protectionPlugin)
            .build()
            .sendMessage();
    }

    private static void sendTaxCurrencyUnavailableMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage,
        final @NotNull String currencyId
    ) {
        new I18n.Builder("storage.message.tax_currency_unavailable", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(currencyId))
            .build()
            .sendMessage();
    }

    private static void sendTaxInsufficientMessage(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull TaxCharge taxCharge
    ) {
        new I18n.Builder("storage.message.tax_insufficient", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(taxCharge.currencyId()))
            .withPlaceholder("amount", formatTaxAmount(plugin, taxCharge))
            .build()
            .sendMessage();
    }

    private static void sendTaxWithdrawFailedMessage(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull TaxCharge taxCharge
    ) {
        new I18n.Builder("storage.message.tax_withdraw_failed", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(taxCharge.currencyId()))
            .withPlaceholder("amount", formatTaxAmount(plugin, taxCharge))
            .build()
            .sendMessage();
    }

    private static void sendStorageFrozenMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.frozen_payment_due", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendStorageDebtPaidMessage(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        new I18n.Builder("storage.message.frozen_payment_paid", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();
    }

    private static void sendStorageDebtLines(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull Map<String, Double> debtEntries
    ) {
        new I18n.Builder("storage.message.frozen_payment_due_header", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();

        for (final Map.Entry<String, Double> debtEntry : debtEntries.entrySet()) {
            if (debtEntry.getKey() == null || debtEntry.getKey().isBlank() || debtEntry.getValue() == null) {
                continue;
            }

            final TaxCharge debtCharge = new TaxCharge(
                normalizeCurrencyId(debtEntry.getKey()),
                Math.max(0.0D, debtEntry.getValue())
            );
            if (debtCharge.amount() <= EPSILON) {
                continue;
            }

            new I18n.Builder("storage.message.frozen_payment_due_line", player)
                .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(debtCharge.currencyId()))
                .withPlaceholder("amount", formatTaxAmount(plugin, debtCharge))
                .build()
                .sendMessage();
        }
    }

    private static void sendStorageDebtPaidLines(
        final @NotNull Player player,
        final @NotNull List<TaxCharge> paidCharges,
        final @NotNull RDR plugin
    ) {
        if (paidCharges.isEmpty()) {
            return;
        }

        new I18n.Builder("storage.message.frozen_payment_paid_header", player)
            .build()
            .sendMessage();

        for (final TaxCharge paidCharge : paidCharges) {
            new I18n.Builder("storage.message.frozen_payment_paid_line", player)
                .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(paidCharge.currencyId()))
                .withPlaceholder("amount", formatTaxAmount(plugin, paidCharge))
                .build()
                .sendMessage();
        }
    }

    private static @NotNull DebtSettlement settleStorageDebt(
        final @NotNull Player owner,
        final @NotNull RDR plugin,
        final @NotNull Map<String, Double> debtEntries
    ) {
        final List<TaxCharge> charges = new ArrayList<>();
        for (final Map.Entry<String, Double> debtEntry : debtEntries.entrySet()) {
            if (debtEntry.getKey() == null || debtEntry.getKey().isBlank() || debtEntry.getValue() == null) {
                continue;
            }
            final double amount = Math.max(0.0D, debtEntry.getValue());
            if (amount <= EPSILON) {
                continue;
            }
            charges.add(new TaxCharge(normalizeCurrencyId(debtEntry.getKey()), amount));
        }
        if (charges.isEmpty()) {
            return new DebtSettlement(true, List.of());
        }

        final JExEconomyBridge economyBridge = JExEconomyBridge.getBridge();
        for (final TaxCharge charge : charges) {
            final TaxCheckStatus checkStatus = checkTaxFunds(owner, plugin, economyBridge, charge);
            if (checkStatus != TaxCheckStatus.SUFFICIENT) {
                return new DebtSettlement(false, List.of());
            }
        }

        final List<TaxCharge> paidCharges = new ArrayList<>();
        for (final TaxCharge charge : charges) {
            if (withdrawTax(owner, plugin, economyBridge, charge)) {
                paidCharges.add(charge);
                continue;
            }

            refundCollectedTaxes(owner, plugin, economyBridge, paidCharges);
            return new DebtSettlement(false, List.of());
        }

        return new DebtSettlement(true, paidCharges);
    }

    private static void sendTaxChargedMessages(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage,
        final @NotNull List<TaxCharge> chargedTaxes
    ) {
        if (chargedTaxes.isEmpty()) {
            return;
        }

        new I18n.Builder("storage.message.tax_charged_header", player)
            .withPlaceholder("storage_key", storage.getStorageKey())
            .build()
            .sendMessage();

        for (final TaxCharge taxCharge : chargedTaxes) {
            new I18n.Builder("storage.message.tax_charged_line", player)
                .withPlaceholder("currency", StorageStorePricingSupport.getCurrencyDisplayName(taxCharge.currencyId()))
                .withPlaceholder("amount", formatTaxAmount(plugin, taxCharge))
                .build()
                .sendMessage();
        }
    }

    private static void recordCollectedTaxCharges(
        final @NotNull RDR plugin,
        final @NotNull Player player,
        final @NotNull List<TaxCharge> collectedCharges
    ) {
        for (final TaxCharge collectedCharge : collectedCharges) {
            if (collectedCharge.amount() <= EPSILON) {
                continue;
            }
            StorageTownTaxBankService.recordCollectedTax(
                plugin,
                player,
                collectedCharge.currencyId(),
                collectedCharge.amount()
            );
        }
    }

    private static @NotNull String formatTaxAmount(
        final @NotNull RDR plugin,
        final @NotNull TaxCharge taxCharge
    ) {
        return StorageStorePricingSupport.formatCurrency(plugin, taxCharge.currencyId(), taxCharge.amount());
    }

    private static boolean isVaultCurrency(final @NotNull String currencyId) {
        return "vault".equalsIgnoreCase(currencyId);
    }

    private static @NotNull String normalizeCurrencyId(final @NotNull String currencyId) {
        return currencyId.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean safeJoin(final @NotNull CompletableFuture<Boolean> future) {
        try {
            return Boolean.TRUE.equals(future.join());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void releaseLease(
        final @NotNull RDR plugin,
        final @NotNull Long storageId,
        final @NotNull UUID playerUuid,
        final @NotNull UUID leaseToken
    ) {
        final RRStorage storageRepository = plugin.getStorageRepository();
        if (storageRepository == null) {
            return;
        }

        storageRepository.releaseLeaseAsync(
            storageId,
            plugin.getServerUuid(),
            playerUuid,
            leaseToken
        );
    }

    private record TaxCharge(@NotNull String currencyId, double amount) {
    }

    private enum TaxCheckStatus {
        SUFFICIENT,
        INSUFFICIENT,
        UNAVAILABLE
    }

    private record DebtSettlement(boolean success, @NotNull List<TaxCharge> paidCharges) {
    }
}
