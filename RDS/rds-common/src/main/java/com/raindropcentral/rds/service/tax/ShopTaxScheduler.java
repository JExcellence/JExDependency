package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.configs.TaxCurrencySection;
import com.raindropcentral.rds.configs.TaxSection;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Schedules shop tax operations.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopTaxScheduler {

    private static final Logger LOGGER = Logger.getLogger("RDS");
    private static final DateTimeFormatter START_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final RDS plugin;
    private boolean running = false;

    /**
     * Creates a new shop tax scheduler.
     *
     * @param plugin plugin instance
     */
    public ShopTaxScheduler(
            final @NotNull RDS plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts shop tax scheduler processing.
     */
    public void start() {
        if (this.running) {
            return;
        }

        final ConfigSection config = this.plugin.getDefaultConfig();
        final TaxSection taxes = config.getTaxes();
        final long initialDelayTicks = ShopTaxSupport.calculateInitialDelayTicks(taxes, Instant.now());
        final long periodTicks = taxes.getDurationTicks();
        final List<String> configuredCurrencies = new ArrayList<>();
        for (final String currencyType : taxes.getCurrencies().keySet()) {
            configuredCurrencies.add(currencyType);
            if (ShopTaxSupport.isCurrencyAvailable(this.plugin, currencyType)) {
                continue;
            }

            LOGGER.warning(
                    "Shop tax scheduler started with unavailable currency "
                            + currencyType
                            + ". Charges in that currency will be skipped until it becomes available."
            );
        }

        LOGGER.info(
                "Scheduling shop taxes every "
                        + periodTicks
                        + " ticks (~"
                        + String.format(Locale.US, "%.2f", periodTicks / 72000.0D)
                        + " hours) starting from "
                        + taxes.getStartTime().format(START_TIME_FORMAT)
                        + " "
                        + taxes.getTimeZoneId().getId()
                        + " using currencies "
                        + configuredCurrencies
        );

        this.plugin.getScheduler().runRepeating(this::collectTaxes, initialDelayTicks, periodTicks);
        this.running = true;
    }

    private void collectTaxes() {
        final ConfigSection config = this.plugin.getDefaultConfig();
        final TaxSection taxes = config.getTaxes();
        final Map<UUID, List<Shop>> ownedShopsByOwner = this.groupTaxableShopsByOwner();
        if (ownedShopsByOwner.isEmpty()) {
            return;
        }

        for (final Map.Entry<UUID, List<Shop>> entry : ownedShopsByOwner.entrySet()) {
            final List<Shop> ownedShops = entry.getValue();
            if (ownedShops.isEmpty()) {
                continue;
            }

            final OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.getKey());
            this.collectTaxesForOwner(taxes, owner, ownedShops);
        }
    }

    private void collectTaxesForOwner(
            final @NotNull TaxSection taxes,
            final @NotNull OfflinePlayer owner,
            final @NotNull List<Shop> ownedShops
    ) {
        final List<Shop> taxableOwnedShops = new ArrayList<>();
        for (final Shop shop : ownedShops) {
            if (ShopTaxSupport.isTaxableShop(shop)) {
                taxableOwnedShops.add(shop);
            }
        }
        if (taxableOwnedShops.isEmpty()) {
            return;
        }

        final int ownedShopCount = taxableOwnedShops.size();
        final int neverAvailabilityItems = ShopTaxSupport.countNeverAvailabilityItems(taxableOwnedShops);
        for (final Map.Entry<String, TaxCurrencySection> taxEntry : taxes.getCurrencies().entrySet()) {
            final String currencyType = taxEntry.getKey();
            if (!ShopTaxSupport.isCurrencyAvailable(this.plugin, currencyType)) {
                continue;
            }

            final double baseTax = ShopTaxSupport.calculateTax(taxEntry.getValue(), ownedShopCount);
            final double taxAmount = ShopTaxSupport.applyNeverItemPenalty(
                    baseTax,
                    taxes.getNeverItemPenaltyRate(),
                    neverAvailabilityItems
            );
            if (taxAmount <= 0D) {
                continue;
            }

            final String formattedTax = ShopTaxSupport.formatCurrency(this.plugin, currencyType, taxAmount);
            final String currencyName = ShopTaxSupport.getCurrencyDisplayName(currencyType);
            final Map<String, Object> placeholders = Map.of(
                    "amount", formattedTax,
                    "owned_shops", ownedShopCount,
                    "never_item_count", neverAvailabilityItems,
                    "currency_type", currencyType,
                    "currency_name", currencyName
            );

            if (!ShopTaxSupport.hasFunds(this.plugin, owner, currencyType, taxAmount)) {
                this.notifyOwner(owner, "tax_scheduler.insufficient_funds", placeholders);
                continue;
            }

            if (!ShopTaxSupport.withdraw(this.plugin, owner, currencyType, taxAmount)) {
                this.notifyOwner(owner, "tax_scheduler.charge_failed", placeholders);
                continue;
            }

            this.recordTaxLedgerEntries(taxableOwnedShops, owner, currencyType, taxAmount, ownedShopCount);
            this.notifyOwner(owner, "tax_scheduler.paid", placeholders);
        }
    }

    private void recordTaxLedgerEntries(
            final @NotNull List<Shop> ownedShops,
            final @NotNull OfflinePlayer owner,
            final @NotNull String currencyType,
            final double totalAmount,
            final int countedShops
    ) {
        if (ownedShops.isEmpty() || totalAmount <= 0D) {
            return;
        }

        final String ownerName = owner.getName() == null || owner.getName().isBlank()
                ? owner.getUniqueId().toString()
                : owner.getName();
        final double baseShare = totalAmount / ownedShops.size();

        for (int index = 0; index < ownedShops.size(); index++) {
            final Shop shop = ownedShops.get(index);
            final double shareAmount = index == ownedShops.size() - 1
                    ? Math.max(0D, totalAmount - (baseShare * index))
                    : baseShare;

            shop.addLedgerEntry(
                    ShopLedgerEntry.taxation(
                            shop,
                            owner.getUniqueId(),
                            ownerName,
                            currencyType,
                            shareAmount,
                            countedShops
                    )
            );
            this.plugin.getShopRepository().update(shop);
        }
    }

    private @NotNull Map<UUID, List<Shop>> groupTaxableShopsByOwner() {
        final Map<UUID, List<Shop>> groupedShops = new HashMap<>();
        for (final Shop shop : this.plugin.getShopRepository().findAllShops()) {
            if (!ShopTaxSupport.isTaxableShop(shop)) {
                continue;
            }

            groupedShops.computeIfAbsent(shop.getOwner(), ignored -> new ArrayList<>()).add(shop);
        }

        return groupedShops;
    }

    private void notifyOwner(
            final @NotNull OfflinePlayer owner,
            final @NotNull String key,
            final @NotNull Map<String, Object> placeholders
    ) {
        final Player onlinePlayer = owner.getPlayer();
        if (onlinePlayer == null) {
            return;
        }

        new I18n.Builder(key, onlinePlayer)
                .withPlaceholders(placeholders)
                .includePrefix()
                .build()
                .sendMessage();
    }
}
