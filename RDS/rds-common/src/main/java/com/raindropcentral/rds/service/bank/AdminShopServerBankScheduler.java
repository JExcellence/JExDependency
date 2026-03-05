package com.raindropcentral.rds.service.bank;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ServerBank;
import com.raindropcentral.rds.database.repository.RServerBank;
import com.raindropcentral.rds.database.repository.RShop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * Schedules periodic transfers from admin-shop banks into the shared server bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class AdminShopServerBankScheduler {

    private final RDS plugin;
    private boolean running = false;
    private long transferIntervalTicks = 0L;
    private Instant nextTransferAt;

    /**
     * Creates a new admin-shop server bank scheduler.
     *
     * @param plugin plugin instance
     */
    public AdminShopServerBankScheduler(
            final @NotNull RDS plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts the recurring admin-shop server bank transfer task when enabled.
     */
    public void start() {
        if (this.running) {
            return;
        }

        final var section = this.plugin.getDefaultConfig().getServerBank();
        if (!section.isEnabled()) {
            this.plugin.getLogger().info("Server bank transfer scheduler is disabled in config.");
            this.running = false;
            this.transferIntervalTicks = 0L;
            this.nextTransferAt = null;
            return;
        }

        final long periodTicks = section.getTransferIntervalTicks();
        this.transferIntervalTicks = periodTicks;
        this.nextTransferAt = this.calculateNextTransferAt(Instant.now());
        this.plugin.getScheduler().runRepeating(this::transferAdminShopBanksToServerBank, periodTicks, periodTicks);
        this.plugin.getLogger().info("Scheduling admin shop bank transfers every " + periodTicks + " ticks.");
        this.running = true;
    }

    /**
     * Indicates whether the scheduler is actively running.
     *
     * @return {@code true} when the recurring transfer task has been scheduled
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Returns the configured transfer period in Bukkit ticks.
     *
     * @return transfer period in ticks
     */
    public long getTransferIntervalTicks() {
        return this.transferIntervalTicks;
    }

    /**
     * Returns the currently tracked instant for the next transfer attempt.
     *
     * @return next transfer instant, or {@code null} when unavailable
     */
    public @Nullable Instant getNextTransferAt() {
        return this.nextTransferAt;
    }

    private void transferAdminShopBanksToServerBank() {
        final RShop shopRepository = this.plugin.getShopRepository();
        final RServerBank serverBankRepository = this.plugin.getServerBankRepository();
        if (shopRepository == null || serverBankRepository == null) {
            return;
        }

        if (!this.plugin.getDefaultConfig().getServerBank().isEnabled()) {
            return;
        }

        for (final Shop shop : shopRepository.findAllShops()) {
            this.transferShopBankBalances(shop, shopRepository, serverBankRepository);
        }

        this.nextTransferAt = this.calculateNextTransferAt(Instant.now());
    }

    private void transferShopBankBalances(
            final @NotNull Shop shop,
            final @NotNull RShop shopRepository,
            final @NotNull RServerBank serverBankRepository
    ) {
        final Map<String, Double> transferable = ServerBankTransferSupport.collectTransferableBalances(shop);
        if (transferable.isEmpty()) {
            return;
        }

        boolean shopUpdated = false;
        for (final Map.Entry<String, Double> entry : transferable.entrySet()) {
            final String currencyType = entry.getKey();
            final double amount = entry.getValue();
            if (!this.depositToServerBank(serverBankRepository, currencyType, amount)) {
                continue;
            }

            if (shop.withdrawBank(currencyType, amount)) {
                shopUpdated = true;
            }
        }

        if (shopUpdated) {
            shopRepository.update(shop);
        }
    }

    private boolean depositToServerBank(
            final @NotNull RServerBank repository,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        try {
            final ServerBank existing = repository.findByCurrencyType(currencyType);
            if (existing == null) {
                final ServerBank created = new ServerBank(currencyType, amount);
                repository.create(created);
                return true;
            }

            existing.deposit(amount);
            repository.update(existing);
            return true;
        } catch (RuntimeException exception) {
            this.plugin.getLogger().warning(
                    "Failed to transfer admin shop bank currency "
                            + currencyType
                            + " to server bank: "
                            + exception.getMessage()
            );
            return false;
        }
    }

    private @NotNull Instant calculateNextTransferAt(
            final @NotNull Instant now
    ) {
        if (this.transferIntervalTicks <= 0L) {
            return now.plusSeconds(1L);
        }

        return now.plusMillis(this.transferIntervalTicks * 50L);
    }
}
